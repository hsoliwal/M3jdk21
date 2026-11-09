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
 * RequestScoped - Declares request-scoped bean lifecycle.
 *
 * <h2>From Grand Java Patterns Proof / Jakarta CDI</h2>
 * <p>
 * Bean instance is created once per HTTP request and destroyed when the request
 * completes.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * {@code @RequestScoped}
 * public class RequestContext {
 *
 *     private final Map<String, Object> attributes = new HashMap<>();
 *
 *     public void setAttribute(String key, Object value) {
 *         attributes.put(key, value);
 *     }
 * }
 * }</pre>
 *
 * @since ContextOS 1.0 - AnOP Scope
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/RequestScoped.java}
 * blob {@code d99a4066b3d7dafa109c1363fd83a601e2a5438f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
@Scope("request")
public @interface RequestScoped {}
