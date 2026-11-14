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
 * ToString - Generates toString() method.
 *
 * <h2>From Lombok Pattern</h2>
 *
 * <pre>{@code
 *
 * {@code @ToString}
 * public class Person {
 *
 *     private String name;
 *     private int age;
 * }
 *
 * // Generates:
 * public String toString() {
 *     return "Person(name=" + name + ", age=" + age + ")";
 * }
 * }</pre>
 *
 * @since ContextOS 1.0 - AnOP Lombok
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/ToString.java}
 * blob {@code 30d7369f32ce9c021e82c6a4a2e12d0d331f1b05} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface ToString {

    /**
     * Include field names in output.
     *
     * @return true to include names
     */
    boolean includeFieldNames() default true;

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
     * Call super.toString().
     *
     * @return true to include super
     */
    boolean callSuper() default false;

    /**
     * Only include explicitly annotated fields.
     *
     * @return true for explicit inclusion
     */
    boolean onlyExplicitlyIncluded() default false;
}
