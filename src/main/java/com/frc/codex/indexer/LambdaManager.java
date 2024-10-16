package com.frc.codex.indexer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.frc.codex.model.FilingPayload;
import com.frc.codex.model.FilingResultRequest;

import software.amazon.awssdk.services.lambda.model.InvokeResponse;

public interface LambdaManager {
	CompletableFuture<InvokeResponse> invokeAsync(FilingPayload payload);
	InvokeResponse invokeSync(FilingPayload payload) throws ExecutionException, InterruptedException;
	FilingResultRequest parseResult(InvokeResponse invokeResponse);
}
