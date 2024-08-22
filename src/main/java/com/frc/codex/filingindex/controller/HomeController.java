package com.frc.codex.filingindex.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.thymeleaf.util.StringUtils;

import com.frc.codex.FilingIndexProperties;

@Controller
public class HomeController {
	private final FilingIndexProperties properties;

	public HomeController(FilingIndexProperties properties) {
		this.properties = properties;
	}

	@GetMapping("/health")
	public ResponseEntity<String> healthPage() {
		return ResponseEntity.ok().build();
	}

	@GetMapping("/")
	public String indexPage(Model model) {
		model.addAttribute("chDocumentUrl", properties.companiesHouseDocumentApiBaseUrl());
		model.addAttribute("chInformationUrl", properties.companiesHouseInformationApiBaseUrl());
		model.addAttribute("chStreamUrl", properties.companiesHouseStreamApiBaseUrl());
		model.addAttribute("chRestApiKey", !StringUtils.isEmpty(properties.companiesHouseRestApiKey()));
		model.addAttribute("chStreamApiKey", !StringUtils.isEmpty(properties.companiesHouseStreamApiKey()));
		return "index";
	}

	@GetMapping("/error")
	public String errorPage() {
		return "error";
	}
}
