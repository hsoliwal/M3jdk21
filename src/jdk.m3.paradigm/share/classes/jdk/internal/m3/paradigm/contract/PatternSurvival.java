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
 * PatternKind Survival Classification.
 *
 * <p>
 * Classifies patterns based on their applicability in modern FOP-first,
 * event-driven, context-aware, distributed architectures.
 *
 * <p>
 * From the Grand Java Patterns Proof analysis:
 *
 * <ul>
 * <li>ALWAYS_HOLD - Structurally invariant, mathematically necessary
 * <li>TRANSFORM - Hold but change shape in modern systems
 * <li>COLLAPSE - Disappear into better abstractions
 * <li>NOT_APPLICABLE - Historical artifacts, anti-patterns in modern systems
 * </ul>
 * @m3.pattern synexia:iop
 * @m3.algorithm Declare four authored applicability classifications; the labels express a taxonomy without proving invariance or automatically classifying a pattern.
 * @m3.dataStructures PatternSurvival enum singleton array in declaration order with final label fields.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/PatternSurvival.java}
 * blob {@code cfb73f1bb9c595dc77f56ee6b30599a192c94b3f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum PatternSurvival {

    /**
     * Patterns that always hold - structurally invariant.
     *
     * <p>
     * These patterns correspond to mathematical or physical necessities. They do
     * not disappear, regardless of paradigm.
     *
     * <p>
     * Examples: Factory, Abstract Factory, Chain of Responsibility, Interpreter,
     * Flyweight, Adapter
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify PatternSurvival#ALWAYS_HOLD with description equal to Structurally invariant - mathematically necessary; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One PatternSurvival enum singleton with its exact constructor literals.
     */
    ALWAYS_HOLD("Structurally invariant - mathematically necessary"),

    /**
     * Patterns that hold but transform.
     *
     * <p>
     * These patterns don't disappear - they change shape. The concept survives but
     * implementation changes.
     *
     * <p>
     * Examples: Singleton (→ Scoped Authority), Builder (→ Records + validation),
     * Observer (→ Signal subscribers), Mediator
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify PatternSurvival#TRANSFORM with description equal to Hold but change shape in modern systems; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One PatternSurvival enum singleton with its exact constructor literals.
     */
    TRANSFORM("Hold but change shape in modern systems"),

    /**
     * Patterns that collapse into better forms.
     *
     * <p>
     * These patterns are symptoms of weaker abstractions. They disappear when their
     * underlying problem is eliminated.
     *
     * <p>
     * Examples: Memento (→ Event log), Decorator (→ Function composition), Document
     * Builder (→ Declarative schemas)
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify PatternSurvival#COLLAPSE with description equal to Disappear into better abstractions; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One PatternSurvival enum singleton with its exact constructor literals.
     */
    COLLAPSE("Disappear into better abstractions"),

    /**
     * Patterns that do not hold strongly.
     *
     * <p>
     * Historical artifacts, not fundamental. Anti-patterns in modern systems.
     *
     * <p>
     * Examples: Service Locator, Shared Database, Global Mutable Singleton
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify PatternSurvival#NOT_APPLICABLE with description equal to Anti-pattern in modern distributed systems; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One PatternSurvival enum singleton with its exact constructor literals.
     */
    NOT_APPLICABLE("Anti-pattern in modern distributed systems");

    /**
     * Store PatternSurvival.description once from the matching constructor argument without validation or transformation.
     * @m3.pattern synexia:iop
     * @m3.algorithm Store PatternSurvival.description once from the matching constructor argument without validation or transformation.
     * @m3.dataStructures Final String reference for the description label.
     */
    private final String description;

    /**
     * Initialize PatternSurvival by directly assigning each constructor argument to its matching final field, preserving label references and any explicit order integer.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initialize PatternSurvival by directly assigning each constructor argument to its matching final field, preserving label references and any explicit order integer.
     * @m3.dataStructures Constructor arguments and final PatternSurvival instance fields.
     */
    PatternSurvival(String description) {
        this.description = description;
    }

    /** Classification description.
     * @m3.pattern synexia:iop
     * @m3.algorithm Return this PatternSurvival singleton's stored description value unchanged.
     * @m3.dataStructures Existing final description String reference; no allocation or transformation.
     */
    public String description() {
        return description;
    }
}
