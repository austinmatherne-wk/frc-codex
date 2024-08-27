package com.frc.codex.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.frc.codex.FilingIndexProperties;
import com.zaxxer.hikari.HikariConfig;

@Component
@Profile("test")
public class TestFilingIndexPropertiesImpl implements FilingIndexProperties {

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

}
