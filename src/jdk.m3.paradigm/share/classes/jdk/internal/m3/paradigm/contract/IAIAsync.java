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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIAsync - AnOP annotation for asynchronous execution hints.
 *
 * <h2>Spring Inspiration, IOP Authority</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ SPRING: @Async → runtime proxy spawns new thread ║
 * ║ AnOP: @AIAsync → compile-time async declaration (NO runtime magic) ║
 * ║ ║
 * ║ Spring says: "I will execute this in a separate thread" ║
 * ║ AnOP says: "This method is designed for async execution" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT spawn threads. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>The Invariant</h2> <blockquote> Spring @Async causes the container to
 * proxy and thread.<br>
 * AnOP @AIAsync declares that this method supports async invocation.
 * </blockquote>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * &#64;AIAsync(executor = "taskExecutor")
 * CompletableFuture<Result> processAsync(Request req);
 * // LLM knows: returns Future, don't block caller
 *
 * @AIAsync(timeout = 30000, fallback = "fallbackMethod")
 * CompletableFuture<Data> fetchData(String id);
 * // LLM knows: has timeout, has fallback
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Async Hints (Spring-inspired)
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares executor, timeout, fallback, exception handler and execution mode hints. SOURCE retention supplies no runtime proxy, thread scheduling or timeout enforcement.
 * @m3.dataStructures SOURCE-retained annotation targeting TYPE and METHOD, with its existing element defaults and nested enum.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIAsync.java}
 * blob {@code 59a7eb4e92a80e53a435922af69ead23c01b3207} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIAsync {

    /**
     * Executor name hint.
     *
     * @return executor name
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares executor metadata for Executor name hint. The annotation element contains no executable body; consuming tooling interprets the value.
     * @m3.dataStructures Annotation element of type {@code String} with exact default {@code ""}.
     */
    String executor() default "";

    /**
     * Timeout in milliseconds (hint).
     *
     * @return timeout ms
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares timeout metadata for Timeout in milliseconds (hint). The annotation element contains no executable body; consuming tooling interprets the value.
     * @m3.dataStructures Annotation element of type {@code long} with exact default {@code 0}.
     */
    long timeout() default 0;

    /**
     * Fallback method name.
     *
     * @return fallback method
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares fallback metadata for Fallback method name. The annotation element contains no executable body; consuming tooling interprets the value.
     * @m3.dataStructures Annotation element of type {@code String} with exact default {@code ""}.
     */
    String fallback() default "";

    /**
     * Exception handler method name.
     *
     * @return exception handler
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares exceptionHandler metadata for Exception handler method name. The annotation element contains no executable body; consuming tooling interprets the value.
     * @m3.dataStructures Annotation element of type {@code String} with exact default {@code ""}.
     */
    String exceptionHandler() default "";

    /**
     * Execution mode hint.
     *
     * @return mode
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares mode metadata for Execution mode hint. The annotation element contains no executable body; consuming tooling interprets the value.
     * @m3.dataStructures Annotation element of type {@code Mode} with exact default {@code Mode.FIRE_AND_FORGET}.
     */
    Mode mode() default Mode.FIRE_AND_FORGET;

    /** Async execution modes.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 4 symbolic execution modes for annotation metadata; consumers interpret the choice.
     * @m3.dataStructures Nested IAIAsync.Mode enum preserving constant declaration order and identities.
     */
    enum Mode {
        /** Fire and forget (no result needed).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the FIRE_AND_FORGET metadata choice: Fire and forget (no result needed). This token does not schedule execution, intercept calls or implement caching.
         * @m3.dataStructures Existing nested enum constant preserving its identity and declaration position.
         */
        FIRE_AND_FORGET,

        /** Await result via Future/CompletableFuture.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the AWAITABLE metadata choice: Await result via Future/CompletableFuture. This token does not schedule execution, intercept calls or implement caching.
         * @m3.dataStructures Existing nested enum constant preserving its identity and declaration position.
         */
        AWAITABLE,

        /** Reactive stream.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the REACTIVE metadata choice: Reactive stream. This token does not schedule execution, intercept calls or implement caching.
         * @m3.dataStructures Existing nested enum constant preserving its identity and declaration position.
         */
        REACTIVE,

        /** Callback-based.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CALLBACK metadata choice: Callback-based. This token does not schedule execution, intercept calls or implement caching.
         * @m3.dataStructures Existing nested enum constant preserving its identity and declaration position.
         */
        CALLBACK
    }
}
