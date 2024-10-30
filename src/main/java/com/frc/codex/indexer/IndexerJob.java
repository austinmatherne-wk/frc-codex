package com.frc.codex.indexer;

import java.io.IOException;
import java.util.function.Supplier;

public interface IndexerJob {
	String getStatus();
	boolean isHealthy();
	void run(Supplier<Boolean> continueCallback) throws IOException;
}
