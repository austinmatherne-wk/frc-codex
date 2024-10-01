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
	private static final String AWS_REGION = "AWS_REGION";
	private static final String COMPANIES_HOUSE_DOCUMENT_API_BASE_URL = "COMPANIES_HOUSE_DOCUMENT_API_BASE_URL";
	private static final String COMPANIES_HOUSE_INFORMATION_API_BASE_URL = "COMPANIES_HOUSE_INFORMATION_API_BASE_URL";
	private static final String COMPANIES_HOUSE_RAPID_RATE_LIMIT = "COMPANIES_HOUSE_RAPID_RATE_LIMIT";
	private static final String COMPANIES_HOUSE_RAPID_RATE_WINDOW = "COMPANIES_HOUSE_RAPID_RATE_WINDOW";
	private static final String COMPANIES_HOUSE_REST_API_KEY = "COMPANIES_HOUSE_REST_API_KEY";
	private static final String COMPANIES_HOUSE_STREAM_API_BASE_URL = "COMPANIES_HOUSE_STREAM_API_BASE_URL";
	private static final String COMPANIES_HOUSE_STREAM_API_KEY = "COMPANIES_HOUSE_STREAM_API_KEY";
	private static final String DB_URL = "DB_URL";
	private static final String DB_USERNAME = "DB_USERNAME";
	private static final String DB_PASSWORD = "DB_PASSWORD";
	private static final String DB_MAX_LIFETIME = "DB_MAX_LIFETIME";
	private static final String FCA_DATA_API_BASE_URL = "FCA_DATA_API_BASE_URL";
	private static final String FCA_SEARCH_API_URL = "FCA_SEARCH_API_URL";
	private static final String MAXIMUM_SEARCH_RESULTS = "MAXIMUM_SEARCH_RESULTS";
	private static final String S3_RESULTS_BUCKET_NAME = "S3_RESULTS_BUCKET_NAME";
	private static final String SEARCH_PAGE_SIZE = "SEARCH_PAGE_SIZE";
	private static final String SECRETS_FILEPATH = "/run/secrets/frc-codex-server.secrets";
	private static final String SQS_JOBS_QUEUE_NAME = "SQS_JOBS_QUEUE_NAME";
	private static final String SQS_RESULTS_QUEUE_NAME = "SQS_RESULTS_QUEUE_NAME";
	private final String companiesHouseDocumentApiBaseUrl;
	private final String companiesHouseInformationApiBaseUrl;
	private final int companiesHouseRapidRateLimit;
	private final int companiesHouseRapidRateWindow;
	private final String companiesHouseRestApiKey;
	private final String companiesHouseStreamApiBaseUrl;
	private final String companiesHouseStreamApiKey;
	private final String dbUrl;
	private final String dbUsername;
	private final String dbPassword;
	private final long dbMaxLifetime;
	private final String fcaDataApiBaseUrl;
	private final String fcaSearchApiUrl;
	private final String awsRegion;
	private final long maximumSearchResults;
	private final String s3ResultsBucketName;
	private final long searchPageSize;
	private final String sqsJobsQueueName;
	private final String sqsResultsQueueName;


	public FilingIndexPropertiesImpl() {
		awsRegion = requireNonNull(getEnv(AWS_REGION));

		companiesHouseDocumentApiBaseUrl = requireNonNull(getEnv(COMPANIES_HOUSE_DOCUMENT_API_BASE_URL));
		companiesHouseInformationApiBaseUrl = requireNonNull(getEnv(COMPANIES_HOUSE_INFORMATION_API_BASE_URL));
		companiesHouseStreamApiBaseUrl = requireNonNull(getEnv(COMPANIES_HOUSE_STREAM_API_BASE_URL));
		// Default rapid rate limit is 20 requests per 10 seconds (600 requests / 5 minutes)
		companiesHouseRapidRateLimit = Integer.parseInt(requireNonNull(getEnv(COMPANIES_HOUSE_RAPID_RATE_LIMIT, "20")));
		companiesHouseRapidRateWindow = Integer.parseInt(requireNonNull(getEnv(COMPANIES_HOUSE_RAPID_RATE_WINDOW, "10000")));

		dbUrl = requireNonNull(getEnv(DB_URL));
		dbUsername = requireNonNull(getEnv(DB_USERNAME));
		dbPassword = requireNonNull(getEnv(DB_PASSWORD));
		dbMaxLifetime = Long.parseLong(requireNonNull(getEnv(DB_MAX_LIFETIME, "300")));

		fcaDataApiBaseUrl = requireNonNull(getEnv(FCA_DATA_API_BASE_URL));
		fcaSearchApiUrl = requireNonNull(getEnv(FCA_SEARCH_API_URL));

		maximumSearchResults = Long.parseLong(requireNonNull(getEnv(MAXIMUM_SEARCH_RESULTS, "100")));
		searchPageSize = Long.parseLong(requireNonNull(getEnv(SEARCH_PAGE_SIZE, "10")));

		s3ResultsBucketName = requireNonNull(getEnv(S3_RESULTS_BUCKET_NAME));
		sqsJobsQueueName = requireNonNull(getEnv(SQS_JOBS_QUEUE_NAME));
		sqsResultsQueueName = requireNonNull(getEnv(SQS_RESULTS_QUEUE_NAME));

		Properties secrets = getSecrets();
		if (secrets.containsKey(COMPANIES_HOUSE_REST_API_KEY)) {
			companiesHouseRestApiKey = requireNonNull(secrets.getProperty(COMPANIES_HOUSE_REST_API_KEY));
		} else {
			companiesHouseRestApiKey = requireNonNull(getEnv(COMPANIES_HOUSE_REST_API_KEY));
		}
		if (secrets.containsKey(COMPANIES_HOUSE_STREAM_API_KEY)) {
			companiesHouseStreamApiKey = requireNonNull(secrets.getProperty(COMPANIES_HOUSE_STREAM_API_KEY));
		} else {
			companiesHouseStreamApiKey = requireNonNull(getEnv(COMPANIES_HOUSE_STREAM_API_KEY));
		}
	}

	private String getEnv(String name) {
		return System.getenv(name);
	}

	private String getEnv(String name, String defaultValue) {
		String value = getEnv(name);
		if (value == null) {
			return defaultValue;
		}
		return value;
	}

	private Properties getSecrets() {
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

	public String awsRegion() {
		return awsRegion;
	}

	public String companiesHouseDocumentApiBaseUrl() {
		return companiesHouseDocumentApiBaseUrl;
	}

	public String companiesHouseInformationApiBaseUrl() {
		return companiesHouseInformationApiBaseUrl;
	}

	public int companiesHouseRapidRateLimit() {
		return companiesHouseRapidRateLimit;
	}

	public int companiesHouseRapidRateWindow() {
		return companiesHouseRapidRateWindow;
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

	public String fcaDataApiBaseUrl() {
		return fcaDataApiBaseUrl;
	}

	public String fcaSearchApiUrl() {
		return fcaSearchApiUrl;
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

	public long maximumSearchResults() {
		return maximumSearchResults;
	}

	public String s3ResultsBucketName() {
		return s3ResultsBucketName;
	}

	public long searchPageSize() {
		return searchPageSize;
	}

	public String sqsJobsQueueName() {
		return sqsJobsQueueName;
	}

	public String sqsResultsQueueName() {
		return sqsResultsQueueName;
	}
}
