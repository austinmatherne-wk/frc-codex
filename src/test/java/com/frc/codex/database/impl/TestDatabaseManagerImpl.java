package com.frc.codex.database.impl;

import java.util.Date;
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
				.stubViewerUrl(filingResultRequest.getStubViewerUrl())
				.build();
		updateFiling(newFiling);
	}

	public UUID createFiling(NewFilingRequest newFilingRequest) {
		Filing filing = Filing.builder()
				.filingId(UUID.randomUUID().toString())
				.discoveredDate(new Date())
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

	public Date getLatestFcaFilingDate(Date defaultDate) {
		return filings.values().stream()
				.filter(f -> f.getRegistryCode().equals("FCA"))
				.map(Filing::getFilingDate)
				.max(Date::compareTo)
				.orElse(defaultDate);
	}

	public Long getLatestStreamTimepoint(Long defaultTimepoint) {
		return filings.values().stream()
				.mapToLong(Filing::getStreamTimepoint)
				.max()
				.orElse(defaultTimepoint);
	}

	public List<Filing> getPendingFilings() {
		return filings.values().stream()
				.filter(f -> f.getStatus().equals(FilingStatus.PENDING.toString()))
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
				.discoveredDate(filing.getDiscoveredDate())
				.status(filing.getStatus())
				.registryCode(filing.getRegistryCode())
				.downloadUrl(filing.getDownloadUrl())
				.streamTimepoint(filing.getStreamTimepoint());
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
