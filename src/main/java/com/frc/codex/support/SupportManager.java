package com.frc.codex.support;

import java.util.UUID;

import com.frc.codex.model.HelpRequest;

public interface SupportManager {
	UUID sendHelpRequest(HelpRequest helpRequest);
}
