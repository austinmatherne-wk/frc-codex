package com.frc.codex.discovery.companieshouse.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.frc.codex.RegistryCode;
import com.frc.codex.database.DatabaseManager;
import com.frc.codex.discovery.companieshouse.CompaniesHouseClient;
import com.frc.codex.discovery.companieshouse.CompaniesHouseCompany;
import com.frc.codex.discovery.companieshouse.CompaniesHouseFiling;
import com.frc.codex.discovery.companieshouse.RateLimitException;
import com.frc.codex.indexer.IndexerJob;
import com.frc.codex.model.NewFilingRequest;

public class CompaniesHouseStreamIndexerImpl implements IndexerJob {
	private static final Logger LOG = LoggerFactory.getLogger(CompaniesHouseStreamIndexerImpl.class);
	private final CompaniesHouseClient companiesHouseClient;
	private final DatabaseManager databaseManager;
	private int companiesHouseSessionFilingCount;
	private Date companiesHouseStreamLastOpenedDate;
	private Long companiesHouseSessionLatestTimepoint;
	private Long companiesHouseSessionStartTimepoint;

	public CompaniesHouseStreamIndexerImpl(
			CompaniesHouseClient companiesHouseClient,
			DatabaseManager databaseManager
	) {
		this.companiesHouseClient = companiesHouseClient;
		this.databaseManager = databaseManager;
	}

	public String getStatus() {
		return String.format("""
						Companies House:
						\tStream last opened: %s
						\tFilings discovered this session: %s
						\tEarliest timepoint this session: %s
						\tLatest timepoint this session: %s""",
				companiesHouseStreamLastOpenedDate,
				companiesHouseSessionFilingCount,
				companiesHouseSessionStartTimepoint,
				companiesHouseSessionLatestTimepoint
		);
	}

	/*
	 * Processes a filing event from the Companies House streaming API.
	 * Returns the timepoint of the event.
	 */
	private long handleFilingStreamEvent(CompaniesHouseFiling companiesHouseFiling) throws JsonProcessingException {
		LOG.debug("CH filing stream event. Handling {}.", companiesHouseFiling.transactionId());
		long timepoint = companiesHouseFiling.timepoint();
		// Streaming event is a filing (should always be the case, but might as well check)
		if (!"filing-history".equals(companiesHouseFiling.resourceKind())) {
			LOG.debug("CH filing stream event: Skipped {}, not a filing.", companiesHouseFiling.transactionId());
			return timepoint;
		}
		// Event does not indicate the filing is deleted
		if ("deleted".equals(companiesHouseFiling.eventType())) {
			LOG.debug("CH filing stream event: Skipped {}, deleted.", companiesHouseFiling.transactionId());
			return timepoint;
		}
		// Category is not explicitly excluded
		if (!companiesHouseClient.filterCategory(companiesHouseFiling.category())) {
			LOG.debug(
					"CH filing stream event: Skipped {}, category \"{}\" excluded.",
					companiesHouseFiling.transactionId(),
					companiesHouseFiling.category()
			);
			return timepoint;
		}
		// Filing has already been indexed
		if (databaseManager.filingExists(RegistryCode.COMPANIES_HOUSE.getCode(), companiesHouseFiling.transactionId())) {
			LOG.debug("CH filing stream event: Skipped {}, filing already exists.", companiesHouseFiling.transactionId());
			return timepoint;
		}
		// Check if an IXBRL document is associated with the filing
		Set<String> filingUrls = companiesHouseClient.getCompanyFilingUrls(
				companiesHouseFiling.companyNumber(),
				companiesHouseFiling.resourceId()
		);
		if (filingUrls.isEmpty()) {
			LOG.debug("CH filing stream event: Skipped {}, no IXBRL documents.", companiesHouseFiling.transactionId());
			return timepoint;
		}

		// Retrieve document date
		LocalDateTime documentDate = companiesHouseFiling.actionDate();
		if (documentDate == null) {
			CompaniesHouseFiling fullFiling = companiesHouseClient.getFiling(
					companiesHouseFiling.companyNumber(),
					companiesHouseFiling.transactionId()
			);
			documentDate = fullFiling.actionDate();
		}

		// Retrieve company name
		CompaniesHouseCompany company = companiesHouseClient.getCompany(companiesHouseFiling.companyNumber());
		String companyName = company.getCompanyName();

		NewFilingRequest newFilingRequest = NewFilingRequest.builder()
				.companyName(companyName)
				.companyNumber(companiesHouseFiling.companyNumber())
				.documentDate(documentDate)
				.downloadUrl(companiesHouseFiling.downloadUrl())
				.externalFilingId(companiesHouseFiling.transactionId())
				.externalViewUrl(companiesHouseFiling.downloadUrl())
				.filingDate(companiesHouseFiling.date())
				.registryCode(RegistryCode.COMPANIES_HOUSE.getCode())
				.streamTimepoint(companiesHouseFiling.timepoint())
				.build();
		UUID filingId = this.databaseManager.createFiling(newFilingRequest);
		LOG.info("Created CH filing for {}: {}", newFilingRequest.getDownloadUrl(), filingId);
		this.companiesHouseSessionFilingCount += 1;
		return timepoint;
	}

	public boolean isHealthy() {
		return companiesHouseStreamLastOpenedDate != null &&
				companiesHouseSessionStartTimepoint != null;
	}

	public void run(Supplier<Boolean> continueCallback) throws IOException {
		if (!continueCallback.get()) {
			return;
		}
		LOG.info("Starting Companies House indexing at {}", System.currentTimeMillis() / 1000);
		Function<CompaniesHouseFiling, Boolean> callback = (CompaniesHouseFiling filing) -> {
			long timepoint;
			try {
				timepoint = handleFilingStreamEvent(filing);
			} catch (JsonProcessingException e) {
				LOG.error("Failed to process filing event.", e);
				return false; // Stop streaming
			}
			if (!continueCallback.get()) {
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
}
