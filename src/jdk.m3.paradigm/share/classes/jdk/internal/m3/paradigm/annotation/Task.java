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
 * Task - Marks discrete work unit.
 *
 * <h2>From Grand Java Patterns Proof</h2>
 * <p>
 * Tasks and Jobs survive because they correspond to how the world works:
 * "Events happen. Work is scheduled. Reactions occur. Policies apply."
 * </p>
 *
 * <h2>Execution Entry Points</h2>
 * <ul>
 * <li>@Listener - "When this happens, run this"</li>
 * <li>@Job - "Run this under this policy"</li>
 * <li>@Task - "Execute this discrete unit of work"</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 * &#64;Task
 * interface DataMigrationTask {
 *     void execute();
 * }
 *
 * &#64;Task(timeout = 5000)
 * void processOrder(Order order) { ... }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Execution
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Task.java}
 * blob {@code b6262264a0af9536f3ccaf0d79f8407f35897f53} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Task {

    /**
     * Task name.
     *
     * @return task identifier
     */
    String value() default "";

    /**
     * Timeout in milliseconds.
     *
     * @return timeout duration
     */
    long timeout() default -1;

    /**
     * Whether task is retryable on failure.
     *
     * @return true if retryable
     */
    boolean retryable() default false;
}
