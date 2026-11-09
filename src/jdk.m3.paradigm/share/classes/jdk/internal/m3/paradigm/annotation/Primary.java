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
 * Primary - Marks bean as primary choice when multiple candidates exist.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;Configuration
 *     public class DataSourceConfig {
 *
 *         &#64;Bean
 *         &#64;Primary
 *         public DataSource primaryDataSource() {
 *             return new HikariDataSource(); // preferred
 *         }
 *
 *         {@code @Bean}
 *         public DataSource secondaryDataSource() {
 *             return new HikariDataSource(); // fallback
 *         }
 *     }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Primary.java}
 * blob {@code 666b6d85f1336d51c8af2db96df82e7bdb2bebaf} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface Primary {
    // Marker annotation - no attributes
}
