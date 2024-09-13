package com.frc.codex.discovery.companieshouse.impl;

import java.net.URI;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.frc.codex.discovery.companieshouse.CompaniesHouseHistoryClient;
@Component
@Profile("test")
public class TestCompaniesHouseHistoryClientImpl implements CompaniesHouseHistoryClient {

	@Override
	public List<URI> getArchiveDownloadLinks() {
		return List.of();
	}

	@Override
	public List<URI> getDailyDownloadLinks() {
		return List.of();
	}

	@Override
	public List<URI> getMonthlyDownloadLinks() {
		return List.of();
	}
}
