package com.frc.codex.indexer.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frc.codex.FilingIndexProperties;
import com.frc.codex.RegistryCode;
import com.frc.codex.database.DatabaseManager;
import com.frc.codex.discovery.companieshouse.CompaniesHouseClient;
import com.frc.codex.discovery.companieshouse.CompaniesHouseHistoryClient;
import com.frc.codex.discovery.companieshouse.RateLimitException;
import com.frc.codex.discovery.fca.FcaClient;
import com.frc.codex.discovery.fca.FcaFiling;
import com.frc.codex.indexer.Indexer;
import com.frc.codex.indexer.QueueManager;
import com.frc.codex.model.Company;
import com.frc.codex.model.Filing;
import com.frc.codex.model.FilingResultRequest;
import com.frc.codex.model.FilingStatus;
import com.frc.codex.model.NewFilingRequest;
import com.frc.codex.model.companieshouse.CompaniesHouseArchive;

@Component
@Profile("application")
public class IndexerImpl implements Indexer {
	private static final DateTimeFormatter CH_JSON_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final int COMPANIES_BATCH_SIZE = 100;
	private static final int COMPANIES_LIMIT = 1000;
	private static final Logger LOG = LoggerFactory.getLogger(IndexerImpl.class);
	private final CompaniesHouseClient companiesHouseClient;
	private final CompaniesHouseHistoryClient companiesHouseHistoryClient;
	private final DatabaseManager databaseManager;
	private final FcaClient fcaClient;
	private final FilingIndexProperties properties;
	private final QueueManager queueManager;

	private final Pattern companiesHouseFilenamePattern;
	private int companiesHouseSessionFilingCount;
	private Date companiesHouseStreamLastOpenedDate;
	private Long companiesHouseSessionLatestTimepoint;
	private Long companiesHouseSessionStartTimepoint;
	private Date fcaSessionLastStartedDate;
	private Date fcaSessionLastEndedDate;

