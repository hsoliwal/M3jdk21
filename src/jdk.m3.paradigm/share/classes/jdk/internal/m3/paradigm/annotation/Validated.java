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
 * Validated - Triggers validation on method parameters.
 *
 * <h2>From Grand Java Patterns Proof</h2>
 * <p>
 * Marks a class or method for validation. Works with Jakarta Validation
 * annotations to enforce constraints.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;Service
 *     {@code @Validated}
 *     public class UserService {
 *
 *         public User createUser(@NotNull @Valid User user) {
 *             // user is guaranteed to be valid
 *         }
 *     }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Validation
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Validated.java}
 * blob {@code a6f31b8f32143460a6661180a179ab8ba4e4375e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface Validated {

    /**
     * Validation groups to apply.
     *
     * @return validation group classes
     */
    Class<?>[] value() default {};
}
