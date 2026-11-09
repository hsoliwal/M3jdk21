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
 * Required - Marks a property as mandatory for dependency injection.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>{@code
 * public class UserService {
 *
 *     private UserRepository repository;
 *
 *     {@code @Required}
 *     public void setRepository(UserRepository repository) {
 *         this.repository = repository;
 *     }
 * }
 * }</pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 * @deprecated Use constructor injection instead
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Required.java}
 * blob {@code 71759beb6ae3d1f140ade8c9a25fc13b751614a6} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
@Deprecated
public @interface Required {}
