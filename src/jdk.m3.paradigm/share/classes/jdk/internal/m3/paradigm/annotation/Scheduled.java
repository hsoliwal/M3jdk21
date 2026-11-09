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
 * Scheduled - Marks method for scheduled execution.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;Component
 * 	public class ScheduledTasks {
 *
 * 		&#64;Scheduled(fixedRate = 5000)
 * 		public void every5Seconds() {
 * 			// runs every 5 seconds
 * 		}
 *
 * 		&#64;Scheduled(fixedDelay = 10000)
 * 		public void after10SecondsDelay() {
 * 			// runs 10 seconds after previous completion
 * 		}
 *
 * 		&#64;Scheduled(cron = "0 0 12 * * ?")
 * 		public void everyDayAtNoon() {
 * 			// cron expression
 * 		}
 * 	}
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Scheduled.java}
 * blob {@code 38390fc1c617c74fb9fc511575d75b7039529345} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface Scheduled {

    /**
     * Cron expression.
     *
     * @return cron expression
     */
    String cron() default "";

    /**
     * Zone for cron expression.
     *
     * @return zone id
     */
    String zone() default "";

    /**
     * Fixed rate in milliseconds.
     *
     * @return fixed rate
     */
    long fixedRate() default -1;

    /**
     * Fixed rate as string (supports property placeholders).
     *
     * @return fixed rate string
     */
    String fixedRateString() default "";

    /**
     * Fixed delay in milliseconds.
     *
     * @return fixed delay
     */
    long fixedDelay() default -1;

    /**
     * Fixed delay as string (supports property placeholders).
     *
     * @return fixed delay string
     */
    String fixedDelayString() default "";

    /**
     * Initial delay in milliseconds.
     *
     * @return initial delay
     */
    long initialDelay() default -1;

    /**
     * Initial delay as string.
     *
     * @return initial delay string
     */
    String initialDelayString() default "";
}
