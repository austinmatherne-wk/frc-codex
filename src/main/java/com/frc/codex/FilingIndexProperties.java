package com.frc.codex;

import org.springframework.boot.context.properties.ConfigurationProperties;

public interface FilingIndexProperties {
	public String companiesHouseDocumentApiBaseUrl();
	public String companiesHouseInformationApiBaseUrl();
	public String companiesHouseRestApiKey();
	public String companiesHouseStreamApiBaseUrl();
	public String companiesHouseStreamApiKey();
}

