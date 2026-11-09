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
import java.util.Objects;
import java.util.Optional;
/**
 * AbstractTool - Base implementation for ITool.
 *
 * <p>
 * Provides CPU-First, LLM-Last tool infrastructure with template method
 * pattern:
 * <ul>
 * <li>Tool identity and metadata</li>
 * <li>Execution with validation</li>
 * <li>Parameter handling</li>
 * <li>Result creation</li>
 * </ul>
 *
 * <h2>Template Method Pattern</h2>
 * <p>
 * Subclasses can override:
 * <ul>
 * <li>{@link #doExecute(Map)} - Core execution logic</li>
 * <li>{@link #doValidate(Map)} - Custom validation</li>
 * <li>{@link #doCheckAvailability()} - Availability checking</li>
 * <li>{@link #onExecutionStarted(Map)} - Callback when execution starts</li>
 * <li>{@link #onExecutionCompleted(ToolResult)} - Callback when execution
 * completes</li>
 * </ul>
 *
 * @since CogniX 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/tools/AbstractTool.java}
 * blob {@code 7b8c2fe4a8f1a8731e3e4861a6a366db57044362} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractTool implements ITool {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractTool.class);


    // --- INTERNED STRING CONSTANTS ---
    private static final TokenInterner INTERNER = TokenInterner.getInstance();
    private static final String ERR_NAME_NULL = INTERNER.intern("name cannot be null");
    private static final String ERR_PARAMS_NULL = INTERNER.intern("params cannot be null");
    private static final String ERR_MISSING_PARAM = INTERNER.intern("Missing required parameter: ");
    private static final String DEFAULT_VERSION = INTERNER.intern("1.0.0");
    private static final String FMT_TOSTRING = INTERNER.intern("Tool{name='%s', category=%s, version='%s'}");

    private final String name;
    private final String description;
    private final Category category;
    private final List<String> aliases;
    private final String version;
    private final List<ParamSpec> parameters;

    /**
     * Create tool with full configuration.
     *
     * @param name
     *            tool name
     * @param description
     *            tool description
     * @param category
     *            tool category
     * @param aliases
     *            alternative names
     * @param version
     *            tool version
     * @param parameters
     *            parameter specifications
     */
    protected AbstractTool(
            String name,
            String description,
            Category category,
            List<String> aliases,
            String version,
            List<ParamSpec> parameters) {
        this.name = Objects.requireNonNull(name, ERR_NAME_NULL);
        this.description = description != null ? description : "";
        this.category = category != null ? category : Category.UTILITY;
        this.aliases = aliases != null ? List.copyOf(aliases) : List.of();
        this.version = version != null ? version : DEFAULT_VERSION;
        this.parameters = parameters != null ? List.copyOf(parameters) : List.of();
    }

    /**
     * Create tool with minimal configuration.
     *
     * @param name
     *            tool name
     * @param description
     *            tool description
     * @param category
     *            tool category
     */
    protected AbstractTool(String name, String description, Category category) {
        this(name, description, category, List.of(), DEFAULT_VERSION, List.of());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public List<ParamSpec> getParameters() {
        return parameters;
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        Objects.requireNonNull(params, ERR_PARAMS_NULL);

        onExecutionStarted(params);

        try {
            // Validate parameters
            Optional<String> validationError = validate(params);
            if (validationError.isPresent()) {
                String errorMsg = validationError.orElseThrow();
                ToolResult failed = ToolResult.failure(errorMsg);
                onExecutionFailed(params, new IllegalArgumentException(errorMsg));
                return failed;
            }

            // Custom validation
            Optional<String> customError = doValidate(params);
            if (customError.isPresent()) {
                String customMsg = customError.orElseThrow();
                ToolResult failed = ToolResult.failure(customMsg);
                onExecutionFailed(params, new IllegalArgumentException(customMsg));
                return failed;
            }

            // Execute
            ToolResult result = doExecute(params);
            onExecutionCompleted(result);
            return result;
        } catch (Exception e) {
            ToolResult failed = ToolResult.failure(e.getMessage());
            onExecutionFailed(params, e);
            return failed;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            return doCheckAvailability();
        } catch (Exception e) {
            return false;
        }
    }

    // --- Template Methods (Override for Custom Behavior) ---

    /**
     * Execute the tool with validated parameters.
     *
     * @param params
     *            validated parameters
     * @return execution result
     */
    protected abstract ToolResult doExecute(Map<String, Object> params);

    /**
     * Custom parameter validation.
     *
     * @param params
     *            parameters to validate
     * @return error message if invalid, empty if valid
     */
    protected Optional<String> doValidate(Map<String, Object> params) {
        return Optional.empty(); // Default: no custom validation
    }

    /**
     * Check if tool is available.
     *
     * @return true if available
     */
    protected boolean doCheckAvailability() {
        return true; // Default: assume available
    }

    // --- Callbacks ---

    /**
     * Callback when execution starts.
     *
     * @param params
     *            execution parameters
     */
    protected void onExecutionStarted(Map<String, Object> params) {
        // Default: no-op
    }

    /**
     * Callback when execution completes successfully.
     *
     * @param result
     *            execution result
     */
    protected void onExecutionCompleted(ToolResult result) {
        // Default: no-op
    }

    /**
     * Callback when execution fails.
     *
     * @param params
     *            execution parameters
     * @param error
     *            the error
     */
    protected void onExecutionFailed(Map<String, Object> params, Exception error) {
        // Default: no-op
    }

    // --- Utility Methods ---

    /**
     * Get required string parameter.
     *
     * @param params
     *            parameter map
     * @param key
     *            parameter key
     * @return parameter value
     * @throws IllegalArgumentException
     *             if missing
     */
    protected String getRequiredString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException(ERR_MISSING_PARAM + key);
        }
        return value.toString();
    }

    /**
     * Get optional string parameter.
     *
     * @param params
     *            parameter map
     * @param key
     *            parameter key
     * @param defaultValue
     *            default value if missing
     * @return parameter value or default
     */
    protected String getOptionalString(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Get optional integer parameter.
     *
     * @param params
     *            parameter map
     * @param key
     *            parameter key
     * @param defaultValue
     *            default value if missing
     * @return parameter value or default
     */
    protected int getOptionalInt(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get optional boolean parameter.
     *
     * @param params
     *            parameter map
     * @param key
     *            parameter key
     * @param defaultValue
     *            default value if missing
     * @return parameter value or default
     */
    protected boolean getOptionalBoolean(Map<String, Object> params, String key, boolean defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Get list parameter.
     *
     * @param params
     *            parameter map
     * @param key
     *            parameter key
     * @return list value or empty list
     */
    @SuppressWarnings("unchecked")
    protected List<String> getListParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) return List.of();
        if (value instanceof List) return (List<String>) value;
        return List.of(value.toString());
    }

    /**
     * Create success result with output.
     *
     * @param output
     *            output content
     * @return success result
     */
    protected ToolResult success(String output) {
        return ToolResult.success(output);
    }

    /**
     * Create success result with output and metadata.
     *
     * @param output
     *            output content
     * @param metadata
     *            additional metadata
     * @return success result
     */
    protected ToolResult success(String output, Map<String, Object> metadata) {
        return ToolResult.success(output, metadata);
    }

    /**
     * Create failure result.
     *
     * @param error
     *            error message
     * @return failure result
     */
    protected ToolResult failure(String error) {
        return ToolResult.failure(error);
    }

    @Override
    public String toString() {
        return String.format(FMT_TOSTRING, name, category, version);
    }
}
