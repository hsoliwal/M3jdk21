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
 * Stability - Tracks implementation stability and version for IOP.
 *
 * <h2>IOP Principle: Implementation Stability Tracking</h2>
 *
 * <pre>
 * +-------------------------------------------------------------------------+
 * |  STABILITY TRACKING                                                     |
 * +-------------------------------------------------------------------------+
 * |                                                                         |
 * |  "Implementations have lifecycle states that affect regeneration."      |
 * |                                                                         |
 * |  Stability Levels:                                                      |
 * |    EXPERIMENTAL - Unstable, may change frequently                       |
 * |    ALPHA        - Early development, expect breaking changes            |
 * |    BETA         - Feature complete, may have bugs                       |
 * |    STABLE       - Production ready, backward compatible                 |
 * |    FROZEN       - Locked, never regenerate                              |
 * |    DEPRECATED   - Scheduled for removal                                 |
 * |                                                                         |
 * |  Regeneration Rules:                                                    |
 * |    EXPERIMENTAL → Always regenerate on spec change                      |
 * |    ALPHA        → Regenerate, warn about breaking changes               |
 * |    BETA         → Regenerate with migration notes                       |
 * |    STABLE       → Regenerate only if backward compatible                |
 * |    FROZEN       → Never regenerate (manual only)                        |
 * |    DEPRECATED   → Skip regeneration, mark for removal                   |
 * |                                                                         |
 * +-------------------------------------------------------------------------+
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	// Mark implementation stability
 * 	&#64;Stability(Level.STABLE)
 * 	&#64;SpecHash("sha256:abc123...")
 * 	public class UserServiceImpl implements IUserService {
 * 		// Stable implementation - backward compatible changes only
 * 	}
 *
 * 	// Frozen implementation (never regenerate)
 * 	&#64;Stability(value = Level.FROZEN, reason = "Hand-tuned performance optimizations")
 * 	public class OptimizedParser implements IParser {
 * 		// Will never be auto-regenerated
 * 	}
 *
 * 	// With version tracking
 * 	&#64;Stability(value = Level.BETA, version = "2.0.0-beta.1", since = "2025-01-15")
 * 	public class NewFeatureImpl implements INewFeature {
 * 		// Beta implementation, tracked version
 * 	}
 * }
 * </pre>
 *
 * <h2>Two-Phase AOP Integration</h2>
 *
 * <pre>
 * PHASE 1: Annotation Processing
 *   - Read @Stability on existing implementations
 *   - Compare @SpecHash with current spec hash
 *   - Decide: regenerate or preserve
 *
 * PHASE 2: Code Generation
 *   - If regenerating: use @Model to select generator
 *   - Apply stability rules for backward compatibility
 *   - Update @SpecHash and @Stability on output
 * </pre>
 *
 * @since ContextOS 1.0 - IOP Stability Tracking
 * @see SpecHash
 * @see Model
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Stability.java}
 * blob {@code 07d908d677b68cc08efc552217c4581da1e90042} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Stability {

    /**
     * The stability level of this implementation.
     *
     * @return stability level
     */
    Level value();

    /**
     * Version string for this implementation. Follows semantic versioning.
     *
     * @return version string
     */
    String version() default "";

    /**
     * Date when this stability level was assigned. ISO-8601 format (YYYY-MM-DD).
     *
     * @return date string
     */
    String since() default "";

    /**
     * Reason for this stability level.
     *
     * @return reason string
     */
    String reason() default "";

    /**
     * Who is responsible for this implementation.
     *
     * @return owner/maintainer
     */
    String owner() default "";

    /** Stability levels for implementations. */
    enum Level {
        /**
         * Experimental - Unstable, expect frequent changes. Regeneration: Always on
         * spec change.
         */
        EXPERIMENTAL,

        /**
         * Alpha - Early development, breaking changes expected. Regeneration: Always,
         * with warnings.
         */
        ALPHA,

        /** Beta - Feature complete, may have bugs. Regeneration: With migration notes. */
        BETA,

        /**
         * Stable - Production ready, backward compatible. Regeneration: Only if
         * backward compatible.
         */
        STABLE,

        /**
         * Frozen - Locked implementation, never regenerate. Regeneration: Manual only,
         * auto-generation skipped.
         */
        FROZEN,

        /**
         * Deprecated - Scheduled for removal. Regeneration: Skipped, marked for
         * deletion.
         */
        DEPRECATED
    }
}
