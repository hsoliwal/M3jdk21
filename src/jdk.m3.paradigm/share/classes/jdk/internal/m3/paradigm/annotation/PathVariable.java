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
 * PathVariable - Binds URI template variable to method parameter.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>{@code
 * &#64;GetMapping("/users/{id}")
 * public User findById(@PathVariable Long id) { ... }
 *
 * &#64;GetMapping("/users/{userId}/orders/{orderId}")
 * public Order findOrder(
 *     &#64;PathVariable("userId") Long userId,
 *     &#64;PathVariable("orderId") Long orderId) { ... }
 * }</pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/PathVariable.java}
 * blob {@code 3a617b3580b50586730ed59c7ef71fc8019089ed} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface PathVariable {

    /**
     * Path variable name.
     *
     * @return variable name
     */
    String value() default "";

    /**
     * Path variable name (alias for value).
     *
     * @return variable name
     */
    String name() default "";

    /**
     * Whether the variable is required.
     *
     * @return true if required
     */
    boolean required() default true;
}
