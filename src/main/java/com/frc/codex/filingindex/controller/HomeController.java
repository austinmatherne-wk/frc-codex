package com.frc.codex.filingindex.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.thymeleaf.util.StringUtils;

import com.frc.codex.FilingIndexProperties;
import com.frc.codex.database.DatabaseManager;
import com.frc.codex.model.Filing;
import com.frc.codex.model.FilingStatus;

@Controller
public class HomeController {
	private final DatabaseManager databaseManager;
	private final FilingIndexProperties properties;

	public HomeController(
			DatabaseManager databaseManager,
			FilingIndexProperties properties
	) {
		this.databaseManager = databaseManager;
		this.properties = properties;
	}

	@GetMapping("/health")
	public ResponseEntity<String> healthPage() {
		return ResponseEntity.ok().build();
	}

	@GetMapping("/")
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
		return "index";
	}

	@GetMapping("/error")
	public String errorPage() {
		return "error";
	}
}
