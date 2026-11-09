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
 * Cancellable - Compile-time annotation for cancellation support.
 *
 * <p>
 * From Grand Java Patterns Proof - AnOP Section:
 *
 * <pre>
 *   Cancellation is usually:
 *   - a volatile / atomic flag
 *   - checked on every call
 *   - rarely true
 *
 *   With generated code:
 *   - branch is visible
 *   - branch prediction kicks in
 *   - cold path is deoptimized
 *   - hot path becomes near-zero cost
 *
 *   This is EXACTLY how the JVM wants to work.
 * </pre>
 *
 * <h2>Generated Code Pattern</h2>
 *
 * <pre>{@code
 *
 * void process() {
 * 	if (ctx.isCancelled())
 * 		return;
 * 	realProcess();
 * }
 * }</pre>
 *
 * <p>
 * The JIT can:
 * <ul>
 * <li>Inline aggressively</li>
 * <li>Eliminate cancelled paths</li>
 * <li>Remove whole wrappers</li>
 * <li>Specialize per call site</li>
 * </ul>
 *
 * @since Grand Java Patterns Proof - AnOP Section
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Cancellable.java}
 * blob {@code 5f6d5e5344a102ebe21713b1e404621ebf397073} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Cancellable {

    /**
     * Check cancellation before execution.
     *
     * @return true to check before
     */
    boolean checkBefore() default true;

    /**
     * Check cancellation at loop iterations.
     *
     * @return true to check in loops
     */
    boolean checkInLoop() default false;
}
