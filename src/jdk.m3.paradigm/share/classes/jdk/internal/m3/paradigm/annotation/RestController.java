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
 * RestController - Marks class as a REST API controller.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;RestController
 *     &#64;RequestMapping("/api/users")
 *     public class UserRestController {
 *
 *         &#64;GetMapping
 *         public List&lt;User> listUsers() {
 *             return userService.findAll(); // returns JSON
 *         }
 *
 *         {@code @PostMapping}
 *         public User create(@RequestBody User user) {
 *             return userService.save(user);
 *         }
 *     }
 * }
 * </pre>
 *
 * <h2>Equivalent To</h2>
 *
 * <pre>
 * {@code @Controller} + @ResponseBody on each method
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/RestController.java}
 * blob {@code 08410a99601bdcdd800579c097c26d3e1ab5648e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
@Controller
public @interface RestController {

    /**
     * Controller name.
     *
     * @return controller name
     */
    String value() default "";
}
