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
 * RequestParam - Binds request parameter to method parameter.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 * {@code
 * &#64;GetMapping("/users")
 * public List<User> search(@RequestParam String name) { ... }
 *
 * &#64;GetMapping("/users")
 * public Page<User> list(
 *     &#64;RequestParam(defaultValue = "0") int page,
 *     &#64;RequestParam(defaultValue = "20") int size) { ... }
 *
 * &#64;GetMapping("/filter")
 * public List<User> filter(
 *     &#64;RequestParam(required = false) String status) { ... }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/RequestParam.java}
 * blob {@code 9f7826aca1a938a6759281b6c52a2791da729d0d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface RequestParam {

    /**
     * Parameter name.
     *
     * @return parameter name
     */
    String value() default "";

    /**
     * Parameter name (alias for value).
     *
     * @return parameter name
     */
    String name() default "";

    /**
     * Whether the parameter is required.
     *
     * @return true if required
     */
    boolean required() default true;

    /**
     * Default value.
     *
     * @return default value
     */
    String defaultValue() default "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";
}
