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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
 * 	// LLM knows: called when UserCreatedEvent is published
 * }
 *
 * @AIEventListener(value = OrderEvent.class, condition = "event.status == 'COMPLETED'", async = true)
 * void onOrderCompleted(OrderEvent event) {
 * 	// LLM knows: filtered by condition, runs async
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Event Hints (Spring-inspired)
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares event type, condition, identity, asynchronous, fallback, ordering and transaction-phase hints for methods without registering an event subscription.
 * @m3.dataStructures SOURCE-retained annotation with exact target {@code ElementType.METHOD}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIEventListener.java}
 * blob {@code 2919a324a37aa167d0f5817a2352fbd46e521819} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(IAIEventListeners.class)
public @interface IAIEventListener {

    /**
     * Event class(es) this method handles.
     *
     * @return event types
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares value metadata for Event class(es) this method handles. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>[]}, with exact default {@code {}}.
     */
    Class<?>[] value() default {};

    /**
     * SpEL-like condition expression (for documentation).
     *
     * @return condition
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares condition metadata for SpEL-like condition expression (for documentation). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String condition() default "";

    /**
     * Event ID (for filtering/tracing).
     *
     * @return event ID
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares id metadata for Event ID (for filtering/tracing). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String id() default "";

    /**
     * Execute asynchronously hint.
     *
     * @return true if async
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares async metadata for Execute asynchronously hint. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}, with exact default {@code false}.
     */
    boolean async() default false;

    /**
     * Fallback method on error.
     *
     * @return fallback method name
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares fallback metadata for Fallback method on error. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String fallback() default "";

    /**
     * Ordering hint (lower = earlier).
     *
     * @return order value
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares order metadata for Ordering hint (lower = earlier). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}, with exact default {@code 0}.
     */
    int order() default 0;

    /**
     * Transaction phase when to listen.
     *
     * @return transaction phase
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares phase metadata for Transaction phase when to listen. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code TransactionPhase}, with exact default {@code TransactionPhase.AFTER_COMMIT}.
     */
    TransactionPhase phase() default TransactionPhase.AFTER_COMMIT;

    /** Transaction phases for event listening.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 5 symbolic event transaction-phase hints for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIEventListener.TransactionPhase enum with constants in existing declaration order.
     */
    enum TransactionPhase {
        /** Listen before commit (in transaction).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the BEFORE_COMMIT metadata choice: Listen before commit (in transaction). Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        BEFORE_COMMIT,

        /** Listen after successful commit.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the AFTER_COMMIT metadata choice: Listen after successful commit. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        AFTER_COMMIT,

        /** Listen after rollback.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the AFTER_ROLLBACK metadata choice: Listen after rollback. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        AFTER_ROLLBACK,

        /** Listen after completion (commit or rollback).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the AFTER_COMPLETION metadata choice: Listen after completion (commit or rollback). Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        AFTER_COMPLETION,

        /** Listen immediately (no transaction awareness).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IMMEDIATE metadata choice: Listen immediately (no transaction awareness). Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        IMMEDIATE
    }
}
