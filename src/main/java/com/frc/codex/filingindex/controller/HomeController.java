package com.frc.codex.filingindex.controller;

import java.time.DateTimeException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import com.frc.codex.FilingIndexProperties;
import com.frc.codex.RegistryCode;
import com.frc.codex.database.DatabaseManager;
import com.frc.codex.model.Filing;
import com.frc.codex.model.HelpRequest;
import com.frc.codex.model.SearchFilingsRequest;
import com.frc.codex.support.SupportManager;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class HomeController {

	private final DatabaseManager databaseManager;
	private final long maximumSearchResults;
	private final long searchPageSize;
	private final SupportManager supportManager;

	public HomeController(
			DatabaseManager databaseManager,
			FilingIndexProperties properties,
			SupportManager supportManager
	) {
		this.databaseManager = databaseManager;
		this.maximumSearchResults = properties.maximumSearchResults();
		this.searchPageSize = properties.searchPageSize();
		this.supportManager = supportManager;
	}

	@GetMapping("/health")
	public ResponseEntity<String> healthPage() {
		return ResponseEntity.ok().build();
	}

	private String getDateValidation(Callable callable) throws Exception {
		try {
			callable.call();
			return null;
		} catch (DateTimeException e) {
			return "Please provide a valid date.";
		}
	}

	@GetMapping("/")
	public ModelAndView indexPage(@ModelAttribute SearchFilingsRequest searchFilingsRequest) throws Exception {
		ModelAndView model = new ModelAndView("index");
		model.addObject("minDocumentDateError", getDateValidation(searchFilingsRequest::getMinDocumentDate));
		model.addObject("minFilingDateError", getDateValidation(searchFilingsRequest::getMinFilingDate));
		model.addObject("maxDocumentDateError", getDateValidation(searchFilingsRequest::getMaxDocumentDate));
		model.addObject("maxFilingDateError", getDateValidation(searchFilingsRequest::getMaxFilingDate));
		model.addObject("registryCodes", RegistryCode.values());
		searchFilingsRequest.setStatus(null);
		List<Filing> filings;
		String message = null;
		boolean maximumResultsReturned = false;
		String moreResultsLink = null;
		if (searchFilingsRequest.isEmpty()) {
			filings = null;
			message = "Please provide a company name and/or number.";
		} else {
			searchFilingsRequest.setLimit(
					Math.max(
							this.searchPageSize,
							Math.min(searchFilingsRequest.getLimit(), this.maximumSearchResults)
					)
			);
			try {
				filings = databaseManager.searchFilings(searchFilingsRequest);
			} catch (DateTimeException e) {
				filings = List.of();
				message = e.getMessage();
			}
			if (filings.isEmpty()) {
				if (message == null) {
					message = "No filings matched your search criteria.";
				}
			} else {
				if (filings.size() >= this.maximumSearchResults) {
					maximumResultsReturned = true;
				} else if (filings.size() >= searchFilingsRequest.getLimit()) {
					moreResultsLink = searchFilingsRequest.getLoadMoreLink(this.searchPageSize, filings.size() - 1);
				}
			}
		}
		model.addObject("filings", filings);
		model.addObject("message", message);
		model.addObject("searchFilingsRequest", searchFilingsRequest);
		model.addObject("maximumResultsReturned", maximumResultsReturned);
		model.addObject("moreResultsLink", moreResultsLink);
		return model;
	}

	@GetMapping("/help")
	public ModelAndView helpPage(HttpServletRequest request) {
		HelpRequest helpRequest = new HelpRequest();
		helpRequest.setReferer(request.getHeader("Referer"));
		ModelAndView model = new ModelAndView("help");
		model.addObject("success", null);
		model.addObject("helpRequest", helpRequest);
		return model;
	}

	@PostMapping("/help")
	public ModelAndView helpPost(HelpRequest helpRequest) {
		UUID id = supportManager.sendHelpRequest(helpRequest);
		ModelAndView model = new ModelAndView("help");
		model.addObject("success", id != null);
		model.addObject("id", id);
		model.addObject("helpRequest", id != null ? new HelpRequest() : helpRequest);
		return model;
	}
}
