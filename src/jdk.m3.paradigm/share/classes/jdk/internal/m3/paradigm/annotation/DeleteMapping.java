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
 * DeleteMapping - Shortcut for @RequestMapping(method = DELETE).
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>{@code
 * {@code @RestController}
 * &#64;RequestMapping("/api/users")
 * public class UserController {
 *
 *     &#64;DeleteMapping("/{id}")
 *     public void delete(@PathVariable Long id) {
 *         userService.delete(id);
 *     }
 * }
 * }</pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/DeleteMapping.java}
 * blob {@code 27f8d520edea4ca49903502e6b5a33cc19bccf33} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
@RequestMapping(method = RequestMapping.RequestMethod.DELETE)
public @interface DeleteMapping {

    /**
     * Path patterns.
     *
     * @return paths
     */
    String[] value() default {};

    /**
     * Path patterns (alias).
     *
     * @return paths
     */
    String[] path() default {};

    /**
     * Request parameters.
     *
     * @return params
     */
    String[] params() default {};

    /**
     * Request headers.
     *
     * @return headers
     */
    String[] headers() default {};

    /**
     * Producible media types.
     *
     * @return produces
     */
    String[] produces() default {};
}
