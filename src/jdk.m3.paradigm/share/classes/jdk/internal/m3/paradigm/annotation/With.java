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
 * With - Generates withX() methods for immutable modification.
 *
 * <h2>From Lombok Pattern</h2> <blockquote> "Creates a clone with one field
 * changed - functional update pattern." </blockquote>
 *
 * <h2>Generated Code</h2>
 *
 * <pre>
 * {@code
 *
 * &#64;Value
 * public class Point {
 *
 *     &#64;With
 *     int x;
 *     {@code @With}
 *     int y;
 * }
 *
 * // Generates:
 * public Point withX(int x) {
 *     return new Point(x, this.y);
 * }
 *
 * public Point withY(int y) {
 *     return new Point(this.x, y);
 * }
 *
 * // Usage:
 * Point p1 = new Point(1, 2);
 * Point p2 = p1.withX(10); // Point(10, 2)
 * }
 * </pre>
 *
 * <h2>Chaining</h2>
 *
 * <pre>{@code
 *
 * Point p = origin.withX(5).withY(10);
 * }</pre>
 *
 * @since ContextOS 1.0 - AnOP Lombok
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/With.java}
 * blob {@code bc06d77bf31cf5a22b99e4528a307ad9f3405371} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface With {

    /**
     * Access level for generated method.
     *
     * @return access level
     */
    AccessLevel value() default AccessLevel.PUBLIC;
}
