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

import java.util.List;
import java.util.Objects;

/**
 * Explicit transport boundary consumed by generated Kafka adapters.
 *
 * <p>No broker client, thread, queue, scheduler, reflection or proxy is installed
 * by AnOP. A concrete runtime owns those mechanics and must honor the supplied
 * {@link KafkaListenerSpec}; the generated adapter owns dispatch, retry,
 * acknowledgement and failure disposition.</p>
 *
 * <p>Each generated adapter permits only one active {@code runOnce} call. A
 * terminal dispatch, transport, acknowledgement, cancellation or HOLD failure
 * halts that adapter. Before creating a replacement, the owner must recover the
 * runtime's unresolved polled records (for example by seeking to safe committed
 * offsets). Recreating an adapter alone does not recover consumer position.</p>
 *
 * <p>These are at-least-once boundaries, not an exactly-once transaction across
 * application effects, dead-letter publication and offset commits. A commit
 * failure can leave its outcome unknown. AUTO acknowledgement remains explicitly
 * runtime-owned and does not acquire the after-success guarantees below.</p>
 *
 * @param <V> listener payload type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/KafkaListenerRuntime.java}
 * blob {@code c282a4af43dd8b64593b97b5a763b66c5010eb32} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface KafkaListenerRuntime<V> extends AutoCloseable {

    /** One immutable inbound Kafka-like record. */
    record Record<V>(String topic, int partition, long offset, String key, V value) {
        public Record {
            Objects.requireNonNull(topic, "topic");
            if (topic.isBlank()) throw new IllegalArgumentException("topic is required");
            if (partition < 0) throw new IllegalArgumentException("partition must be non-negative");
            if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        }
    }

    /**
     * Poll records in partition order, bounded by both {@code maxPollRecords} and
     * {@code maxInFlight}. Return a stable list containing no null descriptors;
     * null payload values are permitted. Manual-commit modes must not commit
     * records here. The runtime retains recovery responsibility for an unfinished
     * batch; generated adapters reject oversized/invalid batches before dispatch.
     */
    List<Record<V>> poll(KafkaListenerSpec spec);

    /**
     * Acknowledge a record after successful handling or confirmed dead-letter
     * publication. Never advance a partition's commit beyond unresolved earlier
     * records. Failure escapes the adapter without re-running its handler; it
     * requires runtime recovery rather than a business-operation retry.
     */
    void acknowledge(Record<V> record);

    /**
     * Acknowledge a fully resolved batch. The default implementation is sequential,
     * not atomic: earlier acknowledgements may succeed before a later one fails.
     * Every supplied record has completed handling or confirmed dead-letter routing.
     */
    default void acknowledgeBatch(List<Record<V>> records) {
        Objects.requireNonNull(records, "records");
        records.forEach(this::acknowledge);
    }

    /**
     * Publish a terminal handler failure to the declared dead-letter destination.
     * Return only after the runtime has confirmed publication under its durability
     * contract; throw if publication fails or is uncertain. Do not acknowledge the
     * source record here: generated code does so only after this method returns.
     */
    void deadLetter(Record<V> record, Throwable failure, String deadLetterTopic);

    /**
     * Preserve an unresolved record for target-local/manual disposition without
     * acknowledging it. Generated code then stops the batch and halts the adapter,
     * preventing later offsets from being processed or acknowledged by that instance.
     */
    void hold(Record<V> record, Throwable failure);

    /** Runtime implementations may release broker resources; default is no-op. */
    @Override
    default void close() {}
}
