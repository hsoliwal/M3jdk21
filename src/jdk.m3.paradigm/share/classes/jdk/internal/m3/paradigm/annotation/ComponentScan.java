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
 * ComponentScan - Specifies packages for Spring to scan for components.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;Configuration
 *     &#64;ComponentScan(basePackages = "com.example.services")
 *     public class AppConfig {
 *     }
 *
 *     {@code @Configuration}
 *     &#64;ComponentScan(basePackageClasses = {UserService.class, OrderService.class},
 *             excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
 *                     pattern = ".*Test.*"))
 *     public class ProdConfig {
 *     }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/ComponentScan.java}
 * blob {@code af4050c08de189cc7382af56197dfe53af1adcba} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface ComponentScan {

    /**
     * Base packages to scan (alias for basePackages).
     *
     * @return package names
     */
    String[] value() default {};

    /**
     * Base packages to scan.
     *
     * @return package names
     */
    String[] basePackages() default {};

    /**
     * Type-safe alternative to basePackages.
     *
     * @return classes whose packages will be scanned
      * @author Synexia
     */
    Class<?>[] basePackageClasses() default {};

    /**
     * Filters to exclude components.
     *
     * @return exclusion filters
     */
    Filter[] excludeFilters() default {};

    /**
     * Filters to include components.
     *
     * @return inclusion filters
     */
    Filter[] includeFilters() default {};

    /** Filter definition for component scanning. */
    @interface Filter {

        /**
         * Filter type.
         *
         * @return filter type
         */
        FilterType type() default FilterType.ANNOTATION;

        /**
         * Classes to filter.
         *
         * @return filter classes
         */
        Class<?>[] classes() default {};

        /**
         * Pattern for regex filtering.
         *
         * @return regex pattern
         */
        String pattern() default "";
    }

    /** Filter type enumeration. */
    enum FilterType {
        ANNOTATION,
        ASSIGNABLE_TYPE,
        ASPECTJ,
        REGEX,
        CUSTOM
    }
}
