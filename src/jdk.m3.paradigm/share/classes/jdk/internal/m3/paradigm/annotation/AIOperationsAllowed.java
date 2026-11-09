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
 * AIOperationsAllowed - Declares the closed set of allowed AI operations.
 *
 * <h2>IOP v1 Operation Authority</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  @AIOperationsAllowed - CLOSED OPERATION SET                              ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  "No change may occur unless it is the result of a declared, legal        ║
 * ║   operation under explicit intent."                                       ║
 * ║                                                                           ║
 * ║  This annotation declares which operations AI may perform on this element.║
 * ║  Any operation NOT in this set is ILLEGAL and MUST be rejected.           ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	// Allow only generation operations
 * 	&#64;AIOperationsAllowed({IOPOperation.GENERATE_IMPLEMENTATION, IOPOperation.GENERATE_TESTS,
 * 			IOPOperation.GENERATE_JAVADOC})
 * 	&#64;AIGenClass("User service")
 * 	interface IUserService {
 *
 * 		User findById(long id);
 * 	}
 *
 * 	// Allow generation + refinement
 * 	&#64;AIOperationsAllowed({IOPOperation.GENERATE_IMPLEMENTATION, IOPOperation.REFINE_METHOD,
 * 			IOPOperation.OPTIMIZE_PERFORMANCE})
 * 	&#64;AIGenClass("Performance-critical service")
 * 	interface IPerformanceService {
 *
 * 		void processLargeDataset(Dataset data);
 * 	}
 *
 * 	// Full structural operations (refactoring allowed)
 * 	&#64;AIOperationsAllowed({IOPOperation.GENERATE_IMPLEMENTATION, IOPOperation.REFINE_METHOD,
 * 			IOPOperation.RENAME_SYMBOL, IOPOperation.EXTRACT_METHOD, IOPOperation.REMOVE_DEAD_CODE})
 * 	&#64;AIGenClass("Evolving service")
 * 	interface IEvolvingService {
 *
 * 		void evolve();
 * 	}
 * }
 * </pre>
 *
 * <h2>Default Operations</h2>
 * <p>
 * If @AIOperationsAllowed is not present, the default allowed set is:
 * </p>
 * <ul>
 * <li>GENERATE_IMPLEMENTATION</li>
 * <li>GENERATE_TESTS</li>
 * <li>GENERATE_JAVADOC</li>
 * <li>ORGANIZE_IMPORTS</li>
 * <li>FORMAT_CODE</li>
 * </ul>
 *
 * <h2>Operation Categories</h2>
 *
 * <pre>
 * SAFE (default allowed):
 *   GENERATE_IMPLEMENTATION, GENERATE_TESTS, GENERATE_JAVADOC
 *   ORGANIZE_IMPORTS, FORMAT_CODE
 *
 * REFINEMENT (requires explicit):
 *   REFINE_METHOD, OPTIMIZE_PERFORMANCE
 *
 * STRUCTURAL (requires explicit):
 *   INTRODUCE_TYPE, MOVE_TYPE, RENAME_SYMBOL
 *   EXTRACT_METHOD, INLINE_METHOD
 *
 * DESTRUCTIVE (requires explicit):
 *   REMOVE_DEAD_CODE
 * </pre>
 *
 * @since ContextOS 1.0 - IOP v1
 * @see IOPOperation
 * @see AIRejects
 * @see AIGenClass
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/AIOperationsAllowed.java}
 * blob {@code 7f2da345a1afa4ba7108fca322ed03201d19cc95} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PACKAGE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIOperationsAllowed {

    /**
     * The closed set of operations allowed on this element.
     *
     * <p>
     * AI MUST reject any operation not in this list with
     * {@link IOPFailure#ILLEGAL_OPERATION}.
     * </p>
     *
     * <pre>
     * Operation presets:
     *
     * GENERATION_ONLY (safest):
     *   &#64;AIOperationsAllowed({GENERATE_IMPLEMENTATION})
     *
     * GENERATION_WITH_TESTS:
     *   &#64;AIOperationsAllowed({GENERATE_IMPLEMENTATION, GENERATE_TESTS})
     *
     * FULL_GENERATION:
     *   &#64;AIOperationsAllowed({GENERATE_IMPLEMENTATION, GENERATE_TESTS, GENERATE_JAVADOC})
     *
     * WITH_REFINEMENT:
     *   &#64;AIOperationsAllowed({GENERATE_IMPLEMENTATION, REFINE_METHOD, OPTIMIZE_PERFORMANCE})
     *
     * FULL_REFACTORING:
     *   &#64;AIOperationsAllowed({GENERATE_IMPLEMENTATION, REFINE_METHOD, RENAME_SYMBOL,
     *                         EXTRACT_METHOD, INLINE_METHOD, REMOVE_DEAD_CODE})
     * </pre>
     *
     * @return array of allowed operations
     */
    IOPOperation[] value();

    /**
     * Whether to inherit parent's operation list.
     *
     * <p>
     * If true, method-level @AIOperationsAllowed adds to class-level list. If
     * false, method-level @AIOperationsAllowed replaces class-level list.
     * </p>
     *
     * @return true to inherit (default: false for security)
     */
    boolean inherit() default false;

    /**
     * Whether to allow NOOP (read-only analysis) implicitly.
     *
     * <p>
     * NOOP is always safe and is implicitly allowed unless explicitly denied.
     * </p>
     *
     * @return true to allow NOOP (default: true)
     */
    boolean allowNoop() default true;

    /**
     * Whether to require human approval for structural operations.
     *
     * <p>
     * If true, structural operations (MOVE_TYPE, RENAME_SYMBOL, etc.) require
     * interactive confirmation before execution.
     * </p>
     *
     * @return true to require approval (default: true)
     */
    boolean requireApprovalForStructural() default true;

    /**
     * Whether to log all operations for audit trail.
     *
     * @return true to enable audit logging (default: true)
     */
    boolean auditLog() default true;

    /** Preset operation sets for common use cases. */
    enum Preset {
        /** Only implementation generation. Operations: GENERATE_IMPLEMENTATION */
        GENERATION_ONLY,

        /**
         * Generation with test creation. Operations: GENERATE_IMPLEMENTATION,
         * GENERATE_TESTS
         */
        GENERATION_WITH_TESTS,

        /**
         * Full generation suite. Operations: GENERATE_IMPLEMENTATION, GENERATE_TESTS,
         * GENERATE_JAVADOC
         */
        FULL_GENERATION,

        /**
         * Generation plus refinement. Operations: GENERATE_IMPLEMENTATION,
         * REFINE_METHOD, OPTIMIZE_PERFORMANCE
         */
        WITH_REFINEMENT,

        /**
         * All refactoring operations. Operations: GENERATE_IMPLEMENTATION,
         * REFINE_METHOD, all structural + cleanup
         */
        FULL_REFACTORING,

        /** Read-only analysis (no modifications). Operations: NOOP only */
        READ_ONLY
    }

    /**
     * Use a preset instead of listing individual operations.
     *
     * <p>
     * If preset is specified, value() is ignored.
     * </p>
     *
     * <pre>{@code
     * &#64;AIOperationsAllowed(preset = Preset.FULL_GENERATION)
     * interface IMyService {
     * }
     * }</pre>
     *
     * @return preset to use (GENERATION_ONLY = use value() instead)
     */
    Preset preset() default Preset.GENERATION_ONLY;
}
