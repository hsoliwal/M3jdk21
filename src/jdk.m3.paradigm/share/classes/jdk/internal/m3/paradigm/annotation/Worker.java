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
 * Worker - Marks a context as a worker in a colony.
 *
 * <h2>Concept: Colony Worker</h2>
 * <p>
 * Workers are the operational contexts in a colony. They:
 * <ul>
 * <li>Perform actual work (processing, computation, I/O)</li>
 * <li>Broadcast results to the colony</li>
 * <li>Subscribe to tasks from Queen or other workers</li>
 * <li>Contribute to collective intelligence</li>
 * </ul>
 *
 * <h2>Worker Roles</h2>
 *
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  WORKER SPECIALIZATIONS                                                 │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │                                                                         │
 * │  @Worker(role = "producer")     Generates data, emits broadcasts        │
 * │  @Worker(role = "consumer")     Receives broadcasts, processes data     │
 * │  @Worker(role = "transformer")  Receives, transforms, re-broadcasts     │
 * │  @Worker(role = "aggregator")   Collects from many, emits summary       │
 * │  @Worker(role = "monitor")      Observes colony health, alerts          │
 * │                                                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;Worker(role = "producer")
 * 	&#64;HiveMind(colony = "sensors")
 * 	public class TemperatureSensor extends AbstractContext&lt;TemperatureSensor> {
 *
 * 		&#64;Broadcast(topic = "sensor.temperature")
 * 		private volatile double currentReading;
 *
 * 		public void measure() {
 * 			this.currentReading = readFromHardware();
 * 			// Automatically broadcasts to colony
 * 		}
 * 	}
 *
 * 	&#64;Worker(role = "aggregator")
 * 	&#64;HiveMind(colony = "sensors")
 * 	public class SensorAggregator extends AbstractContext&lt;SensorAggregator> {
 *
 * 		private final List&lt;Double> readings = new CopyOnWriteArrayList&lt;>();
 *
 * 		&#64;Subscribe(topic = "sensor.*")
 * 		public void onReading(double value) {
 * 			readings.add(value);
 * 		}
 *
 * 		&#64;Broadcast(topic = "aggregated.stats")
 * 		public Stats getStats() {
 * 			return Stats.of(readings);
 * 		}
 * 	}
 * }
 * </pre>
 *
 * <h2>Worker Groups</h2>
 * <p>
 * Workers can be grouped for targeted broadcasts:
 *
 * <pre>{@code
 * // Broadcast only to workers in "processing" group
 * hive.broadcast("sensors", "task.process", data,
 *     ctx -> ctx.hasAnnotation(Worker.class) &&
 *            "processing".equals(ctx.annotation(Worker.class).group()));
 * }</pre>
 *
 * @since ContextOS 1.0 - HiveMind Framework
 * @see HiveMind
 * @see Colony
 * @see Queen
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Worker.java}
 * blob {@code db4b4a991557057a3417e689678aafaaf41dadf7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP LazyInit batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Worker {

    /**
     * Worker role (producer, consumer, transformer, aggregator, monitor).
     *
     * @return role name
     */
    String role() default "worker";

    /**
     * Worker group for targeted broadcasts.
     *
     * @return group name
     */
    String group() default "";

    /**
     * Maximum concurrent tasks this worker handles.
     *
     * @return max tasks, 0 for unlimited
     */
    int maxTasks() default 0;

    /**
     * Whether worker can be evicted under memory pressure.
     *
     * @return true if evictable
     */
    boolean evictable() default true;

    /**
     * Worker priority (higher = receives broadcasts first among workers).
     *
     * @return priority level
     */
    int priority() default 0;

    /**
     * Whether to auto-restart on failure.
     *
     * @return true to auto-restart
     */
    boolean autoRestart() default false;

    /**
     * Restart delay in milliseconds.
     *
     * @return restart delay ms
     */
    long restartDelayMs() default 1000;
}
