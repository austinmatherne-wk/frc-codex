package com.frc.codex.support;

import java.util.UUID;

import com.frc.codex.model.HelpRequest;
import com.frc.codex.model.SurveyRequest;

public interface SupportManager {
	UUID sendHelpRequest(HelpRequest helpRequest);
	UUID sendSurveyRequest(SurveyRequest surveyRequest);
}