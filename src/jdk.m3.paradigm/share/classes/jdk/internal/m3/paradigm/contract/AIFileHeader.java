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
 * TODO:        Action required - incomplete implementation
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
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIFileHeader.java}
 * blob {@code 9ae3d59d84c8cfd96f6cdd637d31e2e477dc1c17} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIFileHeader {

    // --- IDENTIFICATION ---

    /**
     * File/module owner (team or individual).
     *
     * @return owner
     */
    String owner() default "";

    /**
     * Classification level.
     *
     * @return classification
     */
    Classification classification() default Classification.NORMAL;

    /**
     * Version of this specification.
     *
     * @return version
     */
    String version() default "1.0";

    /**
     * Last reviewed date (ISO-8601).
     *
     * @return review date
     */
    String lastReviewed() default "";

    // --- RFC-2119 REQUIREMENTS (Universal LLM Triggers) ---

    /**
     * MUST requirements - absolute requirements (RFC-2119). LLM MUST follow
     * these without exception.
     *
     * @return must requirements
     */
    String[] must() default {};

    /**
     * MUST NOT prohibitions - absolute prohibitions (RFC-2119). LLM MUST NOT do
     * these under any circumstances.
     *
     * @return must not requirements
     */
    String[] mustNot() default {};

    /**
     * SHOULD recommendations - recommended (RFC-2119). LLM SHOULD follow unless
     * good reason not to.
     *
     * @return should requirements
     */
    String[] should() default {};

    /**
     * SHOULD NOT discouragements - not recommended (RFC-2119). LLM SHOULD NOT
     * do unless good reason to.
     *
     * @return should not requirements
     */
    String[] shouldNot() default {};

    /**
     * MAY options - optional (RFC-2119). LLM MAY do these based on judgment.
     *
     * @return may options
     */
    String[] may() default {};

    // --- CONSTRAINTS ---

    /**
     * Technical constraints.
     *
     * @return constraints
     */
    String[] constraints() default {};

    /**
     * Dependencies this file has.
     *
     * @return dependencies
     */
    String[] dependencies() default {};

    /**
     * Files that depend on this file.
     *
     * @return dependents
     */
    String[] dependents() default {};

    // --- LLM TARGETING ---

    /**
     * Target LLM models.
     *
     * @return models
     */
    AILLMHint.Model[] models() default {AILLMHint.Model.ALL};

    /**
     * Required capability level.
     *
     * @return capability
     */
    AILLMHint.Capability capability() default AILLMHint.Capability.STANDARD;

    /**
     * Temperature hint for generation.
     *
     * @return temperature
     */
    float temperature() default 0.3f;

    // --- CONTEXT ---

    /**
     * Brief description for LLM context.
     *
     * @return description
     */
    String description() default "";

    /**
     * Purpose of this file.
     *
     * @return purpose
     */
    String purpose() default "";

    /**
     * Related files for context.
     *
     * @return related files
     */
    String[] relatedFiles() default {};

    /**
     * Domain/bounded context.
     *
     * @return domain
     */
    String domain() default "";

    // --- ENUMS ---

    /** File classification levels. */
    enum Classification {
        /** Normal file - standard processing. */
        NORMAL,

        /** Important file - careful attention. */
        IMPORTANT,

        /** Critical file - maximum attention, expert review. */
        CRITICAL,

        /** Security-sensitive - security constraints apply. */
        SECURITY,

        /** Data-critical - data integrity constraints apply. */
        DATA,

        /** Performance-critical - performance constraints apply. */
        PERFORMANCE,

        /** Experimental - may change significantly. */
        EXPERIMENTAL,

        /** Deprecated - scheduled for removal. */
        DEPRECATED,

        /** Frozen - do not modify without approval. */
        FROZEN
    }
}
