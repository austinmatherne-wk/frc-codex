package com.frc.codex.discovery.companieshouse.impl;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.frc.codex.discovery.companieshouse.CompaniesHouseHistoryClient;

@Component
@Profile("application")
public class CompaniesHouseHistoryClientImpl implements CompaniesHouseHistoryClient {

	private final URI DOWNLOAD_ARCHIVE_PAGE_URL = URI.create("https://download.companieshouse.gov.uk/historicmonthlyaccountsdata.html");
	private final URI DOWNLOAD_DAILY_PAGE_URL = URI.create("https://download.companieshouse.gov.uk/en_accountsdata.html");
	private final URI DOWNLOAD_MONTHLY_PAGE_URL = URI.create("https://download.companieshouse.gov.uk/en_monthlyaccountsdata.html");
	private final Logger LOG = LoggerFactory.getLogger(CompaniesHouseHistoryClientImpl.class);

	private final Pattern archiveHrefPattern;
	private final Pattern dailyHrefPattern;
	private final Pattern hrefPattern;
	private final Pattern monthlyHrefPattern;
	private final RestTemplate restTemplate;

	public CompaniesHouseHistoryClientImpl() {
		this.archiveHrefPattern = Pattern.compile("archive/Accounts_Monthly_Data-.*\\.zip");
		this.dailyHrefPattern = Pattern.compile("Accounts_Bulk_Data-.*\\.zip");
		this.hrefPattern = Pattern.compile("<a href=\"(.*?)\">");
		this.monthlyHrefPattern = Pattern.compile("Accounts_Monthly_Data-.*\\.zip");
		this.restTemplate = new RestTemplate();
	}

	@Override
	public void downloadArchive(URI uri, Path outputFilePath) throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));
		HttpEntity<String> entity = new HttpEntity<>(null, headers);
		ResponseEntity<byte[]> response = restTemplate.exchange(
				uri,
				HttpMethod.GET,
				entity,
				byte[].class
		);
		byte[] body = requireNonNull(response.getBody());
		Files.write(outputFilePath, body);
	}

	@Override
	public List<URI> getArchiveDownloadLinks() {
		return scrapeMatchingHrefs(DOWNLOAD_ARCHIVE_PAGE_URL, archiveHrefPattern);
	}

	@Override
	public List<URI> getDailyDownloadLinks() {
		return scrapeMatchingHrefs(DOWNLOAD_DAILY_PAGE_URL, dailyHrefPattern);
	}

	private List<URI> scrapeMatchingHrefs(URI pageUri, Pattern pattern) {
		List<String> hrefs = scrapeLinkHrefs(pageUri);
		URI baseUri = DOWNLOAD_ARCHIVE_PAGE_URL.resolve(".");
		List<URI> uris = new ArrayList<>();
		for (String href : hrefs) {
			if (pattern.matcher(href).matches()) {
				URI uri = baseUri.resolve(href);
				uris.add(uri);
			}
		}
		return uris;
	}

	private List<String> scrapeLinkHrefs(URI uri) {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(List.of(MediaType.TEXT_HTML));
		HttpEntity<String> entity = new HttpEntity<>(null, headers);
		ResponseEntity<String> response = restTemplate.exchange(
				uri,
				HttpMethod.GET,
				entity,
				String.class
		);
		String body = requireNonNull(response.getBody());
		Matcher matcher = hrefPattern.matcher(body);
		List<String> matches = new ArrayList<>();
		while (matcher.find()) {
			matches.add(matcher.group(1));
		}
		return matches;
	}

	@Override
	public List<URI> getMonthlyDownloadLinks() {
		return scrapeMatchingHrefs(DOWNLOAD_MONTHLY_PAGE_URL, monthlyHrefPattern);
	}
}
