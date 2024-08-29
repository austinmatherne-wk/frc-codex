package com.frc.codex.database.impl;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
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
import com.frc.codex.model.FilingResultRequest;
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

	public void applyFilingResult(FilingResultRequest filingResultRequest) {
		try (Connection connection = getInitializedConnection(false)) {
			String sql = "UPDATE filings SET " +
					"status = ?, " +
					"stub_viewer_url = ? " +
					"WHERE filing_id = ?";
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, "completed");
			statement.setString(2, filingResultRequest.getStubViewerUrl());
			statement.setObject(3, filingResultRequest.getFilingId());

			int affectedRows = statement.executeUpdate();
			if (affectedRows == 0) {
				throw new SQLException("Updating filing result failed, no rows affected.");
			}
			connection.commit();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public UUID createFiling(NewFilingRequest newFilingRequest) {
		UUID filingId;
		try (Connection connection = getInitializedConnection(false)) {
			String sql = "INSERT INTO filings " +
					"(status, registry_code, download_url, stream_timepoint) " +
					"VALUES (?, ?, ?, ?)";
			PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
			statement.setString(1, "pending");
			statement.setString(2, newFilingRequest.getRegistryCode());
			statement.setString(3, newFilingRequest.getDownloadUrl());
			if (newFilingRequest.getStreamTimepoint() == null) {
				statement.setNull(4, java.sql.Types.BIGINT);
			} else {
				statement.setLong(4, newFilingRequest.getStreamTimepoint());
			}

			int affectedRows = statement.executeUpdate();
			if (affectedRows == 0) {
				throw new SQLException("Creating filing failed, no rows affected.");
			}
			connection.commit();
			try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					filingId = UUID.fromString(generatedKeys.getString("filing_id"));
				} else {
					throw new SQLException("Creating filing failed, no ID obtained.");
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return filingId;
	}

	public boolean filingExists(NewFilingRequest newFilingRequest) {
		try (Connection connection = getInitializedConnection(true)) {
			String sql = "SELECT filing_id FROM filings " +
					"WHERE download_url = ? " +
					"LIMIT 1";
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setObject(1, newFilingRequest.getDownloadUrl());
			ResultSet resultSet = statement.executeQuery();
			return resultSet.next();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Filing getFiling(UUID filingId) {
		try (Connection connection = getInitializedConnection(true)) {
			String sql = "SELECT * FROM filings WHERE filing_id = ?";
			PreparedStatement statement = connection.prepareStatement(sql);
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

	public Date getLatestFcaFilingDate(Date defaultDate) {
		try (Connection connection = getInitializedConnection(true)) {
			String sql = "SELECT MAX(filing_date) FROM filings WHERE registry_code = 'FCA'";
			PreparedStatement statement = connection.prepareStatement(sql);
			ResultSet resultSet = statement.executeQuery();
			Date result = null;
			if (resultSet.next()) {
				result = resultSet.getDate(1);
			}
			if (result == null) {
				return defaultDate;
			}
			return result;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Long getLatestStreamTimepoint(Long defaultTimepoint) {
		try (Connection connection = getInitializedConnection(true)) {
			PreparedStatement statement = connection.prepareStatement(
					"SELECT MAX(stream_timepoint) FROM filings"
			);
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				return resultSet.getLong(1);
			}
			return defaultTimepoint;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<Filing> getPendingFilings() {
		try (Connection connection = getInitializedConnection(true)) {
			String sql = "SELECT * FROM filings WHERE status = 'pending'";
			PreparedStatement statement = connection.prepareStatement(sql);
			ResultSet resultSet = statement.executeQuery();
			return getFilings(resultSet);
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

	public void updateFilingStatus(UUID filingId, String status) {
		try (Connection connection = getInitializedConnection(false)) {
			String sql = "UPDATE filings SET status = ? WHERE filing_id = ?";
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, status);
			statement.setObject(2, filingId);

			int affectedRows = statement.executeUpdate();
			if (affectedRows == 0) {
				throw new SQLException("Updating filing status failed, no rows affected.");
			}
			connection.commit();
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
				.load();
		flyway.migrate();
		LOGGER.info("Finished database migrations.");
	}

	public void close() throws Exception {
		try (AutoCloseable closeWriteDataSource = ((Closeable) writeDataSource)) {
			((Closeable) readDataSource).close();
		}
	}
}
