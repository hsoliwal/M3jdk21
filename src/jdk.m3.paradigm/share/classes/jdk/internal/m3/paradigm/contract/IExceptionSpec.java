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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IExceptionSpec - Exception handling specification for AI code generation.
 *
 * <h2>Exception Handling Patterns</h2>
 *
 * <pre>
 * Exception patterns in Java:
 *   - try/catch/finally (traditional)
 *   - @ExceptionHandler (Spring MVC)
 *   - @ControllerAdvice (global handlers)
 *   - Optional/Result (functional)
 *   - CompletableFuture.exceptionally()
 *   - Reactive onErrorResume()
 *   - Circuit breaker fallbacks
 *
 * &#64;IExceptionSpec specifies exception handling in IOP style.
 * </pre>
 *
 * <h2>Generated Code PatternKind</h2>
 *
 * <pre>{@code
 * // From: @IExceptionSpec(handle = {IOException.class}, fallback =
 * // "defaultValue")
 * // Generates:
 *
 * public String readFile(Path path) {
 * 	try {
 * 		return Files.readString(path);
 * 	} catch (IOException e) {
 * 		log.warn("Failed to read file: {}", path, e);
 * 		return "defaultValue";
 * 	}
 * }
 *
 * // From: @IExceptionSpec(translate = UserNotFoundException.class, to = "404
 * // Not Found")
 * // Generates exception translation
 * }</pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * interface IFileService {
 *
 *     // Handle with fallback
 * &#64;IExceptionSpec(handle = IOException.class, fallbackMethod = "getDefaultContent")
 * String readFile(Path path);
 *
 * // Translate exception
 * &#64;IExceptionSpec(translate = @IExceptionSpec.Translation(from = SQLException.class, to = DataAccessException.class, message = "Database operation failed"))
 * User findUser(String id);
 *
 * // Retry on exception
 * &#64;IExceptionSpec(handle = TimeoutException.class, retry = @IExceptionSpec.Retry(maxAttempts = 3))
 * Response callExternalService();
 *
 * // Log and rethrow
 * &#64;IExceptionSpec(handle = Exception.class, action = LOG_RETHROW, logLevel = ERROR)
 * void criticalOperation();
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Exception Specification
 * @see IMethodSpec
 * @see IRetSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares handled and passthrough failures, recovery, translation, retry, circuit, logging and alert preferences; it catches no exception and invokes no fallback.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IExceptionSpec.java}
 * blob {@code 0e882d578a4ceb1bca325d3466f0bccb87a3dbae} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(IExceptionSpecs.class)
public @interface IExceptionSpec {

    // --- SIMPLE OPTIONS ---

    /**
     * Predefined exception handling pattern.
     *
     * @return option
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#option intent: Predefined exception handling pattern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Option}; default expression {@code Option.NONE}.
     */
    Option option() default Option.NONE;

    /** Common exception patterns.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IExceptionSpec.Option choices described as Common exception patterns. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 8 constants in the existing source order.
     */
    enum Option {
        /** No preset - configure manually.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.Option#NONE policy choice: No preset - configure manually. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.Option; its declared order and identity are preserved.
         */
        NONE,

        /** Log and rethrow.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.Option#LOG_RETHROW policy choice: Log and rethrow. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.Option; its declared order and identity are preserved.
         */
        LOG_RETHROW,

        /** Log and suppress (return null).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.Option#LOG_SUPPRESS policy choice: Log and suppress (return null). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.Option; its declared order and identity are preserved.
         */
        LOG_SUPPRESS,

        /** Log and return default.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.Option#LOG_DEFAULT policy choice: Log and return default. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.Option; its declared order and identity are preserved.
         */
        LOG_DEFAULT,

        /** Translate to runtime exception.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.Option#WRAP_RUNTIME policy choice: Translate to runtime exception. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.Option; its declared order and identity are preserved.
         */
        WRAP_RUNTIME,

        /** Translate to checked exception.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.Option#WRAP_CHECKED policy choice: Translate to checked exception. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.Option; its declared order and identity are preserved.
         */
        WRAP_CHECKED,

