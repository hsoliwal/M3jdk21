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
import jdk.internal.m3.paradigm.annotation.Stability;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IFlowSpec - Flow orchestration specification for AI code generation.
 *
 * <h2>THE FLOW SPEC</h2>
 *
 * <pre>
 * Flow patterns inspired by:
 * - Apache Camel EIP (Enterprise Integration Patterns)
 * - Spring Integration
 * - Ant build flows (XML task definitions)
 * - DAG orchestration (Airflow-style)
 * - ReAct agent loops
 *
 * Flow elements:
 * - TOOL: Single bean method invocation
 * - BRANCH: Conditional routing (if-then)
 * - PARALLEL: Concurrent execution (Scatter-Gather)
 * - LOOP: Iterative execution (ReAct pattern)
 *
 * &#64;IFlowSpec defines flow orchestration in IOP style.
 * "Declarative flows, explicit execution."
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * // Class-level: define a flow orchestrator
 * &#64;IFlowSpec(
 * id = "invoice-processor",
 * description = "Process incoming invoices",
 * mode = ExecutionMode.DAG,
 * timeout = 30000
 * )
 * public interface IInvoiceFlow&lt;Self extends IInvoiceFlow&lt;Self>> extends IContext&lt;Self> {
 *
 * &#64;IFlowSpec.Step(
 * id = "parse",
 * bean = "invoiceParser",
 * method = "parse"
 * )
 * void parseInvoice();
 *
 * &#64;IFlowSpec.Step(id = "validate", bean = "validator", depends = "parse", condition = "${body.confidence >= 0.8}")
 * void validateInvoice();
 *
 * &#64;IFlowSpec.Parallel(id = "enrich", depends = "validate", aggregate = "merger")
 * void enrichInvoice();
 * }
 * }
 * </pre>
 *
 * <h2>XML Flow Definition</h2>
 *
 * <pre>{@code
 * <flows xmlns="http://synexia.com/flow">
 * <flow id="invoice-agent" description="Process invoice">
 * <tool id="parse" bean="parseSkill" method="parse"/>
 * <branch condition="${body.confidence >= 0.8}" depends="parse">
 * <tool id="execute" bean="executor"/>
 * </branch>
 * <parallel id="search" depends="parse" aggregate="merger">
 * <tool id="db" bean="dbTool"/>
 * <tool id="api" bean="apiTool"/>
 * </parallel>
 * <loop id="react" condition="${!body.complete}" max="5">
 * <tool id="act" bean="actor"/>
 * <tool-ref id="parse"/>
 * </loop>
 * </flow>
 * </flows>
 * }</pre>
 *
 * @since IOP 1.1 - Flow Specification
 * @see IAgentSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares flow identity, execution mode, retry, context and nested step/branch/parallel/loop schemas; it invokes no bean and evaluates no condition.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.TYPE}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IFlowSpec.java}
 * blob {@code 99a9d084667e7fb9f7e01951d4b51e8604afbbef} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IFlowSpec {

    // --- IDENTITY ---

    /**
     * Flow ID (unique within application).
     *
     * @return flow ID
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFlowSpec#id intent: Flow ID (unique within application). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String id() default "";

    /**
     * Flow description.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFlowSpec#description intent: Flow description. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String description() default "";

    /**
     * Flow specification value (alternative to description).
     *
     * @return spec value
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFlowSpec#value intent: Flow specification value (alternative to description). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    // --- EXECUTION MODE ---

    /**
     * Execution mode.
     *
     * @return mode
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFlowSpec#mode intent: Execution mode. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code ExecutionMode}; default expression {@code ExecutionMode.SEQUENTIAL}.
     */
    ExecutionMode mode() default ExecutionMode.SEQUENTIAL;

    /** Flow execution modes.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IFlowSpec.ExecutionMode choices described as Flow execution modes. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum ExecutionMode {
        /** Execute steps in order.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFlowSpec.ExecutionMode#SEQUENTIAL policy choice: Execute steps in order. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFlowSpec.ExecutionMode; its declared order and identity are preserved.
         */
        SEQUENTIAL,

        /** Execute steps as DAG (respect dependencies).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFlowSpec.ExecutionMode#DAG policy choice: Execute steps as DAG (respect dependencies). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFlowSpec.ExecutionMode; its declared order and identity are preserved.
         */
        DAG,

        /** Execute all steps in parallel.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFlowSpec.ExecutionMode#PARALLEL policy choice: Execute all steps in parallel. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFlowSpec.ExecutionMode; its declared order and identity are preserved.
         */
        PARALLEL,

        /** Event-driven execution.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFlowSpec.ExecutionMode#EVENT_DRIVEN policy choice: Event-driven execution. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFlowSpec.ExecutionMode; its declared order and identity are preserved.
         */
        EVENT_DRIVEN,

        /** ReAct loop until complete.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IFlowSpec.ExecutionMode#REACT_LOOP policy choice: ReAct loop until complete. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IFlowSpec.ExecutionMode; its declared order and identity are preserved.
         */
        REACT_LOOP
    }

    // --- TIMEOUT & RETRY ---

    /**
     * Flow timeout in milliseconds.
     *
     * @return timeout
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFlowSpec#timeout intent: Flow timeout in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 60000}.
     */
    int timeout() default 60000;

    /**
     * Max retries on failure.
     *
     * @return retries
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFlowSpec#maxRetries intent: Max retries on failure. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 3}.
     */
    int maxRetries() default 3;

    /**
     * Retry backoff multiplier.
     *
     * @return backoff
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFlowSpec#retryBackoff intent: Retry backoff multiplier. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 2.0}.
     */
    double retryBackoff() default 2.0;

    // --- CONTEXT ---

    /**
     * Context class for flow data.
     *
     * @return context class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFlowSpec#contextClass intent: Context class for flow data. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> contextClass() default Void.class;

    /**
     * Whether to persist flow state.
     *
     * @return true if persistent
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFlowSpec#persistent intent: Whether to persist flow state. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean persistent() default false;

    // --- VIRTUAL THREADS (Java 21) ---

    /**
     * Use virtual threads for parallel execution.
     *
     * @return true for virtual threads
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFlowSpec#virtualThreads intent: Use virtual threads for parallel execution. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean virtualThreads() default true;

    // --- FLOW ELEMENTS (Nested Annotations) ---

    /** Step - Single tool/bean invocation.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares a named bean invocation, dependencies, condition, timeout and retries for a method; it invokes no bean.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Step {

        /** Step ID.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Step#id intent: Step ID. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String id();

        /** Bean name to invoke.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Step#bean intent: Bean name to invoke. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String bean();

        /** Method name (optional).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Step#method intent: Method name (optional). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String method() default "";

        /** Dependencies (comma-separated step IDs).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Step#depends intent: Dependencies (comma-separated step IDs). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String depends() default "";

        /** Condition expression (SpEL/EL).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Step#condition intent: Condition expression (SpEL/EL). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String condition() default "";

        /** Step timeout in ms.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Step#timeout intent: Step timeout in ms. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
         */
        int timeout() default 0;

        /** Max retries.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Step#retries intent: Max retries. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
         */
        int retries() default 0;
    }

    /** Branch - Conditional execution.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares a conditional branch and optional alternative by identifier; it evaluates no expression.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Branch {

        /** Branch ID.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Branch#id intent: Branch ID. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String id();

        /** Condition expression.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Branch#condition intent: Condition expression. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String condition();

        /** Dependencies.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Branch#depends intent: Dependencies. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String depends() default "";

        /** Else branch ID (optional).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Branch#elseBranch intent: Else branch ID (optional). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String elseBranch() default "";
    }

    /** Parallel - Concurrent execution (Scatter-Gather EIP).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares a parallel group with dependencies, aggregation, join timeout and failure preference; it starts no task and joins no result.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Parallel {

        /** Parallel group ID.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Parallel#id intent: Parallel group ID. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String id();

        /** Dependencies.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Parallel#depends intent: Dependencies. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String depends() default "";

        /** Aggregator bean for results.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Parallel#aggregate intent: Aggregator bean for results. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String aggregate() default "";

        /** Join timeout in ms.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Parallel#joinTimeout intent: Join timeout in ms. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 30000}.
         */
        int joinTimeout() default 30000;

        /** Fail-fast on any error.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Parallel#failFast intent: Fail-fast on any error. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
         */
        boolean failFast() default true;
    }

    /** Loop - Iterative execution (ReAct pattern).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares an iterative step with condition, dependencies, iteration limit and delay; it performs no iteration.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Loop {

        /** Loop ID.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Loop#id intent: Loop ID. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String id();

        /** Continue condition expression.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Loop#condition intent: Continue condition expression. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String condition();

        /** Dependencies.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Loop#depends intent: Dependencies. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String depends() default "";

        /** Max iterations.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Loop#maxIterations intent: Max iterations. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 10}.
         */
        int maxIterations() default 10;

        /** Delay between iterations in ms.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.Loop#delayMs intent: Delay between iterations in ms. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
         */
        int delayMs() default 0;
    }

    /** Reference to another step (for reuse in loops).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares a reusable flow-step identifier at a method; it resolves or invokes no step.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface StepRef {

        /** Referenced step ID.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IFlowSpec.StepRef#id intent: Referenced step ID. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String id();
    }

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFlowSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.OPUS}.
     */
    Model.ModelType model() default Model.ModelType.OPUS;

    /**
     * Stability level.
     *
     * @return stability
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IFlowSpec#stability intent: Stability level. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Stability.Level}; default expression {@code Stability.Level.STABLE}.
     */
    Stability.Level stability() default Stability.Level.STABLE;
}
