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
 * Secured - Declares security context requirement.
 *
 * <h2>From Grand Java Patterns Proof</h2>
 * <p>
 * Maps to SecurityContext for role-based access control.
 * </p>
 *
 * <h2>Context Mapping</h2>
 *
 * <pre>
 * | Annotation | Context          |
 * |------------|------------------|
 * | @Secured   | Security context |
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * &#64;Secured("ROLE_ADMIN")
 * public void deleteUser(Long id) { ... }
 *
 * &#64;Secured({"ROLE_USER", "ROLE_ADMIN"})
 * public List<Order> getOrders() { ... }
 * }</pre>
 *
 * @since ContextOS 1.0 - AnOP Security
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Secured.java}
 * blob {@code 1efe3e762a7a5d989dedbd2a71e6d36085eaa848} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface Secured {

    /**
     * Required roles/authorities.
     *
     * @return role names
     */
    String[] value();
}
