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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Subscribe - Marks a method as a receiver for HiveMind broadcasts.
 *
 * <h2>Concept</h2>
 * <p>
 * Methods annotated with @Subscribe receive broadcasts from other contexts in
 * the same colony that match the topic pattern.
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;HiveMind(colony = "sensors")
 * 	public class DashboardContext {
 *
 * 		&#64;Subscribe(topic = "temperature.*")
 * 		public void onTemperature(double reading, String sensorId) {
 * 			updateDisplay(sensorId, reading);
 * 		}
 *
 * 		&#64;Subscribe(topic = "alert.critical")
 * 		public void onCriticalAlert(AlertEvent alert) {
 * 			notifyOperator(alert);
 * 		}
 * 	}
 * }
 * </pre>
 *
 * <h2>Topic Patterns</h2>
 * <ul>
 * <li>{@code "exact.topic"} - Exact match only</li>
 * <li>{@code "prefix.*"} - Wildcard suffix</li>
 * <li>{@code "*.suffix"} - Wildcard prefix</li>
 * <li>{@code "*"} - All topics in colony</li>
 * </ul>
 *
 * <h2>Method Signature</h2>
 * <p>
 * The method parameter type must match the broadcast type. Optional second
 * parameter receives sender context ID.
 *
 * <h2>Threading Model</h2>
 * <ul>
 * <li>{@code async=true}: Called on hive thread pool</li>
 * <li>{@code async=false}: Called on broadcaster's thread (blocking)</li>
 * <li>{@code ordered=true}: Preserves broadcast order per sender</li>
 * </ul>
 *
 * <h2>AnOP Compliance</h2>
 * <p>
 * SOURCE retention - compile-time code generation registers the subscription.
 * No runtime reflection.
 *
 * @since ContextOS 1.0 - HiveMind Pattern
 * @see HiveMind
 * @see Broadcast
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Subscribe.java}
 * blob {@code a182b199d2ddd7e11cc284806cd4c9e0c2211afa} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP LazyInit batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(Subscribe.List.class)
public @interface Subscribe {

    /**
     * Topic pattern to subscribe to. Supports wildcards: "prefix.*", "*.suffix",
     * "*"
     *
     * @return topic pattern
     */
    String topic();

    /**
     * Async reception (non-blocking).
     *
     * @return true for async
     */
    boolean async() default true;

    /**
     * Preserve message order per sender.
     *
     * @return true to preserve order
     */
    boolean ordered() default false;

    /**
     * Filter expression (JEXL) for conditional reception. Empty string means no
     * filter.
     *
     * @return filter expression
     */
    String filter() default "";

    /**
     * Maximum pending messages before back-pressure.
     *
     * @return max pending, 0 for unbounded
     */
    int maxPending() default 256;

    /** Container for repeatable @Subscribe annotations. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface List {

        /**
         * Array of Subscribe annotations.
         *
         * @return the array of Subscribe annotations
         */
        Subscribe[] value();
    }
}
