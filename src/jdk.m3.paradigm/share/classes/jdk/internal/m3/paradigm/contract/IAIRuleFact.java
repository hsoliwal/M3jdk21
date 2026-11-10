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
 * &#64;AIRuleFact(name = "order", mutable = false, events = {"orderCreated", "orderUpdated"})
 * public record Order(String id, BigDecimal total, List<Item> items) {
 * }
 *
 * // Mark a parameter as receiving a fact
 * void applyDiscount(@AIRuleFact(name = "order") Order order) {
 * }
 *
 * // Fact with lifecycle
 * @AIRuleFact(name = "session", lifecycle = Lifecycle.REQUEST_SCOPED, events = {"sessionStart", "sessionEnd"})
 * public class UserSession {
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Rule Fact Hints
 * @see IAIRuleSet
 * @see IAIBusinessRule
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares fact identity, type, mutability, events, lifecycle, temporal and expiration hints without inserting facts into an engine or implementing expiration.
 * @m3.dataStructures SOURCE-retained annotation with exact target {@code {ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD}}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIRuleFact.java}
 * blob {@code 9e47b09cf3897191f4cb6fb53974c0e7408e0f2b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(IAIRuleFacts.class)
public @interface IAIRuleFact {

    /**
     * Fact name/identifier for rule binding.
     *
     * @return fact name
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares name metadata for Fact name/identifier for rule binding. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String name() default "";

    /**
     * Fact type (if not inferred from annotated element).
     *
     * @return fact type
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares type metadata for Fact type (if not inferred from annotated element). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}, with exact default {@code Object.class}.
     */
    Class<?> type() default Object.class;

    /**
     * Fact is mutable (can be modified by rules).
     *
     * @return true if mutable
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares mutable metadata for Fact is mutable (can be modified by rules). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}, with exact default {@code true}.
     */
    boolean mutable() default true;

    /**
     * Events that affect this fact (for CEP - Complex Event Processing).
     *
     * @return event names
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares events metadata for Events that affect this fact (for CEP - Complex Event Processing). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}, with exact default {@code {}}.
     */
    String[] events() default {};

    /**
     * Fact lifecycle scope.
     *
     * @return lifecycle
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares lifecycle metadata for Fact lifecycle scope. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Lifecycle}, with exact default {@code Lifecycle.STATELESS}.
     */
    Lifecycle lifecycle() default Lifecycle.STATELESS;

    /**
     * Time window for temporal facts (CEP). Format: ISO-8601 duration (e.g., "PT5M"
     * for 5 minutes).
     *
     * @return time window
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares timeWindow metadata for Time window for temporal facts (CEP). Format: ISO-8601 duration (e.g., "PT5M" for 5 minutes). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String timeWindow() default "";

    /**
     * Fact is an event (for CEP).
     *
     * @return true if event
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares event metadata for Fact is an event (for CEP). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}, with exact default {@code false}.
     */
    boolean event() default false;

    /**
     * Timestamp field for events.
     *
     * @return timestamp field name
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares timestampField metadata for Timestamp field for events. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String timestampField() default "";

    /**
     * Expiration duration for events.
     *
     * @return expiration duration
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares expires metadata for Expiration duration for events. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String expires() default "";

    /**
     * Fact description.
     *
     * @return description
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares description metadata for Fact description. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String description() default "";

    /** Fact lifecycle scopes.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 5 symbolic rule fact lifecycle hints for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIRuleFact.Lifecycle enum with constants in existing declaration order.
     */
    enum Lifecycle {
        /** Stateless - no session affinity.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the STATELESS metadata choice: Stateless - no session affinity. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        STATELESS,

        /** Request-scoped - lives for one rule evaluation.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the REQUEST_SCOPED metadata choice: Request-scoped - lives for one rule evaluation. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        REQUEST_SCOPED,

        /** Session-scoped - lives for session duration.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SESSION_SCOPED metadata choice: Session-scoped - lives for session duration. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        SESSION_SCOPED,

        /** Long-lived - persists across sessions.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the PERSISTENT metadata choice: Long-lived - persists across sessions. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        PERSISTENT,

        /** Temporal - has time-based lifecycle (CEP).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the TEMPORAL metadata choice: Temporal - has time-based lifecycle (CEP). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        TEMPORAL
    }
}
