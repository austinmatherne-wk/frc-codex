package com.frc.codex.discovery.companieshouse.impl;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.frc.codex.discovery.companieshouse.CompaniesHouseClient;

@Component
@Profile("test")
public class TestCompaniesHouseClientImpl implements CompaniesHouseClient {

	public String getCompany(String companyNumber) {
		return null;
	}

	public Set<String> getCompanyFilingUrls(String companyNumber, String filingId) {
		return null;
	}

	public String getCompanyFilingHistory(String companyNumber) {
		return null;
	}

	public Set<String> getCompanyFilingUrls(String companyNumber) {
		return null;
	}

	public void streamFilings(Long timepoint, Function<String, Boolean> callback) {

	}
}
