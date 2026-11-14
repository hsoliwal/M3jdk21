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
 * AITransactional - AnOP annotation for transaction boundary hints.
 *
 * <h2>Spring Inspiration, IOP Authority</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ SPRING: @Transactional → runtime proxy manages TX boundaries ║
 * ║ AnOP: @AITransactional → compile-time TX boundary declaration ║
 * ║ ║
 * ║ Spring says: "I will wrap this in a transaction" ║
 * ║ AnOP says: "This method requires transactional semantics" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT create transactions. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * &#64;AITransactional
 * void saveUser(User user) {
 * 	// LLM knows: atomic operation, rollback on exception
 * }
 *
 * @AITransactional(readOnly = true)
 * User findUser(String id) {
 * 	// LLM knows: read-only, can optimize
 * }
 *
 * &#64;AITransactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE, timeout = 30)
 * void criticalOperation() {
 * 	// LLM knows: new TX, serializable, timeout
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Transaction Hints (Spring-inspired)
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Transactional.html">Spring @Transactional</a>
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares manager, propagation, isolation, timeout, read-only, rollback and tracing hints without opening or committing transactions.
 * @m3.dataStructures SOURCE-retained annotation with exact target {@code {ElementType.TYPE, ElementType.METHOD}}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAITransactional.java}
 * blob {@code 867440671fba821a53b1c229ef826fb38992039c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAITransactional {

    /**
     * Transaction manager name hint.
     *
     * @return TX manager name
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares transactionManager metadata for Transaction manager name hint. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String transactionManager() default "";

    /**
     * Propagation behavior.
     *
     * @return propagation
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares propagation metadata for Propagation behavior. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Propagation}, with exact default {@code Propagation.REQUIRED}.
     */
    Propagation propagation() default Propagation.REQUIRED;

    /**
     * Isolation level.
     *
     * @return isolation
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares isolation metadata for Isolation level. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Isolation}, with exact default {@code Isolation.DEFAULT}.
     */
    Isolation isolation() default Isolation.DEFAULT;

    /**
     * Timeout in seconds.
     *
     * @return timeout
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares timeout metadata for Timeout in seconds. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}, with exact default {@code -1}.
     */
    int timeout() default -1;

    /**
     * Read-only transaction hint.
     *
     * @return true if read-only
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares readOnly metadata for Read-only transaction hint. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}, with exact default {@code false}.
     */
    boolean readOnly() default false;

    /**
     * Exceptions that trigger rollback.
     *
     * @return exception classes
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares rollbackFor metadata for Exceptions that trigger rollback. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<? extends Throwable>[]}, with exact default {@code {}}.
     */
    Class<? extends Throwable>[] rollbackFor() default {};

    /**
     * Exception names that trigger rollback.
     *
     * @return exception names
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares rollbackForClassName metadata for Exception names that trigger rollback. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}, with exact default {@code {}}.
     */
    String[] rollbackForClassName() default {};

    /**
     * Exceptions that do NOT trigger rollback.
     *
     * @return exception classes
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares noRollbackFor metadata for Exceptions that do NOT trigger rollback. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<? extends Throwable>[]}, with exact default {@code {}}.
     */
    Class<? extends Throwable>[] noRollbackFor() default {};

    /**
     * Exception names that do NOT trigger rollback.
     *
     * @return exception names
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares noRollbackForClassName metadata for Exception names that do NOT trigger rollback. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}, with exact default {@code {}}.
     */
    String[] noRollbackForClassName() default {};

    /**
     * Label for transaction tracing.
     *
     * @return label
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares label metadata for Label for transaction tracing. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String label() default "";

    // --- ENUMS (Spring-compatible) ---

    /** Transaction propagation behaviors (Spring-compatible).
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 7 symbolic transaction propagation hints for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAITransactional.Propagation enum with constants in existing declaration order.
     */
    enum Propagation {
        /** Support current TX, create new if none exists.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the REQUIRED metadata choice: Support current TX, create new if none exists. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        REQUIRED,

        /** Support current TX, execute non-transactionally if none.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SUPPORTS metadata choice: Support current TX, execute non-transactionally if none. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        SUPPORTS,

        /** Support current TX, throw if none exists.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the MANDATORY metadata choice: Support current TX, throw if none exists. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        MANDATORY,

        /** Create new TX, suspend current if exists.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the REQUIRES_NEW metadata choice: Create new TX, suspend current if exists. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        REQUIRES_NEW,

        /** Execute non-transactionally, suspend current if exists.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the NOT_SUPPORTED metadata choice: Execute non-transactionally, suspend current if exists. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        NOT_SUPPORTED,

        /** Execute non-transactionally, throw if TX exists.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the NEVER metadata choice: Execute non-transactionally, throw if TX exists. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        NEVER,

        /** Execute within nested TX if current exists.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the NESTED metadata choice: Execute within nested TX if current exists. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        NESTED
    }

    /** Transaction isolation levels (Spring-compatible).
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 5 symbolic transaction isolation hints for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAITransactional.Isolation enum with constants in existing declaration order.
     */
    enum Isolation {
        /** Use default isolation of underlying datastore.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the DEFAULT metadata choice: Use default isolation of underlying datastore. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        DEFAULT,

        /** Dirty reads, non-repeatable reads, phantoms possible.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the READ_UNCOMMITTED metadata choice: Dirty reads, non-repeatable reads, phantoms possible. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        READ_UNCOMMITTED,

        /** Dirty reads prevented; non-repeatable, phantoms possible.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the READ_COMMITTED metadata choice: Dirty reads prevented; non-repeatable, phantoms possible. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        READ_COMMITTED,

        /** Dirty, non-repeatable reads prevented; phantoms possible.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the REPEATABLE_READ metadata choice: Dirty, non-repeatable reads prevented; phantoms possible. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        REPEATABLE_READ,

        /** All prevented (full ACID).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SERIALIZABLE metadata choice: All prevented (full ACID). Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        SERIALIZABLE
    }
}
