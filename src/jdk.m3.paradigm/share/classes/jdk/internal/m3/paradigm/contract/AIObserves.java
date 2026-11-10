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
 * AIObserves - AnOP annotation for CDI-style event observer hints.
 *
 * <h2>Jakarta CDI Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ CDI: @Observes → method parameter that receives events ║
 * ║ AnOP: @AIObserves → compile-time observer declaration ║
 * ║ ║
 * ║ CDI says: "Inject the event into this parameter when fired" ║
 * ║ AnOP says: "This parameter receives events" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT deliver events. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>CDI vs Spring</h2>
 * <ul>
 * <li>CDI: @Observes on parameter</li>
 * <li>Spring: @EventListener on method</li>
 * <li>AnOP: Both @AIObserves and @AIEventListener supported</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * // Observer method
 * void onUserCreated(@AIObserves UserCreatedEvent event) {
 *     // LLM knows: called when UserCreatedEvent is fired
 * }
 *
 * // Async observer
 * void onOrderPlaced(@AIObserves(async = true) OrderEvent event) {
 *     // LLM knows: async delivery
 * }
 *
 * // Conditional observer
 * void onPaymentReceived(
 *         &#64;AIObserves(during = TransactionPhase.AFTER_SUCCESS) PaymentEvent event) {
 *     // LLM knows: only after successful transaction
 * }
 *
 * // With qualifier
 * void onPriorityOrder(@AIObserves @AIQualifier("priority") OrderEvent event) {
 *     // LLM knows: only priority orders
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Observer Hints (CDI-inspired)
 * @see <a href=
 *      "https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html">Jakarta
 *      CDI</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIObserves.java}
 * blob {@code 3e46b3a34622fba775089fd80fe6b2b83a413f4d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIObserves {

    /**
     * Reception type.
     *
     * @return reception
     */
    Reception reception() default Reception.ALWAYS;

    /**
     * Transaction phase when to receive.
     *
     * @return transaction phase
     */
    TransactionPhase during() default TransactionPhase.IN_PROGRESS;

    /**
     * Async delivery hint.
     *
     * @return true for async
     */
    boolean async() default false;

    /**
     * Priority/ordering hint.
     *
     * @return priority
     */
    int priority() default 0;

    /** Reception types (CDI-compatible). */
    enum Reception {
        /** Always notify, create observer if needed. */
        ALWAYS,

        /** Only notify if observer instance exists. */
        IF_EXISTS
    }

    /** Transaction phases (CDI-compatible). */
    enum TransactionPhase {
        /** Observe during transaction (default). */
        IN_PROGRESS,

        /** Observe before completion. */
        BEFORE_COMPLETION,

        /** Observe after successful completion. */
        AFTER_SUCCESS,

        /** Observe after failure/rollback. */
        AFTER_FAILURE,

        /** Observe after any completion. */
        AFTER_COMPLETION
    }
}
