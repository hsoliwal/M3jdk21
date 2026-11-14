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
 * IDagSpec - DAG (Directed Acyclic Graph) flow specification for AI code
 * generation.
 *
 * <h2>DAG Flow Generation</h2>
 *
 * <pre>
 * From @IDagSpec annotation, the IOP pipeline generates:
 *   1. DAG executor with topological ordering
 *   2. Step dependency resolution
 *   3. Parallel execution where possible
 *   4. Error handling and retry
 *   5. Progress tracking
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;ISpec(value = "ETL data pipeline", library = Library.DAG_FLOW)
 * 	public interface IDataPipeline&lt;Self extends IDataPipeline&lt;Self>> extends IContext&lt;Self> {
 *
 * 		&#64;IDagSpec(step = "extract", order = 1, description = "Extract data from source")
 * 		List&lt;Record> extract();
 *
 * 		&#64;IDagSpec(step = "transform", order = 2, dependsOn = "extract", description = "Transform records")
 * 		List&lt;Record> transform(List&lt;Record> records);
 *
 * 		&#64;IDagSpec(step = "load", order = 3, dependsOn = "transform", description = "Load to destination")
 * 		void load(List&lt;Record> records);
 * 	}
 * }
 * </pre>
 *
 * <h2>Generated DAG Executor</h2>
 *
 * <pre>{@code
 * public class DataPipelineExecutor {
 *
 *     private final DAG&lt;String> dag = new DAG&lt;>();
 *
 *     public DataPipelineExecutor() {
 *         dag.addNode("extract");
 *         dag.addNode("transform");
 *         dag.addNode("load");
 *         dag.addEdge("extract", "transform");
 *         dag.addEdge("transform", "load");
 * }
 *
 * public void execute() {
 * for (String step : dag.topologicalSort()) {
 * executeStep(step);
 * }
 * }
 * }
 * }</pre>
 *
 * @since IOP 1.0 - DAG Flow
 * @see IopParadigm.DAGPattern
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares a DAG step, dependencies, ordering, concurrency, retries, checkpoints, patterns and compensation; it schedules no step and proves no acyclicity.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IDagSpec.java}
 * blob {@code 58b45d0c2303293effca147308834391908218ad} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IDagSpec {

    // --- STEP DEFINITION ---

    /**
     * Step name/ID in the DAG.
     *
     * @return step name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#step intent: Step name/ID in the DAG. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String step() default "";

    /**
     * Execution order (for topological hints).
     *
     * @return order number
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#order intent: Execution order (for topological hints). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int order() default 0;

    /**
     * Human-readable description.
     *
     * @return description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#description intent: Human-readable description. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String description() default "";

    // --- DEPENDENCIES ---

    /**
     * Steps this step depends on.
     *
     * @return dependency step names
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#dependsOn intent: Steps this step depends on. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] dependsOn() default {};

    /**
     * Whether all dependencies must complete (AND) or any (OR).
     *
     * @return dependency mode
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#dependencyMode intent: Whether all dependencies must complete (AND) or any (OR). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code DependencyMode}; default expression {@code DependencyMode.ALL}.
     */
    DependencyMode dependencyMode() default DependencyMode.ALL;

    /** Dependency modes.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IDagSpec.DependencyMode choices described as Dependency modes. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 2 constants in the existing source order.
     */
    enum DependencyMode {
        /** All dependencies must complete.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.DependencyMode#ALL policy choice: All dependencies must complete. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.DependencyMode; its declared order and identity are preserved.
         */
        ALL,
        /** Any one dependency is sufficient.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.DependencyMode#ANY policy choice: Any one dependency is sufficient. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.DependencyMode; its declared order and identity are preserved.
         */
        ANY
    }

    // --- EXECUTION ---

    /**
     * Whether this step can run in parallel with siblings.
     *
     * @return true if parallelizable
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#parallel intent: Whether this step can run in parallel with siblings. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean parallel() default true;

    /**
     * Maximum execution time in milliseconds (0 = no limit).
     *
     * @return timeout
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#timeoutMs intent: Maximum execution time in milliseconds (0 = no limit). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int timeoutMs() default 0;

    /**
     * Priority for scheduling (higher = first).
     *
     * @return priority
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#priority intent: Priority for scheduling (higher = first). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int priority() default 0;

    // --- RETRY ---

    /**
     * Maximum retry attempts.
     *
     * @return retries
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#maxRetries intent: Maximum retry attempts. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 3}.
     */
    int maxRetries() default 3;

    /**
     * Retry backoff in milliseconds.
     *
     * @return backoff
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#retryBackoffMs intent: Retry backoff in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 1000}.
     */
    int retryBackoffMs() default 1000;

    /**
     * Backoff multiplier for exponential backoff.
     *
     * @return multiplier
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#backoffMultiplier intent: Backoff multiplier for exponential backoff. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code double}; default expression {@code 2.0}.
     */
    double backoffMultiplier() default 2.0;

    // --- ERROR HANDLING ---

    /**
     * Error handling strategy.
     *
     * @return error strategy
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#errorStrategy intent: Error handling strategy. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code ErrorStrategy}; default expression {@code ErrorStrategy.FAIL_FAST}.
     */
    ErrorStrategy errorStrategy() default ErrorStrategy.FAIL_FAST;

    /** Error handling strategies.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IDagSpec.ErrorStrategy choices described as Error handling strategies. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum ErrorStrategy {
        /** Stop entire DAG on failure.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.ErrorStrategy#FAIL_FAST policy choice: Stop entire DAG on failure. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.ErrorStrategy; its declared order and identity are preserved.
         */
        FAIL_FAST,
        /** Continue other branches, mark this branch failed.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.ErrorStrategy#CONTINUE policy choice: Continue other branches, mark this branch failed. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.ErrorStrategy; its declared order and identity are preserved.
         */
        CONTINUE,
        /** Skip this step and dependencies.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.ErrorStrategy#SKIP policy choice: Skip this step and dependencies. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.ErrorStrategy; its declared order and identity are preserved.
         */
        SKIP,
        /** Use fallback value.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.ErrorStrategy#FALLBACK policy choice: Use fallback value. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.ErrorStrategy; its declared order and identity are preserved.
         */
        FALLBACK,
        /** Retry then continue.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.ErrorStrategy#RETRY_THEN_CONTINUE policy choice: Retry then continue. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.ErrorStrategy; its declared order and identity are preserved.
         */
        RETRY_THEN_CONTINUE
    }

    /**
     * Fallback method name (for FALLBACK strategy).
     *
     * @return fallback method
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#fallbackMethod intent: Fallback method name (for FALLBACK strategy). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String fallbackMethod() default "";

    // --- CHECKPOINTING ---

    /**
     * Whether to checkpoint state after this step.
     *
     * @return true for checkpoint
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#checkpoint intent: Whether to checkpoint state after this step. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean checkpoint() default false;

    /**
     * Checkpoint storage type.
     *
     * @return storage type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#checkpointStorage intent: Checkpoint storage type. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code CheckpointStorage}; default expression {@code CheckpointStorage.MEMORY}.
     */
    CheckpointStorage checkpointStorage() default CheckpointStorage.MEMORY;

    /** Checkpoint storage options.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IDagSpec.CheckpointStorage choices described as Checkpoint storage options. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 4 constants in the existing source order.
     */
    enum CheckpointStorage {
        /** In-memory (lost on restart).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.CheckpointStorage#MEMORY policy choice: In-memory (lost on restart). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.CheckpointStorage; its declared order and identity are preserved.
         */
        MEMORY,
        /** Local file system.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.CheckpointStorage#FILE policy choice: Local file system. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.CheckpointStorage; its declared order and identity are preserved.
         */
        FILE,
        /** Database.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.CheckpointStorage#DATABASE policy choice: Database. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.CheckpointStorage; its declared order and identity are preserved.
         */
        DATABASE,
        /** Distributed (e.g., Redis).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.CheckpointStorage#DISTRIBUTED policy choice: Distributed (e.g., Redis). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.CheckpointStorage; its declared order and identity are preserved.
         */
        DISTRIBUTED
    }

    // --- MONITORING ---

    /**
     * Whether to emit metrics for this step.
     *
     * @return true for metrics
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#metrics intent: Whether to emit metrics for this step. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean metrics() default true;

    /**
     * Custom metric tags.
     *
     * @return tags as key=value pairs
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#metricTags intent: Custom metric tags. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] metricTags() default {};

    /**
     * Log level for this step.
     *
     * @return log level
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#logLevel intent: Log level for this step. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code LogLevel}; default expression {@code LogLevel.INFO}.
     */
    LogLevel logLevel() default LogLevel.INFO;

    /** Log levels.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IDagSpec.LogLevel choices described as Log levels. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum LogLevel {
        /** Trace level.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.LogLevel#TRACE policy choice: Trace level. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.LogLevel; its declared order and identity are preserved.
         */
        TRACE,
        /** Debug level.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.LogLevel#DEBUG policy choice: Debug level. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.LogLevel; its declared order and identity are preserved.
         */
        DEBUG,
        /** Info level.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.LogLevel#INFO policy choice: Info level. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.LogLevel; its declared order and identity are preserved.
         */
        INFO,
        /** Warn level.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.LogLevel#WARN policy choice: Warn level. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.LogLevel; its declared order and identity are preserved.
         */
        WARN,
        /** Error level.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.LogLevel#ERROR policy choice: Error level. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.LogLevel; its declared order and identity are preserved.
         */
        ERROR
    }

    // --- PATTERNS ---

    /**
     * DAG execution pattern for this step.
     *
     * @return DAG pattern
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#pattern intent: DAG execution pattern for this step. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Pattern}; default expression {@code Pattern.STANDARD}.
     */
    Pattern pattern() default Pattern.STANDARD;

    /** DAG patterns.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IDagSpec.Pattern choices described as DAG patterns. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 9 constants in the existing source order.
     */
    enum Pattern {
        /** Standard sequential step.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.Pattern#STANDARD policy choice: Standard sequential step. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.Pattern; its declared order and identity are preserved.
         */
        STANDARD,
        /** Fork (split into parallel).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.Pattern#FORK policy choice: Fork (split into parallel). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.Pattern; its declared order and identity are preserved.
         */
        FORK,
        /** Join (wait for parallel branches).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.Pattern#JOIN policy choice: Join (wait for parallel branches). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.Pattern; its declared order and identity are preserved.
         */
        JOIN,
        /** Map (apply to collection).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.Pattern#MAP policy choice: Map (apply to collection). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.Pattern; its declared order and identity are preserved.
         */
        MAP,
        /** Reduce (aggregate results).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.Pattern#REDUCE policy choice: Reduce (aggregate results). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.Pattern; its declared order and identity are preserved.
         */
        REDUCE,
        /** Filter (conditional pass-through).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.Pattern#FILTER policy choice: Filter (conditional pass-through). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.Pattern; its declared order and identity are preserved.
         */
        FILTER,
        /** Branch (conditional execution).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.Pattern#BRANCH policy choice: Branch (conditional execution). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.Pattern; its declared order and identity are preserved.
         */
        BRANCH,
        /** Loop (iterate until condition).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.Pattern#LOOP policy choice: Loop (iterate until condition). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.Pattern; its declared order and identity are preserved.
         */
        LOOP,
        /** Saga step (with compensation).
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IDagSpec.Pattern#SAGA policy choice: Saga step (with compensation). This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IDagSpec.Pattern; its declared order and identity are preserved.
         */
        SAGA
    }

    /**
     * Compensation method for SAGA pattern.
     *
     * @return compensation method name
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#compensationMethod intent: Compensation method for SAGA pattern. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String compensationMethod() default "";

    // --- INPUT/OUTPUT ---

    /**
     * Input type for this step.
     *
     * @return input class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#inputType intent: Input type for this step. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> inputType() default Void.class;

    /**
     * Output type for this step.
     *
     * @return output class
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#outputType intent: Output type for this step. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}; default expression {@code Void.class}.
     */
    Class<?> outputType() default Void.class;

    /**
     * Whether output should be cached for reuse.
     *
     * @return true for caching
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#cacheOutput intent: Whether output should be cached for reuse. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean cacheOutput() default false;

    // --- MODEL ---

    /**
     * Preferred model for generation.
     *
     * @return model type
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IDagSpec#model intent: Preferred model for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.SONNET}.
     */
    Model.ModelType model() default Model.ModelType.SONNET;
}
