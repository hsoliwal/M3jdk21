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
 * IAspectSpec - Aspect-Oriented specification for cross-cutting concerns.
 *
 * <h2>IOP Paradigm Stack: AOP Layer</h2>
 *
 * <pre>
 * COP → AOP → OOP → FOP → IOP → AnOP
 *        ↑
 *   &#64;IAspectSpec lives here
 *
 * AOP handles cross-cutting concerns:
 *   - Logging (before/after method calls)
 *   - Security (authorization checks)
 *   - Transactions (begin/commit/rollback)
 *   - Caching (memoization)
 *   - Retry logic (fault tolerance)
 *   - Metrics (timing, counting)
 * </pre>
 *
 * <h2>Compile-Time AOP (No Runtime Magic)</h2>
 *
 * <pre>
 * IOP generates aspect code at COMPILE TIME:
 *   - No runtime proxies
 *   - No reflection
 *   - No dynamic weaving
 *   - Just generated wrapper code
 *
 * "AOP ends when wrappers are generated"
 * </pre>
 *
 * <h2>Generated Code PatternKind</h2>
 *
 * <pre>{@code
 * // From: @IAspectSpec(advice = AROUND, concern = LOGGING)
 * // Applied to: public User findById(String id)
 * // Generates:
 *
 * public User findById(String id) {
 * 	log.info("Entering findById({})", id);
 * 	long start = System.nanoTime();
 * 	try {
 * 		User result = findById_original(id);
 * 		log.info("Exiting findById in {}ms", (System.nanoTime() - start) / 1_000_000);
 * 		return result;
 * 	} catch (Exception e) {
 * 		log.error("Error in findById: {}", e.getMessage(), e);
 * 		throw e;
 * 	}
 * }
 *
 * private User findById_original(String id) {
 * 	// Original implementation
 * }
 * }</pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 * // Method-level aspect
 * &#64;IAspectSpec(
 *     advice = Advice.AROUND,
 *     concern = Concern.LOGGING,
 *     pointcut = "execution(* find*(..))"
 * )
 * public interface IUserRepository&lt;Self extends IUserRepository&lt;Self>> extends IContext&lt;Self> {
 *     User findById(String id);
 *     List&lt;User> findByName(String name);
 * }
 *
 * // Class-level aspect (applies to all methods)
 * &#64;IAspectSpec(concern = Concern.TRANSACTION)
 * public class OrderService {
 *     // All methods wrapped in transaction
 * }
 *
 * // Multiple aspects
 * &#64;IAspectSpec(concern = Concern.SECURITY)
 * &#64;IAspectSpec(concern = Concern.AUDIT)
 * public void deleteUser(String id) { ... }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Aspect Specification
 * @see IMethodSpec
 * @see ILogSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares advice timing, concern, pointcut, implementation selector and concern-specific settings; it weaves no aspect and enforces no cross-cutting behavior.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.TYPE, ElementType.METHOD}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAspectSpec.java}
 * blob {@code c6058965c8448c34295b5e2733b11c7bb2402984} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(IAspectSpecs.class)
public @interface IAspectSpec {

    // --- ADVICE TYPE ---

    /**
     * Type of advice to apply.
     *
     * @return advice type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#advice intent: Type of advice to apply. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Advice}; default expression {@code Advice.AROUND}.
     */
    Advice advice() default Advice.AROUND;

    /** Advice types (when to execute aspect code).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAspectSpec.Advice choices described as Advice types (when to execute aspect code). Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum Advice {
        /** Execute before method.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Advice#BEFORE policy choice: Execute before method. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Advice; its declared order and identity are preserved.
         */
        BEFORE,

        /** Execute after method (success only).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Advice#AFTER_RETURNING policy choice: Execute after method (success only). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Advice; its declared order and identity are preserved.
         */
        AFTER_RETURNING,

        /** Execute after method throws exception.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Advice#AFTER_THROWING policy choice: Execute after method throws exception. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Advice; its declared order and identity are preserved.
         */
        AFTER_THROWING,

        /** Execute after method (success or exception).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Advice#AFTER policy choice: Execute after method (success or exception). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Advice; its declared order and identity are preserved.
         */
        AFTER,

