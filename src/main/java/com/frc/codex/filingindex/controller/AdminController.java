package com.frc.codex.filingindex.controller;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.frc.codex.database.DatabaseManager;
import com.frc.codex.discovery.companieshouse.CompaniesHouseClient;
import com.frc.codex.discovery.fca.FcaClient;
import com.frc.codex.discovery.fca.FcaFiling;
import com.frc.codex.indexer.Indexer;
import com.frc.codex.indexer.QueueManager;
import com.frc.codex.model.Filing;
import com.frc.codex.model.NewFilingRequest;

@Controller
public class AdminController {
	private final CompaniesHouseClient companiesHouseClient;
	private final DatabaseManager databaseManager;
	private final FcaClient fcaClient;
	private final Indexer indexer;
	private final QueueManager queueManager;

	public AdminController(
			CompaniesHouseClient companiesHouseClient,
			DatabaseManager databaseManager,
			FcaClient fcaClient,
			Indexer indexer,
			QueueManager queueManager
	) {
		this.companiesHouseClient = companiesHouseClient;
		this.databaseManager = databaseManager;
		this.fcaClient = fcaClient;
		this.indexer = indexer;
		this.queueManager = queueManager;
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
	 * This endpoint demonstrates the database client functionality
	 * by loading filing data from the database.
	 */
	@GetMapping("/admin/smoketest/database")
	public String smokeTestDatabasePage(Model model) {
		List<Filing> filings = this.databaseManager.getPendingFilings();
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
		return smokeTestDatabasePage(model);
	}

	/**
	 * This endpoint demonstrates progress of the indexer by showing
	 * its progress in discovering filings.
	 */
	@GetMapping("/admin/smoketest/indexer")
	public ModelAndView smokeTestIndexerPage() {
		ModelAndView model = new ModelAndView("admin/smoketest/indexer");
		String indexerStatus = indexer.getStatus();
		model.addObject("indexerStatus", indexerStatus);
		boolean healthy = indexerStatus.contains("Healthy");
		model.setStatus(healthy ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
		return model;
	}

	/*
	 * This endpoint demonstrates the FCA client functionality
	 * by loading the last week's worth of filings.
	 */
	@GetMapping("/admin/smoketest/fca")
	public ModelAndView smokeTestFcaPage() {
		ModelAndView model = new ModelAndView("admin/smoketest/fca");
		Date sinceDate = new Date(new Date().getTime() - 30L * 24 * 60 * 60 * 1000);
		List<FcaFiling> filings = this.fcaClient.fetchAllSinceDate(sinceDate);
		model.addObject("sinceDate", sinceDate);
		model.addObject("filings", filings);
		boolean healthy = filings.size() > 0;
		model.setStatus(healthy ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
		return model;
	}

	/*
	 * This endpoint demonstrates progress of the indexer and processor by showing
	 * statistics of the associated SQS queues.
	 */
	@GetMapping("/admin/smoketest/queue")
	public ModelAndView smokeTestSqsPage() {
		ModelAndView model = new ModelAndView("admin/smoketest/queue");
		String queueStatus = queueManager.getStatus();
		model.addObject("queueStatus", queueStatus);
		return model;
	}
}
