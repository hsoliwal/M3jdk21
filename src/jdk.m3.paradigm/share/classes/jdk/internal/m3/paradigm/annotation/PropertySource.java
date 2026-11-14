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
 * PropertySource - Declares property file locations.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;Configuration
 * 	&#64;PropertySource("classpath:application.properties")
 * 	public class AppConfig {
 *
 * 		&#64;PropertyValue("${app.name}")
 * 		private String appName;
 * 	}
 *
 * 	&#64;Configuration
 * 	&#64;PropertySource(value = "file:/etc/app/config.properties", ignoreResourceNotFound = true)
 * 	public class ExternalConfig {
 * 	}
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/PropertySource.java}
 * blob {@code 9ba56344f66ef233c96c7aa49ddc90b0c356dcbc} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface PropertySource {

    /**
     * Property resource locations.
     *
     * @return resource paths
     */
    String[] value();

    /**
     * Whether to ignore missing resources.
     *
     * @return true to ignore
     */
    boolean ignoreResourceNotFound() default false;

    /**
     * Property file encoding.
     *
     * @return charset name
     */
    String encoding() default "";
}
