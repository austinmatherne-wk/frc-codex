package com.frc.codex.model;

import java.util.UUID;

public class FilingResultRequest {
	private final UUID filingId;
	private final String stubViewerUrl;
	private final boolean success;

	private FilingResultRequest(Builder builder) {
		this.filingId = builder.filingId;
		this.stubViewerUrl = builder.stubViewerUrl;
		this.success = builder.success;
	}

	public static Builder builder() {
		return new Builder();
	}

	public UUID getFilingId() {
		return filingId;
	}

	public String getStubViewerUrl() {
		return stubViewerUrl;
	}

	public String getStatus() {
		return success ? "completed" : "failed";
	}

	public static class Builder {
		private UUID filingId;
		private String stubViewerUrl;
		private boolean success;

		public Builder filingId(UUID filingId) {
			this.filingId = filingId;
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
