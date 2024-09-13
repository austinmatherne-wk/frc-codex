package com.frc.codex.discovery.companieshouse;

import java.net.URI;
import java.util.List;

public interface CompaniesHouseHistoryClient {
	List<URI> getArchiveDownloadLinks();
	List<URI> getDailyDownloadLinks();
	List<URI> getMonthlyDownloadLinks();
}
