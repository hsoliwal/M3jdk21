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
 * Retry - Compile-time annotation for retry policy decoration.
 *
 * <p>
 * From Grand Java Patterns Proof - AnOP Section:
 *
 * <pre>
 *   Allowed intent:
 *   - policy (@Retry, @Timeout)
 *   - constraints (@SingleWriter)
 *   - safety (@Cancellable)
 *
 *   Forbidden intent:
 *   - retries with RUNTIME policies
 *   - dynamic routing
 *
 *   If it crosses a boundary → it must be a different TYPE, not an annotation.
 * </pre>
 *
 * <h2>Generated Code Pattern</h2>
 *
 * <pre>{@code
 *
 * void process() {
 * 	RetryPolicy.execute(3, 100, () -> process_impl());
 * }
 * }</pre>
 *
 * @since Grand Java Patterns Proof - AnOP Section
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Retry.java}
 * blob {@code dbe64469a4cd3ad9e69de3ab3ab5f4d783dfa7e7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Retry {

    /**
     * Maximum retry attempts.
     *
     * @return max attempts
     */
    int maxAttempts() default 3;

    /**
     * Delay between retries in milliseconds.
     *
     * @return delay ms
     */
    long delayMs() default 100;

    /**
     * Exceptions to retry on.
     *
     * @return retryable exceptions
     */
    Class<? extends Throwable>[] on() default {Exception.class};

    /**
     * Exponential backoff multiplier.
     *
     * @return backoff multiplier (1.0 = no backoff)
     */
    double backoffMultiplier() default 1.0;
}
