package com.frc.codex.filingindex.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

import com.frc.codex.FilingIndexProperties;
import com.frc.codex.database.DatabaseManager;
import com.frc.codex.model.Filing;
import com.frc.codex.model.FilingStatus;
import com.frc.codex.model.SearchFilingsRequest;

@Controller
public class HomeController {

	private final DatabaseManager databaseManager;
	private final long maximumSearchResults;
	private final long searchPageSize;

	public HomeController(
			DatabaseManager databaseManager,
			FilingIndexProperties properties
	) {
		this.databaseManager = databaseManager;
		this.maximumSearchResults = properties.maximumSearchResults();
		this.searchPageSize = properties.searchPageSize();
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
		boolean maximumResultsReturned = false;
		String moreResultsLink = null;
		if (searchFilingsRequest.isEmpty()) {
			filings = null;
			message = "Please provide at least one search criterion.";
		} else {
			searchFilingsRequest.setLimit(
					Math.max(
							this.searchPageSize,
							Math.min(searchFilingsRequest.getLimit(), this.maximumSearchResults)
					)
			);
			filings = databaseManager.searchFilings(searchFilingsRequest);
			if (filings.isEmpty()) {
				message = "No filings matched your search criteria.";
			} else {
				message = null;
				if (filings.size() >= this.maximumSearchResults) {
					maximumResultsReturned = true;
				} else if (filings.size() >= searchFilingsRequest.getLimit()) {
					moreResultsLink = "/?companyName=" + searchFilingsRequest.getCompanyName() +
							"&companyNumber=" + searchFilingsRequest.getCompanyNumber() +
							"&limit=" + (searchFilingsRequest.getLimit() + this.searchPageSize) +
							"#result-" + (filings.size() - 1);
				}
			}
		}
		ModelAndView model = new ModelAndView("index");
		model.addObject("filings", filings);
		model.addObject("message", message);
		model.addObject("searchFilingsRequest", searchFilingsRequest);
		model.addObject("maximumResultsReturned", maximumResultsReturned);
		model.addObject("moreResultsLink", moreResultsLink);
		return model;
	}
}
