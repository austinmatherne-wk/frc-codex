package com.frc.codex.database;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.frc.codex.RegistryCode;
import com.frc.codex.model.Filing;
import com.frc.codex.model.FilingResultRequest;
import com.frc.codex.model.FilingStatus;
import com.frc.codex.model.NewFilingRequest;

public interface DatabaseManager {
	void applyFilingResult(FilingResultRequest filingResultRequest);
	UUID createFiling(NewFilingRequest newFilingRequest);
	boolean filingExists(NewFilingRequest newFilingRequest);
	Filing getFiling(UUID filingId);
	List<Filing> getFilingsByStatus(FilingStatus status);
	Date getLatestFcaFilingDate(Date defaultDate);
	Long getLatestStreamTimepoint(Long defaultTimepoint);
	long getRegistryCount(RegistryCode registryCode);
	void updateFilingStatus(UUID filingId, String status);
}
