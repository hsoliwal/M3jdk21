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

import java.lang.annotation.*;

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
 * &#64;AIGenerated(model = "SONNET", specHash = "sha256:ghi789...", genCount = 3)
 * &#64;AIIteration(iteration = 3, prevHash = "sha256:def456...", deltaTokens = 12, changeReason = "Added null check for edge case")
 * public class UserServiceImpl implements IUserService {
 *     // Third iteration of implementation
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
 * @see AIShortcoming
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIIteration.java}
 * blob {@code e7492194d84451aa941268c88d165e891046a062} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface AIIteration {

    /**
     * Iteration/version number. Starts at 1, increments with each regeneration.
     *
     * @return iteration number
     */
    int iteration();

    /**
     * Previous spec hash (before this iteration). Empty string for first
     * iteration.
     *
     * @return previous spec hash
     */
    String prevHash() default "";

    /**
     * Delta tokens from previous iteration. Positive = more tokens, negative =
     * fewer tokens.
     *
     * @return token difference
     */
    int deltaTokens() default 0;

    /**
     * Reason for regeneration. What changed in the spec.
     *
     * @return change reason
     */
    String changeReason() default "";

    /**
     * Type of change that triggered regeneration.
     *
     * @return change type
     */
    ChangeType changeType() default ChangeType.SPEC_CHANGE;

    /** Types of changes that trigger regeneration. */
    /**
     * ChangeType.
     * @author Synexia
     */
    enum ChangeType {
        /** Initial generation (no previous version). */
        INITIAL,
        /** Spec description changed. */
        SPEC_CHANGE,
        /** Constraint added/modified. */
        CONSTRAINT_CHANGE,
        /** Invariant added/modified. */
        INVARIANT_CHANGE,
        /** Method signature changed. */
        SIGNATURE_CHANGE,
        /** Test failure triggered regeneration. */
        TEST_FAILURE,
        /** Manual regeneration requested. */
        MANUAL,
        /** Model upgrade (better model now available). */
        MODEL_UPGRADE,
        /** Shortcoming addressed. */
        SHORTCOMING_FIX
    }

    /**
     * Lines changed from previous iteration.
     *
     * @return lines changed (additions + deletions)
     */
    int linesChanged() default 0;

    /**
     * Files affected by this change.
     *
     * @return file paths
     */
    String[] filesAffected() default {};

    /**
     * Whether this was an incremental or full regeneration.
     *
     * @return true for incremental
     */
    boolean incremental() default false;

    /**
     * Duration to generate this iteration in milliseconds.
     *
     * @return generation time
     */
    int durationMs() default 0;
}
