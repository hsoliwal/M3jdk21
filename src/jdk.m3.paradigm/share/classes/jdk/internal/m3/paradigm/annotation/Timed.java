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
 * Timed - Declarative method timing/metrics via AOP.
 *
 * <h2>From Grand Java Patterns Proof</h2>
 * <p>
 * Metrics collection is an orthogonal concern that should not pollute domain
 * logic. This annotation enables automatic method timing through compile-time
 * AOP.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * public interface ContextService {
 *
 *     {@code @Timed}
 *     void ingest(String contextId, String content);
 * }
 *
 * // Framework implements timing:
 * // [TIME] ingest: 1234567 ns
 * }</pre>
 *
 * <h2>AnOP Principle</h2>
 * <p>
 * The user declares timing intent; the framework implements measurement. No
 * runtime reflection, no annotation checks on hot paths.
 * </p>
 *
 * @since ContextOS 1.0 - AnOP AOP
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Timed.java}
 * blob {@code 60655a84489e79c0e2d01cf881bba92ed6cac650} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Timed {

    /**
     * Metric name override.
     *
     * @return metric name, defaults to method name
     */
    String value() default "";

    /**
     * Whether to record histogram.
     *
     * @return true to record distribution
     */
    boolean histogram() default false;

    /**
     * Percentiles to track.
     *
     * @return percentile values (e.g., 0.5, 0.95, 0.99)
     */
    double[] percentiles() default {};
}
