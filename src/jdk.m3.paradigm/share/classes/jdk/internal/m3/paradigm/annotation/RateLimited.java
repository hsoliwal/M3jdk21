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
 * RateLimited - Declarative rate limiting via AOP.
 *
 * <h2>From Grand Java Patterns Proof</h2>
 * <p>
 * Rate limiting is a cross-cutting concern that protects services from
 * overload. This annotation declares rate limiting intent without exposing
 * implementation details.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * public interface ApiService {
 *
 *     {@code @RateLimited}
 *     Response handle(Request request);
 * }
 * }</pre>
 *
 * <h2>AnOP Principle</h2>
 * <p>
 * The user declares protection intent; the framework implements the rate
 * limiting algorithm (token bucket, sliding window, etc.).
 * </p>
 *
 * @since ContextOS 1.0 - AnOP AOP
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/RateLimited.java}
 * blob {@code 8c161471ada8bade6603054cb6da6a1d3a68bf25} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface RateLimited {

    /**
     * Maximum requests per time unit.
     *
     * @return rate limit
     */
    int value() default 100;

    /**
     * Time window in milliseconds.
     *
     * @return window duration
     */
    long window() default 1000;

    /**
     * Key extractor for per-key limiting.
     *
     * @return SpEL expression for key
     */
    String key() default "";
}
