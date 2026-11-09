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

package jdk.internal.m3.paradigm.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * InvariantCheck - Marks method for invariant verification via AOP.
 *
 * <h2>From Grand Java Patterns Proof</h2>
 * <p>
 * This annotation enables aspect-oriented invariant checking. The framework
 * verifies conservation laws and domain constraints after method execution.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * public class Simulator<S extends SystemState> {
 *
 *     {@code @InvariantCheck}
 *     public S step(S state) {
 *         // After execution, AOP verifies conservation laws
 *         return evolve(state);
 *     }
 * }
 * }</pre>
 *
 * <h2>AnOP Principle</h2>
 * <p>
 * Annotations must behave like language modifiers, not frameworks.
 * InvariantCheck declares intent for verification without exposing the
 * verification mechanism.
 * </p>
 *
 * @since ContextOS 1.0 - AnOP AOP
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/InvariantCheck.java}
 * blob {@code f9da740a9620a3ffe60a264b09ed01dc89f395cc} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface InvariantCheck {

    /**
     * Description of the invariant being checked.
     *
     * @return invariant description
     */
    String value() default "";

    /**
     * Whether to fail fast on violation.
     *
     * @return true to throw on violation
     */
    boolean failFast() default true;
}
