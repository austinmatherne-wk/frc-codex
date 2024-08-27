package com.frc.codex.filingindex.controller;

import java.io.IOException;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.frc.codex.FilingIndexProperties;
import com.frc.codex.discovery.companieshouse.CompaniesHouseClient;
import com.frc.codex.discovery.companieshouse.impl.CompaniesHouseClientImpl;
import com.frc.codex.discovery.companieshouse.impl.CompaniesHouseConfigImpl;

@Controller
public class AdminController {
	private final CompaniesHouseClient companiesHouseClient;

	public AdminController(FilingIndexProperties properties) {
		this.companiesHouseClient = new CompaniesHouseClientImpl(new CompaniesHouseConfigImpl(properties));
	}

	/**
	 * This endpoint demonstrates the Companies House client functionality
	 * by loading a company's information JSON.
	 */
	@GetMapping("/admin/smoketest/companieshouse/company/{companyNumber}")
	public String smokeTestCompanyPage(
			Model model,
			@PathVariable("companyNumber") String companyNumber
	) throws JsonProcessingException {
		String company = this.companiesHouseClient.getCompany(companyNumber);
		model.addAttribute("company", company);
		Set<String> filingUrls = this.companiesHouseClient.getCompanyFilingUrls(companyNumber);
		model.addAttribute("filingUrls", String.join("\n", filingUrls));
		return "admin/smoketest/companieshouse/company";
	}

	/**
	 * This endpoint demonstrates the Companies House client functionality
	 * by capturing at least 10 seconds and at least 1 event from the
	 * filing stream.
	 */
	@GetMapping("/admin/smoketest/companieshouse/stream")
	public String smokeTestFilingStreamPage(Model model) {
		String stream;
		try {
			stream = String.join("\n", this.companiesHouseClient.streamFilings(10 * 1000));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		model.addAttribute("stream", stream);
		return "admin/smoketest/companieshouse/stream";
	}
}
