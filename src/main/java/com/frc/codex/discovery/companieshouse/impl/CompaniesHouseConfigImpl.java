package com.frc.codex.discovery.companieshouse.impl;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.frc.codex.FilingIndexProperties;
import com.frc.codex.discovery.companieshouse.CompaniesHouseConfig;

@Component
public class CompaniesHouseConfigImpl implements CompaniesHouseConfig {

	private final String documentApiBaseUrl;
	private final String informationApiBaseUrl;
	private final int rapidRateLimit;
	private final int rapidRateWindow;
	private final String restApiKey;
	private final String streamApiBaseUrl;
	private final String streamApiKey;

	public CompaniesHouseConfigImpl(FilingIndexProperties properties) {
		requireNonNull(properties, "properties is required");
		this.documentApiBaseUrl = Objects.toString(properties.companiesHouseDocumentApiBaseUrl(), "");
		this.informationApiBaseUrl = Objects.toString(properties.companiesHouseInformationApiBaseUrl(), "");
		this.rapidRateLimit = properties.companiesHouseRapidRateLimit();
		this.rapidRateWindow = properties.companiesHouseRapidRateWindow();
		this.restApiKey = properties.companiesHouseRestApiKey();
		this.streamApiBaseUrl = Objects.toString(properties.companiesHouseStreamApiBaseUrl(), "");
		this.streamApiKey = properties.companiesHouseStreamApiKey();
	}

	public String documentApiBaseUrl() {
		return this.documentApiBaseUrl;
	}

	public String informationApiBaseUrl() {
		return this.informationApiBaseUrl;
	}

	public int rapidRateLimit() {
		return this.rapidRateLimit;
	}

	public int rapidRateWindow() {
		return this.rapidRateWindow;
	}

	public String restApiKey() {
		return this.restApiKey;
	}

	public String streamApiBaseUrl() {
		return this.streamApiBaseUrl;
	}

	public String streamApiKey() {
		return this.streamApiKey;
	}
}
