package com.frc.codex.impl;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.frc.codex.FilingIndexProperties;
import com.zaxxer.hikari.HikariConfig;

@Component
@Profile("application")
public class FilingIndexPropertiesImpl implements FilingIndexProperties {
	private static final String COMPANIES_HOUSE_DOCUMENT_API_BASE_URL = "COMPANIES_HOUSE_DOCUMENT_API_BASE_URL";
	private static final String COMPANIES_HOUSE_INFORMATION_API_BASE_URL = "COMPANIES_HOUSE_INFORMATION_API_BASE_URL";
	private static final String COMPANIES_HOUSE_REST_API_KEY = "COMPANIES_HOUSE_REST_API_KEY";
	private static final String COMPANIES_HOUSE_STREAM_API_BASE_URL = "COMPANIES_HOUSE_STREAM_API_BASE_URL";
	private static final String COMPANIES_HOUSE_STREAM_API_KEY = "COMPANIES_HOUSE_STREAM_API_KEY";
	private static final String DB_URL = "DB_URL";
	private static final String DB_USERNAME = "DB_USERNAME";
	private static final String DB_PASSWORD = "DB_PASSWORD";
	private static final String DB_MAX_LIFETIME = "DB_MAX_LIFETIME";
	private static final String SECRETS_FILEPATH = "/run/secrets/frc-codex-server.secrets";
	private final String companiesHouseDocumentApiBaseUrl;
	private final String companiesHouseInformationApiBaseUrl;
	private final String companiesHouseRestApiKey;
	private final String companiesHouseStreamApiBaseUrl;
	private final String companiesHouseStreamApiKey;
	private final String dbUrl;
	private final String dbUsername;
	private final String dbPassword;
	private final long dbMaxLifetime;

	public FilingIndexPropertiesImpl() {
		companiesHouseDocumentApiBaseUrl = requireNonNull(System.getenv(COMPANIES_HOUSE_DOCUMENT_API_BASE_URL));
		companiesHouseInformationApiBaseUrl = requireNonNull(System.getenv(COMPANIES_HOUSE_INFORMATION_API_BASE_URL));
		companiesHouseStreamApiBaseUrl = requireNonNull(System.getenv(COMPANIES_HOUSE_STREAM_API_BASE_URL));

		Properties secrets = getSecrets();
		if (secrets.containsKey(COMPANIES_HOUSE_REST_API_KEY)) {
			companiesHouseRestApiKey = requireNonNull(secrets.getProperty(COMPANIES_HOUSE_REST_API_KEY));
		} else {
			companiesHouseRestApiKey = requireNonNull(System.getenv(COMPANIES_HOUSE_REST_API_KEY));
		}
		if (secrets.containsKey(COMPANIES_HOUSE_STREAM_API_KEY)) {
			companiesHouseStreamApiKey = requireNonNull(secrets.getProperty(COMPANIES_HOUSE_STREAM_API_KEY));
		} else {
			companiesHouseStreamApiKey = requireNonNull(System.getenv(COMPANIES_HOUSE_STREAM_API_KEY));
		}

		dbUrl = requireNonNull(System.getenv(DB_URL));
		dbUsername = requireNonNull(System.getenv(DB_USERNAME));
		dbPassword = requireNonNull(System.getenv(DB_PASSWORD));
		dbMaxLifetime = Long.parseLong(requireNonNull(System.getenv(DB_MAX_LIFETIME)));
	}

	private static Properties getSecrets() {
		try {
			Properties properties = new Properties();
			File file = new File(SECRETS_FILEPATH);
			if (!file.exists() || !file.isFile()) {
				return properties;
			}
			FileInputStream fileInputStream = new FileInputStream(file);
			properties.load(fileInputStream);
			fileInputStream.close();
			return properties;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String companiesHouseDocumentApiBaseUrl() {
		return companiesHouseDocumentApiBaseUrl;
	}

	public String companiesHouseInformationApiBaseUrl() {
		return companiesHouseInformationApiBaseUrl;
	}

	public String companiesHouseRestApiKey() {
		return companiesHouseRestApiKey;
	}

	public String companiesHouseStreamApiBaseUrl() {
		return companiesHouseStreamApiBaseUrl;
	}

	public String companiesHouseStreamApiKey() {
		return companiesHouseStreamApiKey;
	}

	public HikariConfig getDatabaseConfig(String poolName) {
		HikariConfig config = new HikariConfig();
		config.setInitializationFailTimeout(0);
		// recommended is true but is turned off for rollback ability on error
		config.setAutoCommit(false);
		config.setJdbcUrl(dbUrl);
		config.setUsername(dbUsername);
		config.setPassword(dbPassword);
		config.setMaxLifetime(dbMaxLifetime);
		config.setPoolName(poolName);
		return config;
	}

	public boolean isDbMigrateAsync() {
		return false;
	}

}
