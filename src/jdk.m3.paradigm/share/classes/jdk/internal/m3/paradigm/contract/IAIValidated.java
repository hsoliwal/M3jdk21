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
 * AIValidated - AnOP annotation for validation hints.
 *
 * <h2>Spring/JSR-303 Inspiration, IOP Authority</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ SPRING: @Validated → runtime validation interception ║
 * ║ AnOP: @AIValidated → compile-time validation declaration ║
 * ║ ║
 * ║ Spring says: "I will validate this before method execution" ║
 * ║ AnOP says: "This parameter/type requires validation" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT run validators. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * &#64;AIValidated
 * void createUser(@AIValidated User user) {
 * 	// LLM knows: User fields must pass JSR-303 constraints
 * }
 *
 * @AIValidated(groups = {OnCreate.class})
 * void save(Entity entity) {
 * 	// LLM knows: validation with specific group
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Validation Hints (Spring/JSR-303-inspired)
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/validation/annotation/Validated.html">Spring @Validated</a>
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares validation groups, fail-fast flag, mode and validator hints without invoking validators or enforcing constraints.
 * @m3.dataStructures SOURCE-retained annotation with exact target {@code {ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD}}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIValidated.java}
 * blob {@code ffc6a9d6424811c71cbb5c4651d188c155911d02} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIValidated {

    /**
     * Validation groups to apply.
     *
     * @return validation groups
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares groups metadata for Validation groups to apply. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>[]}, with exact default {@code {}}.
     */
    Class<?>[] groups() default {};

    /**
     * Fail-fast mode (stop on first violation).
     *
     * @return true for fail-fast
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares failFast metadata for Fail-fast mode (stop on first violation). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}, with exact default {@code false}.
     */
    boolean failFast() default false;

    /**
     * Validation mode.
     *
     * @return mode
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares mode metadata for Validation mode. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Mode}, with exact default {@code Mode.CONSTRAINTS}.
     */
    Mode mode() default Mode.CONSTRAINTS;

    /**
     * Custom validator bean name hint.
     *
     * @return validator name
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares validator metadata for Custom validator bean name hint. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String validator() default "";

    /** Validation modes.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 4 symbolic validation mode hints for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIValidated.Mode enum with constants in existing declaration order.
     */
    enum Mode {
        /** Use JSR-303 constraints on the class.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CONSTRAINTS metadata choice: Use JSR-303 constraints on the class. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        CONSTRAINTS,

        /** Use custom validator.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CUSTOM metadata choice: Use custom validator. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        CUSTOM,

        /** Programmatic validation only.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the PROGRAMMATIC metadata choice: Programmatic validation only. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        PROGRAMMATIC,

        /** Both constraints and custom.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the BOTH metadata choice: Both constraints and custom. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        BOTH
    }
}
