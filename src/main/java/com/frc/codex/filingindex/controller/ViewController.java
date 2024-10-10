package com.frc.codex.filingindex.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frc.codex.FilingIndexProperties;
import com.frc.codex.database.DatabaseManager;
import com.frc.codex.indexer.LambdaManager;
import com.frc.codex.model.Filing;
import com.frc.codex.model.FilingPayload;
import com.frc.codex.model.FilingResultRequest;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
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
	private final S3ClientBuilder s3ClientBuilder;
	private final ConcurrentHashMap<UUID, Boolean> filingProgressMap;
	public ViewController(
			FilingIndexProperties properties,
			DatabaseManager databaseManager,
			LambdaManager lambdaManager
	) {
		this.properties = properties;
		this.databaseManager = databaseManager;
		this.lambdaManager = lambdaManager;
		Region awsRegion = Region.of(properties.awsRegion());
		this.s3ClientBuilder = S3Client.builder()
				.forcePathStyle(true)
				.region(awsRegion);
		this.filingProgressMap = new ConcurrentHashMap<>();
	}

	private FilingResultRequest parseResult(InvokeResponse invokeResponse) {
		JsonNode root;
		try {
			root = OBJECT_MAPPER.readTree(invokeResponse.payload().asByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		LOG.info("Received response: {}", root);
		return FilingResultRequest.builder()
				.json(root)
				.build();
	}

	private ResponseEntity<String> onDemandResult(String filingId)
			throws ExecutionException, InterruptedException, IOException {
		UUID filingUuid = UUID.fromString(filingId);
		String assetKey;
		try {
			LOG.info("Processing filing on demand: {}", filingUuid);
			Filing filing = databaseManager.getFiling(filingUuid);
			InvokeResponse invokeResponse = lambdaManager.invokeSync(new FilingPayload(
					filing.getFilingId(),
					filing.getDownloadUrl(),
					filing.getRegistryCode()
			));
			Integer statusCode = invokeResponse.statusCode();
			if (statusCode >= 300 || statusCode < 200) {
				filingProgressMap.remove(filingUuid);
				return ResponseEntity.status(statusCode).body(invokeResponse.functionError());
			}
			FilingResultRequest filingResultRequest = parseResult(invokeResponse);
			assetKey = filingResultRequest.getStubViewerUrl();
			databaseManager.applyFilingResult(filingResultRequest);
		} catch (Exception e) {
			filingProgressMap.remove(filingUuid);
			throw e;
		}
		filingProgressMap.put(filingUuid, true);
		return viewerAssetPage(filingId, assetKey);
	}

	private ResponseEntity<String> unavailableResult() {
		return ResponseEntity
				.status(202)
				.body("This filing is not currently available.");
	}

	private ResponseEntity<String> waitResult(UUID filingUuid, Boolean completed) throws IOException {
		while (!completed) {
			LOG.info("Waiting for filing to be processed by previous request: {}", filingUuid);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			completed = filingProgressMap.get(filingUuid);
			if (completed == null) {
				return unavailableResult();
			}
		}
		Filing filing = databaseManager.getFiling(filingUuid);
		return viewerAssetPage(filingUuid.toString(), filing.getStubViewerUrl());
	}

	/**
	 * This endpoint serves as a proxy to the S3 bucket hosting the stub viewer.
	 */
	@RequestMapping("/view/{filingId}/viewer")
	@ResponseBody
	public ResponseEntity<String> viewerPage(
			@PathVariable("filingId") String filingId
	) throws IOException, ExecutionException, InterruptedException {
		UUID filingUuid = UUID.fromString(filingId);
		Filing filing = databaseManager.getFiling(filingUuid);
		if (filing.getStatus().equals("completed")) {
			return viewerAssetPage(filingId, filing.getStubViewerUrl());
		}
		if (filing.getStatus().equals("failed")) {
			return unavailableResult();
		}
		Boolean completed = filingProgressMap.putIfAbsent(filingUuid, false);
		if (completed == null) {
			return onDemandResult(filingId);
		} else {
			return waitResult(filingUuid, completed);
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
}
