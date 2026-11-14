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
 * Serialized - declares ordered execution intent for compile-time materialization.
 *
 * <p>Ordering is not free. Capacity, backpressure and contention policy belong in
 * the contract so a small annotation cannot secretly become an unbounded queue or
 * an infinite spin loop. Generated code may use a ring buffer, ticket/baton, or a
 * JDK blocking primitive, but bounded spin must promote when progress stalls.</p>
 *
 * <p>This annotation is SOURCE-only and has no runtime interception behavior.</p>
 *
 * @since ContextOS 1.1 - AnOP concurrency
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Serialized.java}
 * blob {@code b602efe85202e073e95eb2d85705487212e026e9} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Serialized {

    /** Serialization group name. */
    String value() default "";

    /** Preferred ordering primitive. */
    ConcurrencyPlan.Primitive primitive() default ConcurrencyPlan.Primitive.BATON;

    /** Waiting/promotion strategy. */
    ConcurrencyPlan.WaitStrategy waitStrategy() default ConcurrencyPlan.WaitStrategy.ADAPTIVE;

    /** Maximum bounded spin hints before promotion. */
    int spinLimit() default 64;

    /** Bounded ordered-work capacity. */
    int capacity() default 1024;

    /** Behavior when the bounded work structure is full. */
    ConcurrencyPlan.Backpressure backpressure() default ConcurrencyPlan.Backpressure.BLOCK;
}
