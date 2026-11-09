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
 * Prototype - Declares prototype-scoped bean lifecycle.
 *
 * <h2>From Grand Java Patterns Proof / Jakarta CDI @Dependent</h2>
 * <p>
 * A new bean instance is created each time it is injected or looked up. No
 * caching - every injection point gets a fresh instance.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * &#64;code
 * &#64;Prototype
 * {@code @RequiredArgsConstructor}
 * public class RequestProcessor {
 *
 * private final Context context;
 *
 * public void process(Request request) {
 * // Fresh instance per injection
 * }
 * }
 * }
 * </pre>
 *
 * <ul>
 * <li>Jakarta CDI: {@code @Dependent}</li>
 * <li>Spring: {@code @Scope("prototype")}</li>
 * <li>Guice: Unscoped (default behavior)</li>
 * </ul>
 *
 * @since ContextOS 1.0 - AnOP Scope
 * @see ApplicationScoped
 * @see Scope
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Prototype.java}
 * blob {@code 795f32ac341dfaba7230f776505fdd571a89c96c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
@Scope("prototype")
public @interface Prototype {}
