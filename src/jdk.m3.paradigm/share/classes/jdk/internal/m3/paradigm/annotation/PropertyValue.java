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
 * PropertyValue - Injects property value from configuration.
 *
 * <h2>From Spring Framework (@Value)</h2>
 *
 * <pre>{@code
 * {@code @Component}
 * public class AppConfig {
 *
 *     &#64;PropertyValue("${app.name}")
 *     private String appName;
 *
 *     &#64;PropertyValue("${app.timeout:30}")
 *     private int timeout; // default 30
 *
 *     &#64;PropertyValue("#{systemProperties['user.home']}")
 *     private String userHome; // SpEL expression
 * }
 * }</pre>
 *
 * <h2>Note</h2>
 * <p>
 * Named PropertyValue instead of Value to avoid conflict with Lombok's @Value
 * annotation.
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/PropertyValue.java}
 * blob {@code c4bf17fdb0adaa90ede3913c14cef74669cef73e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface PropertyValue {

    /**
     * Property expression.
     *
     * @return property expression
     */
    String value();
}
