package com.frc.codex.indexer.impl;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frc.codex.RegistryCode;
import com.frc.codex.database.DatabaseManager;
import com.frc.codex.discovery.companieshouse.CompaniesHouseClient;
import com.frc.codex.discovery.fca.FcaClient;
import com.frc.codex.discovery.fca.FcaFiling;
import com.frc.codex.indexer.Indexer;
import com.frc.codex.indexer.QueueManager;
import com.frc.codex.model.Filing;
import com.frc.codex.model.FilingResultRequest;
import com.frc.codex.model.NewFilingRequest;

@Component
@Profile("application")
public class IndexerImpl implements Indexer {
	private static final Logger LOG = LoggerFactory.getLogger(IndexerImpl.class);
	private final CompaniesHouseClient companiesHouseClient;
	private final DatabaseManager databaseManager;
	private final FcaClient fcaClient;
	private final QueueManager queueManager;


	private int companiesHouseSessionFilingCount;
	private Date companiesHouseStreamLastOpenedDate;
	private Long companiesHouseSessionLatestTimepoint;
	private Long companiesHouseSessionStartTimepoint;
	private Date fcaSessionLastStartedDate;
	private Date fcaSessionLastEndedDate;

	public IndexerImpl(
			CompaniesHouseClient companiesHouseClient,
			DatabaseManager databaseManager,
			FcaClient fcaClient,
			QueueManager queueManager
	) {
		this.companiesHouseClient = companiesHouseClient;
		this.databaseManager = databaseManager;
		this.fcaClient = fcaClient;
		this.queueManager = queueManager;
	}

	public String getStatus() {
		boolean healthy = companiesHouseStreamLastOpenedDate != null &&
				companiesHouseSessionStartTimepoint != null;
		return String.format("""
						Indexer Status: %s
						Companies House:
						\tStream last opened: %s
						\tFilings discovered this session: %s
						\tEarliest timepoint this session: %s
						\tLatest timepoint this session: %s
						FCA:
						\tLast started: %s
						\tLast finished: %s""",
				healthy ? "Healthy" : "Unhealthy",
				companiesHouseStreamLastOpenedDate,
				companiesHouseSessionFilingCount,
				companiesHouseSessionStartTimepoint,
				companiesHouseSessionLatestTimepoint,
				fcaSessionLastStartedDate,
				fcaSessionLastEndedDate
		);
	}

