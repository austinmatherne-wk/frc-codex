package com.frc.codex.discovery.companieshouse;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.frc.codex.model.Company;
import com.frc.codex.model.NewFilingRequest;

public interface CompaniesHouseClient {
	Company getCompany(String companyNumber) throws JsonProcessingException;
	List<NewFilingRequest> getCompanyFilings(String companyNumber, String companyName) throws JsonProcessingException;
	Set<String> getCompanyFilingUrls(String companyNumber, String filingId) throws JsonProcessingException;
	boolean isEnabled();
	void streamFilings(Long timepoint, Function<String, Boolean> callback) throws IOException;
}
