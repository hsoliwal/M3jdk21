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
 * Logged - Declarative logging via AOP decorator.
 *
 * <h2>From Grand Java Patterns Proof</h2>
 * <p>
 * Logging is an orthogonal concern that should not pollute domain logic. This
 * annotation enables automatic logging of method entry/exit through
 * compile-time AOP.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     public interface ContextService {
 *
 *         &#64;Logged
 *         void ingest(String contextId, String content);
 *     }
 *
 *     // Implementation
 *     public class ContextServiceImpl implements ContextService {
 *
 *         &#64;Override
 *         &#64;Logged
 *         {@code @Timed}
 *         public void ingest(String contextId, String content) {
 *             // [LOG] ingest - automatically logged
 *         }
 *     }
 * }
 * </pre>
 *
 * <h2>AnOP Principle</h2>
 * <p>
 * Annotations select invariants, not mechanisms. The framework chooses how to
 * implement logging.
 * </p>
 *
 * @since ContextOS 1.0 - AnOP AOP
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Logged.java}
 * blob {@code e3ae0f16a84faac3247506d7c00e65c5d98196f5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Logged {

    /**
     * Log level to use.
     *
     * @return log level name (DEBUG, INFO, WARN, ERROR)
     */
    String level() default "INFO";

    /**
     * Whether to log method arguments.
     *
     * @return true to log arguments
     */
    boolean logArgs() default false;

    /**
     * Whether to log return value.
     *
     * @return true to log result
     */
    boolean logResult() default false;
}
