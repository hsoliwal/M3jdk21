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
 * Colony - Defines a named colony (namespace) for collective contexts.
 *
 * <h2>Concept: Hive Namespace</h2>
 * <p>
 * A Colony is a logical grouping of contexts that share knowledge. Contexts in
 * different colonies are isolated from each other.
 *
 * <h2>Hive Metaphor</h2>
 *
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  COLONY STRUCTURE                                                       │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │                                                                         │
 * │  @Colony("analytics")              @Colony("sensors")                   │
 * │  ┌───────────────────┐            ┌───────────────────┐                 │
 * │  │  @Queen           │            │  @Queen           │                 │
 * │  │  Coordinator      │            │  SensorHub        │                 │
 * │  │       ↓           │            │       ↓           │                 │
 * │  │  @Worker @Worker  │            │  @Worker @Worker  │                 │
 * │  │  Metrics Reports  │            │  Temp   Humidity  │                 │
 * │  └───────────────────┘            └───────────────────┘                 │
 * │         ↕                                ↕                              │
 * │  [Colony Message Bus]            [Colony Message Bus]                   │
 * │                                                                         │
 * │  Colonies are ISOLATED - no cross-colony broadcasts                     │
 * │                                                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 * // Define a colony at package level
 * &#64;Colony(name = "analytics", description = "Real-time analytics processing")
 * package com.example.analytics;
 *
 * // Or at class level (joins existing colony)
 * &#64;Colony("analytics")
 * {@code @HiveMind}
 * public class MetricsAggregator extends AbstractContext<MetricsAggregator> {
 *     // Automatically joins "analytics" colony
 * }
 * }
 * </pre>
 *
 * <h2>Colony Lifecycle</h2>
 * <ul>
 * <li>Colony is created when first context joins</li>
 * <li>Colony persists while any context is member</li>
 * <li>Colony is dissolved when last context leaves</li>
 * <li>Colony metadata stored in registry.db</li>
 * </ul>
 *
 * @since ContextOS 1.0 - HiveMind Framework
 * @see HiveMind
 * @see Queen
 * @see Worker
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Colony.java}
 * blob {@code d49e2f01a65261997ce734bbd3ddfeb8f6db047f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP LazyInit batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Colony {

    /**
     * Colony name (unique identifier).
     *
     * @return colony name
     */
    String value() default "";

    /**
     * Colony name (alternative to value).
     *
     * @return colony name
     */
    String name() default "";

    /**
     * Human-readable description.
     *
     * @return description
     */
    String description() default "";

    /**
     * Maximum number of contexts in colony. 0 = unlimited.
     *
     * @return max members
     */
    int maxMembers() default 0;

    /**
     * Whether colony requires a Queen.
     *
     * @return true if queen required
     */
    boolean requiresQueen() default false;

    /**
     * Auto-dissolve when empty (no members).
     *
     * @return true to auto-dissolve
     */
    boolean autoDissolve() default true;
}
