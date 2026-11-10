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
package jdk.internal.m3.paradigm.contract;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIProduces - AnOP annotation for CDI-style producer hints.
 *
 * <h2>Jakarta CDI Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ CDI: @Produces → producer method/field for injection ║
 * ║ AnOP: @AIProduces → compile-time producer declaration ║
 * ║ ║
 * ║ CDI says: "This method/field produces injectable instances" ║
 * ║ AnOP says: "This is a producer" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT produce instances. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>CDI vs Guice/Dagger</h2>
 * <ul>
 * <li>CDI @Produces = Guice/Dagger @Provides (method producers)</li>
 * <li>CDI allows field producers (Guice/Dagger don't)</li>
 * <li>CDI has @Disposes for cleanup (Guice/Dagger don't)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * // Method producer
 * &#64;AIProduces
 * &#64;AIInstanceScope(Cardinality.APPLICATION)
 * EntityManager produceEntityManager() {
 * 	return entityManagerFactory.createEntityManager();
 * }
 *
 * // Field producer
 * &#64;AIProduces
 * &#64;AIQualifier("maxPoolSize")
 * private int maxPoolSize = 10;
 *
 * // With disposer
 * void closeEntityManager(@AIDisposes EntityManager em) {
 * 	em.close();
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Producer Hints (CDI-inspired)
 * @see <a href=
 *      "https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html">Jakarta
 *      CDI</a>
 * @m3.pattern spring:ioc-container
 * @m3.algorithm Declares CDI-style producer methods or fields and alternative-selection hints; this annotation produces no object.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.METHOD, ElementType.FIELD}; its elements and nested declarations are metadata only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIProduces.java}
 * blob {@code 1a75d29b47036ed0c61f1f47d9d2490dae27e379} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIProduces {

    /**
     * Alternative producer (used when alternatives are enabled).
     *
     * @return true if alternative
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Marks the producer as an alternative rather than an unconditional default; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures boolean annotation element; default expression false.
     */
    boolean alternative() default false;

    /**
     * Priority for alternative selection.
     *
     * @return priority
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Declares alternative producer selection priority; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures int annotation element; default expression 0.
     */
    int priority() default 0;

    /**
     * Producer description.
     *
     * @return description
     * @m3.pattern spring:ioc-container
     * @m3.algorithm Carries the producer purpose description; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String description() default "";
}