        /** Wrap entire method execution.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Advice#AROUND policy choice: Wrap entire method execution. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Advice; its declared order and identity are preserved.
         */
        AROUND
    }

    // --- CROSS-CUTTING CONCERN ---

    /**
     * Cross-cutting concern to apply.
     *
     * @return concern type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#concern intent: Cross-cutting concern to apply. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Concern}; default expression {@code Concern.CUSTOM}.
     */
    Concern concern() default Concern.CUSTOM;

    /** Predefined cross-cutting concerns.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAspectSpec.Concern choices described as Predefined cross-cutting concerns. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 13 constants in the existing source order.
     */
    enum Concern {
        /** Custom aspect (use aspectClass).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Concern#CUSTOM policy choice: Custom aspect (use aspectClass). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Concern; its declared order and identity are preserved.
         */
        CUSTOM,

        /** Logging (entry, exit, errors).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Concern#LOGGING policy choice: Logging (entry, exit, errors). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Concern; its declared order and identity are preserved.
         */
        LOGGING,

        /** Security/authorization check.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Concern#SECURITY policy choice: Security/authorization check. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Concern; its declared order and identity are preserved.
         */
        SECURITY,

        /** Transaction management.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Concern#TRANSACTION policy choice: Transaction management. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Concern; its declared order and identity are preserved.
         */
        TRANSACTION,

        /** Result caching/memoization.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Concern#CACHING policy choice: Result caching/memoization. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Concern; its declared order and identity are preserved.
         */
        CACHING,

        /** Retry on failure.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Concern#RETRY policy choice: Retry on failure. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Concern; its declared order and identity are preserved.
         */
        RETRY,

        /** Circuit breaker pattern.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Concern#CIRCUIT_BREAKER policy choice: Circuit breaker pattern. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Concern; its declared order and identity are preserved.
         */
        CIRCUIT_BREAKER,

        /** Rate limiting.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Concern#RATE_LIMIT policy choice: Rate limiting. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Concern; its declared order and identity are preserved.
         */
        RATE_LIMIT,

        /** Timeout enforcement.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Concern#TIMEOUT policy choice: Timeout enforcement. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Concern; its declared order and identity are preserved.
         */
        TIMEOUT,

        /** Metrics collection.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Concern#METRICS policy choice: Metrics collection. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Concern; its declared order and identity are preserved.
         */
        METRICS,

        /** Audit trail.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Concern#AUDIT policy choice: Audit trail. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Concern; its declared order and identity are preserved.
         */
        AUDIT,

        /** Validation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Concern#VALIDATION policy choice: Validation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Concern; its declared order and identity are preserved.
         */
        VALIDATION,

        /** Exception translation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Concern#EXCEPTION_TRANSLATION policy choice: Exception translation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Concern; its declared order and identity are preserved.
         */
        EXCEPTION_TRANSLATION
    }

    // --- POINTCUT ---

    /**
     * Pointcut expression (AspectJ-like syntax). Defaults to the annotated
     * method/class.
     *
     * <pre>
     * Examples:
     *   "execution(* find*(..))"           - All find methods
     *   "execution(public * *(..))"        - All public methods
     *   "execution(* *Service.*(..))"      - All methods in *Service classes
     *   "@annotation(Transactional)"       - Methods with @Transactional
     * </pre>
     *
     * @return pointcut expression
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#pointcut intent: Requested AspectJ-like selection expression, with empty default referring to the annotated method/class; the annotation parses or applies no pointcut. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String pointcut() default "";

    // --- CUSTOM ASPECT ---

    /**
     * Custom aspect class for CUSTOM concern. Must implement appropriate functional
     * interface.
     *
     * @return aspect class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#aspectClass intent: Custom aspect class for CUSTOM concern. Must implement appropriate functional interface. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> aspectClass() default Void.class;

    /**
     * Method name in aspect class to invoke.
     *
     * @return aspect method name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#aspectMethod intent: Method name in aspect class to invoke. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "apply"}.
     */
    String aspectMethod() default "apply";

    // --- LOGGING OPTIONS ---

    /**
     * Log level for LOGGING concern.
     *
     * @return log level
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#logLevel intent: Log level for LOGGING concern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code ILogSpec.Level}; default expression {@code ILogSpec.Level.INFO}.
     */
    ILogSpec.Level logLevel() default ILogSpec.Level.INFO;

    /**
     * Whether to log method arguments.
     *
     * @return true to log args
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#logArgs intent: Whether to log method arguments. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean logArgs() default true;

    /**
     * Whether to log return value.
     *
     * @return true to log result
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#logResult intent: Whether to log return value. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean logResult() default false;

    // --- TRANSACTION OPTIONS ---

    /**
     * Transaction propagation for TRANSACTION concern.
     *
     * @return propagation type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#propagation intent: Transaction propagation for TRANSACTION concern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Propagation}; default expression {@code Propagation.REQUIRED}.
     */
    Propagation propagation() default Propagation.REQUIRED;

    /** Transaction propagation types.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAspectSpec.Propagation choices described as Transaction propagation types. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 7 constants in the existing source order.
     */
    enum Propagation {
        /** Create new or use existing transaction.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Propagation#REQUIRED policy choice: Create new or use existing transaction. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Propagation; its declared order and identity are preserved.
         */
        REQUIRED,

        /** Create new transaction, suspend existing.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Propagation#REQUIRES_NEW policy choice: Create new transaction, suspend existing. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Propagation; its declared order and identity are preserved.
         */
        REQUIRES_NEW,

        /** Use existing transaction, error if none.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Propagation#MANDATORY policy choice: Use existing transaction, error if none. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Propagation; its declared order and identity are preserved.
         */
        MANDATORY,

        /** Use existing transaction, or run without.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Propagation#SUPPORTS policy choice: Use existing transaction, or run without. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Propagation; its declared order and identity are preserved.
         */
        SUPPORTS,

        /** Run without transaction, suspend existing.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Propagation#NOT_SUPPORTED policy choice: Run without transaction, suspend existing. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Propagation; its declared order and identity are preserved.
         */
        NOT_SUPPORTED,

        /** Run without transaction, error if exists.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Propagation#NEVER policy choice: Run without transaction, error if exists. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Propagation; its declared order and identity are preserved.
         */
        NEVER,

        /** Nested transaction (savepoint).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAspectSpec.Propagation#NESTED policy choice: Nested transaction (savepoint). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAspectSpec.Propagation; its declared order and identity are preserved.
         */
        NESTED
    }

    /**
     * Whether transaction is read-only.
     *
     * @return true for read-only
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#readOnly intent: Whether transaction is read-only. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean readOnly() default false;

    /**
     * Exceptions that trigger rollback.
     *
     * @return exception classes
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#rollbackFor intent: Exceptions that trigger rollback. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<? extends Throwable>[]}; default expression {@code {}}.
     */
    Class<? extends Throwable>[] rollbackFor() default {};

    // --- RETRY OPTIONS ---

    /**
     * Maximum retry attempts for RETRY concern.
     *
     * @return max attempts
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#maxAttempts intent: Maximum retry attempts for RETRY concern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 3}.
     */
    int maxAttempts() default 3;

    /**
     * Delay between retries in milliseconds.
     *
     * @return delay in ms
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#retryDelayMs intent: Delay between retries in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code 1000}.
     */
    long retryDelayMs() default 1000;

    /**
     * Backoff multiplier for retry delay.
     *
     * @return backoff multiplier
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#backoffMultiplier intent: Backoff multiplier for retry delay. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 2.0}.
     */
    double backoffMultiplier() default 2.0;

    /**
     * Exceptions that should trigger retry.
     *
     * @return retryable exceptions
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#retryOn intent: Exceptions that should trigger retry. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<? extends Throwable>[]}; default expression {@code {Exception.class}}.
     */
    Class<? extends Throwable>[] retryOn() default {Exception.class};

    // --- CACHING OPTIONS ---

    /**
     * Cache name for CACHING concern.
     *
     * @return cache name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#cacheName intent: Cache name for CACHING concern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String cacheName() default "";

    /**
     * Cache key expression.
     *
     * @return key expression
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#cacheKey intent: Cache key expression. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String cacheKey() default "";

    /**
     * Time-to-live in seconds for cache entries.
     *
     * @return TTL in seconds
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#cacheTtlSeconds intent: Time-to-live in seconds for cache entries. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int cacheTtlSeconds() default 0;

    // --- TIMEOUT OPTIONS ---

    /**
     * Timeout in milliseconds for TIMEOUT concern.
     *
     * @return timeout in ms
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#timeoutMs intent: Timeout in milliseconds for TIMEOUT concern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}; default expression {@code 30000}.
     */
    long timeoutMs() default 30000;

    // --- SECURITY OPTIONS ---

    /**
     * Required roles for SECURITY concern.
     *
     * @return required roles
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#roles intent: Required roles for SECURITY concern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] roles() default {};

    /**
     * Required permissions for SECURITY concern.
     *
     * @return required permissions
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#permissions intent: Required permissions for SECURITY concern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] permissions() default {};

    // --- ORDER ---

    /**
     * Order of aspect execution (lower = earlier). Important when multiple aspects
     * apply.
     *
     * @return execution order
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#order intent: Order of aspect execution (lower = earlier). Important when multiple aspects apply. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int order() default 0;

    // --- MODEL ---

    /**
     * Preferred model for generating aspect code.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAspectSpec#model intent: Preferred model for generating aspect code. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;
}
