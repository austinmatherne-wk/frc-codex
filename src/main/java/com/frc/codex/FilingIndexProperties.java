package com.frc.codex;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("frc")
public record FilingIndexProperties(
	String companiesHouseDocumentApiBaseUrl,
	String companiesHouseInformationApiBaseUrl,
	String companiesHouseRestApiKey,
	String companiesHouseStreamApiBaseUrl,
	String companiesHouseStreamApiKey
) { }
