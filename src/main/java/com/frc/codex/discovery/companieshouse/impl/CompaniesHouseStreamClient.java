package com.frc.codex.discovery.companieshouse.impl;

import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.function.Function;


public class CompaniesHouseStreamClient {
	private final String apiKey;
	private final String baseUrl;

	public CompaniesHouseStreamClient(String baseUrl, String apiKey) {
		this.apiKey = requireNonNull(apiKey);
		this.baseUrl = requireNonNull(baseUrl);
	}

	private void stream(String url, Function<String, Boolean> callback) throws IOException {
		URL fullUrl = new URL(baseUrl + url);
		HttpURLConnection conn = (HttpURLConnection) fullUrl.openConnection();
		conn.setRequestMethod("GET");
		// https://developer-specs.company-information.service.gov.uk/streaming-api/guides/authentication
		String encodedApiKey = Base64.getEncoder().encodeToString((apiKey + ":").getBytes());
		conn.setRequestProperty("Authorization", "Basic " + encodedApiKey);

		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		while ((line = reader.readLine()) != null) {
			if (!callback.apply(line)) {
				break;
			}
		}
		reader.close();
	}

	public void streamFilings(Function<String, Boolean> callback) throws IOException {
		stream("/filings", callback);
	}

	public void streamFilings(long timepoint, Function<String, Boolean> callback) throws IOException {
		stream("/filings?timepoint=" + timepoint, callback);
	}
}
