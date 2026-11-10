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
 * ISpec - The Complete IOP Specification for LLM Code Generation.
 *
 * <h2>THE NEW WAY LLMS CODE</h2>
 *
 * <pre>
 * +=========================================================================+
 * ║                                                                         ║
 * ║  "Code is the memory. Code is the contract. Code is the implementation. ║
 * ║   Code is the document. Code is the text. Code IS LLM memory."          ║
 * ║                                                                         ║
 * +=========================================================================+
 * </pre>
 *
 * <h2>IOP: Interface-Oriented Programming</h2>
 *
 * <pre>
 * +-------------------------------------------------------------------------+
 * |  THE FUNDAMENTAL PRINCIPLE                                              |
 * +-------------------------------------------------------------------------+
 * |                                                                         |
 * |  OLD (fragile): Human intent → conversation → AI memory → code          |
 * |  NEW (stable):  Human intent → code/contract → AI edits → compiler      |
 * |                                                                         |
 * |  In IOP:                                                                |
 * |    - The CODEBASE is the memory                                         |
 * |    - The COMPILER is the authority                                      |
 * |    - The AI is STATELESS                                                |
 * |    - The INTERFACE is the contract                                      |
 * |    - The JAVADOC is the prompt                                          |
 * |    - The ANNOTATION is the metadata                                     |
 * |    - The REGISTRY is the history                                        |
 * |                                                                         |
 * +-------------------------------------------------------------------------+
 * </pre>
 *
 * <h2>Two-Phase AOP Pipeline</h2>
 *
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  PHASE 1: READ (Annotation Processing)                                  │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │  1. Scan for @ISpec interfaces                                          │
 * │  2. Parse interface + JavaDoc + annotations                             │
 * │  3. Compute spec hash: SHA-256(interface + javadoc + constraints)       │
 * │  4. Query registry for existing implementation                          │
 * │  5. Compare spec hash with stored hash                                  │
 * │  6. Check stability level - can we regenerate?                          │
 * │  7. Decision: SKIP (unchanged) or PROCEED (changed/new)                 │
 * └─────────────────────────────────────────────────────────────────────────┘
 *                                    │
 *                                    ▼
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  PHASE 2: GENERATE (Code Generation)                                    │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │  1. Select model: OPUS → SONNET → HAIKU → OLLAMA                        │
 * │  2. Compose prompt from:                                                │
 * │     - Interface signature                                               │
 * │     - JavaDoc content                                                   │
 * │     - @ISpec.value() description                                        │
 * │     - @ISpec.constraints() boundaries                                   │
 * │     - @ISpec.invariants() assertions                                    │
 * │     - @ISpec.examples() input/output pairs                              │
 * │  3. Generate implementation                                             │
 * │  4. If test() specified → run tests                                     │
 * │     - PASS → accept implementation                                      │
 * │     - FAIL → feed error to model, retry (max retries)                   │
 * │  5. Mark with @AIGenerated metadata                                     │
 * │  6. Update registry with generation record                              │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Registry Tracking</h2>
 *
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  REGISTRY: Complete History of All Generations                          │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │                                                                         │
 * │  JSONL Format (.iop-registry/generations.jsonl):                        │
 * │  ─────────────────────────────────────────────────                      │
 * │  {"id":"uuid","spec":"IUserService","hash":"sha256:...","model":"OPUS", │
 * │   "impl":"UserServiceImpl","at":"2025-01-15T10:30:00Z","tokens":2847,   │
 * │   "ms":4521,"test":true,"stability":"STABLE","gen":3}                   │
 * │                                                                         │
 * │  SQLite Schema (.iop-registry/generations.db):                          │
 * │  ─────────────────────────────────────────────                          │
 * │  CREATE TABLE generations (                                             │
 * │    id            TEXT PRIMARY KEY,      -- UUID                         │
 * │    spec_hash     TEXT NOT NULL,         -- SHA-256 of spec              │
 * │    spec_name     TEXT NOT NULL,         -- Interface FQCN               │
 * │    impl_name     TEXT NOT NULL,         -- Implementation FQCN          │
 * │    model         TEXT NOT NULL,         -- OPUS/SONNET/HAIKU/OLLAMA     │
 * │    stability     TEXT NOT NULL,         -- Stability level              │
 * │    generated_at  TEXT NOT NULL,         -- ISO-8601 timestamp           │
 * │    gen_count     INTEGER DEFAULT 1,     -- Generation count             │
 * │    test_passed   BOOLEAN,               -- Test result                  │
 * │    tokens_used   INTEGER,               -- Token cost                   │
 * │    duration_ms   INTEGER,               -- Generation time              │
 * │    retries       INTEGER DEFAULT 0,     -- Retry count                  │
 * │    prev_hash     TEXT,                  -- Previous spec hash           │
 * │    diff_summary  TEXT,                  -- Change summary               │
 * │    constraints   TEXT,                  -- JSON array                   │
 * │    invariants    TEXT                   -- JSON array                   │
 * │  );                                                                     │
 * │  CREATE INDEX idx_spec ON generations(spec_name);                       │
 * │  CREATE INDEX idx_hash ON generations(spec_hash);                       │
 * │                                                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * // Complete IOP specification
 * &#64;ISpec(
 *     value = """
 *         User authentication service.
 *         Validates credentials against secure store.
 *         Rate-limits failed attempts.
 *         Emits security events.
 *         """,
 *     model = Model.ModelType.OPUS,           // Security = OPUS
 *     stability = Stability.Level.STABLE,     // Production-ready
 *     test = IAuthServiceTest.class,          // Verify against tests
 *     registry = ISpec.RegistryType.SQLITE,   // Track in SQLite
 *     constraints = {
 *         "Must hash passwords with bcrypt",
 *         "Must rate-limit to 5 attempts per minute",
 *         "Must log all authentication events"
 *     },
 *     invariants = {
 *         "No plaintext passwords in logs",
 *         "No credentials in exceptions"
 *     },
 *     examples = {
 *         "valid user + correct password -> AuthResult.SUCCESS",
 *         "valid user + wrong password -> AuthResult.INVALID_CREDENTIALS",
 *         "invalid user -> AuthResult.USER_NOT_FOUND"
 *     }
 * )
 * public interface IAuthService<Self extends IAuthService<Self>> extends IContext<Self> {
 *
 *     /**
 *      * Authenticate user with credentials.
 *      *
 *      * @param username the username (never null)
 *      * @param password the password (never null, not stored)
 *      * @return authentication result (never null)
 *      * /
 *     AuthResult authenticate(String username, String password);
 * }
 * }</pre>
 *
 * <h2>Model Selection Matrix</h2>
 *
 * <pre>
 * | Task Complexity        | Model   | Cost | Latency | Quality |
 * |------------------------|---------|------|---------|---------|
 * | Security-critical      | OPUS    | $$$  | High    | Highest |
 * | Architecture decisions | OPUS    | $$$  | High    | Highest |
 * | Complex algorithms     | OPUS    | $$$  | High    | Highest |
 * | General implementation | SONNET  | $$   | Medium  | High    |
 * | Refactoring            | SONNET  | $$   | Medium  | High    |
 * | Boilerplate/scaffolds  | HAIKU   | $    | Low     | Good    |
 * | Simple transforms      | HAIKU   | $    | Low     | Good    |
 * | Bulk processing        | OLLAMA  | Free | Medium  | Varies  |
 * | Local-only tasks       | OLLAMA  | Free | Low     | Varies  |
 * </pre>
 *
 * @since ContextOS 1.0 - IOP Complete Specification
 * @see AIGenClass
 * @see AIGenMethod
 * @see AIGenerated
 * @see SpecHash
 * @see Model
 * @see Stability
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/ISpec.java}
 * blob {@code cf751f569a07549c86ae7150d47f1e873e50b259} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP LazyInit batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface ISpec {

    // --- SPECIFICATION ---

    /**
     * Natural language description of the interface behavior. This is the primary
     * prompt for LLM code generation.
     *
     * @return behavior description
     */
    String value();

    /**
     * Constraints that guide generation (soft requirements). These influence the
     * generated code but are not enforced.
     *
     * @return constraint descriptions
     */
    String[] constraints() default {};

    /**
     * Invariants that MUST hold (hard requirements). These become assertions and
     * are verified at test time.
     *
     * @return invariant expressions
     */
    String[] invariants() default {};

    /**
     * Example input/output pairs for the LLM to learn from. Format: "input ->
     * expected_output"
     *
     * @return example pairs
     */
    String[] examples() default {};

    // --- MODEL SELECTION ---

    /**
     * Preferred model for generation.
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
     * Maximum tokens for generation. 0 means use model default.
     *
     * @return max tokens
     */
    int maxTokens() default 0;

    /**
     * Temperature for generation (0.0-1.0). -1 means use model default.
     *
     * @return temperature
     */
    double temperature() default -1.0;

    // --- STABILITY & QUALITY ---

    /**
     * Target stability level for generated implementation.
     *
     * @return stability level
     */
    Stability.Level stability() default Stability.Level.ALPHA;

    /**
     * Test class to verify generated implementation.
     *
     * @return test class (Void.class = no test)
     */
    Class<?> test() default Void.class;

    /**
     * Maximum generation attempts before failing.
     *
     * @return max retries (default: 3)
     */
    int maxRetries() default 3;

    // --- REGISTRY TRACKING ---

    /**
     * Registry storage type for tracking generations.
     *
     * @return registry type
     */
    RegistryType registry() default RegistryType.JSONL;

    /**
     * Custom registry path (relative to project root). Empty string uses default:
     * .iop-registry/
     *
     * @return registry path
     */
    String registryPath() default "";

    // --- LAZY LOADING & DEFERRED COMPUTATION (IOP Pattern) ---

    /**
     * LazyInit hash strategy for spec change detection.
     *
     * <p>
     * Two-Level LazyInit Hash (default):
     * </p>
     * <ol>
     * <li>xxHash64 (8 bytes, ~3 GB/s) - always computed</li>
     * <li>SHA-256 (32 bytes, ~500 MB/s) - only if xxHash64 matches</li>
     * </ol>
     *
     * <p>
     * Cost Savings: 70-90% of SHA-256 computations avoided.
     * </p>
     *
     * @return lazy hash strategy
     */
    LazyHashStrategy lazyHash() default LazyHashStrategy.TWO_LEVEL;

    /**
     * LazyInit loading of implementation components.
     *
     * <p>
     * When enabled:
     * </p>
     * <ul>
     * <li>Implementation class loaded on first use</li>
     * <li>Dependencies resolved lazily</li>
     * <li>Reduces startup time</li>
     * </ul>
     *
     * @return true to enable lazy loading
     */
    boolean lazyLoad() default true;

    /**
     * LazyInit initialization of expensive resources.
     *
     * <p>
     * Resources are created on first access, not at construction.
     * </p>
     * <ul>
     * <li>Database connections</li>
     * <li>HTTP clients</li>
     * <li>Cache instances</li>
     * </ul>
     *
     * @return true to defer resource initialization
     */
    boolean deferResources() default true;

    // --- CODE GENERATION OPTIONS ---

    /**
     * Package for generated implementation. Empty string means same package +
     * ".impl".
     *
     * @return target package
     */
    String targetPackage() default "";

    /**
     * Suffix for generated class name.
     *
     * @return class suffix (default: "Impl")
     */
    String classSuffix() default "Impl";

    /**
     * Whether to generate JavaDoc in implementation.
     *
     * @return true to generate JavaDoc
     */
    boolean generateJavadoc() default true;

    /**
     * Whether to generate null checks for parameters.
     *
     * @return true to generate null checks
     */
    boolean nullChecks() default true;

    /**
     * Whether to generate logging statements.
     *
     * @return true to generate logging
     */
    boolean generateLogging() default false;

    /**
     * Maximum cyclomatic complexity for generated methods. 0 means no limit.
     *
     * @return max complexity (default: 10)
     */
    int maxComplexity() default 10;

    // --- REGISTRY TYPES ---

    /** Registry storage types for generation tracking. */
    enum RegistryType {
        /**
         * JSON Lines file: .iop-registry/generations.jsonl Git-friendly, append-only,
         * human-readable.
         */
        JSONL,

        /**
         * SQLite database: .iop-registry/generations.db Queryable, indexed, supports
         * analytics.
         */
        SQLITE,

        /** Both JSONL and SQLite. JSONL for git, SQLite for queries. */
        BOTH,

        /** No registry (not recommended). No history, no incremental builds. */
        NONE
    }

    // --- LAZY HASH STRATEGIES ---

    /**
     * LazyInit hash computation strategies for spec change detection.
     *
     * <h2>IOP Principle: "Defer unnecessary computation"</h2>
     *
     * <pre>
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │  LAZY HASH STRATEGY                                                      │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │                                                                         │
     * │  TWO_LEVEL (Recommended):                                               │
     * │    1. Compute xxHash64 (8 bytes, ~3 GB/s)                               │
     * │    2. If xxHash64 matches → compute SHA-256 (confirmation)              │
     * │    3. If xxHash64 differs → SKIP SHA-256 (100% saved)                   │
     * │                                                                         │
     * │  Cost Analysis:                                                         │
     * │    • Different content: 0 SHA-256 calls                                 │
     * │    • Same content: 1 SHA-256 call (confirmation)                        │
     * │    • Typical savings: 70-90% of SHA-256 avoided                         │
     * │                                                                         │
     * │  QUICK_ONLY:                                                            │
     * │    Use xxHash64 only (fast, slight collision risk)                      │
     * │    Best for: high-volume, low-criticality                               │
     * │                                                                         │
     * │  FULL_ONLY:                                                             │
     * │    Use SHA-256 always (slow, cryptographic guarantee)                   │
     * │    Best for: security-critical, low-volume                              │
     * │                                                                         │
     * └─────────────────────────────────────────────────────────────────────────┘
     * </pre>
     */
    enum LazyHashStrategy {
        /**
         * Two-level lazy hash: xxHash64 first, SHA-256 only if needed. Default
         * strategy. Best balance of speed and accuracy.
         *
         * <p>
         * Flow:
         * </p>
         * <ol>
         * <li>Compute xxHash64 (always, fast)</li>
         * <li>Compare quick hashes</li>
         * <li>If quick differs → content changed (no SHA-256)</li>
         * <li>If quick matches → compute SHA-256 for confirmation</li>
         * </ol>
         */
        TWO_LEVEL,

        /**
         * Quick hash only: xxHash64 with no SHA-256 confirmation. Fastest but has
         * slight collision risk (~1 in 2^64).
         *
         * <p>
         * Use when:
         * </p>
         * <ul>
         * <li>Processing high volumes</li>
         * <li>Collision is acceptable (non-critical)</li>
         * <li>Speed is paramount</li>
         * </ul>
         */
        QUICK_ONLY,

        /**
         * Full hash only: SHA-256 always computed. Slowest but cryptographically
         * secure.
         *
         * <p>
         * Use when:
         * </p>
         * <ul>
         * <li>Security is critical</li>
         * <li>Collision must be impossible</li>
         * <li>Volume is low</li>
         * </ul>
         */
        FULL_ONLY,

        /**
         * Adaptive: switches strategy based on content size.
         * <ul>
         * <li>Small content (&lt;1KB): FULL_ONLY</li>
         * <li>Medium content (1KB-1MB): TWO_LEVEL</li>
         * <li>Large content (>1MB): QUICK_ONLY with sampling</li>
         * </ul>
         */
        ADAPTIVE
    }
}
