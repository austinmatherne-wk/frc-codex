package com.frc.codex.indexer;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.frc.codex.model.Filing;
import com.frc.codex.model.FilingResultRequest;

public interface QueueManager {
	void addJobs(List<Filing> filings, Consumer<Filing> callback);
	String getStatus();
	void processResults(Function<FilingResultRequest, Boolean> callback);
}
