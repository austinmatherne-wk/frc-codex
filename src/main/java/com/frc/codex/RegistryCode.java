package com.frc.codex;

public enum RegistryCode {
	COMPANIES_HOUSE ("CH", "Companies House", "Companies House",  "CRN"),
	FCA ("FCA", "Financial Conduct Authority (FCA)", "FCA", "LEI"),;

	private final String code;
	private final String fullName;
	private final String shortName;
	private final String companyFileReference;


	private RegistryCode(String code, String fullName, String shortName, String companyFileReference) {
		this.code = code;
		this.fullName = fullName;
		this.shortName = shortName;
		this.companyFileReference = companyFileReference;
	}

	public String getCode() {
		return this.code;
	}

	public String getFullName() {
		return this.fullName;
	}

	public String getShortName() {
		return this.shortName;
	}

	public String getCompanyFileReference() {
		return companyFileReference;
	}
}
