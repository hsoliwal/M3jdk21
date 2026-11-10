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
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.internal.m3.paradigm.contract;

import java.lang.annotation.*;

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
 * @see AIBusinessRule
 * @see AIRuleEngine
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIRuleSet.java}
 * blob {@code f567d337424aca3c63f35987cd12e33d5602ab11} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIRuleSet {

    /**
     * Rule set name/identifier.
     *
     * @return name
     */
    String name();

    /**
     * Rule set description.
     *
     * @return description
     */
    String description() default "";

    /**
     * Target rule engine.
     *
     * @return engine
     */
    Engine engine() default Engine.UNSPECIFIED;

    /**
     * Rule ordering strategy.
     *
     * @return order
     */
    Order order() default Order.NATURAL;

    /**
     * Execution mode.
     *
     * @return mode
     */
    Mode mode() default Mode.FIRE_ALL;

    /**
     * Package/namespace for rules.
     *
     * @return package name
     */
    String rulePackage() default "";

    /**
     * Version of the rule set.
     *
     * @return version
     */
    String version() default "1.0";

    /**
     * Enabled flag.
     *
     * @return true if enabled
     */
    boolean enabled() default true;

    /** Rule engines supported. */
    enum Engine {
        /** Unspecified - use project default. */
        UNSPECIFIED,

        // --- JAVA RULE ENGINES ---

        /** Drools (Red Hat). Maven: org.drools:drools-core */
        DROOLS,

        /** Easy Rules. Maven: org.jeasy:easy-rules-core */
        EASY_RULES,

        /** OpenL Tablets. Maven: org.openl.rules:org.openl.rules */
        OPENL_TABLETS,

        /** RuleBook. Maven: com.deliveredtechnologies:rulebook-core */
        RULEBOOK,

        /** jBPM (process + rules). Maven: org.jbpm:jbpm-flow */
        JBPM,

        /** Camunda DMN. Maven: org.camunda.bpm.dmn:camunda-engine-dmn */
        CAMUNDA_DMN,

        /** Flowable DMN. Maven: org.flowable:flowable-dmn-engine */
        FLOWABLE_DMN,

        // --- STANDARDS ---

        /** DMN (Decision Model and Notation). Standard-based, vendor-neutral. */
        DMN,

        /** JSR-94 (Java Rule Engine API). Legacy standard. */
        JSR_94,

        // --- LIGHTWEIGHT ---

        /** Custom/embedded (simple if-then). */
        CUSTOM,

        /** Expression Language (SpEL, MVEL, JEXL). */
        EXPRESSION_LANGUAGE
    }

    /** Rule ordering strategies. */
    enum Order {
        /** Natural order (as defined). */
        NATURAL,

        /** Priority-based (higher first). */
        PRIORITY_BASED,

        /** Salience-based (Drools). */
        SALIENCE,

        /** Alphabetical by name. */
        ALPHABETICAL,

        /** Random order. */
        RANDOM,

        /** Dependency order (prerequisites first). */
        DEPENDENCY
    }

    /** Execution modes. */
    enum Mode {
        /** Fire all matching rules. */
        FIRE_ALL,

        /** Fire until agenda empty. */
        FIRE_UNTIL_HALT,

        /** Fire first match only. */
        FIRST_MATCH,

        /** Fire N rules max. */
        LIMITED,

        /** Fire all, collect results. */
        COLLECT_RESULTS
    }
}
