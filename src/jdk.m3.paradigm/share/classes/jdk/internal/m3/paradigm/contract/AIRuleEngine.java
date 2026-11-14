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
 * public interface IDiscountEngine {
 *
 *     DiscountResult evaluate(Order order, Customer customer);
 * }
 *
 * @AIRuleEngine(style = Style.DECISION_TABLE, format = Format.SPREADSHEET)
 * RiskLevel assessRisk(Application application);
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Rule Engine Hints
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIRuleEngine.java}
 * blob {@code 4318364ab8b0bb3d8327b91e0c01c9cf19f46795} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIRuleEngine {

    /**
     * Rule evaluation style.
     *
     * @return evaluation style
     */
    Style style() default Style.FORWARD_CHAINING;

    /**
     * Rule execution mode.
     *
     * @return execution mode
     */
    Execution execution() default Execution.ALL_RULES;

    /**
     * Conflict resolution strategy.
     *
     * @return conflict resolution
     */
    ConflictResolution conflict() default ConflictResolution.PRIORITY;

    /**
     * Rule format hint.
     *
     * @return format
     */
    Format format() default Format.CODE;

    /**
     * Stateful session hint.
     *
     * @return true for stateful
     */
    boolean stateful() default false;

    /** Rule evaluation styles. */
    enum Style {
        // --- INFERENCE STYLES ---

        /** Forward chaining (data-driven, Rete algorithm). */
        FORWARD_CHAINING,

        /** Backward chaining (goal-driven, Prolog-style). */
        BACKWARD_CHAINING,

        /** Hybrid (both forward and backward). */
        HYBRID,

        // --- DECLARATIVE STYLES ---

        /** Decision table (matrix of conditions/actions). */
        DECISION_TABLE,

        /** Decision tree (hierarchical if-then). */
        DECISION_TREE,

        /** Scorecard (weighted scoring). */
        SCORECARD,

        // --- SIMPLE STYLES ---

        /** Simple condition-action rules. */
        CONDITION_ACTION,

        /** Production rules (WHEN-THEN). */
        PRODUCTION,

        /** Event-condition-action (ECA). */
        ECA
    }

    /** Rule execution modes. */
    enum Execution {
        /** Execute all matching rules. */
        ALL_RULES,

        /** Execute first matching rule only. */
        FIRST_MATCH,

        /** Execute until agenda is empty. */
        UNTIL_HALT,

        /** Execute N rules maximum. */
        LIMITED,

        /** Execute in parallel. */
        PARALLEL,

        /** Execute sequentially. */
        SEQUENTIAL
    }

    /** Conflict resolution strategies. */
    enum ConflictResolution {
        /** Higher priority wins. */
        PRIORITY,

        /** Most specific rule wins. */
        SPECIFICITY,

        /** Most recently added wins. */
        RECENCY,

        /** First loaded wins. */
        LOAD_ORDER,

        /** Random selection. */
        RANDOM,

        /** All conflicting rules fire. */
        NO_RESOLUTION
    }

    /** Rule format hints. */
    enum Format {
        /** Rules in code (Java, annotations). */
        CODE,

        /** Rules in DSL (Drools DRL, etc.). */
        DSL,

        /** Rules in spreadsheet (Excel, CSV). */
        SPREADSHEET,

        /** Rules in JSON/YAML. */
        JSON,

        /** Rules in database tables. */
        DATABASE,

        /** Rules in natural language. */
        NATURAL_LANGUAGE
    }
}
