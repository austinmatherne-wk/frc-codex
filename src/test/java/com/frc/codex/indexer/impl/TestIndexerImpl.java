package com.frc.codex.indexer.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.frc.codex.indexer.Indexer;

@Component
@Profile("test")
public class TestIndexerImpl implements Indexer {
	@Override
	public String getStatus() {
		return "Healthy";
	}

	@Override
	public void indexFca() {

	}

	@Override
	public void processResults() {

	}

	@Override
	public void queueJobs() {

	}
}

