package com.frc.codex.model.companieshouse;

import java.net.URI;
import java.sql.Timestamp;

public class CompaniesHouseArchive {
	private final String filename;
	private final URI uri;
	private final String archiveType;
	private final Timestamp completedDate;

	public CompaniesHouseArchive(Builder b) {
		this.filename = b.filename;
		this.uri = b.uri;
		this.archiveType = b.archiveType;
		this.completedDate = b.completedDate;
	}

	public String getFilename() {
		return filename;
	}

	public URI getUri() {
		return uri;
	}

	public String getArchiveType() {
		return archiveType;
	}

	public Timestamp getCompletedDate() {
		return completedDate;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String filename;
		private URI uri;
		private String archiveType;
		private Timestamp completedDate;

		public CompaniesHouseArchive build() {
			return new CompaniesHouseArchive(this);
		}

		public Builder filename(String filename) {
			this.filename = filename;
			return this;
		}

		public Builder uri(URI uri) {
			this.uri = uri;
			return this;
		}

		public Builder uri(String uri) {
			this.uri = URI.create(uri);
			return this;
		}

		public Builder archiveType(String archiveType) {
			this.archiveType = archiveType;
			return this;
		}

		public Builder completedDate(Timestamp completedDate) {
			this.completedDate = completedDate;
			return this;
		}
	}
}
