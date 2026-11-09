/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.util.concurrent.TimeUnit;
/**
 * Simple rate limiter using token bucket pattern.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * RateLimiter limiter = RateLimiter.ofRate(10.0); // 10 calls/sec
 *
 * if (limiter.tryAcquire()) {
 * 	doWork();
 * }
 *
 * // Or blocking:
 * limiter.acquire(); // waits if needed
 * doWork();
 * }</pre>
 *
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/RateLimiter.java}
 * blob {@code 6b29b66e3269438e069ba99b9a40412bc30da5ad} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class RateLimiter {
    private static final PlaceholderLogger log = PlaceholderLogger.of(RateLimiter.class);


    /** Milliseconds in a second. */
    private static final long MILLIS_PER_SECOND = 1000;

    private final long intervalNs;
    private long lastTime;

    RateLimiter(long intervalMs) {
        this.intervalNs = TimeUnit.MILLISECONDS.toNanos(intervalMs);
        this.lastTime = System.nanoTime() - intervalNs;
    }

    /**
     * Creates a rate limiter with minimum interval between calls.
     *
     * @param intervalMs
     *            minimum interval in milliseconds
     * @return rate limiter
     */
    public static RateLimiter ofIntervalMs(long intervalMs) {
        return new RateLimiter(intervalMs);
    }

    /**
     * Creates a rate limiter with maximum calls per second.
     *
     * @param callsPerSecond
     *            maximum calls per second
     * @return rate limiter
     */
    public static RateLimiter ofRate(double callsPerSecond) {
        long intervalMs = (long) (MILLIS_PER_SECOND / callsPerSecond);
        return new RateLimiter(Math.max(1, intervalMs));
    }

    /**
     * Tries to acquire a permit.
     *
     * @return true if permitted, false if rate limited
     */
    public synchronized boolean tryAcquire() {
        long now = System.nanoTime();
        if (now - lastTime >= intervalNs) {
            lastTime = now;
            return true;
        }
        return false;
    }

    /**
     * Acquires a permit, blocking if necessary.
     *
     * @throws InterruptedException
     *             if interrupted
     */
    public void acquire() throws InterruptedException {
        while (!tryAcquire()) {
            long waitNs = intervalNs - (System.nanoTime() - lastTime);
            if (waitNs > 0) {
                TimeUnit.NANOSECONDS.sleep(waitNs);
            }
        }
    }

    /**
     * Acquires without throwing InterruptedException.
     *
     * @return true if acquired, false if interrupted
     */
    public boolean acquireUninterruptibly() {
        try {
            acquire();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
