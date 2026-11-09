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
 * Value - Generates immutable data class.
 *
 * <h2>From Lombok Pattern</h2> <blockquote> "Immutable variant of @Data - all
 * fields are private final, no setters, class is final." </blockquote>
 *
 * <h2>Generated Code</h2>
 *
 * <pre>{@code
 * {@code @Value}
 * public class Point {
 *
 *     int x;
 *     int y;
 * }
 *
 * // Generates:
 * // - final class Point
 * // - private final int x, y
 * // - getX(), getY() (no setters)
 * // - equals(Object), hashCode()
 * // - toString()
 * // - AllArgsConstructor
 * }</pre>
 *
 * <h2>Equivalent To</h2>
 *
 * <pre>
 * &#64;Getter @FieldDefaults(makeFinal=true, level=PRIVATE)
 * {@code @AllArgsConstructor} @ToString @EqualsAndHashCode
 * </pre>
 *
 * <h2>Use With @Builder</h2>
 *
 * <pre>{@code
 * &#64;Value
 * {@code @Builder}
 * public class ImmutableConfig {
 *
 *     String host;
 *     int port;
 * }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Lombok
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Value.java}
 * blob {@code 2fb4e87bc70b7fded3fdf951a135d840728a4a77} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Value {

    /**
     * Fields to exclude from equals/hashCode.
     *
     * @return excluded field names
     */
    String[] exclude() default {};

    /**
     * Whether to generate static factory method.
     *
     * @return factory method name, empty for none
     */
    String staticConstructor() default "";
}
