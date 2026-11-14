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
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/Scheduled.html">Spring @Scheduled</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIScheduled.java}
 * blob {@code 756cc099673fea9c61a9aa604dc0524e925a8fd5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(AISchedules.class)
public @interface AIScheduled {

    /**
     * Cron expression (Spring cron format).
     *
     * @return cron expression
     */
    String cron() default "";

    /**
     * Timezone for cron expression.
     *
     * @return timezone
     */
    String zone() default "";

    /**
     * Fixed rate in milliseconds.
     *
     * @return rate ms
     */
    long fixedRate() default -1;

    /**
     * Fixed delay in milliseconds (after completion).
     *
     * @return delay ms
     */
    long fixedDelay() default -1;

    /**
     * Initial delay in milliseconds.
     *
     * @return initial delay ms
     */
    long initialDelay() default 0;

    /**
     * Description of schedule purpose.
     *
     * @return description
     */
    String description() default "";

    /**
     * Idempotency requirement.
     *
     * @return true if must be idempotent
     */
    boolean idempotent() default true;

    /**
     * Overlap policy.
     *
     * @return overlap behavior
     */
    Overlap overlap() default Overlap.SKIP;

    /** Overlap policies. */
    enum Overlap {
        /** Skip if previous still running. */
        SKIP,

        /** Queue for later execution. */
        QUEUE,

        /** Allow concurrent execution. */
        CONCURRENT,

        /** Cancel previous and start new. */
        CANCEL_PREVIOUS
    }
}
