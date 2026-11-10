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
package jdk.internal.m3.paradigm.contract;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
 * 	// LLM knows: retry 3 times, 1s delay, idempotent expected
 * }
 *
 * &#64;AIRetryable(include = {IOException.class, TimeoutException.class}, exclude = {
 * 		IllegalArgumentException.class}, maxAttempts = 5, backoff = &#64;Backoff(delay = 100, multiplier = 2.0, maxDelay = 5000))
 * void reliableWrite(Data data) {
 * 	// LLM knows: retry specific exceptions, exponential backoff
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Retry Hints (Spring-inspired)
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares retry inclusion and exclusion classes, attempts, backoff, recovery, state, label and idempotency hints without executing retries.
 * @m3.dataStructures SOURCE-retained annotation with exact target {@code {ElementType.TYPE, ElementType.METHOD}}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIRetryable.java}
 * blob {@code 2b166d3ca0888afc80dd1b5402d2292ee84f3614} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIRetryable {

    /**
     * Exception types to include for retry.
     *
     * @return exception classes
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares include metadata for Exception types to include for retry. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<? extends Throwable>[]}, with exact default {@code {}}.
     */
    Class<? extends Throwable>[] include() default {};

    /**
     * Exception types to exclude from retry.
     *
     * @return exception classes
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares exclude metadata for Exception types to exclude from retry. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<? extends Throwable>[]}, with exact default {@code {}}.
     */
    Class<? extends Throwable>[] exclude() default {};

    /**
     * Maximum number of attempts (including first).
     *
     * @return max attempts
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares maxAttempts metadata for Maximum number of attempts (including first). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}, with exact default {@code 3}.
     */
    int maxAttempts() default 3;

    /**
     * Backoff configuration.
     *
     * @return backoff
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares backoff metadata for Backoff configuration. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Backoff}, with exact default {@code @Backoff}.
     */
    Backoff backoff() default @Backoff;

    /**
     * Recovery method name.
     *
     * @return recovery method
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares recover metadata for Recovery method name. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String recover() default "";

    /**
     * Stateful retry (for distributed scenarios).
     *
     * @return true if stateful
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares stateful metadata for Stateful retry (for distributed scenarios). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}, with exact default {@code false}.
     */
    boolean stateful() default false;

    /**
     * Retry label for tracing.
     *
     * @return label
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares label metadata for Retry label for tracing. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String label() default "";

    /**
     * Idempotency requirement.
     *
     * @return true if must be idempotent
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares idempotent metadata for Idempotency requirement. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}, with exact default {@code true}.
     */
    boolean idempotent() default true;

    // --- BACKOFF ANNOTATION ---

    /** Backoff configuration for retries.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares nested retry delay, maximum delay, multiplier, jitter and strategy values without sleeping or calculating a retry interval.
     * @m3.dataStructures SOURCE-retained annotation with exact target {@code {}}, preserving existing elements, defaults and nested declarations.
     */
    @Target({})
    @Retention(RetentionPolicy.SOURCE)
    @interface Backoff {

        /**
         * Initial delay in milliseconds.
         *
         * @return delay ms
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares delay metadata for Initial delay in milliseconds. Consuming tooling interprets this value; the annotation element has no executable body.
         * @m3.dataStructures Annotation element of type {@code long}, with exact default {@code 1000}.
         */
        long delay() default 1000;

        /**
         * Maximum delay in milliseconds.
         *
         * @return max delay ms
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares maxDelay metadata for Maximum delay in milliseconds. Consuming tooling interprets this value; the annotation element has no executable body.
         * @m3.dataStructures Annotation element of type {@code long}, with exact default {@code 0}.
         */
        long maxDelay() default 0;

        /**
         * Multiplier for exponential backoff.
         *
         * @return multiplier
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares multiplier metadata for Multiplier for exponential backoff. Consuming tooling interprets this value; the annotation element has no executable body.
         * @m3.dataStructures Annotation element of type {@code double}, with exact default {@code 0}.
         */
        double multiplier() default 0;

        /**
         * Random factor for jitter (0.0 to 1.0).
         *
         * @return random factor
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares random metadata for Random factor for jitter (0.0 to 1.0). Consuming tooling interprets this value; the annotation element has no executable body.
         * @m3.dataStructures Annotation element of type {@code double}, with exact default {@code 0}.
         */
        double random() default 0;

        /**
         * Backoff strategy.
         *
         * @return strategy
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares strategy metadata for Backoff strategy. Consuming tooling interprets this value; the annotation element has no executable body.
         * @m3.dataStructures Annotation element of type {@code Strategy}, with exact default {@code Strategy.FIXED}.
         */
        Strategy strategy() default Strategy.FIXED;

        /** Backoff strategies.
         * @m3.pattern synexia:iop
         * @m3.algorithm Enumerates 5 symbolic retry backoff strategies for metadata consumers; enum tokens implement no runtime policy.
         * @m3.dataStructures Nested IAIRetryable.Strategy enum with constants in existing declaration order.
         */
        enum Strategy {
            /** Fixed delay between retries.
             * @m3.pattern synexia:iop
             * @m3.algorithm Names the FIXED metadata choice: Fixed delay between retries. Naming the token performs no scheduling, retry, interception or transaction operation.
             * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
             */
            FIXED,

            /** Exponential backoff.
             * @m3.pattern synexia:iop
             * @m3.algorithm Names the EXPONENTIAL metadata choice: Exponential backoff. Naming the token performs no scheduling, retry, interception or transaction operation.
             * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
             */
            EXPONENTIAL,

            /** Exponential with jitter.
             * @m3.pattern synexia:iop
             * @m3.algorithm Names the EXPONENTIAL_JITTER metadata choice: Exponential with jitter. Naming the token performs no scheduling, retry, interception or transaction operation.
             * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
             */
            EXPONENTIAL_JITTER,

            /** Linear increase.
             * @m3.pattern synexia:iop
             * @m3.algorithm Names the LINEAR metadata choice: Linear increase. Naming the token performs no scheduling, retry, interception or transaction operation.
             * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
             */
            LINEAR,

            /** No delay.
             * @m3.pattern synexia:iop
             * @m3.algorithm Names the NONE metadata choice: No delay. Naming the token performs no scheduling, retry, interception or transaction operation.
             * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
             */
            NONE
        }
    }
}
