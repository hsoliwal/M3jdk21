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
 * CookieValue - Binds cookie value to method parameter.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 * {@code
 *
 * &#64;GetMapping("/user")
 * public User getUser(&#64;CookieValue("sessionId") String sessionId,
 * 		@CookieValue(value = "theme", defaultValue = "light") String theme) {
 * 	// ...
 * }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring Web
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/CookieValue.java}
 * blob {@code c8436daa17e1b46a5b5d2ab1c909111cfcea023e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface CookieValue {

    /**
     * Cookie name.
     *
     * @return cookie name
     */
    String value() default "";

    /**
     * Cookie name (alias).
     *
     * @return cookie name
     */
    String name() default "";

    /**
     * Whether the cookie is required.
     *
     * @return true if required
     */
    boolean required() default true;

    /**
     * Default value if cookie is missing.
     *
     * @return default value
     */
    String defaultValue() default "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";
}
