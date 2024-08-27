package com.frc.codex.filingindex.controller;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.frc.codex.FilingIndexProperties;
import com.frc.codex.database.DatabaseManager;
import com.frc.codex.discovery.companieshouse.CompaniesHouseClient;
import com.frc.codex.discovery.companieshouse.impl.CompaniesHouseClientImpl;
import com.frc.codex.discovery.companieshouse.impl.CompaniesHouseConfigImpl;
import com.frc.codex.model.Filing;
import com.frc.codex.model.NewFilingRequest;

@Controller
public class AdminController {
	private final CompaniesHouseClient companiesHouseClient;
	private final DatabaseManager databaseManager;

	public AdminController(FilingIndexProperties properties, DatabaseManager databaseManager) {
		this.companiesHouseClient = new CompaniesHouseClientImpl(new CompaniesHouseConfigImpl(properties));
		this.databaseManager = databaseManager;
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

	/**
	 * This endpoint demonstrates the database client functionality
	 * by loading filing data from the database.
	 */
	@GetMapping("/admin/smoketest/database")
	public String smokeTestDatabase(Model model) {
		List<Filing> filings = this.databaseManager.listFilings();
		model.addAttribute("filings", filings);
		model.addAttribute("newFilingRequest", new NewFilingRequest());
		return "admin/smoketest/database";
	}

	/**
	 * This endpoint demonstrates the database client functionality
	 * by loading filing data from the database.
	 */
	@PostMapping("/admin/smoketest/database")
	public String smokeTestDatabaseSubmit(
			@ModelAttribute NewFilingRequest newFilingRequest,
			Model model
	) {
		this.databaseManager.createFiling(newFilingRequest);
		return smokeTestDatabase(model);
	}
}
