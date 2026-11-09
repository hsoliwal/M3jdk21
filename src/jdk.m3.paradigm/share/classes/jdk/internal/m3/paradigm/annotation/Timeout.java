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
import java.util.concurrent.TimeUnit;

/**
 * Timeout - Compile-time annotation for timeout policy decoration.
 *
 * <p>
 * From Grand Java Patterns Proof - AnOP Section:
 *
 * <pre>
 *   Allowed intent:
 *   - decoration (like @Buffered)
 *   - policy (@Retry, @Timeout)
 *   - constraints (@SingleWriter)
 *
 *   Annotations must behave like language modifiers, not frameworks.
 * </pre>
 *
 * <h2>Generated Code Pattern</h2>
 *
 * <pre>{@code
 *
 * void process() {
 * 	TimeoutPolicy.execute(Duration.ofSeconds(30), () -> process_impl());
 * }
 * }</pre>
 *
 * @since Grand Java Patterns Proof - AnOP Section
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Timeout.java}
 * blob {@code 14508a1cb67deda8ca1ab4ba5b074ffec25451cc} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Timeout {

    /**
     * Timeout value.
     *
     * @return timeout value
     */
    long value();

    /**
     * Time unit for the timeout.
     *
     * @return time unit
     */
    TimeUnit unit() default TimeUnit.MILLISECONDS;

    /**
     * Whether to throw on timeout.
     *
     * @return true to throw TimeoutException
     */
    boolean throwOnTimeout() default true;
}
