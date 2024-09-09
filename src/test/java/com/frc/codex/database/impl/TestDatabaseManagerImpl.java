package com.frc.codex.database.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.frc.codex.RegistryCode;
import com.frc.codex.database.DatabaseManager;
import com.frc.codex.model.Filing;
import com.frc.codex.model.FilingResultRequest;
import com.frc.codex.model.FilingStatus;
import com.frc.codex.model.NewFilingRequest;
import com.google.common.collect.ImmutableList;

@Component
@Profile("test")
public class TestDatabaseManagerImpl implements DatabaseManager {
	private final Map<UUID, Filing> filings;

	public TestDatabaseManagerImpl() {
		this.filings = new HashMap<>();
	}

	public void applyFilingResult(FilingResultRequest filingResultRequest) {
		Filing newFiling = copyFiling(filingResultRequest.getFilingId())
				.status(filingResultRequest.getStatus().toString())
				.error(filingResultRequest.getError())
				.logs(filingResultRequest.getLogs())
				.stubViewerUrl(filingResultRequest.getStubViewerUrl())
				.build();
		updateFiling(newFiling);
	}

	public UUID createFiling(NewFilingRequest newFilingRequest) {
		Filing filing = Filing.builder()
				.filingId(UUID.randomUUID().toString())
				.companyName(newFilingRequest.getCompanyName())
				.companyNumber(newFilingRequest.getCompanyNumber())
				.discoveredDate(Timestamp.from(Instant.now()))
				.status(FilingStatus.PENDING.toString())
				.registryCode(newFilingRequest.getRegistryCode())
				.downloadUrl(newFilingRequest.getDownloadUrl())
				.streamTimepoint(newFilingRequest.getStreamTimepoint())
				.build();
		filings.put(filing.getFilingId(), filing);
		return filing.getFilingId();
	}

	public boolean filingExists(NewFilingRequest newFilingRequest) {
		return filings.values().stream()
				.anyMatch(f -> f.getDownloadUrl().equals(newFilingRequest.getDownloadUrl()));
	}

	public Filing getFiling(UUID filingId) {
		return filings.get(filingId);
	}

	public LocalDateTime getLatestFcaFilingDate(LocalDateTime defaultDate) {
		return filings.values().stream()
				.filter(f -> f.getRegistryCode().equals("FCA"))
				.map(Filing::getFilingDate)
				.max(LocalDateTime::compareTo)
				.orElse(defaultDate);
	}

	public Long getLatestStreamTimepoint(Long defaultTimepoint) {
		return filings.values().stream()
				.mapToLong(Filing::getStreamTimepoint)
				.max()
				.orElse(defaultTimepoint);
	}

	public List<Filing> getFilingsByStatus(FilingStatus status) {
		return filings.values().stream()
				.filter(f -> f.getStatus().equals(status.toString()))
				.collect(ImmutableList.toImmutableList());
	}

	public long getRegistryCount(RegistryCode registryCode) {
		return filings.values().stream()
				.filter(f -> f.getRegistryCode().equals(registryCode.toString()))
				.count();
	}

	private Filing.Builder copyFiling(UUID filingId) {
		Filing filing = getFiling(filingId);
		return Filing.builder()
				.filingId(filing.getFilingId().toString())
				.companyName(filing.getCompanyName())
				.companyNumber(filing.getCompanyNumber())
				.discoveredDate(filing.getDiscoveredDate())
				.status(filing.getStatus())
				.registryCode(filing.getRegistryCode())
				.downloadUrl(filing.getDownloadUrl())
				.streamTimepoint(filing.getStreamTimepoint())
				.error(filing.getError())
				.logs(filing.getLogs());
	}

	private void updateFiling(Filing filing) {
		filings.put(filing.getFilingId(), filing);
	}

	public void updateFilingStatus(UUID filingId, String status) {
		Filing newFiling = copyFiling(filingId)
				.status(status)
				.build();
		updateFiling(newFiling);
	}
}
