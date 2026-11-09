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
 * Controller - Marks class as a web controller.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>{@code
 * {@code @Controller}
 * public class UserController {
 *
 *     &#64;GetMapping("/users")
 *     public String listUsers(Model model) {
 *         model.addAttribute("users", userService.findAll());
 *         return "users"; // view name
 *     }
 * }
 * }</pre>
 *
 * <h2>Semantic Meaning</h2>
 * <p>
 * Indicates that the class handles HTTP requests and returns views. This is a
 * specialization of @Component for the web layer.
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Controller.java}
 * blob {@code 039d292158329f3cf839c2b99c2dddc9b5cf0094} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
@Component
public @interface Controller {

    /**
     * Controller name.
     *
     * @return controller name
     */
    String value() default "";
}
