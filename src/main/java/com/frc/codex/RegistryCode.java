package com.frc.codex;

public enum RegistryCode {
	COMPANIES_HOUSE ("CH", "Companies House"),
	FCA ("FCA", "Financial Conduct Authority (FCA)"),;

	private final String code;
	private final String displayName;

	private RegistryCode(String n, String d) {
		code = n;
		displayName = d;
	}

	public String getCode() {
		return this.code;
	}

	public String getDisplayName() {
		return this.displayName;
	}
}
