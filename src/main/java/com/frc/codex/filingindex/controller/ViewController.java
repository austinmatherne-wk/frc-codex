package com.frc.codex.filingindex.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

import org.apache.coyote.Response;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.frc.codex.FilingIndexProperties;
import com.frc.codex.database.DatabaseManager;
import com.frc.codex.impl.FilingIndexPropertiesImpl;

@RestController
public class ViewController {
	private final DatabaseManager databaseManager;
	private final FilingIndexProperties properties;
	private final RestTemplate restTemplate;

	public ViewController(
			DatabaseManager databaseManager,
			FilingIndexProperties properties
	) {
		this.databaseManager = databaseManager;
		this.properties = properties;
		this.restTemplate = new RestTemplate();
	}

	/**
	 * This endpoint serves as a proxy to the S3 bucket hosting the stub viewer.
	 */
	@RequestMapping("/view/{jobId}/{assetKey}")
	@ResponseBody
	public String viewerAssetPage(
			@PathVariable("jobId") String jobId,
			@PathVariable("assetKey") String assetKey
	) throws URISyntaxException
	{
		URI uri = new URI(properties.awsHost() + "/frc-codex-results/" + jobId + "/" + assetKey);
		ResponseEntity<String> responseEntity =
				restTemplate.exchange(uri, HttpMethod.GET, null, String.class);
		return responseEntity.getBody();
	}
}
