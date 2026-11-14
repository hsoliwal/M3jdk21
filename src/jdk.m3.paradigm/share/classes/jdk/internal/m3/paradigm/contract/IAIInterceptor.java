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
package jdk.internal.m3.paradigm.contract;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
 * log.info("Entering: {}", ctx.getMethod().getName());
 * try {
 * return ctx.proceed();
 * } finally {
 * log.info("Exiting: {}", ctx.getMethod().getName());
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
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares interceptor binding classes, priority and purpose for type metadata without wrapping method calls or invoking lifecycle callbacks.
 * @m3.dataStructures SOURCE-retained annotation with exact target {@code ElementType.TYPE}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIInterceptor.java}
 * blob {@code 9bc5896984b1b84910d3d3fea9f4df6d2f2e6430} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIInterceptor {

    /**
     * Interceptor binding annotations.
     *
     * @return binding annotation classes
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares bindings metadata for Interceptor binding annotations. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<? extends Annotation>[]}, with exact default {@code {}}.
     */
    Class<? extends Annotation>[] bindings() default {};

    /**
     * Interceptor priority (lower = invoked earlier).
     *
     * @return priority
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares priority metadata for Interceptor priority (lower = invoked earlier). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}, with exact default {@code 0}.
     */
    int priority() default 0;

    /**
     * Interceptor purpose.
     *
     * @return purpose
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares purpose metadata for Interceptor purpose. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Purpose}, with exact default {@code Purpose.GENERAL}.
     */
    Purpose purpose() default Purpose.GENERAL;

    /** Interceptor purposes.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 8 symbolic interceptor-purpose hints for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIInterceptor.Purpose enum with constants in existing declaration order.
     */
    enum Purpose {
        /** General-purpose.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the GENERAL metadata choice: General-purpose. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        GENERAL,

        /** Logging/tracing.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the LOGGING metadata choice: Logging/tracing. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        LOGGING,

        /** Security/authorization.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SECURITY metadata choice: Security/authorization. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        SECURITY,

        /** Transaction management.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the TRANSACTION metadata choice: Transaction management. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        TRANSACTION,

        /** Caching.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CACHING metadata choice: Caching. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        CACHING,

        /** Validation.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the VALIDATION metadata choice: Validation. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        VALIDATION,

        /** Metrics/monitoring.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the METRICS metadata choice: Metrics/monitoring. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        METRICS,

        /** Error handling/circuit breaker.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the RESILIENCE metadata choice: Error handling/circuit breaker. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        RESILIENCE
    }

    // --- LIFECYCLE CALLBACKS ---

    /** Marks a method as around-invoke interceptor. CDI: @AroundInvoke
     * @m3.pattern synexia:iop
     * @m3.algorithm Marks method declarations as around-invoke callback metadata without invoking the method or wrapping a call.
     * @m3.dataStructures SOURCE-retained annotation with exact target {@code ElementType.METHOD}, preserving existing elements, defaults and nested declarations.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface AroundInvoke {}

    /** Marks a method as around-construct interceptor. CDI: @AroundConstruct
     * @m3.pattern synexia:iop
     * @m3.algorithm Marks method declarations as around-construction callback metadata without invoking constructors or callbacks.
     * @m3.dataStructures SOURCE-retained annotation with exact target {@code ElementType.METHOD}, preserving existing elements, defaults and nested declarations.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface AroundConstruct {}

    /** Marks a method as post-construct callback. CDI/EJB: @PostConstruct
     * @m3.pattern synexia:iop
     * @m3.algorithm Marks method declarations as post-construction callback metadata without initializing objects or dispatching callbacks.
     * @m3.dataStructures SOURCE-retained annotation with exact target {@code ElementType.METHOD}, preserving existing elements, defaults and nested declarations.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface PostConstruct {}

    /** Marks a method as pre-destroy callback. CDI/EJB: @PreDestroy
     * @m3.pattern synexia:iop
     * @m3.algorithm Marks method declarations as pre-destruction callback metadata without disposing objects or dispatching callbacks.
     * @m3.dataStructures SOURCE-retained annotation with exact target {@code ElementType.METHOD}, preserving existing elements, defaults and nested declarations.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface PreDestroy {}
}
