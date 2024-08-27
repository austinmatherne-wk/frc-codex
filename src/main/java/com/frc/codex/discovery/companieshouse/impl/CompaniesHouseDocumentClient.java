package com.frc.codex.discovery.companieshouse.impl;

import static java.util.Objects.requireNonNull;

import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class CompaniesHouseDocumentClient {
	private final String baseUrl;
	private final RestTemplate restTemplate;
	private final HttpEntity<String> getJsonEntity;

	public CompaniesHouseDocumentClient(String baseUrl, String apiKey) {
		this.baseUrl = requireNonNull(baseUrl);
		this.restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		// https://developer-specs.company-information.service.gov.uk/guides/authorisation
		headers.setBasicAuth(apiKey, "");
		this.getJsonEntity = new HttpEntity<>("parameters", headers);
	}

	private String get(String url) {
		ResponseEntity<String> response = restTemplate.exchange(
				baseUrl + url,
				HttpMethod.GET,
				this.getJsonEntity,
				String.class
		);
		return response.getBody();
	}

	public String getMetadata(String documentId) {
		return get("/document/" + documentId);
	}
}
