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

import jdk.internal.m3.collections.PlaceholderLogger;

/**
 * IOP Exception Law - Exception Handling Philosophy and Rules.
 *
 * <h2>THE IOP EXCEPTION PHILOSOPHY</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║   🔒 IOP EXCEPTION RULES (LOCKED - NON-NEGOTIABLE)                       ║
 * ║                                                                           ║
 * ║   1. EXCEPTIONS ARE PART OF THE CONTRACT                                 ║
 * ║      - Declared in interface (throws clause or @IExceptionSpec)          ║
 * ║      - Documented in JavaDoc                                             ║
 * ║      - Part of the API, not an afterthought                              ║
 * ║                                                                           ║
 * ║   2. NO SILENT SWALLOWING                                                ║
 * ║      - catch(Exception e) {} is FORBIDDEN                                ║
 * ║      - Every exception must be: logged, translated, or propagated        ║
 * ║      - "Ignore" option exists but requires explicit @IExceptionSpec      ║
 * ║                                                                           ║
 * ║   3. EXCEPTION HIERARCHY MATTERS                                         ║
 * ║      - Domain exceptions extend from domain base                         ║
 * ║      - Infrastructure exceptions wrap with context                       ║
 * ║      - Never lose the root cause                                         ║
 * ║                                                                           ║
 * ║   4. RECOVERY IS EXPLICIT                                                ║
 * ║      - Retry logic declared in @IExceptionSpec                           ║
 * ║      - Circuit breakers declared in @IExceptionSpec                      ║
 * ║      - Fallbacks declared in @IExceptionSpec                             ║
 * ║      - LLM cannot invent recovery strategies                             ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * @since IOP 1.0
 * @see IExceptionSpec
 * @see IAIErrorPolicy
 * @m3.pattern synexia:iop
 * @m3.algorithm Exposes immutable exception-law text and enum metadata without enforcing exception handling.
 * @m3.dataStructures Final utility class with String literal constants and nested enums.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IOP_EXCEPTION_LAW.java}
 * blob {@code 280c83b9acf951b0d0333af47696bdd928d1104d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IOP_EXCEPTION_LAW {
    private static final PlaceholderLogger log = PlaceholderLogger.of(IOP_EXCEPTION_LAW.class);


    /** Restricts construction of the law catalog.
     * @m3.pattern synexia:iop
     * @m3.algorithm Prevents external construction through the existing private empty constructor.
     * @m3.dataStructures No explicit constructor parameters or instance state.
     */
    private IOP_EXCEPTION_LAW() {}

    // --- RULE 1: EXCEPTIONS ARE PART OF THE CONTRACT ---

    /**
     * Exceptions are part of the contract.
     *
     * <p>
     * In IOP, exceptions are not afterthoughts. They are:
     * <ul>
     * <li>Declared in the interface (throws clause or @IExceptionSpec)</li>
     * <li>Documented in JavaDoc</li>
     * <li>Part of the API specification</li>
     * </ul>
     *
     * <pre>{@code
     * // GOOD - Exception is part of contract
     * interface IUserService {
     *     /**
     *      * Finds user by ID.
     *      * @throws UserNotFoundException if user not found
     *      *\/
     *     &#64;IExceptionSpec(handle = UserNotFoundException.class, action = PROPAGATE)
     *     User findById(String id) throws UserNotFoundException;
     * }
     *
     * // BAD - Exception not declared
     * interface IUserService {
     *     User findById(String id);  // throws hidden RuntimeException
     * }
     * }</pre>
     * @m3.pattern synexia:iop
     * @m3.algorithm Retains the exact RULE_1 exception-law documentation payload for consumers.
     * @m3.dataStructures Existing static final String text block preserving literal content and Java constant identity.
     */
    public static final String RULE_1 = """
			EXCEPTIONS ARE PART OF THE CONTRACT.
			- Declared in interface (throws clause or @IExceptionSpec)
			- Documented in JavaDoc
			- Part of the API, not an afterthought
			""";

    // --- RULE 2: NO SILENT SWALLOWING ---

    /**
     * No silent swallowing of exceptions.
     *
     * <p>
     * The following is FORBIDDEN in IOP:
     *
     * <pre>{@code
     * // FORBIDDEN - Silent swallowing
     * try {
     * 	dangerousOperation();
     * } catch (Exception e) {
     * 	// Empty catch block - ILLEGAL
     * }
     *
     * // REQUIRED - Explicit handling
     * try {
     * 	dangerousOperation();
     * } catch (Exception e) {
     * 	log.error("Operation failed", e); // At minimum: log
     * 	throw new DomainException("Context", e); // Or: translate
     * 	// Or: return fallback with @IExceptionSpec
     * }
     * }</pre>
     * @m3.pattern synexia:iop
     * @m3.algorithm Retains the exact RULE_2 exception-law documentation payload for consumers.
     * @m3.dataStructures Existing static final String text block preserving literal content and Java constant identity.
     */
    public static final String RULE_2 = """
			NO SILENT SWALLOWING.
			- catch(Exception e) {} is FORBIDDEN
			- Every exception must be: logged, translated, or propagated
			- "Ignore" option requires explicit @IExceptionSpec(action = IGNORE)
			""";

    /** Empty catch blocks are forbidden.
     * @m3.pattern synexia:iop
     * @m3.algorithm Retains the exact EMPTY_CATCH_FORBIDDEN exception-law documentation payload for consumers.
     * @m3.dataStructures Existing static final String text block preserving literal content and Java constant identity.
     */
    public static final String EMPTY_CATCH_FORBIDDEN = """
			// FORBIDDEN:
			catch (Exception e) {}
			catch (Exception e) { /* ignore */ }
			catch (Exception ignored) {}

			// REQUIRED (at minimum):
			catch (Exception e) { log.warn("...", e); }
			""";

    // --- RULE 3: EXCEPTION HIERARCHY ---

    /**
     * Exception hierarchy matters.
     *
     * <pre>
     * Domain Exception Hierarchy:
     *
     *   DomainException (base)
     *     ├── ValidationException
     *     │     ├── InvalidInputException
     *     │     └── ConstraintViolationException
     *     ├── EntityException
     *     │     ├── EntityNotFoundException
     *     │     └── EntityAlreadyExistsException
     *     └── BusinessRuleException
     *           ├── InsufficientFundsException
     *           └── OperationNotAllowedException
     *
     *   InfrastructureException (base)
     *     ├── DataAccessException
     *     ├── ExternalServiceException
     *     └── ConfigurationException
     * </pre>
     * @m3.pattern synexia:iop
     * @m3.algorithm Retains the exact RULE_3 exception-law documentation payload for consumers.
     * @m3.dataStructures Existing static final String text block preserving literal content and Java constant identity.
     */
    public static final String RULE_3 = """
			EXCEPTION HIERARCHY MATTERS.
			- Domain exceptions extend from domain base
			- Infrastructure exceptions wrap with context
			- Never lose the root cause (use initCause or constructor)
			""";

    /** IOP Exception hierarchy template.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 9 symbolic ExceptionCategory choices for law metadata; selecting a token performs no enforcement.
     * @m3.dataStructures Nested enum preserving declaration order, names and identities.
     */
    public enum ExceptionCategory {
        /** Business/domain logic exceptions.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the DOMAIN exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        DOMAIN,

        /** Validation failures.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the VALIDATION exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        VALIDATION,

        /** Entity/resource not found.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the NOT_FOUND exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        NOT_FOUND,

        /** Entity already exists.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the ALREADY_EXISTS exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        ALREADY_EXISTS,

        /** Business rule violation.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the BUSINESS_RULE exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        BUSINESS_RULE,

        /** Infrastructure/technical exceptions.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the INFRASTRUCTURE exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        INFRASTRUCTURE,

        /** External service failures.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the EXTERNAL_SERVICE exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        EXTERNAL_SERVICE,

        /** Configuration errors.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CONFIGURATION exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        CONFIGURATION,

        /** Security/authorization failures.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SECURITY exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        SECURITY
    }

    // --- RULE 4: RECOVERY IS EXPLICIT ---

    /**
     * Recovery strategies must be explicit.
     *
     * <p>
     * LLM cannot invent recovery strategies. All recovery must be declared
     * via @IExceptionSpec:
     *
     * <pre>
     * {@code
     *
     * // Retry - declared explicitly
     * &#64;IExceptionSpec(handle = TimeoutException.class, retry = @Retry(maxAttempts = 3, backoffMs = 1000))
     * Response callService();
     *
     * // Circuit breaker - declared explicitly
     * &#64;IExceptionSpec(handle = ServiceException.class, circuitBreaker = @CircuitBreaker(failureThreshold = 5, resetTimeoutMs = 30000))
     * Response callExternalApi();
     *
     * // Fallback - declared explicitly
     * @IExceptionSpec(handle = Exception.class, fallbackMethod = "getDefaultValue")
     * Config getConfig();
     * }
     * </pre>
     * @m3.pattern synexia:iop
     * @m3.algorithm Retains the exact RULE_4 exception-law documentation payload for consumers.
     * @m3.dataStructures Existing static final String text block preserving literal content and Java constant identity.
     */
    public static final String RULE_4 = """
			RECOVERY IS EXPLICIT.
			- Retry logic declared in @IExceptionSpec
			- Circuit breakers declared in @IExceptionSpec
			- Fallbacks declared in @IExceptionSpec
			- LLM cannot invent recovery strategies
			""";

    // --- ERROR POLICY TYPES ---

    /**
     * Error policy types for semantic expectation.
     *
     * <p>
     * Use @IAIErrorPolicy to declare the error handling philosophy:
     *
     * <pre>
     * {@code
     * // Payment processing - must fail fast
     * &#64;IAIErrorPolicy(FAIL_FAST)
     * interface IPaymentProcessor { ... }
     *
     * // Analytics - best effort is acceptable
     * &#64;IAIErrorPolicy(BEST_EFFORT)
     * interface IAnalyticsCollector { ... }
     *
     * // Gateway - propagate to caller
     * &#64;IAIErrorPolicy(PROPAGATE)
     * interface IApiGateway { ... }
     * }
     * </pre>
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 6 symbolic ErrorPolicy choices for law metadata; selecting a token performs no enforcement.
     * @m3.dataStructures Nested enum preserving declaration order, names and identities.
     */
    public enum ErrorPolicy {
        /**
         * FAIL_FAST - Fail immediately on first error.
         *
         * <p>
         * Use for: payments, critical operations, data integrity.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the FAIL_FAST exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        FAIL_FAST,

        /**
         * PROPAGATE - Propagate error to caller.
         *
         * <p>
         * Use for: gateways, proxies, passthrough services.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the PROPAGATE exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        PROPAGATE,

        /**
         * BEST_EFFORT - Continue processing, log errors.
         *
         * <p>
         * Use for: analytics, logging, non-critical operations.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the BEST_EFFORT exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        BEST_EFFORT,

        /**
         * RETRY_THEN_FAIL - Retry with backoff, then fail.
         *
         * <p>
         * Use for: network calls, external services.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the RETRY_THEN_FAIL exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        RETRY_THEN_FAIL,

        /**
         * CIRCUIT_BREAKER - Use circuit breaker pattern.
         *
         * <p>
         * Use for: external dependencies, cascading failure prevention.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CIRCUIT_BREAKER exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        CIRCUIT_BREAKER,

        /**
         * FALLBACK - Use fallback value/method.
         *
         * <p>
         * Use for: configuration, optional features.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the FALLBACK exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        FALLBACK
    }

    // --- EXCEPTION HANDLING ACTIONS ---

    /** Exception handling actions.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 10 symbolic ExceptionAction choices for law metadata; selecting a token performs no enforcement.
     * @m3.dataStructures Nested enum preserving declaration order, names and identities.
     */
    public enum ExceptionAction {
        /** Log and rethrow original exception.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the LOG_RETHROW exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        LOG_RETHROW,

        /** Log and suppress (return null/default).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the LOG_SUPPRESS exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        LOG_SUPPRESS,

        /** Log and return fallback value.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the LOG_FALLBACK exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        LOG_FALLBACK,

        /** Translate to different exception type.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the TRANSLATE exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        TRANSLATE,

        /** Retry the operation.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the RETRY exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        RETRY,

        /** Use circuit breaker.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CIRCUIT_BREAKER exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        CIRCUIT_BREAKER,

        /** Wrap in RuntimeException.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the WRAP_RUNTIME exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        WRAP_RUNTIME,

        /** Call recovery method.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the RECOVER exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        RECOVER,

        /** Silently ignore (DANGEROUS - requires justification).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IGNORE exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        IGNORE,

        /** Propagate unchanged.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the PROPAGATE exception metadata choice; the enum constant itself performs no exception handling.
         * @m3.dataStructures Existing enum constant preserving its identity and declaration order.
         */
        PROPAGATE
    }

    // --- THE IOP EXCEPTION SENTENCE ---

    /** The IOP exception sentence (locked).
     * @m3.pattern synexia:iop
     * @m3.algorithm Retains the exact CANONICAL_SENTENCE exception-law documentation payload for consumers.
     * @m3.dataStructures Existing static final String text block preserving literal content and Java constant identity.
     */
    public static final String CANONICAL_SENTENCE = """
			Exceptions are contracts.
			Recovery is explicit.
			Silence is forbidden.
			""";

    // --- LLM CONSTRAINTS ---

    /** What LLM is ALLOWED to do with exceptions.
     * @m3.pattern synexia:iop
     * @m3.algorithm Retains the exact LLM_ALLOWED exception-law documentation payload for consumers.
     * @m3.dataStructures Existing static final String text block preserving literal content and Java constant identity.
     */
    public static final String LLM_ALLOWED = """
			LLM MAY:
			- Generate try/catch blocks as specified by @IExceptionSpec
			- Log exceptions at specified level
			- Translate exceptions as specified
			- Call fallback methods as specified
			- Implement retry logic as specified
			""";

    /** What LLM is FORBIDDEN to do with exceptions.
     * @m3.pattern synexia:iop
     * @m3.algorithm Retains the exact LLM_FORBIDDEN exception-law documentation payload for consumers.
     * @m3.dataStructures Existing static final String text block preserving literal content and Java constant identity.
     */
    public static final String LLM_FORBIDDEN = """
			LLM MUST NOT:
			- Invent retry logic without @IExceptionSpec
			- Invent circuit breakers without @IExceptionSpec
			- Silently swallow exceptions
			- Add catch blocks not declared in spec
			- Change exception types without @Translation
			- Lose root cause information
			""";
}
