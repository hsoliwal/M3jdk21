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
 * IOP v1 Operation Model - Closed enum of allowed AI operations.
 *
 * <h2>IOP Authority, Not Intelligence</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  IOP v1: OPERATION MODEL (CLOSED SET)                                     ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  "No change may occur unless it is the result of a declared, legal        ║
 * ║   operation under explicit intent."                                       ║
 * ║                                                                           ║
 * ║  This enum defines the COMPLETE set of operations an AI may perform.      ║
 * ║  Any operation not in this set is ILLEGAL and must be rejected.           ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Why Closed?</h2>
 * <p>
 * Open-ended AI operations lead to unpredictable outcomes. By constraining the
 * operation set, we achieve:
 * </p>
 * <ul>
 * <li>Predictable code changes</li>
 * <li>Auditable transformations</li>
 * <li>Reversible operations</li>
 * <li>Compiler-verifiable intent</li>
 * </ul>
 *
 * <h2>Operation Categories</h2>
 *
 * <pre>
 * GENERATION:
 *   GENERATE_IMPLEMENTATION - Create new implementation from spec
 *
 * REFINEMENT:
 *   REFINE_METHOD          - Improve existing method (same contract)
 *   OPTIMIZE_PERFORMANCE   - Optimize without changing behavior
 *
 * STRUCTURAL:
 *   INTRODUCE_TYPE         - Create new type (interface, class, record, enum)
 *   MOVE_TYPE              - Relocate type to different package
 *   RENAME_SYMBOL          - Rename (class, method, field, parameter)
 *   EXTRACT_METHOD         - Extract code block into new method
 *   INLINE_METHOD          - Inline method body at call sites
 *
 * CLEANUP:
 *   REMOVE_DEAD_CODE       - Delete unreachable/unused code
 *   ORGANIZE_IMPORTS       - Sort and clean imports
 *   FORMAT_CODE            - Apply code style formatting
 *
 * DOCUMENTATION:
 *   GENERATE_JAVADOC       - Create/update JavaDoc
 *   GENERATE_TESTS         - Create unit tests for spec
 * </pre>
 *
 * @since ContextOS 1.0 - IOP v1
 * @see AIOperationsAllowed
 * @see IOPFailure
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/IOPOperation.java}
 * blob {@code 7ec9add6e45a6050940c44ab66c9a4c4a576152d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum IOPOperation {

    // --- GENERATION OPERATIONS ---

    /**
     * Generate implementation from interface + JavaDoc + constraints.
     *
     * <p>
     * This is the primary IOP operation. Given a fully-specified interface,
     * generate a compliant implementation.
     * </p>
     *
     * <pre>
     * Input:  Interface with JavaDoc, @AIGenClass, constraints
     * Output: Implementation class with @AIGenerated marker
     * </pre>
     */
    GENERATE_IMPLEMENTATION,

    /**
     * Generate unit tests from interface specification.
     *
     * <pre>
     * Input:  Interface with JavaDoc, invariants, examples
     * Output: Test class verifying all contracts
     * </pre>
     */
    GENERATE_TESTS,

    /**
     * Generate or update JavaDoc documentation.
     *
     * <pre>
     * Input:  Class/method without or with incomplete JavaDoc
     * Output: Complete JavaDoc following project conventions
     * </pre>
     */
    GENERATE_JAVADOC,

    // --- REFINEMENT OPERATIONS ---

    /**
     * Refine existing method implementation.
     *
     * <p>
     * The method signature and contract MUST remain unchanged. Only the
     * implementation body may be modified.
     * </p>
     *
     * <pre>
     * Input:  Existing method with @AIGenerated
     * Output: Improved implementation, same contract
     * Invariant: All existing tests must still pass
     * </pre>
     */
    REFINE_METHOD,

    /**
     * Optimize performance without changing behavior.
     *
     * <pre>
     * Input:  Working implementation
     * Output: Faster/leaner implementation
     * Invariant: Behavior unchanged, tests pass
     * </pre>
     */
    OPTIMIZE_PERFORMANCE,

    // --- STRUCTURAL OPERATIONS ---

    /**
     * Introduce a new type (interface, class, record, enum).
     *
     * <pre>
     * Input:  Intent description, package location
     * Output: New type with JavaDoc
     * </pre>
     */
    INTRODUCE_TYPE,

    /**
     * Move type to different package.
     *
     * <p>
     * Updates all references automatically.
     * </p>
     *
     * <pre>
     * Input:  Source type, target package
     * Output: Relocated type, updated imports everywhere
     * </pre>
     */
    MOVE_TYPE,

    /**
     * Rename symbol (class, method, field, parameter, variable).
     *
     * <p>
     * Updates all references automatically.
     * </p>
     *
     * <pre>
     * Input:  Symbol location, new name
     * Output: Renamed symbol, updated references
     * Constraint: New name must not conflict
     * </pre>
     */
    RENAME_SYMBOL,

    /**
     * Extract code block into new method.
     *
     * <pre>
     * Input:  Code selection, method name
     * Output: New method + call site replacement
     * </pre>
     */
    EXTRACT_METHOD,

    /**
     * Inline method body at call sites.
     *
     * <p>
     * Inverse of EXTRACT_METHOD.
     * </p>
     *
     * <pre>
     * Input:  Method to inline
     * Output: Call sites replaced with body, method removed
     * </pre>
     */
    INLINE_METHOD,

    // --- CLEANUP OPERATIONS ---

    /**
     * Remove dead (unreachable/unused) code.
     *
     * <pre>
     * Input:  Codebase analysis
     * Output: Removed: unused imports, unreachable code, unused private members
     * Constraint: Only remove provably dead code
     * </pre>
     */
    REMOVE_DEAD_CODE,

    /**
     * Organize imports (sort, remove unused, add missing).
     *
     * <pre>
     * Input:  Java file
     * Output: Clean, sorted imports
     * </pre>
     */
    ORGANIZE_IMPORTS,

    /**
     * Apply code style formatting.
     *
     * <pre>
     * Input:  Java file
     * Output: Formatted according to project style
     * Note: Uses external formatter (not LLM)
     * </pre>
     */
    FORMAT_CODE,

    // --- SPECIAL ---

    /** No operation - used for read-only analysis. */
    NOOP;

    /**
     * Check if this operation is a generation operation.
     *
     * @return true if operation creates new code
     */
    public boolean isGeneration() {
        return this == GENERATE_IMPLEMENTATION || this == GENERATE_TESTS || this == GENERATE_JAVADOC;
    }

    /**
     * Check if this operation is a refinement operation.
     *
     * @return true if operation improves existing code
     */
    public boolean isRefinement() {
        return this == REFINE_METHOD || this == OPTIMIZE_PERFORMANCE;
    }

    /**
     * Check if this operation is a structural operation.
     *
     * @return true if operation changes code structure
     */
    public boolean isStructural() {
        return matchesAny(INTRODUCE_TYPE, MOVE_TYPE, RENAME_SYMBOL, EXTRACT_METHOD, INLINE_METHOD);
    }

    /**
     * Check if this operation is a cleanup operation.
     *
     * @return true if operation cleans code
     */
    public boolean isCleanup() {
        return matchesAny(REMOVE_DEAD_CODE, ORGANIZE_IMPORTS, FORMAT_CODE);
    }

    /**
     * Check if this operation modifies code.
     *
     * @return true if operation changes files
     */
    public boolean modifiesCode() {
        return this != NOOP;
    }

    /**
     * Check if this operation requires LLM.
     *
     * @return true if operation needs AI model
     */
    public boolean requiresLLM() {
        return matchesAny(
                GENERATE_IMPLEMENTATION, GENERATE_TESTS, GENERATE_JAVADOC, REFINE_METHOD, OPTIMIZE_PERFORMANCE);
    }

    /**
     * Check if this operation can be done by IDE/AST tools.
     *
     * @return true if operation is tool-executable
     */
    public boolean isToolExecutable() {
        return isRefactoringTool() || isCodeCleanupTool();
    }

    private boolean isRefactoringTool() {
        return matchesAny(MOVE_TYPE, RENAME_SYMBOL, EXTRACT_METHOD, INLINE_METHOD);
    }

    private boolean isCodeCleanupTool() {
        return matchesAny(REMOVE_DEAD_CODE, ORGANIZE_IMPORTS, FORMAT_CODE);
    }

    private boolean matchesAny(IOPOperation... operations) {
        return java.util.Arrays.stream(operations).anyMatch(operation -> this == operation);
    }
}
