package com.frc.codex.tools;

import java.sql.Timestamp;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RateLimiter
{
	private static final Logger LOG = LoggerFactory.getLogger(RateLimiter.class);
	protected final Queue<Timestamp> timestamps;
	protected final int rapidRateLimit;
	protected final int rapidRateWindow;

	public RateLimiter(int rapidRateLimit, int rapidRateWindow) {
		this.rapidRateLimit = rapidRateLimit;
		this.rapidRateWindow = rapidRateWindow;
		this.timestamps = new ConcurrentLinkedQueue<>();
	}

	public void registerTimestamp() {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		timestamps.add(now);

		// Remove timestamps older than one minute
		Timestamp oneMinuteAgo = new Timestamp(now.getTime() - rapidRateWindow);
		while (!timestamps.isEmpty() && timestamps.peek().before(oneMinuteAgo)) {
			timestamps.poll();
		}
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
}
