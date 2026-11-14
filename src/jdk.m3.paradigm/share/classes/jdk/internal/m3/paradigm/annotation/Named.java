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
 * Named - Provides bean name for injection.
 *
 * <h2>From Jakarta CDI / JSR-330</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;Named("primaryDataSource")
 *     &#64;Service
 *     public class PrimaryDataSource implements DataSource {
 *     }
 *
 *     &#64;Service
 *     public class UserRepository {
 *
 *         {@code @Inject}
 *         &#64;Named("primaryDataSource")
 *         private DataSource dataSource;
 *     }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP CDI
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Named.java}
 * blob {@code 32bd5ae855b83bdb84c2a6ae0a88652100570971} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
@Qualifier
public @interface Named {

    /**
     * Bean name.
     *
     * @return name for injection
     */
    String value() default "";
}
