package com.frc.codex.database.impl;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.frc.codex.FilingIndexProperties;
import com.frc.codex.database.DatabaseManager;
import com.frc.codex.model.Filing;
import com.frc.codex.model.NewFilingRequest;
import com.google.common.collect.ImmutableList;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.annotation.PostConstruct;

@Component
@Profile("application")
public class DatabaseManagerImpl implements AutoCloseable, DatabaseManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManagerImpl.class);
	private final boolean dbMigrateAsync;
	private final DataSource readDataSource;
	private final DataSource writeDataSource;

	public DatabaseManagerImpl(FilingIndexProperties properties) {
		this.dbMigrateAsync = properties.isDbMigrateAsync();
		this.readDataSource = new HikariDataSource(properties.getDatabaseConfig("read"));
		this.writeDataSource = new HikariDataSource(properties.getDatabaseConfig("write"));
	}

	public UUID createFiling(NewFilingRequest newFilingRequest) {
		UUID filingId;
		try (Connection connection = getInitializedConnection(false)) {
			PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO filings (status, registry_code, download_url, stream_timepoint) " +
					"VALUES ('pending', ?, ?, ?)",
					PreparedStatement.RETURN_GENERATED_KEYS
			);
			statement.setString(1, newFilingRequest.getRegistryCode());
			statement.setString(2, newFilingRequest.getDownloadUrl());
			if (newFilingRequest.getStreamTimepoint() == null) {
				statement.setNull(3, java.sql.Types.BIGINT);
			} else {
				statement.setLong(3, newFilingRequest.getStreamTimepoint());
			}

			int affectedRows = statement.executeUpdate();
			if (affectedRows == 0) {
				throw new SQLException("Creating filing failed, no rows affected.");
			}
			connection.commit();
			try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					filingId = UUID.fromString(generatedKeys.getString("filing_id"));
				}
				else {
					throw new SQLException("Creating filing failed, no ID obtained.");
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return filingId;
	}

	public Filing getFiling(UUID filingId) {
		try (Connection connection = getInitializedConnection(true)) {
			PreparedStatement statement = connection.prepareStatement(
					"SELECT * FROM filings WHERE filing_id = ?"
			);
			statement.setObject(1, filingId);
			ResultSet resultSet = statement.executeQuery();
			List<Filing> filings = getFilings(resultSet);
			if (filings.isEmpty()) {
				return null;
			}
			return filings.get(0);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@PostConstruct
	public void postConstruct() {
		migrate();
	}

	private Connection getInitializedConnection(boolean readOnly) throws SQLException {
		DataSource dataSource = readOnly ? readDataSource : writeDataSource;
		return dataSource.getConnection();
	}

	/*
	 * List all `filings` records in the database.
	 */
	public List<Filing> listFilings() {
		try (Connection connection = getInitializedConnection(true)) {
			PreparedStatement statement = connection.prepareStatement("SELECT * FROM filings");
			ResultSet resultSet = statement.executeQuery();
			return getFilings(resultSet);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<Filing> getFilings(ResultSet resultSet) throws SQLException {
		ImmutableList.Builder<Filing> results = ImmutableList.builder();
		while (resultSet.next()) {
			results.add(Filing.builder()
					.filingId(resultSet.getString("filing_id"))
					.discoveredDate(resultSet.getDate("discovered_date"))
					.status(resultSet.getString("status"))
					.registryCode(resultSet.getString("registry_code"))
					.downloadUrl(resultSet.getString("download_url"))
					.companyNumber(resultSet.getString("company_number"))
					.companyName(resultSet.getString("company_name"))
					.lei(resultSet.getString("lei"))
					.filename(resultSet.getString("filename"))
					.filingType(resultSet.getString("filing_type"))
					.filingDate(resultSet.getDate("filing_date"))
					.documentDate(resultSet.getDate("document_date"))
					.streamTimepoint(resultSet.getLong("stream_timepoint"))
					.stubViewerUrl(resultSet.getString("stub_viewer_url"))
					.oimCsvUrl(resultSet.getString("oim_csv_url"))
					.oimJsonUrl(resultSet.getString("oim_json_url"))
					.build());
		}
		return results.build();
	}

	public void migrate() {
		if (dbMigrateAsync) {
			new Thread(this::migrateImpl, "db-migrate").start();
		} else {
			migrateImpl();
		}
	}

	private void migrateImpl() {
		LOGGER.info("Starting database migrations.");
		Flyway flyway = Flyway.configure()
				.dataSource(writeDataSource)
				.placeholders(getMigratePlaceholders())
				.load();
		flyway.migrate();
		LOGGER.info("Finished database migrations.");
	}

	/**
	 * Create a map of placeholder variables that are used to substitute SQL dialog
	 * specifics into the flyway migration scripts.
	 */
	private Map<String, String> getMigratePlaceholders() {
		Map<String, String> migratePlaceholders = new HashMap<>();
		return migratePlaceholders;
	}

	@Override
	public void close() throws Exception {
		try (AutoCloseable closeWriteDataSource = ((Closeable) writeDataSource)) {
			((Closeable) readDataSource).close();
		}
	}
}
