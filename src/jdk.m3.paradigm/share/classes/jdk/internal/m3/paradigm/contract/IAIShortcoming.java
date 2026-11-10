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
 * {@code @AIShortcoming} captures these for future improvement passes.
 * </pre>
 *
 * <h2>Usage (Auto-Applied)</h2>
 *
 * <pre>{@code
 * &#64;IAIGenerated(model = "SONNET", ...)
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
 * public int processItems(List<Item> items) {
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
 * @see IAIGenerated
 * @see IAIIteration
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares AI-reported limitations, their severity, rationale and suggested future corrections; this annotation records guidance rather than implementing or verifying the requested behavior.
 * @m3.dataStructures CLASS-retained annotation targeting {ElementType.TYPE, ElementType.METHOD}; preserves its declared element and nested-token schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIShortcoming.java}
 * blob {@code 8b5d272253732923b881a807c4ed461523b73b99} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
@Documented
@Repeatable(IAIShortcomings.class)
public @interface IAIShortcoming {

    /**
     * What is the shortcoming. Clear description of the limitation.
     *
     * @return shortcoming description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIShortcoming#what advisory metadata for What is the shortcoming. Clear description of the limitation. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; explicit value required, no default.
     */
    String what();

    /**
     * Why this shortcoming exists. Explains the trade-off or constraint.
     *
     * @return reason
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIShortcoming#why advisory metadata for Why this shortcoming exists. Explains the trade-off or constraint. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String why() default "";

    /**
     * Severity of the shortcoming.
     *
     * @return severity level
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIShortcoming#severity advisory metadata for Severity of the shortcoming. The element itself neither executes nor enforces that request.
     * @m3.dataStructures Severity annotation element; exact default expression Severity.MEDIUM.
     */
    Severity severity() default Severity.MEDIUM;

    /** Severity levels.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 4 symbolic choices for Severity levels within IAIShortcoming advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAIShortcoming.Severity enum with 4 source-declared constants in existing order.
     */
    enum Severity {
        /** Minor issue - cosmetic, non-urgent.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIShortcoming.Severity#LOW token within Severity levels: Minor issue - cosmetic, non-urgent. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIShortcoming.Severity enum constant, retaining declared order and identity; it stores no implementation object.
         */
        LOW,
        /** Moderate issue - should address soon.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIShortcoming.Severity#MEDIUM token within Severity levels: Moderate issue - should address soon. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIShortcoming.Severity enum constant, retaining declared order and identity; it stores no implementation object.
         */
        MEDIUM,
        /** Significant issue - address in next iteration.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIShortcoming.Severity#HIGH token within Severity levels: Significant issue - address in next iteration. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIShortcoming.Severity enum constant, retaining declared order and identity; it stores no implementation object.
         */
        HIGH,
        /** Critical issue - requires immediate attention.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIShortcoming.Severity#CRITICAL token within Severity levels: Critical issue - requires immediate attention. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIShortcoming.Severity enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CRITICAL
    }

    /**
     * Suggested fix for the shortcoming.
     *
     * @return fix suggestion
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIShortcoming#suggestedFix advisory metadata for Suggested fix for the shortcoming. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String suggestedFix() default "";

    /**
     * Category of shortcoming.
     *
     * @return category
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIShortcoming#category advisory metadata for Category of shortcoming. The element itself neither executes nor enforces that request.
     * @m3.dataStructures Category annotation element; exact default expression Category.OTHER.
     */
    Category category() default Category.OTHER;

    /** Shortcoming categories.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 8 symbolic choices for Shortcoming categories within IAIShortcoming advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAIShortcoming.Category enum with 8 source-declared constants in existing order.
     */
    enum Category {
        /** Performance issue.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIShortcoming.Category#PERFORMANCE token within Shortcoming categories: Performance issue. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIShortcoming.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        PERFORMANCE,
        /** Security concern.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIShortcoming.Category#SECURITY token within Shortcoming categories: Security concern. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIShortcoming.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        SECURITY,
        /** Missing test coverage.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIShortcoming.Category#TESTING token within Shortcoming categories: Missing test coverage. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIShortcoming.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        TESTING,
        /** Error handling gaps.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIShortcoming.Category#ERROR_HANDLING token within Shortcoming categories: Error handling gaps. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIShortcoming.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ERROR_HANDLING,
        /** Documentation missing.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIShortcoming.Category#DOCUMENTATION token within Shortcoming categories: Documentation missing. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIShortcoming.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        DOCUMENTATION,
        /** Code style/readability.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIShortcoming.Category#STYLE token within Shortcoming categories: Code style/readability. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIShortcoming.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        STYLE,
        /** Architecture concern.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIShortcoming.Category#ARCHITECTURE token within Shortcoming categories: Architecture concern. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIShortcoming.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        ARCHITECTURE,
        /** Other/uncategorized.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIShortcoming.Category#OTHER token within Shortcoming categories: Other/uncategorized. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIShortcoming.Category enum constant, retaining declared order and identity; it stores no implementation object.
         */
        OTHER
    }

    /**
     * Estimated effort to fix (in hours).
     *
     * @return effort estimate
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIShortcoming#effortHours advisory metadata for Estimated effort to fix (in hours). The element itself neither executes nor enforces that request.
     * @m3.dataStructures double annotation element; exact default expression 0.
     */
    double effortHours() default 0;

    /**
     * Whether this has been addressed in a later iteration.
     *
     * @return true if addressed
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIShortcoming#addressed advisory metadata for Whether this has been addressed in a later iteration. The element itself neither executes nor enforces that request.
     * @m3.dataStructures boolean annotation element; exact default expression false.
     */
    boolean addressed() default false;

    /**
     * Iteration that addressed this shortcoming.
     *
     * @return iteration number (0 = not addressed)
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIShortcoming#addressedInIteration advisory metadata for Iteration that addressed this shortcoming. The element itself neither executes nor enforces that request.
     * @m3.dataStructures int annotation element; exact default expression 0.
     */
    int addressedInIteration() default 0;
}
