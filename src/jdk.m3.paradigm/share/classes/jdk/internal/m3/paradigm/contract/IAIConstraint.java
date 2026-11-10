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
 * AIConstraint - AnOP annotation for domain constraint hints.
 *
 * <h2>AnOP Advisory: Constraint Hints</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Constraints describe INVARIANTS that must hold.                          ║
 * ║  Constraints are HINTS for validation code generation.                    ║
 * ║  Actual enforcement is generated, not magical.                            ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;ISpec(value = "Order entity")
 * 	&#64;AIConstraint(name = "POSITIVE_TOTAL", expression = "total >= 0", type = Type.INVARIANT)
 * 	&#64;AIConstraint(name = "ITEMS_NOT_EMPTY", expression = "items.size() > 0", type = Type.PRECONDITION)
 * 	public interface IOrder&lt;Self extends IOrder&lt;Self>> extends IContext&lt;Self> {
 *
 * 		BigDecimal getTotal();
 *
 * 		List&lt;OrderItem> getItems();
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Constraint Hints
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares constraint name, required expression, evaluation-phase hint, message and severity without checking expressions or enforcing constraints.
 * @m3.dataStructures SOURCE-retained annotation with exact target {@code {ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER}}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIConstraint.java}
 * blob {@code 179da4c68928d3ac5a724a3e4f323c04db6f19c3} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(IAIConstraints.class)
public @interface IAIConstraint {

    /**
     * Constraint name/identifier.
     *
     * @return constraint name
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares name metadata for Constraint name/identifier. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String name() default "";

    /**
     * Constraint expression (pseudo-code or natural language).
     *
     * @return constraint expression
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares expression metadata for Constraint expression (pseudo-code or natural language). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, required explicitly with no default value.
     */
    String expression();

    /**
     * Constraint type (when to check).
     *
     * @return constraint type
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares type metadata for Constraint type (when to check). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Type}, with exact default {@code Type.INVARIANT}.
     */
    Type type() default Type.INVARIANT;

    /**
     * Error message when constraint is violated.
     *
     * @return error message
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares message metadata for Error message when constraint is violated. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String message() default "";

    /**
     * Severity level.
     *
     * @return severity
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares severity metadata for Severity level. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Severity}, with exact default {@code Severity.ERROR}.
     */
    Severity severity() default Severity.ERROR;

    /** Constraint types (Design by Contract).
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 9 symbolic constraint evaluation-phase hints for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIConstraint.Type enum with constants in existing declaration order.
     */
    enum Type {
        // --- DESIGN BY CONTRACT ---

        /** Must be true before method execution.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the PRECONDITION metadata choice: Must be true before method execution. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        PRECONDITION,

        /** Must be true after method execution.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the POSTCONDITION metadata choice: Must be true after method execution. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        POSTCONDITION,

        /** Must always be true (class invariant).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the INVARIANT metadata choice: Must always be true (class invariant). Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        INVARIANT,

        // --- VALIDATION ---

        /** Input validation.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the INPUT metadata choice: Input validation. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        INPUT,

        /** Output validation.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the OUTPUT metadata choice: Output validation. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        OUTPUT,

        /** State validation.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the STATE metadata choice: State validation. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        STATE,

        // --- TEMPORAL ---

        /** Must be true during execution.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the DURING metadata choice: Must be true during execution. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        DURING,

        /** Must eventually become true.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the EVENTUALLY metadata choice: Must eventually become true. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        EVENTUALLY,

        /** Must always remain true.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the ALWAYS metadata choice: Must always remain true. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        ALWAYS
    }

    /** Constraint violation severity.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 4 symbolic constraint severity hints for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIConstraint.Severity enum with constants in existing declaration order.
     */
    enum Severity {
        /** Informational only.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the INFO metadata choice: Informational only. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        INFO,

        /** Warning - continue execution.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the WARNING metadata choice: Warning - continue execution. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        WARNING,

        /** Error - stop execution.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the ERROR metadata choice: Error - stop execution. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        ERROR,

        /** Fatal - system cannot recover.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the FATAL metadata choice: Fatal - system cannot recover. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        FATAL
    }
}
