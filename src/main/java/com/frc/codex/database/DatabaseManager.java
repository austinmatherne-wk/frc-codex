package com.frc.codex.database;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.frc.codex.RegistryCode;
import com.frc.codex.model.Filing;
import com.frc.codex.model.FilingResultRequest;
import com.frc.codex.model.FilingStatus;
import com.frc.codex.model.NewFilingRequest;
import com.frc.codex.model.SearchFilingsRequest;
import com.frc.codex.model.companieshouse.CompaniesHouseArchive;

public interface DatabaseManager {
	void applyFilingResult(FilingResultRequest filingResultRequest);
	boolean companiesHouseArchiveExists(String filename);
	String createCompaniesHouseArchive(CompaniesHouseArchive archive);
	UUID createFiling(NewFilingRequest newFilingRequest);
	boolean filingExists(NewFilingRequest newFilingRequest);
	Filing getFiling(UUID filingId);
	List<Filing> getFilingsByStatus(FilingStatus status);
	LocalDateTime getLatestFcaFilingDate(LocalDateTime defaultDate);
	Long getLatestStreamTimepoint(Long defaultTimepoint);
	long getRegistryCount(RegistryCode registryCode);
	List<Filing> searchFilings(SearchFilingsRequest searchFilingsRequest);
	void updateFilingStatus(UUID filingId, String status);
}
