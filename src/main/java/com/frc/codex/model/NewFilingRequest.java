package com.frc.codex.model;

import java.time.LocalDateTime;

public class NewFilingRequest {
	private String companyName;
	private String companyNumber;
	private String registryCode;
	private String downloadUrl;
	private LocalDateTime filingDate;
	private Long streamTimepoint;

	public String getCompanyName() {
		return companyName;
	}

	public String getCompanyNumber() {
		return companyNumber;
	}

	public String getRegistryCode() {
		return registryCode;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public LocalDateTime getFilingDate() {
		return filingDate;
	}

	public Long getStreamTimepoint() {
		return streamTimepoint;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public void setCompanyNumber(String companyNumber) {
		this.companyNumber = companyNumber;
	}

	public void setRegistryCode(String registryCode) {
		this.registryCode = registryCode;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public void setFilingDate(LocalDateTime filingDate) {
		this.filingDate = filingDate;
	}

	public void setStreamTimepoint(Long streamTimepoint) {
		this.streamTimepoint = streamTimepoint;
	}
}
