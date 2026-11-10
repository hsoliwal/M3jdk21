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
 * 	// LLM knows: called when UserCreatedEvent is fired
 * }
 *
 * // Async observer
 * void onOrderPlaced(@AIObserves(async = true) OrderEvent event) {
 * 	// LLM knows: async delivery
 * }
 *
 * // Conditional observer
 * void onPaymentReceived(&#64;AIObserves(during = TransactionPhase.AFTER_SUCCESS) PaymentEvent event) {
 * 	// LLM knows: only after successful transaction
 * }
 *
 * // With qualifier
 * void onPriorityOrder(@AIObserves @AIQualifier("priority") OrderEvent event) {
 * 	// LLM knows: only priority orders
 * }
 * }
 * </pre>
 *
 * @m3.pattern gof:observer
 * @m3.algorithm Declares an event-observer parameter and delivery hints only; no subscription or event dispatch occurs here.
 * @m3.dataStructures SOURCE-retained PARAMETER annotation with Reception, TransactionPhase, boolean and int elements.
 *
 * @since IOP 1.0 - AnOP Observer Hints (CDI-inspired)
 * @see <a href=
 *      "https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html">Jakarta
 *      CDI</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIObserves.java}
 * blob {@code 20ae94249a15ff89b253336275069dc24486f2f9} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIObserves {

    /**
     * Reception type.
     *
     * @m3.pattern gof:observer
     * @m3.algorithm Declares event reception policy, default ALWAYS; does not create observers or dispatch events.
     * @m3.dataStructures Reception enum annotation element with Reception.ALWAYS default.
     *
     * @return reception
     */
    Reception reception() default Reception.ALWAYS;

    /**
     * Transaction phase when to receive.
     *
     * @m3.pattern gof:observer
     * @m3.algorithm Declares notification transaction phase, default IN_PROGRESS; does not schedule callbacks.
     * @m3.dataStructures TransactionPhase enum annotation element with TransactionPhase.IN_PROGRESS default.
     *
     * @return transaction phase
     */
    TransactionPhase during() default TransactionPhase.IN_PROGRESS;

    /**
     * Async delivery hint.
     *
     * @m3.pattern gof:observer
     * @m3.algorithm Declares asynchronous-delivery intent, default false; does not create threads or queues.
     * @m3.dataStructures Primitive boolean annotation element with false default.
     *
     * @return true for async
     */
    boolean async() default false;

    /**
     * Priority/ordering hint.
     *
     * @m3.pattern gof:observer
     * @m3.algorithm Declares observer ordering priority, default zero; does not sort or dispatch subscribers.
     * @m3.dataStructures Primitive int annotation element with zero default.
     *
     * @return priority
     */
    int priority() default 0;

    /** Reception types (CDI-compatible).
     * @m3.pattern gof:observer
     * @m3.algorithm Enumerates two observer-instance reception policies; does not create or select an observer.
     * @m3.dataStructures Nested Reception enum with 2 declared constants.
     */
    enum Reception {
        /** Always notify, create observer if needed.
         * @m3.pattern gof:observer
         * @m3.algorithm Declares ALWAYS as the notify even when observer creation is needed notification policy token; no corresponding behavior executes here.
         * @m3.dataStructures One Reception enum constant; identity token rather than a callback or stored service.
         */
        ALWAYS,

        /** Only notify if observer instance exists.
         * @m3.pattern gof:observer
         * @m3.algorithm Declares IF_EXISTS as the notify only an existing observer notification policy token; no corresponding behavior executes here.
         * @m3.dataStructures One Reception enum constant; identity token rather than a callback or stored service.
         */
        IF_EXISTS
    }

    /** Transaction phases (CDI-compatible).
     * @m3.pattern gof:observer
     * @m3.algorithm Enumerates five notification phase tokens; does not observe transaction completion.
     * @m3.dataStructures Nested TransactionPhase enum with 5 declared constants.
     */
    enum TransactionPhase {
        /** Observe during transaction (default).
         * @m3.pattern gof:observer
         * @m3.algorithm Declares IN_PROGRESS as the during the transaction notification policy token; no corresponding behavior executes here.
         * @m3.dataStructures One TransactionPhase enum constant; identity token rather than a callback or stored service.
         */
        IN_PROGRESS,

        /** Observe before completion.
         * @m3.pattern gof:observer
         * @m3.algorithm Declares BEFORE_COMPLETION as the before completion notification policy token; no corresponding behavior executes here.
         * @m3.dataStructures One TransactionPhase enum constant; identity token rather than a callback or stored service.
         */
        BEFORE_COMPLETION,

        /** Observe after successful completion.
         * @m3.pattern gof:observer
         * @m3.algorithm Declares AFTER_SUCCESS as the after successful completion notification policy token; no corresponding behavior executes here.
         * @m3.dataStructures One TransactionPhase enum constant; identity token rather than a callback or stored service.
         */
        AFTER_SUCCESS,

        /** Observe after failure/rollback.
         * @m3.pattern gof:observer
         * @m3.algorithm Declares AFTER_FAILURE as the after failure or rollback notification policy token; no corresponding behavior executes here.
         * @m3.dataStructures One TransactionPhase enum constant; identity token rather than a callback or stored service.
         */
        AFTER_FAILURE,

        /** Observe after any completion.
         * @m3.pattern gof:observer
         * @m3.algorithm Declares AFTER_COMPLETION as the after any completion notification policy token; no corresponding behavior executes here.
         * @m3.dataStructures One TransactionPhase enum constant; identity token rather than a callback or stored service.
         */
        AFTER_COMPLETION
    }
}
