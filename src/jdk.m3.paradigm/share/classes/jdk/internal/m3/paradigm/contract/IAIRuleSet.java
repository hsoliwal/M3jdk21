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
 * AIRuleSet - AnOP annotation for rule grouping hints.
 *
 * <h2>AnOP Advisory: Rule Set Organization</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Rule sets group related rules for execution.                            ║
 * ║  Maps to: Drools KieBase, Easy Rules RulesEngine, OpenL Tablets.         ║
 * ║                                                                           ║
 * ║  CRITICAL: AnOP does NOT execute rules. It declares organization.        ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Library Mapping</h2>
 * <ul>
 * <li>Drools: @AIRuleSet → KieBase / KieSession</li>
 * <li>Easy Rules: @AIRuleSet → RulesEngine</li>
 * <li>OpenL Tablets: @AIRuleSet → Decision Table</li>
 * <li>DMN: @AIRuleSet → DRD (Decision Requirements Diagram)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 * &#64;AIRuleSet(
 *     name = "pricing-rules",
 *     description = "Product pricing calculation rules",
 *     engine = Engine.DROOLS,
 *     order = Order.PRIORITY_BASED
 * )
 * public class PricingRuleSet {
 *
 *     &#64;AIBusinessRule(name = "BULK_DISCOUNT", ...)
 *     &#64;AIRuleFact(type = Order.class)
 *     public void bulkDiscount(Order order) { }
 *
 *     &#64;AIBusinessRule(name = "LOYALTY_DISCOUNT", ...)
 *     public void loyaltyDiscount(Customer customer, Order order) { }
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Rule Set Hints
 * @see IAIBusinessRule
 * @see IAIRuleEngine
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares rule-set name, description, engine, ordering, execution, namespace, version and enabled-state hints without loading or executing rules.
 * @m3.dataStructures SOURCE-retained annotation with exact target {@code ElementType.TYPE}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIRuleSet.java}
 * blob {@code 112e432c91689e0482e02bb11f6e9cad8cb1a45f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIRuleSet {

    /**
     * Rule set name/identifier.
     *
     * @return name
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares name metadata for Rule set name/identifier. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, required explicitly with no default value.
     */
    String name();

    /**
     * Rule set description.
     *
     * @return description
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares description metadata for Rule set description. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String description() default "";

    /**
     * Target rule engine.
     *
     * @return engine
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares engine metadata for Target rule engine. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Engine}, with exact default {@code Engine.UNSPECIFIED}.
     */
    Engine engine() default Engine.UNSPECIFIED;

    /**
     * Rule ordering strategy.
     *
     * @return order
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares order metadata for Rule ordering strategy. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Order}, with exact default {@code Order.NATURAL}.
     */
    Order order() default Order.NATURAL;

    /**
     * Execution mode.
     *
     * @return mode
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares mode metadata for Execution mode. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Mode}, with exact default {@code Mode.FIRE_ALL}.
     */
    Mode mode() default Mode.FIRE_ALL;

    /**
     * Package/namespace for rules.
     *
     * @return package name
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares rulePackage metadata for Package/namespace for rules. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String rulePackage() default "";

    /**
     * Version of the rule set.
     *
     * @return version
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares version metadata for Version of the rule set. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code "1.0"}.
     */
    String version() default "1.0";

    /**
     * Enabled flag.
     *
     * @return true if enabled
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares enabled metadata for Enabled flag. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}, with exact default {@code true}.
     */
    boolean enabled() default true;

    /** Rule engines supported.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 12 symbolic rule-engine choices for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIRuleSet.Engine enum with constants in existing declaration order.
     */
    enum Engine {
        /** Unspecified - use project default.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the UNSPECIFIED metadata choice: Unspecified - use project default. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        UNSPECIFIED,

        // --- JAVA RULE ENGINES ---

        /** Drools (Red Hat). Maven: org.drools:drools-core
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the DROOLS metadata choice: Drools (Red Hat). Maven: org.drools:drools-core Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        DROOLS,

        /** Easy Rules. Maven: org.jeasy:easy-rules-core
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the EASY_RULES metadata choice: Easy Rules. Maven: org.jeasy:easy-rules-core Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        EASY_RULES,

        /** OpenL Tablets. Maven: org.openl.rules:org.openl.rules
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the OPENL_TABLETS metadata choice: OpenL Tablets. Maven: org.openl.rules:org.openl.rules Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        OPENL_TABLETS,

        /** RuleBook. Maven: com.deliveredtechnologies:rulebook-core
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the RULEBOOK metadata choice: RuleBook. Maven: com.deliveredtechnologies:rulebook-core Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        RULEBOOK,

        /** jBPM (process + rules). Maven: org.jbpm:jbpm-flow
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the JBPM metadata choice: jBPM (process + rules). Maven: org.jbpm:jbpm-flow Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        JBPM,

        /** Camunda DMN. Maven: org.camunda.bpm.dmn:camunda-engine-dmn
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CAMUNDA_DMN metadata choice: Camunda DMN. Maven: org.camunda.bpm.dmn:camunda-engine-dmn Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        CAMUNDA_DMN,

        /** Flowable DMN. Maven: org.flowable:flowable-dmn-engine
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the FLOWABLE_DMN metadata choice: Flowable DMN. Maven: org.flowable:flowable-dmn-engine Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        FLOWABLE_DMN,

        // --- STANDARDS ---

        /** DMN (Decision Model and Notation). Standard-based, vendor-neutral.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the DMN metadata choice: DMN (Decision Model and Notation). Standard-based, vendor-neutral. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        DMN,

        /** JSR-94 (Java Rule Engine API). Legacy standard.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the JSR_94 metadata choice: JSR-94 (Java Rule Engine API). Legacy standard. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        JSR_94,

        // --- LIGHTWEIGHT ---

        /** Custom/embedded (simple if-then).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CUSTOM metadata choice: Custom/embedded (simple if-then). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        CUSTOM,

        /** Expression Language (SpEL, MVEL, JEXL).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the EXPRESSION_LANGUAGE metadata choice: Expression Language (SpEL, MVEL, JEXL). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        EXPRESSION_LANGUAGE
    }

    /** Rule ordering strategies.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 6 symbolic rule ordering hints for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIRuleSet.Order enum with constants in existing declaration order.
     */
    enum Order {
        /** Natural order (as defined).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the NATURAL metadata choice: Natural order (as defined). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        NATURAL,

        /** Priority-based (higher first).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the PRIORITY_BASED metadata choice: Priority-based (higher first). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        PRIORITY_BASED,

        /** Salience-based (Drools).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SALIENCE metadata choice: Salience-based (Drools). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        SALIENCE,

        /** Alphabetical by name.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the ALPHABETICAL metadata choice: Alphabetical by name. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        ALPHABETICAL,

        /** Random order.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the RANDOM metadata choice: Random order. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        RANDOM,

        /** Dependency order (prerequisites first).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the DEPENDENCY metadata choice: Dependency order (prerequisites first). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        DEPENDENCY
    }

    /** Execution modes.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 5 symbolic rule execution hints for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIRuleSet.Mode enum with constants in existing declaration order.
     */
    enum Mode {
        /** Fire all matching rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the FIRE_ALL metadata choice: Fire all matching rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        FIRE_ALL,

        /** Fire until agenda empty.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the FIRE_UNTIL_HALT metadata choice: Fire until agenda empty. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        FIRE_UNTIL_HALT,

        /** Fire first match only.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the FIRST_MATCH metadata choice: Fire first match only. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        FIRST_MATCH,

        /** Fire N rules max.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the LIMITED metadata choice: Fire N rules max. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        LIMITED,

        /** Fire all, collect results.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the COLLECT_RESULTS metadata choice: Fire all, collect results. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        COLLECT_RESULTS
    }
}
