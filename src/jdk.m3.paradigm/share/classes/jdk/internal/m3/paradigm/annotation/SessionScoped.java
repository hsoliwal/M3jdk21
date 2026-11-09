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
 * SessionScoped - Declares session-scoped bean lifecycle.
 *
 * <h2>From Grand Java Patterns Proof / Jakarta CDI</h2>
 * <p>
 * Bean instance is created once per HTTP session and destroyed when the session
 * expires or is invalidated.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * {@code @SessionScoped}
 * public class ShoppingCart {
 *
 *     private final List<Item> items = new ArrayList<>();
 *
 *     public void addItem(Item item) {
 *         items.add(item);
 *     }
 * }
 * }</pre>
 *
 * @since ContextOS 1.0 - AnOP Scope
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/SessionScoped.java}
 * blob {@code 5a295660ee6cb50b16269b54c15d71a928529d87} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
@Scope("session")
public @interface SessionScoped {}
