package com.frc.codex.database;

import java.util.List;
import java.util.UUID;

import com.frc.codex.model.Filing;
import com.frc.codex.model.NewFilingRequest;

public interface DatabaseManager {
	UUID createFiling(NewFilingRequest newFilingRequest);
	Filing getFiling(UUID filingId);
	List<Filing> listFilings();
}
