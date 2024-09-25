package com.frc.codex.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.frc.codex.FilingIndexProperties;
import com.zaxxer.hikari.HikariConfig;

@Component
@Profile("test")
public class TestFilingIndexPropertiesImpl implements FilingIndexProperties {

	public String awsAccessKeyId() {
		return "AWS_ACCESS_KEY_ID";
	}

	public String awsHost() {
		return "http://localhost:8087/jobs";
	}

	public String awsRegion() {
		return "eu-west-2";
	}

	public String awsSecretAccessKey() {
		return "AWS_SECRET_ACCESS_KEY";
	}

	public String companiesHouseDocumentApiBaseUrl() {
		return "http://localhost:8085";
	}

	public String companiesHouseInformationApiBaseUrl() {
		return "http://localhost:8085";
	}

	public String companiesHouseRestApiKey() {
		return "XXX";
	}

	public String companiesHouseStreamApiBaseUrl() {
		return "http://localhost:8085";
	}

	public String companiesHouseStreamApiKey() {
		return "XXX";
	}

	public String fcaDataApiBaseUrl() {
		return "http://localhost:8086/data";
	}

	public String fcaSearchApiUrl() {
		return "http://localhost:8086/search";
	}

	public HikariConfig getDatabaseConfig(String poolName) {
		HikariConfig config = new HikariConfig();
		config.setInitializationFailTimeout(0);
		config.setAutoCommit(false);
		config.setJdbcUrl("http://localhost:5432/frc_codex");
		config.setUsername("frc_codex");
		config.setPassword("frc_codex");
		config.setMaxLifetime(300);
		config.setPoolName(poolName);
		return config;
	}

	public boolean isDbMigrateAsync() {
		return false;
	}

	public long maximumSearchResults() {
		return 100;
	}

	public long searchPageSize() {
		return 10;
	}

	public String sqsJobsQueueName() {
		return "frc_codex_jobs";
	}

	public String sqsResultsQueueName() {
		return "frc_codex_results";
	}
}
