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
 * Queen - Marks a context as the coordinator/leader of a colony.
 *
 * <h2>Concept: Colony Coordinator</h2>
 * <p>
 * The Queen is the central coordinator of a colony. It:
 * <ul>
 * <li>Receives all broadcasts first (highest priority)</li>
 * <li>Can veto broadcasts to workers</li>
 * <li>Manages colony membership</li>
 * <li>Aggregates colony state</li>
 * <li>Handles colony lifecycle events</li>
 * </ul>
 *
 * <h2>Hive Metaphor</h2>
 *
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  QUEEN ROLE                                                             │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │                                                                         │
 * │                     ┌──────────────┐                                    │
 * │                     │   @Queen     │                                    │
 * │                     │  Coordinator │                                    │
 * │                     └──────┬───────┘                                    │
 * │                            │                                            │
 * │              ┌─────────────┼─────────────┐                              │
 * │              ↓             ↓             ↓                              │
 * │        ┌─────────┐   ┌─────────┐   ┌─────────┐                          │
 * │        │ @Worker │   │ @Worker │   │ @Worker │                          │
 * │        │ Context │   │ Context │   │ Context │                          │
 * │        └─────────┘   └─────────┘   └─────────┘                          │
 * │                                                                         │
 * │  Queen receives ALL broadcasts before workers (filter/transform/veto)   │
 * │                                                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;Queen
 *     &#64;HiveMind(colony = "sensors")
 *     public class SensorHub extends AbstractContext&lt;SensorHub> {
 *
 *         // Receives all broadcasts first
 *         &#64;Subscribe(topic = "*")
 *         public void onAnyMessage(Object message, String senderId) {
 *             log.info("Colony message from { }: { }", senderId, message);
 *         }
 *
 *         // Queen-specific: called when worker joins
 *         &#64;OnWorkerJoin
 *         public void workerJoined(IContext&lt;?> worker) {
 *             log.info("Worker joined: { }", worker.id());
 *         }
 *
 *         // Queen-specific: called when worker leaves
 *         {@code @OnWorkerLeave}
 *         public void workerLeft(IContext&lt;?> worker) {
 *             log.info("Worker left: { }", worker.id());
 *         }
 *     }
 * }
 * </pre>
 *
 * <h2>Singleton Rule</h2>
 * <p>
 * Each colony can have at most ONE Queen. If a second @Queen tries to join, it
 * becomes a @Worker instead (or throws if strict).
 *
 * @since ContextOS 1.0 - HiveMind Framework
 * @see HiveMind
 * @see Colony
 * @see Worker
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Queen.java}
 * blob {@code 2dd2fd3b5e39ba2936a8e4ab90c4c531d8665b7b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP LazyInit batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Queen {

    /**
     * Whether to fail if colony already has a queen. If false, demote to worker
     * silently.
     *
     * @return true to fail on conflict
     */
    boolean strict() default false;

    /**
     * Priority boost for broadcast reception. Queen always receives before workers.
     *
     * @return priority boost (added to base priority)
     */
    int priorityBoost() default 1000;

    /**
     * Whether queen can veto broadcasts to workers.
     *
     * @return true to enable veto power
     */
    boolean canVeto() default false;

    /**
     * Heartbeat interval in milliseconds. Queen sends heartbeat to workers. 0 = no
     * heartbeat.
     *
     * @return heartbeat interval ms
     */
    long heartbeatMs() default 0;
}
