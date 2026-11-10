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

/**
 * DAG Patterns - Flow Orchestration and Pipeline Patterns.
 *
 * <p>
 * Patterns for orchestrating work as directed acyclic graphs, pipelines, and
 * complex workflows.
 * @m3.pattern synexia:iop
 * @m3.algorithm Defines 39 singleton tokens in declaration order, with generated name, ordinal, values and valueOf behavior. The declared descriptor constructor retains its supplied references and getters return them directly. Naming a pattern or annotation does not implement that named behavior.
 * @m3.dataStructures Enum values in source order: PIPELINE, PARALLEL, FORK_JOIN, BRANCH, LOOP, MERGE, SAGA, CHOREOGRAPHY, ORCHESTRATION, REACT_LOOP, PLAN_EXECUTE, MAP_REDUCE, SCATTER_GATHER, SPLITTER, AGGREGATOR, ROUTER, FILTER, TRANSFORM, COMPENSATION, RETRY_STEP, SKIP_STEP, ERROR_CHANNEL, DEAD_LETTER, CHECKPOINT, RESUME, STATE_MACHINE, EVENT_DRIVEN, BATCH, WINDOWING, RATE_LIMITING, PRIORITY_QUEUE, DELAY, DAG_TIMEOUT, TOOL_USE, MULTI_AGENT, SUPERVISOR, DELEGATION, REFLECTION, MEMORY_RETRIEVAL.
 * @m3.member {@code FIELD:PIPELINE} pattern=synexia:iop; algorithm=Initializes PIPELINE using the existing expression <code>new DAGPattern("Basic", "Execute steps in sequence")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:PARALLEL} pattern=synexia:iop; algorithm=Initializes PARALLEL using the existing expression <code>new DAGPattern("Basic", "Execute steps concurrently")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:FORK_JOIN} pattern=synexia:iop; algorithm=Initializes FORK_JOIN using the existing expression <code>new DAGPattern("Basic", "Fork to parallel, join results")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:BRANCH} pattern=synexia:iop; algorithm=Initializes BRANCH using the existing expression <code>new DAGPattern("Basic", "Conditional execution path")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:LOOP} pattern=synexia:iop; algorithm=Initializes LOOP using the existing expression <code>new DAGPattern("Basic", "Repeat until condition")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:MERGE} pattern=synexia:iop; algorithm=Initializes MERGE using the existing expression <code>new DAGPattern("Basic", "Combine multiple paths")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SAGA} pattern=synexia:iop; algorithm=Initializes SAGA using the existing expression <code>new DAGPattern("Orchestration", "Distributed transaction with compensation")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:CHOREOGRAPHY} pattern=synexia:iop; algorithm=Initializes CHOREOGRAPHY using the existing expression <code>new DAGPattern("Orchestration", "Decentralized coordination via events")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:ORCHESTRATION} pattern=synexia:iop; algorithm=Initializes ORCHESTRATION using the existing expression <code>new DAGPattern("Orchestration", "Centralized coordinator controls flow")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:REACT_LOOP} pattern=synexia:iop; algorithm=Initializes REACT_LOOP using the existing expression <code>new DAGPattern("Orchestration", "Reason-Act-Observe loop for agents")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:PLAN_EXECUTE} pattern=synexia:iop; algorithm=Initializes PLAN_EXECUTE using the existing expression <code>new DAGPattern("Orchestration", "Plan steps then execute")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:MAP_REDUCE} pattern=synexia:iop; algorithm=Initializes MAP_REDUCE using the existing expression <code>new DAGPattern("DataFlow", "Map function then reduce results")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SCATTER_GATHER} pattern=synexia:iop; algorithm=Initializes SCATTER_GATHER using the existing expression <code>new DAGPattern("DataFlow", "Scatter to workers, gather results")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SPLITTER} pattern=synexia:iop; algorithm=Initializes SPLITTER using the existing expression <code>new DAGPattern("DataFlow", "Split input into multiple outputs")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:AGGREGATOR} pattern=synexia:iop; algorithm=Initializes AGGREGATOR using the existing expression <code>new DAGPattern("DataFlow", "Aggregate multiple inputs into one")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:ROUTER} pattern=synexia:iop; algorithm=Initializes ROUTER using the existing expression <code>new DAGPattern("DataFlow", "Route to destination based on content")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:FILTER} pattern=synexia:iop; algorithm=Initializes FILTER using the existing expression <code>new DAGPattern("DataFlow", "Filter items based on predicate")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:TRANSFORM} pattern=synexia:iop; algorithm=Initializes TRANSFORM using the existing expression <code>new DAGPattern("DataFlow", "Transform item to different form")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:COMPENSATION} pattern=synexia:iop; algorithm=Initializes COMPENSATION using the existing expression <code>new DAGPattern("ErrorHandling", "Undo previous steps on failure")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:RETRY_STEP} pattern=synexia:iop; algorithm=Initializes RETRY_STEP using the existing expression <code>new DAGPattern("ErrorHandling", "Retry failed step")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SKIP_STEP} pattern=synexia:iop; algorithm=Initializes SKIP_STEP using the existing expression <code>new DAGPattern("ErrorHandling", "Skip failed step and continue")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:ERROR_CHANNEL} pattern=synexia:iop; algorithm=Initializes ERROR_CHANNEL using the existing expression <code>new DAGPattern("ErrorHandling", "Route errors to separate handler")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:DEAD_LETTER} pattern=synexia:iop; algorithm=Initializes DEAD_LETTER using the existing expression <code>new DAGPattern("ErrorHandling", "Store unprocessable items")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:CHECKPOINT} pattern=synexia:iop; algorithm=Initializes CHECKPOINT using the existing expression <code>new DAGPattern("State", "Save progress for recovery")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:RESUME} pattern=synexia:iop; algorithm=Initializes RESUME using the existing expression <code>new DAGPattern("State", "Resume from checkpoint")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:STATE_MACHINE} pattern=synexia:iop; algorithm=Initializes STATE_MACHINE using the existing expression <code>new DAGPattern("State", "Manage states and transitions")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:EVENT_DRIVEN} pattern=synexia:iop; algorithm=Initializes EVENT_DRIVEN using the existing expression <code>new DAGPattern("State", "React to events")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:BATCH} pattern=synexia:iop; algorithm=Initializes BATCH using the existing expression <code>new DAGPattern("Scheduling", "Process items in batches")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:WINDOWING} pattern=synexia:iop; algorithm=Initializes WINDOWING using the existing expression <code>new DAGPattern("Scheduling", "Process items in time/count windows")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:RATE_LIMITING} pattern=synexia:iop; algorithm=Initializes RATE_LIMITING using the existing expression <code>new DAGPattern("Scheduling", "Control processing rate")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:PRIORITY_QUEUE} pattern=synexia:iop; algorithm=Initializes PRIORITY_QUEUE using the existing expression <code>new DAGPattern("Scheduling", "Process by priority")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:DELAY} pattern=synexia:iop; algorithm=Initializes DELAY using the existing expression <code>new DAGPattern("Scheduling", "Delay step execution")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:DAG_TIMEOUT} pattern=synexia:iop; algorithm=Initializes DAG_TIMEOUT using the existing expression <code>new DAGPattern("Scheduling", "Timeout step if too slow")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:TOOL_USE} pattern=synexia:iop; algorithm=Initializes TOOL_USE using the existing expression <code>new DAGPattern("Agent", "Agent uses tools to accomplish task")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:MULTI_AGENT} pattern=synexia:iop; algorithm=Initializes MULTI_AGENT using the existing expression <code>new DAGPattern("Agent", "Multiple agents collaborate")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SUPERVISOR} pattern=synexia:iop; algorithm=Initializes SUPERVISOR using the existing expression <code>new DAGPattern("Agent", "Supervisor coordinates worker agents")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:DELEGATION} pattern=synexia:iop; algorithm=Initializes DELEGATION using the existing expression <code>new DAGPattern("Agent", "Delegate subtask to another agent")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:REFLECTION} pattern=synexia:iop; algorithm=Initializes REFLECTION using the existing expression <code>new DAGPattern("Agent", "Agent reflects on own output")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:MEMORY_RETRIEVAL} pattern=synexia:iop; algorithm=Initializes MEMORY_RETRIEVAL using the existing expression <code>new DAGPattern("Agent", "Retrieve from long-term memory")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One DAGPattern enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:category} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The category declaration is a stored String field or record component with no initializer and no executable algorithm body.; algorithm.review=iop-annotation-contracts-declarations/reviewed-declarations.json#DAGPattern-4315; data.structures=One stored String field or component. The enum or record constructor retains the supplied reference directly, without validation or defensive copying.; description=Stored category descriptor in DAGPattern.
 * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration is a stored String field or record component with no initializer and no executable algorithm body.; algorithm.review=iop-annotation-contracts-declarations/reviewed-declarations.json#DAGPattern-4350; data.structures=One stored String field or component. The enum or record constructor retains the supplied reference directly, without validation or defensive copying.; description=Stored description descriptor in DAGPattern.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/DAGPattern.java}
 * blob {@code 3eec6144e1a9fe663e1a51e39fe53280cdda560d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum DAGPattern {

    // --- BASIC FLOW ---
    /** Sequential pipeline. */
    PIPELINE("Basic", "Execute steps in sequence"),
    /** Parallel execution. */
    PARALLEL("Basic", "Execute steps concurrently"),
    /** Fork-join. */
    FORK_JOIN("Basic", "Fork to parallel, join results"),
    /** Branch. */
    BRANCH("Basic", "Conditional execution path"),
    /** Loop. */
    LOOP("Basic", "Repeat until condition"),
    /** Merge. */
    MERGE("Basic", "Combine multiple paths"),

    // --- ORCHESTRATION ---
    /** Saga pattern. */
    SAGA("Orchestration", "Distributed transaction with compensation"),
    /** Choreography. */
    CHOREOGRAPHY("Orchestration", "Decentralized coordination via events"),
    /** Orchestration. */
    ORCHESTRATION("Orchestration", "Centralized coordinator controls flow"),
    /** ReAct loop. */
    REACT_LOOP("Orchestration", "Reason-Act-Observe loop for agents"),
    /** Plan-Execute. */
    PLAN_EXECUTE("Orchestration", "Plan steps then execute"),

    // --- DATA FLOW ---
    /** Map-reduce. */
    MAP_REDUCE("DataFlow", "Map function then reduce results"),
    /** Scatter-gather. */
    SCATTER_GATHER("DataFlow", "Scatter to workers, gather results"),
    /** Splitter. */
    SPLITTER("DataFlow", "Split input into multiple outputs"),
    /** Aggregator. */
    AGGREGATOR("DataFlow", "Aggregate multiple inputs into one"),
    /** Router. */
    ROUTER("DataFlow", "Route to destination based on content"),
    /** Filter. */
    FILTER("DataFlow", "Filter items based on predicate"),
    /** Transform. */
    TRANSFORM("DataFlow", "Transform item to different form"),

    // --- ERROR HANDLING ---
    /** Compensation. */
    COMPENSATION("ErrorHandling", "Undo previous steps on failure"),
    /** Retry step. */
    RETRY_STEP("ErrorHandling", "Retry failed step"),
    /** Skip step. */
    SKIP_STEP("ErrorHandling", "Skip failed step and continue"),
    /** Error channel. */
    ERROR_CHANNEL("ErrorHandling", "Route errors to separate handler"),
    /** Dead letter. */
    DEAD_LETTER("ErrorHandling", "Store unprocessable items"),

    // --- STATE MANAGEMENT ---
    /** Checkpoint. */
    CHECKPOINT("State", "Save progress for recovery"),
    /** Resume. */
    RESUME("State", "Resume from checkpoint"),
    /** State machine. */
    STATE_MACHINE("State", "Manage states and transitions"),
    /** Event-driven. */
    EVENT_DRIVEN("State", "React to events"),

    // --- SCHEDULING ---
    /** Batch. */
    BATCH("Scheduling", "Process items in batches"),
    /** Windowing. */
    WINDOWING("Scheduling", "Process items in time/count windows"),
    /** Rate limiting. */
    RATE_LIMITING("Scheduling", "Control processing rate"),
    /** Priority queue. */
    PRIORITY_QUEUE("Scheduling", "Process by priority"),
    /** Delay. */
    DELAY("Scheduling", "Delay step execution"),
    /** Timeout. */
    DAG_TIMEOUT("Scheduling", "Timeout step if too slow"),

    // --- AGENT PATTERNS ---
    /** Tool use. */
    TOOL_USE("Agent", "Agent uses tools to accomplish task"),
    /** Multi-agent. */
    MULTI_AGENT("Agent", "Multiple agents collaborate"),
    /** Supervisor. */
    SUPERVISOR("Agent", "Supervisor coordinates worker agents"),
    /** Delegation. */
    DELEGATION("Agent", "Delegate subtask to another agent"),
    /** Reflection. */
    REFLECTION("Agent", "Agent reflects on own output"),
    /** Memory retrieval. */
    MEMORY_RETRIEVAL("Agent", "Retrieve from long-term memory");

    private final String category;
    private final String description;

    /**
     * DAGPattern descriptor reference constructor.
     * @m3.pattern synexia:iop
     * @m3.algorithm Assigns the supplied category, description references directly to category, description fields in source order, without validation or defensive copying.
     * @m3.dataStructures Constructor parameters and corresponding final descriptor fields: category, description.
     */
    DAGPattern(String category, String description) {
        this.category = category;
        this.description = description;
    }

    /** PatternKind category.
     * @m3.pattern synexia:iop
     * @m3.algorithm Returns the stored category reference directly, without copying, validating or performing the named pattern or annotation behavior.
     * @m3.dataStructures One final String reference. ElementType arrays remain shared mutable aliases where present.
     */
    public String category() {
        return category;
    }

    /** PatternKind description.
     * @m3.pattern synexia:iop
     * @m3.algorithm Returns the stored description reference directly, without copying, validating or performing the named pattern or annotation behavior.
     * @m3.dataStructures One final String reference. ElementType arrays remain shared mutable aliases where present.
     */
    public String description() {
        return description;
    }
}
