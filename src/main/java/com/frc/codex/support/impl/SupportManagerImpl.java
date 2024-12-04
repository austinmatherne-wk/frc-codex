package com.frc.codex.support.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.frc.codex.properties.FilingIndexProperties;
import com.frc.codex.model.HelpRequest;
import com.frc.codex.model.SurveyRequest;
import com.frc.codex.support.SupportManager;

@Component
public class SupportManagerImpl implements SupportManager {
	private static final Logger LOG = LoggerFactory.getLogger(SupportManagerImpl.class);
	private final String supportEmail;

	public SupportManagerImpl(FilingIndexProperties properties) {
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
