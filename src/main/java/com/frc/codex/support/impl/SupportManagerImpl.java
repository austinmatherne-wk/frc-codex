package com.frc.codex.support.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.frc.codex.properties.FilingIndexProperties;
import com.frc.codex.model.HelpRequest;
import com.frc.codex.model.SurveyRequest;
import com.frc.codex.support.SupportManager;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;

@Component
public class SupportManagerImpl implements SupportManager {
	private static final Logger LOG = LoggerFactory.getLogger(SupportManagerImpl.class);
	private final SesClient sesClient;
	private final String supportEmail;

	public SupportManagerImpl(FilingIndexProperties properties) {
		this.sesClient = SesClient.builder().build();
		this.supportEmail = properties.supportEmail();
	}

	public UUID sendHelpRequest(HelpRequest helpRequest) {
		if (supportEmail == null || supportEmail.isEmpty()) {
			LOG.info("Support email address is not configured. Could not email help request: {}.", helpRequest);
			return null;
		}
		UUID id = UUID.randomUUID();
		String subject = "FRC CODEx Help Request [" + id + "]";
		String body =
				"ID: " + id + "\n" +
				"Name: " + helpRequest.getName() + "\n" +
				"Email: " + helpRequest.getEmailAddress() + "\n" +
				"Referer URL: " + helpRequest.getReferer() + "\n" +
				"Message: \n" + helpRequest.getMessage() + "\n";
		try {
			SendEmailRequest emailRequest = SendEmailRequest.builder()
					.destination(Destination.builder()
							.toAddresses(supportEmail)
							.build())
					.message(Message.builder()
							.subject(Content.builder()
									.data(subject)
									.build())
							.body(Body.builder()
									.text(Content.builder()
											.data(body)
											.build())
									.build())
							.build())
					.replyToAddresses(helpRequest.getEmailAddress())
					.source(supportEmail)
					.build();
			SendEmailResponse response = sesClient.sendEmail(emailRequest);
			LOG.info("Sent help request message {} ({}).", response.messageId(), helpRequest);
			return id;
		} catch (SesException e) {
			LOG.error("Failed to send help request email ({}).", helpRequest, e);
			return null;
		}
	}

	public UUID sendSurveyRequest(SurveyRequest surveyRequest) {
		UUID id = UUID.randomUUID();
		if (surveyRequest.getSearchUtilityRating() != null) {
			LOG.info("[SURVEY RESPONSE] SEARCH_UTILITY={} (ID: {})", surveyRequest.getSearchUtilityRating(), id);
		}
		if (surveyRequest.getSearchSpeedRating() != null) {
			LOG.info("[SURVEY RESPONSE] SEARCH_SPEED={} (ID: {})", surveyRequest.getSearchSpeedRating(), id);
		}
		if (surveyRequest.getViewerSpeedRating() != null) {
			LOG.info("[SURVEY RESPONSE] VIEWER_SPEED={} (ID: {})", surveyRequest.getViewerSpeedRating(), id);
		}
		return id;
	}
}
