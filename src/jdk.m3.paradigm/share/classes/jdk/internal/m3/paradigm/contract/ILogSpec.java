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
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ILogSpec - Logging specification for AI code generation.
 *
 * <h2>Inspired By Human IDLs</h2>
 *
 * <pre>
 * Humans invented many specification languages:
 *   - Syslog RFC 5424 (structured logging)
 *   - Log4j patterns (format specification)
 *   - ELK format (Elasticsearch, Logstash, Kibana)
 *   - Splunk CIM (Common Information Model)
 *   - OpenTelemetry (distributed tracing)
 *
 * &#64;ILogSpec unifies these into IOP-style code specification.
 * </pre>
 *
 * <h2>Generated Code PatternKind</h2>
 *
 * <pre>{@code
 * // From: @ILogSpec(level = INFO, format = STRUCTURED, mdc = {"userId",
 * // "requestId"})
 * // Generates:
 *
 * private static final Logger log = LoggerFactory.getLogger(MyService.class);
 *
 * public Result process(Request req) {
 * 	try (var ignored = MDC.putCloseable("userId", req.userId());
 * 			var ignored2 = MDC.putCloseable("requestId", req.requestId())) {
 *
 * 		log.info("Processing request", kv("operation", "process"), kv("inputSize", req.size()));
 *
 * 		// ... method body ...
 *
 * 		log.info("Request completed", kv("operation", "process"), kv("durationMs", duration),
 * 				kv("status", "success"));
 * 		return result;
 *
 * 	} catch (Exception e) {
 * 		log.error("Request failed", kv("operation", "process"), kv("error", e.getMessage()), e);
 * 		throw e;
 * 	}
 * }
 * }</pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * &#64;ILogSpec(level = Level.INFO, format = Format.STRUCTURED, mdc = {"userId", "requestId",
 * 		"correlationId"}, entryMessage = "Starting {operation}", exitMessage = "Completed {operation} in {durationMs}ms", errorMessage = "Failed {operation}: {error}", includeArgs = true, includeResult = false, // Don't
 * 																																																					// log
 * 																																																					// sensitive
 * 																																																					// data
 * 		includeDuration = true)
 * public class UserService {
 * 	// All methods get structured logging
 * }
 * }</pre>
 *
 * @since IOP 1.0 - Logging Specification
 * @see IMethodSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares logging levels, formats, templates, context propagation, exclusions and sampling hints; it emits no log message and performs no redaction itself.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.TYPE, ElementType.METHOD}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/ILogSpec.java}
 * blob {@code 15da6262d1413171f6c45fc43c7aaacc557bf3c8} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface ILogSpec {

    // --- LOG LEVEL ---

    /**
     * Default log level for entry/exit messages.
     *
     * @return log level
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#level intent: Default log level for entry/exit messages. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Level}; default expression {@code Level.INFO}.
     */
    Level level() default Level.INFO;

    /**
     * Log level for error messages.
     *
     * @return error log level
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#errorLevel intent: Log level for error messages. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Level}; default expression {@code Level.ERROR}.
     */
    Level errorLevel() default Level.ERROR;

    /** Log levels (SLF4J compatible).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ILogSpec.Level choices described as Log levels (SLF4J compatible). Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum Level {
        /** Trace level - most verbose.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ILogSpec.Level#TRACE policy choice: Trace level - most verbose. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ILogSpec.Level; its declared order and identity are preserved.
         */
        TRACE,
        /** Debug level - development info.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ILogSpec.Level#DEBUG policy choice: Debug level - development info. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ILogSpec.Level; its declared order and identity are preserved.
         */
        DEBUG,
        /** Info level - normal operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ILogSpec.Level#INFO policy choice: Info level - normal operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ILogSpec.Level; its declared order and identity are preserved.
         */
        INFO,
        /** Warn level - potential issues.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ILogSpec.Level#WARN policy choice: Warn level - potential issues. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ILogSpec.Level; its declared order and identity are preserved.
         */
        WARN,
        /** Error level - failures.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ILogSpec.Level#ERROR policy choice: Error level - failures. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ILogSpec.Level; its declared order and identity are preserved.
         */
        ERROR
    }

    // --- FORMAT ---

    /**
     * Log message format.
     *
     * @return format type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#format intent: Log message format. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Format}; default expression {@code Format.STRUCTURED}.
     */
    Format format() default Format.STRUCTURED;

    /** Log format types.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ILogSpec.Format choices described as Log format types. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum Format {
        /** Simple text messages.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ILogSpec.Format#SIMPLE policy choice: Simple text messages. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ILogSpec.Format; its declared order and identity are preserved.
         */
        SIMPLE,
        /** Structured key-value pairs (JSON-friendly).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ILogSpec.Format#STRUCTURED policy choice: Structured key-value pairs (JSON-friendly). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ILogSpec.Format; its declared order and identity are preserved.
         */
        STRUCTURED,
        /** OpenTelemetry compatible.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ILogSpec.Format#OTEL policy choice: OpenTelemetry compatible. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ILogSpec.Format; its declared order and identity are preserved.
         */
        OTEL,
        /** ELK stack compatible.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ILogSpec.Format#ELK policy choice: ELK stack compatible. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ILogSpec.Format; its declared order and identity are preserved.
         */
        ELK
    }

    // --- MDC (Mapped Diagnostic Context) ---

    /**
     * MDC keys to propagate from method parameters. Parameters matching these names
     * are added to MDC.
     *
     * @return MDC key names
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#mdc intent: MDC keys to propagate from method parameters. Parameters matching these names are added to MDC. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] mdc() default {};

    /**
     * Whether to auto-detect common MDC fields (userId, requestId, etc.).
     *
     * @return true to auto-detect
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#autoMdc intent: Whether to auto-detect common MDC fields (userId, requestId, etc.). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean autoMdc() default true;

    // --- MESSAGE TEMPLATES ---

    /**
     * Entry message template. Placeholders: {method}, {class}, {args}, {operation}
     *
     * @return entry message template
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#entryMessage intent: Entry message template. Placeholders: {method}, {class}, {args}, {operation} Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "Entering {method}"}.
     */
    String entryMessage() default "Entering {method}";

    /**
     * Exit message template. Placeholders: {method}, {class}, {result},
     * {durationMs}, {operation}
     *
     * @return exit message template
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#exitMessage intent: Exit message template. Placeholders: {method}, {class}, {result}, {durationMs}, {operation} Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "Exiting {method} in {durationMs}ms"}.
     */
    String exitMessage() default "Exiting {method} in {durationMs}ms";

    /**
     * Error message template. Placeholders: {method}, {class}, {error},
     * {errorType}, {operation}
     *
     * @return error message template
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#errorMessage intent: Error message template. Placeholders: {method}, {class}, {error}, {errorType}, {operation} Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "Error in {method}: {error}"}.
     */
    String errorMessage() default "Error in {method}: {error}";

    // --- CONTENT OPTIONS ---

    /**
     * Whether to log method arguments. SECURITY: Set false for methods with
     * sensitive data.
     *
     * @return true to log arguments
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#includeArgs intent: Whether to log method arguments. SECURITY: Set false for methods with sensitive data. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean includeArgs() default true;

    /**
     * Whether to log return value. SECURITY: Set false for methods returning
     * sensitive data.
     *
     * @return true to log result
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#includeResult intent: Whether to log return value. SECURITY: Set false for methods returning sensitive data. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean includeResult() default false;

    /**
     * Whether to log execution duration.
     *
     * @return true to log duration
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#includeDuration intent: Whether to log execution duration. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean includeDuration() default true;

    /**
     * Whether to log stack trace on errors.
     *
     * @return true to include stack trace
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#includeStackTrace intent: Whether to log stack trace on errors. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean includeStackTrace() default true;

    /**
     * Parameter names to exclude from logging (sensitive data).
     *
     * @return parameter names to exclude
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#excludeParams intent: Parameter names to exclude from logging (sensitive data). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {"password", "secret", "token", "key", "credential"}}.
     */
    String[] excludeParams() default {"password", "secret", "token", "key", "credential"};

    /**
     * Maximum length for logged values (truncate longer).
     *
     * @return max value length (0 = no limit)
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#maxValueLength intent: Maximum length for logged values (truncate longer). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 1000}.
     */
    int maxValueLength() default 1000;

    // --- PERFORMANCE ---

    /**
     * Threshold in ms before logging as slow operation. 0 = disable slow logging.
     *
     * @return slow threshold in ms
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#slowThresholdMs intent: Threshold in ms before logging as slow operation. 0 = disable slow logging. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code 0}.
     */
    long slowThresholdMs() default 0;

    /**
     * Log level for slow operations.
     *
     * @return slow operation log level
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#slowLevel intent: Log level for slow operations. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Level}; default expression {@code Level.WARN}.
     */
    Level slowLevel() default Level.WARN;

    // --- SAMPLING ---

    /**
     * Sampling rate (0.0-1.0). 1.0 = log all, 0.1 = log 10%. Useful for
     * high-throughput methods.
     *
     * @return sampling rate
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#samplingRate intent: Sampling rate (0.0-1.0). 1.0 = log all, 0.1 = log 10%. Useful for high-throughput methods. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 1.0}.
     */
    double samplingRate() default 1.0;

    // --- CORRELATION ---

    /**
     * Whether to generate correlation ID if not present.
     *
     * @return true to generate correlation ID
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#generateCorrelationId intent: Whether to generate correlation ID if not present. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean generateCorrelationId() default false;

    /**
     * Header/key name for correlation ID.
     *
     * @return correlation ID key
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ILogSpec#correlationIdKey intent: Header/key name for correlation ID. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "correlationId"}.
     */
    String correlationIdKey() default "correlationId";
}
