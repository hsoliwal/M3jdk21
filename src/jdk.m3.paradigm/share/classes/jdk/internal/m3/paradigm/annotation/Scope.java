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
 * Scope - Specifies bean scope.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;Component
 * 	&#64;Scope("prototype")
 * 	public class PrototypeBean {
 * 	}
 *
 * 	&#64;Component
 * 	&#64;Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
 * 	public class RequestScopedBean {
 * 	}
 * }
 * </pre>
 *
 * <h2>Standard Scopes</h2>
 * <ul>
 * <li>singleton - Single instance per context (default)</li>
 * <li>prototype - New instance each time</li>
 * <li>request - One per HTTP request</li>
 * <li>session - One per HTTP session</li>
 * <li>application - One per ServletContext</li>
 * </ul>
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Scope.java}
 * blob {@code 13ab92d5641f48df4e8475d904696caa9863d722} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface Scope {

    /**
     * Scope name.
     *
     * @return scope
     */
    String value() default "singleton";

    /**
     * Scope name (alias for value).
     *
     * @return scope
     */
    String scopeName() default "";

    /**
     * Proxy mode.
     *
     * @return proxy mode
     */
    ScopedProxyMode proxyMode() default ScopedProxyMode.DEFAULT;

    /** Scoped proxy modes. */
    enum ScopedProxyMode {
        DEFAULT,
        NO,
        INTERFACES,
        TARGET_CLASS
    }
}
