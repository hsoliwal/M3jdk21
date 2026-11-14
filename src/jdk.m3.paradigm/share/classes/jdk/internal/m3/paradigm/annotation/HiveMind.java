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
 * HiveMind - Marks a context as a participant in collective intelligence.
 *
 * <h2>Concept: Collective Context Injection</h2>
 * <p>
 * A HiveMind is a swarm of contexts that share knowledge collectively. Unlike
 * traditional DI where dependencies are injected top-down, HiveMind enables
 * horizontal knowledge propagation across peer contexts.
 *
 * <h2>HiveMind vs Traditional DI</h2>
 *
 * <pre>
 * Traditional DI (vertical):
 *     Container
 *        ↓
 *     Service A ← @Inject dependency
 *        ↓
 *     Service B
 *
 * HiveMind (horizontal):
 *     Context A ←→ Context B ←→ Context C
 *         ↓           ↓           ↓
 *     [────── Shared Knowledge Pool ──────]
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;HiveMind(colony = "analytics")
 * 	public class MetricsContext extends AbstractContext&lt;MetricsContext> {
 *
 * 		&#64;Broadcast
 * 		private volatile MetricSnapshot latestMetrics;
 *
 * 		&#64;Subscribe(topic = "metrics.update")
 * 		public void onMetricsReceived(MetricSnapshot snapshot) {
 * 			// Received from another hive member
 * 		}
 * 	}
 * }
 * </pre>
 *
 * <h2>Colony Isolation</h2>
 * <p>
 * Contexts can belong to different colonies (named hives). Broadcasts only
 * propagate within the same colony.
 *
 * <h2>Integration with ContextOS</h2>
 * <ul>
 * <li>Each context UUID.db can join a colony</li>
 * <li>LazyInit loading preserved - dormant contexts don't receive broadcasts</li>
 * <li>BLOOM activates hive participation</li>
 * <li>COLLAPSE deactivates (but membership retained)</li>
 * </ul>
 *
 * @since ContextOS 1.0 - Collective Context Pattern
 * @see Broadcast
 * @see Subscribe
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/HiveMind.java}
 * blob {@code 5cfe302fe54ee4d314964d6debda9e3a3dc80d82} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP LazyInit batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface HiveMind {

    /**
     * Colony name (hive identifier). Contexts in the same colony share knowledge.
     *
     * @return colony name, defaults to "default"
     */
    String colony() default "default";

    /**
     * Priority in the hive (higher = receives broadcasts first).
     *
     * @return priority level
     */
    int priority() default 0;

    /**
     * Whether to auto-activate on context BLOOM.
     *
     * @return true to auto-activate
     */
    boolean autoActivate() default true;

    /**
     * Maximum broadcast queue size (back-pressure).
     *
     * @return max queue size, 0 for unbounded
     */
    int maxQueueSize() default 1024;
}
