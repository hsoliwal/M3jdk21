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
 * Getter - Generates getter methods.
 *
 * <h2>From Lombok Pattern</h2>
 *
 * <pre>{@code
 *
 * {@code @Getter}
 * private String name;
 *
 * // Generates:
 * public String getName() {
 *     return this.name;
 * }
 * }</pre>
 *
 * <h2>Boolean Fields</h2>
 *
 * <pre>{@code
 *
 * {@code @Getter}
 * private boolean active;
 *
 * // Generates: isActive() for boolean
 * }</pre>
 *
 * @since ContextOS 1.0 - AnOP Lombok
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Getter.java}
 * blob {@code 8472fefe6a1b3de0a897062621a7ecae5bd7ae4c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Getter {

    /**
     * Access level.
     *
     * @return access level
     */
    AccessLevel value() default AccessLevel.PUBLIC;

    /**
     * Whether to generate lazy getter.
     *
     * @return true for lazy initialization
     */
    boolean lazy() default false;
}
