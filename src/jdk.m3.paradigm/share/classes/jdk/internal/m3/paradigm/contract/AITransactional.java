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
 *     // LLM knows: atomic operation, rollback on exception
 * }
 *
 * @AITransactional(readOnly = true)
 * User findUser(String id) {
 *     // LLM knows: read-only, can optimize
 * }
 *
 * &#64;AITransactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE, timeout = 30)
 * void criticalOperation() {
 *     // LLM knows: new TX, serializable, timeout
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Transaction Hints (Spring-inspired)
 * @see <a href=
 *      "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Transactional.html">Spring @Transactional</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AITransactional.java}
 * blob {@code 247f0282dcfbd27f8571e5b57e0520f6fc57a605} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AITransactional {

    /**
     * Transaction manager name hint.
     *
     * @return TX manager name
     */
    String transactionManager() default "";

    /**
     * Propagation behavior.
     *
     * @return propagation
     */
    Propagation propagation() default Propagation.REQUIRED;

    /**
     * Isolation level.
     *
     * @return isolation
     */
    Isolation isolation() default Isolation.DEFAULT;

    /**
     * Timeout in seconds.
     *
     * @return timeout
     */
    int timeout() default -1;

    /**
     * Read-only transaction hint.
     *
     * @return true if read-only
     */
    boolean readOnly() default false;

    /**
     * Exceptions that trigger rollback.
     *
     * @return exception classes
     */
    Class<? extends Throwable>[] rollbackFor() default {};

    /**
     * Exception names that trigger rollback.
     *
     * @return exception names
     */
    String[] rollbackForClassName() default {};

    /**
     * Exceptions that do NOT trigger rollback.
     *
     * @return exception classes
     */
    Class<? extends Throwable>[] noRollbackFor() default {};

    /**
     * Exception names that do NOT trigger rollback.
     *
     * @return exception names
     */
    String[] noRollbackForClassName() default {};

    /**
     * Label for transaction tracing.
     *
     * @return label
     */
    String label() default "";

    // --- ENUMS (Spring-compatible) ---

    /** Transaction propagation behaviors (Spring-compatible). */
    enum Propagation {
        /** Support current TX, create new if none exists. */
        REQUIRED,

        /** Support current TX, execute non-transactionally if none. */
        SUPPORTS,

        /** Support current TX, throw if none exists. */
        MANDATORY,

        /** Create new TX, suspend current if exists. */
        REQUIRES_NEW,

        /** Execute non-transactionally, suspend current if exists. */
        NOT_SUPPORTED,

        /** Execute non-transactionally, throw if TX exists. */
        NEVER,

        /** Execute within nested TX if current exists. */
        NESTED
    }

    /** Transaction isolation levels (Spring-compatible). */
    enum Isolation {
        /** Use default isolation of underlying datastore. */
        DEFAULT,

        /** Dirty reads, non-repeatable reads, phantoms possible. */
        READ_UNCOMMITTED,

        /** Dirty reads prevented; non-repeatable, phantoms possible. */
        READ_COMMITTED,

        /** Dirty, non-repeatable reads prevented; phantoms possible. */
        REPEATABLE_READ,

        /** All prevented (full ACID). */
        SERIALIZABLE
    }
}
