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
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.internal.m3.paradigm.contract;

import java.lang.annotation.*;

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
 *     return entityManagerFactory.createEntityManager();
 * }
 *
 * // Field producer
 * @AIProduces
 * &#64;AIQualifier("maxPoolSize")
 * private int maxPoolSize = 10;
 *
 * // With disposer
 * void closeEntityManager(@AIDisposes EntityManager em) {
 *     em.close();
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Producer Hints (CDI-inspired)
 * @see <a href=
 *      "https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html">Jakarta
 *      CDI</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIProduces.java}
 * blob {@code 6d8401f13914c8b6d2faaada4be7610c3ee4faad} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIProduces {

    /**
     * Alternative producer (used when alternatives are enabled).
     *
     * @return true if alternative
     */
    boolean alternative() default false;

    /**
     * Priority for alternative selection.
     *
     * @return priority
     */
    int priority() default 0;

    /**
     * Producer description.
     *
     * @return description
     */
    String description() default "";
}
