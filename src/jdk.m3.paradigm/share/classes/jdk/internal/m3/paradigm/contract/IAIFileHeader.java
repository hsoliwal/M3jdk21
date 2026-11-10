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

import jdk.internal.m3.paradigm.annotation.Model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIFileHeader - FILE-LEVEL annotation for LLM attention triggers.
 *
 * <h2>╔═══════════════════════════════════════════════════════════════════════╗</h2>
 * <h2>║ @LLM-ATTENTION: THIS ANNOTATION MUST BE AT THE TOP OF EVERY FILE ║</h2>
 * <h2>╚═══════════════════════════════════════════════════════════════════════╝</h2>
 *
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  WHEN AN LLM READS THIS FILE, IT SHOULD FIRST LOOK AT @AIFileHeader    │
 * │  TO UNDERSTAND:                                                         │
 * │    • What this file does                                                │
 * │    • What constraints apply                                             │
 * │    • What the LLM MUST and MUST NOT do                                  │
 * │    • Which LLM model should process this                                │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Universal LLM Triggers (Work Across All Models)</h2>
 *
 * <pre>
 * CRITICAL:    Highest attention - security, data integrity
 * IMPORTANT:   High attention - core functionality
 * WARNING:     Caution required - edge cases, gotchas
 * NOTE:        Information - context, background
 * TO-DO:       Action required - incomplete implementation
 * FIXME:       Bug - known issue needs fixing
 * MUST:        RFC-2119 - absolute requirement
 * MUST NOT:    RFC-2119 - absolute prohibition
 * SHOULD:      RFC-2119 - recommended
 * SHOULD NOT:  RFC-2119 - not recommended
 * MAY:         RFC-2119 - optional
 * </pre>
 *
 * <h2>Usage - MUST be first annotation in file</h2>
 *
 * <pre>{@code
 * /*
 *  * @LLM-ATTENTION
 *  * OWNER: security-team
 *  * CLASSIFICATION: CRITICAL
 *  * MUST: Follow security best practices
 *  * MUST NOT: Log sensitive data
 *  * CONSTRAINTS: No reflection, no dynamic class loading
 *  *\/
 * &#64;AIFileHeader(
 *     owner = "security-team",
 *     classification = Classification.CRITICAL,
 *     must = {"Follow security best practices", "Validate all input"},
 *     mustNot = {"Log sensitive data", "Use reflection"},
 *     constraints = {"No dynamic class loading"}
 * )
 * package com.example.security;
 * }</pre>
 *
 * <h2>Simulated Header Block (For LLM Parsing)</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ @LLM-ATTENTION                                                           ║
 * ║ ═══════════════════════════════════════════════════════════════════════  ║
 * ║ FILE: ISecurityService.java                                              ║
 * ║ OWNER: security-team                                                     ║
 * ║ CLASSIFICATION: CRITICAL                                                 ║
 * ║ ───────────────────────────────────────────────────────────────────────  ║
 * ║ MUST:                                                                    ║
 * ║   • Validate all input                                                   ║
 * ║   • Use parameterized queries                                            ║
 * ║   • Hash passwords with bcrypt                                           ║
 * ║ MUST NOT:                                                                ║
 * ║   • Log passwords or tokens                                              ║
 * ║   • Use String concatenation for SQL                                     ║
 * ║   • Store secrets in code                                                ║
 * ║ ───────────────────────────────────────────────────────────────────────  ║
 * ║ MODELS: CLAUDE_OPUS, GPT_4 (requires expert capability)                  ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * @since IOP 1.0 - AnOP File-Level LLM Attention
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares package-level file-context, ownership, requirements, model and review hints; its actual target is PACKAGE, and the annotation validates no ownership claim or requirement.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.PACKAGE}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIFileHeader.java}
 * blob {@code 0fc494c4c05f8b4772a4e6ccde3d87f332e8c13b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIFileHeader {

    // --- IDENTIFICATION ---

    /**
     * File/module owner (team or individual).
     *
     * @return owner
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#owner intent: File/module owner (team or individual). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String owner() default "";

    /**
     * Classification level.
     *
     * @return classification
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#classification intent: Classification level. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Classification}; default expression {@code Classification.NORMAL}.
     */
    Classification classification() default Classification.NORMAL;

    /**
     * Version of this specification.
     *
     * @return version
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#version intent: Version of this specification. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "1.0"}.
     */
    String version() default "1.0";

    /**
     * Last reviewed date (ISO-8601).
     *
     * @return review date
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#lastReviewed intent: Last reviewed date (ISO-8601). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String lastReviewed() default "";

    // --- RFC-2119 REQUIREMENTS (Universal LLM Triggers) ---

    /**
     * MUST requirements - absolute requirements (RFC-2119). LLM MUST follow these
     * without exception.
     *
     * @return must requirements
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#must intent: Recorded mandatory requirement strings for downstream generation context. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] must() default {};

    /**
     * MUST NOT prohibitions - absolute prohibitions (RFC-2119). LLM MUST NOT do
     * these under any circumstances.
     *
     * @return must not requirements
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#mustNot intent: Recorded prohibited-operation strings for downstream generation context. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] mustNot() default {};

    /**
     * SHOULD recommendations - recommended (RFC-2119). LLM SHOULD follow unless
     * good reason not to.
     *
     * @return should requirements
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#should intent: Recorded recommendation strings for downstream generation context. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] should() default {};

    /**
     * SHOULD NOT discouragements - not recommended (RFC-2119). LLM SHOULD NOT do
     * unless good reason to.
     *
     * @return should not requirements
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#shouldNot intent: Recorded discouraged-operation strings for downstream generation context. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] shouldNot() default {};

    /**
     * MAY options - optional (RFC-2119). LLM MAY do these based on judgment.
     *
     * @return may options
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#may intent: Recorded optional-operation strings for downstream generation context. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] may() default {};

    // --- CONSTRAINTS ---

    /**
     * Technical constraints.
     *
     * @return constraints
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#constraints intent: Technical constraints. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] constraints() default {};

    /**
     * Dependencies this file has.
     *
     * @return dependencies
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#dependencies intent: Dependencies this file has. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] dependencies() default {};

    /**
     * Files that depend on this file.
     *
     * @return dependents
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#dependents intent: Files that depend on this file. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] dependents() default {};

    // --- LLM TARGETING ---

    /**
     * Target LLM models.
     *
     * @return models
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#models intent: Target LLM models. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code AILLMHint.Model[]}; default expression {@code {AILLMHint.Model.ALL}}.
     */
    AILLMHint.Model[] models() default {AILLMHint.Model.ALL};

    /**
     * Required capability level.
     *
     * @return capability
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#capability intent: Required capability level. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code AILLMHint.Capability}; default expression {@code AILLMHint.Capability.STANDARD}.
     */
    AILLMHint.Capability capability() default AILLMHint.Capability.STANDARD;

    /**
     * Temperature hint for generation.
     *
     * @return temperature
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#temperature intent: Temperature hint for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code float}; default expression {@code 0.3F}.
     */
    float temperature() default 0.3f;

    // --- CONTEXT ---

    /**
     * Brief description for LLM context.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#description intent: Brief description for LLM context. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String description() default "";

    /**
     * Purpose of this file.
     *
     * @return purpose
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#purpose intent: Purpose of this file. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String purpose() default "";

    /**
     * Related files for context.
     *
     * @return related files
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#relatedFiles intent: Related files for context. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] relatedFiles() default {};

    /**
     * Domain/bounded context.
     *
     * @return domain
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIFileHeader#domain intent: Domain/bounded context. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String domain() default "";

    // --- ENUMS ---

    /** File classification levels. */
    /**
     * Classification.
     * @author Synexia
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAIFileHeader.Classification choices described as Classification. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 9 constants in the existing source order.
     */
    enum Classification {
        /** Normal file - standard processing.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIFileHeader.Classification#NORMAL policy choice: Normal file - standard processing. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIFileHeader.Classification; its declared order and identity are preserved.
         */
        NORMAL,

        /** Important file - careful attention.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIFileHeader.Classification#IMPORTANT policy choice: Important file - careful attention. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIFileHeader.Classification; its declared order and identity are preserved.
         */
        IMPORTANT,

        /** Critical file - maximum attention, expert review.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIFileHeader.Classification#CRITICAL policy choice: Critical file - maximum attention, expert review. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIFileHeader.Classification; its declared order and identity are preserved.
         */
        CRITICAL,

        /** Security-sensitive - security constraints apply.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIFileHeader.Classification#SECURITY policy choice: Security-sensitive - security constraints apply. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIFileHeader.Classification; its declared order and identity are preserved.
         */
        SECURITY,

        /** Data-critical - data integrity constraints apply.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIFileHeader.Classification#DATA policy choice: Data-critical - data integrity constraints apply. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIFileHeader.Classification; its declared order and identity are preserved.
         */
        DATA,

        /** Performance-critical - performance constraints apply.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIFileHeader.Classification#PERFORMANCE policy choice: Performance-critical - performance constraints apply. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIFileHeader.Classification; its declared order and identity are preserved.
         */
        PERFORMANCE,

        /** Experimental - may change significantly.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIFileHeader.Classification#EXPERIMENTAL policy choice: Experimental - may change significantly. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIFileHeader.Classification; its declared order and identity are preserved.
         */
        EXPERIMENTAL,

        /** Deprecated - scheduled for removal.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIFileHeader.Classification#DEPRECATED policy choice: Deprecated - scheduled for removal. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIFileHeader.Classification; its declared order and identity are preserved.
         */
        DEPRECATED,

        /** Frozen - do not modify without approval.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIFileHeader.Classification#FROZEN policy choice: Requested frozen-source classification; annotation use itself grants no permission to change the source. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIFileHeader.Classification; its declared order and identity are preserved.
         */
        FROZEN
    }
}
