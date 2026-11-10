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
 * Broadcast - Marks a field or method as a broadcast source in a HiveMind.
 *
 * <h2>Concept</h2>
 * <p>
 * When a @Broadcast field changes or method is called, the value is propagated
 * to all active contexts in the same colony.
 *
 * <h2>Usage on Fields</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;HiveMind(colony = "sensors")
 * 	public class TemperatureContext {
 *
 * 		&#64;Broadcast(topic = "temperature.reading")
 * 		private volatile double currentTemp;
 *
 * 		public void updateTemp(double temp) {
 * 			this.currentTemp = temp; // Triggers broadcast
 * 		}
 * 	}
 * }
 * </pre>
 *
 * <h2>Usage on Methods</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;HiveMind(colony = "analytics")
 * 	public class MetricsPublisher {
 *
 * 		&#64;Broadcast(topic = "metrics.batch")
 * 		public MetricsBatch publishBatch(List&lt;Metric> metrics) {
 * 			// Return value is broadcast to colony
 * 			return MetricsBatch.of(metrics);
 * 		}
 * 	}
 * }
 * </pre>
 *
 * <h2>Delivery Semantics</h2>
 * <ul>
 * <li>{@code reliable=true}: At-least-once delivery (queued)</li>
 * <li>{@code reliable=false}: Best-effort, may drop under load</li>
 * <li>{@code ttl}: Message expiry in milliseconds</li>
 * </ul>
 *
 * <h2>AnOP Compliance</h2>
 * <p>
 * SOURCE retention - compile-time code generation creates the broadcasting
 * wrapper. No runtime reflection.
 *
 * @since ContextOS 1.0 - HiveMind Pattern
 * @see HiveMind
 * @see Subscribe
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Broadcast.java}
 * blob {@code b0928139e8cea17907d91cf0ee87585469107bb8} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP LazyInit batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Broadcast {

    /**
     * Topic name for this broadcast. Subscribers filter by topic.
     *
     * @return topic name
     */
    String topic() default "";

    /**
     * Reliable delivery (at-least-once) vs best-effort.
     *
     * @return true for reliable delivery
     */
    boolean reliable() default false;

    /**
     * Time-to-live in milliseconds (0 = no expiry).
     *
     * @return TTL in ms
     */
    long ttl() default 0;

    /**
     * Whether to broadcast null values.
     *
     * @return true to include nulls
     */
    boolean includeNull() default false;

    /**
     * Async broadcast (non-blocking).
     *
     * @return true for async
     */
    boolean async() default true;
}
