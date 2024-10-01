package com.frc.codex.model;

import java.sql.Timestamp;

public class Company {
	private final String companyNumber;
	private final Timestamp discoveredDate;
	private final Timestamp completedDate;
	private final String companyName;

	public Company(Builder b) {
		this.companyNumber = b.companyNumber;
		this.discoveredDate = b.discoveredDate;
		this.completedDate = b.completedDate;
		this.companyName = b.companyName;
	}

	public String getCompanyNumber() {
		return companyNumber;
	}

	public Timestamp getDiscoveredDate() {
		return discoveredDate;
	}

	public Timestamp getCompletedDate() {
		return completedDate;
	}

	public String getCompanyName() {
		return companyName;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String companyNumber;
		private Timestamp discoveredDate;
		private Timestamp completedDate;
		private String companyName;

		public Company build() {
			return new Company(this);
		}

		public Builder companyNumber(String companyNumber) {
			this.companyNumber = companyNumber;
			return this;
		}

		public Builder discoveredDate(Timestamp discoveredDate) {
			this.discoveredDate = discoveredDate;
			return this;
		}

		public Builder completedDate(Timestamp completedDate) {
			this.completedDate = completedDate;
			return this;
		}

		public Builder companyName(String companyName) {
			this.companyName = companyName;
			return this;
		}
	}
}
