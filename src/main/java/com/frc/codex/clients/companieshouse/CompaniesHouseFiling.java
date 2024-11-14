package com.frc.codex.clients.companieshouse;

import java.time.LocalDateTime;

public record CompaniesHouseFiling(
		LocalDateTime actionDate,
		String category,
		String companyNumber,
		LocalDateTime date,
		String eventType,
		String resourceId,
		String resourceKind,
		Long timepoint,
		String transactionId
) {

	public String downloadUrl() {
		return "https://find-and-update.company-information.service.gov.uk/company/"
				+ companyNumber + "/filing-history/" + transactionId
				+ "/document?format=xhtml&download=0";
	}

}
