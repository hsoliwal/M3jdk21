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
 * AIEventListener - AnOP annotation for event handling hints.
 *
 * <h2>Spring Inspiration, IOP Authority</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ SPRING: @EventListener → runtime event subscription ║
 * ║ AnOP: @AIEventListener → compile-time event handler declaration ║
 * ║ ║
 * ║ Spring says: "I will call this when the event is published" ║
 * ║ AnOP says: "This method handles this type of event" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT wire event buses. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * &#64;AIEventListener(UserCreatedEvent.class)
 * void onUserCreated(UserCreatedEvent event) {
 *     // LLM knows: called when UserCreatedEvent is published
 * }
 *
 * @AIEventListener(value = OrderEvent.class, condition = "event.status == 'COMPLETED'", async = true)
 * void onOrderCompleted(OrderEvent event) {
 *     // LLM knows: filtered by condition, runs async
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Event Hints (Spring-inspired)
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/event/EventListener.html">Spring @EventListener</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIEventListener.java}
 * blob {@code 657381b46716fea2c16402fd7edc8b56e71d13c7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(AIEventListeners.class)
public @interface AIEventListener {

    /**
     * Event class(es) this method handles.
     *
     * @return event types
      * @author Synexia
     */
    Class<?>[] value() default {};

    /**
     * SpEL-like condition expression (for documentation).
     *
     * @return condition
     */
    String condition() default "";

    /**
     * Event ID (for filtering/tracing).
     *
     * @return event ID
     */
    String id() default "";

    /**
     * Execute asynchronously hint.
     *
     * @return true if async
     */
    boolean async() default false;

    /**
     * Fallback method on error.
     *
     * @return fallback method name
     */
    String fallback() default "";

    /**
     * Ordering hint (lower = earlier).
     *
     * @return order value
     */
    int order() default 0;

    /**
     * Transaction phase when to listen.
     *
     * @return transaction phase
     */
    TransactionPhase phase() default TransactionPhase.AFTER_COMMIT;

    /** Transaction phases for event listening. */
    enum TransactionPhase {
        /** Listen before commit (in transaction). */
        BEFORE_COMMIT,

        /** Listen after successful commit. */
        AFTER_COMMIT,

        /** Listen after rollback. */
        AFTER_ROLLBACK,

        /** Listen after completion (commit or rollback). */
        AFTER_COMPLETION,

        /** Listen immediately (no transaction awareness). */
        IMMEDIATE
    }
}
