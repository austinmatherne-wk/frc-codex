package com.frc.codex.model;

import java.time.LocalDateTime;

import org.thymeleaf.util.StringUtils;

public class SearchFilingsRequest {
	private String companyName;
	private String companyNumber;
	private String status;

	public String getCompanyName() {
		return companyName;
	}

	public String getCompanyNumber() {
		return companyNumber;
	}

	public String getStatus() {
		return status;
	}

	public boolean isEmpty() {
		return StringUtils.isEmpty(companyName) && StringUtils.isEmpty(companyNumber);
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public void setCompanyNumber(String companyNumber) {
		this.companyNumber = companyNumber;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
