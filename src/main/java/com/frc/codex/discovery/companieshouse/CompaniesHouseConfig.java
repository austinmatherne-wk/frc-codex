package com.frc.codex.discovery.companieshouse;

public interface CompaniesHouseConfig {
	String documentApiBaseUrl();
	String informationApiBaseUrl();
	int rapidRateLimit();
	int rapidRateWindow();
	String restApiKey();
	String streamApiBaseUrl();
	String streamApiKey();
}
