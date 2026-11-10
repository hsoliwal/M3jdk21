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
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/Async.html">Spring @Async</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIAsync.java}
 * blob {@code 9bcfa8533ea3a7dc764a4584854c02ee923ddccd} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIAsync {

    /**
     * Executor name hint.
     *
     * @return executor name
     */
    String executor() default "";

    /**
     * Timeout in milliseconds (hint).
     *
     * @return timeout ms
     */
    long timeout() default 0;

    /**
     * Fallback method name.
     *
     * @return fallback method
     */
    String fallback() default "";

    /**
     * Exception handler method name.
     *
     * @return exception handler
     */
    String exceptionHandler() default "";

    /**
     * Execution mode hint.
     *
     * @return mode
     */
    Mode mode() default Mode.FIRE_AND_FORGET;

    /** Async execution modes. */
    /**
     * Mode.
     * @author Synexia
     */
    enum Mode {
        /** Fire and forget (no result needed). */
        FIRE_AND_FORGET,

        /** Await result via Future/CompletableFuture. */
        AWAITABLE,

        /** Reactive stream. */
        REACTIVE,

        /** Callback-based. */
        CALLBACK
    }
}
