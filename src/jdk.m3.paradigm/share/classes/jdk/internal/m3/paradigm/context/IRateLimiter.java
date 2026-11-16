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
package jdk.internal.m3.paradigm.context;

import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * IRateLimiter - Rate Limiter pattern for throttling (Microservice).
 *
 * <p>Limits rate of operations to prevent overload.
 * Inspired by Resilience4j/Guava but pure Java 21.
 *
 * <p>Algorithms:
 * <pre>
 *   TOKEN_BUCKET   → Tokens replenish at fixed rate
 *   SLIDING_WINDOW → Count requests in sliding time window
 *   FIXED_WINDOW   → Count requests in fixed time periods
 * </pre>
 *
 * @since Resilience Pattern Internalization - Apache 2.0
 */
@ISpec(
    value = "Rate Limiter - throttles request rate",
    constraints = {
        "Thread-safe via atomic operations",
        "Multiple algorithms: token bucket, sliding window",
        "Configurable permits per period"
    },
    invariants = {
        "acquire() blocks until permit available",
        "tryAcquire() returns immediately",
        "Rate is maintained over time"
    },
    examples = {
        "IRateLimiter.tokenBucket(100, Duration.ofSeconds(1))",
        "IRateLimiter.slidingWindow(1000, Duration.ofMinutes(1))",
        "limiter.execute(() -> apiCall())"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/resilience/IRateLimiter.java}
 * blob {@code 0055cc5ab4a420c5d53bed8e09e9b76d005b0271} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IRateLimiter {

    /**
     * Acquire a permit, blocking if necessary.
     *
     * @return true if acquired, false if interrupted
     */
    boolean acquire();

    /**
     * Try to acquire a permit without blocking.
     *
     * @return true if permit acquired
     */
    boolean tryAcquire();

    /**
     * Try to acquire with timeout.
     *
     * @param timeout max wait time
     * @return true if permit acquired within timeout
     */
    boolean tryAcquire(Duration timeout);

    /**
     * Execute operation if permit available.
     *
     * @param <T> result type
     * @param operation operation to execute
     * @return result
     * @throws RateLimitExceededException if rate exceeded
     */
    default <T> T execute(Supplier<T> operation) {
        if (!acquire()) {
            throw new RateLimitExceededException("Rate limit exceeded");
        }
        return operation.get();
    }

    /**
     * Execute operation with timeout.
     *
     * @param <T> result type
     * @param operation operation
     * @param timeout max wait for permit
     * @return result
     * @throws RateLimitExceededException if timeout
     */
    default <T> T execute(Supplier<T> operation, Duration timeout) {
        if (!tryAcquire(timeout)) {
            throw new RateLimitExceededException("Rate limit exceeded after " + timeout);
        }
        return operation.get();
    }

    /**
     * Get available permits.
     *
     * @return permits available
     */
    int availablePermits();

    /**
     * Get metrics.
     *
     * @return rate limiter metrics
     */
    Metrics metrics();

    /** Rate limiter metrics. */
    record Metrics(
            int permitsPerPeriod,
            Duration period,
            int availablePermits,
            long totalPermitsGranted,
            long totalPermitsDenied
    ) {}

    // --- EXCEPTION ---

    /** Exception when rate limit exceeded. */
    @SuppressWarnings("serial") // LINT_SUPPRESS (declared, paradigm COP-2 2026-11-10): source-retention
    class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }

    // --- FACTORY METHODS ---

    /**
     * Create token bucket rate limiter.
     *
     * @param permitsPerSecond permits per second
     * @return token bucket limiter
     */
    static IRateLimiter tokenBucket(int permitsPerSecond) {
        return new TokenBucketRateLimiter(permitsPerSecond, Duration.ofSeconds(1));
    }

    /**
     * Create token bucket rate limiter with custom period.
     *
     * @param permits permits per period
     * @param period time period
     * @return token bucket limiter
     */
    static IRateLimiter tokenBucket(int permits, Duration period) {
        return new TokenBucketRateLimiter(permits, period);
    }

    /**
     * Create sliding window rate limiter.
     *
     * @param permits permits per window
     * @param window sliding window duration
     * @return sliding window limiter
     */
    static IRateLimiter slidingWindow(int permits, Duration window) {
        return new SlidingWindowRateLimiter(permits, window);
    }

    /**
     * Create fixed window rate limiter.
     *
     * @param permits permits per window
     * @param window window duration
     * @return fixed window limiter
     */
    static IRateLimiter fixedWindow(int permits, Duration window) {
        return new FixedWindowRateLimiter(permits, window);
    }

    // --- IMPLEMENTATIONS ---

    /** Token bucket rate limiter. */
    class TokenBucketRateLimiter implements IRateLimiter {
        private final int maxTokens;
        private final long refillIntervalNanos;
        private final AtomicInteger tokens;
        private final AtomicLong lastRefillTime;
        private final Duration period;

        private final AtomicLong granted = new AtomicLong(0);
        private final AtomicLong denied = new AtomicLong(0);

        TokenBucketRateLimiter(int permitsPerPeriod, Duration period) {
            this.maxTokens = permitsPerPeriod;
            this.period = period;
            this.refillIntervalNanos = period.toNanos() / permitsPerPeriod;
            this.tokens = new AtomicInteger(permitsPerPeriod);
            this.lastRefillTime = new AtomicLong(System.nanoTime());
        }

        @Override
        // NFL: token bucket refill spin — blocking acquire — loop preserved per M3 §3
        public boolean acquire() {
            while (true) {
                refill();
                if (tokens.get() > 0 && tokens.decrementAndGet() >= 0) {
                    granted.incrementAndGet();
                    return true;
                }
                // Wait for refill
                try {
                    TimeUnit.NANOSECONDS.sleep(refillIntervalNanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        @Override
        public boolean tryAcquire() {
            refill();
            if (tokens.get() > 0 && tokens.decrementAndGet() >= 0) {
                granted.incrementAndGet();
                return true;
            }
            denied.incrementAndGet();
            return false;
        }

        @Override
        public boolean tryAcquire(Duration timeout) {
            long deadline = System.nanoTime() + timeout.toNanos();
            // NFL: deadline-bounded poll loop — rate limiter tryAcquire — loop preserved per M3 §3
            while (System.nanoTime() < deadline) {
                if (tryAcquire()) {
                    return true;
                }
                try {
                    TimeUnit.NANOSECONDS.sleep(min(refillIntervalNanos,
                            deadline - System.nanoTime()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            denied.incrementAndGet();
            return false;
        }

        @Override
        public int availablePermits() {
            refill();
            return max(0, tokens.get());
        }

        @Override
        public Metrics metrics() {
            return new Metrics(maxTokens, period, availablePermits(), granted.get(), denied.get());
        }

        private void refill() {
            long now = System.nanoTime();
            long lastRefill = lastRefillTime.get();
            long elapsed = now - lastRefill;

            if (elapsed > refillIntervalNanos) {
                int tokensToAdd = (int) (elapsed / refillIntervalNanos);
                if (tokensToAdd > 0 && lastRefillTime.compareAndSet(lastRefill, now)) {
                    int newTokens = min(maxTokens, tokens.get() + tokensToAdd);
                    tokens.set(newTokens);
                }
            }
        }
    }

    /** Sliding window rate limiter. */
    class SlidingWindowRateLimiter implements IRateLimiter {
        private final int maxRequests;
        private final Duration windowSize;
        private final long[] timestamps;
        private int head = 0;
        private int count = 0;
        private final Object lock = new Object();

        private final AtomicLong granted = new AtomicLong(0);
        private final AtomicLong denied = new AtomicLong(0);

        SlidingWindowRateLimiter(int maxRequests, Duration windowSize) {
            this.maxRequests = maxRequests;
            this.windowSize = windowSize;
            this.timestamps = new long[maxRequests];
        }

        @Override
        public boolean acquire() {
            return tryAcquire(Duration.ofMinutes(1));
        }

        @Override
        public boolean tryAcquire() {
            synchronized (lock) {
                evictExpired();
                if (count < maxRequests) {
                    addTimestamp(System.nanoTime());
                    granted.incrementAndGet();
                    return true;
                }
                denied.incrementAndGet();
                return false;
            }
        }

        @Override
        public boolean tryAcquire(Duration timeout) {
            long deadline = System.nanoTime() + timeout.toNanos();
            // NFL: deadline-bounded poll loop — sliding window tryAcquire — loop preserved per M3 §3
            while (System.nanoTime() < deadline) {
                if (tryAcquire()) {
                    return true;
                }
                try {
                    Thread.sleep(10); // Small sleep between attempts
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        }

        @Override
        public int availablePermits() {
            synchronized (lock) {
                evictExpired();
                return maxRequests - count;
            }
        }

        @Override
        public Metrics metrics() {
            return new Metrics(maxRequests, windowSize, availablePermits(), granted.get(), denied.get());
        }

        // NFL: ring buffer eviction with mutable head/count — loop preserved per M3 §3
        private void evictExpired() {
            long cutoff = System.nanoTime() - windowSize.toNanos();
            while (count > 0 && timestamps[head] < cutoff) {
                head = (head + 1) % maxRequests;
                count--;
            }
        }

        private void addTimestamp(long timestamp) {
            int tail = (head + count) % maxRequests;
            timestamps[tail] = timestamp;
            count++;
        }
    }

    /** Fixed window rate limiter. */
    class FixedWindowRateLimiter implements IRateLimiter {
        private final int maxRequests;
        private final Duration windowSize;
        private final AtomicInteger windowCount = new AtomicInteger(0);
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

        private final AtomicLong granted = new AtomicLong(0);
        private final AtomicLong denied = new AtomicLong(0);

        FixedWindowRateLimiter(int maxRequests, Duration windowSize) {
            this.maxRequests = maxRequests;
            this.windowSize = windowSize;
        }

        @Override
        public boolean acquire() {
            return tryAcquire(Duration.ofMinutes(1));
        }

        @Override
        public boolean tryAcquire() {
            checkAndResetWindow();
            if (windowCount.incrementAndGet() <= maxRequests) {
                granted.incrementAndGet();
                return true;
            }
            windowCount.decrementAndGet();
            denied.incrementAndGet();
            return false;
        }

        @Override
        public boolean tryAcquire(Duration timeout) {
            long deadline = System.currentTimeMillis() + timeout.toMillis();
            // NFL: deadline-bounded poll loop — fixed window tryAcquire — loop preserved per M3 §3
            while (System.currentTimeMillis() < deadline) {
                if (tryAcquire()) {
                    return true;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        }

        @Override
        public int availablePermits() {
            checkAndResetWindow();
            return max(0, maxRequests - windowCount.get());
        }

        @Override
        public Metrics metrics() {
            return new Metrics(maxRequests, windowSize, availablePermits(), granted.get(), denied.get());
        }

        private void checkAndResetWindow() {
            long now = System.currentTimeMillis();
            long windowStartMillis = windowStart.get();
            if (now - windowStartMillis >= windowSize.toMillis()) {
                if (windowStart.compareAndSet(windowStartMillis, now)) {
                    windowCount.set(0);
                }
            }
        }
    }
}
