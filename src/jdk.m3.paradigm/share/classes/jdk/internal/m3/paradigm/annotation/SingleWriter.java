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
 * SingleWriter - declares a single-writer invariant.
 *
 * <p>The application states ownership; compile-time materialization chooses the
 * JDK mechanism. The generated implementation must use bounded contention and
 * promote rather than spin indefinitely.</p>
 *
 * @since ContextOS 1.1 - AnOP concurrency
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/SingleWriter.java}
 * blob {@code 4994073a3db546eaa804b162459500780b1e08d8} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface SingleWriter {

    /** Lock/group name for related writes. */
    String lockName() default "default";

    /** Preferred generated primitive. */
    ConcurrencyPlan.Primitive primitive() default ConcurrencyPlan.Primitive.REENTRANT_LOCK;

    /** Contention/promotion strategy. */
    ConcurrencyPlan.WaitStrategy waitStrategy() default ConcurrencyPlan.WaitStrategy.ADAPTIVE;

    /** Maximum bounded spin hints before promotion. */
    int spinLimit() default 64;

    /** Whether generated blocking primitives should prefer fairness. */
    boolean fair() default true;
}
