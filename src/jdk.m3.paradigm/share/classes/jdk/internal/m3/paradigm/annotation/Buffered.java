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
 * Buffered - Declarative buffering decorator.
 *
 * <h2>From Grand Java Patterns Proof</h2> <blockquote> "AnOP should feel
 * like @Buffered on an InputStream: simple, predictable, and honest."
 * </blockquote>
 *
 * <p>
 * The canonical example of what AnOP SHOULD be - a simple decorator that wraps
 * behavior without hidden complexity.
 * </p>
 *
 * <h2>The @Buffered Mental Model</h2>
 *
 * <pre>{@code
 *
 * {@code @Buffered}
 * InputStream in;
 *
 * // Is equivalent to:
 * InputStream in = new BufferedInputStream(original);
 * }</pre>
 *
 * <h2>Why @Buffered Is Perfect AnOP</h2>
 * <ul>
 * <li>No hidden threads</li>
 * <li>No hidden queues</li>
 * <li>No hidden state machines</li>
 * <li>Just decoration</li>
 * </ul>
 *
 * <h2>The Decoration Rule</h2> <blockquote> "Annotations must behave like
 * language modifiers, not frameworks." If the annotation cannot be explained as
 * SIMPLE DECORATION, it is invalid. </blockquote>
 *
 * <h2>Good AnOP Annotations</h2>
 *
 * <pre>
 * &#64;Buffered     → Simple decoration
 * &#64;Observable   → Simple decoration
 * &#64;Retryable    → Simple decoration
 * {@code @SingleWriter} → Simple constraint
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Decoration
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Buffered.java}
 * blob {@code 783dc085455993626ed93ba4bcc3b3d805c2da22} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Buffered {

    /**
     * Buffer size.
     *
     * @return buffer size in bytes
     */
    int size() default 8192;
}
