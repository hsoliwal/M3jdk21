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
 * Bean - Marks method as a bean factory method.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;Configuration
 *     public class AppConfig {
 *
 *         {@code @Bean}
 *         public DataSource dataSource() {
 *             return new HikariDataSource();
 *         }
 *
 *         &#64;Bean("customName")
 *         public Service myService(DataSource ds) {
 *             return new ServiceImpl(ds);
 *         }
 *     }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Bean.java}
 * blob {@code 05b819ba0d6f56e27518f8007542f54d75e4f57d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface Bean {

    /**
     * Bean names (first is primary, rest are aliases).
     *
     * @return bean names
     */
    String[] value() default {};

    /**
     * Bean names (alias for value).
     *
     * @return bean names
     */
    String[] name() default {};

    /**
     * Autowire mode.
     *
     * @return autowire mode
     */
    Autowire autowire() default Autowire.NO;

    /**
     * Init method name.
     *
     * @return init method
     */
    String initMethod() default "";

    /**
     * Destroy method name.
     *
     * @return destroy method
     */
    String destroyMethod() default "(inferred)";

    /** Autowire modes. */
    enum Autowire {
        NO,
        BY_NAME,
        BY_TYPE
    }
}
