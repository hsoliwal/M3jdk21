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
 * Builder - Generates builder pattern implementation.
 *
 * <h2>From Lombok Pattern</h2> <blockquote> "The Builder pattern provides a
 * flexible solution to object construction." </blockquote>
 *
 * <h2>Generated Code</h2>
 *
 * <pre>{@code
 *
 * {@code @Builder}
 * public class Person {
 *
 *     private String name;
 *     private int age;
 * }
 *
 * // Usage:
 * Person p = Person.builder().name("Alice").age(30).build();
 * }</pre>
 *
 * <h2>Generated Members</h2>
 * <ul>
 * <li>Static {@code builder()} method</li>
 * <li>Inner {@code Builder} class with fluent setters</li>
 * <li>{@code build()} method returning new instance</li>
 * <li>Optional {@code toBuilder()} for modification</li>
 * </ul>
 *
 * @since ContextOS 1.0 - AnOP Lombok
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Builder.java}
 * blob {@code a9195138f907ff5deee1a3ed99324d66b324220c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Builder {

    /**
     * Name of the builder class.
     *
     * @return builder class name
     */
    String builderClassName() default "";

    /**
     * Name of the builder method.
     *
     * @return builder method name
     */
    String builderMethodName() default "builder";

    /**
     * Name of the build method.
     *
     * @return build method name
     */
    String buildMethodName() default "build";

    /**
     * Whether to generate toBuilder() method.
     *
     * @return true to generate toBuilder
     */
    boolean toBuilder() default false;

    /**
     * Access level for generated methods.
     *
     * @return access level
     */
    AccessLevel access() default AccessLevel.PUBLIC;

    /**
     * Prefix for setter methods in builder.
     *
     * @return setter prefix
     */
    String setterPrefix() default "";
}
