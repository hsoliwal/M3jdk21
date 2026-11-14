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
 * MessageListener - Generic message listener with compile-time code generation.
 *
 * <h2>From Grand Java Patterns Proof</h2>
 * <p>
 * A broker-agnostic message listener annotation that generates explicit
 * consumer code at compile-time. Works with any messaging system.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * @MessageListener(channel = "events")
 * interface EventHandler {
 *
 * 	void onEvent(Event event);
 * }
 * }</pre>
 *
 * <h2>AnOP Principle</h2>
 * <p>
 * The annotation declares message consumption intent. The compile-time
 * processor generates appropriate consumer code based on the configured
 * messaging infrastructure.
 * </p>
 *
 * @since ContextOS 1.0 - AnOP Messaging
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/MessageListener.java}
 * blob {@code 71cc5067ec21dfe6999c49d19ea2b0e2e593e304} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface MessageListener {

    /**
     * Channel/topic name.
     *
     * @return channel name
     */
    String channel() default "";

    /**
     * Channel/topic name (alias).
     *
     * @return channel name
     */
    String value() default "";

    /**
     * Message type to handle.
     *
     * @return message class
     */
    Class<?> messageType() default Object.class;

    /**
     * Whether message handling is idempotent.
     *
     * @return true if idempotent
     */
    boolean idempotent() default false;

    /**
     * Error handling strategy.
     *
     * @return error handler bean name
     */
    String errorHandler() default "";
}
