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
 * Bounded - Declares capacity limit for backpressure protection.
 *
 * <h2>From Grand Java Patterns Proof</h2>
 * <p>
 * Bounded concurrency protects services from overload. The annotation maps to a
 * semaphore invariant, hidden from the user.
 * </p>
 *
 * <h2>Invariant Mapping</h2>
 *
 * <pre>
 * | Annotation  | Invariant        | Hidden primitive |
 * |-------------|------------------|------------------|
 * | @Bounded    | capacity limited | semaphore        |
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * &#64;SingleWriter
 * &#64;Bounded(1024)
 * void ingest();
 *
 * // Or with default capacity:
 * @Bounded(64)
 * void process();
 * }
 * </pre>
 *
 * <h2>Hidden Implementation</h2>
 *
 * <pre>{@code
 * // Framework holds:
 * final Semaphore permits = new Semaphore(64, false);
 *
 * // Proxy does:
 * if (!permits.tryAcquire()) {
 * 	rejectOrBackpressure();
 * }
 * try {
 * 	proceed();
 * } finally {
 * 	permits.release();
 * }
 * }</pre>
 *
 * <h2>AnOP Principle</h2>
 * <p>
 * User never sees backpressure logic, rejection strategy, or fairness flags.
 * They only declare capacity intent.
 * </p>
 *
 * @since ContextOS 1.0 - AnOP Concurrency
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Bounded.java}
 * blob {@code 11a3f846a96c30924d0867d4852c0687168c63b1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Bounded {

    /**
     * Maximum concurrent executions.
     *
     * @return capacity limit
     */
    int value() default 64;

    /**
     * Whether to use fair ordering.
     *
     * @return true for fair semaphore
     */
    boolean fair() default false;
}