        /** Retry with backoff.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.Option#RETRY policy choice: Retry with backoff. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.Option; its declared order and identity are preserved.
         */
        RETRY,

        /** Circuit breaker fallback.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.Option#CIRCUIT_BREAKER policy choice: Circuit breaker fallback. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.Option; its declared order and identity are preserved.
         */
        CIRCUIT_BREAKER
    }

    // --- EXCEPTION TYPES ---

    /**
     * Exception types to handle.
     *
     * @return exception classes
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#handle intent: Exception types to handle. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<? extends Throwable>[]}; default expression {@code {}}.
     */
    Class<? extends Throwable>[] handle() default {};

    /**
     * Exception types to NOT handle (passthrough).
     *
     * @return exception classes
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#passthrough intent: Exception types to NOT handle (passthrough). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<? extends Throwable>[]}; default expression {@code {}}.
     */
    Class<? extends Throwable>[] passthrough() default {};

    // --- ACTION ---

    /**
     * Action to take on exception.
     *
     * @return action
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#action intent: Action to take on exception. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Action}; default expression {@code Action.LOG_RETHROW}.
     */
    Action action() default Action.LOG_RETHROW;

    /** Exception handling actions.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IExceptionSpec.Action choices described as Exception handling actions. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 9 constants in the existing source order.
     */
    enum Action {
        /** Log and rethrow original.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.Action#LOG_RETHROW policy choice: Log and rethrow original. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.Action; its declared order and identity are preserved.
         */
        LOG_RETHROW,

        /** Log and suppress (return null/default).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.Action#LOG_SUPPRESS policy choice: Log and suppress (return null/default). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.Action; its declared order and identity are preserved.
         */
        LOG_SUPPRESS,

        /** Log and return fallback value.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.Action#LOG_FALLBACK policy choice: Log and return fallback value. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.Action; its declared order and identity are preserved.
         */
        LOG_FALLBACK,

        /** Translate to different exception.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.Action#TRANSLATE policy choice: Translate to different exception. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.Action; its declared order and identity are preserved.
         */
        TRANSLATE,

        /** Retry the operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.Action#RETRY policy choice: Retry the operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.Action; its declared order and identity are preserved.
         */
        RETRY,

        /** Use circuit breaker fallback.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.Action#CIRCUIT_BREAKER policy choice: Use circuit breaker fallback. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.Action; its declared order and identity are preserved.
         */
        CIRCUIT_BREAKER,

        /** Wrap in RuntimeException.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.Action#WRAP_RUNTIME policy choice: Wrap in RuntimeException. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.Action; its declared order and identity are preserved.
         */
        WRAP_RUNTIME,

        /** Call recovery method.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.Action#RECOVER policy choice: Call recovery method. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.Action; its declared order and identity are preserved.
         */
        RECOVER,

        /** Silently ignore (dangerous).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.Action#IGNORE policy choice: Silently ignore (dangerous). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.Action; its declared order and identity are preserved.
         */
        IGNORE
    }

    // --- FALLBACK ---

    /**
     * Fallback value (literal or SpEL expression).
     *
     * @return fallback value
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#fallback intent: Fallback value (literal or SpEL expression). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String fallback() default "";

    /**
     * Fallback method name in same class.
     *
     * @return method name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#fallbackMethod intent: Fallback method name in same class. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String fallbackMethod() default "";

    /**
     * Supplier class for fallback value.
     *
     * @return supplier class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#fallbackSupplier intent: Supplier class for fallback value. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> fallbackSupplier() default Void.class;

    // --- TRANSLATION ---

    /**
     * Exception translation rules.
     *
     * @return translations
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#translate intent: Exception translation rules. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Translation[]}; default expression {@code {}}.
     */
    Translation[] translate() default {};

    /** Exception translation rule.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares source/target throwable types, message and cause-preservation preference; it constructs no exception and retains implicit CLASS retention without explicit Target restriction.
     * @m3.dataStructures CLASS (implicit default)-retained annotation targeting {@code all declaration contexts (no explicit Target)}; existing element and enum schemas are preserved.
     */
    @interface Translation {

