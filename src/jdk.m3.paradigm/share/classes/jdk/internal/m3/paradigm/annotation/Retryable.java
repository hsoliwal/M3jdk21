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
 * Retryable - Marks method for automatic retry on failure.
 *
 * <h2>From Grand Java Patterns Proof</h2>
 * <p>
 * Retryable maps to a RetryContext, providing declarative retry behavior
 * without polluting business logic with retry loops.
 * </p>
 *
 * <h2>Context Mapping</h2>
 *
 * <pre>
 * | Annotation   | Context          |
 * |--------------|------------------|
 * | @Retryable   | RetryContext     |
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 * &#64;Retryable
 * public Response callExternalApi() {
 *     return httpClient.get(url);
 * }
 *
 * &#64;Retryable(maxAttempts = 5, backoff = @Backoff(delay = 1000))
 * public void processWithBackoff() { ... }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Resilience
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Retryable.java}
 * blob {@code 4fa0cf71f108b8a26f0331ded1b8b35b930d6fc9} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Retryable {

    /**
     * Maximum retry attempts.
     *
     * @return max attempts
     */
    int maxAttempts() default 3;

    /**
     * Exception types that trigger retry.
     *
     * @return retryable exceptions
     */
    Class<? extends Throwable>[] value() default {};

    /**
     * Exception types to include for retry.
     *
     * @return included exceptions
     */
    Class<? extends Throwable>[] include() default {};

    /**
     * Exception types to exclude from retry.
     *
     * @return excluded exceptions
     */
    Class<? extends Throwable>[] exclude() default {};

    /**
     * Backoff specification.
     *
     * @return backoff config
     */
    Backoff backoff() default @Backoff;

    /** Backoff configuration for retries. */
    @interface Backoff {

        /**
         * Initial delay in milliseconds.
         *
         * @return delay
         */
        long delay() default 1000;

        /**
         * Maximum delay.
         *
         * @return max delay
         */
        long maxDelay() default 0;

        /**
         * Multiplier for exponential backoff.
         *
         * @return multiplier
         */
        double multiplier() default 0;
    }
}
