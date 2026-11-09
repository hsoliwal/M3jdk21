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
 * SessionAttribute - Accesses session-scoped attribute.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 * {@code
 *
 * &#64;GetMapping("/dashboard")
 * public String dashboard(&#64;SessionAttribute("currentUser") User user,
 * 		@SessionAttribute(name = "theme", required = false) String theme) {
 * 	// ...
 * }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring Web
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/SessionAttribute.java}
 * blob {@code ee9710a6aab4650001df923acde30138987104bf} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface SessionAttribute {

    /**
     * Attribute name.
     *
     * @return session attribute name
     */
    String value() default "";

    /**
     * Attribute name (alias).
     *
     * @return session attribute name
     */
    String name() default "";

    /**
     * Whether the attribute is required.
     *
     * @return true if required
     */
    boolean required() default true;
}
