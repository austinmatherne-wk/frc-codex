package com.frc.codex.discovery.companieshouse;

import java.sql.Timestamp;

public class CompaniesHouseCompany {
	private final String companyNumber;
	private final String companyName;

	public CompaniesHouseCompany(Builder b) {
		this.companyNumber = b.companyNumber;
		this.companyName = b.companyName;
	}

	public String getCompanyNumber() {
		return companyNumber;
	}

	public String getCompanyName() {
		return companyName;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String companyNumber;
		private String companyName;

		public CompaniesHouseCompany build() {
			return new CompaniesHouseCompany(this);
		}

		public Builder companyNumber(String companyNumber) {
			this.companyNumber = companyNumber;
			return this;
		}

		public Builder companyName(String companyName) {
			this.companyName = companyName;
			return this;
		}
	}
}