        /** Source exception type.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExceptionSpec.Translation#from intent: Source exception type. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code Class<? extends Throwable>}; an explicit value is required.
         */
        Class<? extends Throwable> from();

        /** Target exception type.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExceptionSpec.Translation#to intent: Target exception type. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code Class<? extends Throwable>}; an explicit value is required.
         */
        Class<? extends Throwable> to();

        /** Message for new exception.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExceptionSpec.Translation#message intent: Message for new exception. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String message() default "";

        /** Whether to include cause.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExceptionSpec.Translation#includeCause intent: Whether to include cause. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
         */
        boolean includeCause() default true;
    }

    // --- LOGGING ---

    /**
     * Log level for exception.
     *
     * @return log level
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#logLevel intent: Log level for exception. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code LogLevel}; default expression {@code LogLevel.WARN}.
     */
    LogLevel logLevel() default LogLevel.WARN;

    /** Log levels.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IExceptionSpec.LogLevel choices described as Log levels. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum LogLevel {
        /** Trace level.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.LogLevel#TRACE policy choice: Trace level. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.LogLevel; its declared order and identity are preserved.
         */
        TRACE,
        /** Debug level.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.LogLevel#DEBUG policy choice: Debug level. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.LogLevel; its declared order and identity are preserved.
         */
        DEBUG,
        /** Info level.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.LogLevel#INFO policy choice: Info level. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.LogLevel; its declared order and identity are preserved.
         */
        INFO,
        /** Warning level.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.LogLevel#WARN policy choice: Warning level. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.LogLevel; its declared order and identity are preserved.
         */
        WARN,
        /** Error level.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.LogLevel#ERROR policy choice: Error level. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.LogLevel; its declared order and identity are preserved.
         */
        ERROR,
        /** No logging.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.LogLevel#NONE policy choice: No logging. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.LogLevel; its declared order and identity are preserved.
         */
        NONE
    }

    /**
     * Custom log message.
     *
     * @return log message
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#logMessage intent: Custom log message. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String logMessage() default "";

    /**
     * Whether to log stack trace.
     *
     * @return true to log stack trace
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#logStackTrace intent: Whether to log stack trace. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean logStackTrace() default true;

    // --- RETRY ---

    /**
     * Retry configuration.
     *
     * @return retry config
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#retry intent: Retry configuration. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Retry}; default expression {@code @Retry}.
     */
    Retry retry() default @Retry;

    /** Retry configuration.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares retry enabling, attempts, backoff and exception filters; it schedules no retry and retains implicit CLASS retention without explicit Target restriction.
     * @m3.dataStructures CLASS (implicit default)-retained annotation targeting {@code all declaration contexts (no explicit Target)}; existing element and enum schemas are preserved.
     */
    @interface Retry {

        /** Whether retry is enabled.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExceptionSpec.Retry#enabled intent: Whether retry is enabled. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
         */
        boolean enabled() default false;

        /** Maximum retry attempts.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExceptionSpec.Retry#maxAttempts intent: Maximum retry attempts. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 3}.
         */
        int maxAttempts() default 3;

        /** Initial backoff in milliseconds.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExceptionSpec.Retry#backoffMs intent: Initial backoff in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 1000}.
         */
        int backoffMs() default 1000;

        /** Backoff multiplier.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExceptionSpec.Retry#multiplier intent: Backoff multiplier. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 2.0}.
         */
        double multiplier() default 2.0;

        /** Maximum backoff in milliseconds.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExceptionSpec.Retry#maxBackoffMs intent: Maximum backoff in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 30000}.
         */
        int maxBackoffMs() default 30000;

        /** Exceptions that trigger retry.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExceptionSpec.Retry#retryOn intent: Exceptions that trigger retry. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code Class<? extends Throwable>[]}; default expression {@code {}}.
         */
        Class<? extends Throwable>[] retryOn() default {};

        /** Exceptions that should not retry.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExceptionSpec.Retry#noRetryOn intent: Exceptions that should not retry. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code Class<? extends Throwable>[]}; default expression {@code {}}.
         */
        Class<? extends Throwable>[] noRetryOn() default {};
    }

    // --- CIRCUIT BREAKER ---

    /**
     * Circuit breaker configuration.
     *
     * @return circuit breaker config
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#circuitBreaker intent: Circuit breaker configuration. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code CircuitBreaker}; default expression {@code @CircuitBreaker}.
     */
    CircuitBreaker circuitBreaker() default @CircuitBreaker;

    /** Circuit breaker configuration.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares circuit failure threshold, timing and fallback expression; it maintains no circuit and retains implicit CLASS retention without explicit Target restriction.
     * @m3.dataStructures CLASS (implicit default)-retained annotation targeting {@code all declaration contexts (no explicit Target)}; existing element and enum schemas are preserved.
     */
    @interface CircuitBreaker {

        /** Whether circuit breaker is enabled.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExceptionSpec.CircuitBreaker#enabled intent: Whether circuit breaker is enabled. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
         */
        boolean enabled() default false;

        /** Failure threshold to open circuit.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExceptionSpec.CircuitBreaker#failureThreshold intent: Failure threshold to open circuit. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 5}.
         */
        int failureThreshold() default 5;

        /** Time window for failure counting (ms).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExceptionSpec.CircuitBreaker#windowMs intent: Time window for failure counting (ms). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 60000}.
         */
        int windowMs() default 60000;

        /** Time before attempting reset (ms).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExceptionSpec.CircuitBreaker#resetTimeoutMs intent: Time before attempting reset (ms). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 30000}.
         */
        int resetTimeoutMs() default 30000;

        /** Fallback when circuit is open.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IExceptionSpec.CircuitBreaker#openFallback intent: Fallback when circuit is open. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String openFallback() default "";
    }

    // --- RECOVERY ---

    /**
     * Recovery method name. Method must accept (Exception) and return same type.
     *
     * @return recovery method
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#recoverMethod intent: Recovery method name. Method must accept (Exception) and return same type. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String recoverMethod() default "";

    /**
     * Recovery handler class.
     *
     * @return handler class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#recoverHandler intent: Recovery handler class. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> recoverHandler() default Void.class;

    // --- METRICS ---

    /**
     * Whether to count exception occurrences.
     *
     * @return true to count
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#countExceptions intent: Whether to count exception occurrences. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean countExceptions() default false;

    /**
     * Metric name for exception counter.
     *
     * @return metric name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#metricName intent: Metric name for exception counter. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String metricName() default "";

    // --- ALERTING ---

    /**
     * Whether this exception should trigger alert.
     *
     * @return true to alert
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#alert intent: Whether this exception should trigger alert. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean alert() default false;

    /**
     * Alert severity.
     *
     * @return severity
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#alertSeverity intent: Alert severity. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code AlertSeverity}; default expression {@code AlertSeverity.WARNING}.
     */
    AlertSeverity alertSeverity() default AlertSeverity.WARNING;

    /** Alert severities.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IExceptionSpec.AlertSeverity choices described as Alert severities. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum AlertSeverity {
        /** Info - not urgent.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.AlertSeverity#INFO policy choice: Info - not urgent. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.AlertSeverity; its declared order and identity are preserved.
         */
        INFO,
        /** Warning - needs attention.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.AlertSeverity#WARNING policy choice: Warning - needs attention. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.AlertSeverity; its declared order and identity are preserved.
         */
        WARNING,
        /** Error - needs prompt action.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.AlertSeverity#ERROR policy choice: Error - needs prompt action. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.AlertSeverity; its declared order and identity are preserved.
         */
        ERROR,
        /** Critical - immediate action.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IExceptionSpec.AlertSeverity#CRITICAL policy choice: Critical - immediate action. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IExceptionSpec.AlertSeverity; its declared order and identity are preserved.
         */
        CRITICAL
    }

    // --- DESCRIPTION ---

    /**
     * Description of exception handling.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#value intent: Description of exception handling. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IExceptionSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;
}
