package com.frc.codex;

public enum RegistryCode {
	COMPANIES_HOUSE ("CH"),
	FCA ("FCA");

	private final String name;

	private RegistryCode(String s) {
		name = s;
	}

	public String toString() {
		return this.name;
	}
}
