package com.frc.codex.discovery.companieshouse.impl;

import com.frc.codex.discovery.companieshouse.CompaniesHouseRateLimiter;

public class CompaniesHouseDocumentClient {
	private final CompaniesHouseHttpClient httpClient;

	public CompaniesHouseDocumentClient(
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

	public String getMetadata(String documentId) {
		return httpClient.get("/document/" + documentId);
	}
}
