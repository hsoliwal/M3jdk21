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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIScheduled - AnOP annotation for scheduled execution hints.
 *
 * <h2>Spring Inspiration, IOP Authority</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ SPRING: @Scheduled → runtime scheduler invokes method ║
 * ║ AnOP: @AIScheduled → compile-time schedule declaration (NO scheduler) ║
 * ║ ║
 * ║ Spring says: "I will invoke this on a schedule" ║
 * ║ AnOP says: "This method is designed for scheduled execution" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT create schedulers. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * &#64;AIScheduled(cron = "0 0 * * * *") // Every hour
 * void hourlyCleanup();
 * // LLM knows: runs on schedule, should be idempotent
 *
 * &#64;AIScheduled(fixedRate = 60000) // Every minute
 * void minutelyCheck();
 * // LLM knows: fixed rate, may overlap
 *
 * @AIScheduled(fixedDelay = 5000, initialDelay = 10000)
 * void delayedTask();
 * // LLM knows: fixed delay (waits for completion), initial delay
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Scheduled Hints (Spring-inspired)
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares cron, timezone, rate, delay, purpose, idempotency and overlap hints without creating timers or scheduling work.
 * @m3.dataStructures SOURCE-retained annotation with exact target {@code ElementType.METHOD}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIScheduled.java}
 * blob {@code 7b93467ed59b8fabd6357d8c2a40c5b11bc2d064} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(IAISchedules.class)
public @interface IAIScheduled {

    /**
     * Cron expression (Spring cron format).
     *
     * @return cron expression
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares cron metadata for Cron expression (Spring cron format). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String cron() default "";

    /**
     * Timezone for cron expression.
     *
     * @return timezone
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares zone metadata for Timezone for cron expression. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String zone() default "";

    /**
     * Fixed rate in milliseconds.
     *
     * @return rate ms
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares fixedRate metadata for Fixed rate in milliseconds. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}, with exact default {@code -1}.
     */
    long fixedRate() default -1;

    /**
     * Fixed delay in milliseconds (after completion).
     *
     * @return delay ms
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares fixedDelay metadata for Fixed delay in milliseconds (after completion). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}, with exact default {@code -1}.
     */
    long fixedDelay() default -1;

    /**
     * Initial delay in milliseconds.
     *
     * @return initial delay ms
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares initialDelay metadata for Initial delay in milliseconds. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code long}, with exact default {@code 0}.
     */
    long initialDelay() default 0;

    /**
     * Description of schedule purpose.
     *
     * @return description
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares description metadata for Description of schedule purpose. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String description() default "";

    /**
     * Idempotency requirement.
     *
     * @return true if must be idempotent
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares idempotent metadata for Idempotency requirement. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}, with exact default {@code true}.
     */
    boolean idempotent() default true;

    /**
     * Overlap policy.
     *
     * @return overlap behavior
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares overlap metadata for Overlap policy. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Overlap}, with exact default {@code Overlap.SKIP}.
     */
    Overlap overlap() default Overlap.SKIP;

    /** Overlap policies.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 4 symbolic schedule overlap policies for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIScheduled.Overlap enum with constants in existing declaration order.
     */
    enum Overlap {
        /** Skip if previous still running.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SKIP metadata choice: Skip if previous still running. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        SKIP,

        /** Queue for later execution.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the QUEUE metadata choice: Queue for later execution. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        QUEUE,

        /** Allow concurrent execution.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CONCURRENT metadata choice: Allow concurrent execution. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        CONCURRENT,

        /** Cancel previous and start new.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CANCEL_PREVIOUS metadata choice: Cancel previous and start new. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        CANCEL_PREVIOUS
    }
}
