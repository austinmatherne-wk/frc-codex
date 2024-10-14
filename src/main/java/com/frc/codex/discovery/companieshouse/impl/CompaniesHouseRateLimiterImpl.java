package com.frc.codex.discovery.companieshouse.impl;

import java.sql.Timestamp;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.frc.codex.discovery.companieshouse.CompaniesHouseConfig;
import com.frc.codex.discovery.companieshouse.CompaniesHouseRateLimiter;

@Component
public class CompaniesHouseRateLimiterImpl implements CompaniesHouseRateLimiter
{
	private static final String HEADER_RATE_LIMIT = "X-Ratelimit-Limit";
	private static final String HEADER_RATE_REMAINING = "X-Ratelimit-Remain";
	private static final String HEADER_RATE_RESET = "X-Ratelimit-Reset";
	private static final Logger LOG = LoggerFactory.getLogger(CompaniesHouseRateLimiterImpl.class);
	private static final int REMAINING_FLOOR = 25;

	private long limit;
	private long remaining;
	private Timestamp updated;
	private final Queue<Timestamp> timestamps;
	private final int rapidRateLimit;
	private final int rapidRateWindow;
	private Timestamp reset;
	private Timestamp lastRejection;

	public CompaniesHouseRateLimiterImpl(CompaniesHouseConfig config) {
		this.rapidRateLimit = config.rapidRateLimit();
		this.rapidRateWindow = config.rapidRateWindow();
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
			if (lastRejection != null && lastRejection.before(reset)) {
				// We no longer care about the last rejection from the previous window.
				lastRejection = null;
			}
			// The limits should have been reset since last update, so we should be healthy.
			return true;
		}
		if (lastRejection != null && lastRejection.before(reset)) {
			// We have been rejected in the current window, so we should be unhealthy.
			LOG.info("Rate limit rejection at {} in current window until {}, unhealthy.", lastRejection, reset);
			return false;
		}
		if (remaining <= REMAINING_FLOOR) {
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
		Timestamp oneMinuteAgo = new Timestamp(now.getTime() - rapidRateWindow);
		while (!timestamps.isEmpty() && timestamps.peek().before(oneMinuteAgo)) {
			timestamps.poll();
		}
	}

	public String toString() {
		return "CompaniesHouseRateLimiterImpl [" +
				"limit=" + limit + ", " +
				"remaining=" + remaining + ", " +
				"updated=" + updated + ", " +
				"reset=" + reset + ", " +
				"lastRejection=" + lastRejection +
				"]";
	}

	public synchronized void waitForRapidRateLimit() throws InterruptedException {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		if (timestamps.size() >= rapidRateLimit) {
			Timestamp oldest = timestamps.peek();
			if (oldest == null) {
				return;
			}
			long waitTime = rapidRateWindow - (now.getTime() - oldest.getTime());
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
		LOG.debug("Updated CH rate limit details: url={}, limit={}, remaining={}, reset={}, updated={}", url, this.limit, this.remaining, this.reset, this.updated);
	}
}
