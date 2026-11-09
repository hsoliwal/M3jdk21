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
 * ModelAttribute - Binds method parameter or return value to model.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>{@code
 * {@code @Controller}
 * public class UserController {
 *
 *     &#64;ModelAttribute("user")
 *     public User getUser(@RequestParam Long id) {
 *         return userService.findById(id);
 *     }
 *
 *     &#64;PostMapping("/users")
 *     public String updateUser(@ModelAttribute User user) {
 *         userService.save(user);
 *         return "redirect:/users";
 *     }
 * }
 * }</pre>
 *
 * @since ContextOS 1.0 - AnOP Spring Web
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/ModelAttribute.java}
 * blob {@code 99dbc87d6ccefe6c8968c7fdd96c3ba45e4879b5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface ModelAttribute {

    /**
     * Model attribute name.
     *
     * @return attribute name
     */
    String value() default "";

    /**
     * Model attribute name (alias).
     *
     * @return attribute name
     */
    String name() default "";

    /**
     * Whether data binding should occur.
     *
     * @return true to enable binding
     */
    boolean binding() default true;
}
