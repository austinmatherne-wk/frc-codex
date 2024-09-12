package com.frc.codex.model;

import org.thymeleaf.util.StringUtils;

public class SearchFilingsRequest {
	private String companyName;
	private String companyNumber;
	private long limit;
	private String status;

	public String getCompanyName() {
		return companyName;
	}

	public String getCompanyNumber() {
		return companyNumber;
	}

	public long getLimit() {
		return limit;
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

	public void setLimit(long limit) {
		this.limit = limit;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
