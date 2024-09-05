package com.frc.codex.indexer.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.frc.codex.FilingIndexProperties;
import com.frc.codex.indexer.QueueManager;
import com.frc.codex.model.Filing;
import com.frc.codex.model.FilingResultRequest;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Component
public class QueueManagerImpl implements QueueManager {
	private static final Logger LOG = LoggerFactory.getLogger(IndexerImpl.class);
	private final FilingIndexProperties properties;
	private final Region awsRegion;

	public QueueManagerImpl(FilingIndexProperties properties) {
		this.properties = properties;
		this.awsRegion = Region.of(properties.awsRegion());
	}

	/*
	 * Adds a list of filings to the jobs queue, invoking a callback on each filing.
	 */
	public void addJobs(List<Filing> filings, Consumer<Filing> callback) {
		try (SqsClient sqsClient = getSqsClient()) {
			String queueUrl = sqsClient
					.getQueueUrl(builder -> builder.queueName("frc_codex_jobs"))
					.queueUrl();
			for (Filing filing : filings) {
				String filingId = filing.getFilingId().toString();
				LOG.info("Queueing filing: {}", filingId);
				MessageAttributeValue downloadUrl = MessageAttributeValue.builder()
						.stringValue(filing.getDownloadUrl())
						.dataType("String")
						.build();
				MessageAttributeValue registryCode = MessageAttributeValue.builder()
						.stringValue(filing.getRegistryCode())
						.dataType("String")
						.build();
				SendMessageRequest request = SendMessageRequest.builder()
						.queueUrl(queueUrl)
						.messageBody(filing.getFilingId().toString())
						.messageAttributes(Map.of(
								"DownloadUrl", downloadUrl,
								"RegistryCode", registryCode
						))
						.build();
				SendMessageResponse response = sqsClient.sendMessage(request);
				if (response.sdkHttpResponse().isSuccessful()) {
					callback.accept(filing);
					LOG.info("Queued filing: {}", filingId);
				} else {
					LOG.error(
							"Failed to queue filing {}: ({}) {}",
							filingId,
							response.sdkHttpResponse().statusCode(),
							response.sdkHttpResponse().statusText()
					);
				}
			}
		}
	}

	private SqsClient getSqsClient() {
		AwsBasicCredentials credentials = AwsBasicCredentials.create(
				properties.awsAccessKeyId(),
				properties.awsSecretAccessKey()
		);
		try {
			return SqsClient.builder()
					.endpointOverride(new URI(properties.awsHost()))
					.region(awsRegion)
					.credentialsProvider(StaticCredentialsProvider.create(credentials))
					.build();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public String getStatus() {
		StringBuilder status = new StringBuilder();
		for(String queueName : List.of("frc_codex_jobs", "frc_codex_results")) {
			try (SqsClient sqsClient = getSqsClient()) {
				status.append(queueName).append(":\n");
				String queueUrl = sqsClient
						.getQueueUrl(builder -> builder.queueName(queueName))
						.queueUrl();
				GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
						.queueUrl(queueUrl)
						.attributeNamesWithStrings(
								"ApproximateNumberOfMessages",
								"ApproximateNumberOfMessagesDelayed",
								"ApproximateNumberOfMessagesNotVisible",
								"LastModifiedTimestamp"
						)
						.build();
				sqsClient.getQueueAttributes(request)
						.attributes()
						.forEach((key, value) -> status
								.append("\t")
								.append(key)
								.append(": ")
								.append(value)
								.append("\n"));
			}
		}
		return status.toString();
	}

	/*
	 * Retrieves messages from the results queue.
	 */
	public void processResults(Function<FilingResultRequest, Boolean> callback) {
		try (SqsClient sqsClient = getSqsClient()) {
			String queueUrl = sqsClient
					.getQueueUrl(builder -> builder.queueName("frc_codex_results"))
					.queueUrl();
			ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
					.queueUrl(queueUrl)
					.messageAttributeNames("All")
					.maxNumberOfMessages(10)
					.waitTimeSeconds(5)
					.build();
			List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();
			LOG.info("Received results messages: {}", messages.size());
			for(Message message : messages) {
				boolean success = Objects.equals(message.messageAttributes().get("Success").stringValue(), "true");
				String error = null;
				String viewerEntrypoint = null;
				if (!success) {
					error = message.messageAttributes().get("Error").stringValue();
				} else {
					viewerEntrypoint = message.messageAttributes().get("ViewerEntrypoint").stringValue();
				}
				UUID filingId = UUID.fromString(message.messageAttributes().get("FilingId").stringValue());
				String logs = message.body();
				FilingResultRequest filingResultRequest = FilingResultRequest.builder()
						.error(error)
						.filingId(filingId)
						.logs(logs)
						.stubViewerUrl(viewerEntrypoint)
						.success(success)
						.build();
				if (callback.apply(filingResultRequest)) {
					sqsClient.deleteMessage(builder -> builder
							.queueUrl(queueUrl)
							.receiptHandle(message.receiptHandle()));
				}
			}
		}
	}
}
