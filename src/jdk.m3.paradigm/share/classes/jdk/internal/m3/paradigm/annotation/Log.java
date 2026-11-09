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
 * Log - Generates java.util.logging Logger field.
 *
 * <h2>From Lombok Pattern</h2>
 *
 * <pre>{@code
 *
 * {@code @Log}
 * public class MyService {
 *
 *     public void process() {
 *         log.info("Processing...");
 *     }
 * }
 *
 * // Generates:
 * private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(MyService.class.getName());
 * }</pre>
 *
 * @since ContextOS 1.0 - AnOP Lombok
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Log.java}
 * blob {@code 28d80e0fb7b657594e82ec9544f562ada0f07f63} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Log {

    /**
     * Topic/category for the logger.
     *
     * @return logger topic
     */
    String topic() default "";
}
