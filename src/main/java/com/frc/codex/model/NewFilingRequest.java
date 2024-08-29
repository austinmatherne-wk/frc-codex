package com.frc.codex.model;

import java.util.Date;

public class NewFilingRequest {
	private String registryCode;
	private String downloadUrl;
	private Date filingDate;
	private Long streamTimepoint;

	public String getRegistryCode() {
		return registryCode;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public Date getFilingDate() {
		return filingDate;
	}

	public Long getStreamTimepoint() {
		return streamTimepoint;
	}

	public void setRegistryCode(String registryCode) {
		this.registryCode = registryCode;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public void setFilingDate(Date filingDate) {
		this.filingDate = filingDate;
	}

	public void setStreamTimepoint(Long streamTimepoint) {
		this.streamTimepoint = streamTimepoint;
	}
}
