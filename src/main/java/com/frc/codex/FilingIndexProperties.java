package com.frc.codex;

import com.zaxxer.hikari.HikariConfig;

public interface FilingIndexProperties {
	String awsRegion();
	String companiesHouseDocumentApiBaseUrl();
	String companiesHouseInformationApiBaseUrl();
	int companiesHouseRapidRateLimit();
	int companiesHouseRapidRateWindow();
	String companiesHouseRestApiKey();
	String companiesHouseStreamApiBaseUrl();
	String companiesHouseStreamApiKey();
	String fcaDataApiBaseUrl();
	String fcaSearchApiUrl();
	boolean enablePreprocessing();
	int filingLimitCompaniesHouse();
	int filingLimitFca();
	HikariConfig getDatabaseConfig(String poolName);
	boolean isAws();
	boolean isDbMigrateAsync();
	long maximumSearchResults();
	String s3ResultsBucketName();
	long searchPageSize();
	String sqsJobsQueueName();
	String sqsResultsQueueName();
}
