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
 * Job - Marks scheduled/periodic execution point.
 *
 * <h2>From Grand Java Patterns Proof</h2>
 * <p>
 * Jobs survive because they correspond to how the world works: "Work is
 * scheduled. Reactions occur. Policies apply."
 * </p>
 *
 * <h2>Why Jobs Survive</h2> <blockquote> "If an annotation does not mark an
 * execution entry point, it will not survive long-term." Listeners and jobs
 * pass this test. </blockquote>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 * &#64;Job
 * interface DataSyncJob {
 *     void sync();
 * }
 *
 * &#64;Job(cron = "0 0 * * * *")  // Every hour
 * void hourlyCleanup() { ... }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Execution
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Job.java}
 * blob {@code 2f51712c86779ebe09ae5d48e19861aea6ef01ca} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Job {

    /**
     * Job name.
     *
     * @return job identifier
     */
    String value() default "";

    /**
     * Cron expression for scheduling.
     *
     * @return cron expression
     */
    String cron() default "";

    /**
     * Fixed delay in milliseconds.
     *
     * @return delay between executions
     */
    long fixedDelay() default -1;

    /**
     * Fixed rate in milliseconds.
     *
     * @return rate of execution
     */
    long fixedRate() default -1;
}
