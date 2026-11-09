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
 * AIGenClass - Marks interface/class for AI-generated implementation.
 *
 * <h2>IOP Two-Phase AOP Model</h2>
 *
 * <pre>
 * +-------------------------------------------------------------------------+
 * |  TWO-PHASE AOP: THE NEW WAY LLMS CODE                                   |
 * +-------------------------------------------------------------------------+
 * |                                                                         |
 * |  PHASE 1: READ (Annotation Processing)                                  |
 * |    ┌─────────────────────────────────────────────────────────────────┐  |
 * |    │ 1. Parse interface + JavaDoc + annotations                      │  |
 * |    │ 2. Compute spec hash: SHA-256(interface + javadoc + constraints)│  |
 * |    │ 3. Compare with @SpecHash on existing implementation            │  |
 * |    │ 4. Check @Stability level - can we regenerate?                  │  |
 * |    │ 5. If hash unchanged AND stability allows → SKIP                │  |
 * |    │ 6. If hash changed OR no implementation → PROCEED to Phase 2    │  |
 * |    └─────────────────────────────────────────────────────────────────┘  |
 * |                                                                         |
 * |  PHASE 2: GENERATE (Code Generation)                                    |
 * |    ┌─────────────────────────────────────────────────────────────────┐  |
 * |    │ 1. Select model from @AIGenClass.model() or @Model annotation   │  |
 * |    │ 2. Compose prompt from JavaDoc + value() + constraints()        │  |
 * |    │ 3. Generate implementation using selected model                 │  |
 * |    │ 4. Mark with @AIGenerated + @SpecHash + @Stability              │  |
 * |    │ 5. If test() specified → verify against test class              │  |
 * |    │ 6. If test fails → regenerate with error context (max 3 tries)  │  |
 * |    └─────────────────────────────────────────────────────────────────┘  |
 * |                                                                         |
 * +-------------------------------------------------------------------------+
 * </pre>
 *
 * <h2>From Grand Java Patterns Proof</h2> <blockquote> "In IOP, the program IS
 * the contract. Code becomes executable documentation. And AI no longer needs
 * long conversational memory." </blockquote>
 *
 * <h2>The Core Realization</h2>
 * <p>
 * When code is the instruction and code is the document, AI does not need
 * memory - it needs literacy.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     // Basic usage - model defaults to SONNET
 *     &#64;AIGenClass("""
 *             Ingest user text.
 *             Extract intent and emotion.
 *             Persist memory.
 *             Emit events.
 *             Must be deterministic and cancellable.
 *             """)
 *     interface IMemoryIngestor {
 *
 *         {@code @AIGenMethod}
 *         void ingest(TextInput input);
 *     }
 *
 *     // Full IOP specification with model, test, stability
 *     &#64;AIGenClass(value = """
 *             Validate and transform user input.
 *             Apply security filters.
 *             Rate limit by user ID.
 *             """, model = Model.ModelType.OPUS, // Security-critical = OPUS
 *             test = IInputValidatorTest.class, // Test class to verify
 *             stability = Stability.Level.STABLE, // Production-ready
 *             constraints = {"Must not throw on null input", "Must log all rejections",
 *                     "Must be thread-safe"}, invariants = {"output.length() &lt;= input.length()",
 *                             "no XSS vectors in output"})
 *     interface IInputValidator {
 *
 *         ValidationResult validate(UserInput input);
 *     }
 * }
 * </pre>
 *
 * <h2>Three Things At Once</h2>
 * <ol>
 * <li><b>Human-readable spec</b> - Natural language in value()</li>
 * <li><b>Compile-time authority</b> - Spec hash tracks changes</li>
 * <li><b>AI-local context</b> - No memory needed, just literacy</li>
 * </ol>
 *
 * <h2>Test-Driven Generation</h2>
 *
 * <pre>
 * When test() is specified:
 *   1. Generate implementation
 *   2. Compile generated code
 *   3. Run test class against implementation
 *   4. If tests PASS → accept implementation
 *   5. If tests FAIL → feed error to model, retry (max 3)
 *   6. If all retries fail → mark as NEEDS_MANUAL_REVIEW
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP IOP
 * @see AIGenMethod
 * @see AIGenerated
 * @see SpecHash
 * @see Model
 * @see Stability
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/AIGenClass.java}
 * blob {@code 6b3c89d24e35cf12cbdeb1502d74cbd88f1ed9ce} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIGenClass {

    /**
     * Optional supplementary behavior intent. Declaration JavaDoc is the primary
     * instruction source; leave this empty to avoid duplicating the contract.
     * The processor rejects a declaration with neither JavaDoc nor explicit intent.
     *
     * @return behavior description
     */
    String value() default "";

    /**
     * Preferred model for generating implementation. Overrides any @Model
     * annotation on the type.
     *
     * @return model type (default: SONNET)
     */
    Model.ModelType model() default Model.ModelType.SONNET;

    /**
     * Fallback model if preferred is unavailable.
     *
     * @return fallback model type
     */
    Model.ModelType fallback() default Model.ModelType.OLLAMA;

    /**
     * Test class to verify generated implementation. If specified, generation
     * enters test-driven mode.
     *
     * @return test class (Void.class means no test)
     */
    Class<?> test() default Void.class;

    /**
     * Target stability level for generated implementation.
     *
     * @return stability level (default: ALPHA)
     */
    Stability.Level stability() default Stability.Level.ALPHA;

    /**
     * Constraints that guide generation. These are soft requirements that influence
     * the generated code.
     *
     * @return constraint descriptions
     */
    String[] constraints() default {};

    /**
     * Invariants that MUST hold in the generated code. These are hard requirements
     * verified at compile/test time.
     *
     * @return invariant expressions
     */
    String[] invariants() default {};

    /**
     * Maximum generation attempts before failing.
     *
     * @return max retries (default: 3)
     */
    int maxRetries() default 3;

    /**
     * Whether to include JavaDoc in the generated implementation.
     *
     * @return true to generate JavaDoc (default: true)
     */
    boolean generateJavadoc() default true;

    /**
     * Package for generated implementation. Empty string means same package as
     * interface + ".impl".
     *
     * @return target package
     */
    String targetPackage() default "";

    /**
     * Suffix for generated class name. E.g., "IUserService" → "UserServiceImpl" if
     * suffix is "Impl".
     *
     * @return class name suffix (default: "Impl")
     */
    String classSuffix() default "Impl";

    // --- REGISTRY TRACKING - Internal Pointer to History ---

    /**
     * Registry storage type for tracking this class's generation history. Each
     * implementation gets its own tracked history.
     *
     * @return registry type
     */
    RegistryType registry() default RegistryType.JSONL;

    /**
     * Custom registry path (relative to project root). Empty string uses default:
     * .iop-registry/
     *
     * <pre>
     * File structure:
     *   .iop-registry/
     *     ├── generations.jsonl      # All generations (append-only)
     *     ├── generations.db         # SQLite index (queryable)
     *     └── history/
     *         ├── IUserService.jsonl # Per-interface history
     *         └── IAuthService.jsonl # Per-interface history
     * </pre>
     *
     * @return registry path
     */
    String registryPath() default "";

    /**
     * Whether to track per-method history in addition to class history. If true,
     * each method gets its own history entry.
     *
     * @return true to track method-level history
     */
    boolean trackMethods() default true;

    /**
     * Maximum history entries to keep per interface. 0 means unlimited. Older
     * entries are archived, not deleted.
     *
     * @return max history size
     */
    int maxHistorySize() default 100;

    /**
     * Registry storage types for generation tracking.
     *
     * <pre>
     * IOP reads the method → finds internal pointer → loads history
     * History shows: all versions, models used, test results, costs
     * </pre>
     */
    enum RegistryType {
        /**
         * JSON Lines file: .iop-registry/generations.jsonl
         *
         * <pre>
         * One line per generation:
         * {"id":"uuid","spec":"IUserService","hash":"sha256:...","at":"2025-01-15T10:30:00Z",...}
         * </pre>
         *
         * Git-friendly, append-only, human-readable.
         */
        JSONL,

        /**
         * SQLite database: .iop-registry/generations.db
         *
         * <pre>
         * CREATE TABLE generations (
         *   id TEXT PRIMARY KEY,
         *   spec_hash TEXT NOT NULL,
         *   spec_name TEXT NOT NULL,
         *   ...
         * );
         * </pre>
         *
         * Queryable, indexed, supports analytics.
         */
        SQLITE,

        /** Both JSONL and SQLite. JSONL for git history, SQLite for queries/analytics. */
        BOTH,

        /**
         * No registry (not recommended for production). No history tracking, no
         * incremental builds.
         */
        NONE
    }
}
