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
 * Exclusive - declares mutual-exclusion intent for compile-time materialization.
 *
 * <h2>AnOP invariant</h2>
 * <pre>
 * &#64;Exclusive
 *     -> bounded contention plan
 *     -> generated JDK primitive
 *     -> explicit reviewable Java
 * </pre>
 *
 * <p>The application never implements mutex mechanics. The framework may use CAS
 * for the uncontended path, but an unbounded {@code while (...)
 * Thread.onSpinWait()} loop is forbidden. A positive spin budget must eventually
 * promote according to {@link #waitStrategy()} so a descheduled owner does not
 * cause another thread to consume a core indefinitely.</p>
 *
 * <p>Compile time is the default authority. This annotation has SOURCE retention
 * and does not install a runtime interceptor or proxy.</p>
 *
 * @since ContextOS 1.1 - AnOP concurrency
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Exclusive.java}
 * blob {@code 4e9b950a4ab146f45c5b1728ff1275fea4428b1d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Exclusive {

    /** Named exclusion group. */
    String value() default "";

    /** Preferred generated JDK primitive. */
    ConcurrencyPlan.Primitive primitive() default ConcurrencyPlan.Primitive.REENTRANT_LOCK;

    /** Promotion behavior after the bounded fast path. */
    ConcurrencyPlan.WaitStrategy waitStrategy() default ConcurrencyPlan.WaitStrategy.ADAPTIVE;

    /** Maximum spin hints before promotion. */
    int spinLimit() default 64;

    /** Whether generated blocking primitives should prefer fairness. */
    boolean fair() default true;
}
