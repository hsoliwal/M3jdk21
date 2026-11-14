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
package jdk.internal.m3.paradigm.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * KafkaListener - Kafka integration intent consumed at compile time.
 *
 * <p>A listener annotation is heavy: it can imply threads, queues, broker binding,
 * ordering, retries, acknowledgements, backpressure and exception semantics. AnOP
 * therefore makes those consequences explicit inputs to generated Java instead of
 * silently installing runtime infrastructure.</p>
 *
 * <pre>
 * &#64;KafkaListener(...)
 *     -> validate contract at compile time
 *     -> emit deterministic AnOP plan
 *     -> materialize explicit consumer Java
 *     -> compile + test
 * </pre>
 *
 * <p>This annotation never starts a thread, queue, retry loop or JDK proxy at
 * runtime. Generated code is ordinary reviewable Java and target-local runtime
 * admission remains separate from the source annotation.</p>
 *
 * @since ContextOS 1.1 - AnOP messaging
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/KafkaListener.java}
 * blob {@code 13f3e6c2aae1408604e5a56fe7e527201c994c83} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface KafkaListener {

    /** Single Kafka topic. */
    String topic() default "";

    /** Multiple Kafka topics. */
    String[] topics() default {};

    /** Consumer group ID. */
    String groupId() default "";

    /** Optional generated/container factory identifier. */
    String containerFactory() default "";

    /** Explicit consumer worker count. */
    int concurrency() default 1;

    /** Bounded handoff/dispatch queue capacity. */
    int queueCapacity() default 1024;

    /** Maximum in-flight records owned by this listener surface. */
    int maxInFlight() default 1024;

    /** Ordering guarantee required by the contract. */
    Ordering ordering() default Ordering.PARTITION;

    /** Backpressure behavior when bounded capacity is exhausted. */
    ConcurrencyPlan.Backpressure backpressure() default ConcurrencyPlan.Backpressure.BLOCK;

    /** Acknowledgement/commit semantics. */
    AckMode ackMode() default AckMode.AFTER_SUCCESS;

    /** Explicit retry attempts; zero means no implicit retry. */
    int retryAttempts() default 0;

    /** Explicit retry delay; zero means no implicit backoff. */
    long retryBackoffMillis() default 0L;

    /** Exception/failure disposition. */
    ExceptionPolicy exceptionPolicy() default ExceptionPolicy.FAIL_FAST;

    /** Required when {@link #exceptionPolicy()} is DEAD_LETTER. */
    String deadLetterTopic() default "";

    /** Poll timeout for generated consumer code. */
    long pollTimeoutMillis() default 1000L;

    /** Maximum records accepted per poll. */
    int maxPollRecords() default 500;

    /** Ordering policies exposed instead of hidden in generated infrastructure. */
    enum Ordering {
        NONE,
        PARTITION,
        KEY,
        GLOBAL
    }

    /** Acknowledgement policies. */
    enum AckMode {
        AUTO,
        AFTER_SUCCESS,
        BATCH_AFTER_SUCCESS,
        MANUAL
    }

    /** Failure semantics. */
    enum ExceptionPolicy {
        FAIL_FAST,
        RETRY_THEN_FAIL,
        DEAD_LETTER,
        HOLD
    }
}
