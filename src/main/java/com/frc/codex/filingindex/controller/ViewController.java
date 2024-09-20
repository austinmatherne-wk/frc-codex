package com.frc.codex.filingindex.controller;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.frc.codex.FilingIndexProperties;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@RestController
public class ViewController {
	private final S3ClientBuilder s3ClientBuilder;
	public ViewController(
			FilingIndexProperties properties
	) {
		Region awsRegion = Region.of(properties.awsRegion());
		AwsBasicCredentials credentials = AwsBasicCredentials.create(
				properties.awsAccessKeyId(),
				properties.awsSecretAccessKey()
		);
		try {
			this.s3ClientBuilder = S3Client.builder()
					.endpointOverride(new URI(properties.awsHost()))
					.forcePathStyle(true)
					.region(awsRegion)
					.credentialsProvider(StaticCredentialsProvider.create(credentials));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
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
				.bucket("frc-codex-results")
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
