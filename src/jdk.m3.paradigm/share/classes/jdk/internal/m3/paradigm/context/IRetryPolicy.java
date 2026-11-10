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
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.lang.Math.min;

/**
 * IRetryPolicy - Retry pattern with backoff strategies (Microservice).
 *
 * <p>Retries failed operations with configurable backoff.
 * Inspired by Resilience4j/Spring Retry but pure Java 21.
 *
 * <p>Backoff strategies:
 * <pre>
 *   FIXED      → Same delay between retries
 *   LINEAR     → Delay increases linearly
 *   EXPONENTIAL → Delay doubles each retry
 *   JITTER     → Random variation to prevent thundering herd
 * </pre>
 *
 * @since Resilience Pattern Internalization - Apache 2.0
 */
@ISpec(
    value = "Retry Policy - retries with configurable backoff strategies",
    constraints = {
        "Configurable max attempts",
        "Multiple backoff strategies",
        "Selective retry by exception type"
    },
    invariants = {
        "execute() retries up to maxAttempts",
        "Backoff delay applied between retries",
        "Last exception propagated if all retries fail"
    },
    examples = {
        "IRetryPolicy.fixed(3, Duration.ofSeconds(1))",
        "IRetryPolicy.exponential(5, Duration.ofMillis(100))",
        "policy.execute(() -> riskyCall())"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/resilience/IRetryPolicy.java}
 * blob {@code e3955d2256433b7ee2b9dcb500d0586fc2c2254f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IRetryPolicy {

    /**
     * Execute operation with retry.
     *
     * @param <T> result type
     * @param operation operation to retry
     * @return result if successful
     * @throws RetryExhaustedException if all retries fail
     */
    <T> T execute(Supplier<T> operation);

    /**
     * Execute runnable with retry.
     *
     * @param operation operation to retry
     */
    default void execute(Runnable operation) {
        execute(() -> {
            operation.run();
            return null; // TODO: FM-36 — Void Callable contract
        });
    }

    /**
     * Get max attempts.
     *
     * @return maximum retry attempts
     */
    int maxAttempts();

    /**
     * Calculate delay for attempt.
     *
     * @param attempt attempt number (1-based)
     * @return delay before next retry
     */
    Duration delayFor(int attempt);

    // --- EXCEPTION ---

    /** Exception when all retries exhausted. */
    @SuppressWarnings("serial") // LINT_SUPPRESS (declared, paradigm COP-2 2026-11-10): source-retention
    class RetryExhaustedException extends RuntimeException {
        private final int attempts;

        public RetryExhaustedException(int attempts, Throwable cause) {
            super("Retry exhausted after " + attempts + " attempts", cause);
            this.attempts = attempts;
        }

        public int attempts() {
            return attempts;
        }
    }

    // --- FACTORY METHODS ---

    /**
     * Create fixed delay retry policy.
     *
     * @param maxAttempts max attempts
     * @param delay delay between retries
     * @return retry policy
     */
    static IRetryPolicy fixed(int maxAttempts, Duration delay) {
        return new FixedRetryPolicy(maxAttempts, delay, e -> true);
    }

    /**
     * Create linear backoff retry policy.
     *
     * @param maxAttempts max attempts
     * @param initialDelay initial delay
     * @return retry policy (delay = initialDelay * attempt)
     */
    static IRetryPolicy linear(int maxAttempts, Duration initialDelay) {
        return new LinearRetryPolicy(maxAttempts, initialDelay, e -> true);
    }

    /**
     * Create exponential backoff retry policy.
     *
     * @param maxAttempts max attempts
     * @param initialDelay initial delay
     * @return retry policy (delay = initialDelay * 2^attempt)
     */
    static IRetryPolicy exponential(int maxAttempts, Duration initialDelay) {
        return exponential(maxAttempts, initialDelay, Duration.ofMinutes(1));
    }

    /**
     * Create exponential backoff with max delay.
     *
     * @param maxAttempts max attempts
     * @param initialDelay initial delay
     * @param maxDelay maximum delay cap
     * @return retry policy
     */
    static IRetryPolicy exponential(int maxAttempts, Duration initialDelay, Duration maxDelay) {
        return new ExponentialRetryPolicy(maxAttempts, initialDelay, maxDelay, e -> true);
    }

    /**
     * Create exponential backoff with jitter.
     *
     * @param maxAttempts max attempts
     * @param initialDelay initial delay
     * @return retry policy with random jitter
     */
    static IRetryPolicy exponentialWithJitter(int maxAttempts, Duration initialDelay) {
        return new JitterRetryPolicy(maxAttempts, initialDelay, Duration.ofMinutes(1), e -> true);
    }

    /**
     * Create no-delay retry (for testing).
     *
     * @param maxAttempts max attempts
     * @return zero-delay retry policy
     */
    static IRetryPolicy immediate(int maxAttempts) {
        return fixed(maxAttempts, Duration.ZERO);
    }

    /**
     * Create builder for custom retry policy.
     *
     * @return retry policy builder
     */
    static Builder builder() {
        return new Builder();
    }

    // --- BUILDER ---

    /** Fluent builder for retry policy. */
    class Builder {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofSeconds(1);
        private Duration maxDelay = Duration.ofMinutes(1);
        private BackoffStrategy strategy = BackoffStrategy.FIXED;
        private Predicate<Throwable> retryOn = e -> true;

        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder initialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
            return this;
        }

        public Builder maxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }

        public Builder fixed() {
            this.strategy = BackoffStrategy.FIXED;
            return this;
        }

        public Builder linear() {
            this.strategy = BackoffStrategy.LINEAR;
            return this;
        }

        public Builder exponential() {
            this.strategy = BackoffStrategy.EXPONENTIAL;
            return this;
        }

        public Builder withJitter() {
            this.strategy = BackoffStrategy.JITTER;
            return this;
        }

        public Builder retryOn(Predicate<Throwable> predicate) {
            this.retryOn = predicate;
            return this;
        }

        @SuppressWarnings("unchecked") // LINT_SUPPRESS (declared, paradigm COP-2 2026-11-10): source-retention
        public Builder retryOn(Class<? extends Throwable>... exceptionTypes) {
            this.retryOn = e -> java.util.Arrays.stream(exceptionTypes)
                    .anyMatch(type -> type.isInstance(e));
            return this;
        }

        public IRetryPolicy build() {
            return switch (strategy) {
                case FIXED -> new FixedRetryPolicy(maxAttempts, initialDelay, retryOn);
                case LINEAR -> new LinearRetryPolicy(maxAttempts, initialDelay, retryOn);
                case EXPONENTIAL -> new ExponentialRetryPolicy(maxAttempts, initialDelay, maxDelay, retryOn);
                case JITTER -> new JitterRetryPolicy(maxAttempts, initialDelay, maxDelay, retryOn);
            };
        }

        private enum BackoffStrategy { FIXED, LINEAR, EXPONENTIAL, JITTER }
    }

    // --- IMPLEMENTATIONS ---

    /** Base retry policy. */
    abstract class BaseRetryPolicy implements IRetryPolicy {
        protected final int maxAttempts;
        protected final Predicate<Throwable> retryOn;

        protected BaseRetryPolicy(int maxAttempts, Predicate<Throwable> retryOn) {
            this.maxAttempts = maxAttempts;
            this.retryOn = retryOn;
        }

        @Override
        public int maxAttempts() {
            return maxAttempts;
        }

        @Override
        public <T> T execute(Supplier<T> operation) {
            Throwable lastException = null;
            // NFL: retry loop with mutable exception tracking and sleep between attempts — loop preserved per M3 §3
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    return operation.get();
                } catch (Throwable e) {
                    lastException = e;
                    if (!retryOn.test(e) || attempt >= maxAttempts) {
                        throw new RetryExhaustedException(attempt, e);
                    }
                    sleep(delayFor(attempt));
                }
            }
            throw new RetryExhaustedException(maxAttempts, lastException);
        }

        protected void sleep(Duration duration) {
            if (duration.isZero() || duration.isNegative()) return;
            try {
                TimeUnit.MILLISECONDS.sleep(duration.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Retry interrupted", e);
            }
        }
    }

    /** Fixed delay retry. */
    class FixedRetryPolicy extends BaseRetryPolicy {
        private final Duration delay;

        FixedRetryPolicy(int maxAttempts, Duration delay, Predicate<Throwable> retryOn) {
            super(maxAttempts, retryOn);
            this.delay = delay;
        }

        @Override
        public Duration delayFor(int attempt) {
            return delay;
        }
    }

    /** Linear backoff retry. */
    class LinearRetryPolicy extends BaseRetryPolicy {
        private final Duration initialDelay;

        LinearRetryPolicy(int maxAttempts, Duration initialDelay, Predicate<Throwable> retryOn) {
            super(maxAttempts, retryOn);
            this.initialDelay = initialDelay;
        }

        @Override
        public Duration delayFor(int attempt) {
            return initialDelay.multipliedBy(attempt);
        }
    }

    /** Exponential backoff retry. */
    class ExponentialRetryPolicy extends BaseRetryPolicy {
        private final Duration initialDelay;
        private final Duration maxDelay;

        ExponentialRetryPolicy(int maxAttempts, Duration initialDelay, Duration maxDelay,
                               Predicate<Throwable> retryOn) {
            super(maxAttempts, retryOn);
            this.initialDelay = initialDelay;
            this.maxDelay = maxDelay;
        }

        @Override
        public Duration delayFor(int attempt) {
            long delay = initialDelay.toMillis() * (1L << (attempt - 1));
            return Duration.ofMillis(min(delay, maxDelay.toMillis()));
        }
    }

    /** Exponential backoff with jitter. */
    class JitterRetryPolicy extends BaseRetryPolicy {
        private final Duration initialDelay;
        private final Duration maxDelay;
        private final Random random = new Random();

        JitterRetryPolicy(int maxAttempts, Duration initialDelay, Duration maxDelay,
                          Predicate<Throwable> retryOn) {
            super(maxAttempts, retryOn);
            this.initialDelay = initialDelay;
            this.maxDelay = maxDelay;
        }

        @Override
        public Duration delayFor(int attempt) {
            long baseDelay = initialDelay.toMillis() * (1L << (attempt - 1));
            long capped = min(baseDelay, maxDelay.toMillis());
            // Add jitter: 50-150% of base delay
            double jitter = 0.5 + random.nextDouble();
            return Duration.ofMillis((long) (capped * jitter));
        }
    }
}
