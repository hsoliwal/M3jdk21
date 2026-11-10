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

import jdk.internal.m3.paradigm.annotation.IMethodSpec;
import jdk.internal.m3.paradigm.annotation.Model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ITransactionSpec - Transaction specification for AI code generation.
 *
 * <h2>Transaction Patterns</h2>
 *
 * <pre>
 * Transaction patterns:
 *   - ACID (local database)
 *   - Saga (distributed, compensation)
 *   - TCC (Try-Confirm-Cancel)
 *   - Outbox pattern
 *   - Event sourcing
 *   - Two-phase commit (2PC)
 *
 * &#64;ITransactionSpec specifies transactional behavior in IOP style.
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * interface IOrderService {
 *
 *     // Simple ACID transaction
 *     &#64;ITransactionSpec(isolation = READ_COMMITTED)
 *     Order createOrder(CreateOrderRequest req);
 *
 *     // Saga with compensation
 * &#64;ITransactionSpec(type = SAGA, steps = {"reserveInventory", "chargePayment", "shipOrder"}, compensations = {
 * "releaseInventory", "refundPayment", "cancelShipment"})
 * Order processOrder(Order order);
 *
 * // Read-only transaction
 * &#64;ITransactionSpec(readOnly = true)
 * List&lt;Order> findOrders(String userId);
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Transaction Specification
 * @see IMethodSpec
 * @see IExecSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares transaction preset, coordination, propagation, isolation, rollback classes, compensation names and timeout; it begins no transaction and supplies no consistency guarantee.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/ITransactionSpec.java}
 * blob {@code 30aee238b552d62d554de874f716850376da7828} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface ITransactionSpec {

    // --- SIMPLE OPTIONS ---

    /**
     * Requested transaction preset.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITransactionSpec#option intent: Requested transaction preset. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Option}; default expression {@code Option.NONE}.
     */
    Option option() default Option.NONE;

    /**
     * Option.
     * @author Synexia
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ITransactionSpec.Option choices described as Transaction presets; enum selection enforces no database behavior. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum Option {
        /**
         * Requested no preset category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Option#NONE policy choice: Requested no preset category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Option; its declared order and identity are preserved.
         */
        NONE,
        /**
         * Requested required transaction category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Option#REQUIRED policy choice: Requested required transaction category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Option; its declared order and identity are preserved.
         */
        REQUIRED,
        /**
         * Requested new transaction category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Option#REQUIRES_NEW policy choice: Requested new transaction category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Option; its declared order and identity are preserved.
         */
        REQUIRES_NEW,
        /**
         * Requested read-only transaction category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Option#READ_ONLY policy choice: Requested read-only transaction category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Option; its declared order and identity are preserved.
         */
        READ_ONLY,
        /**
         * Requested saga coordination category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Option#SAGA policy choice: Requested saga coordination category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Option; its declared order and identity are preserved.
         */
        SAGA,
        /**
         * Requested manual transaction control category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Option#MANUAL policy choice: Requested manual transaction control category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Option; its declared order and identity are preserved.
         */
        MANUAL
    }

    // --- TRANSACTION TYPE ---

    /**
     * Requested transaction coordination category.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITransactionSpec#type intent: Requested transaction coordination category. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Type}; default expression {@code Type.ACID}.
     */
    Type type() default Type.ACID;

    /**
     * Transaction coordination categories; enum selection enforces no database behavior.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ITransactionSpec.Type choices described as Transaction coordination categories; enum selection enforces no database behavior. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum Type {
        /**
         * Requested ACID transaction category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Type#ACID policy choice: Requested ACID transaction category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Type; its declared order and identity are preserved.
         */
        ACID,
        /**
         * Requested saga coordination category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Type#SAGA policy choice: Requested saga coordination category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Type; its declared order and identity are preserved.
         */
        SAGA,
        /**
         * Requested try/confirm/cancel coordination category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Type#TCC policy choice: Requested try/confirm/cancel coordination category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Type; its declared order and identity are preserved.
         */
        TCC,
        /**
         * Requested transactional outbox category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Type#OUTBOX policy choice: Requested transactional outbox category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Type; its declared order and identity are preserved.
         */
        OUTBOX,
        /**
         * Requested eventual consistency category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Type#EVENTUAL policy choice: Requested eventual consistency category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Type; its declared order and identity are preserved.
         */
        EVENTUAL
    }

    // --- PROPAGATION ---

    /**
     * Requested relationship with an existing transaction context.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITransactionSpec#propagation intent: Requested relationship with an existing transaction context. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Propagation}; default expression {@code Propagation.REQUIRED}.
     */
    Propagation propagation() default Propagation.REQUIRED;

    /**
     * Transaction-context relationships; enum selection enforces no database behavior.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ITransactionSpec.Propagation choices described as Transaction-context relationships; enum selection enforces no database behavior. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 7 constants in the existing source order.
     */
    enum Propagation {
        /**
         * Requested participation or creation category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Propagation#REQUIRED policy choice: Requested participation or creation category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Propagation; its declared order and identity are preserved.
         */
        REQUIRED,
        /**
         * Requested new transaction with existing context suspended category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Propagation#REQUIRES_NEW policy choice: Requested new transaction with existing context suspended category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Propagation; its declared order and identity are preserved.
         */
        REQUIRES_NEW,
        /**
         * Requested optional participation category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Propagation#SUPPORTS policy choice: Requested optional participation category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Propagation; its declared order and identity are preserved.
         */
        SUPPORTS,
        /**
         * Requested execution outside a transaction category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Propagation#NOT_SUPPORTED policy choice: Requested execution outside a transaction category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Propagation; its declared order and identity are preserved.
         */
        NOT_SUPPORTED,
        /**
         * Requested existing transaction required category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Propagation#MANDATORY policy choice: Requested existing transaction required category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Propagation; its declared order and identity are preserved.
         */
        MANDATORY,
        /**
         * Requested absence of transaction required category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Propagation#NEVER policy choice: Requested absence of transaction required category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Propagation; its declared order and identity are preserved.
         */
        NEVER,
        /**
         * Requested nested transaction category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Propagation#NESTED policy choice: Requested nested transaction category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Propagation; its declared order and identity are preserved.
         */
        NESTED
    }

    // --- ISOLATION ---

    /**
     * Requested transaction isolation category.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITransactionSpec#isolation intent: Requested transaction isolation category. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Isolation}; default expression {@code Isolation.DEFAULT}.
     */
    Isolation isolation() default Isolation.DEFAULT;

    /**
     * Isolation categories; enum selection enforces no database behavior.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the ITransactionSpec.Isolation choices described as Isolation categories; enum selection enforces no database behavior. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum Isolation {
        /**
         * Requested consumer default isolation category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Isolation#DEFAULT policy choice: Requested consumer default isolation category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Isolation; its declared order and identity are preserved.
         */
        DEFAULT,
        /**
         * Requested read-uncommitted isolation category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Isolation#READ_UNCOMMITTED policy choice: Requested read-uncommitted isolation category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Isolation; its declared order and identity are preserved.
         */
        READ_UNCOMMITTED,
        /**
         * Requested read-committed isolation category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Isolation#READ_COMMITTED policy choice: Requested read-committed isolation category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Isolation; its declared order and identity are preserved.
         */
        READ_COMMITTED,
        /**
         * Requested repeatable-read isolation category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Isolation#REPEATABLE_READ policy choice: Requested repeatable-read isolation category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Isolation; its declared order and identity are preserved.
         */
        REPEATABLE_READ,
        /**
         * Requested serializable isolation category; exact implementation belongs to consuming transaction tooling.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the ITransactionSpec.Isolation#SERIALIZABLE policy choice: Requested serializable isolation category; exact implementation belongs to consuming transaction tooling. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of ITransactionSpec.Isolation; its declared order and identity are preserved.
         */
        SERIALIZABLE
    }

    // --- ROLLBACK ---

    /**
     * Exception classes for which rollback is requested.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITransactionSpec#rollbackFor intent: Exception classes for which rollback is requested. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<? extends Throwable>[]}; default expression {@code {}}.
     */
    Class<? extends Throwable>[] rollbackFor() default {};

    /**
     * Exception classes for which rollback is not requested; this schema defines no precedence against rollbackFor.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITransactionSpec#noRollbackFor intent: Exception classes for which rollback is not requested; this schema defines no precedence against rollbackFor. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<? extends Throwable>[]}; default expression {@code {}}.
     */
    Class<? extends Throwable>[] noRollbackFor() default {};

    // --- SAGA ---

    /**
     * Named transaction or saga steps requested by the specification.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITransactionSpec#steps intent: Named transaction or saga steps requested by the specification. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] steps() default {};

    /**
     * Named compensation actions; the schema validates no positional correspondence with steps.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITransactionSpec#compensations intent: Named compensation actions; the schema validates no positional correspondence with steps. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] compensations() default {};

    // --- TIMEOUT & READ-ONLY ---

    /**
     * Requested transaction timeout in seconds; the preserved default is negative one and has no interpretation implemented here.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITransactionSpec#timeoutSeconds intent: Requested transaction timeout in seconds; the preserved default is negative one and has no interpretation implemented here. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code -1}.
     */
    int timeoutSeconds() default -1;

    /**
     * Requested read-only transaction preference.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITransactionSpec#readOnly intent: Requested read-only transaction preference. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean readOnly() default false;

    // --- DESCRIPTION ---

    /**
     * Natural-language transaction requirements.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITransactionSpec#value intent: Natural-language transaction requirements. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String value() default "";

    /**
     * Preferred code-generation model token.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares ITransactionSpec#model intent: Preferred code-generation model token. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.OPUS}.
     */
    Model.ModelType model() default Model.ModelType.OPUS;
}