	public IndexerImpl(
			FilingIndexProperties properties,
			CompaniesHouseClient companiesHouseClient,
			CompaniesHouseHistoryClient companiesHouseHistoryClient,
			DatabaseManager databaseManager,
			FcaClient fcaClient,
			QueueManager queueManager
	) {
		this.properties = properties;
		this.companiesHouseClient = companiesHouseClient;
		this.companiesHouseHistoryClient = companiesHouseHistoryClient;
		this.databaseManager = databaseManager;
		this.fcaClient = fcaClient;
		this.queueManager = queueManager;
		this.companiesHouseFilenamePattern = Pattern.compile(
				"Prod\\d+_\\d+_([a-zA-Z0-9]+)_(\\d{8})\\.html"
		);
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
		String eventType = event.get("type").asText();
		if (!"changed".equals(eventType)) {
			// Only other possible value is "deleted".
			return timepoint;
		}
		JsonNode data = filing.get("data");
		JsonNode filingDateNode = data.get("date");
		LocalDateTime filingDate = null;
		if (filingDateNode != null) {
			String dateStr = filingDateNode.asText();
			try {
				filingDate = LocalDate.parse(dateStr, CH_JSON_DATE_FORMAT).atStartOfDay();
			} catch (Exception e) {
				throw new RuntimeException("Failed to parse date: " + dateStr, e);
			}
		}
		// Note: `action_date` is not a documented field but appears to consistently represent
		// the document date for the filing.
		JsonNode documentDateNode = data.get("action_date");
		LocalDateTime documentDate = null;
		if (documentDateNode != null) {
			String dateStr = documentDateNode.asText();
			try {
				documentDate = LocalDate.parse(dateStr, CH_JSON_DATE_FORMAT).atStartOfDay();
			} catch (Exception e) {
				throw new RuntimeException("Failed to parse date: " + dateStr, e);
			}
		}
		String resourceUri = filing.get("resource_uri").asText();
		String[] resourceUriSplit = resourceUri.split("/");
		String companyNumber = resourceUriSplit[2];
		String resourceId = filing.get("resource_id").asText();
		String externalFilingId = data.get("transaction_id").asText();
		Set<String> filingUrls = this.companiesHouseClient.getCompanyFilingUrls(companyNumber, resourceId);
		for (String filingUrl : filingUrls) {
			NewFilingRequest newFilingRequest = new NewFilingRequest();
			newFilingRequest.setCompanyNumber(companyNumber);
			newFilingRequest.setDocumentDate(documentDate);
			newFilingRequest.setDownloadUrl(filingUrl);
			newFilingRequest.setFilingDate(filingDate);
			newFilingRequest.setRegistryCode(RegistryCode.COMPANIES_HOUSE.toString());
			newFilingRequest.setExternalFilingId(externalFilingId);
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
	public void indexCompaniesHouseFilings() throws IOException {
		if (databaseManager.checkRegistryLimit(RegistryCode.COMPANIES_HOUSE, properties.filingLimitCompaniesHouse())) {
			return;
		}
		LOG.info("Starting Companies House indexing at {}", System.currentTimeMillis() / 1000);
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
			if (databaseManager.checkRegistryLimit(RegistryCode.COMPANIES_HOUSE, properties.filingLimitCompaniesHouse())) {
				return false;
			}
			if (companiesHouseSessionStartTimepoint == null) {
				companiesHouseSessionStartTimepoint = timepoint;
			}
			companiesHouseSessionLatestTimepoint = timepoint;
			return true; // Continue streaming
		};
		long startTimepoint = this.databaseManager.getLatestStreamTimepoint(null);
		this.companiesHouseStreamLastOpenedDate = new Date();
		try {
			this.companiesHouseClient.streamFilings(startTimepoint, callback);
		} catch (RateLimitException e) {
			LOG.warn("Rate limit exceeded while streaming CH filings. Resuming later.", e);
		}
	}

	@Scheduled(fixedDelay = 60 * 1000)
	public void indexFilingsFromCompaniesIndex() throws JsonProcessingException {
		LOG.info("Indexing filings from companies index.");
		List<Company> companies = databaseManager.getIncompleteCompanies(COMPANIES_BATCH_SIZE);
		LOG.info("Loaded {} incomplete companies from companies index.", companies.size());
		for (Company company : companies) {
			String companyNumber = company.getCompanyNumber();
			String companyName = company.getCompanyName();
			if (companyName == null) {
				// If we don't already have the company's name, pull it from the API.
				Company updateCompany = companiesHouseClient.getCompany(companyNumber);
				companyName = updateCompany.getCompanyName();
				databaseManager.updateCompany(updateCompany);
			}
			LOG.info("Retrieving filings for company {}.", companyNumber);
			List<NewFilingRequest> filings;
			try {
				filings = companiesHouseClient.getCompanyFilings(companyNumber);
			} catch (RateLimitException e) {
				LOG.warn("Rate limit exceeded while retrieving CH filing history. Resuming later.", e);
				return;
			}
			LOG.info("Retrieved {} filings for company {}.", filings.size(), companyNumber);
			for (NewFilingRequest filing : filings) {
				filing.setCompanyName(companyName);
				if (databaseManager.filingExists(filing)) {
					LOG.info("Skipping existing filing: {}", filing.getDownloadUrl());
					continue;
				}
				if (databaseManager.checkRegistryLimit(RegistryCode.COMPANIES_HOUSE, properties.filingLimitCompaniesHouse())) {
					break;
				}
				UUID filingId = databaseManager.createFiling(filing);
				LOG.info("Created CH filing for {}: {}", filing.getDownloadUrl(), filingId);

			}
			Company updatedCompany = Company.builder()
					.companyNumber(companyNumber)
					.completedDate(new Timestamp(System.currentTimeMillis()))
					.build();
			databaseManager.updateCompany(updatedCompany);
			LOG.info("Completed company: {}", companyNumber);
		}
	}

	/*
	 * Processes a Companies House archive by downloading it and index known companies by
	 * extracting company numbers from the contained filenames.
	 * Returns true if the archive was processed successfully or doesn't need processing.
	 */
	private boolean processCompaniesHouseArchive(URI uri, String archiveType) {
		if (databaseManager.checkCompaniesLimit(COMPANIES_LIMIT)) {
			return false;
		}
		String filename = new File(uri.getPath()).getName();
		if (databaseManager.companiesHouseArchiveExists(filename)) {
			LOG.debug("Skipping existing CH archive: {}", uri);
			return true;
		}
		boolean completed = true;
		LOG.info("Downloading archive: {}", uri);
		File tempFile;
		try {
			tempFile = File.createTempFile(filename, ".zip");
			tempFile.deleteOnExit();
			this.companiesHouseHistoryClient.downloadArchive(uri, tempFile.toPath());
		} catch (IOException e) {
			LOG.error("Failed to download archive: {}", uri, e);
			return false;
		}
		LOG.info("Downloaded archive: {}", tempFile.toPath());

		List<String> arcnames;
		try (ZipFile zipFile = new ZipFile(tempFile)) {
			arcnames = zipFile.stream()
					.map(ZipEntry::getName)
					.sorted()
					.toList();
		} catch (Exception e) {
			LOG.error("Failed to get arcnames for archive: {}", uri, e);
			return false;
		}
		LOG.info("Found arcnames: {}", arcnames.size());

		// Example: Prod223_3785_13056435_20240331.html
		for (String arcname : arcnames) {
			Matcher matcher = companiesHouseFilenamePattern.matcher(arcname);
			if (!matcher.matches()) {
				LOG.error("Found invalid archive entry in {}: {}", uri, arcname);
				completed = false;
				continue;
			}
			String companyNumber = matcher.group(1);
			Company company = Company.builder()
					.companyNumber(companyNumber)
					.build();
			if (databaseManager.companyExists(company)) {
				LOG.debug("Skipping existing company: {}", companyNumber);
				continue;
			}
			databaseManager.createCompany(company);
			LOG.debug("Created company {}.", companyNumber);
		}
		if (completed) {
			CompaniesHouseArchive archive = CompaniesHouseArchive.builder()
					.filename(filename)
					.uri(uri)
					.archiveType(archiveType)
					.build();
			databaseManager.createCompaniesHouseArchive(archive);
		}
		return true;
	}

	@Scheduled(fixedDelay = 30 * 60 * 1000)
	public void indexCompaniesFromCompaniesHouseArchives() {
		if (databaseManager.checkCompaniesLimit(COMPANIES_LIMIT)) {
			return;
		}
		List<URI> downloadLinks;
		downloadLinks = companiesHouseHistoryClient.getDailyDownloadLinks();
		for (URI uri : downloadLinks) {
			if (!processCompaniesHouseArchive(uri, "daily")) {
				return;
			}
		}
		downloadLinks = companiesHouseHistoryClient.getMonthlyDownloadLinks();
		for (URI uri : downloadLinks) {
			if (!processCompaniesHouseArchive(uri, "monthly")) {
				return;
			}
		}
		downloadLinks = companiesHouseHistoryClient.getArchiveDownloadLinks();
		for (URI uri : downloadLinks) {
			if (!processCompaniesHouseArchive(uri, "archive")) {
				return;
			}
		}
	}

	/*
	 * Indexes FCA filings.
	 * Runs hourly, taking only a few seconds.
	 * Can share a scheduler thread with other tasks.
	 */
	@Scheduled(fixedDelay = 60 * 60 * 1000)
	public void indexFca() {
		fcaSessionLastStartedDate = new Date();
		LOG.info("Starting FCA indexing at {}", fcaSessionLastStartedDate);
		if (databaseManager.checkRegistryLimit(RegistryCode.FCA, properties.filingLimitFca())) {
			return;
		}
		LocalDateTime fcaStartDate = properties.fcaPastDays() <= 0 ? null :
				LocalDateTime.now().minusDays(properties.fcaPastDays());
		LocalDateTime latestSubmittedDate = databaseManager.getLatestFcaFilingDate(fcaStartDate);
		List<FcaFiling> filings = fcaClient.fetchAllSinceDate(latestSubmittedDate);
		for (FcaFiling filing : filings) {
			NewFilingRequest newFilingRequest = new NewFilingRequest();
			newFilingRequest.setCompanyName(filing.companyName());
			newFilingRequest.setCompanyNumber(filing.lei());
			newFilingRequest.setDocumentDate(filing.documentDate());
			newFilingRequest.setDownloadUrl(filing.downloadUrl());
			newFilingRequest.setExternalFilingId(filing.sequenceId());
			newFilingRequest.setFilingDate(filing.submittedDate());
			newFilingRequest.setExternalViewUrl(filing.infoUrl());
			newFilingRequest.setRegistryCode(RegistryCode.FCA.toString());
			if (databaseManager.filingExists(newFilingRequest)) {
				LOG.info("Skipping existing FCA filing: {}", filing.downloadUrl());
				continue;
			}
			if (databaseManager.checkRegistryLimit(RegistryCode.FCA, properties.filingLimitFca())) {
				break;
			}
			UUID filingId = databaseManager.createFiling(newFilingRequest);
			LOG.info("Created FCA filing for {}: {}", filing.downloadUrl(), filingId);
		}
		fcaSessionLastEndedDate = new Date();
		LOG.info("Completed FCA indexing at {}", fcaSessionLastEndedDate);
	}


	/*
	 * Retrieves messages from the results queue and applies them to the database.
	 * Reruns after a delay of 20 seconds.
	 * Can share a scheduler thread with other tasks.
	 */
	@Scheduled(fixedDelay = 20 * 1000)
	public void processResults() {
		if (!properties.enablePreprocessing()) {
			return;
		}
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
		if (!properties.enablePreprocessing()) {
			return;
		}
		LOG.info("Starting to queue jobs.");
		List<Filing> filings = databaseManager.getFilingsByStatus(FilingStatus.PENDING);
		LOG.info("Pending filings: {}", filings.size());
		queueManager.addJobs(filings, (Filing filing) -> {
			databaseManager.updateFilingStatus(filing.getFilingId(), FilingStatus.QUEUED.toString());
		});
	}
}
