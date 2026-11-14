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
 * Observed - Enables observation/metrics for the annotated element.
 *
 * <h2>From Grand Java Patterns Proof</h2>
 * <p>
 * Part of the AnOP observability stack. Different from @Observable which is
 * about publish/subscribe - @Observed is about metrics collection.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 * &#64;Observed
 * &#64;Transactional
 * &#64;Async
 * &#64;Cacheable
 * &#64;Retryable
 * &#64;Validated
 * public Response process(Request request) { ... }
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Observability
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Observed.java}
 * blob {@code 415b5c17faaacd9f1a8dba2a39f2af811237d659} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Observed {

    /**
     * Observation name override.
     *
     * @return name for metrics
     */
    String name() default "";

    /**
     * Low cardinality key-values for observation.
     *
     * @return key-value pairs
     */
    String[] lowCardinalityKeyValues() default {};

    /**
     * High cardinality key-values for observation.
     *
     * @return key-value pairs
     */
    String[] highCardinalityKeyValues() default {};
}
