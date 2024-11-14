package com.frc.codex.clients.companieshouse;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.frc.codex.model.NewFilingRequest;

public interface CompaniesHouseClient {
	boolean filterCategory(String category);
	CompaniesHouseCompany getCompany(String companyNumber) throws JsonProcessingException;
	CompaniesHouseFiling getFiling(String companyNumber, String transactionId) throws JsonProcessingException;
	List<NewFilingRequest> getCompanyFilings(String companyNumber, String companyName) throws JsonProcessingException;
	Set<String> getCompanyFilingUrls(String companyNumber, String filingId) throws JsonProcessingException;
	boolean isEnabled();
	void streamFilings(Long timepoint, Function<CompaniesHouseFiling, Boolean> callback) throws IOException;
}