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
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.internal.m3.paradigm.contract;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIShortcoming - Records limitations identified by AI during generation.
 *
 * <h2>Self-Improvement PatternKind</h2>
 *
 * <pre>
 * When AI generates code, it can identify its own limitations:
 *   - "This algorithm is O(n²), could be O(n log n)"
 *   - "Edge case not fully tested"
 *   - "Consider adding caching for repeated calls"
 *
 * &#64;AIShortcoming captures these for future improvement passes.
 * </pre>
 *
 * <h2>Usage (Auto-Applied)</h2>
 *
 * <pre>{@code
 * &#64;AIGenerated(model = "SONNET", ...)
 * &#64;AIShortcoming(
 *     what = "O(n²) complexity in nested loop",
 *     why = "Simple implementation prioritized over optimization",
 *     severity = Severity.MEDIUM,
 *     suggestedFix = "Use HashMap for O(1) lookups"
 * )
 * &#64;AIShortcoming(
 *     what = "No input validation for negative numbers",
 *     severity = Severity.HIGH,
 *     suggestedFix = "Add precondition check"
 * )
 * public int processItems(List&lt;Item> items) {
 *     // Implementation with known shortcomings
 * }
 * }</pre>
 *
 * <h2>Improvement Workflow</h2>
 *
 * <pre>
 * 1. AI generates code and identifies shortcomings
 * 2. Shortcomings stored in @AIShortcoming annotations
 * 3. Future improvement pass reads shortcomings
 * 4. AI addresses shortcomings in next iteration
 * 5. Cycle continues until no shortcomings remain
 * </pre>
 *
 * @since IOP 1.0 - Self-Improvement Tracking
 * @see AIIteration
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIShortcoming.java}
 * blob {@code d4f189b32a27a8e407a1aea262df7357a2ab28fc} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
@Documented
@Repeatable(AIShortcomings.class)
public @interface AIShortcoming {

    /**
     * What is the shortcoming. Clear description of the limitation.
     *
     * @return shortcoming description
     */
    String what();

    /**
     * Why this shortcoming exists. Explains the trade-off or constraint.
     *
     * @return reason
     */
    String why() default "";

    /**
     * Severity of the shortcoming.
     *
     * @return severity level
     */
    Severity severity() default Severity.MEDIUM;

    /** Severity levels. */
    enum Severity {
        /** Minor issue - cosmetic, non-urgent. */
        LOW,
        /** Moderate issue - should address soon. */
        MEDIUM,
        /** Significant issue - address in next iteration. */
        HIGH,
        /** Critical issue - requires immediate attention. */
        CRITICAL
    }

    /**
     * Suggested fix for the shortcoming.
     *
     * @return fix suggestion
     */
    String suggestedFix() default "";

    /**
     * Category of shortcoming.
     *
     * @return category
     */
    Category category() default Category.OTHER;

    /** Shortcoming categories. */
    enum Category {
        /** Performance issue. */
        PERFORMANCE,
        /** Security concern. */
        SECURITY,
        /** Missing test coverage. */
        TESTING,
        /** Error handling gaps. */
        ERROR_HANDLING,
        /** Documentation missing. */
        DOCUMENTATION,
        /** Code style/readability. */
        STYLE,
        /** Architecture concern. */
        ARCHITECTURE,
        /** Other/uncategorized. */
        OTHER
    }

    /**
     * Estimated effort to fix (in hours).
     *
     * @return effort estimate
     */
    double effortHours() default 0;

    /**
     * Whether this has been addressed in a later iteration.
     *
     * @return true if addressed
     */
    boolean addressed() default false;

    /**
     * Iteration that addressed this shortcoming.
     *
     * @return iteration number (0 = not addressed)
     */
    int addressedInIteration() default 0;
}
