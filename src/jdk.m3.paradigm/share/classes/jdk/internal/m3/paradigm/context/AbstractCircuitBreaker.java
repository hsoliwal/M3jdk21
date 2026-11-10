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

import jdk.internal.m3.collections.PlaceholderLogger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Abstract base implementation of {@link ICircuitBreaker} using atomic operations.
 *
 * <p>This class provides a production-ready, thread-safe circuit breaker implementation
 * with machine-grade quality:
 * <ul>
 *   <li>Lock-free atomic operations for all state transitions</li>
 *   <li>Automatic timeout checking and state recovery</li>
 *   <li>Comprehensive metrics tracking</li>
 *   <li>Configurable thresholds and timeouts</li>
 * </ul>
 *
 * <p>Thread Safety: All methods are thread-safe using {@link AtomicReference} and
 * {@link AtomicInteger} for state management.
 *
 * <p>Performance: Lock-free design allows high-throughput operation under contention.
 *
 * @see ICircuitBreaker
 * @since 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/resilience/AbstractCircuitBreaker.java}
 * blob {@code 45a04ea3aa9331d87b16ebe757989483d06e13f7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractCircuitBreaker implements ICircuitBreaker {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractCircuitBreaker.class);


    /** Failure threshold before opening circuit. */
    protected final int failureThreshold;

    /** Duration to keep circuit open before testing recovery. */
    protected final Duration openDuration;

    /** Number of test calls permitted in HALF_OPEN state. */
    protected final int halfOpenPermits;

    /** Current circuit breaker state. */
    protected final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);

    /** Current failure count in CLOSED state. */
    protected final AtomicInteger failures = new AtomicInteger(0);

    /** Number of calls made in HALF_OPEN state. */
    protected final AtomicInteger halfOpenCalls = new AtomicInteger(0);

    /** Timestamp when circuit was opened. */
    protected volatile Instant openedAt = null;

    // Metrics tracking
    protected final AtomicInteger totalCalls = new AtomicInteger(0);
    protected final AtomicInteger successfulCalls = new AtomicInteger(0);
    protected final AtomicInteger failedCalls = new AtomicInteger(0);
    protected final AtomicInteger rejectedCalls = new AtomicInteger(0);

    /**
     * Constructs an abstract circuit breaker with specified configuration.
     *
     * @param failureThreshold number of failures before opening circuit
     * @param openDuration duration to keep circuit open
     * @param halfOpenPermits number of test calls in half-open state
     */
    protected AbstractCircuitBreaker(int failureThreshold, Duration openDuration, int halfOpenPermits) {
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("Failure threshold must be positive");
        }
        if (openDuration == null || openDuration.isNegative()) {
            throw new IllegalArgumentException("Open duration must be non-negative");
        }
        if (halfOpenPermits <= 0) {
            throw new IllegalArgumentException("Half-open permits must be positive");
        }

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
            log.warn("Circuit breaker is OPEN - rejecting call");
            throw new CircuitBreakerOpenException("Circuit breaker is OPEN");
        }

        if (currentState == State.HALF_OPEN) {
            if (halfOpenCalls.incrementAndGet() > halfOpenPermits) {
                rejectedCalls.incrementAndGet();
                log.warn("Circuit breaker HALF_OPEN limit reached - rejecting call");
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
            if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                failures.set(0);
                halfOpenCalls.set(0);
                log.info("Circuit breaker recovered - transitioning to CLOSED");
            }
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
            if (state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                openedAt = Instant.now();
                halfOpenCalls.set(0);
                log.warn("Circuit breaker recovery failed - transitioning back to OPEN");
            }
        } else if (currentState == State.CLOSED) {
            int currentFailures = failures.incrementAndGet();
            if (currentFailures >= failureThreshold) {
                if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                    openedAt = Instant.now();
                    log.error("Circuit breaker opened after {} failures", currentFailures);
                }
            }
        }
    }

    @Override
    public void transitionTo(State newState) {
        if (newState == null) {
            throw new IllegalArgumentException("State cannot be null");
        }

        state.set(newState);

        if (newState == State.CLOSED) {
            failures.set(0);
            halfOpenCalls.set(0);
            openedAt = null;
        } else if (newState == State.OPEN) {
            openedAt = Instant.now();
        } else if (newState == State.HALF_OPEN) {
            halfOpenCalls.set(0);
        }

        log.info("Circuit breaker transitioned to {}", newState);
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

    /**
     * Checks if circuit should transition from OPEN to HALF_OPEN.
     *
     * <p>This method is called automatically before each operation to ensure
     * timely state transitions.
     */
    protected void checkOpenTimeout() {
        if (state.get() == State.OPEN && openedAt != null) {
            if (Duration.between(openedAt, Instant.now()).compareTo(openDuration) > 0) {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    halfOpenCalls.set(0);
                    log.info("Circuit breaker timeout elapsed - transitioning to HALF_OPEN");
                }
            }
        }
    }

    /**
     * Resets all metrics and state to initial values.
     *
     * <p>This method is useful for testing or when reconfiguring the circuit breaker.
     */
    protected void reset() {
        state.set(State.CLOSED);
        failures.set(0);
        halfOpenCalls.set(0);
        openedAt = null;
        totalCalls.set(0);
        successfulCalls.set(0);
        failedCalls.set(0);
        rejectedCalls.set(0);
        log.info("Circuit breaker reset to initial state");
    }
}
