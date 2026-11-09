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
 * Listener - Event Listener Semantic Annotation.
 *
 * <h2>From Grand Java Patterns Proof</h2> <blockquote> "Listener annotations
 * survived because they match human intent. People understand 'when X happens,
 * do Y'." </blockquote>
 *
 * <h2>Semantic Meaning</h2>
 * <p>
 * A method annotated with @Listener declares:
 * <ul>
 * <li>It receives events/messages</li>
 * <li>It may be called asynchronously</li>
 * <li>It should not block</li>
 * <li>It should be idempotent (if possible)</li>
 * </ul>
 *
 * <h2>Generated Code Pattern</h2>
 *
 * <pre>{@code
 *
 * // Source:
 * {@code @Listener}
 * void onMessage(Message msg);
 *
 * // Generated wrapper:
 * void onMessage_wrapper(Message msg) {
 *     if (ctx.isCancelled())
 *         return;
 *     ObservableAspect.before(ctx);
 *     try {
 *         onMessage_impl(msg);
 *     } finally {
 *         ObservableAspect.after(ctx);
 *     }
 * }
 * }</pre>
 *
 * <h2>Combined with Other Annotations</h2>
 * <p>
 *
 * {@code @Listener} combines naturally with:
 * <ul>
 * <li>@Observable - adds observability</li>
 * <li>@Cancellable - adds cancellation check</li>
 * <li>@Retry - adds retry policy</li>
 * <li>@Timeout - adds timeout</li>
 * </ul>
 *
 * @since Grand Java Patterns Proof - AnOP Section
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Listener.java}
 * blob {@code d82a95fbd6aa83adc2bbb48f00511b14099909a6} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Listener {

    /**
     * Event topic or channel name.
     *
     * @return topic name
     */
    String topic() default "";

    /**
     * Event type to listen for.
     *
     * @return event class
     */
    Class<?> eventType() default Object.class;

    /**
     * Filter expression for event selection.
     *
     * @return filter expression
     */
    String filter() default "";

    /**
     * Whether the listener is idempotent. Affects retry and error handling
     * behavior.
     *
     * @return true if idempotent
     */
    boolean idempotent() default false;

    /**
     * Priority for listener ordering. Higher values = earlier execution.
     *
     * @return priority value
     */
    int priority() default 0;
}
