package com.frc.codex.filingindex.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

import com.frc.codex.database.DatabaseManager;
import com.frc.codex.model.Filing;
import com.frc.codex.model.FilingStatus;
import com.frc.codex.model.SearchFilingsRequest;

@Controller
public class HomeController {
	private final DatabaseManager databaseManager;

	public HomeController(
			DatabaseManager databaseManager
	) {
		this.databaseManager = databaseManager;
	}

	@GetMapping("/health")
	public ResponseEntity<String> healthPage() {
		return ResponseEntity.ok().build();
	}

	@GetMapping("/")
	public ModelAndView indexPage(@ModelAttribute SearchFilingsRequest searchFilingsRequest) {
		searchFilingsRequest.setStatus(FilingStatus.COMPLETED.toString());
		List<Filing> filings;
		String message;
		if (searchFilingsRequest.isEmpty()) {
			filings = null;
			message = "Please provide at least one search criterion.";
		} else {
			filings = databaseManager.searchFilings(searchFilingsRequest);
			if (filings.isEmpty()) {
				message = "No filings matched your search criteria.";
			} else {
				message = null;
			}
		}
		ModelAndView model = new ModelAndView("index");
		model.addObject("filings", filings);
		model.addObject("message", message);
		model.addObject("searchFilingsRequest", searchFilingsRequest);
		return model;
	}
}
