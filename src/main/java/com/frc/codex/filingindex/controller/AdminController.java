package com.frc.codex.filingindex.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.frc.codex.FilingIndexProperties;
import com.frc.codex.database.DatabaseManager;
import com.frc.codex.discovery.companieshouse.CompaniesHouseClient;
import com.frc.codex.discovery.companieshouse.CompaniesHouseHistoryClient;
import com.frc.codex.discovery.fca.FcaClient;
import com.frc.codex.discovery.fca.FcaFiling;
import com.frc.codex.indexer.Indexer;
import com.frc.codex.indexer.QueueManager;
import com.frc.codex.model.Filing;
import com.frc.codex.model.FilingStatus;
import com.frc.codex.model.NewFilingRequest;

@Controller
public class AdminController {
	private final CompaniesHouseClient companiesHouseClient;
	private final CompaniesHouseHistoryClient companiesHouseHistoryClient;
	private final DatabaseManager databaseManager;
	private final FcaClient fcaClient;
	private final Indexer indexer;
	private final FilingIndexProperties properties;
	private final QueueManager queueManager;

	public AdminController(
			CompaniesHouseClient companiesHouseClient,
			CompaniesHouseHistoryClient companiesHouseHistoryClient,
			DatabaseManager databaseManager,
			FcaClient fcaClient,
			Indexer indexer,
			FilingIndexProperties properties,
			QueueManager queueManager
	) {
		this.companiesHouseClient = companiesHouseClient;
		this.companiesHouseHistoryClient = companiesHouseHistoryClient;
		this.databaseManager = databaseManager;
		this.fcaClient = fcaClient;
		this.indexer = indexer;
		this.properties = properties;
		this.queueManager = queueManager;
	}

	@GetMapping("/admin")
	public String indexPage(Model model) {
		model.addAttribute("awsAccessKeyId", !StringUtils.isEmpty(properties.awsAccessKeyId()));
		model.addAttribute("awsHost", properties.awsHost());
		model.addAttribute("awsRegion", properties.awsRegion());
		model.addAttribute("awsSecretAccessKey", !StringUtils.isEmpty(properties.awsSecretAccessKey()));
		model.addAttribute("chDocumentUrl", properties.companiesHouseDocumentApiBaseUrl());
		model.addAttribute("chInformationUrl", properties.companiesHouseInformationApiBaseUrl());
		model.addAttribute("chRestApiKey", !StringUtils.isEmpty(properties.companiesHouseRestApiKey()));
		model.addAttribute("chStreamApiKey", !StringUtils.isEmpty(properties.companiesHouseStreamApiKey()));
		model.addAttribute("chStreamUrl", properties.companiesHouseStreamApiBaseUrl());
		model.addAttribute("fcaDataApiBaseUrl", properties.fcaDataApiBaseUrl());
		model.addAttribute("fcaSearchApiUrl", properties.fcaSearchApiUrl());
		List<Filing> filings = databaseManager.getFilingsByStatus(FilingStatus.COMPLETED);
		model.addAttribute("filings", filings);
		return "admin/index";
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

	@GetMapping("/admin/smoketest/companieshouse/history")
	public String smokeTestHistoryPage(
			Model model
	) {
		List<URI> dailyLinks = this.companiesHouseHistoryClient.getDailyDownloadLinks();
		List<URI> monthlyLinks = this.companiesHouseHistoryClient.getMonthlyDownloadLinks();
		List<URI> archiveLinks = this.companiesHouseHistoryClient.getArchiveDownloadLinks();
		model.addAttribute("dailyLinks", dailyLinks);
		model.addAttribute("monthlyLinks", monthlyLinks);
		model.addAttribute("archiveLinks", archiveLinks);
		return "admin/smoketest/companieshouse/history";
	}

	/**
	 * This endpoint demonstrates the database client functionality
	 * by loading filing data from the database.
	 */
	@GetMapping("/admin/smoketest/database")
	public ModelAndView smokeTestDatabasePage() {
		ModelAndView model = new ModelAndView("admin/smoketest/database");
		List<Filing> pendingFilings = this.databaseManager.getFilingsByStatus(FilingStatus.PENDING);
		List<Filing> queuedFilings = this.databaseManager.getFilingsByStatus(FilingStatus.QUEUED);
		List<Filing> unprocessedFilings = Stream.concat(pendingFilings.stream(), queuedFilings.stream()).toList();
		model.addObject("unprocessedFilings", unprocessedFilings);
		List<Filing> failedFilings = this.databaseManager.getFilingsByStatus(FilingStatus.FAILED);
		model.addObject("failedFilings", failedFilings);
		List<Filing> completedFilings = this.databaseManager.getFilingsByStatus(FilingStatus.COMPLETED);
		model.addObject("completedFilings", completedFilings);
		model.addObject("newFilingRequest", new NewFilingRequest());
		boolean healthy = completedFilings.size() > 0 && failedFilings.size() == 0;
		model.setStatus(healthy ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
		return model;
	}

	/**
	 * This endpoint demonstrates the database client functionality
	 * by loading filing data from the database.
	 */
	@PostMapping("/admin/smoketest/database")
	public ModelAndView smokeTestDatabaseSubmit(
			@ModelAttribute NewFilingRequest newFilingRequest
	) {
		this.databaseManager.createFiling(newFilingRequest);
		return smokeTestDatabasePage();
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
		LocalDateTime sinceDate = LocalDateTime.now().minusDays(30);
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
