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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIRuleFact - AnOP annotation for rule engine fact hints.
 *
 * <h2>AnOP Advisory: Rule Facts</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Facts are the data objects that rules evaluate.                         ║
 * ║  Maps to: Drools Fact, Easy Rules Fact, working memory objects.          ║
 * ║                                                                           ║
 * ║  CRITICAL: AnOP does NOT insert facts. It declares data contracts.       ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Library Mapping</h2>
 * <ul>
 * <li>Drools: Facts inserted into KieSession working memory</li>
 * <li>Easy Rules: Facts object passed to RulesEngine.fire()</li>
 * <li>DMN: Input data for decision evaluation</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * // Mark a class as a rule fact
 * &#64;AIRuleFact(name = "order", mutable = false, events = {"orderCreated",
 *         "orderUpdated"})
 * public record Order(String id, BigDecimal total, List<Item> items) {
 * }
 *
 * // Mark a parameter as receiving a fact
 * void applyDiscount(@AIRuleFact(name = "order") Order order) {
 * }
 *
 * // Fact with lifecycle
 * @AIRuleFact(name = "session", lifecycle = Lifecycle.REQUEST_SCOPED, events = {
 *         "sessionStart", "sessionEnd"})
 * public class UserSession {
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Rule Fact Hints
 * @see AIRuleSet
 * @see AIBusinessRule
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIRuleFact.java}
 * blob {@code d9e27dadc2eb144c584cb703e2a79d547c77e70d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(AIRuleFacts.class)
public @interface AIRuleFact {

    /**
     * Fact name/identifier for rule binding.
     *
     * @return fact name
     */
    String name() default "";

    /**
     * Fact type (if not inferred from annotated element).
     *
     * @return fact type
     */
    Class<?> type() default Object.class;

    /**
     * Fact is mutable (can be modified by rules).
     *
     * @return true if mutable
     */
    boolean mutable() default true;

    /**
     * Events that affect this fact (for CEP - Complex Event Processing).
     *
     * @return event names
     */
    String[] events() default {};

    /**
     * Fact lifecycle scope.
     *
     * @return lifecycle
     */
    Lifecycle lifecycle() default Lifecycle.STATELESS;

    /**
     * Time window for temporal facts (CEP). Format: ISO-8601 duration (e.g.,
     * "PT5M" for 5 minutes).
     *
     * @return time window
     */
    String timeWindow() default "";

    /**
     * Fact is an event (for CEP).
     *
     * @return true if event
     */
    boolean event() default false;

    /**
     * Timestamp field for events.
     *
     * @return timestamp field name
     */
    String timestampField() default "";

    /**
     * Expiration duration for events.
     *
     * @return expiration duration
     */
    String expires() default "";

    /**
     * Fact description.
     *
     * @return description
     */
    String description() default "";

    /** Fact lifecycle scopes. */
    enum Lifecycle {
        /** Stateless - no session affinity. */
        STATELESS,

        /** Request-scoped - lives for one rule evaluation. */
        REQUEST_SCOPED,

        /** Session-scoped - lives for session duration. */
        SESSION_SCOPED,

        /** Long-lived - persists across sessions. */
        PERSISTENT,

        /** Temporal - has time-based lifecycle (CEP). */
        TEMPORAL
    }
}
