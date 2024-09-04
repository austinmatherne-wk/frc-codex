package com.frc.codex.model;

public enum FilingStatus {
	PENDING ("pending"),
	QUEUED ("queued"),
	COMPLETED ("completed"),
	FAILED ("failed");

	private final String name;

	private FilingStatus(String s) {
		name = s;
	}

	public String toString() {
		return this.name;
	}
}
