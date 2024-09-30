package com.frc.codex;

import com.zaxxer.hikari.HikariConfig;

public interface FilingIndexProperties {
	String awsRegion();
	String companiesHouseDocumentApiBaseUrl();
	String companiesHouseInformationApiBaseUrl();
	String companiesHouseRestApiKey();
	String companiesHouseStreamApiBaseUrl();
	String companiesHouseStreamApiKey();
	String fcaDataApiBaseUrl();
	String fcaSearchApiUrl();
	HikariConfig getDatabaseConfig(String poolName);
	boolean isDbMigrateAsync();
	long maximumSearchResults();
	String s3ResultsBucketName();
	long searchPageSize();
	String sqsJobsQueueName();
	String sqsResultsQueueName();
}
