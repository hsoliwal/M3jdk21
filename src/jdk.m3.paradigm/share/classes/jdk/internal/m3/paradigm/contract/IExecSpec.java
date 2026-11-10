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
package jdk.internal.m3.paradigm.contract;

import jdk.internal.m3.paradigm.annotation.IMethodSpec;
import jdk.internal.m3.paradigm.annotation.Model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IExecSpec - Execution/threading specification for AI code generation.
 *
 * <h2>Execution Patterns</h2>
 *
 * <pre>
 * Java execution models:
 *   - Synchronous (same thread)
 *   - Async (platform threads)
 *   - Virtual threads (Project Loom)
 *   - Fork/Join (parallel streams)
 *   - Reactive (Flow/Reactor)
 *   - Event loop (Netty/Vert.x)
 *   - Actor model (Akka)
 *   - Coroutines (Kotlin-style)
 *
 * &#64;IExecSpec specifies execution semantics in IOP style.
 * </pre>
 *
 * <h2>Generated Code PatternKind</h2>
 *
 * <pre>{@code
 * // From: @IExecSpec(mode = VIRTUAL, timeout = 30000)
 * // Generates:
 *
 * public Result process(Request req) {
 *     return Executors.newVirtualThreadPerTaskExecutor()
 *         .submit(() -> doProcess(req))
 *         .get(30, TimeUnit.SECONDS);
 * }
 *
 * // From: @IExecSpec(mode = PARALLEL, parallelism = 4)
 * // Generates:
 *
 * public List&lt;Result> processAll(List&lt;Request> requests) {
 * 	return requests.parallelStream().map(this::process).toList();
 * }
 * }</pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 * interface IDataProcessor {
 *
 *     // Virtual thread execution @IExecSpec(mode = VIRTUAL, timeout = 30000) Result processAsync(Request req);
 *
 * // Parallel batch processing @IExecSpec(mode = PARALLEL, parallelism = 4) List&lt;Result> processBatch(List&lt;Request>
 * requests);
 *
 * // Scheduled execution
 * &#64;IExecSpec(mode = SCHEDULED, cron = "0 0 * * *")
 *     void dailyCleanup();
 *
 *     // Rate-limited execution
 *     &#64;IExecSpec(
 *         mode = SYNC,
 *         rateLimit = @IExecSpec.RateLimit(permits = 100, perSeconds = 1)
 *     )
 *     Response callExternalApi(Request req);
 * }
 * }
 *                 </pre>
 *
 * @since IOP 1.0 - Execution Specification
 * @see IMethodSpec
 * @see IAspectSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares method execution, scheduling, timeout, retry, rate-limit, bulkhead, circuit and synchronization requirements; it creates no executor and establishes no thread-safety or timing guarantee.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IExecSpec.java}
 * blob {@code a9647496f627e5eb3bf715931aea285c0a248d93} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IExecSpec {

    // --- SIMPLE OPTIONS ---

    /**
     * Predefined execution pattern.
     *
     * @return option
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#option intent: Predefined execution pattern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Option}; default expression {@code Option.NONE}.
     */
    Option option() default Option.NONE;

    /** Common execution patterns.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IExecSpec.Option choices described as Common execution patterns. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 8 constants in the existing source order.
     */
    enum Option {
        /** No preset - configure manually.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Option#NONE policy choice: No preset - configure manually. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Option; its declared order and identity are preserved.
         */
        NONE,

        /** Synchronous execution (default).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Option#SYNC policy choice: Synchronous execution (default). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Option; its declared order and identity are preserved.
         */
        SYNC,

        /** Async with virtual threads.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Option#ASYNC_VIRTUAL policy choice: Async with virtual threads. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Option; its declared order and identity are preserved.
         */
        ASYNC_VIRTUAL,

        /** Async with platform threads.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Option#ASYNC_PLATFORM policy choice: Async with platform threads. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Option; its declared order and identity are preserved.
         */
        ASYNC_PLATFORM,

        /** Parallel stream processing.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Option#PARALLEL policy choice: Parallel stream processing. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Option; its declared order and identity are preserved.
         */
        PARALLEL,

        /** Background task.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Option#BACKGROUND policy choice: Background task. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Option; its declared order and identity are preserved.
         */
        BACKGROUND,

        /** Scheduled task.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Option#SCHEDULED policy choice: Scheduled task. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Option; its declared order and identity are preserved.
         */
        SCHEDULED,

        /** Event-driven (non-blocking).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Option#EVENT_DRIVEN policy choice: Event-driven (non-blocking). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Option; its declared order and identity are preserved.
         */
        EVENT_DRIVEN
    }

    // --- EXECUTION MODE ---

    /**
     * Execution mode.
     *
     * @return mode
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#mode intent: Execution mode. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Mode}; default expression {@code Mode.SYNC}.
     */
    Mode mode() default Mode.SYNC;

    /** Execution modes.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IExecSpec.Mode choices described as Execution modes. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 8 constants in the existing source order.
     */
    enum Mode {
        /** Synchronous - same thread.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Mode#SYNC policy choice: Synchronous - same thread. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Mode; its declared order and identity are preserved.
         */
        SYNC,

        /** Virtual threads (Project Loom).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Mode#VIRTUAL policy choice: Virtual threads (Project Loom). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Mode; its declared order and identity are preserved.
         */
        VIRTUAL,

        /** Platform threads (traditional).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Mode#PLATFORM policy choice: Platform threads (traditional). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Mode; its declared order and identity are preserved.
         */
        PLATFORM,

        /** Fork/Join pool (parallel).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Mode#PARALLEL policy choice: Fork/Join pool (parallel). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Mode; its declared order and identity are preserved.
         */
        PARALLEL,

        /** Scheduled executor.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Mode#SCHEDULED policy choice: Scheduled executor. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Mode; its declared order and identity are preserved.
         */
        SCHEDULED,

        /** Custom executor.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Mode#CUSTOM policy choice: Custom executor. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Mode; its declared order and identity are preserved.
         */
        CUSTOM,

        /** Event loop (non-blocking).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Mode#EVENT_LOOP policy choice: Event loop (non-blocking). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Mode; its declared order and identity are preserved.
         */
        EVENT_LOOP,

        /** Actor model.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Mode#ACTOR policy choice: Actor model. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Mode; its declared order and identity are preserved.
         */
        ACTOR
    }

    // --- EXECUTOR CONFIGURATION ---

    /**
     * Executor bean name (for CUSTOM mode).
     *
     * @return executor name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#executor intent: Executor bean name (for CUSTOM mode). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String executor() default "";

    /**
     * Parallelism level (for PARALLEL mode).
     *
     * @return parallelism
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#parallelism intent: Parallelism level (for PARALLEL mode). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code -1}.
     */
    int parallelism() default -1;

    /**
     * Core pool size (for thread pool).
     *
     * @return core size
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#corePoolSize intent: Core pool size (for thread pool). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int corePoolSize() default 0;

    /**
     * Max pool size (for thread pool).
     *
     * @return max size
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#maxPoolSize intent: Max pool size (for thread pool). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int maxPoolSize() default 0;

    /**
     * Queue capacity (for bounded queue).
     *
     * @return capacity
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#queueCapacity intent: Queue capacity (for bounded queue). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int queueCapacity() default 0;

    // --- TIMEOUT ---

    /**
     * Execution timeout in milliseconds. 0 = no timeout.
     *
     * @return timeout
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#timeoutMs intent: Execution timeout in milliseconds. 0 = no timeout. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int timeoutMs() default 0;

    /**
     * Action on timeout.
     *
     * @return timeout action
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#onTimeout intent: Action on timeout. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code TimeoutAction}; default expression {@code TimeoutAction.EXCEPTION}.
     */
    TimeoutAction onTimeout() default TimeoutAction.EXCEPTION;

    /** Timeout actions.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IExecSpec.TimeoutAction choices described as Timeout actions. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum TimeoutAction {
        /** Throw TimeoutException.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.TimeoutAction#EXCEPTION policy choice: Throw TimeoutException. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.TimeoutAction; its declared order and identity are preserved.
         */
        EXCEPTION,

        /** Return default value.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.TimeoutAction#DEFAULT policy choice: Return default value. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.TimeoutAction; its declared order and identity are preserved.
         */
        DEFAULT,

        /** Return null.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.TimeoutAction#NULL policy choice: Return null. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.TimeoutAction; its declared order and identity are preserved.
         */
        NULL,

        /** Cancel and return partial.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.TimeoutAction#PARTIAL policy choice: Cancel and return partial. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.TimeoutAction; its declared order and identity are preserved.
         */
        PARTIAL,

        /** Log and continue.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.TimeoutAction#LOG_CONTINUE policy choice: Log and continue. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.TimeoutAction; its declared order and identity are preserved.
         */
        LOG_CONTINUE
    }

    /**
     * Default value on timeout.
     *
     * @return default expression
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#timeoutDefault intent: Default value on timeout. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String timeoutDefault() default "";

    // --- SCHEDULING ---

    /**
     * Cron expression (for SCHEDULED mode).
     *
     * @return cron expression
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#cron intent: Cron expression (for SCHEDULED mode). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String cron() default "";

    /**
     * Fixed delay in milliseconds.
     *
     * @return fixed delay
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#fixedDelayMs intent: Fixed delay in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code 0}.
     */
    long fixedDelayMs() default 0;

    /**
     * Fixed rate in milliseconds.
     *
     * @return fixed rate
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#fixedRateMs intent: Fixed rate in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code 0}.
     */
    long fixedRateMs() default 0;

    /**
     * Initial delay in milliseconds.
     *
     * @return initial delay
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#initialDelayMs intent: Initial delay in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code 0}.
     */
    long initialDelayMs() default 0;

    // --- RATE LIMITING ---

    /**
     * Rate limit configuration.
     *
     * @return rate limit
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#rateLimit intent: Rate limit configuration. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code RateLimit}; default expression {@code @RateLimit}.
     */
    RateLimit rateLimit() default @RateLimit;

    /** Rate limit configuration.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares permit-window and blocking preferences for rate limiting; it acquires no permit and retains implicit CLASS retention without explicit Target restriction.
     * @m3.dataStructures CLASS (implicit default)-retained annotation targeting {@code all declaration contexts (no explicit Target)}; existing element and enum schemas are preserved.
     */
    @interface RateLimit {

        /** Whether rate limiting is enabled.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.RateLimit#enabled intent: Whether rate limiting is enabled. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
         */
        boolean enabled() default false;

        /** Number of permits per time window.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.RateLimit#permits intent: Number of permits per time window. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 100}.
         */
        int permits() default 100;

        /** Time window in seconds.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.RateLimit#perSeconds intent: Time window in seconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 1}.
         */
        int perSeconds() default 1;

        /** Whether to block or reject when limit exceeded.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.RateLimit#block intent: Whether to block or reject when limit exceeded. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
         */
        boolean block() default false;

        /** Timeout for blocking in milliseconds.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.RateLimit#blockTimeoutMs intent: Timeout for blocking in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 1000}.
         */
        int blockTimeoutMs() default 1000;
    }

    // --- BULKHEAD ---

    /**
     * Bulkhead configuration (isolation).
     *
     * @return bulkhead
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#bulkhead intent: Bulkhead configuration (isolation). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Bulkhead}; default expression {@code @Bulkhead}.
     */
    Bulkhead bulkhead() default @Bulkhead;

    /** Bulkhead configuration.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares concurrency-isolation and waiting preferences; it allocates no semaphore and retains implicit CLASS retention without explicit Target restriction.
     * @m3.dataStructures CLASS (implicit default)-retained annotation targeting {@code all declaration contexts (no explicit Target)}; existing element and enum schemas are preserved.
     */
    @interface Bulkhead {

        /** Whether bulkhead is enabled.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.Bulkhead#enabled intent: Whether bulkhead is enabled. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
         */
        boolean enabled() default false;

        /** Maximum concurrent calls.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.Bulkhead#maxConcurrent intent: Maximum concurrent calls. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 10}.
         */
        int maxConcurrent() default 10;

        /** Maximum wait time for semaphore.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.Bulkhead#maxWaitMs intent: Maximum wait time for semaphore. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
         */
        int maxWaitMs() default 0;
    }

    // --- RETRY ---

    /**
     * Retry configuration.
     *
     * @return retry config
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#retry intent: Retry configuration. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Retry}; default expression {@code @Retry}.
     */
    Retry retry() default @Retry;

    /** Retry configuration.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares retry count, backoff and included/excluded exception classes; it catches no failure and retains implicit CLASS retention without explicit Target restriction.
     * @m3.dataStructures CLASS (implicit default)-retained annotation targeting {@code all declaration contexts (no explicit Target)}; existing element and enum schemas are preserved.
     */
    @interface Retry {

        /** Maximum retry attempts.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.Retry#maxAttempts intent: Maximum retry attempts. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
         */
        int maxAttempts() default 0;

        /** Initial backoff in milliseconds.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.Retry#backoffMs intent: Initial backoff in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 1000}.
         */
        int backoffMs() default 1000;

        /** Backoff multiplier.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.Retry#multiplier intent: Backoff multiplier. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 2.0}.
         */
        double multiplier() default 2.0;

        /** Maximum backoff in milliseconds.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.Retry#maxBackoffMs intent: Maximum backoff in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 30000}.
         */
        int maxBackoffMs() default 30000;

        /** Exceptions that trigger retry.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.Retry#retryOn intent: Exceptions that trigger retry. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code Class<? extends Exception>[]}; default expression {@code {Exception.class}}.
         */
        Class<? extends Exception>[] retryOn() default {Exception.class};

        /** Exceptions that should not retry.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.Retry#noRetryOn intent: Exceptions that should not retry. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code Class<? extends Exception>[]}; default expression {@code {}}.
         */
        Class<? extends Exception>[] noRetryOn() default {};
    }

    // --- CIRCUIT BREAKER ---

    /**
     * Circuit breaker configuration.
     *
     * @return circuit breaker
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#circuitBreaker intent: Circuit breaker configuration. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code CircuitBreaker}; default expression {@code @CircuitBreaker}.
     */
    CircuitBreaker circuitBreaker() default @CircuitBreaker;

    /** Circuit breaker configuration.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares failure/success thresholds and circuit timing; it maintains no state and retains implicit CLASS retention without explicit Target restriction.
     * @m3.dataStructures CLASS (implicit default)-retained annotation targeting {@code all declaration contexts (no explicit Target)}; existing element and enum schemas are preserved.
     */
    @interface CircuitBreaker {

        /** Whether circuit breaker is enabled.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.CircuitBreaker#enabled intent: Whether circuit breaker is enabled. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
         */
        boolean enabled() default false;

        /** Failure threshold to open circuit.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.CircuitBreaker#failureThreshold intent: Failure threshold to open circuit. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 5}.
         */
        int failureThreshold() default 5;

        /** Success threshold to close circuit.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.CircuitBreaker#successThreshold intent: Success threshold to close circuit. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 3}.
         */
        int successThreshold() default 3;

        /** Time window for failure counting (ms).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.CircuitBreaker#windowMs intent: Time window for failure counting (ms). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 60000}.
         */
        int windowMs() default 60000;

        /** Time before attempting reset (ms).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExecSpec.CircuitBreaker#resetTimeoutMs intent: Time before attempting reset (ms). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 30000}.
         */
        int resetTimeoutMs() default 30000;
    }

    // --- THREAD SAFETY ---

    /**
     * Whether method is thread-safe.
     *
     * @return true if thread-safe
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#threadSafe intent: Whether method is thread-safe. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean threadSafe() default true;

    /**
     * Synchronization strategy.
     *
     * @return sync strategy
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#sync intent: Synchronization strategy. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code SyncStrategy}; default expression {@code SyncStrategy.NONE}.
     */
    SyncStrategy sync() default SyncStrategy.NONE;

    /** Synchronization strategies.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IExecSpec.SyncStrategy choices described as Synchronization strategies. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum SyncStrategy {
        /** No synchronization.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.SyncStrategy#NONE policy choice: No synchronization. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.SyncStrategy; its declared order and identity are preserved.
         */
        NONE,
        /** synchronized block.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.SyncStrategy#SYNCHRONIZED policy choice: synchronized block. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.SyncStrategy; its declared order and identity are preserved.
         */
        SYNCHRONIZED,
        /** ReentrantLock.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.SyncStrategy#LOCK policy choice: ReentrantLock. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.SyncStrategy; its declared order and identity are preserved.
         */
        LOCK,
        /** ReadWriteLock.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.SyncStrategy#READ_WRITE_LOCK policy choice: ReadWriteLock. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.SyncStrategy; its declared order and identity are preserved.
         */
        READ_WRITE_LOCK,
        /** StampedLock.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.SyncStrategy#STAMPED_LOCK policy choice: StampedLock. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.SyncStrategy; its declared order and identity are preserved.
         */
        STAMPED_LOCK,
        /** Atomic operations.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.SyncStrategy#ATOMIC policy choice: Atomic operations. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.SyncStrategy; its declared order and identity are preserved.
         */
        ATOMIC
    }

    // --- OBSERVABILITY ---

    /**
     * Whether to trace execution.
     *
     * @return true to trace
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#traced intent: Whether to trace execution. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean traced() default false;

    /**
     * Whether to meter execution.
     *
     * @return true to meter
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#metered intent: Whether to meter execution. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean metered() default false;

    /**
     * Metric name.
     *
     * @return metric name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#metricName intent: Metric name. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String metricName() default "";

    // --- PRIORITY ---

    /**
     * Execution priority.
     *
     * @return priority
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#priority intent: Execution priority. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Priority}; default expression {@code Priority.NORMAL}.
     */
    Priority priority() default Priority.NORMAL;

    /** Execution priorities.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IExecSpec.Priority choices described as Execution priorities. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum Priority {
        /** Lowest priority.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Priority#LOW policy choice: Lowest priority. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Priority; its declared order and identity are preserved.
         */
        LOW,
        /** Normal priority (default).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Priority#NORMAL policy choice: Normal priority (default). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Priority; its declared order and identity are preserved.
         */
        NORMAL,
        /** High priority.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Priority#HIGH policy choice: High priority. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Priority; its declared order and identity are preserved.
         */
        HIGH,
        /** Critical - must execute immediately.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExecSpec.Priority#CRITICAL policy choice: Critical - must execute immediately. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExecSpec.Priority; its declared order and identity are preserved.
         */
        CRITICAL
    }

    // --- DESCRIPTION ---

    /**
     * Description of execution requirements.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#value intent: Description of execution requirements. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExecSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.OPUS}.
     */
    Model.ModelType model() default Model.ModelType.OPUS;
}
