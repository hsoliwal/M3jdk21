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
 * EventListener - Marks method as an event listener.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;Component
 * 	public class UserEventHandler {
 *
 * 		&#64;EventListener
 * 		public void handleUserCreated(UserCreatedEvent event) {
 * 			// handle event
 * 		}
 *
 * 		&#64;EventListener(condition = "#event.priority == 'HIGH'")
 * 		public void handleHighPriority(TaskEvent event) {
 * 			// conditional handling
 * 		}
 *
 * 		&#64;EventListener({UserCreatedEvent.class, UserUpdatedEvent.class})
 * 		public void handleUserChanges(Object event) {
 * 			// handle multiple event types
 * 		}
 * 	}
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/EventListener.java}
 * blob {@code 570cb6fbbfe76c76639da831e49ca242b431eed4} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface EventListener {

    /**
     * Event classes to listen for.
     *
     * @return event classes
     */
    Class<?>[] value() default {};

    /**
     * Event classes (alias for value).
     *
     * @return event classes
     */
    Class<?>[] classes() default {};

    /**
     * SpEL condition for event handling.
     *
     * @return condition expression
     */
    String condition() default "";

    /**
     * Listener id.
     *
     * @return listener id
     */
    String id() default "";
}
