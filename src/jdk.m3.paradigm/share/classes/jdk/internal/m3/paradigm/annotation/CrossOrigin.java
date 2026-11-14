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
 * CrossOrigin - Enables Cross-Origin Resource Sharing (CORS).
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 * {@code
 * &#64;RestController
 * &#64;CrossOrigin(origins = "http://localhost:3000")
 * public class UserController {
 *
 *     &#64;GetMapping("/users")
 *     public List<User> getUsers() { ... }
 *
 *     &#64;CrossOrigin(origins = "*", maxAge = 3600)
 *     &#64;PostMapping("/users")
 *     public User createUser(@RequestBody User user) { ... }
 * }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring Web
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/CrossOrigin.java}
 * blob {@code b2d546cd7acf21c566ceba8f7796ea2d1fb8f71a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface CrossOrigin {

    /**
     * Allowed origins.
     *
     * @return origin URLs
     */
    String[] origins() default {};

    /**
     * Allowed origin patterns (supports wildcards).
     *
     * @return origin patterns
     */
    String[] originPatterns() default {};

    /**
     * Allowed HTTP methods.
     *
     * @return method names
     */
    String[] methods() default {};

    /**
     * Allowed request headers.
     *
     * @return header names
     */
    String[] allowedHeaders() default {};

    /**
     * Headers to expose in response.
     *
     * @return header names
     */
    String[] exposedHeaders() default {};

    /**
     * Whether credentials are allowed.
     *
     * @return true if credentials allowed
     */
    String allowCredentials() default "";

    /**
     * Max age of preflight response cache (seconds).
     *
     * @return max age
     */
    long maxAge() default -1;
}
