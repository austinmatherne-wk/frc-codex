package com.frc.codex.discovery.companieshouse;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public interface CompaniesHouseHistoryClient {
	void downloadArchive(URI uri, Path outputFilePath) throws IOException;
	List<URI> getArchiveDownloadLinks();
	List<URI> getDailyDownloadLinks();
	List<URI> getMonthlyDownloadLinks();
}
