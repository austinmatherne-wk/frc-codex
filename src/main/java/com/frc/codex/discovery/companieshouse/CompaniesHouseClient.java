package com.frc.codex.discovery.companieshouse;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface CompaniesHouseClient {
	String getCompany(String companyNumber);

	Set<String> getCompanyFilingUrls(String companyNumber, String filingId) throws JsonProcessingException;

	String getCompanyFilingHistory(String companyNumber);

	Set<String> getCompanyFilingUrls(String companyNumber) throws JsonProcessingException;

	List<String> streamFilings(long maxMs) throws IOException;

	void streamFilings(Function<String, Boolean> callback) throws IOException;
}
