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
 * Handler - Marks method as event/message handler.
 *
 * <h2>From Grand Java Patterns Proof</h2>
 * <p>
 * Generic handler annotation for event-driven processing. Works
 * with @EventListener, @MessageListener, @KafkaListener patterns.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;Component
 *     public class OrderProcessor {
 *
 *         &#64;Handler
 *         {@code @Override}
 *         public void handle(OrderEvent event) {
 *             // Process order event
 *         }
 *     }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Event
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Handler.java}
 * blob {@code f27fef026d5ac2e3e1a0672d945cbce4ae2c8c82} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Handler {

    /**
     * Handler name.
     *
     * @return handler identifier
     */
    String value() default "";

    /**
     * Event type to handle.
     *
     * @return event class
     */
    Class<?> eventType() default Object.class;
}
