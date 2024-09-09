package com.frc.codex.model;

import java.util.UUID;

public class FilingResultRequest {
	private final String error;
	private final UUID filingId;
	private final String logs;
	private final String stubViewerUrl;
	private final boolean success;

	private FilingResultRequest(Builder builder) {
		this.error = builder.error;
		this.filingId = builder.filingId;
		this.logs = builder.logs;
		this.stubViewerUrl = builder.stubViewerUrl;
		this.success = builder.success;
	}

	public static Builder builder() {
		return new Builder();
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
		private String error;
		private UUID filingId;
		private String logs;
		private String stubViewerUrl;
		private boolean success;

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
