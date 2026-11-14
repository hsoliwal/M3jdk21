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
 * Inject - Jakarta CDI standard injection annotation.
 *
 * <h2>From Jakarta CDI / JSR-330</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;Service
 *     public class OrderService {
 *
 *         &#64;Inject
 *         private OrderRepository repository;
 *
 *         {@code @Inject}
 *         public OrderService(PaymentGateway gateway) {
 *             this.gateway = gateway;
 *         }
 *     }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP CDI
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Inject.java}
 * blob {@code 3f6aff423ea0a92c89b364a8e3c19669eb93973d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only, no reflection
@Documented
public @interface Inject {}
