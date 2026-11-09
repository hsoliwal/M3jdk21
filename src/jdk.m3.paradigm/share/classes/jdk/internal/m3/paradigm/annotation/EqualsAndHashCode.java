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
 * EqualsAndHashCode - Generates equals() and hashCode() methods.
 *
 * <h2>From Lombok Pattern</h2>
 *
 * <pre>{@code
 * {@code @EqualsAndHashCode}
 * public class Person {
 *
 *     private String name;
 *     private int age;
 * }
 *
 * // Generates equals() and hashCode() based on all fields
 * }</pre>
 *
 * <h2>Exclude Fields</h2>
 *
 * <pre>{@code
 * @EqualsAndHashCode(exclude = {"id", "createdAt"})
 * public class Entity {
 * }
 * }</pre>
 *
 * @since ContextOS 1.0 - AnOP Lombok
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/EqualsAndHashCode.java}
 * blob {@code 18800345be43942ba75288f1d00be7f875109355} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface EqualsAndHashCode {

    /**
     * Fields to exclude.
     *
     * @return excluded field names
     */
    String[] exclude() default {};

    /**
     * Fields to include (overrides exclude).
     *
     * @return included field names
     */
    String[] of() default {};

    /**
     * Call super.equals/hashCode.
     *
     * @return true to include super
     */
    boolean callSuper() default false;

    /**
     * Use getters instead of field access.
     *
     * @return true to use getters
     */
    boolean doNotUseGetters() default false;

    /**
     * Only include explicitly annotated fields.
     *
     * @return true for explicit inclusion
     */
    boolean onlyExplicitlyIncluded() default false;

    /**
     * Cache the hashCode.
     *
     * @return true to cache
     */
    boolean cacheStrategy() default false;
}
