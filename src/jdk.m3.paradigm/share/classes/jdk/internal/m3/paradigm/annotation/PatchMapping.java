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
 * PatchMapping - Maps HTTP PATCH requests.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>{@code
 * {@code @RestController}
 * &#64;RequestMapping("/api/users")
 * public class UserController {
 *
 *     &#64;PatchMapping("/{id}")
 *     public User partialUpdate(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
 *         return userService.patch(id, updates);
 *     }
 * }
 * }</pre>
 *
 * @since ContextOS 1.0 - AnOP Spring Web
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/PatchMapping.java}
 * blob {@code 30e6d5c4bc34fedb9a654257a570793f76bca408} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
@RequestMapping(method = RequestMapping.RequestMethod.PATCH)
public @interface PatchMapping {

    /**
     * Path mapping.
     *
     * @return URL paths
     */
    String[] value() default {};

    /**
     * Path mapping (alias).
     *
     * @return URL paths
     */
    String[] path() default {};

    /**
     * Consumable media types.
     *
     * @return media types
     */
    String[] consumes() default {};

    /**
     * Producible media types.
     *
     * @return media types
     */
    String[] produces() default {};

    /**
     * Request parameters.
     *
     * @return parameter conditions
     */
    String[] params() default {};

    /**
     * Request headers.
     *
     * @return header conditions
     */
    String[] headers() default {};
}
