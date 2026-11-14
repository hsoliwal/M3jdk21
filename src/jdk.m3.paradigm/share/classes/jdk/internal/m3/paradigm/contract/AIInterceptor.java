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

import java.lang.annotation.*;

/**
 * AIInterceptor - AnOP annotation for interceptor pattern hints.
 *
 * <h2>Jakarta CDI / EJB Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ CDI/EJB: @Interceptor → intercepts method invocations ║
 * ║ AnOP: @AIInterceptor → compile-time interceptor declaration ║
 * ║ ║
 * ║ Frameworks say: "Intercept calls and wrap with logic" ║
 * ║ AnOP says: "This class intercepts invocations" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT intercept calls. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Interceptor vs Decorator</h2>
 * <ul>
 * <li>Interceptor: Generic, applies to multiple types via bindings</li>
 * <li>Decorator: Type-specific, implements decorated interface</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 * // Interceptor binding annotation
 * &#64;AIInterceptorBinding
 * &#64;Target({ElementType.TYPE, ElementType.METHOD})
 * &#64;Retention(RetentionPolicy.SOURCE)
 * public @interface Logged { }
 *
 * // Interceptor implementation
 * &#64;AIInterceptor(bindings = {Logged.class}, priority = 100)
 * public class LoggingInterceptor {
 *
 * &#64;AIAroundInvoke
 * public Object logInvocation(InvocationContext ctx) throws Exception {
 * log.info("Entering: { }", ctx.getMethod().getName());
 * try {
 * return ctx.proceed();
 * } finally {
 * log.info("Exiting: { }", ctx.getMethod().getName());
 * }
 * }
 * }
 *
 * // Apply interceptor
 * &#64;Logged
 * public class UserService {
 * public User findById(String id) { ... }
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Interceptor Hints (CDI/EJB-inspired)
 * @see <a href=
 *      "https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html">Jakarta
 *      CDI</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIInterceptor.java}
 * blob {@code 880bce30493203f30d670bb040ea89df45338f1d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIInterceptor {

    /**
     * Interceptor binding annotations.
     *
     * @return binding annotation classes
     */
    Class<? extends Annotation>[] bindings() default {};

    /**
     * Interceptor priority (lower = invoked earlier).
     *
     * @return priority
     */
    int priority() default 0;

    /**
     * Interceptor purpose.
     *
     * @return purpose
     */
    Purpose purpose() default Purpose.GENERAL;

    /** Interceptor purposes. */
    enum Purpose {
        /** General-purpose. */
        GENERAL,

        /** Logging/tracing. */
        LOGGING,

        /** Security/authorization. */
        SECURITY,

        /** Transaction management. */
        TRANSACTION,

        /** Caching. */
        CACHING,

        /** Validation. */
        VALIDATION,

        /** Metrics/monitoring. */
        METRICS,

        /** Error handling/circuit breaker. */
        RESILIENCE
    }

    // --- LIFECYCLE CALLBACKS ---

    /** Marks a method as around-invoke interceptor. CDI: @AroundInvoke */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface AroundInvoke {}

    /** Marks a method as around-construct interceptor. CDI: @AroundConstruct */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface AroundConstruct {}

    /** Marks a method as post-construct callback. CDI/EJB: @PostConstruct */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface PostConstruct {}

    /** Marks a method as pre-destroy callback. CDI/EJB: @PreDestroy */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface PreDestroy {}
}
