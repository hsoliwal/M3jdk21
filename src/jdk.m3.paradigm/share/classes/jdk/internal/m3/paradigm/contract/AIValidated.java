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
 *     // LLM knows: User fields must pass JSR-303 constraints
 * }
 *
 * @AIValidated(groups = {OnCreate.class})
 * void save(Entity entity) {
 *     // LLM knows: validation with specific group
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Validation Hints (Spring/JSR-303-inspired)
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/validation/annotation/Validated.html">Spring @Validated</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIValidated.java}
 * blob {@code e504798bb803f5d8141dc335bd380ce40e53e281} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIValidated {

    /**
     * Validation groups to apply.
     *
     * @return validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Fail-fast mode (stop on first violation).
     *
     * @return true for fail-fast
     */
    boolean failFast() default false;

    /**
     * Validation mode.
     *
     * @return mode
     */
    Mode mode() default Mode.CONSTRAINTS;

    /**
     * Custom validator bean name hint.
     *
     * @return validator name
     */
    String validator() default "";

    /** Validation modes. */
    enum Mode {
        /** Use JSR-303 constraints on the class. */
        CONSTRAINTS,

        /** Use custom validator. */
        CUSTOM,

        /** Programmatic validation only. */
        PROGRAMMATIC,

        /** Both constraints and custom. */
        BOTH
    }
}
