package com.frc.codex.discovery.companieshouse.impl;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import com.frc.codex.RegistryCode;
import com.frc.codex.discovery.companieshouse.CompaniesHouseClient;
import com.frc.codex.discovery.companieshouse.CompaniesHouseConfig;
import com.frc.codex.discovery.companieshouse.CompaniesHouseRateLimiter;
import com.frc.codex.model.NewFilingRequest;

@Component
@Profile("application")
public class CompaniesHouseClientImpl implements CompaniesHouseClient {
	private static final Set<String> ACCEPTED_CONTENT_TYPES = Set.of("application/xml", "application/xhtml+xml");
	private static final DateTimeFormatter CH_JSON_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final Set<String> IGNORED_CONTENT_TYPES = Set.of("application/pdf", "application/json", "text/csv");
	private final Logger LOG = LoggerFactory.getLogger(CompaniesHouseClientImpl.class);
	private final CompaniesHouseConfig config;
	private final CompaniesHouseHttpClient document;
	private final CompaniesHouseHttpClient information;
	public final CompaniesHouseStreamClient stream;

	public CompaniesHouseClientImpl(CompaniesHouseConfig config, CompaniesHouseRateLimiter rateLimiter) {
		this.config = requireNonNull(config);
		this.document = new CompaniesHouseHttpClient(rateLimiter, config.documentApiBaseUrl(), config.restApiKey());
		this.information = new CompaniesHouseHttpClient(rateLimiter, config.informationApiBaseUrl(), config.restApiKey());
		this.stream = new CompaniesHouseStreamClient(config.streamApiBaseUrl(), config.streamApiKey());
	}

	public String getCompany(String companyNumber) {
		return information.get("/company/" + companyNumber);
	}

	public Set<String> getCompanyFilingUrls(String companyNumber, String filingId) throws JsonProcessingException {
		String json = information.get("/company/" + companyNumber + "/filing-history/" + filingId);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(json);
		return getCompanyFilingUrls(node);
	}

	public String getCompanyFilingHistory(String companyNumber, int itemsPerPage, int startIndex) {
		return information.get("/company/" + companyNumber + "/filing-history?category=accounts&items_per_page=" + itemsPerPage + "&start_index=" + startIndex);
	}

	public List<NewFilingRequest> getCompanyFilings(String companyNumber) throws JsonProcessingException {
		List<NewFilingRequest> filings = new ArrayList<>();
		int index = 0;
		int itemsPerPage = 100;
		int totalItems = Integer.MAX_VALUE;
		while (index + itemsPerPage < totalItems) {
			String json = getCompanyFilingHistory(companyNumber, itemsPerPage, index);
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(json);
			JsonNode items = node.get("items");
			if (items == null) {
				break;
			}
			totalItems = node.get("total_count").asInt();
			for (JsonNode item : items) {
				JsonNode filingDateNode = item.get("date");
				LocalDateTime filingDate = null;
				if (filingDateNode != null) {
					String dateStr = filingDateNode.asText();
					try {
						filingDate = LocalDate.parse(dateStr, CH_JSON_DATE_FORMAT).atStartOfDay();
					} catch (Exception e) {
						throw new RuntimeException("Failed to parse date: " + dateStr, e);
					}
				}
				JsonNode documentDateNode = item.get("action_date");
				LocalDateTime documentDate = null;
				if (documentDateNode != null) {
					String dateStr = documentDateNode.asText();
					try {
						documentDate = LocalDate.parse(dateStr, CH_JSON_DATE_FORMAT).atStartOfDay();
					} catch (Exception e) {
						throw new RuntimeException("Failed to parse date: " + dateStr, e);
					}
				}
				Set<String> filingUrls = getCompanyFilingUrls(item);
				for (String filingUrl : filingUrls) {
					NewFilingRequest newFilingRequest = new NewFilingRequest();
					newFilingRequest.setCompanyNumber(companyNumber);
					newFilingRequest.setDocumentDate(documentDate);
					newFilingRequest.setDownloadUrl(filingUrl);
					newFilingRequest.setFilingDate(filingDate);
					newFilingRequest.setRegistryCode(RegistryCode.COMPANIES_HOUSE.toString());
					filings.add(newFilingRequest);
				}
			}
			index += itemsPerPage;
		}
		return filings;
	}

	private Set<String> getCompanyFilingUrls(JsonNode node) throws JsonProcessingException {
		Set<String> filingUrls = new HashSet<>();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode links = node.get("links");
		if (links == null) {
			return filingUrls;
		}
		JsonNode documentMetadata = links.get("document_metadata");
		if (documentMetadata == null) {
			return filingUrls;
		}
		String documentMetadataUrl = documentMetadata.asText();
		String documentId = documentMetadataUrl.substring(documentMetadataUrl.lastIndexOf("/") + 1);
		String metadata = document.get("/document/" + documentId);
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

	public void streamFilings(Long timepoint, Function<String, Boolean> callback) throws IOException {
		stream.streamFilings(timepoint, callback);
	}
}
