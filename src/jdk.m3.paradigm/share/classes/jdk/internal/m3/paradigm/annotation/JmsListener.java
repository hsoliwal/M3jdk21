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
 * JmsListener - JMS message listener with compile-time code generation.
 *
 * <h2>From Grand Java Patterns Proof</h2>
 * <p>
 * Like KafkaListener, this annotation declares JMS consumption intent at
 * compile-time, generating explicit consumer code.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * @JmsListener(destination = "orders.queue")
 * interface OrderHandler {
 *
 * 	void onMessage(Order order);
 * }
 * }</pre>
 *
 * <h2>AnOP Principle</h2>
 * <p>
 * Annotations must never perform integration at runtime. All messaging wiring
 * is explicit in generated code.
 * </p>
 *
 * @since ContextOS 1.0 - AnOP Messaging
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/JmsListener.java}
 * blob {@code 1e3bf1d541f681d1016a2b93ea00e4f2e44e29aa} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface JmsListener {

    /**
     * JMS destination name.
     *
     * @return queue or topic name
     */
    String destination() default "";

    /**
     * JMS destination name (alias).
     *
     * @return queue or topic name
     */
    String value() default "";

    /**
     * Container factory bean name.
     *
     * @return factory name
     */
    String containerFactory() default "";

    /**
     * Message selector expression.
     *
     * @return JMS selector
     */
    String selector() default "";

    /**
     * Subscription name for durable subscriptions.
     *
     * @return subscription name
     */
    String subscription() default "";

    /**
     * Concurrency setting.
     *
     * @return concurrency range (e.g., "3-10")
     */
    String concurrency() default "";
}
