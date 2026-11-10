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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIRuleEngine - AnOP annotation for rule engine integration hints.
 *
 * <h2>AnOP Advisory: Rule Engine Hints</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Rule engines execute declarative rules at runtime.                       ║
 * ║  This annotation hints HOW to structure rule evaluation.                  ║
 * ║  It does NOT specify WHICH engine to use (that's classpath).              ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * &#64;ISpec(value = "Discount calculation engine")
 * &#64;AIRuleEngine(style = Style.FORWARD_CHAINING, execution = Execution.SEQUENTIAL, conflict = ConflictResolution.PRIORITY)
 * public interface IDiscountEngine<Self extends IDiscountEngine<Self>> extends IContext<Self> {
 *
 * 	DiscountResult evaluate(Order order, Customer customer);
 * }
 *
 * @AIRuleEngine(style = Style.DECISION_TABLE, format = Format.SPREADSHEET)
 * RiskLevel assessRisk(Application application);
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Rule Engine Hints
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares evaluation style, execution mode, conflict resolution, format and statefulness hints without evaluating rules or provisioning an engine.
 * @m3.dataStructures SOURCE-retained annotation with exact target {@code {ElementType.TYPE, ElementType.METHOD}}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIRuleEngine.java}
 * blob {@code 09b561011de56588eec8d9cf0c3fda44ee8cc1e4} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIRuleEngine {

    /**
     * Rule evaluation style.
     *
     * @return evaluation style
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares style metadata for Rule evaluation style. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Style}, with exact default {@code Style.FORWARD_CHAINING}.
     */
    Style style() default Style.FORWARD_CHAINING;

    /**
     * Rule execution mode.
     *
     * @return execution mode
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares execution metadata for Rule execution mode. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Execution}, with exact default {@code Execution.ALL_RULES}.
     */
    Execution execution() default Execution.ALL_RULES;

    /**
     * Conflict resolution strategy.
     *
     * @return conflict resolution
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares conflict metadata for Conflict resolution strategy. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code ConflictResolution}, with exact default {@code ConflictResolution.PRIORITY}.
     */
    ConflictResolution conflict() default ConflictResolution.PRIORITY;

    /**
     * Rule format hint.
     *
     * @return format
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares format metadata for Rule format hint. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Format}, with exact default {@code Format.CODE}.
     */
    Format format() default Format.CODE;

    /**
     * Stateful session hint.
     *
     * @return true for stateful
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares stateful metadata for Stateful session hint. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}, with exact default {@code false}.
     */
    boolean stateful() default false;

    /** Rule evaluation styles.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 9 symbolic rule evaluation-style hints for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIRuleEngine.Style enum with constants in existing declaration order.
     */
    enum Style {
        // --- INFERENCE STYLES ---

        /** Forward chaining (data-driven, Rete algorithm).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the FORWARD_CHAINING metadata choice: Forward chaining (data-driven, Rete algorithm). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        FORWARD_CHAINING,

        /** Backward chaining (goal-driven, Prolog-style).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the BACKWARD_CHAINING metadata choice: Backward chaining (goal-driven, Prolog-style). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        BACKWARD_CHAINING,

        /** Hybrid (both forward and backward).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the HYBRID metadata choice: Hybrid (both forward and backward). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        HYBRID,

        // --- DECLARATIVE STYLES ---

        /** Decision table (matrix of conditions/actions).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the DECISION_TABLE metadata choice: Decision table (matrix of conditions/actions). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        DECISION_TABLE,

        /** Decision tree (hierarchical if-then).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the DECISION_TREE metadata choice: Decision tree (hierarchical if-then). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        DECISION_TREE,

        /** Scorecard (weighted scoring).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SCORECARD metadata choice: Scorecard (weighted scoring). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        SCORECARD,

        // --- SIMPLE STYLES ---

        /** Simple condition-action rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CONDITION_ACTION metadata choice: Simple condition-action rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        CONDITION_ACTION,

        /** Production rules (WHEN-THEN).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the PRODUCTION metadata choice: Production rules (WHEN-THEN). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        PRODUCTION,

        /** Event-condition-action (ECA).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the ECA metadata choice: Event-condition-action (ECA). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        ECA
    }

    /** Rule execution modes.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 6 symbolic rule execution-mode hints for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIRuleEngine.Execution enum with constants in existing declaration order.
     */
    enum Execution {
        /** Execute all matching rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the ALL_RULES metadata choice: Execute all matching rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        ALL_RULES,

        /** Execute first matching rule only.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the FIRST_MATCH metadata choice: Execute first matching rule only. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        FIRST_MATCH,

        /** Execute until agenda is empty.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the UNTIL_HALT metadata choice: Execute until agenda is empty. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        UNTIL_HALT,

        /** Execute N rules maximum.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the LIMITED metadata choice: Execute N rules maximum. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        LIMITED,

        /** Execute in parallel.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the PARALLEL metadata choice: Execute in parallel. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        PARALLEL,

        /** Execute sequentially.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SEQUENTIAL metadata choice: Execute sequentially. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        SEQUENTIAL
    }

    /** Conflict resolution strategies.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 6 symbolic rule conflict-resolution hints for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIRuleEngine.ConflictResolution enum with constants in existing declaration order.
     */
    enum ConflictResolution {
        /** Higher priority wins.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the PRIORITY metadata choice: Higher priority wins. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        PRIORITY,

        /** Most specific rule wins.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SPECIFICITY metadata choice: Most specific rule wins. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        SPECIFICITY,

        /** Most recently added wins.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the RECENCY metadata choice: Most recently added wins. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        RECENCY,

        /** First loaded wins.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the LOAD_ORDER metadata choice: First loaded wins. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        LOAD_ORDER,

        /** Random selection.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the RANDOM metadata choice: Random selection. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        RANDOM,

        /** All conflicting rules fire.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the NO_RESOLUTION metadata choice: All conflicting rules fire. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        NO_RESOLUTION
    }

    /** Rule format hints.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 6 symbolic rule representation hints for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIRuleEngine.Format enum with constants in existing declaration order.
     */
    enum Format {
        /** Rules in code (Java, annotations).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CODE metadata choice: Rules in code (Java, annotations). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        CODE,

        /** Rules in DSL (Drools DRL, etc.).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the DSL metadata choice: Rules in DSL (Drools DRL, etc.). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        DSL,

        /** Rules in spreadsheet (Excel, CSV).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SPREADSHEET metadata choice: Rules in spreadsheet (Excel, CSV). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        SPREADSHEET,

        /** Rules in JSON/YAML.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the JSON metadata choice: Rules in JSON/YAML. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        JSON,

        /** Rules in database tables.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the DATABASE metadata choice: Rules in database tables. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        DATABASE,

        /** Rules in natural language.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the NATURAL_LANGUAGE metadata choice: Rules in natural language. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        NATURAL_LANGUAGE
    }
}
