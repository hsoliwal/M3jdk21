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
 * RequestMapping - Maps HTTP requests to handler methods.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 * {@code
 * &#64;RestController
 * &#64;RequestMapping("/api/users")
 * public class UserController {
 *     &#64;RequestMapping(method = RequestMethod.GET)
 *     public List<User> list() { ... }
 *
 *     &#64;RequestMapping(value = "/{id}", method = RequestMethod.GET)
 *     public User findById(@PathVariable Long id) { ... }
 *
 *     &#64;RequestMapping(method = RequestMethod.POST,
 *                     consumes = "application/json",
 *                     produces = "application/json")
 *     public User create(@RequestBody User user) { ... }
 * }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/RequestMapping.java}
 * blob {@code 504c9321b576dc3ab80ebe1fcf05cd3fc6294559} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface RequestMapping {

    /**
     * Name of the mapping.
     *
     * @return mapping name
     */
    String name() default "";

    /**
     * Path patterns.
     *
     * @return paths
     */
    String[] value() default {};

    /**
     * Path patterns (alias for value).
     *
     * @return paths
     */
    String[] path() default {};

    /**
     * HTTP methods.
     *
     * @return methods
     */
    RequestMethod[] method() default {};

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
     * Consumable media types.
     *
     * @return consumes
     */
    String[] consumes() default {};

    /**
     * Producible media types.
     *
     * @return produces
     */
    String[] produces() default {};

    /** HTTP methods. */
    /**
     * RequestMethod.
     * @author Synexia
     */
    enum RequestMethod {
        GET,
        HEAD,
        POST,
        PUT,
        PATCH,
        DELETE,
        OPTIONS,
        TRACE
    }
}
