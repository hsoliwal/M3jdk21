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
package jdk.internal.m3.collections;

import java.util.List;
import java.util.Map;
import java.util.Optional;
/**
 * ITool - Common interface for all tools in the AI Maven Plugin.
 *
 * <p>
 * Implements the CPU-First, LLM-Last architecture where tools are prioritized
 * by execution cost:
 * <ol>
 * <li>CPU tools (deterministic, fast, free)</li>
 * <li>Local LLM tools (Ollama - no cloud tokens)</li>
 * <li>Cloud LLM tools (Claude/GPT - last resort)</li>
 * </ol>
 *
 * <h2>Tool Categories</h2>
 *
 * <pre>
 * Category        Examples                      Priority
 * ─────────────────────────────────────────────────────────
 * UNIX            grep, sed, awk, find          1 (CPU)
 * AST             parse, refactor, analyze      1 (CPU)
 * NLP             tokenize, sentences, POS      1 (CPU)
 * SEARCH          lucene index, query           1 (CPU)
 * EXTRACT         tika, pdfbox, poi             1 (CPU)
 * QUALITY         checkstyle, pmd, spotbugs     1 (CPU)
 * LLM_LOCAL       ollama generate, explain      2 (Local)
 * LLM_CLOUD       claude, openai                3 (Cloud)
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * ITool tool = ToolRegistry.get("grep");
 * ToolResult result = tool.execute(Map.of("pattern", "TODO", "file", "src/Main.java"));
 * if (result.isSuccess()) {
 * 	log.info(result.getOutput());
 * }
 * }</pre>
 *
 * @since 1.1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/tools/ITool.java}
 * blob {@code b1c1854b170c5ebdc465af9607ba9f204a737627} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ITool {
    PlaceholderLogger log = PlaceholderLogger.of(ITool.class);

    // --- INTERNED STRING CONSTANTS ---
    TokenInterner INTERNER = TokenInterner.getInstance();
    String ERR_MISSING_PARAM = INTERNER.intern("Missing required parameter: ");
    String DEFAULT_VERSION = INTERNER.intern("1.0.0");

    // --- IDENTITY ---

    /**
     * Get the unique tool name.
     *
     * @return tool name (e.g., "grep", "sed", "ollama-generate")
     */
    String getName();

    /**
     * Get tool description.
     *
     * @return human-readable description
     */
    String getDescription();

    /**
     * Get tool category.
     *
     * @return category enum
     */
    Category getCategory();

    /**
     * Get tool aliases.
     *
     * @return list of alternative names
     */
    default List<String> getAliases() {
        return List.of();
    }

    /**
     * Get tool version.
     *
     * @return version string
     */
    default String getVersion() {
        return DEFAULT_VERSION;
    }

    // --- EXECUTION ---

    /**
     * Execute the tool with parameters.
     *
     * @param params
     *            input parameters
     * @return execution result
     */
    ToolResult execute(Map<String, Object> params);

    /**
     * Execute with string arguments (CLI-style).
     *
     * @param args
     *            command-line style arguments
     * @return execution result
     */
    default ToolResult execute(String... args) {
        return execute(Map.of("args", List.of(args)));
    }

    /**
     * Check if tool is available/ready.
     *
     * @return true if tool can execute
     */
    default boolean isAvailable() {
        return true;
    }

    // --- PARAMETERS ---

    /**
     * Get parameter definitions.
     *
     * @return list of parameter specs
     */
    default List<ParamSpec> getParameters() {
        return List.of();
    }

    /**
     * Validate parameters before execution.
     *
     * @param params
     *            input parameters
     * @return validation result (empty if valid)
     */
    default Optional<String> validate(Map<String, Object> params) {
        return getParameters().stream()
                .filter(spec -> spec.required() && !params.containsKey(spec.name()))
                .findFirst()
                .map(spec -> ERR_MISSING_PARAM + spec.name());
    }

    // --- NESTED TYPES ---

    /** Tool category for prioritization. */
    enum Category {

        /** CPU-based Unix tools (grep, sed, awk). */
        UNIX(1, "Unix Tools"),

        /** CPU-based AST tools (JavaParser, Spoon). */
        AST(1, "AST Analysis"),

        /** CPU-based NLP tools (OpenNLP). */
        NLP(1, "NLP Processing"),

        /** CPU-based search tools (Lucene). */
        SEARCH(1, "Full-Text Search"),

        /** CPU-based extraction tools (Tika). */
        EXTRACT(1, "Document Extraction"),

        /** CPU-based quality tools (Checkstyle, PMD). */
        QUALITY(1, "Code Quality"),

        /** Local LLM tools (Ollama). */
        LLM_LOCAL(2, "Local LLM"),

        /** Cloud LLM tools (Claude, OpenAI). */
        LLM_CLOUD(3, "Cloud LLM"),

        /** Flow orchestration tools (Camel). */
        FLOW(1, "Flow Orchestration"),

        /** Utility tools. */
        UTILITY(1, "Utilities");

        private final int priority;
        private final String displayName;

        Category(int priority, String displayName) {
            this.priority = priority;
            this.displayName = displayName;
        }

        public int getPriority() {
            return priority;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isCpuFirst() {
            return priority == 1;
        }
    }

    /**
     * Parameter specification.
     *
     * @param name
     *            parameter name
     * @param description
     *            parameter description
     * @param type
     *            parameter type
     * @param required
     *            whether required
     * @param defaultValue
     *            default value (null if none)
     */
    record ParamSpec(String name, String description, Class<?> type, boolean required, Object defaultValue) {

        public static ParamSpec required(String name, String desc, Class<?> type) {
            return new ParamSpec(name, desc, type, true, null);
        }

        public static ParamSpec optional(String name, String desc, Class<?> type, Object def) {
            return new ParamSpec(name, desc, type, false, def);
        }
    }

    /**
     * Tool execution result.
     *
     * @param success
     *            whether execution succeeded
     * @param output
     *            output content
     * @param error
     *            error message (if failed)
     * @param exitCode
     *            exit code (0 = success)
     * @param metadata
     *            additional metadata
     */
    record ToolResult(boolean success, String output, String error, int exitCode, Map<String, Object> metadata) {

        public static ToolResult success(String output) {
            return new ToolResult(true, output, null, 0, Map.of());
        }

        public static ToolResult success(String output, Map<String, Object> meta) {
            return new ToolResult(true, output, null, 0, meta);
        }

        public static ToolResult failure(String error) {
            return new ToolResult(false, null, error, 1, Map.of());
        }

        public static ToolResult failure(String error, int exitCode) {
            return new ToolResult(false, null, error, exitCode, Map.of());
        }

        public List<String> lines() {
            return output == null ? List.of() : List.of(output.split("\n"));
        }
    }
}
