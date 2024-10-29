package com.frc.codex.model;


public enum Rating {
	VERY_UNSATISFIED("Very Unsatisfied", 1),
	UNSATISFIED("Unsatisfied", 2),
	NEUTRAL("Neutral", 3),
	SATISFIED("Satisfied", 4),
	VERY_SATISFIED("Very Satisfied", 5);

	private final int value;
	private final String label;

	Rating(String label, int value) {
		this.label = label;
		this.value = value;
	}

	public String getLabel() {
		return label;
	}

	public int getValue() {
		return value;
	}
}
