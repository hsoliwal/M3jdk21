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
 * AIGenerated - Marks code that was generated from intent specification.
 *
 * <h2>IOP Generation Registry</h2>
 *
 * <pre>
 * +-------------------------------------------------------------------------+
 * |  GENERATION REGISTRY: TRACKING ALL LLM-GENERATED CODE                   |
 * +-------------------------------------------------------------------------+
 * |                                                                         |
 * |  "Code is the memory. Code is the contract. Code is the implementation. |
 * |   Code is the document. Code is the text."                              |
 * |                                                                         |
 * |  Registry Storage Options:                                              |
 * |    ┌─────────────────────────────────────────────────────────────────┐  |
 * |    │ FLAT FILE: .iop-registry/generations.jsonl                      │  |
 * |    │   {"id":"uuid","specHash":"sha256:...","model":"OPUS",...}      │  |
 * |    │   One line per generation, append-only, git-friendly            │  |
 * |    └─────────────────────────────────────────────────────────────────┘  |
 * |    ┌─────────────────────────────────────────────────────────────────┐  |
 * |    │ SQLITE: .iop-registry/generations.db                            │  |
 * |    │   CREATE TABLE generations (                                    │  |
 * |    │     id TEXT PRIMARY KEY,                                        │  |
 * |    │     spec_hash TEXT NOT NULL,                                    │  |
 * |    │     source_interface TEXT NOT NULL,                             │  |
 * |    │     generated_class TEXT NOT NULL,                              │  |
 * |    │     model TEXT NOT NULL,                                        │  |
 * |    │     stability TEXT NOT NULL,                                    │  |
 * |    │     generated_at TEXT NOT NULL,                                 │  |
 * |    │     generation_count INTEGER DEFAULT 1,                         │  |
 * |    │     test_passed BOOLEAN,                                        │  |
 * |    │     tokens_used INTEGER,                                        │  |
 * |    │     duration_ms INTEGER,                                        │  |
 * |    │     prev_spec_hash TEXT,                                        │  |
 * |    │     diff_summary TEXT                                           │  |
 * |    │   );                                                            │  |
 * |    └─────────────────────────────────────────────────────────────────┘  |
 * |                                                                         |
 * |  Registry Benefits:                                                     |
 * |    - Full history of all generations                                    |
 * |    - Incremental builds (skip unchanged specs)                          |
 * |    - Cost tracking (tokens used per generation)                         |
 * |    - Audit trail (who/what/when/why)                                    |
 * |    - Rollback capability (previous versions)                            |
 * |    - Analytics (model performance, failure rates)                       |
 * |                                                                         |
 * +-------------------------------------------------------------------------+
 * </pre>
 *
 * <h2>From Grand Java Patterns Proof - IOP Section</h2> <blockquote>
 * "@AIGenerated declares that the behavior was generated from intent, is not
 * hand-written, and must never be edited manually." </blockquote>
 *
 * <h2>Purpose</h2>
 * <p>
 * This annotation is a <b>provenance + trust boundary + registry entry</b>. It
 * answers real questions engineers have:
 * <ul>
 * <li>Can I edit this? → NO, it will be overwritten</li>
 * <li>Can I delete this? → YES, it will be regenerated</li>
 * <li>Can I regenerate this? → YES, if stability allows</li>
 * <li>Who owns this behavior? → The spec interface owns it</li>
 * <li>Is this authoritative or derived? → DERIVED from spec</li>
 * <li>What model generated this? → See model() attribute</li>
 * <li>How many times regenerated? → See generationCount()</li>
 * <li>What was the spec hash? → See specHash()</li>
 * </ul>
 *
 * <h2>Hard Rule</h2> <blockquote> "Any file marked @AIGenerated is disposable.
 * Any file not marked is authoritative." </blockquote>
 *
 * <h2>Full Example</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;AIGenerated(id = "550e8400-e29b-41d4-a716-446655440000", value = "Handles text ingestion, extracts intent and emotion.", source = IMemoryIngestor.class, specHash = "sha256:a1b2c3d4e5f6...", model = Model.ModelType.OPUS, stability = Stability.Level.STABLE, generatedAt = "2025-01-15T10:30:00Z", generationCount = 3, testPassed = true, tokensUsed = 2847, durationMs = 4521, constraints = {
 * 			"Must be idempotent", "Must not block", "Must support cancellation"})
 * 	final class MemoryIngestorImpl implements IMemoryIngestor {
 *
 * 		&#64;Override
 * 		public void ingest(TextInput input) {
 * 			// deterministic generated code
 * 		}
 * 	}
 * }
 * </pre>
 *
 * <h2>What This Annotation Does NOT Do</h2>
 * <ul>
 * <li>Execute AI</li>
 * <li>Invoke models</li>
 * <li>Change runtime behavior</li>
 * <li>Affect proxies or IoC</li>
 * </ul>
 *
 * <p>
 * It is documentation + governance + registry, nothing more.
 *
 * @since Grand Java Patterns Proof - IOP Section
 * @see AIGenClass
 * @see AIGenMethod
 * @see SpecHash
 * @see Model
 * @see Stability
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/AIGenerated.java}
 * blob {@code d48ee846d8599a969416f5e27fbf1576555de644} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIGenerated {

    // --- IDENTITY ---

    /**
     * Unique ID for this generation (UUID). Used as primary key in the registry.
     *
     * @return generation UUID
     */
    String id() default "";

    /**
     * Natural language description of the generated behavior. This is documentation
     * + audit trail, not executable logic.
     *
     * @return behavior description
     */
    String value();

    /**
     * Source interface that defined the intent. This is the "contract" that this
     * implementation fulfills.
     *
     * @return source interface class
     */
    Class<?> source() default Void.class;

    // --- SPEC TRACKING ---

    /**
     * Hash of the specification that generated this code. Format:
     * "sha256:{64-char-hex}" Used for incremental builds.
     *
     * @return spec hash
     */
    String specHash() default "";

    /**
     * Hash of the previous specification (for diffing).
     *
     * @return previous spec hash
     */
    String prevSpecHash() default "";

    /**
     * Summary of changes from previous version.
     *
     * @return diff summary
     */
    String diffSummary() default "";

    // --- MODEL & GENERATION ---

    /**
     * Model that generated this code.
     *
     * @return model type
     */
    Model.ModelType model() default Model.ModelType.SONNET;

    /**
     * Generator identity (tooling provenance).
     *
     * @return generator name
     */
    String generator() default "iop-codegen";

    /**
     * Generation timestamp (ISO-8601 format).
     *
     * @return timestamp string
     */
    String generatedAt() default "";

    /**
     * Number of times this spec has been generated. Increments each time spec
     * changes and regeneration occurs.
     *
     * @return generation count
     */
    int generationCount() default 1;

    // --- STABILITY & QUALITY ---

    /**
     * Stability level of this generated implementation.
     *
     * @return stability level
     */
    Stability.Level stability() default Stability.Level.ALPHA;

    /**
     * Whether tests passed after generation.
     *
     * @return true if tests passed
     */
    boolean testPassed() default false;

    /**
     * Test class used to verify generation.
     *
     * @return test class (Void.class = no test)
     */
    Class<?> testClass() default Void.class;

    // --- COST TRACKING ---

    /**
     * Number of tokens used for generation. Used for cost tracking and
     * optimization.
     *
     * @return tokens used
     */
    int tokensUsed() default 0;

    /**
     * Generation duration in milliseconds. Used for performance tracking.
     *
     * @return duration in ms
     */
    int durationMs() default 0;

    /**
     * Number of retries before successful generation. 0 means generated on first
     * attempt.
     *
     * @return retry count
     */
    int retries() default 0;

    // --- CONSTRAINTS & INVARIANTS ---

    /**
     * Constraints that guided generation. Used for review, regeneration, and
     * diffing.
     *
     * @return array of constraints
     */
    String[] constraints() default {};

    /**
     * Invariants that were enforced in generation.
     *
     * @return array of invariants
     */
    String[] invariants() default {};

    // --- REGISTRY METADATA ---

    /**
     * Registry storage type used.
     *
     * @return registry type
     */
    RegistryType registry() default RegistryType.JSONL;

    /** Registry storage types for generation tracking. */
    enum RegistryType {
        /** JSON Lines file: .iop-registry/generations.jsonl */
        JSONL,

        /** SQLite database: .iop-registry/generations.db */
        SQLITE,

        /** No registry (not recommended for production) */
        NONE
    }

    // MEMBER_UNION (user decision 2026-11-10, "Union into AnOP owners"): members of the IOP twin
    /**
     * Whether tests passed after generation.
     *
     * @return true if tests passed
     */
    boolean testsPassed() default false;

    /**
     * Generation iteration count. Incremented each time the spec is
     * regenerated.
     *
     * @return generation count
     */
    int genCount() default 1;

    /**
     * Generator version.
     *
     * @return generator version
     */
    String generatorVersion() default "";

    /**
     * Generation timestamp. Format: ISO-8601 (e.g., "2026-01-15T10:30:00Z")
     *
     * @return timestamp
     */
    String timestamp();
}