	/*
	 * Processes a filing event JSON from the Companies House streaming API.
	 * Returns the timepoint of the event.
	 */
	private long handleFilingStreamEvent(String json) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode filing = mapper.readTree(json);
		JsonNode event = filing.get("event");
		long timepoint = event.get("timepoint").asLong();
		String resourceKind = filing.get("resource_kind").asText();
		if (!"filing-history".equals(resourceKind)) {
			return timepoint;
		}
		String resourceUri = filing.get("resource_uri").asText();
		String[] resourceUriSplit = resourceUri.split("/");
		String companyNumber = resourceUriSplit[2];
		String resourceId = filing.get("resource_id").asText();
		String eventType = event.get("type").asText();
		String publishedAt = event.get("published_at").asText();
		Set<String> filingUrls = this.companiesHouseClient.getCompanyFilingUrls(companyNumber, resourceId);
		for (String filingUrl : filingUrls) {
			NewFilingRequest newFilingRequest = new NewFilingRequest();
			newFilingRequest.setDownloadUrl(filingUrl);
			newFilingRequest.setRegistryCode(RegistryCode.COMPANIES_HOUSE.toString());
			newFilingRequest.setStreamTimepoint(timepoint);
			if (databaseManager.filingExists(newFilingRequest)) {
				LOG.info("Skipping existing CH filing: {}", filingUrl);
				continue;
			}
			UUID filingId = this.databaseManager.createFiling(newFilingRequest);
			LOG.info("Created CH filing for {}: {}", filingUrl, filingId);
			this.companiesHouseSessionFilingCount += 1;
		}
		return timepoint;
	}

	/*
	 * Indexes Companies House filings.
	 * Runs continuously as long as HTTP connection remains open.
	 * If the connection closes, resumes after one minute.
	 * One scheduler thread is effectively dedicated to this task.
	 */
	@Scheduled(fixedDelay = 60 * 1000)
	public void indexCompaniesHouse() throws IOException {
		LOG.info("Starting Companies House indexing at " + System.currentTimeMillis() / 1000);
		Function<String, Boolean> callback = (String filing) -> {
			if (filing == null || filing.length() <= 1) {
				// The stream emits blank "heartbeat" lines.
				return true;
			}
			long timepoint;
			try {
				timepoint = handleFilingStreamEvent(filing);
			} catch (JsonProcessingException e) {
				LOG.error("Failed to process filing event.", e);
				return false; // Stop streaming
			}
			if (companiesHouseSessionStartTimepoint == null) {
				companiesHouseSessionStartTimepoint = timepoint;
			}
			companiesHouseSessionLatestTimepoint = timepoint;
			return true; // Continue streaming
		};
		long startTimepoint = this.databaseManager.getLatestStreamTimepoint(null);
		this.companiesHouseStreamLastOpenedDate = new Date();
		this.companiesHouseClient.streamFilings(startTimepoint, callback);
		LOG.info("Completed Companies House indexing at " + System.currentTimeMillis() / 1000);
	}

	/*
	 * Indexes FCA filings.
	 * Runs hourly, taking only a few seconds.
	 * Can share a scheduler thread with other tasks.
	 */
	@Scheduled(fixedDelay = 60 * 60 * 1000)
	public void indexFca() {
		fcaSessionLastStartedDate = new Date();
		LOG.info("Starting FCA indexing at " + fcaSessionLastStartedDate);
		Date latestSubmittedDate = databaseManager.getLatestFcaFilingDate(new Date(new Date().getTime() - 30L * 24 * 60 * 60 * 1000));
		List<FcaFiling> filings = fcaClient.fetchAllSinceDate(latestSubmittedDate);
		for (FcaFiling filing : filings) {
			NewFilingRequest newFilingRequest = new NewFilingRequest();
			newFilingRequest.setDownloadUrl(filing.downloadUrl());
			newFilingRequest.setFilingDate(filing.submittedDate());
			newFilingRequest.setRegistryCode(RegistryCode.FCA.toString());
			if (databaseManager.filingExists(newFilingRequest)) {
				LOG.info("Skipping existing FCA filing: {}", filing.downloadUrl());
				continue;
			}
			UUID filingId = databaseManager.createFiling(newFilingRequest);
			LOG.info("Created FCA filing for {}: {}", filing.downloadUrl(), filingId);
		}
		fcaSessionLastEndedDate = new Date();
		LOG.info("Completed FCA indexing at " + fcaSessionLastEndedDate);
	}


	/*
	 * Retrieves messages from the results queue and applies them to the database.
	 * Reruns after a delay of 20 seconds.
	 * Can share a scheduler thread with other tasks.
	 */
	@Scheduled(fixedDelay = 20 * 1000)
	public void processResults() {
		LOG.info("Starting to process results.");
		queueManager.processResults((FilingResultRequest filingResultRequest) -> {
			try {
				LOG.info("Applying filing result: {}", filingResultRequest);
				databaseManager.applyFilingResult(filingResultRequest);
				return true;
			} catch (Exception e) {
				LOG.error("Failed to process result: {}", filingResultRequest.getFilingId(), e);
				return false;
			}
		});
	}

	/*
	 * Retrieves pending filings from the database and adds them to the job queue.
	 * Reruns after a delay of 20 seconds.
	 * Can share a scheduler thread with other tasks.
	 */
	@Scheduled(fixedDelay = 20 * 1000)
	public void queueJobs() {
		LOG.info("Starting to queue jobs.");
		List<Filing> filings = databaseManager.getPendingFilings();
		LOG.info("Pending filings: {}", filings.size());
		queueManager.addJobs(filings, (Filing filing) -> {
			databaseManager.updateFilingStatus(filing.getFilingId(), "queued");
		});
	}
}
