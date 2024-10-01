package com.frc.codex.discovery.companieshouse.impl;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frc.codex.discovery.companieshouse.CompaniesHouseClient;
import com.frc.codex.discovery.companieshouse.CompaniesHouseConfig;
import com.frc.codex.discovery.companieshouse.CompaniesHouseRateLimiter;

@Component
@Profile("application")
public class CompaniesHouseClientImpl implements CompaniesHouseClient {
	private static final Set<String> ACCEPTED_CONTENT_TYPES = Set.of("application/xml", "application/xhtml+xml");
	private static final Set<String> IGNORED_CONTENT_TYPES = Set.of("application/pdf", "application/json", "text/csv");
	private final Logger LOG = LoggerFactory.getLogger(CompaniesHouseClientImpl.class);
	private final CompaniesHouseConfig config;
	public final CompaniesHouseDocumentClient document;
	public final CompaniesHouseInformationClient information;
	public final CompaniesHouseStreamClient stream;

	public CompaniesHouseClientImpl(CompaniesHouseConfig config, CompaniesHouseRateLimiter rateLimiter) {
		this.config = requireNonNull(config);
		this.document = new CompaniesHouseDocumentClient(rateLimiter, config.documentApiBaseUrl(), config.restApiKey());
		this.information = new CompaniesHouseInformationClient(rateLimiter, config.informationApiBaseUrl(), config.restApiKey());
		this.stream = new CompaniesHouseStreamClient(config.streamApiBaseUrl(), config.streamApiKey());
	}

	public String getCompany(String companyNumber) {
		return information.getCompany(companyNumber);
	}

	public Set<String> getCompanyFilingUrls(String companyNumber, String filingId) throws JsonProcessingException {
		String json = information.getCompanyFiling(companyNumber, filingId);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(json);
		return getCompanyFilingUrls(node);
	}

	public String getCompanyFilingHistory(String companyNumber) {
		return information.getCompanyFilingHistory(companyNumber);
	}

	private Set<String> getCompanyFilingUrls(JsonNode node) throws JsonProcessingException {
		Set<String> filingUrls = new HashSet<>();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode links = node.get("links");
		JsonNode documentMetadata = links.get("document_metadata");
		if (documentMetadata == null) {
			return filingUrls;
		}
		String documentMetadataUrl = documentMetadata.asText();
		String documentId = documentMetadataUrl.substring(documentMetadataUrl.lastIndexOf("/") + 1);
		String metadata = document.getMetadata(documentId);
		JsonNode metadataNode = mapper.readTree(metadata);
		JsonNode resources = metadataNode.get("resources");
		if (resources != null) {
			for (Iterator<Map.Entry<String, JsonNode>> it = resources.fields(); it.hasNext(); ) {
				Map.Entry<String, JsonNode> field = it.next();
				String key = field.getKey();
				if (IGNORED_CONTENT_TYPES.contains(key)) {
					continue;
				}
				if (!ACCEPTED_CONTENT_TYPES.contains(key)) {
					LOG.error("Unexpected content type: {}", key);
					continue;
				}
				// `contentType` query parameter is only added to indicate which content type is available at the URL
				// It is not a functional use of the Companies House Documents API.
				String contentType = URLEncoder.encode(key, StandardCharsets.UTF_8);
				filingUrls.add(config.documentApiBaseUrl() + "/document/" + documentId + "/content?contentType=" + contentType);
			}
		}
		return filingUrls;
	}

	public Set<String> getCompanyFilingUrls(String companyNumber) throws JsonProcessingException {
		String json = getCompanyFilingHistory(companyNumber);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(json);
		Set<String> filingUrls = new HashSet<>();
		JsonNode items = node.get("items");
		if (items == null) {
			return filingUrls;
		}
		for (JsonNode item : items) {
			filingUrls.addAll(getCompanyFilingUrls(item));
		}
		return filingUrls;
	}

	public void streamFilings(Long timepoint, Function<String, Boolean> callback) throws IOException {
		stream.streamFilings(timepoint, callback);
	}
}
