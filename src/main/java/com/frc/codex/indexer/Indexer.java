package com.frc.codex.indexer;

public interface Indexer {
	String getStatus();
	void indexFca();
	void processResults();
	void queueJobs();
}
