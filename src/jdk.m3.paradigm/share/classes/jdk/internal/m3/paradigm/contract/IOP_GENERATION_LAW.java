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

import java.util.Arrays;

/**
 * IOP_GENERATION_LAW - The Immutable Rules of IOP Code Generation.
 *
 * <h2>🔒🔒🔒 LOCKED - CANNOT BE OVERRIDDEN 🔒🔒🔒</h2>
 *
 * <h2>THE CORE PRINCIPLE (IOP-GOLD)</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║   CPU does structure.                                                     ║
 * ║   Templates do determinism.                                               ║
 * ║   LLM does only semantic deltas.                                          ║
 * ║                                                                           ║
 * ║   If the LLM is doing structure, you already lost.                        ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>THE FUNDAMENTAL LAW</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║  IF OUTPUT CAN BE PRODUCED FROM A TEMPLATE,                               ║
 * ║  THE LLM MUST NOT TOUCH IT.                                               ║
 * ║                                                                           ║
 * ║  Violations = tooling bug.                                                ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>THE ONE SHARP LINE</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║  LLMs are terrible at factories.                                          ║
 * ║  They are tolerable at judgment.                                          ║
 * ║                                                                           ║
 * ║  IOP turns that into a system.                                            ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>IOP GENERATION STACK (FINAL FORM)</h2>
 *
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  PHASE 0: CONTRACTS (Human)                                             │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │                                                                         │
 * │  @ISpec(value = "Payment processing flow")                              │
 * │  @IAIPattern(dag = DAG.SAGA)                                            │
 * │  public interface IPaymentFlow&lt;Self extends IPaymentFlow&lt;Self>> extends IContext&lt;Self> {                                        │
 * │      PaymentResult process(Payment payment);                            │
 * │  }                                                                      │
 * │                                                                         │
 * │  Zero creativity. Zero guessing.                                        │
 * │                                                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 *                                     │
 *                                     ▼
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  PHASE 1: CPU / TEMPLATE ENGINE (Deterministic) — FREE                  │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │                                                                         │
 * │  CPU + JavaPoet/JTE handles:                                            │
 * │  ✓ Project layout                                                       │
 * │  ✓ Module wiring                                                        │
 * │  ✓ Package naming                                                       │
 * │  ✓ DI skeleton                                                          │
 * │  ✓ Thread pool wiring                                                   │
 * │  ✓ Error-handling scaffolds                                             │
 * │  ✓ Logging hooks                                                        │
 * │  ✓ Metrics hooks                                                        │
 * │  ✓ Class/interface declarations                                         │
 * │  ✓ Method signatures                                                    │
 * │  ✓ Standard patterns (Singleton, Factory, Builder)                      │
 * │  ✓ JavaDoc structure                                                    │
 * │  ✓ Imports                                                              │
 * │                                                                         │
 * │  Templates are STATIC, VERSIONED, DIFFABLE.                             │
 * │  NO AI HERE.                                                            │
 * │                                                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 *                                     │
 *                                     ▼
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  PHASE 2: LLM (Minimal Surface Area) — TOKENS                           │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │                                                                         │
 * │  LLM ONLY SEES:                                                         │
 * │  • Contract annotations                                                 │
 * │  • Invariant expressions                                                │
 * │  • Method signatures                                                    │
 * │  • Local AST context                                                    │
 * │                                                                         │
 * │  LLM OUTPUTS:                                                           │
 * │  • Method bodies                                                        │
 * │  • Small glue logic                                                     │
 * │  • Condition handling                                                   │
 * │  • Comments if requested                                                │
 * │                                                                         │
 * │  LLM NEVER:                                                             │
 * │  ✗ Creates files                                                        │
 * │  ✗ Chooses architecture                                                 │
 * │  ✗ Picks patterns                                                       │
 * │  ✗ Decides threading                                                    │
 * │  ✗ Invents structure                                                    │
 * │  ✗ Generates imports                                                    │
 * │  ✗ Creates packages                                                     │
 * │                                                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 *                                     │
 *                                     ▼
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  PHASE 3: CPU / MERGE + VALIDATE — FREE                                 │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │                                                                         │
 * │  1. Insert LLM output into template placeholders                        │
 * │  2. Format with google-java-format                                      │
 * │  3. Validate with JDT/ecj compiler                                      │
 * │  4. Run tests if specified                                              │
 * │  5. Write to file                                                       │
 * │  6. Update generation registry                                          │
 * │                                                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>WHY LLM COST SCALES WITH ENTROPY</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║  Generating boilerplate = PURE ENTROPY WASTE                              ║
 * ║  • AST wiring            → CPU can do at O(1)                             ║
 * ║  • Imports               → CPU can do at O(1)                             ║
 * ║  • Folders               → CPU can do at O(1)                             ║
 * ║  • Package structure     → CPU can do at O(1)                             ║
 * ║                                                                           ║
 * ║  LLM should ONLY resolve:                                                 ║
 * ║  • Naming (semantic choice)                                               ║
 * ║  • Intent mapping (what does the contract mean?)                          ║
 * ║  • Edge semantics (what happens at boundaries?)                           ║
 * ║  • Constraint resolution (how to satisfy invariants?)                     ║
 * ║                                                                           ║
 * ║  LESS LLM WORK = BETTER RESULTS + LESS HALLUCINATION                      ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>WHAT THIS ENABLES</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║  1. Offline generation (CPU-only mode available)                          ║
 * ║  2. Reproducible builds (templates are versioned)                         ║
 * ║  3. No hallucinated structure (LLM sees minimal scope)                    ║
 * ║  4. LLM swapability (Ollama, local, Claude — doesn't matter)              ║
 * ║  5. IDE-native experience (JDT/LSP friendly)                              ║
 * ║                                                                           ║
 * ║  THIS IS HOW YOU MAKE LLMs BORING AND RELIABLE.                           ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>TOKEN ECONOMICS</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  PURE LLM (BAD):                                                          ║
 * ║  ───────────────                                                          ║
 * ║  Prompt: 500 tokens + Output: 2000 tokens = 2500 tokens                   ║
 * ║  Cost: $0.0075/generation                                                 ║
 * ║                                                                           ║
 * ║  HYBRID IOP (GOOD):                                                       ║
 * ║  ──────────────────                                                       ║
 * ║  Template: 1500 tokens worth = FREE                                       ║
 * ║  LLM: 300 tokens (method bodies only)                                     ║
 * ║  Cost: $0.0009/generation                                                 ║
 * ║                                                                           ║
 * ║  SAVINGS: 88% reduction                                                   ║
 * ║                                                                           ║
 * ║  AT SCALE (1000 generations/day):                                         ║
 * ║  Pure LLM:  $7.50/day = $225/month = $2,700/year                          ║
 * ║  Hybrid:    $0.90/day = $27/month  = $324/year                            ║
 * ║  SAVINGS:   $2,376/year per 1000 daily generations                        ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * @since IOP 1.0 - Generation Law
 * @see TemplateEngines
 * @m3.pattern synexia:iop
 * @m3.algorithm Exposes generation-law text, scope enums and locale-sensitive substring classification helpers; these constants do not enforce generation permissions.
 * @m3.dataStructures Final utility class with String literal constants and nested enums and boolean helper results.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IOP_GENERATION_LAW.java}
 * blob {@code 06df9fbaeb4b6ba5e3d8ecbdf274dfd784ce4188} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IOP_GENERATION_LAW {

    /** Restricts construction of the law catalog.
     * @m3.pattern synexia:iop
     * @m3.algorithm Prevents external construction through the existing private empty constructor.
     * @m3.dataStructures No explicit constructor parameters or instance state.
     */
    private IOP_GENERATION_LAW() {}

    // --- THE IMMUTABLE RULES ---

    /** Rule 1: Template-first generation.
     * @m3.pattern synexia:iop
     * @m3.algorithm Retains the exact RULE_1 generation-law documentation payload for consumers.
     * @m3.dataStructures Existing static final String text block preserving literal content and Java constant identity.
     */
    public static final String RULE_1 = """
			IF OUTPUT CAN BE PRODUCED FROM A TEMPLATE,
			THE LLM MUST NOT TOUCH IT.
			Violations = tooling bug.
			""";

    /** Rule 2: LLM minimal surface.
     * @m3.pattern synexia:iop
     * @m3.algorithm Retains the exact RULE_2 generation-law documentation payload for consumers.
     * @m3.dataStructures Existing static final String text block preserving literal content and Java constant identity.
     */
    public static final String RULE_2 = """
			LLM sees: contracts, invariants, signatures, local context.
			LLM outputs: method bodies, glue logic, conditions.
			LLM NEVER: creates files, chooses architecture, picks patterns.
			""";

    /** Rule 3: CPU handles structure.
     * @m3.pattern synexia:iop
     * @m3.algorithm Retains the exact RULE_3 generation-law documentation payload for consumers.
     * @m3.dataStructures Existing static final String text block preserving literal content and Java constant identity.
     */
    public static final String RULE_3 = """
			CPU does structure.
			Templates do determinism.
			LLM does only semantic deltas.
			""";

    /** Rule 4: Reproducibility.
     * @m3.pattern synexia:iop
     * @m3.algorithm Retains the exact RULE_4 generation-law documentation payload for consumers.
     * @m3.dataStructures Existing static final String text block preserving literal content and Java constant identity.
     */
    public static final String RULE_4 = """
			Templates are STATIC, VERSIONED, DIFFABLE.
			Same input + same template = same output.
			No AI randomness in structure.
			""";

    /** Rule 5: The sharp line.
     * @m3.pattern synexia:iop
     * @m3.algorithm Retains the exact RULE_5 generation-law documentation payload for consumers.
     * @m3.dataStructures Existing static final String text block preserving literal content and Java constant identity.
     */
    public static final String RULE_5 = """
			LLMs are terrible at factories.
			They are tolerable at judgment.
			IOP turns that into a system.
			""";

    // --- BOUNDARY DEFINITIONS ---

    /** What CPU/Template handles (FREE).
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 19 symbolic TemplateScope choices for law metadata; selecting a token performs no enforcement.
     * @m3.dataStructures Nested enum preserving declaration order, names and identities.
     */
    public enum TemplateScope {
        /** Identifies the PROJECT_LAYOUT scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the PROJECT_LAYOUT generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        PROJECT_LAYOUT,
        /** Identifies the MODULE_WIRING scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the MODULE_WIRING generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        MODULE_WIRING,
        /** Identifies the PACKAGE_NAMING scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the PACKAGE_NAMING generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        PACKAGE_NAMING,
        /** Identifies the DI_SKELETON scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the DI_SKELETON generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        DI_SKELETON,
        /** Identifies the THREAD_POOL_WIRING scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the THREAD_POOL_WIRING generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        THREAD_POOL_WIRING,
        /** Identifies the ERROR_HANDLING_SCAFFOLD scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the ERROR_HANDLING_SCAFFOLD generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        ERROR_HANDLING_SCAFFOLD,
        /** Identifies the LOGGING_HOOKS scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the LOGGING_HOOKS generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        LOGGING_HOOKS,
        /** Identifies the METRICS_HOOKS scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the METRICS_HOOKS generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        METRICS_HOOKS,
        /** Identifies the CLASS_DECLARATION scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CLASS_DECLARATION generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        CLASS_DECLARATION,
        /** Identifies the INTERFACE_DECLARATION scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the INTERFACE_DECLARATION generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        INTERFACE_DECLARATION,
        /** Identifies the METHOD_SIGNATURES scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the METHOD_SIGNATURES generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        METHOD_SIGNATURES,
        /** Identifies the FIELD_DECLARATIONS scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the FIELD_DECLARATIONS generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        FIELD_DECLARATIONS,
        /** Identifies the CONSTRUCTORS scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CONSTRUCTORS generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        CONSTRUCTORS,
        /** Identifies the IMPORTS scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IMPORTS generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        IMPORTS,
        /** Identifies the JAVADOC_STRUCTURE scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the JAVADOC_STRUCTURE generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        JAVADOC_STRUCTURE,
        /** Identifies the ANNOTATION_PLACEMENT scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the ANNOTATION_PLACEMENT generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        ANNOTATION_PLACEMENT,
        /** Identifies the STANDARD_PATTERNS scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the STANDARD_PATTERNS generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        STANDARD_PATTERNS,
        /** Identifies the ACCESSORS scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the ACCESSORS generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        ACCESSORS,
        /** Identifies the OBJECT_METHODS scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the OBJECT_METHODS generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        OBJECT_METHODS
    }

    /** What LLM handles (TOKENS).
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 8 symbolic LLMScope choices for law metadata; selecting a token performs no enforcement.
     * @m3.dataStructures Nested enum preserving declaration order, names and identities.
     */
    public enum LLMScope {
        /** Identifies the METHOD_BODIES scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the METHOD_BODIES generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        METHOD_BODIES,
        /** Identifies the GLUE_LOGIC scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the GLUE_LOGIC generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        GLUE_LOGIC,
        /** Identifies the CONDITION_HANDLING scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CONDITION_HANDLING generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        CONDITION_HANDLING,
        /** Identifies the EDGE_CASE_LOGIC scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the EDGE_CASE_LOGIC generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        EDGE_CASE_LOGIC,
        /** Identifies the CONSTRAINT_RESOLUTION scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CONSTRAINT_RESOLUTION generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        CONSTRAINT_RESOLUTION,
        /** Identifies the INTENT_MAPPING scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the INTENT_MAPPING generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        INTENT_MAPPING,
        /** Identifies the SEMANTIC_NAMING scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SEMANTIC_NAMING generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        SEMANTIC_NAMING,
        /** Identifies the COMMENTS scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the COMMENTS generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        COMMENTS
    }

    /** What LLM is FORBIDDEN from doing.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 9 symbolic LLMForbidden choices for law metadata; selecting a token performs no enforcement.
     * @m3.dataStructures Nested enum preserving declaration order, names and identities.
     */
    public enum LLMForbidden {
        /** Identifies the CREATE_FILES scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CREATE_FILES generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        CREATE_FILES,
        /** Identifies the CHOOSE_ARCHITECTURE scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CHOOSE_ARCHITECTURE generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        CHOOSE_ARCHITECTURE,
        /** Identifies the PICK_PATTERNS scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the PICK_PATTERNS generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        PICK_PATTERNS,
        /** Identifies the DECIDE_THREADING scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the DECIDE_THREADING generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        DECIDE_THREADING,
        /** Identifies the INVENT_STRUCTURE scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the INVENT_STRUCTURE generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        INVENT_STRUCTURE,
        /** Identifies the GENERATE_IMPORTS scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the GENERATE_IMPORTS generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        GENERATE_IMPORTS,
        /** Identifies the CREATE_PACKAGES scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CREATE_PACKAGES generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        CREATE_PACKAGES,
        /** Identifies the MODIFY_BUILD_FILES scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the MODIFY_BUILD_FILES generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        MODIFY_BUILD_FILES,
        /** Identifies the CHANGE_DEPENDENCIES scope choice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CHANGE_DEPENDENCIES generation-scope token; the constant does not implement or authorize that operation.
         * @m3.dataStructures Existing nested enum constant preserving identity and declaration order.
         */
        CHANGE_DEPENDENCIES
    }

    // --- VALIDATION ---

    /**
     * Validates that a proposed LLM operation is legal.
     *
     * @param operation
     *            the operation to check
     * @return true if legal, false if forbidden
     * @m3.pattern synexia:iop
     * @m3.algorithm Scans LLMForbidden values with noneMatch, uppercases operation in the default locale, replaces underscores in each enum name with spaces and checks substring absence. Null input retains its existing NullPointerException.
     * @m3.dataStructures Enum values array, sequential stream, String transformations and boolean result.
     */
    public static boolean isLLMOperationLegal(String operation) {
        return Arrays.stream(LLMForbidden.values())
                .noneMatch(forbidden ->
                        operation.toUpperCase().contains(forbidden.name().replace('_', ' ')));
    }

    /**
     * Checks if output should be template-generated.
     *
     * @param outputType
     *            the type of output
     * @return true if template should handle it
     * @m3.pattern synexia:iop
     * @m3.algorithm Scans TemplateScope values with anyMatch, uppercases outputType in the default locale, replaces underscores in each enum name with spaces and checks substring presence. Null input retains its existing NullPointerException.
     * @m3.dataStructures Enum values array, sequential stream, String transformations and boolean result.
     */
    public static boolean shouldUseTemplate(String outputType) {
        return Arrays.stream(TemplateScope.values())
                .anyMatch(
                        scope -> outputType.toUpperCase().contains(scope.name().replace('_', ' ')));
    }
}
