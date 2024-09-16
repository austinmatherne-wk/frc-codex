package com.frc.codex.database.impl;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.StringUtils;

import com.frc.codex.FilingIndexProperties;
import com.frc.codex.RegistryCode;
import com.frc.codex.database.DatabaseManager;
import com.frc.codex.model.Filing;
import com.frc.codex.model.FilingResultRequest;
import com.frc.codex.model.FilingStatus;
import com.frc.codex.model.NewFilingRequest;
import com.frc.codex.model.SearchFilingsRequest;
import com.google.common.collect.ImmutableList;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.annotation.PostConstruct;

@Component
@Profile("application")
public class DatabaseManagerImpl implements AutoCloseable, DatabaseManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManagerImpl.class);
	public static final Calendar TIMEZONE_UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

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
					"error = ?, " +
					"logs = ?, " +
					"status = ?, " +
					"stub_viewer_url = ?, " +
					"company_name = COALESCE(company_name, ?), " +
					"company_number = COALESCE(company_number, ?), " +
					"document_date = COALESCE(document_date, ?) " +
					"WHERE filing_id = ?";
			PreparedStatement statement = connection.prepareStatement(sql);
			int i = 0;
			statement.setString(++i, filingResultRequest.getError());
			statement.setString(++i, filingResultRequest.getLogs());
			statement.setString(++i, filingResultRequest.getStatus().toString());
			statement.setString(++i, filingResultRequest.getStubViewerUrl());
			statement.setString(++i, filingResultRequest.getCompanyName());
			statement.setString(++i, filingResultRequest.getCompanyNumber());
			statement.setTimestamp(++i, getTimestamp(filingResultRequest.getDocumentDate()), TIMEZONE_UTC);
			statement.setObject(++i, filingResultRequest.getFilingId());

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
					"(status, registry_code, download_url, filing_date, stream_timepoint, company_name, company_number) " +
					"VALUES (?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
			int i = 0;
			statement.setString(++i, FilingStatus.PENDING.toString());
			statement.setString(++i, newFilingRequest.getRegistryCode());
			statement.setString(++i, newFilingRequest.getDownloadUrl());
			statement.setTimestamp(++i, getTimestamp(newFilingRequest.getFilingDate()), TIMEZONE_UTC);
			if (newFilingRequest.getStreamTimepoint() == null) {
				statement.setNull(++i, java.sql.Types.BIGINT);
			} else {
				statement.setLong(++i, newFilingRequest.getStreamTimepoint());
			}
			statement.setString(++i, newFilingRequest.getCompanyName());
			statement.setString(++i, newFilingRequest.getCompanyNumber());

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

	public LocalDateTime getLatestFcaFilingDate(LocalDateTime defaultDate) {
		try (Connection connection = getInitializedConnection(true)) {
			String sql = "SELECT MAX(filing_date) FROM filings WHERE registry_code = 'FCA'";
			PreparedStatement statement = connection.prepareStatement(sql);
			ResultSet resultSet = statement.executeQuery();
			LocalDateTime result = null;
			if (resultSet.next()) {
				result = getLocalDateTime(resultSet.getTimestamp(1));
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

	public List<Filing> getFilingsByStatus(FilingStatus status) {
		try (Connection connection = getInitializedConnection(true)) {
			String sql = "SELECT * FROM filings WHERE status = ?";
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, status.toString());
			ResultSet resultSet = statement.executeQuery();
			return getFilings(resultSet);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public long getRegistryCount(RegistryCode registryCode) {
		try (Connection connection = getInitializedConnection(true)) {
			String sql = "SELECT COUNT(*) FROM filings WHERE registry_code = ?";
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, registryCode.toString());
			ResultSet resultSet = statement.executeQuery();
			resultSet.next();
			return resultSet.getLong(1);
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

	private LocalDateTime getLocalDateTime(Timestamp timestamp) {
		if (timestamp == null) {
			return null;
		}
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp.getTime()), ZoneOffset.UTC);
	}

	private Timestamp getTimestamp(LocalDateTime localDateTime) {
		if (localDateTime == null) {
			return null;
		}
		return new Timestamp(localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli());
	}

	public List<Filing> getFilings(ResultSet resultSet) throws SQLException {
		ImmutableList.Builder<Filing> results = ImmutableList.builder();
		while (resultSet.next()) {
			results.add(Filing.builder()
					.filingId(resultSet.getString("filing_id"))
					.discoveredDate(resultSet.getTimestamp("discovered_date"))
					.status(resultSet.getString("status"))
					.registryCode(resultSet.getString("registry_code"))
					.downloadUrl(resultSet.getString("download_url"))
					.companyNumber(resultSet.getString("company_number"))
					.companyName(resultSet.getString("company_name"))
					.filename(resultSet.getString("filename"))
					.filingType(resultSet.getString("filing_type"))
					.filingDate(getLocalDateTime(
							resultSet.getTimestamp("filing_date", TIMEZONE_UTC)
					))
					.documentDate(getLocalDateTime(
							resultSet.getTimestamp("document_date", TIMEZONE_UTC)
					))
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

	public List<Filing> searchFilings(SearchFilingsRequest searchFilingsRequest) {
		try (Connection connection = getInitializedConnection(true)) {
			List<String> selects = new ArrayList<>();
			List<String> queries = new ArrayList<>();
			List<String> conditions = new ArrayList<>();
			List<Object> parameters = new ArrayList<>();
			List<String> orderBys = new ArrayList<>() {
				{
					add("filing_date DESC");
					add("filing_id");
				}
			};
			if (!StringUtils.isEmpty(searchFilingsRequest.getCompanyName())) {
				selects.add("ts_rank(to_tsvector('english', company_name), query) as rank");
				queries.add("websearch_to_tsquery(?) query");
				conditions.add("query @@ to_tsvector('english', company_name)");
				parameters.add(searchFilingsRequest.getCompanyName());
				orderBys.add(0, "rank DESC");
			}
			if (!StringUtils.isEmpty(searchFilingsRequest.getCompanyNumber())) {
				conditions.add("company_number = ?");
				parameters.add(searchFilingsRequest.getCompanyNumber());
			}
			if (!StringUtils.isEmpty(searchFilingsRequest.getStatus())) {
				conditions.add("status = ?");
				parameters.add(searchFilingsRequest.getStatus());
			}
			StringBuilder sqlBuilder = new StringBuilder("SELECT * ");
			if (selects.size() > 0) {
				sqlBuilder.append(", ");
				sqlBuilder.append(String.join(", ", selects));
			}
			sqlBuilder.append(" FROM filings ");
			if (queries.size() > 0) {
				sqlBuilder.append(", ");
				sqlBuilder.append(String.join(", ", queries));
			}
			if (conditions.size() > 0) {
				sqlBuilder.append(" WHERE ");
				sqlBuilder.append(String.join(" AND ", conditions));
			}
			sqlBuilder.append(" ORDER BY ");
			sqlBuilder.append(String.join(", ", orderBys));
			sqlBuilder.append(" LIMIT ?;");
			parameters.add(Math.max(1, searchFilingsRequest.getLimit()));
			String sql = sqlBuilder.toString();
			PreparedStatement statement = connection.prepareStatement(sql);
			for (int i = 0; i < parameters.size(); i++) {
				statement.setObject(i + 1, parameters.get(i));
			}
			ResultSet resultSet = statement.executeQuery();
			return getFilings(resultSet);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}