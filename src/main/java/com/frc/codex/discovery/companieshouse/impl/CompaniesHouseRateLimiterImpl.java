package com.frc.codex.discovery.companieshouse.impl;

import java.sql.Timestamp;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.frc.codex.discovery.companieshouse.CompaniesHouseRateLimiter;

@Component
public class CompaniesHouseRateLimiterImpl implements CompaniesHouseRateLimiter
{
	private static final String HEADER_RATE_LIMIT = "X-Ratelimit-Limit";
	private static final String HEADER_RATE_REMAINING = "X-Ratelimit-Remain";
	private static final String HEADER_RATE_RESET = "X-Ratelimit-Reset";
	private static final Logger LOG = LoggerFactory.getLogger(CompaniesHouseRateLimiterImpl.class);
	private static final int RAPID_RATE_LIMIT = 20; // 600 requests per 5 minutes = 2 requests / second = 20 requests / 10 seconds
	private static final int RAPID_RATE_WINDOW = 10 * 1000; // 10 seconds


	private long limit;
	private long remaining;
	private Timestamp updated;
	private final Queue<Timestamp> timestamps;
	private Timestamp reset;
	private Timestamp lastRejection;

	public CompaniesHouseRateLimiterImpl() {
		this.timestamps = new ConcurrentLinkedQueue<>();
	}

	public long getLimit() {
		return limit;
	}

	public long getRemaining() {
		return remaining;
	}

	public Timestamp getUpdatedTimestamp() {
		return updated;
	}

	public Timestamp getResetTimestamp() {
		return reset;
	}

	public boolean isHealthy() {
		try {
			waitForRapidRateLimit();
		} catch (InterruptedException e) {
			return false;
		}
		if (updated == null || reset == null) {
			LOG.info("Rate limit status unknown, assuming healthy.");
			return true;
		};
		Timestamp now = new Timestamp(System.currentTimeMillis());
		if (now.after(reset)) {
			// The limits should have been reset since last update, so we should be healthy.
			return true;
		}
		if (lastRejection != null && lastRejection.before(reset)) {
			// We have been rejected in the current window, so we should be unhealthy.
			LOG.info("Rate limit rejection at {} in current window until {}, unhealthy.", lastRejection, reset);
			return false;
		}
		if (remaining <= 0) {
			// We have no remaining requests, so we should be unhealthy.
			LOG.info("Rate limit exhausted, unhealthy.");
			return false;
		}
		// We have remaining requests, so we should be healthy.
		return true;
	}

	public void notifyRejection() {
		lastRejection = new Timestamp(System.currentTimeMillis());
	}

	private void registerTimestamp() {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		timestamps.add(now);

		// Remove timestamps older than one minute
		Timestamp oneMinuteAgo = new Timestamp(now.getTime() - RAPID_RATE_WINDOW);
		while (!timestamps.isEmpty() && timestamps.peek().before(oneMinuteAgo)) {
			timestamps.poll();
		}
	}

	public void waitForRapidRateLimit() throws InterruptedException {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		if (timestamps.size() >= RAPID_RATE_LIMIT) {
			Timestamp oldest = timestamps.peek();
			long waitTime = RAPID_RATE_WINDOW - (now.getTime() - oldest.getTime());
			if (waitTime > 0) {
				LOG.info("Waiting for rapid rate limit: {} ms", waitTime);
				Thread.sleep(waitTime);
			}
		}
	}

	public void updateLimits(ResponseEntity<?> response, String url) {
		registerTimestamp();
		HttpHeaders headers = response.getHeaders();
		String limit = headers.getFirst(HEADER_RATE_LIMIT);
		if (limit != null) {
			this.limit = Long.parseLong(limit);
		}
		String remaining = headers.getFirst(HEADER_RATE_REMAINING);
		String reset = headers.getFirst(HEADER_RATE_RESET);
		if (remaining != null && reset != null) {
			this.remaining = Long.parseLong(remaining);
			this.reset = new Timestamp(Long.parseLong(reset) * 1000);
			this.updated = new Timestamp(headers.getDate());
		}
		LOG.info("Updated CH rate limit details: url={}, limit={}, remaining={}, reset={}, updated={}", url, this.limit, this.remaining, this.reset, this.updated);
	}
}
