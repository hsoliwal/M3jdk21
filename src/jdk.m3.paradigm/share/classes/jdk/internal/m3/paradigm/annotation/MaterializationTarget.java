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

import java.util.EnumMap;

/**
 * IOP v1 Materialization Targets - Where generated artifacts land.
 *
 * <h2>IOP Materialization Model</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  IOP v1: MATERIALIZATION TARGETS                                          ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  "AI output must materialize to a declared target.                        ║
 * ║   No invisible side effects. No hidden artifacts."                        ║
 * ║                                                                           ║
 * ║  Targets:                                                                 ║
 * ║    SOURCE   - .java files in src/ directories                             ║
 * ║    BYTECODE - .class files (compile-time weaving)                         ║
 * ║    MODULE   - module-info.java, exports/requires                          ║
 * ║    RESOURCE - src/main/resources/ files                                   ║
 * ║    TEST     - src/test/java/ test files                                   ║
 * ║    CONFIG   - application.yml, pom.xml, etc.                              ║
 * ║    DOCS     - JavaDoc, markdown, diagrams                                 ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Why Explicit Targets?</h2>
 * <ul>
 * <li>Predictable file changes</li>
 * <li>Clear ownership boundaries</li>
 * <li>Git diff-friendly</li>
 * <li>Build system integration</li>
 * </ul>
 *
 * <h2>Target Combinations</h2>
 *
 * <pre>
 * Single target:
 *   &#64;AIMaterializesTo(SOURCE)          - Only source files
 *   &#64;AIMaterializesTo(BYTECODE)        - Only compiled classes
 *
 * Multiple targets:
 *   &#64;AIMaterializesTo({SOURCE, TEST})  - Source + tests
 *   &#64;AIMaterializesTo({SOURCE, DOCS})  - Source + documentation
 * </pre>
 *
 * @since ContextOS 1.0 - IOP v1
 * @see AIMaterializesTo
 * @see IOPOperation
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/MaterializationTarget.java}
 * blob {@code b1a5fba3113bd5eb743ecd9464b302a9693a3de1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum MaterializationTarget {

    // --- PRIMARY TARGETS ---

    /**
     * Source code files (.java).
     *
     * <p>
     * Generated into src/main/java/ or src/{module}/main/java/.
     * </p>
     *
     * <pre>
     * File pattern: src/ ** /*.java
     * Ownership: Developer + AI (co-owned)
     * Version control: Always committed
     * </pre>
     */
    SOURCE,

    /**
     * Compiled bytecode (.class files).
     *
     * <p>
     * Generated at compile-time through annotation processing or bytecode weaving
     * (ASM, ByteBuddy).
     * </p>
     *
     * <pre>
     * File pattern: target/classes/ ** /*.class
     * Ownership: Build system
     * Version control: Never committed (generated)
     * </pre>
     */
    BYTECODE,

    /**
     * Module descriptor (module-info.java).
     *
     * <p>
     * Modifies exports, requires, provides, uses declarations.
     * </p>
     *
     * <pre>
     * File pattern: src/ * /main/java/module-info.java
     * Ownership: Module system
     * Version control: Always committed
     * </pre>
     */
    MODULE,

    /**
     * Resource files (non-Java).
     *
     * <p>
     * Generated into src/main/resources/ or src/{module}/main/resources/.
     * </p>
     *
     * <pre>
     * File patterns:
     *   - *.properties
     *   - *.xml
     *   - *.json
     *   - *.yaml / *.yml
     *   - META-INF/services/*
     * Ownership: Application config
     * Version control: Usually committed
     * </pre>
     */
    RESOURCE,

    // --- SECONDARY TARGETS ---

    /**
     * Test source files.
     *
     * <p>
     * Generated into src/test/java/ or src/{module}/test/java/.
     * </p>
     *
     * <pre>
     * File pattern: src/ * /test/java/ ** /*Test.java
     * Ownership: Test framework
     * Version control: Always committed
     * </pre>
     */
    TEST,

    /**
     * Test resources.
     *
     * <p>
     * Generated into src/test/resources/.
     * </p>
     *
     * <pre>
     * File pattern: src/ * /test/resources/ ** /*
     * Ownership: Test framework
     * Version control: Usually committed
     * </pre>
     */
    TEST_RESOURCE,

    /**
     * Configuration files.
     *
     * <p>
     * Build configuration, application config.
     * </p>
     *
     * <pre>
     * File patterns:
     *   - pom.xml
     *   - build.gradle
     *   - application.yml
     *   - logback.xml
     * Ownership: Build/DevOps
     * Version control: Always committed
     * </pre>
     */
    CONFIG,

    /**
     * Documentation files.
     *
     * <p>
     * Generated documentation artifacts.
     * </p>
     *
     * <pre>
     * File patterns:
     *   - docs/ ** /*.md
     *   - JavaDoc (in source)
     *   - diagrams (*.puml, *.mmd)
     * Ownership: Documentation
     * Version control: Usually committed
     * </pre>
     */
    DOCS,

    // --- SPECIAL TARGETS ---

    /**
     * In-memory only (no file output).
     *
     * <p>
     * Used for analysis, validation, or preview without persistence.
     * </p>
     *
     * <pre>
     * Use case: Dry-run, validation, preview
     * Ownership: Transient
     * Version control: N/A
     * </pre>
     */
    MEMORY,

    /**
     * Standard output (console/log).
     *
     * <p>
     * Used for reporting, debugging, or interactive feedback.
     * </p>
     *
     * <pre>
     * Use case: CLI tools, debugging, reports
     * Ownership: User/Log system
     * Version control: N/A
     * </pre>
     */
    STDOUT,

    /**
     * Registry storage (.iop-registry/).
     *
     * <p>
     * Internal tracking files, not application code.
     * </p>
     *
     * <pre>
     * File patterns:
     *   - .iop-registry/generations.jsonl
     *   - .iop-registry/generations.db
     * Ownership: IOP framework
     * Version control: Optional (JSONL is git-friendly)
     * </pre>
     */
    REGISTRY;

    // --- ENUMMAP LOOKUP TABLES (Better performance than switch) ---

    /** Lookup table for base directory calculation. */
    private static final EnumMap<MaterializationTarget, java.util.function.Function<String, String>>
            BASE_DIRECTORY_FUNCTIONS;

    /** Lookup table for file pattern retrieval. */
    private static final EnumMap<MaterializationTarget, String> FILE_PATTERNS;

    static {
        // Initialize base directory lookup table
        BASE_DIRECTORY_FUNCTIONS = new EnumMap<>(MaterializationTarget.class);
        BASE_DIRECTORY_FUNCTIONS.put(
                SOURCE, moduleName -> (moduleName != null ? "src-" + moduleName + "/" : "src/") + "main/java");
        BASE_DIRECTORY_FUNCTIONS.put(BYTECODE, moduleName -> "target/classes");
        BASE_DIRECTORY_FUNCTIONS.put(
                MODULE, moduleName -> (moduleName != null ? "src-" + moduleName + "/" : "src/") + "main/java");
        BASE_DIRECTORY_FUNCTIONS.put(
                RESOURCE, moduleName -> (moduleName != null ? "src-" + moduleName + "/" : "src/") + "main/resources");
        BASE_DIRECTORY_FUNCTIONS.put(
                TEST, moduleName -> (moduleName != null ? "src-" + moduleName + "/" : "src/") + "test/java");
        BASE_DIRECTORY_FUNCTIONS.put(
                TEST_RESOURCE,
                moduleName -> (moduleName != null ? "src-" + moduleName + "/" : "src/") + "test/resources");
        BASE_DIRECTORY_FUNCTIONS.put(CONFIG, moduleName -> ".");
        BASE_DIRECTORY_FUNCTIONS.put(DOCS, moduleName -> "docs");
        BASE_DIRECTORY_FUNCTIONS.put(REGISTRY, moduleName -> ".iop-registry");
        BASE_DIRECTORY_FUNCTIONS.put(MEMORY, moduleName -> null);
        BASE_DIRECTORY_FUNCTIONS.put(STDOUT, moduleName -> null);

        // Initialize file pattern lookup table
        FILE_PATTERNS = new EnumMap<>(MaterializationTarget.class);
        FILE_PATTERNS.put(SOURCE, "*.java");
        FILE_PATTERNS.put(TEST, "*.java");
        FILE_PATTERNS.put(MODULE, "*.java");
        FILE_PATTERNS.put(BYTECODE, "*.class");
        FILE_PATTERNS.put(RESOURCE, "*");
        FILE_PATTERNS.put(TEST_RESOURCE, "*");
        FILE_PATTERNS.put(CONFIG, "*.{xml,yml,yaml,properties,json}");
        FILE_PATTERNS.put(DOCS, "*.{md,puml,mmd}");
        FILE_PATTERNS.put(REGISTRY, "*.{jsonl,db}");
        FILE_PATTERNS.put(MEMORY, null);
        FILE_PATTERNS.put(STDOUT, null);
    }

    // --- UTILITY METHODS ---

    /**
     * Check if this target produces files.
     *
     * @return true if target creates files
     */
    public boolean producesFiles() {
        return this != MEMORY && this != STDOUT;
    }

    /**
     * Check if this target should be version controlled.
     *
     * @return true if target should be committed to git
     */
    public boolean shouldVersionControl() {
        return this == SOURCE || this == MODULE || this == TEST || this == CONFIG || this == DOCS || this == RESOURCE;
    }

    /**
     * Check if this target is for source code.
     *
     * @return true if target is source code
     */
    public boolean isSourceCode() {
        return this == SOURCE || this == TEST;
    }

    /**
     * Check if this target is transient (not persisted).
     *
     * @return true if target is transient
     */
    public boolean isTransient() {
        return this == MEMORY || this == STDOUT;
    }

    /**
     * Get the base directory for this target.
     *
     * @param moduleName
     *            module name (null for root)
     * @return base directory path
     */
    public String getBaseDirectory(String moduleName) {
        // Use EnumMap lookup instead of switch - O(1) performance
        java.util.function.Function<String, String> function = BASE_DIRECTORY_FUNCTIONS.get(this);
        return function != null ? function.apply(moduleName) : null;
    }

    /**
     * Get file extension filter for this target.
     *
     * @return file extension pattern (e.g., "*.java")
     */
    public String getFilePattern() {
        // Use EnumMap lookup instead of switch - O(1) performance
        return FILE_PATTERNS.get(this);
    }
}
