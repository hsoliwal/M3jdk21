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
 * SpringBootApplication - Main Spring Boot application annotation.
 *
 * <h2>From Spring Boot</h2>
 * <p>
 * Combines @Configuration, @EnableAutoConfiguration, and @ComponentScan.
 * </p>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;SpringBootApplication
 * 	public class MyApplication {
 *
 * 		public static void main(String[] args) {
 * 			SpringApplication.run(MyApplication.class, args);
 * 		}
 * 	}
 *
 * 	&#64;SpringBootApplication(scanBasePackages = "com.example")
 * 	public class CustomApp {
 * 	}
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring Boot
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/SpringBootApplication.java}
 * blob {@code 28b9555f8f395c1c4445236b885abaa6e1182cd1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
@Configuration
@ComponentScan
public @interface SpringBootApplication {

    /**
     * Exclude specific auto-configuration classes.
     *
     * @return classes to exclude
     */
    Class<?>[] exclude() default {};

    /**
     * Exclude auto-configuration by name.
     *
     * @return class names to exclude
     */
    String[] excludeName() default {};

    /**
     * Base packages to scan.
     *
     * @return package names
     */
    String[] scanBasePackages() default {};

    /**
     * Type-safe base package scanning.
     *
     * @return classes whose packages to scan
     */
    Class<?>[] scanBasePackageClasses() default {};

    /**
     * Whether to proxy @Bean methods.
     *
     * @return true for proxying
     */
    boolean proxyBeanMethods() default true;
}
