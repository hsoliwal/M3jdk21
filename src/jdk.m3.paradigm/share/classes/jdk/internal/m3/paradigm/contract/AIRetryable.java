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
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.internal.m3.paradigm.contract;

import java.lang.annotation.*;

/**
 * AIRetryable - AnOP annotation for retry behavior hints.
 *
 * <h2>Spring Inspiration, IOP Authority</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ SPRING: @Retryable → runtime retry interception ║
 * ║ AnOP: @AIRetryable → compile-time retry declaration (NO retry impl) ║
 * ║ ║
 * ║ Spring says: "I will retry this method on failure" ║
 * ║ AnOP says: "This method should be retried on failure" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT implement retries. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * &#64;AIRetryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
 * String callExternalService() {
 *     // LLM knows: retry 3 times, 1s delay, idempotent expected
 * }
 *
 * @AIRetryable(include = {IOException.class,
 *         TimeoutException.class}, exclude = {
 *                 IllegalArgumentException.class}, maxAttempts = 5, backoff = @Backoff(delay = 100, multiplier = 2.0, maxDelay = 5000))
 * void reliableWrite(Data data) {
 *     // LLM knows: retry specific exceptions, exponential backoff
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Retry Hints (Spring-inspired)
 * @see <a href=
 *      "https://docs.spring.io/spring-retry/docs/current/api/org/springframework/retry/annotation/Retryable.html">Spring @Retryable</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIRetryable.java}
 * blob {@code 6036c1e4fd58c466721ee1c19408e45b34f4695d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIRetryable {

    /**
     * Exception types to include for retry.
     *
     * @return exception classes
     */
    Class<? extends Throwable>[] include() default {};

    /**
     * Exception types to exclude from retry.
     *
     * @return exception classes
     */
    Class<? extends Throwable>[] exclude() default {};

    /**
     * Maximum number of attempts (including first).
     *
     * @return max attempts
     */
    int maxAttempts() default 3;

    /**
     * Backoff configuration.
     *
     * @return backoff
     */
    Backoff backoff() default @Backoff;

    /**
     * Recovery method name.
     *
     * @return recovery method
     */
    String recover() default "";

    /**
     * Stateful retry (for distributed scenarios).
     *
     * @return true if stateful
     */
    boolean stateful() default false;

    /**
     * Retry label for tracing.
     *
     * @return label
     */
    String label() default "";

    /**
     * Idempotency requirement.
     *
     * @return true if must be idempotent
     */
    boolean idempotent() default true;

    // --- BACKOFF ANNOTATION ---

    /** Backoff configuration for retries. */
    @Target({})
    @Retention(RetentionPolicy.SOURCE)
    @interface Backoff {

        /**
         * Initial delay in milliseconds.
         *
         * @return delay ms
         */
        long delay() default 1000;

        /**
         * Maximum delay in milliseconds.
         *
         * @return max delay ms
         */
        long maxDelay() default 0;

        /**
         * Multiplier for exponential backoff.
         *
         * @return multiplier
         */
        double multiplier() default 0;

        /**
         * Random factor for jitter (0.0 to 1.0).
         *
         * @return random factor
         */
        double random() default 0;

        /**
         * Backoff strategy.
         *
         * @return strategy
         */
        Strategy strategy() default Strategy.FIXED;

        /** Backoff strategies. */
        enum Strategy {
            /** Fixed delay between retries. */
            FIXED,

            /** Exponential backoff. */
            EXPONENTIAL,

            /** Exponential with jitter. */
            EXPONENTIAL_JITTER,

            /** Linear increase. */
            LINEAR,

            /** No delay. */
            NONE
        }
    }
}
