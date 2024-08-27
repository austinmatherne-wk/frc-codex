package com.frc.codex.discovery.companieshouse.impl;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.frc.codex.FilingIndexProperties;
import com.frc.codex.discovery.companieshouse.CompaniesHouseConfig;

@Component
public class CompaniesHouseConfigImpl implements CompaniesHouseConfig {

	private final String documentApiBaseUrl;
	private final String informationApiBaseUrl;
	private final String restApiKey;
	private final String streamApiBaseUrl;
	private final String streamApiKey;

	public CompaniesHouseConfigImpl(FilingIndexProperties properties) {
		requireNonNull(properties, "properties is required");
		this.documentApiBaseUrl = Objects.toString(properties.companiesHouseDocumentApiBaseUrl(), "");
		this.informationApiBaseUrl = Objects.toString(properties.companiesHouseInformationApiBaseUrl(), "");
		this.restApiKey = Objects.toString(properties.companiesHouseRestApiKey(), "");
		this.streamApiBaseUrl = Objects.toString(properties.companiesHouseStreamApiBaseUrl(), "");
		this.streamApiKey = Objects.toString(properties.companiesHouseStreamApiKey(), "");
	}

	@Override
	public String documentApiBaseUrl() {
		return this.documentApiBaseUrl;
	}

	@Override
	public String informationApiBaseUrl() {
		return this.informationApiBaseUrl;
	}

	@Override
	public String restApiKey() {
		return this.restApiKey;
	}

	@Override
	public String streamApiBaseUrl() {
		return this.streamApiBaseUrl;
	}

	@Override
	public String streamApiKey() {
		return this.streamApiKey;
	}
}
