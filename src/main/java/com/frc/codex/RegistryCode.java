package com.frc.codex;

public enum RegistryCode {
	COMPANIES_HOUSE ("CH", "Companies House"),
	FCA ("FCA", "Financial Conduct Authority (FCA)"),;

	private final String name;
	public final String displayName;

	private RegistryCode(String n, String d) {
		name = n;
		displayName = d;
	}
	public String toString() {
		return this.name;
	}
}
