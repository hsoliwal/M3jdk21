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
 * IToolSpec - Tool/Function specification for AI agents.
 *
 * <h2>Tool Patterns</h2>
 *
 * <pre>
 * Tool patterns for LLM agents:
 *   - Function calling (OpenAI/Anthropic)
 *   - MCP (Model Context Protocol)
 *   - LangChain tools
 *   - Custom tool interfaces
 *
 * &#64;IToolSpec defines tools that AI agents can use.
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * &#64;IToolSpec(
 *     name = "search_web",
 *     description = "Search the web for information",
 *     parameters = {
 *         &#64;IToolSpec.Param(name = "query", type = "string", required = true),
 * &#64;IToolSpec.Param(name = "maxResults", type = "integer", defaultValue = "10")})
 * public interface IWebSearchTool&lt;Self extends IWebSearchTool&lt;Self>> extends IContext&lt;Self> {
 *
 * 	List&lt;SearchResult> search(String query, int maxResults);
 * }
 *
 * // Method-level tool
 * interface IFileTools {
 *
 * 	&#64;IToolSpec(name = "read_file", description = "Read contents of a file")
 * 	String readFile(@IToolSpec.Param(description = "File path") String path);
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Tool Specification
 * @see IAgentSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares tool identity, parameters, result type, execution, confirmation and protocol hints; it registers no tool and performs no action or approval.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.TYPE, ElementType.METHOD}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IToolSpec.java}
 * blob {@code 58158c620b7b853ae217296d9d952cda5ca195d4} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IToolSpec {

    // --- IDENTITY ---

    /**
     * Tool identifier.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IToolSpec#name intent: Tool identifier. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
     */
    String name();

    /**
     * Description of the proposed tool behavior.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IToolSpec#description intent: Description of the proposed tool behavior. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
     */
    String description();

    // --- PARAMETERS ---

    /**
     * Parameter schemas in supplied order.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IToolSpec#parameters intent: Parameter schemas in supplied order. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Param[]}; default expression {@code {}}.
     */
    Param[] parameters() default {};

    /**
     * Declares a parameter name, description, type label, requiredness, default and permitted-value hints; it validates no input.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares a parameter name, description, type label, requiredness, default and permitted-value hints; it validates no input.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.PARAMETER}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.SOURCE)
    @interface Param {

        /**
         * Tool parameter identifier.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IToolSpec.Param#name intent: Tool parameter identifier. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String name() default "";

        /**
         * Human-readable parameter purpose.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IToolSpec.Param#description intent: Human-readable parameter purpose. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String description() default "";

        /**
         * Parameter type label, defaulting to string; this schema does not parse a type language.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IToolSpec.Param#type intent: Parameter type label, defaulting to string; this schema does not parse a type language. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "string"}.
         */
        String type() default "string";

        /**
         * Whether a supplied parameter value is requested.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IToolSpec.Param#required intent: Whether a supplied parameter value is requested. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
         */
        boolean required() default true;

        /**
         * Default value encoded as a string.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IToolSpec.Param#defaultValue intent: Default value encoded as a string. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String defaultValue() default "";

        /**
         * Permitted-value hints; the annotation performs no membership check.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IToolSpec.Param#enumValues intent: Permitted-value hints; the annotation performs no membership check. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
         */
        String[] enumValues() default {};
    }

    // --- RETURN ---

    /**
     * Description of the expected tool result.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IToolSpec#returnDescription intent: Description of the expected tool result. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String returnDescription() default "";

    /**
     * Expected result type; Void.class remains the unspecified sentinel.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IToolSpec#returnType intent: Expected result type; Void.class remains the unspecified sentinel. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> returnType() default Void.class;

    // --- EXECUTION ---

    /**
     * Whether asynchronous tool execution is requested.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IToolSpec#async intent: Whether asynchronous tool execution is requested. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean async() default false;

    /**
     * Requested execution timeout in milliseconds.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IToolSpec#timeoutMs intent: Requested execution timeout in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 30000}.
     */
    int timeoutMs() default 30000;

    /**
     * Requested confirmation policy hint; false grants no permission and true performs no approval flow.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IToolSpec#requiresConfirmation intent: Requested confirmation policy hint; false grants no permission and true performs no approval flow. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean requiresConfirmation() default false;

    // --- PROTOCOL ---

    /**
     * Requested tool-protocol category.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IToolSpec#protocol intent: Requested tool-protocol category. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Protocol}; default expression {@code Protocol.FUNCTION_CALLING}.
     */
    Protocol protocol() default Protocol.FUNCTION_CALLING;

    /**
     * Tool-protocol categories; tokens supply schema values without execution or authorization.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IToolSpec.Protocol choices described as Tool-protocol categories; tokens supply schema values without execution or authorization. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum Protocol {
        /**
         * Requested function calling category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IToolSpec.Protocol#FUNCTION_CALLING policy choice: Requested function calling category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IToolSpec.Protocol; its declared order and identity are preserved.
         */
        FUNCTION_CALLING,
        /**
         * Requested Model Context Protocol category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IToolSpec.Protocol#MCP policy choice: Requested Model Context Protocol category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IToolSpec.Protocol; its declared order and identity are preserved.
         */
        MCP,
        /**
         * Requested LangChain tool integration category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IToolSpec.Protocol#LANGCHAIN policy choice: Requested LangChain tool integration category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IToolSpec.Protocol; its declared order and identity are preserved.
         */
        LANGCHAIN,
        /**
         * Requested custom integration category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IToolSpec.Protocol#CUSTOM policy choice: Requested custom integration category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IToolSpec.Protocol; its declared order and identity are preserved.
         */
        CUSTOM
    }

    // --- CATEGORY ---

    /**
     * Tool capability classification; classification grants no access.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IToolSpec#category intent: Tool capability classification; classification grants no access. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Category}; default expression {@code Category.GENERAL}.
     */
    Category category() default Category.GENERAL;

    /**
     * Tool capability categories; tokens supply schema values without execution or authorization.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IToolSpec.Category choices described as Tool capability categories; tokens supply schema values without execution or authorization. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 7 constants in the existing source order.
     */
    enum Category {
        /**
         * Requested general-purpose tools category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IToolSpec.Category#GENERAL policy choice: Requested general-purpose tools category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IToolSpec.Category; its declared order and identity are preserved.
         */
        GENERAL,
        /**
         * Requested filesystem tools category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IToolSpec.Category#FILE_SYSTEM policy choice: Requested filesystem tools category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IToolSpec.Category; its declared order and identity are preserved.
         */
        FILE_SYSTEM,
        /**
         * Requested web tools category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IToolSpec.Category#WEB policy choice: Requested web tools category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IToolSpec.Category; its declared order and identity are preserved.
         */
        WEB,
        /**
         * Requested database tools category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IToolSpec.Category#DATABASE policy choice: Requested database tools category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IToolSpec.Category; its declared order and identity are preserved.
         */
        DATABASE,
        /**
         * Requested code tools category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IToolSpec.Category#CODE policy choice: Requested code tools category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IToolSpec.Category; its declared order and identity are preserved.
         */
        CODE,
        /**
         * Requested communication tools category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IToolSpec.Category#COMMUNICATION policy choice: Requested communication tools category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IToolSpec.Category; its declared order and identity are preserved.
         */
        COMMUNICATION,
        /**
         * Requested system tools category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IToolSpec.Category#SYSTEM policy choice: Requested system tools category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IToolSpec.Category; its declared order and identity are preserved.
         */
        SYSTEM
    }

    // --- DESCRIPTION ---

    /**
     * Natural-language tool specification.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IToolSpec#value intent: Natural-language tool specification. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    /**
     * Preferred generation-model token.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IToolSpec#model intent: Preferred generation-model token. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.HAIKU}.
     */
    Model.ModelType model() default Model.ModelType.HAIKU;
}
