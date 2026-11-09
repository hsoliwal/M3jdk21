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

/**
 * IOP v1 Failure Semantics - Enumerated failure codes.
 *
 * <h2>IOP Explicit Failure Model</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  IOP v1: FAILURE SEMANTICS (CLOSED SET)                                   ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  "AI must fail explicitly with a declared reason,                         ║
 * ║   never silently or with generic errors."                                 ║
 * ║                                                                           ║
 * ║  Every failure MUST map to one of these codes.                            ║
 * ║  Each code has defined recovery actions.                                  ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Failure Categories</h2>
 *
 * <pre>
 * SPECIFICATION FAILURES:
 *   MISSING_SPECIFICATION      - No spec provided (interface/JavaDoc)
 *   AMBIGUOUS_SPECIFICATION    - Spec has multiple interpretations
 *   INCOMPLETE_SPECIFICATION   - Spec missing required information
 *   CONFLICTING_CONSTRAINTS    - Constraints contradict each other
 *
 * OPERATION FAILURES:
 *   ILLEGAL_OPERATION          - Operation not in allowed set
 *   UNSUPPORTED_OPERATION      - Operation valid but not implemented
 *   PRECONDITION_FAILED        - Operation preconditions not met
 *
 * TARGET FAILURES:
 *   AMBIGUOUS_TARGET           - Cannot determine unique target
 *   TARGET_NOT_FOUND           - Specified target does not exist
 *   TARGET_LOCKED              - Target is frozen/immutable
 *
 * GENERATION FAILURES:
 *   CONSTRAINT_VIOLATION       - Generated code violates constraints
 *   INVARIANT_VIOLATION        - Generated code breaks invariants
 *   NON_DETERMINISTIC_RESULT   - Same input produces different outputs
 *   UNSATISFIABLE_INTENT       - Intent cannot be satisfied
 *
 * VERIFICATION FAILURES:
 *   TEST_FAILURE               - Generated code fails tests
 *   COMPILE_FAILURE            - Generated code doesn't compile
 *   VERIFICATION_TIMEOUT       - Verification took too long
 *
 * RESOURCE FAILURES:
 *   MODEL_UNAVAILABLE          - Requested LLM not available
 *   TOKEN_LIMIT_EXCEEDED       - Context too large for model
 *   RATE_LIMIT_EXCEEDED        - API rate limit hit
 * </pre>
 *
 * @since ContextOS 1.0 - IOP v1
 * @see AIRejects
 * @see IOPOperation
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/IOPFailure.java}
 * blob {@code f483d099e8cc849921d40a4cca558858b6f54ac7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum IOPFailure {

    // --- SPECIFICATION FAILURES ---

    /**
     * No specification provided.
     *
     * <p>
     * The interface lacks JavaDoc, @AIGenClass, or @AIGenMethod.
     * </p>
     *
     * <pre>
     * Recovery: Add specification to interface/method
     * Severity: BLOCKING
     * </pre>
     */
    MISSING_SPECIFICATION,

    /**
     * Specification has multiple valid interpretations.
     *
     * <p>
     * The AI cannot determine the intended behavior.
     * </p>
     *
     * <pre>
     * Recovery: Clarify specification with more constraints/examples
     * Severity: BLOCKING
     * </pre>
     */
    AMBIGUOUS_SPECIFICATION,

    /**
     * Specification is missing required information.
     *
     * <p>
     * Some aspects of behavior are unspecified.
     * </p>
     *
     * <pre>
     * Recovery: Add missing details to specification
     * Severity: BLOCKING
     * </pre>
     */
    INCOMPLETE_SPECIFICATION,

    /**
     * Constraints contradict each other.
     *
     * <p>
     * It is impossible to satisfy all constraints simultaneously.
     * </p>
     *
     * <pre>
     * Example: "must be O(1)" AND "must sort the list"
     * Recovery: Remove or relax conflicting constraints
     * Severity: BLOCKING
     * </pre>
     */
    CONFLICTING_CONSTRAINTS,

    // --- OPERATION FAILURES ---

    /**
     * Requested operation is not in the allowed set.
     *
     * <p>
     * The @AIOperationsAllowed annotation does not include this operation.
     * </p>
     *
     * <pre>
     * Recovery: Add operation to allowed set, or use different operation
     * Severity: BLOCKING
     * </pre>
     */
    ILLEGAL_OPERATION,

    /**
     * Operation is valid but not yet implemented.
     *
     * <pre>
     * Recovery: Use alternative operation, or wait for implementation
     * Severity: BLOCKING
     * </pre>
     */
    UNSUPPORTED_OPERATION,

    /**
     * Operation preconditions are not satisfied.
     *
     * <pre>
     * Example: REFINE_METHOD on non-existent method
     * Recovery: Ensure preconditions are met
     * Severity: BLOCKING
     * </pre>
     */
    PRECONDITION_FAILED,

    // --- TARGET FAILURES ---

    /**
     * Cannot determine unique target for operation.
     *
     * <p>
     * Multiple elements match the target specification.
     * </p>
     *
     * <pre>
     * Example: "rename method foo" when multiple methods named foo exist
     * Recovery: Provide more specific target identifier
     * Severity: BLOCKING
     * </pre>
     */
    AMBIGUOUS_TARGET,

    /**
     * Specified target does not exist.
     *
     * <pre>
     * Example: REFINE_METHOD on method that was deleted
     * Recovery: Verify target exists, update reference
     * Severity: BLOCKING
     * </pre>
     */
    TARGET_NOT_FOUND,

    /**
     * Target is frozen and cannot be modified.
     *
     * <p>
     * The target has @Stability.Level.FROZEN.
     * </p>
     *
     * <pre>
     * Recovery: Unfreeze target, or operate on different element
     * Severity: BLOCKING
     * </pre>
     */
    TARGET_LOCKED,

    // --- GENERATION FAILURES ---

    /**
     * Generated code violates specified constraints.
     *
     * <pre>
     * Example: Generated code uses reflection when @constraint="no reflection"
     * Recovery: Retry with different approach, or relax constraint
     * Severity: RETRIABLE
     * </pre>
     */
    CONSTRAINT_VIOLATION,

    /**
     * Generated code breaks specified invariants.
     *
     * <pre>
     * Example: @invariant="result >= 0" but code can return -1
     * Recovery: Retry generation, add assertions
     * Severity: RETRIABLE
     * </pre>
     */
    INVARIANT_VIOLATION,

    /**
     * Same input produces different outputs.
     *
     * <p>
     * The generation is not deterministic, which violates @AIDeterministic.
     * </p>
     *
     * <pre>
     * Recovery: Fix non-determinism source, or remove @AIDeterministic
     * Severity: WARNING (may be acceptable)
     * </pre>
     */
    NON_DETERMINISTIC_RESULT,

    /**
     * Intent cannot be satisfied with current constraints.
     *
     * <p>
     * It is mathematically/logically impossible to satisfy the intent.
     * </p>
     *
     * <pre>
     * Example: "Generate O(1) sorting algorithm"
     * Recovery: Relax constraints, change intent
     * Severity: BLOCKING
     * </pre>
     */
    UNSATISFIABLE_INTENT,

    // --- VERIFICATION FAILURES ---

    /**
     * Generated code fails unit tests.
     *
     * <pre>
     * Recovery: Retry generation with test failure context
     * Severity: RETRIABLE (up to maxRetries)
     * </pre>
     */
    TEST_FAILURE,

    /**
     * Generated code does not compile.
     *
     * <pre>
     * Recovery: Retry generation with compiler error context
     * Severity: RETRIABLE
     * </pre>
     */
    COMPILE_FAILURE,

    /**
     * Verification process exceeded time limit.
     *
     * <pre>
     * Recovery: Increase timeout, simplify verification
     * Severity: WARNING
     * </pre>
     */
    VERIFICATION_TIMEOUT,

    // --- RESOURCE FAILURES ---

    /**
     * Requested LLM model is not available.
     *
     * <pre>
     * Recovery: Use fallback model
     * Severity: RETRIABLE
     * </pre>
     */
    MODEL_UNAVAILABLE,

    /**
     * Context size exceeds model's token limit.
     *
     * <pre>
     * Recovery: Reduce context, split into smaller operations
     * Severity: BLOCKING
     * </pre>
     */
    TOKEN_LIMIT_EXCEEDED,

    /**
     * API rate limit has been exceeded.
     *
     * <pre>
     * Recovery: Wait and retry, use local model
     * Severity: RETRIABLE
     * </pre>
     */
    RATE_LIMIT_EXCEEDED;

    // --- UTILITY METHODS ---

    /**
     * Check if this failure is retriable.
     *
     * @return true if operation can be retried
     */
    public boolean isRetriable() {
        return this == CONSTRAINT_VIOLATION
                || this == INVARIANT_VIOLATION
                || this == TEST_FAILURE
                || this == COMPILE_FAILURE
                || this == MODEL_UNAVAILABLE
                || this == RATE_LIMIT_EXCEEDED;
    }

    /**
     * Check if this failure is blocking (prevents all progress).
     *
     * @return true if failure is blocking
     */
    public boolean isBlocking() {
        return isSpecificationBlocker() || isOperationBlocker() || isTargetBlocker() || isResourceBlocker();
    }

    private boolean isSpecificationBlocker() {
        return matchesAny(
                MISSING_SPECIFICATION, AMBIGUOUS_SPECIFICATION, INCOMPLETE_SPECIFICATION, CONFLICTING_CONSTRAINTS);
    }

    private boolean isOperationBlocker() {
        return matchesAny(ILLEGAL_OPERATION, UNSUPPORTED_OPERATION, PRECONDITION_FAILED);
    }

    private boolean isTargetBlocker() {
        return matchesAny(AMBIGUOUS_TARGET, TARGET_NOT_FOUND, TARGET_LOCKED);
    }

    private boolean isResourceBlocker() {
        return matchesAny(UNSATISFIABLE_INTENT, TOKEN_LIMIT_EXCEEDED);
    }

    private boolean matchesAny(IOPFailure... failures) {
        return java.util.Arrays.stream(failures).anyMatch(failure -> this == failure);
    }

    /**
     * Check if this is a specification-related failure.
     *
     * @return true if failure relates to specification
     */
    public boolean isSpecificationFailure() {
        return matchesAny(
                MISSING_SPECIFICATION, AMBIGUOUS_SPECIFICATION, INCOMPLETE_SPECIFICATION, CONFLICTING_CONSTRAINTS);
    }

    /**
     * Check if this is an operation-related failure.
     *
     * @return true if failure relates to operation
     */
    public boolean isOperationFailure() {
        return matchesAny(ILLEGAL_OPERATION, UNSUPPORTED_OPERATION, PRECONDITION_FAILED);
    }

    /**
     * Check if this is a target-related failure.
     *
     * @return true if failure relates to target
     */
    public boolean isTargetFailure() {
        return matchesAny(AMBIGUOUS_TARGET, TARGET_NOT_FOUND, TARGET_LOCKED);
    }

    /**
     * Check if this is a generation-related failure.
     *
     * @return true if failure relates to generation
     */
    public boolean isGenerationFailure() {
        return matchesAny(CONSTRAINT_VIOLATION, INVARIANT_VIOLATION, NON_DETERMINISTIC_RESULT, UNSATISFIABLE_INTENT);
    }

    /**
     * Check if this is a verification-related failure.
     *
     * @return true if failure relates to verification
     */
    public boolean isVerificationFailure() {
        return matchesAny(TEST_FAILURE, COMPILE_FAILURE, VERIFICATION_TIMEOUT);
    }

    /**
     * Check if this is a resource-related failure.
     *
     * @return true if failure relates to resources
     */
    public boolean isResourceFailure() {
        return matchesAny(MODEL_UNAVAILABLE, TOKEN_LIMIT_EXCEEDED, RATE_LIMIT_EXCEEDED);
    }

    /**
     * Get severity level for this failure.
     *
     * @return severity level
     */
    public Severity getSeverity() {
        if (isBlocking()) {
            return Severity.BLOCKING;
        }
        if (isRetriable()) {
            return Severity.RETRIABLE;
        }
        return Severity.WARNING;
    }

    /** Severity levels for failures. */
    public enum Severity {
        /** Can be retried, may succeed on subsequent attempts. */
        RETRIABLE,

        /** Warning only, operation may continue. */
        WARNING,

        /** Blocks all progress, must be fixed. */
        BLOCKING
    }
}
