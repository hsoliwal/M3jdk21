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
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.internal.m3.paradigm.contract;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIDecorator - AnOP annotation for decorator pattern hints.
 *
 * <h2>Jakarta CDI / GoF Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ CDI: @Decorator → decorator class wrapping target ║
 * ║ GoF: Decorator PatternKind → add behavior dynamically ║
 * ║ AnOP: @AIDecorator → compile-time decorator declaration ║
 * ║ ║
 * ║ CDI says: "This decorates the target, enable in beans.xml" ║
 * ║ AnOP says: "This class decorates another" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT apply decorators. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;AIDecorator(decorates = IUserService.class, priority = 100)
 *     public class LoggingUserServiceDecorator implements IUserService {
 *
 *         &#64;AIDelegate
 *         private IUserService delegate;
 *
 *         &#64;Override
 *         public User findById(String id) {
 *             log.info("Finding user: { }", id);
 *             return delegate.findById(id);
 *         }
 *     }
 *
 *     &#64;AIDecorator(decorates = IPaymentService.class, purpose = Purpose.SECURITY)
 *     public class AuditingPaymentDecorator implements IPaymentService {
 *         // Adds audit logging to all payment operations
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Decorator Hints (CDI/GoF-inspired)
 * @see <a href=
 *      "https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html">Jakarta
 *      CDI Decorators</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIDecorator.java}
 * blob {@code 320fbad6d7f4cb0da892c3779c31a7cba42de4c8} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIDecorator {

    /**
     * Interface/class being decorated.
     *
     * @return decorated type
     */
    Class<?> decorates() default Object.class;

    /**
     * Decorator priority (higher = applied later, wraps earlier).
     *
     * @return priority
     */
    int priority() default 0;

    /**
     * Decorator purpose.
     *
     * @return purpose
     */
    Purpose purpose() default Purpose.GENERAL;

    /**
     * Enabled by default.
     *
     * @return true if enabled
     */
    boolean enabled() default true;

    /** Decorator purposes. */
    enum Purpose {
        /** General-purpose decorator. */
        GENERAL,

        /** Logging/tracing. */
        LOGGING,

        /** Security/authorization. */
        SECURITY,

        /** Caching. */
        CACHING,

        /** Validation. */
        VALIDATION,

        /** Metrics/monitoring. */
        METRICS,

        /** Error handling/retry. */
        RESILIENCE,

        /** Transaction management. */
        TRANSACTION
    }
}
