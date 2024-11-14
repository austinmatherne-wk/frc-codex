package com.frc.codex.clients.companieshouse;

public interface CompaniesHouseConfig {
	String documentApiBaseUrl();
	String informationApiBaseUrl();
	int rapidRateLimit();
	int rapidRateWindow();
	String restApiKey();
	String streamApiBaseUrl();
	String streamApiKey();
}
