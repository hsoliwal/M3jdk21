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
 * AIGenMethod - Marks method for AI-generated implementation.
 *
 * <h2>IOP Two-Phase AOP: Method-Level Generation</h2>
 *
 * <pre>
 * +-------------------------------------------------------------------------+
 * |  METHOD GENERATION WORKFLOW                                             |
 * +-------------------------------------------------------------------------+
 * |                                                                         |
 * |  Code is the memory. Code is the contract. Code is the implementation.  |
 * |  Code is the document. Code is the text.                                |
 * |                                                                         |
 * |  Method Generation Flow:                                                |
 * |    ┌─────────────────────────────────────────────────────────────────┐  |
 * |    │ 1. Read JavaDoc on method (primary spec)                        │  |
 * |    │ 2. Read @AIGenMethod.value() (additional context)               │  |
 * |    │ 3. Read @AIGenMethod.constraints() (boundaries)                 │  |
 * |    │ 4. Read @AIGenMethod.invariants() (assertions)                  │  |
 * |    │ 5. Compute method-level spec hash                               │  |
 * |    │ 6. If hash unchanged → skip generation                          │  |
 * |    │ 7. If hash changed → generate with specified model              │  |
 * |    │ 8. If testMethod() → verify against test                        │  |
 * |    └─────────────────────────────────────────────────────────────────┘  |
 * |                                                                         |
 * +-------------------------------------------------------------------------+
 * </pre>
 *
 * <h2>From Grand Java Patterns Proof</h2> <blockquote> "You no longer prompt
 * the AI. You edit the program." </blockquote>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	interface IMemoryIngestor {
 *
 * 		// Method inherits model from @AIGenClass, uses JavaDoc as spec
 * 		&#64;AIGenMethod("""
 * 				Validate input.
 * 				Normalize text.
 * 				Dispatch to intent extractor.
 * 				Idempotent. Non-blocking.
 * 				""")
 * 		void ingest(TextInput input);
 *
 * 		// Method-level model override for complex logic (OPUS for NLP)
 * 		&#64;AIGenMethod(value = "Extract emotion", model = Model.ModelType.OPUS, constraints = {"Handle Unicode",
 * 				"Empty on failure"}, invariants = {"result.confidence >= 0.0", "result.confidence &lt;= 1.0"})
 * 		EmotionVector extractEmotion(String text);
 *
 * 		// Simple method can use HAIKU
 * 		&#64;AIGenMethod(model = Model.ModelType.HAIKU)
 * 		String sanitize(String input);
 * 	}
 * }
 * </pre>
 *
 * <h2>JavaDoc + @AIGenMethod Synergy</h2>
 *
 * <pre>{@code
 * /**
 *  * Process a single text input.
 *  *
 *  * <p>This method validates the input, normalizes whitespace,
 *  * and dispatches to the intent extractor pipeline.
 *  *
 *  * @param input raw user text (never null)
 *  * @return processed result (never null)
 *  * @throws InvalidInputException if text is malformed
 *  * @see IIntentExtractor
 *  * /
 * &#64;AIGenMethod(invariants = {"return != null", "!return.isEmpty()"})
 * ProcessResult ingest(TextInput input);
 * }</pre>
 *
 * <h2>Why JavaDoc Is Enough</h2>
 * <p>
 * JavaDoc is already a rich, standardized, versioned, diffable, tool-understood
 * language. It has @param, @return, @throws, @see, @link. No new DSL needed.
 * The annotation adds generation metadata only.
 * </p>
 *
 * <h2>Spec Hash Computation (Method Level)</h2>
 *
 * <pre>
 * Hash inputs:
 *   - Method signature (name, params, return type)
 *   - Method JavaDoc (normalized)
 *   - @AIGenMethod.value()
 *   - @AIGenMethod.constraints()
 *   - @AIGenMethod.invariants()
 *
 * Hash algorithm: SHA-256
 * Format: "sha256:{hex-digest}"
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP IOP
 * @see AIGenClass
 * @see AIGenerated
 * @see SpecHash
 * @see Model
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/AIGenMethod.java}
 * blob {@code 49fad495599c654ae3c606046c8c40866823423a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIGenMethod {

    /**
     * Natural language description of method behavior. Supplements JavaDoc - use
     * for generation hints.
     *
     * @return behavior description
     */
    String value() default "";

    /**
     * Preferred model for generating this method. Overrides
     * class-level @AIGenClass.model().
     *
     * @return model type (DEFAULT inherits from class)
     */
    Model.ModelType model() default Model.ModelType.DEFAULT;

    /**
     * Test method name to verify generated implementation. Must be in the test
     * class specified by @AIGenClass.test().
     *
     * @return test method name (empty = no test)
     */
    String testMethod() default "";

    /**
     * Constraints that guide generation. These are soft requirements that influence
     * the generated code.
     *
     * @return constraint descriptions
     */
    String[] constraints() default {};

    /**
     * Invariants that MUST hold in the generated code. These become assertions in
     * the generated method. Format: Java boolean expressions using parameter names.
     *
     * <pre>{@code
     *
     * &#64;AIGenMethod(invariants = {"input != null", "result >= 0", "result <= input.length()"})
     * int countWords(String input);
     * }</pre>
     *
     * @return invariant expressions
     */
    String[] invariants() default {};

    /**
     * Pre-conditions that must be true before method execution. These become
     * validation checks at method entry.
     *
     * @return pre-condition expressions
     */
    String[] preconditions() default {};

    /**
     * Post-conditions that must be true after method execution. These become
     * validation checks before return.
     *
     * @return post-condition expressions
     */
    String[] postconditions() default {};

    /**
     * Example inputs and outputs for the AI to learn from. Format: "input ->
     * expected_output"
     *
     * <pre>{@code
     *
     * &#64;AIGenMethod(examples = {"\"hello world\" -> 2", "\"\" -> 0", "\"one\" -> 1"})
     * int countWords(String input);
     * }</pre>
     *
     * @return example pairs
     */
    String[] examples() default {};

    /**
     * Whether to generate logging statements in the method.
     *
     * @return true to generate logging (default: false)
     */
    boolean generateLogging() default false;

    /**
     * Whether to generate null checks for parameters.
     *
     * @return true to generate null checks (default: true)
     */
    boolean nullChecks() default true;

    /**
     * Maximum complexity score allowed for generated method. 0 means no limit. Uses
     * cyclomatic complexity.
     *
     * @return max complexity (default: 10)
     */
    int maxComplexity() default 10;

    // --- HISTORY TRACKING - Internal Pointer to Method History ---

    /**
     * History ID for this method's generation history. Auto-generated if empty.
     * Used as internal pointer to history file/DB.
     *
     * <pre>
     * IOP reads method → finds historyId → loads history:
     *   .iop-registry/methods/{historyId}.jsonl
     *
     * History entry:
     * {
     *   "id": "method-uuid",
     *   "interface": "com.example.IUserService",
     *   "method": "authenticate",
     *   "signature": "(String,String)AuthResult",
     *   "specHash": "sha256:abc123...",
     *   "model": "OPUS",
     *   "generatedAt": "2025-01-15T10:30:00Z",
     *   "tokensUsed": 847,
     *   "durationMs": 1234,
     *   "testMethod": "testAuthenticate",
     *   "testPassed": true,
     *   "genCount": 5,
     *   "prevHash": "sha256:xyz789..."
     * }
     * </pre>
     *
     * @return history ID (empty = auto-generate)
     */
    String historyId() default "";

    /**
     * Whether to track this method's generation history. Inherits
     * from @AIGenClass.trackMethods() if not specified.
     *
     * @return true to track history (default: true)
     */
    boolean trackHistory() default true;

    /**
     * Previous spec hash for this method (for diffing). Auto-populated by the
     * generator from registry.
     *
     * @return previous spec hash
     */
    String prevSpecHash() default "";

    /**
     * Generation count for this method. Incremented each time the method is
     * regenerated.
     *
     * @return generation count
     */
    int genCount() default 0;

    // --- SHORTCOMINGS IDENTIFICATION (Self-Improvement) ---

    /**
     * Known shortcomings in the current implementation. LLM identifies these during
     * generation and records them. Used for future improvement passes.
     *
     * <pre>
     * &#64;AIGenMethod(
     *     value = "Complex algorithm",
     *     shortcomings = {
     *         "O(n²) complexity - could be optimized to O(n log n)",
     *         "Edge case: empty input not fully tested",
     *         "Consider adding caching for repeated calls"
     *     }
     * )
     * </pre>
     *
     * @return array of identified shortcomings
     */
    String[] shortcomings() default {};

    /**
     * Improvement suggestions for future iterations. LLM proposes these based on
     * code analysis.
     *
     * @return array of improvement suggestions
     */
    String[] improvements() default {};

    /**
     * Technical debt level identified by LLM.
     *
     * @return debt level
     */
    DebtLevel debt() default DebtLevel.NONE;

    /** Technical debt levels for tracking code quality. */
    enum DebtLevel {
        /** No technical debt identified. */
        NONE,

        /** Minor issues - cosmetic, non-urgent. */
        LOW,

        /** Moderate issues - should address soon. */
        MEDIUM,

        /** Significant issues - address in next iteration. */
        HIGH,

        /** Critical issues - requires immediate attention. */
        CRITICAL
    }
}
