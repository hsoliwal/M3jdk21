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
 * Consumes - Declares accepted media types for a resource.
 *
 * <h2>From Jakarta JAX-RS</h2>
 * <p>
 * Specifies the MIME media types that the annotated method or class can
 * consume.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;RestController
 *     &#64;RequestMapping("/api/users")
 *     public class UserController {
 *
 *         {@code @PostMapping}
 *         &#64;Consumes("application/json")
 *         public User createUser(@RequestBody User user) {
 *             return userService.create(user);
 *         }
 *
 *         &#64;PutMapping("/{id}")
 *         &#64;Consumes({"application/json", "application/xml"})
 *         public User updateUser(@PathVariable Long id, @RequestBody User user) {
 *             return userService.update(id, user);
 *         }
 *     }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP JAX-RS
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Consumes.java}
 * blob {@code db32c886ce92aeccfc2d72050bc5cade28cf9632} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface Consumes {

    /**
     * Media types that can be consumed.
     *
     * @return media type strings
     */
    String[] value() default {};
}
