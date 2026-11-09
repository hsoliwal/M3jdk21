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
 * Observable - Compile-time annotation for observability decoration.
 *
 * <p>
 * From Grand Java Patterns Proof - AnOP Section:
 *
 * <pre>
 *   AnOP is UX, not semantics.
 *   Annotations must behave like @Buffered.
 *   They may: decorate, constrain, optimize.
 *   They must NEVER: introduce queues, change execution domain,
 *                    spawn threads, bind brokers.
 *
 *   Retention = SOURCE only.
 *   Compile-time consumed.
 *   Eliminated after generation.
 * </pre>
 *
 * <h2>Generated Code Pattern</h2>
 *
 * <pre>{@code
 *
 * // Before generation:
 * {@code @Observable}
 * void process();
 *
 * // After generation:
 * void process() {
 *     ObservableAspect.before(ctx);
 *     try {
 *         process_impl();
 *     } finally {
 *         ObservableAspect.after(ctx);
 *     }
 * }
 * }</pre>
 *
 * <p>
 * No reflection. No proxy. JIT-visible. Debuggable.
 *
 * @since Grand Java Patterns Proof - AnOP Section
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Observable.java}
 * blob {@code 8756ad66c9664efbb498da365ad4c385b4ab2bf0} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Observable {

    /**
     * Name for the observation point.
     *
     * @return observation name
     */
    String value() default "";

    /**
     * Whether to record timing metrics.
     *
     * @return true to record timing
     */
    boolean timing() default true;

    /**
     * Whether to trace entry/exit.
     *
     * @return true to trace
     */
    boolean trace() default false;
}
