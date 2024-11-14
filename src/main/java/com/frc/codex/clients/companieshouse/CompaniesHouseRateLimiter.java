package com.frc.codex.clients.companieshouse;

import java.sql.Timestamp;

import org.springframework.http.ResponseEntity;

public interface CompaniesHouseRateLimiter {
	long getLimit();
	long getRemaining();
	Timestamp getUpdatedTimestamp();
	Timestamp getResetTimestamp();
	boolean isHealthy();
	void notifyRejection();
	void updateLimits(ResponseEntity<?> response, String url);
}
