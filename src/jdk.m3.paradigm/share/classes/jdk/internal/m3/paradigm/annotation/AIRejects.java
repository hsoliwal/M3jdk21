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

package jdk.internal.m3.paradigm.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIRejects - Declares explicit failure reasons for AI operations.
 *
 * <h2>IOP v1 Failure Semantics</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  @AIRejects - EXPLICIT FAILURE DECLARATION                                ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  "AI must fail explicitly with a declared reason,                         ║
 * ║   never silently or with generic errors."                                 ║
 * ║                                                                           ║
 * ║  This annotation declares which failure codes apply to this element.      ║
 * ║  AI MUST reject with one of these codes, never with unlisted failures.    ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * // Declare expected failure modes for an interface
 * &#64;AIRejects({IOPFailure.MISSING_SPECIFICATION, IOPFailure.CONSTRAINT_VIOLATION, IOPFailure.AMBIGUOUS_TARGET,
 * 		IOPFailure.NON_DETERMINISTIC_RESULT})
 * &#64;AIGenClass("User authentication service")
 * interface IAuthService {
 *
 * 	AuthResult authenticate(String username, String password);
 * }
 *
 * // Declare method-specific failure modes
 * &#64;AIRejects({IOPFailure.CONSTRAINT_VIOLATION, IOPFailure.INVARIANT_VIOLATION})
 * &#64;AIGenMethod("Validate credentials against secure store")
 * AuthResult authenticate(String username, String password);
 * }
 * </pre>
 *
 * <h2>Failure Contract</h2>
 *
 * <pre>
 * When AI encounters a failure condition:
 *   1. Check if failure code is in @AIRejects list
 *   2. If YES → report failure with that code
 *   3. If NO → the operation should have succeeded (bug in AI)
 *
 * This creates a CONTRACT between spec and AI:
 *   - Spec declares: "these failures are possible"
 *   - AI promises: "I will only fail with these codes"
 * </pre>
 *
 * <h2>Benefits</h2>
 * <ul>
 * <li>Predictable error handling</li>
 * <li>Clear recovery paths</li>
 * <li>Testable failure modes</li>
 * <li>Self-documenting error contract</li>
 * </ul>
 *
 * @since ContextOS 1.0 - IOP v1
 * @see IOPFailure
 * @see AIOperationsAllowed
 * @see AIGenClass
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/AIRejects.java}
 * blob {@code f83f3c77345a0157383fc3dec4e24e0c69b9058b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIRejects {

    /**
     * The set of failure codes that apply to this element.
     *
     * <p>
     * AI MUST fail with one of these codes if it cannot complete the operation. Any
     * other failure indicates a bug in the AI.
     * </p>
     *
     * <pre>
     * Common patterns:
     *
     * Specification-focused:
     *   &#64;AIRejects({MISSING_SPECIFICATION, AMBIGUOUS_SPECIFICATION})
     *
     * Generation-focused:
     *   &#64;AIRejects({CONSTRAINT_VIOLATION, INVARIANT_VIOLATION, TEST_FAILURE})
     *
     * Full coverage:
     *   &#64;AIRejects({
     *       MISSING_SPECIFICATION,
     *       AMBIGUOUS_SPECIFICATION,
     *       CONSTRAINT_VIOLATION,
     *       TEST_FAILURE,
     *       COMPILE_FAILURE
     *   })
     * </pre>
     *
     * @return array of possible failure codes
     */
    IOPFailure[] value();

    /**
     * Whether to inherit parent's rejection list.
     *
     * <p>
     * If true, method-level @AIRejects adds to class-level list. If false,
     * method-level @AIRejects replaces class-level list.
     * </p>
     *
     * @return true to inherit (default: true)
     */
    boolean inherit() default true;

    /**
     * Handler class for custom failure processing.
     *
     * <p>
     * Optional handler that receives failure events for logging, metrics, or custom
     * recovery logic.
     * </p>
     *
     * <pre>{@code
     * &#64;AIRejects(
     *     value = {IOPFailure.CONSTRAINT_VIOLATION},
     *     handler = MyFailureHandler.class
     * )
     * }</pre>
     *
     * @return handler class (Void.class means no handler)
     */
    Class<?> handler() default Void.class;

    /**
     * Whether failures should be logged automatically.
     *
     * @return true to enable auto-logging (default: true)
     */
    boolean logFailures() default true;

    /**
     * Maximum retries before accepting failure.
     *
     * <p>
     * Only applies to retriable failures (see {@link IOPFailure#isRetriable()}).
     * </p>
     *
     * @return max retries (default: 3)
     */
    int maxRetries() default 3;
}
