package com.frc.codex.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

public class FilingResultRequest {
	private final String companyName;
	private final String companyNumber;
	private final LocalDateTime documentDate;
	private final String error;
	private final UUID filingId;
	private final String logs;
	private final String stubViewerUrl;
	private final boolean success;

	private FilingResultRequest(Builder builder) {
		this.companyName = builder.companyName;
		this.companyNumber = builder.companyNumber;
		this.documentDate = builder.documentDate;
		this.error = builder.error;
		this.filingId = builder.filingId;
		this.logs = builder.logs;
		this.stubViewerUrl = builder.stubViewerUrl;
		this.success = builder.success;
	}

	public static Builder builder() {
		return new Builder();
	}

	public String getCompanyName() {
		return companyName;
	}

	public String getCompanyNumber() {
		return companyNumber;
	}

	public LocalDateTime getDocumentDate() {
		return documentDate;
	}

	public String getError() {
		return error;
	}

	public UUID getFilingId() {
		return filingId;
	}

	public String getLogs() {
		return logs;
	}

	public String getStubViewerUrl() {
		return stubViewerUrl;
	}

	public FilingStatus getStatus() {
		return success ? FilingStatus.COMPLETED : FilingStatus.FAILED;
	}

	public static class Builder {
		private String companyName;
		private String companyNumber;
		private LocalDateTime documentDate;
		private String error;
		private UUID filingId;
		private String logs;
		private String stubViewerUrl;
		private boolean success;

		public Builder companyName(String companyName) {
			this.companyName = companyName;
			return this;
		}

		public Builder companyNumber(String companyNumber) {
			this.companyNumber = companyNumber;
			return this;
		}

		public Builder documentDate(LocalDateTime documentDate) {
			this.documentDate = documentDate;
			return this;
		}

		public Builder error(String error) {
			this.error = error;
			return this;
		}

		public Builder filingId(UUID filingId) {
			this.filingId = filingId;
			return this;
		}

		public Builder logs(String logs) {
			this.logs = logs;
			return this;
		}

		public Builder stubViewerUrl(String stubViewerUrl) {
			this.stubViewerUrl = stubViewerUrl;
			return this;
		}

		public Builder success(boolean success) {
			this.success = success;
			return this;
		}

		public FilingResultRequest build() {
			return new FilingResultRequest(this);
		}
	}
}
