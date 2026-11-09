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
 * AIDeterministic - Marks method/class as requiring deterministic AI
 * generation.
 *
 * <h2>IOP v1 Determinism Contract</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  @AIDeterministic - SAME INPUT → SAME OUTPUT                              ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  "Given the same specification, AI MUST produce the same implementation." ║
 * ║                                                                           ║
 * ║  This annotation declares that:                                           ║
 * ║    1. Same interface + JavaDoc → same generated code                      ║
 * ║    2. No randomness in generation                                         ║
 * ║    3. Output is reproducible and verifiable                               ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * // Method must produce deterministic output
 * &#64;AIDeterministic
 * &#64;AIGenMethod("Calculate hash of input")
 * String hash(String input);
 *
 * // Class-level: all methods must be deterministic
 * &#64;AIDeterministic
 * &#64;AIGenClass("Pure calculation service")
 * interface ICalculator {
 *
 * 	int add(int a, int b);
 *
 * 	int multiply(int a, int b);
 * }
 *
 * // With tolerance for minor variations
 * @AIDeterministic(tolerance = Tolerance.SEMANTICALLY_EQUIVALENT)
 * &#64;AIGenMethod("Format date")
 * String formatDate(LocalDate date);
 * }
 * </pre>
 *
 * <h2>Determinism Levels</h2>
 *
 * <pre>
 * EXACT:
 *   - Byte-for-byte identical output
 *   - Strictest requirement
 *   - Used for: crypto, checksums
 *
 * SEMANTICALLY_EQUIVALENT:
 *   - Same behavior, may differ in whitespace/formatting
 *   - Common for most code generation
 *   - Used for: business logic
 *
 * BEHAVIORALLY_EQUIVALENT:
 *   - Same observable behavior, implementation may vary
 *   - Most flexible
 *   - Used for: performance-optimized code
 * </pre>
 *
 * <h2>Verification</h2>
 * <p>
 * When @AIDeterministic is present, the IOP pipeline:
 * </p>
 * <ol>
 * <li>Generates code twice with same input</li>
 * <li>Compares outputs according to tolerance level</li>
 * <li>Fails with {@link IOPFailure#NON_DETERMINISTIC_RESULT} if different</li>
 * </ol>
 *
 * @since ContextOS 1.0 - IOP v1
 * @see AISideEffectFree
 * @see AIMayTouchExternalState
 * @see IOPFailure#NON_DETERMINISTIC_RESULT
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/AIDeterministic.java}
 * blob {@code 9c933a3984f5b9267603e684600b7f7d805dcd74} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIDeterministic {

    /**
     * Tolerance level for determinism verification.
     *
     * @return tolerance level (default: SEMANTICALLY_EQUIVALENT)
     */
    Tolerance tolerance() default Tolerance.SEMANTICALLY_EQUIVALENT;

    /**
     * Whether to verify determinism during generation.
     *
     * <p>
     * If true, generation runs twice and compares output. This doubles generation
     * cost but ensures determinism.
     * </p>
     *
     * @return true to verify (default: false for performance)
     */
    boolean verify() default false;

    /**
     * Seed for reproducible random number generation.
     *
     * <p>
     * If the generated code needs randomness (e.g., test data), this seed ensures
     * reproducibility.
     * </p>
     *
     * @return random seed (default: 0 = no randomness allowed)
     */
    long seed() default 0L;

    /**
     * Whether to fail on non-deterministic result.
     *
     * <p>
     * If false, non-determinism is logged as warning but generation continues.
     * </p>
     *
     * @return true to fail on non-determinism (default: true)
     */
    boolean strict() default true;

    /** Tolerance levels for determinism comparison. */
    /**
     * Tolerance.
     * @author Synexia
     */
    enum Tolerance {
        /**
         * Byte-for-byte identical output.
         *
         * <p>
         * The strictest level. Generated code must be exactly the same including
         * whitespace, comments, and formatting.
         * </p>
         */
        EXACT,

        /**
         * Semantically equivalent output.
         *
         * <p>
         * Generated code may differ in whitespace, comments, and formatting, but must
         * have identical AST structure.
         * </p>
         */
        SEMANTICALLY_EQUIVALENT,

        /**
         * Behaviorally equivalent output.
         *
         * <p>
         * Generated code may have different structure, but must produce the same
         * observable behavior for all inputs.
         * </p>
         */
        BEHAVIORALLY_EQUIVALENT
    }
}
