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

	public String getSupportEmail() {
		return supportEmail;
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
