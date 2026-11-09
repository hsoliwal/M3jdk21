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
 * Qualifier - Selects specific bean by name when autowiring.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;Service
 *     public class OrderService {
 *
 *         {@code @Autowired}
 *         &#64;Qualifier("primaryDataSource")
 *         private DataSource dataSource;
 *     }
 * }
 * </pre>
 *
 * <h2>Custom Qualifiers</h2>
 *
 * <pre>
 * {@code
 * &#64;Qualifier
 * &#64;Retention(RetentionPolicy.SOURCE)  // IOP: Compile-time only
 * public @interface MainDatabase { }
 *
 * &#64;MainDatabase
 * &#64;Bean
 * public DataSource mainDb() { ... }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Qualifier.java}
 * blob {@code 0fef007ba7accdd68bb9616b7531d951e182c6fd} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface Qualifier {

    /**
     * Qualifier value.
     *
     * @return qualifier
     */
    String value() default "";
}
