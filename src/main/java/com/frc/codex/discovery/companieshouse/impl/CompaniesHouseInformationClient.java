package com.frc.codex.discovery.companieshouse.impl;

import com.frc.codex.discovery.companieshouse.CompaniesHouseRateLimiter;

public class CompaniesHouseInformationClient {
	private final CompaniesHouseHttpClient httpClient;

	public CompaniesHouseInformationClient(
		CompaniesHouseRateLimiter rateLimiter,
		String baseUrl,
		String apiKey
	) {
		this.httpClient = new CompaniesHouseHttpClient(
				rateLimiter,
				baseUrl,
				apiKey
		);
	}
	public String getCompany(String companyNumber) {
		return httpClient.get("/company/" + companyNumber);
	}

	public String getCompanyFiling(String companyNumber, String filingId) {
		return httpClient.get("/company/" + companyNumber + "/filing-history/" + filingId);
	}

	public String getCompanyFilingHistory(String companyNumber) {
		return httpClient.get("/company/" + companyNumber + "/filing-history");
	}
}
