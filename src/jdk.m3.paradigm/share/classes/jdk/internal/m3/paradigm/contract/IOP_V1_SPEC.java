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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IOP v1 - Library-Bound Generation Specification.
 *
 * <h2>THE ONE IOP SENTENCE (FINAL FORM)</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║   Annotations choose libraries.                                          ║
 * ║   Templates choose structure.                                            ║
 * ║   Markers choose LLM scope.                                              ║
 * ║   Anything else is illegal.                                              ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>STEP 1: @AnOPLibrary Schema (Foundation)</h2>
 * <p>
 * Declare legal implementation families. Nothing outside this list may appear
 * in code.
 *
 * <h2>STEP 2: Annotation → Template Selection</h2>
 * <p>
 * Templates are library-bound, never generic. CPU selects, not LLM.
 *
 * <h2>STEP 3: Marker Syntax (LLM Jurisdiction)</h2>
 * <p>
 * Canonical marker defines where LLM can write. Silence = prohibition.
 *
 * <h2>STEP 4: Marker ↔ Annotation Inheritance</h2>
 * <p>
 * Markers inherit constraints automatically from annotations.
 *
 * <h2>STEP 5: LLM Write Permissions Matrix</h2>
 * <p>
 * LLM can only write method bodies inside markers. Everything else is
 * forbidden.
 *
 * <h2>STEP 6: Diff Validator (Build Gate)</h2>
 * <p>
 * Build fails if LLM edits outside markers.
 *
 * <h2>STEP 7: End-to-End Flow</h2>
 *
 * <pre>
 * 1. Human writes annotations
 * 2. CPU selects template (library-bound)
 * 3. Template expands structure
 * 4. Markers declare LLM scope
 * 5. LLM fills semantic gaps
 * 6. Diff validator enforces legality
 * 7. JDT/LSP reflects intent + constraints
 * </pre>
 *
 * @since IOP 1.0
 * @m3.pattern synexia:iop
 * @m3.algorithm Groups an embedded source-retained library annotation, exact text constants, ordered write-permission and flow labels, and a bodyless diff-validator interface. Text and descriptors do not parse templates or enforce editing constraints.
 * @m3.dataStructures Four static String constants, one annotation type, two nested enums and one nested interface with three method contracts.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IOP_V1_SPEC.java}
 * blob {@code 44bf421ac0632cb4b73488c1169297de2bf30e47} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IOP_V1_SPEC {

    /**
     * Private constructor for the static declaration owner.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty private constructor body, preventing ordinary caller construction.
     * @m3.dataStructures No constructor parameters or declared instance state.
     */
    private IOP_V1_SPEC() {}

    // --- STEP 1: @AnOPLibrary SCHEMA ---

    /**
     * AnOPLibrary - Declares legal implementation families.
     *
     * <p>
     * Hard rule: If a library is not declared here, it is illegal everywhere.
     *
     * <h2>Usage Example</h2>
     *
     * <pre>
     * {@code
     * &#64;AnOPTransport(
     *     type = Transport.HTTP,
     *     libraries = {
     *         &#64;AnOPLibrary(name = "apache-httpclient", version = "5.x", preferred = true),
     *         &#64;AnOPLibrary(name = "okhttp", version = "4.x")
     *     }
     * )
     * }
     * </pre>
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares name, version and preferred elements, with SOURCE retention and an empty declaration target set for annotation-value use. No runtime library loading or validation is performed.
     * @m3.dataStructures One required String element, one String element defaulting to empty, and one boolean element defaulting to false.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({})
    public @interface AnOPLibrary {

        /** Canonical library ID (not Maven artifactId)
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares a bodyless String annotation element with no default value. The element defines metadata and has no executable implementation body.
         * @m3.dataStructures One String annotation element value required explicitly.
         */
        String name();

        /** Version constraint
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares a bodyless String annotation element with default empty String. The element defines metadata and has no executable implementation body.
         * @m3.dataStructures One String annotation element value with a declared default.
         */
        String version() default "";

        /** Whether this is the preferred implementation
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares a bodyless boolean annotation element with default false. The element defines metadata and has no executable implementation body.
         * @m3.dataStructures One boolean annotation element value with a declared default.
         */
        boolean preferred() default false;
    }

    // --- STEP 2: TEMPLATE DIRECTORY LAW ---

    /**
     * Template directory structure law.
     *
     * <p>
     * Templates are library-bound, never generic.
     *
     * <pre>
     * templates/
     *  └── transport/
     *      └── http/
     *          └── apache-httpclient/
     *              └── Client.java.tpl
     * </pre>
     *
     * <h4>Selection Algorithm (CPU, not LLM)</h4>
     * <ol>
     * <li>Read annotation (@AnOPTransport)</li>
     * <li>Extract type</li>
     * <li>Resolve preferred library</li>
     * <li>Map to template path</li>
     * <li>Expand template</li>
     * <li>Freeze structure</li>
     * </ol>
     *
     * <p>
     * <strong>LLM is not involved.</strong>
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String TEMPLATE_DIRECTORY_LAW = """
			templates/
			 └── {category}/
			     └── {type}/
			         └── {library}/
			             └── {Name}.java.ftl
			""";

    // --- STEP 3: MARKER SYNTAX ---

    /**
     * Canonical marker syntax for LLM jurisdiction.
     *
     * <pre>
     * // &lt;IOP-LLM-ALLOWED:id&gt;
     * // LANG=Java
     * // ROLE=business-logic
     * // LIBRARY=apache-httpclient
     * // CONSTRAINTS:
     * // - must satisfy @IopInvariant
     * // - no new imports
     * // - no threading
     * // &lt;/IOP-LLM-ALLOWED&gt;
     * </pre>
     *
     * <h4>Marker Semantics</h4>
     * <table><caption>Table</caption>
     * <tr>
     * <th>Region</th>
     * <th>Owner</th>
     * <th>Mutable</th>
     * </tr>
     * <tr>
     * <td>Outside marker</td>
     * <td>CPU / Template</td>
     * <td>NO</td>
     * </tr>
     * <tr>
     * <td>Inside marker</td>
     * <td>LLM</td>
     * <td>YES (bounded)</td>
     * </tr>
     * </table>
     *
     * <p>
     * <strong>Silence = prohibition.</strong>
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String MARKER_SYNTAX = """
			// <IOP-LLM-ALLOWED:{id}>
			// LANG={language}
			// ROLE={role}
			// LIBRARY={library}
			// CONSTRAINTS:
			// - {constraint1}
			// - {constraint2}
			// </IOP-LLM-ALLOWED>
			""";

    // --- STEP 4: MARKER ↔ ANNOTATION INHERITANCE ---

    /**
     * Markers inherit constraints automatically from annotations.
     *
     * <p>
     * Example inheritance:
     * <ul>
     * <li>@IopInvariant → must be respected inside marker</li>
     * <li>@AnOPTransport → LLM may not change transport</li>
     * <li>@AnOPLibrary → LLM may only use declared library</li>
     * </ul>
     *
     * <p>
     * LLM sees expanded constraint context, not raw freedom.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String INHERITANCE_LAW = """
			Annotations cascade to markers.
			LLM sees constraints, not freedom.
			""";

    // --- STEP 5: LLM WRITE PERMISSIONS MATRIX ---

    /** LLM write permissions matrix.
     * @m3.pattern synexia:iop
     * @m3.algorithm Constructs 7 ordered enum values with the supplied boolean flag, stores it unchanged and exposes it through isAllowed. The descriptor values do not enforce source restrictions.
     * @m3.dataStructures Enum singleton values with one final boolean field each.
     */
    public enum LLMPermission {

        /** Write method body - ALLOWED
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the WRITE_METHOD_BODY singleton with the boolean literal true in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        WRITE_METHOD_BODY(true),

        /** Add private helpers inside marker - CONDITIONAL
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the ADD_PRIVATE_HELPERS_INSIDE singleton with the boolean literal true in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        ADD_PRIVATE_HELPERS_INSIDE(true),

        /** Add imports - FORBIDDEN
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the ADD_IMPORTS singleton with the boolean literal false in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        ADD_IMPORTS(false),

        /** Change class signature - FORBIDDEN
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the CHANGE_CLASS_SIGNATURE singleton with the boolean literal false in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        CHANGE_CLASS_SIGNATURE(false),

        /** Add dependencies - FORBIDDEN
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the ADD_DEPENDENCIES singleton with the boolean literal false in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        ADD_DEPENDENCIES(false),

        /** Choose different library - FORBIDDEN
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the CHOOSE_DIFFERENT_LIBRARY singleton with the boolean literal false in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        CHOOSE_DIFFERENT_LIBRARY(false),

        /** Change threading/IO model - FORBIDDEN
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the CHANGE_THREADING_IO_MODEL singleton with the boolean literal false in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        CHANGE_THREADING_IO_MODEL(false);

        /**
         * Immutable allowed descriptor flag.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the constructor-supplied allowed boolean value in the corresponding final field without validation or normalization.
         * @m3.dataStructures One final boolean field per instance.
         */
        private final boolean allowed;

        /**
         * Initializes one LLMPermission descriptor flag.
         * @m3.pattern synexia:bop
         * @m3.algorithm Assigns the supplied boolean allowed parameter to its final field without validation.
         * @m3.dataStructures One boolean parameter and one final boolean instance field.
         */
        LLMPermission(boolean allowed) {
            this.allowed = allowed;
        }

        /**
         * Accessor for the stored allowed value.
         * @m3.pattern synexia:bop
         * @m3.algorithm Returns the existing allowed field directly without conversion, validation or mutation.
         * @m3.dataStructures One stored boolean value and its returned value.
         */
        public boolean isAllowed() {
            return allowed;
        }
    }

    // --- STEP 6: DIFF VALIDATOR (BUILD GATE) ---

    /**
     * Diff validation rules.
     *
     * <ol>
     * <li>No edits outside IOP-LLM-ALLOWED</li>
     * <li>No new imports unless explicitly marked</li>
     * <li>No dependency drift</li>
     * <li>No public API change</li>
     * <li>Invariants preserved</li>
     * </ol>
     *
     * <p>
     * <strong>Failure mode:</strong>
     * <ul>
     * <li>Build fails</li>
     * <li>Agent output rejected</li>
     * <li>No human arbitration required</li>
     * </ul>
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares three bodyless boolean methods. Implementations supply editing, import and content validation; this interface provides no region parser, import policy or invariant enforcement algorithm.
     * @m3.dataStructures String names/content, primitive line numbers, caller-supplied List of String imports and three boolean return contracts.
     */
    public interface DiffValidator {

        /** Check if edit is within allowed region
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares a boolean validation operation without a method body. Any validation algorithm, null policy, exception behavior and effects belong to the implementing class.
         * @m3.dataStructures One String filename and two int line arguments and a boolean return contract.
         */
        boolean isEditAllowed(String filename, int startLine, int endLine);

        /** Check if imports are allowed
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares a boolean validation operation without a method body. Any validation algorithm, null policy, exception behavior and effects belong to the implementing class.
         * @m3.dataStructures One String filename and a List of String imports and a boolean return contract.
         */
        boolean areImportsAllowed(String filename, java.util.List<String> imports);

        /** Check if invariants are preserved
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares a boolean validation operation without a method body. Any validation algorithm, null policy, exception behavior and effects belong to the implementing class.
         * @m3.dataStructures Two String references for filename and content and a boolean return contract.
         */
        boolean areInvariantsPreserved(String filename, String content);
    }

    // --- STEP 7: END-TO-END FLOW ---

    /**
     * End-to-end flow.
     *
     * <pre>
     * 1. Human writes annotations
     * 2. CPU selects template (library-bound)
     * 3. Template expands structure
     * 4. Markers declare LLM scope
     * 5. LLM fills semantic gaps
     * 6. Diff validator enforces legality
     * 7. JDT/LSP reflects intent + constraints
     * </pre>
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares 7 enum singleton values in source order. The values label intent and perform no independent validation or enforcement.
     * @m3.dataStructures Ordered enum singleton values and compiler-generated values/valueOf accessors.
     */
    public enum FlowStep {
        /**
         * Human writes annotations flow-step label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the HUMAN_WRITES_ANNOTATIONS enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        HUMAN_WRITES_ANNOTATIONS,
        /**
         * Cpu selects template flow-step label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the CPU_SELECTS_TEMPLATE enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        CPU_SELECTS_TEMPLATE,
        /**
         * Template expands structure flow-step label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the TEMPLATE_EXPANDS_STRUCTURE enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        TEMPLATE_EXPANDS_STRUCTURE,
        /**
         * Markers declare llm scope flow-step label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the MARKERS_DECLARE_LLM_SCOPE enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        MARKERS_DECLARE_LLM_SCOPE,
        /**
         * Llm fills semantic gaps flow-step label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the LLM_FILLS_SEMANTIC_GAPS enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        LLM_FILLS_SEMANTIC_GAPS,
        /**
         * Diff validator enforces flow-step label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the DIFF_VALIDATOR_ENFORCES enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        DIFF_VALIDATOR_ENFORCES,
        /**
         * Jdt lsp reflects intent flow-step label.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the JDT_LSP_REFLECTS_INTENT enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        JDT_LSP_REFLECTS_INTENT
    }

    // --- WHY THIS WORKS ---

    /**
     * Why IOP works (factual).
     *
     * <ul>
     * <li>No converter hell (no IDL)</li>
     * <li>No hallucinated glue</li>
     * <li>No refactor anxiety</li>
     * <li>CPU does repetition</li>
     * <li>LLM does meaning only</li>
     * <li>Humans stop fighting tools</li>
     * </ul>
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String WHY_THIS_WORKS = "This is IOP done right.";
}
