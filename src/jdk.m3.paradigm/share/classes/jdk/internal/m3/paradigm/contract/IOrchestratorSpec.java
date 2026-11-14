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
 * IOrchestratorSpec - Multi-agent orchestration specification for AI code
 * generation.
 *
 * <h2>THE ORCHESTRATOR SPEC</h2>
 *
 * <pre>
 * Orchestration patterns:
 *   - Supervisor: Routes tasks to specialized workers
 *   - Sequential: Chain of agents
 *   - Parallel: Concurrent agent execution
 *   - Conditional: Dynamic routing based on context
 *   - Hierarchical: Multi-level agent trees
 *
 * Aggregation strategies:
 *   - FIRST: Return first result
 *   - LAST: Return last result
 *   - MERGE: Combine all results
 *   - VOTE: Majority consensus
 *   - CUSTOM: Custom aggregator
 *
 * &#64;IOrchestratorSpec defines multi-agent coordination in IOP style.
 * "One supervisor, many workers. Clean separation of concerns."
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * // Supervisor orchestrator
 * &#64;IOrchestratorSpec(
 *     name = "CodeSupervisor",
 *     description = "Routes coding tasks to specialized agents",
 *     mode = OrchestratorMode.SUPERVISOR,
 *     workers = {
 *         &#64;IOrchestratorSpec.Worker(
 *             name = "reviewer",
 *             agent = ICodeReviewerAgent.class,
 *             capabilities = {"review", "analyze"}
 *         ),
 *         &#64;IOrchestratorSpec.Worker(
 *             name = "fixer",
 *             agent = IBugFixerAgent.class,
 *             capabilities = {"fix", "refactor"}
 *         ),
 *         &#64;IOrchestratorSpec.Worker(
 *             name = "tester",
 *             agent = ITestWriterAgent.class,
 *             capabilities = {"test", "verify"}
 *         )
 *     },
 *     routingStrategy = RoutingStrategy.CAPABILITY_MATCH
 * )
 * public interface ICodeSupervisor&lt;Self extends ICodeSupervisor&lt;Self>> extends IContext&lt;Self> {
 *
 *     &#64;IOrchestratorSpec.Route(
 *         pattern = "review.*|analyze.*",
 *         worker = "reviewer"
 *     )
 *     AgentResult reviewCode(String code);
 *
 *     &#64;IOrchestratorSpec.Route(
 *         pattern = "fix.*|refactor.*",
 *         worker = "fixer"
 *     )
 *     AgentResult fixCode(String code, String issue);
 * }
 *
 * // Parallel orchestrator
 * &#64;IOrchestratorSpec(
 *     name = "SearchOrchestrator",
 *     mode = OrchestratorMode.PARALLEL,
 *     workers = {...},
 *     aggregation = AggregationStrategy.MERGE
 * )
 * public interface ISearchOrchestrator&lt;Self extends ISearchOrchestrator&lt;Self>> extends IContext&lt;Self> {
 *     List&lt;SearchResult> search(String query);
 * }
 * }
 * </pre>
 *
 * @since IOP 1.1 - Orchestrator Specification
 * @see IAgentSpec
 * @see IFlowSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares worker schemas, routing, aggregation, concurrency, timeout and fallback preferences; it launches no worker, delegates no task and performs no routing.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.TYPE}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IOrchestratorSpec.java}
 * blob {@code 118c188b593f977b0a082d418bd28085ac51e2de} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IOrchestratorSpec {

    // --- IDENTITY ---

    /**
     * Orchestrator name.
     *
     * @return name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IOrchestratorSpec#name intent: Orchestrator name. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
     */
    String name();

    /**
     * Orchestrator description.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IOrchestratorSpec#description intent: Orchestrator description. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String description() default "";

    /**
     * Specification value (alternative to description).
     *
     * @return value
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IOrchestratorSpec#value intent: Specification value (alternative to description). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    // --- ORCHESTRATION MODE ---

    /**
     * Orchestration mode.
     *
     * @return mode
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IOrchestratorSpec#mode intent: Orchestration mode. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code OrchestratorMode}; default expression {@code OrchestratorMode.SUPERVISOR}.
     */
    OrchestratorMode mode() default OrchestratorMode.SUPERVISOR;

    /** Orchestration modes.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IOrchestratorSpec.OrchestratorMode choices described as Orchestration modes. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum OrchestratorMode {
        /** Supervisor routes to workers based on task.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.OrchestratorMode#SUPERVISOR policy choice: Supervisor routes to workers based on task. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.OrchestratorMode; its declared order and identity are preserved.
         */
        SUPERVISOR,

        /** Sequential chain of agents.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.OrchestratorMode#SEQUENTIAL policy choice: Sequential chain of agents. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.OrchestratorMode; its declared order and identity are preserved.
         */
        SEQUENTIAL,

        /** Parallel execution of all workers.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.OrchestratorMode#PARALLEL policy choice: Parallel execution of all workers. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.OrchestratorMode; its declared order and identity are preserved.
         */
        PARALLEL,

        /** Conditional routing based on context.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.OrchestratorMode#CONDITIONAL policy choice: Conditional routing based on context. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.OrchestratorMode; its declared order and identity are preserved.
         */
        CONDITIONAL,

        /** Hierarchical multi-level tree.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.OrchestratorMode#HIERARCHICAL policy choice: Hierarchical multi-level tree. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.OrchestratorMode; its declared order and identity are preserved.
         */
        HIERARCHICAL,

        /** Event-driven agent activation.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.OrchestratorMode#EVENT_DRIVEN policy choice: Event-driven agent activation. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.OrchestratorMode; its declared order and identity are preserved.
         */
        EVENT_DRIVEN
    }

    // --- WORKERS ---

    /**
     * Worker agents.
     *
     * @return workers
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IOrchestratorSpec#workers intent: Worker agents. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Worker[]}; default expression {@code {}}.
     */
    Worker[] workers() default {};

    /** Worker agent definition.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares an embedded worker identity, agent type, capabilities, priority, concurrency and timeout; its empty Target restricts direct application and it starts no agent.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code {}}; existing element and enum schemas are preserved.
     */
    @Target({})
    @Retention(RetentionPolicy.SOURCE)
    @interface Worker {

        /** Worker name (unique within orchestrator).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IOrchestratorSpec.Worker#name intent: Worker name (unique within orchestrator). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String name();

        /** Agent interface class.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IOrchestratorSpec.Worker#agent intent: Agent interface class. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code Class<?>}; an explicit value is required.
         */
        Class<?> agent();

        /** Capabilities this worker provides.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IOrchestratorSpec.Worker#capabilities intent: Capabilities this worker provides. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
         */
        String[] capabilities() default {};

        /** Priority (lower = higher priority).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IOrchestratorSpec.Worker#priority intent: Priority (lower = higher priority). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 100}.
         */
        int priority() default 100;

        /** Maximum concurrent tasks.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IOrchestratorSpec.Worker#maxConcurrent intent: Maximum concurrent tasks. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 1}.
         */
        int maxConcurrent() default 1;

        /** Timeout for worker in ms.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IOrchestratorSpec.Worker#timeout intent: Timeout for worker in ms. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 60000}.
         */
        int timeout() default 60000;
    }

    // --- ROUTING ---

    /**
     * Routing strategy for supervisor mode.
     *
     * @return strategy
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IOrchestratorSpec#routingStrategy intent: Routing strategy for supervisor mode. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code RoutingStrategy}; default expression {@code RoutingStrategy.CAPABILITY_MATCH}.
     */
    RoutingStrategy routingStrategy() default RoutingStrategy.CAPABILITY_MATCH;

    /** Routing strategies.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IOrchestratorSpec.RoutingStrategy choices described as Routing strategies. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum RoutingStrategy {
        /** Match task to worker capabilities.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.RoutingStrategy#CAPABILITY_MATCH policy choice: Match task to worker capabilities. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.RoutingStrategy; its declared order and identity are preserved.
         */
        CAPABILITY_MATCH,

        /** Round-robin distribution.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.RoutingStrategy#ROUND_ROBIN policy choice: Round-robin distribution. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.RoutingStrategy; its declared order and identity are preserved.
         */
        ROUND_ROBIN,

        /** Least-busy worker.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.RoutingStrategy#LEAST_BUSY policy choice: Least-busy worker. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.RoutingStrategy; its declared order and identity are preserved.
         */
        LEAST_BUSY,

        /** Random selection.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.RoutingStrategy#RANDOM policy choice: Random selection. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.RoutingStrategy; its declared order and identity are preserved.
         */
        RANDOM,

        /** LLM-based routing decision.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.RoutingStrategy#LLM_ROUTE policy choice: LLM-based routing decision. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.RoutingStrategy; its declared order and identity are preserved.
         */
        LLM_ROUTE,

        /** Custom router bean.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.RoutingStrategy#CUSTOM policy choice: Custom router bean. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.RoutingStrategy; its declared order and identity are preserved.
         */
        CUSTOM
    }

    /**
     * Custom router bean name (when strategy = CUSTOM).
     *
     * @return bean name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IOrchestratorSpec#routerBean intent: Custom router bean name (when strategy = CUSTOM). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String routerBean() default "";

    // --- AGGREGATION ---

    /**
     * Result aggregation strategy.
     *
     * @return strategy
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IOrchestratorSpec#aggregation intent: Result aggregation strategy. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code AggregationStrategy}; default expression {@code AggregationStrategy.FIRST}.
     */
    AggregationStrategy aggregation() default AggregationStrategy.FIRST;

    /** Aggregation strategies.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IOrchestratorSpec.AggregationStrategy choices described as Aggregation strategies. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum AggregationStrategy {
        /** Return first result.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.AggregationStrategy#FIRST policy choice: Return first result. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.AggregationStrategy; its declared order and identity are preserved.
         */
        FIRST,

        /** Return last result.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.AggregationStrategy#LAST policy choice: Return last result. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.AggregationStrategy; its declared order and identity are preserved.
         */
        LAST,

        /** Merge all results.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.AggregationStrategy#MERGE policy choice: Merge all results. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.AggregationStrategy; its declared order and identity are preserved.
         */
        MERGE,

        /** Majority vote (for decisions).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.AggregationStrategy#VOTE policy choice: Majority vote (for decisions). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.AggregationStrategy; its declared order and identity are preserved.
         */
        VOTE,

        /** Best result by score.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.AggregationStrategy#BEST_SCORE policy choice: Best result by score. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.AggregationStrategy; its declared order and identity are preserved.
         */
        BEST_SCORE,

        /** Custom aggregator.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IOrchestratorSpec.AggregationStrategy#CUSTOM policy choice: Custom aggregator. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IOrchestratorSpec.AggregationStrategy; its declared order and identity are preserved.
         */
        CUSTOM
    }

    /**
     * Custom aggregator bean name.
     *
     * @return bean name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IOrchestratorSpec#aggregatorBean intent: Custom aggregator bean name. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String aggregatorBean() default "";

    // --- EXECUTION ---

    /**
     * Use virtual threads (Java 21).
     *
     * @return true for virtual threads
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IOrchestratorSpec#virtualThreads intent: Use virtual threads (Java 21). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean virtualThreads() default true;

    /**
     * Global timeout in ms.
     *
     * @return timeout
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IOrchestratorSpec#timeout intent: Global timeout in ms. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 120000}.
     */
    int timeout() default 120000;

    /**
     * Max parallel workers.
     *
     * @return max parallel
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IOrchestratorSpec#maxParallel intent: Max parallel workers. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 10}.
     */
    int maxParallel() default 10;

    /**
     * Fail-fast on worker error.
     *
     * @return true for fail-fast
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IOrchestratorSpec#failFast intent: Fail-fast on worker error. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean failFast() default false;

    // --- RETRY & FALLBACK ---

    /**
     * Max retries per worker.
     *
     * @return retries
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IOrchestratorSpec#maxRetries intent: Max retries per worker. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 2}.
     */
    int maxRetries() default 2;

    /**
     * Fallback worker name.
     *
     * @return fallback name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IOrchestratorSpec#fallbackWorker intent: Fallback worker name. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String fallbackWorker() default "";

    // --- ROUTE ANNOTATION ---

    /** Route - Maps task patterns to workers.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares a task-pattern and optional condition selecting a worker name; it compiles no regex and evaluates no condition.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Route {

        /** Regex pattern to match task.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IOrchestratorSpec.Route#pattern intent: Regex pattern to match task. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ".*"}.
         */
        String pattern() default ".*";

        /** Worker name to route to.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IOrchestratorSpec.Route#worker intent: Worker name to route to. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
         */
        String worker();

        /** Priority (lower = matched first).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IOrchestratorSpec.Route#priority intent: Priority (lower = matched first). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 100}.
         */
        int priority() default 100;

        /** Condition expression (SpEL).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IOrchestratorSpec.Route#condition intent: Condition expression (SpEL). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
         */
        String condition() default "";
    }

    /** Broadcast - Send to all workers (parallel mode).
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares worker-capability filtering and aggregation preference; it sends no message and starts no parallel execution.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Broadcast {

        /** Filter workers by capability.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IOrchestratorSpec.Broadcast#capabilities intent: Filter workers by capability. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
         */
        String[] capabilities() default {};

        /** Aggregation for broadcast results.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IOrchestratorSpec.Broadcast#aggregation intent: Aggregation for broadcast results. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code AggregationStrategy}; default expression {@code AggregationStrategy.MERGE}.
         */
        AggregationStrategy aggregation() default AggregationStrategy.MERGE;
    }

    /** Chain - Sequential worker chain.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares worker names in sequence and result-passing preference; it invokes no worker and transfers no value.
     * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @interface Chain {

        /** Worker names in order.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IOrchestratorSpec.Chain#workers intent: Worker names in order. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code String[]}; an explicit value is required.
         */
        String[] workers();

        /** Pass previous result to next.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Declares IOrchestratorSpec.Chain#passResult intent: Pass previous result to next. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
         * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
         */
        boolean passResult() default true;
    }

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IOrchestratorSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.OPUS}.
     */
    Model.ModelType model() default Model.ModelType.OPUS;

    /**
     * Stability level.
     *
     * @return stability
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IOrchestratorSpec#stability intent: Stability level. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Stability.Level}; default expression {@code Stability.Level.STABLE}.
     */
    Stability.Level stability() default Stability.Level.STABLE;
}
