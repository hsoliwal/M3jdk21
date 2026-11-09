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
 * Cron - Declares cron-based scheduling.
 *
 * <p>
 * Job/scheduling annotations like Cron survive because they mark execution
 * entry points - "Run this under this policy".
 * </p>
 *
 * <p>
 * Expression format: second minute hour day-of-month month day-of-week
 * </p>
 *
 * @since ContextOS 1.0 - AnOP Scheduling
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Cron.java}
 * blob {@code 033e51db8fe69cbb88eaafb1bb15cb3eb5ed4ab3} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Cron {

    /**
     * Cron expression.
     *
     * @return cron expression string
     */
    String value() default "";

    /**
     * Cron expression (alias).
     *
     * @return cron expression string
     */
    String expression() default "";

    /**
     * Time zone for scheduling.
     *
     * @return zone ID
     */
    String zone() default "";
}
