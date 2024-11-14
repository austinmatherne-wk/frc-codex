package com.frc.codex.clients.companieshouse;

public class RateLimitException extends RuntimeException {
	public RateLimitException(String message) {
		super(message);
	}

	public RateLimitException(String message, Throwable cause) {
		super(message, cause);
	}
}
