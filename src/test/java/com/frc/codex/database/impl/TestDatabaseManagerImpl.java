package com.frc.codex.database.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.frc.codex.database.DatabaseManager;
import com.frc.codex.model.Filing;
import com.frc.codex.model.NewFilingRequest;
import com.google.common.collect.ImmutableList;

@Component
@Profile("test")
public class TestDatabaseManagerImpl implements DatabaseManager {
	private final Map<UUID, Filing> filings;

	public TestDatabaseManagerImpl() {
		this.filings = new HashMap<>();
	}

	public UUID createFiling(NewFilingRequest newFilingRequest) {
		Filing filing = Filing.builder()
				.filingId(UUID.randomUUID().toString())
				.discoveredDate(new Date())
				.status("pending")
				.registryCode(newFilingRequest.getRegistryCode())
				.downloadUrl(newFilingRequest.getDownloadUrl())
				.streamTimepoint(newFilingRequest.getStreamTimepoint())
				.build();
		filings.put(filing.getFilingId(), filing);
		return filing.getFilingId();
	}

	public Filing getFiling(UUID filingId) {
		return filings.get(filingId);
	}

	public List<Filing> listFilings() {
		return ImmutableList.copyOf(filings.values());
	}
}
