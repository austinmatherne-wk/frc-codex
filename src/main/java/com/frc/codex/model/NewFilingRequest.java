package com.frc.codex.model;

public class NewFilingRequest {
	private String registryCode;
	private String downloadUrl;
	private Long streamTimepoint;

	public String getRegistryCode() {
		return registryCode;
	}

	public String getDownloadUrl() {
		return downloadUrl;
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

	public void setStreamTimepoint(Long streamTimepoint) {
		this.streamTimepoint = streamTimepoint;
	}
}
