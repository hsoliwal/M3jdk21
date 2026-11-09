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
 * AISideEffectFree - Marks method/class as having no side effects.
 *
 * <h2>IOP v1 Purity Contract</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  @AISideEffectFree - PURE FUNCTION CONTRACT                               ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  "The generated method MUST NOT modify any state outside its scope."      ║
 * ║                                                                           ║
 * ║  This annotation declares that:                                           ║
 * ║    1. No modification of input parameters                                 ║
 * ║    2. No modification of instance fields (if method)                      ║
 * ║    3. No I/O operations (file, network, console)                          ║
 * ║    4. No calls to methods with side effects                               ║
 * ║    5. Result depends ONLY on inputs                                       ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * // Pure function - no side effects
 * &#64;AISideEffectFree
 * &#64;AIGenMethod("Calculate square root")
 * double sqrt(double value);
 *
 * // Pure service - all methods are side-effect-free
 * &#64;AISideEffectFree
 * &#64;AIGenClass("Mathematical calculations")
 * interface IMathService {
 *
 * 	double sin(double angle);
 *
 * 	double cos(double angle);
 *
 * 	double tan(double angle);
 * }
 *
 * // Allow specific exceptions
 * @AISideEffectFree(allowLogging = true)
 * &#64;AIGenMethod("Parse input with logging")
 * ParseResult parse(String input);
 * }
 * </pre>
 *
 * <h2>What Qualifies as Side Effect</h2>
 *
 * <pre>
 * SIDE EFFECTS (forbidden by default):
 *   ✗ Modifying input parameters
 *   ✗ Modifying instance/static fields
 *   ✗ Writing to files
 *   ✗ Sending network requests
 *   ✗ Writing to console
 *   ✗ Throwing exceptions (debatable)
 *   ✗ Calling non-pure methods
 *
 * NOT SIDE EFFECTS (allowed):
 *   ✓ Reading input parameters
 *   ✓ Creating new objects
 *   ✓ Local variable mutations
 *   ✓ Pure method calls
 *   ✓ Returning computed values
 * </pre>
 *
 * <h2>Benefits of Pure Functions</h2>
 * <ul>
 * <li>Easier to test (no mocking needed)</li>
 * <li>Safer to parallelize</li>
 * <li>Memoization-friendly</li>
 * <li>Referentially transparent</li>
 * <li>Easier to reason about</li>
 * </ul>
 *
 * @since ContextOS 1.0 - IOP v1
 * @see AIDeterministic
 * @see AIMayTouchExternalState
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/AISideEffectFree.java}
 * blob {@code 5d12149f0a268182495be5eebb2bbcb33c0d7aa5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AISideEffectFree {

    /**
     * Whether to allow logging statements.
     *
     * <p>
     * Logging is technically a side effect (writes to console/file), but is often
     * acceptable in "mostly pure" functions.
     * </p>
     *
     * @return true to allow logging (default: false)
     */
    boolean allowLogging() default false;

    /**
     * Whether to allow throwing exceptions.
     *
     * <p>
     * Exceptions can be considered side effects because they affect control flow
     * outside the function.
     * </p>
     *
     * @return true to allow exceptions (default: true)
     */
    boolean allowExceptions() default true;

    /**
     * Whether to allow reading system properties/environment.
     *
     * <p>
     * Reading environment is not strictly pure (external state), but is often
     * acceptable for configuration.
     * </p>
     *
     * @return true to allow env reads (default: false)
     */
    boolean allowEnvReads() default false;

    /**
     * Whether to allow creating new threads.
     *
     * <p>
     * Thread creation is a side effect (affects system resources).
     * </p>
     *
     * @return true to allow thread creation (default: false)
     */
    boolean allowThreadCreation() default false;

    /**
     * Whether to verify purity during generation.
     *
     * <p>
     * If true, generated code is analyzed for side effects.
     * </p>
     *
     * @return true to verify (default: true)
     */
    boolean verify() default true;

    /**
     * Methods that are known to be pure (whitelist).
     *
     * <p>
     * Fully qualified method names that can be called despite appearing to have
     * side effects.
     * </p>
     *
     * <pre>{@code
     * &#64;AISideEffectFree(knownPure = {
     *     "java.lang.Math.sqrt",
     *     "java.lang.String.substring"
     * })
     * }</pre>
     *
     * @return array of known pure method names
     */
    String[] knownPure() default {};

    /**
     * Methods that are known to have side effects (blacklist).
     *
     * <p>
     * Calling any of these methods will fail purity verification.
     * </p>
     *
     * @return array of known impure method names
     */
    String[] knownImpure() default {};
}
