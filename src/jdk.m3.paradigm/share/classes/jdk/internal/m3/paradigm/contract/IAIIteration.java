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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIIteration - Tracks version history of AI-generated code.
 *
 * <h2>IOP: Code Evolution Tracking</h2>
 *
 * <pre>
 * Each time a spec changes, AI regenerates code.
 * &#64;AIIteration tracks the evolution:
 *
 * v1: Initial generation (spec hash: abc123)
 * v2: Constraint added (spec hash: def456, delta: +47 tokens)
 * v3: Bug fix in invariant (spec hash: ghi789, delta: +12 tokens)
 *
 * This creates a git-like history WITHIN the code itself.
 * </pre>
 *
 * <h2>Usage (Auto-Applied)</h2>
 *
 * <pre>{@code
 * &#64;IAIGenerated(model = "SONNET", specHash = "sha256:ghi789...", genCount = 3)
 * &#64;AIIteration(iteration = 3, prevHash = "sha256:def456...", deltaTokens = 12, changeReason = "Added null check for edge case")
 * public class UserServiceImpl implements IUserService {
 * 	// Third iteration of implementation
 * }
 * }
 * </pre>
 *
 * <h2>Why Track Iterations?</h2>
 * <ul>
 * <li>Understand code evolution over time</li>
 * <li>Rollback to previous versions if needed</li>
 * <li>Cost analysis (tokens per change)</li>
 * <li>Identify frequently-changing specs (instability indicator)</li>
 * <li>Optimize regeneration strategy</li>
 * </ul>
 *
 * @since IOP 1.0 - Version Tracking
 * @see IAIGenerated
 * @see IAIShortcoming
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares required iteration number plus previous hash, change, token, source and timing metadata. These supplied values do not authenticate provenance or prove generation history.
 * @m3.dataStructures CLASS-retained annotation with exact target {@code {ElementType.TYPE, ElementType.METHOD}}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIIteration.java}
 * blob {@code 167f1683ff18f0d5b40e0bf699387ee31d24fb1f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface IAIIteration {

    /**
     * Iteration/version number. Starts at 1, increments with each regeneration.
     *
     * @return iteration number
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares iteration metadata for Iteration/version number. Starts at 1, increments with each regeneration. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}, required explicitly with no default value.
     */
    int iteration();

    /**
     * Previous spec hash (before this iteration). Empty string for first iteration.
     *
     * @return previous spec hash
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares prevHash metadata for Previous spec hash (before this iteration). Empty string for first iteration. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String prevHash() default "";

    /**
     * Delta tokens from previous iteration. Positive = more tokens, negative =
     * fewer tokens.
     *
     * @return token difference
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares deltaTokens metadata for Delta tokens from previous iteration. Positive = more tokens, negative = fewer tokens. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}, with exact default {@code 0}.
     */
    int deltaTokens() default 0;

    /**
     * Reason for regeneration. What changed in the spec.
     *
     * @return change reason
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares changeReason metadata for Reason for regeneration. What changed in the spec. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String changeReason() default "";

    /**
     * Type of change that triggered regeneration.
     *
     * @return change type
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares changeType metadata for Type of change that triggered regeneration. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code ChangeType}, with exact default {@code ChangeType.SPEC_CHANGE}.
     */
    ChangeType changeType() default ChangeType.SPEC_CHANGE;

    /** Types of changes that trigger regeneration.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 9 symbolic generation change classifications for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIIteration.ChangeType enum with constants in existing declaration order.
     */
    enum ChangeType {
        /** Initial generation (no previous version).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the INITIAL metadata choice: Initial generation (no previous version). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        INITIAL,
        /** Spec description changed.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SPEC_CHANGE metadata choice: Spec description changed. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        SPEC_CHANGE,
        /** Constraint added/modified.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CONSTRAINT_CHANGE metadata choice: Constraint added/modified. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        CONSTRAINT_CHANGE,
        /** Invariant added/modified.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the INVARIANT_CHANGE metadata choice: Invariant added/modified. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        INVARIANT_CHANGE,
        /** Method signature changed.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SIGNATURE_CHANGE metadata choice: Method signature changed. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        SIGNATURE_CHANGE,
        /** Test failure triggered regeneration.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the TEST_FAILURE metadata choice: Test failure triggered regeneration. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        TEST_FAILURE,
        /** Manual regeneration requested.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the MANUAL metadata choice: Manual regeneration requested. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        MANUAL,
        /** Model upgrade (better model now available).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the MODEL_UPGRADE metadata choice: Model upgrade (better model now available). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        MODEL_UPGRADE,
        /** Shortcoming addressed.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SHORTCOMING_FIX metadata choice: Shortcoming addressed. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        SHORTCOMING_FIX
    }

    /**
     * Lines changed from previous iteration.
     *
     * @return lines changed (additions + deletions)
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares linesChanged metadata for Lines changed from previous iteration. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}, with exact default {@code 0}.
     */
    int linesChanged() default 0;

    /**
     * Files affected by this change.
     *
     * @return file paths
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares filesAffected metadata for Files affected by this change. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}, with exact default {@code {}}.
     */
    String[] filesAffected() default {};

    /**
     * Whether this was an incremental or full regeneration.
     *
     * @return true for incremental
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares incremental metadata for Whether this was an incremental or full regeneration. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}, with exact default {@code false}.
     */
    boolean incremental() default false;

    /**
     * Duration to generate this iteration in milliseconds.
     *
     * @return generation time
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares durationMs metadata for Duration to generate this iteration in milliseconds. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}, with exact default {@code 0}.
     */
    int durationMs() default 0;
}
