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
 * Compile-time concurrency policy shared by AnOP concurrency annotations.
 *
 * <p>The application declares an invariant; generated Java selects the JDK
 * primitive. {@link Thread#onSpinWait()} is only a bounded CPU hint. A positive
 * spin budget must promote to yield, park, or a blocking primitive so a waiting
 * thread cannot burn a core indefinitely while an owner is descheduled.</p>
 *
 * <p>This annotation is source-retained. It never installs locks, queues, threads,
 * proxies, or schedulers at runtime.</p>
 *
 * @since ContextOS 1.1 - IOP + AnOP compile-time convergence
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/ConcurrencyPlan.java}
 * blob {@code 28bcb373a5049de00e61e427837fd3e16da1c585} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface ConcurrencyPlan {

    /** Preferred implementation primitive. */
    Primitive primitive() default Primitive.AUTO;

    /** Waiting/promotion strategy under contention. */
    WaitStrategy waitStrategy() default WaitStrategy.ADAPTIVE;

    /** Maximum bounded spin hints before promotion. */
    int spinLimit() default 64;

    /** Maximum yields before park/block promotion. */
    int yieldLimit() default 16;

    /** Initial park interval for adaptive/polling materializations. */
    long parkNanos() default 100_000L;

    /** Whether generated blocking primitives should prefer fairness. */
    boolean fair() default true;

    /** Bounded queue/ring capacity; zero means not applicable. */
    int capacity() default 0;

    /** Backpressure policy when a bounded structure is full. */
    Backpressure backpressure() default Backpressure.BLOCK;

    /** JDK-level implementation families available to generators. */
    enum Primitive {
        AUTO,
        CAS,
        SEMAPHORE,
        REENTRANT_LOCK,
        STAMPED_LOCK,
        COUNT_DOWN_LATCH,
        PHASER,
        RING_BUFFER,
        BATON
    }

    /** Contention policy; NONE is valid only with a zero spin budget. */
    enum WaitStrategy {
        NONE,
        ADAPTIVE,
        SPIN_THEN_YIELD_THEN_PARK,
        PARK,
        BLOCKING
    }

    /** Bounded-pressure response exposed at the annotation surface. */
    enum Backpressure {
        BLOCK,
        REJECT,
        DROP_OLDEST,
        DROP_LATEST,
        CALLER_RUNS
    }
}
