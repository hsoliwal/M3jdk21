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
 * IAgentSpec - AI Agent specification for AI code generation.
 *
 * <h2>THE AGENT SPEC</h2>
 *
 * <pre>
 * Agent patterns:
 *   - ReAct (Reasoning + Acting)
 *   - Plan-and-Execute
 *   - Tool-using agents
 *   - Multi-agent systems
 *   - Autonomous agents
 *
 * &#64;IAgentSpec defines AI agents in IOP style.
 * "Agents defining agents. The recursion is beautiful."
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * &#64;IAgentSpec(
 *     name = "CodeReviewer",
 *     description = "Reviews code and suggests improvements",tools ={
 * 		ReadFileTool.class, SearchTool.class, EditTool.class}, model = OPUS, maxIterations = 10)
 * public interface ICodeReviewerAgent&lt;Self extends ICodeReviewerAgent&lt;Self>> extends IContext&lt;Self> {
 *
 * 	&#64;IAgentSpec.Task(goal = "Review the pull request and provide feedback", outputFormat = MARKDOWN)
 * 	String reviewPullRequest(String prUrl);
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Agent Specification
 * @see IPromptSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares agent identity, model, tools, execution pattern, limits and memory preferences; it creates no agent and authorizes no tool call.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.TYPE}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAgentSpec.java}
 * blob {@code 74a549d8f88fc3efdd66dd4f81b52b951e225de2} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAgentSpec {

    // --- IDENTITY ---

    /**
     * Agent identifier.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAgentSpec#name intent: Agent identifier. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
     */
    String name();

    /**
     * Human-readable agent role description.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAgentSpec#description intent: Human-readable agent role description. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String description() default "";

    // --- MODEL ---

    /**
     * Requested generation-model token.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAgentSpec#model intent: Requested generation-model token. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.OPUS}.
     */
    Model.ModelType model() default Model.ModelType.OPUS;

    /**
     * Requested sampling temperature; no range check is performed.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAgentSpec#temperature intent: Requested sampling temperature; no range check is performed. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 0.3}.
     */
    double temperature() default 0.3;

    /**
     * Requested token budget.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAgentSpec#maxTokens intent: Requested token budget. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 4096}.
     */
    int maxTokens() default 4096;

    // --- TOOLS ---

    /**
     * Tool class references requested for the agent; these references grant no authority.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAgentSpec#tools intent: Tool class references requested for the agent; these references grant no authority. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>[]}; default expression {@code {}}.
     */
    Class<?>[] tools() default {};

    /**
     * Requested tool identifiers; no lookup or access grant occurs.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAgentSpec#toolNames intent: Requested tool identifiers; no lookup or access grant occurs. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] toolNames() default {};

    // --- BEHAVIOR ---

    /**
     * Requested agent execution-pattern category.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAgentSpec#type intent: Requested agent execution-pattern category. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code AgentType}; default expression {@code AgentType.REACT}.
     */
    AgentType type() default AgentType.REACT;

    /**
     * Agent execution-pattern categories; tokens supply schema values without execution or authorization.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAgentSpec.AgentType choices described as Agent execution-pattern categories; tokens supply schema values without execution or authorization. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum AgentType {
        /**
         * Requested reasoning/action cycles category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAgentSpec.AgentType#REACT policy choice: Requested reasoning/action cycles category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAgentSpec.AgentType; its declared order and identity are preserved.
         */
        REACT,
        /**
         * Requested planning followed by execution category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAgentSpec.AgentType#PLAN_EXECUTE policy choice: Requested planning followed by execution category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAgentSpec.AgentType; its declared order and identity are preserved.
         */
        PLAN_EXECUTE,
        /**
         * Requested function-calling operation category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAgentSpec.AgentType#FUNCTION_CALLING policy choice: Requested function-calling operation category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAgentSpec.AgentType; its declared order and identity are preserved.
         */
        FUNCTION_CALLING,
        /**
         * Requested conversation-oriented operation category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAgentSpec.AgentType#CONVERSATIONAL policy choice: Requested conversation-oriented operation category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAgentSpec.AgentType; its declared order and identity are preserved.
         */
        CONVERSATIONAL,
        /**
         * Requested autonomous operation category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAgentSpec.AgentType#AUTONOMOUS policy choice: Requested autonomous operation category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAgentSpec.AgentType; its declared order and identity are preserved.
         */
        AUTONOMOUS
    }

    /**
     * Requested iteration limit.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAgentSpec#maxIterations intent: Requested iteration limit. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 10}.
     */
    int maxIterations() default 10;

    /**
     * Requested tool-call limit; the annotation performs no accounting.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAgentSpec#maxToolCalls intent: Requested tool-call limit; the annotation performs no accounting. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 50}.
     */
    int maxToolCalls() default 50;

    // --- MEMORY ---

    /**
     * Requested conversational-memory category.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAgentSpec#memory intent: Requested conversational-memory category. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code MemoryType}; default expression {@code MemoryType.CONVERSATION}.
     */
    MemoryType memory() default MemoryType.CONVERSATION;

    /**
     * Agent memory categories; tokens supply schema values without execution or authorization.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAgentSpec.MemoryType choices described as Agent memory categories; tokens supply schema values without execution or authorization. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum MemoryType {
        /**
         * Requested no memory category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAgentSpec.MemoryType#NONE policy choice: Requested no memory category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAgentSpec.MemoryType; its declared order and identity are preserved.
         */
        NONE,
        /**
         * Requested conversation history category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAgentSpec.MemoryType#CONVERSATION policy choice: Requested conversation history category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAgentSpec.MemoryType; its declared order and identity are preserved.
         */
        CONVERSATION,
        /**
         * Requested summarized history category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAgentSpec.MemoryType#SUMMARY policy choice: Requested summarized history category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAgentSpec.MemoryType; its declared order and identity are preserved.
         */
        SUMMARY,
        /**
         * Requested vector-backed recall category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAgentSpec.MemoryType#VECTOR policy choice: Requested vector-backed recall category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAgentSpec.MemoryType; its declared order and identity are preserved.
         */
        VECTOR,
        /**
         * Requested combined memory category; selecting this token executes no operation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAgentSpec.MemoryType#HYBRID policy choice: Requested combined memory category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAgentSpec.MemoryType; its declared order and identity are preserved.
         */
        HYBRID
    }

    // --- PLANNING ---

    /**
     * Whether a planning stage is requested before execution.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAgentSpec#planFirst intent: Whether a planning stage is requested before execution. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean planFirst() default false;

    /**
     * Whether a reflection stage is requested after execution.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAgentSpec#reflectAfter intent: Whether a reflection stage is requested after execution. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean reflectAfter() default false;

    // --- TASK DEFINITION (nested) ---

    /**
     * Declares a method-level task goal, steps and output category; it starts no task.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares a method-level task goal, steps and output category; it starts no task.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Task {

        /**
         * Task goal text.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IAgentSpec.Task#goal intent: Task goal text. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String goal();

        /**
         * Requested task-step descriptions in supplied order.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IAgentSpec.Task#steps intent: Requested task-step descriptions in supplied order. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
         */
        String[] steps() default {};

        /**
         * Requested task output representation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IAgentSpec.Task#outputFormat intent: Requested task output representation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code OutputFormat}; default expression {@code OutputFormat.TEXT}.
         */
        OutputFormat outputFormat() default OutputFormat.TEXT;

        /**
         * Task output representations; tokens supply schema values without execution or authorization.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Enumerates the IAgentSpec.Task.OutputFormat choices described as Task output representations; tokens supply schema values without execution or authorization. Selection records an annotation value; execution remains the consumer responsibility.
         * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
         */
        enum OutputFormat {
            /**
             * Requested plain text category; selecting this token executes no operation.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the IAgentSpec.Task.OutputFormat#TEXT policy choice: Requested plain text category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of IAgentSpec.Task.OutputFormat; its declared order and identity are preserved.
             */
            TEXT,
            /**
             * Requested JSON category; selecting this token executes no operation.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the IAgentSpec.Task.OutputFormat#JSON policy choice: Requested JSON category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of IAgentSpec.Task.OutputFormat; its declared order and identity are preserved.
             */
            JSON,
            /**
             * Requested Markdown category; selecting this token executes no operation.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the IAgentSpec.Task.OutputFormat#MARKDOWN policy choice: Requested Markdown category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of IAgentSpec.Task.OutputFormat; its declared order and identity are preserved.
             */
            MARKDOWN,
            /**
             * Requested source code category; selecting this token executes no operation.
             * @m3.pattern synexia:aiop
             * @m3.algorithm Names the IAgentSpec.Task.OutputFormat#CODE policy choice: Requested source code category; selecting this token executes no operation. This enum token does not implement the selected runtime behavior.
             * @m3.dataStructures Existing constant of IAgentSpec.Task.OutputFormat; its declared order and identity are preserved.
             */
            CODE
        }
    }

    // --- DESCRIPTION ---

    /**
     * Natural-language agent specification.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAgentSpec#value intent: Natural-language agent specification. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";
}
