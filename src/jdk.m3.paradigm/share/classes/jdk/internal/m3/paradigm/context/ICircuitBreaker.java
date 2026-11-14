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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * ICircuitBreaker - Circuit Breaker pattern (Microservice).
 *
 * <p>Prevents cascade failures by failing fast when service is unhealthy.
 * Inspired by Resilience4j but pure Java 21, no dependencies.
 *
 * <p>States:
 * <pre>
 *   CLOSED   → Normal operation, counting failures
 *   OPEN     → Failing fast, not calling backend
 *   HALF_OPEN → Testing if backend recovered
 * </pre>
 *
 * <p>From Grand Java Patterns Proof:
 * <pre>
 *   Circuit breaker is overload protection.
 *   Like electrical fuse - breaks on overload.
 *   Maps to: feedback control, homeostasis.
 * </pre>
 *
 * @since Resilience Pattern Internalization - Apache 2.0
 */
@ISpec(
    value = "Circuit Breaker - prevents cascade failures with state machine",
    constraints = {
        "Thread-safe via atomic operations",
        "Three states: CLOSED, OPEN, HALF_OPEN",
        "Configurable thresholds and timeouts"
    },
    invariants = {
        "CLOSED: requests pass through",
        "OPEN: requests fail immediately",
        "HALF_OPEN: limited test requests"
    },
    examples = {
        "ICircuitBreaker.create(5, Duration.ofSeconds(30))",
        "breaker.execute(() -> riskyCall())",
        "breaker.state() == State.CLOSED"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/resilience/ICircuitBreaker.java}
 * blob {@code 1a437460fe98c9bf547633a36f66e496e4b51e76} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ICircuitBreaker {

    /** Circuit breaker states. */
    enum State {
        /** Normal operation - counting failures. */
        CLOSED,
        /** Failing fast - backend assumed down. */
        OPEN,
        /** Testing recovery - limited requests. */
        HALF_OPEN
    }

    /**
     * Get current state.
     *
     * @return current state
     */
    State state();

    /**
     * Execute operation through circuit breaker.
     *
     * @param <T> result type
     * @param operation operation to execute
     * @return operation result
     * @throws CircuitBreakerOpenException if circuit is open
     */
    <T> T execute(Supplier<T> operation);

    /**
     * Execute runnable through circuit breaker.
     *
     * @param operation operation to execute
     * @throws CircuitBreakerOpenException if circuit is open
     */
    default void execute(Runnable operation) {
        execute(() -> {
            operation.run();
            return null; // TODO: FM-36 — Void Callable contract
        });
    }

    /** Record success manually. */
    void recordSuccess();

    /** Record failure manually. */
    void recordFailure();

    /**
     * Force state transition (for testing).
     *
     * @param state new state
     */
    void transitionTo(State state);

    /**
     * Get failure count.
     *
     * @return current failure count
     */
    int failureCount();

    /**
     * Get metrics.
     *
     * @return circuit breaker metrics
     */
    Metrics metrics();

    /** Circuit breaker metrics. */
    record Metrics(
            int totalCalls,
            int successfulCalls,
            int failedCalls,
            int rejectedCalls,
            State currentState
    ) {}

    // --- EXCEPTION ---

    /** Exception thrown when circuit is open. */
    @SuppressWarnings("serial") // LINT_SUPPRESS (declared, paradigm COP 2026-11-10): source-retention
    class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }

    // --- FACTORY METHODS ---

    /**
     * Create circuit breaker with default settings.
     *
     * @return circuit breaker (threshold=5, timeout=30s)
     */
    static ICircuitBreaker create() {
        return create(5, Duration.ofSeconds(30));
    }

    /**
     * Create circuit breaker with custom settings.
     *
     * @param failureThreshold failures before opening
     * @param openDuration time to stay open before half-open
     * @return circuit breaker
     */
    static ICircuitBreaker create(int failureThreshold, Duration openDuration) {
        return new SimpleCircuitBreaker(failureThreshold, openDuration, 1);
    }

    /**
     * Create circuit breaker with all settings.
     *
     * @param failureThreshold failures before opening
     * @param openDuration time to stay open
     * @param halfOpenPermits number of test calls in half-open
     * @return circuit breaker
     */
    static ICircuitBreaker create(int failureThreshold, Duration openDuration, int halfOpenPermits) {
        return new SimpleCircuitBreaker(failureThreshold, openDuration, halfOpenPermits);
    }

    // --- IMPLEMENTATION ---

    /** Simple thread-safe implementation. */
    class SimpleCircuitBreaker implements ICircuitBreaker {
        private final int failureThreshold;
        private final Duration openDuration;
        private final int halfOpenPermits;

        private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
        private final AtomicInteger failures = new AtomicInteger(0);
        private final AtomicInteger halfOpenCalls = new AtomicInteger(0);
        private volatile Instant openedAt = null;

        // Metrics
        private final AtomicInteger totalCalls = new AtomicInteger(0);
        private final AtomicInteger successfulCalls = new AtomicInteger(0);
        private final AtomicInteger failedCalls = new AtomicInteger(0);
        private final AtomicInteger rejectedCalls = new AtomicInteger(0);

        SimpleCircuitBreaker(int failureThreshold, Duration openDuration, int halfOpenPermits) {
            this.failureThreshold = failureThreshold;
            this.openDuration = openDuration;
            this.halfOpenPermits = halfOpenPermits;
        }

        @Override
        public State state() {
            checkOpenTimeout();
            return state.get();
        }

        @Override
        public <T> T execute(Supplier<T> operation) {
            totalCalls.incrementAndGet();
            checkOpenTimeout();

            State currentState = state.get();

            if (currentState == State.OPEN) {
                rejectedCalls.incrementAndGet();
                throw new CircuitBreakerOpenException("Circuit breaker is OPEN");
            }

            if (currentState == State.HALF_OPEN) {
                if (halfOpenCalls.incrementAndGet() > halfOpenPermits) {
                    rejectedCalls.incrementAndGet();
                    throw new CircuitBreakerOpenException("Circuit breaker HALF_OPEN limit reached");
                }
            }

            try {
                T result = operation.get();
                recordSuccess();
                return result;
            } catch (Exception e) {
                recordFailure();
                throw e;
            }
        }

        @Override
        public void recordSuccess() {
            successfulCalls.incrementAndGet();
            State currentState = state.get();

            if (currentState == State.HALF_OPEN) {
                // Recovery confirmed - close circuit
                state.set(State.CLOSED);
                failures.set(0);
                halfOpenCalls.set(0);
            } else if (currentState == State.CLOSED) {
                // Reset failure count on success
                failures.set(0);
            }
        }

        @Override
        public void recordFailure() {
            failedCalls.incrementAndGet();
            State currentState = state.get();

            if (currentState == State.HALF_OPEN) {
                // Still failing - reopen circuit
                state.set(State.OPEN);
                openedAt = Instant.now();
                halfOpenCalls.set(0);
            } else if (currentState == State.CLOSED) {
                int currentFailures = failures.incrementAndGet();
                if (currentFailures >= failureThreshold) {
                    state.set(State.OPEN);
                    openedAt = Instant.now();
                }
            }
        }

        @Override
        public void transitionTo(State newState) {
            state.set(newState);
            if (newState == State.CLOSED) {
                failures.set(0);
                halfOpenCalls.set(0);
            } else if (newState == State.OPEN) {
                openedAt = Instant.now();
            } else if (newState == State.HALF_OPEN) {
                halfOpenCalls.set(0);
            }
        }

        @Override
        public int failureCount() {
            return failures.get();
        }

        @Override
        public Metrics metrics() {
            return new Metrics(
                    totalCalls.get(),
                    successfulCalls.get(),
                    failedCalls.get(),
                    rejectedCalls.get(),
                    state.get()
            );
        }

        private void checkOpenTimeout() {
            if (state.get() == State.OPEN && openedAt != null) {
                if (Duration.between(openedAt, Instant.now()).compareTo(openDuration) > 0) {
                    state.compareAndSet(State.OPEN, State.HALF_OPEN);
                    halfOpenCalls.set(0);
                }
            }
        }
    }
}
