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
 * Observer - Marks method as event observer.
 *
 * <h2>From Grand Java Patterns Proof / Jakarta CDI</h2>
 * <p>
 * Observer pattern for event-driven systems. Part of the execution entry point
 * family.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;Component
 *     public class OrderObserver {
 *
 *         &#64;Observer
 *         void onOrderCreated(OrderCreatedEvent event) {
 *             // React to order creation
 *         }
 *
 *         {@code @Observer}
 *         void onOrderShipped(@Observer ShippedEvent event) {
 *             // React to shipment
 *         }
 *     }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Event
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Observer.java}
 * blob {@code 2b70a2cf5ec95781f7793332de677b5712609e3a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Observer {

    /**
     * Event type to observe.
     *
     * @return event class
     */
    Class<?> value() default Object.class;

    /**
     * Whether observation is asynchronous.
     *
     * @return true for async
     */
    boolean async() default false;
}
