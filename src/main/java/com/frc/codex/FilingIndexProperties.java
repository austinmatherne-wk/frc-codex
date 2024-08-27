package com.frc.codex;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.zaxxer.hikari.HikariConfig;

public interface FilingIndexProperties {
	String companiesHouseDocumentApiBaseUrl();
	String companiesHouseInformationApiBaseUrl();
	String companiesHouseRestApiKey();
	String companiesHouseStreamApiBaseUrl();
	String companiesHouseStreamApiKey();
	HikariConfig getDatabaseConfig(String poolName);
	boolean isDbMigrateAsync();
}

