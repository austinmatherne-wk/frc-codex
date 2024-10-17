package com.frc.codex.filingindex.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frc.codex.FilingIndexProperties;
import com.frc.codex.database.DatabaseManager;
import com.frc.codex.indexer.LambdaManager;
import com.frc.codex.model.Filing;
import com.frc.codex.model.FilingPayload;
import com.frc.codex.model.FilingResultRequest;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@RestController
public class ViewController {
	private static final Logger LOG = LoggerFactory.getLogger(ViewController.class);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private final DatabaseManager databaseManager;
	private final LambdaManager lambdaManager;
	private final FilingIndexProperties properties;
	private final RestTemplate restTemplate;
	private final S3ClientBuilder s3ClientBuilder;
	private final ConcurrentHashMap<UUID, CompletableFuture<InvokeResponse>> invokeFutures;
	public ViewController(
			FilingIndexProperties properties,
			DatabaseManager databaseManager,
			LambdaManager lambdaManager
	) {
		this.properties = properties;
		this.databaseManager = databaseManager;
		this.lambdaManager = lambdaManager;
		this.restTemplate = new RestTemplate();
		this.s3ClientBuilder = S3Client.builder()
				.forcePathStyle(true);
		this.invokeFutures = new ConcurrentHashMap<>();
	}

	private ModelAndView loadingResult(Filing filing) {
		return new ModelAndView("redirect:/view/" + filing.getFilingId() + "/loading");
	}

	private ModelAndView onDemandResult(Filing filing) {
		UUID filingId = filing.getFilingId();
		try {
			LOG.info("Processing filing on demand: {}", filingId);
			CompletableFuture<InvokeResponse> invokeResponse = lambdaManager.invokeAsync(new FilingPayload(
					filingId,
					filing.getDownloadUrl(),
					filing.getRegistryCode()
			));
			invokeFutures.put(filingId, invokeResponse);
			return loadingResult(filing);
		} catch (Exception e) {
			invokeFutures.remove(filingId);
			throw e;
		}
	}

	private ModelAndView viewerResult(UUID filingId, String stubViewerUrl) {
		return new ModelAndView("redirect:/view/" + filingId + "/" + stubViewerUrl);
	}

	@GetMapping("/view/{filingId}/loading")
	public ModelAndView loadingPage(
			@PathVariable("filingId") String filingId
	) {
		ModelAndView model = new ModelAndView("view/loading");
		model.addObject("iframeSrc", "/view/" + filingId + "/public");
		return model;
	}

	@GetMapping("/view/{filingId}/public")
	public ResponseEntity<byte[]> publicPage(
			@PathVariable("filingId") String filingId
	) {
		UUID filingUuid = UUID.fromString(filingId);
		Filing filing = databaseManager.getFiling(filingUuid);
		String filingUrl = filing.getExternalViewUrl();
		return restTemplate.exchange(
				filingUrl,
				HttpMethod.GET,
				new HttpEntity<>(null, new HttpHeaders()),
				byte[].class
		);
	}

	/**
	 * This endpoint serves as a proxy to the S3 bucket hosting the stub viewer.
	 */
	@RequestMapping("/view/{filingId}/viewer")
	public ModelAndView viewerPage(
			@PathVariable("filingId") String filingId
	) {
		UUID filingUuid = UUID.fromString(filingId);
		Filing filing = databaseManager.getFiling(filingUuid);
		if (filing.getStatus().equals("completed")) {
			// Already completed, redirect directly to viewer.
			return viewerResult(filing.getFilingId(), filing.getStubViewerUrl());
		}
		if (filing.getStatus().equals("failed")) {
			// Generation failed, show error message.
			ModelAndView modelAndView = new ModelAndView("view/unavailable");
			modelAndView.addObject("message", "Viewer generation failed. Please try again later.");
			return modelAndView;
		}
		CompletableFuture<InvokeResponse> future = invokeFutures.get(filingUuid);
		if (future == null) {
			// No request in progress. We'll start one.
			return onDemandResult(filing);
		} else {
			// A request is already in progress, we'll redirect to the loading page to wait.
			return loadingResult(filing);
		}
	}

	/**
	 * This endpoint serves as a proxy to the S3 bucket hosting the stub viewer.
	 */
	@RequestMapping("/view/{jobId}/{assetKey}")
	@ResponseBody
	public ResponseEntity<String> viewerAssetPage(
			@PathVariable("jobId") String jobId,
			@PathVariable("assetKey") String assetKey
	) throws IOException {
		String key = jobId + "/" + assetKey;
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(properties.s3ResultsBucketName())
				.key(key)
				.build();
		try (S3Client s3 = s3ClientBuilder.build()) {
			try (ResponseInputStream<GetObjectResponse> response = s3.getObject(getObjectRequest)) {
				String decodedString = new String(response.readAllBytes(), StandardCharsets.UTF_8);
				return ResponseEntity.ok(decodedString);
			}
		}
	}

	@GetMapping("/view/{filingId}/wait")
	public ModelAndView waitPage(
			@PathVariable("filingId") String filingId
	) {
		UUID filingUuid = UUID.fromString(filingId);
		if (invokeFutures.containsKey(filingUuid)) {
			CompletableFuture<InvokeResponse> future = invokeFutures.get(filingUuid);
			if (future.isDone()) {
				invokeFutures.remove(filingUuid);
			}
			try {
				LOG.info("Awaiting Lambda result for filing: {}", filingUuid);
				InvokeResponse invokeResponse = future.get();
				// Synchronize this block to ensure that only one request
				// applies the result and removes the future.
				synchronized (future) {
					if (invokeFutures.containsKey(filingUuid)) {
						FilingResultRequest result = lambdaManager.parseResult(invokeResponse);
						// Apply the result before we remove from the map to ensure that no requests
						// occur after a future is removed but before the result is applied.
						databaseManager.applyFilingResult(result);
						invokeFutures.remove(filingUuid);
					}
				}
			} catch (InterruptedException | ExecutionException e) {
				LOG.error("Encountered exception while awaiting Lambda result for filing: {}", filingUuid, e);
			}
		}
		return new ModelAndView("redirect:/view/" + filingId + "/viewer");
	}
}
