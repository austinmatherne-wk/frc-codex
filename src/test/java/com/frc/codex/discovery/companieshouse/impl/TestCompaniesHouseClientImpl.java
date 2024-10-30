package com.frc.codex.discovery.companieshouse.impl;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.frc.codex.discovery.companieshouse.CompaniesHouseClient;
import com.frc.codex.discovery.companieshouse.CompaniesHouseCompany;
import com.frc.codex.discovery.companieshouse.CompaniesHouseFiling;
import com.frc.codex.model.NewFilingRequest;

@Component
@Profile("test")
public class TestCompaniesHouseClientImpl implements CompaniesHouseClient {
	public boolean filterCategory(String category) {
		return true;
	}

	public CompaniesHouseCompany getCompany(String companyNumber) {
		return null;
	}

	public CompaniesHouseFiling getFiling(String companyNumber, String transactionId) {
		return null;
	}

	public List<NewFilingRequest> getCompanyFilings(String companyNumber, String companyName) {
		return null;
	}

	public Set<String> getCompanyFilingUrls(String companyNumber, String filingId) {
		return null;
	}

	public boolean isEnabled() {
		return true;
	}

	public void streamFilings(Long timepoint, Function<CompaniesHouseFiling, Boolean> callback) {

	}
}
