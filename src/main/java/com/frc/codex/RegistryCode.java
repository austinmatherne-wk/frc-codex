package com.frc.codex;

public enum RegistryCode {
	COMPANIES_HOUSE ("CH", "Companies House", "CoHo", "govuk-tag govuk-tag--turquoise", "CRN"),
	FCA ("FCA", "Financial Conduct Authority (FCA)", "FCA", "govuk-tag govuk-tag--purple", "LEI"),;

	private final String code;
	private final String companyFileReference;
	private final String displayName;
	private final String shortName;
	private final String tagClass;


	private RegistryCode(String n, String d, String s, String c, String f) {
		code = n;
		companyFileReference = f;
		displayName = d;
		shortName = s;
		tagClass = c;
	}

	public String getCode() {
		return this.code;
	}

	public String getCompanyFileReference() {
		return companyFileReference;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public String getShortName() {
		return this.shortName;
	}
	public String getTagClass() {return tagClass;}
}
