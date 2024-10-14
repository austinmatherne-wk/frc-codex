package com.frc.codex.indexer.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.frc.codex.FilingIndexProperties;
import com.frc.codex.indexer.LambdaManager;
import com.frc.codex.model.FilingPayload;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

@Component
public class LambdaManagerImpl implements LambdaManager {
	private static final Logger LOG = LoggerFactory.getLogger(IndexerImpl.class);
	private final LambdaAsyncClient client;
	private final FilingIndexProperties properties;

	public LambdaManagerImpl(FilingIndexProperties properties) throws URISyntaxException {
		this.properties = properties;
		if (properties.isAws()) {
			this.client = LambdaAsyncClient.builder()
					.region(Region.of(properties.awsRegion()))
					.build();
		} else {
			this.client = LambdaAsyncClient.builder()
					.endpointOverride(new URI("http://lambda.localhost:8080/"))
					.overrideConfiguration(ClientOverrideConfiguration.builder()
							.apiCallAttemptTimeout(Duration.ofSeconds(300))
							.apiCallTimeout(Duration.ofSeconds(300))
							.build())
					.region(Region.of(properties.awsRegion()))
					.build();
		}
	}

	private SdkBytes serializePayload(FilingPayload payload) {
		return SdkBytes.fromString(payload.toString(), StandardCharsets.UTF_8);
	}

	public CompletableFuture<InvokeResponse> invokeAsync(FilingPayload payload) {
		InvokeRequest request = InvokeRequest.builder()
				.functionName(properties.awsLambdaFunctionName())
				.payload(serializePayload(payload))
				.build();
		return client.invoke(request);
	}

	public InvokeResponse invokeSync(FilingPayload payload) throws ExecutionException, InterruptedException {
		if (properties.isAws()) {
			CompletableFuture<InvokeResponse> future = invokeAsync(payload);
			return future.get();
		} else {
			// Local Lambda container can't currently handle concurrent requests (as it's meant to emulate
			// a single Lambda container which is never tasked with concurrent requests).
			// This may be improved upon in the future:
			// https://github.com/aws/aws-lambda-runtime-interface-emulator/issues/97
			synchronized (this) {
				LOG.info("Invoking lambda function...");
				try {
					CompletableFuture<InvokeResponse> future = invokeAsync(payload);
					return future.get();
				} finally {
					LOG.info("Completed lambda function invocation.");
				}
			}
		}
	}
}
