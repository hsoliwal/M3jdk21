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
 * ResponseStatus - Sets HTTP response status code.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 * {@code
 * &#64;ResponseStatus(HttpStatus.CREATED)
 * &#64;PostMapping("/users")
 * public User createUser(@RequestBody User user) { ... }
 *
 * &#64;ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "User not found")
 * public class UserNotFoundException extends RuntimeException { }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring Web
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/ResponseStatus.java}
 * blob {@code 522fc77ecd333d0efaf0ac16118f0538a42b95f8} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface ResponseStatus {

    /**
     * HTTP status code.
     *
     * @return status code value
     */
    int value() default 200;

    /**
     * HTTP status code (alias).
     *
     * @return status code value
     */
    int code() default 200;

    /**
     * Reason phrase for the status.
     *
     * @return reason string
     */
    String reason() default "";
}
