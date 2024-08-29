package com.frc.codex.model;

import java.util.UUID;

public class FilingResultRequest {
	private UUID filingId;
	private String stubViewerUrl;

	private FilingResultRequest(Builder builder) {
		this.filingId = builder.filingId;
		this.stubViewerUrl = builder.stubViewerUrl;
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

	public static class Builder {
		private UUID filingId;
		private String stubViewerUrl;

		public Builder filingId(UUID filingId) {
			this.filingId = filingId;
			return this;
		}

		public Builder stubViewerUrl(String stubViewerUrl) {
			this.stubViewerUrl = stubViewerUrl;
			return this;
		}

		public FilingResultRequest build() {
			return new FilingResultRequest(this);
		}
	}
}
