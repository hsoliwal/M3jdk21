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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Immutable runtime contract materialized from {@link KafkaListener}.
 *
 * <p>The annotation disappears after compilation; this record is the explicit
 * ordinary-Java contract handed to the transport/runtime boundary. It contains
 * every heavy semantic that would otherwise be hidden in a listener container.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/KafkaListenerSpec.java}
 * blob {@code a2bb501713c596162becc6c983ff345da05e1dd8} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record KafkaListenerSpec(
        List<String> topics,
        String groupId,
        String containerFactory,
        int concurrency,
        int queueCapacity,
        int maxInFlight,
        KafkaListener.Ordering ordering,
        ConcurrencyPlan.Backpressure backpressure,
        KafkaListener.AckMode ackMode,
        int retryAttempts,
        long retryBackoffMillis,
        KafkaListener.ExceptionPolicy exceptionPolicy,
        String deadLetterTopic,
        long pollTimeoutMillis,
        int maxPollRecords) {

    public KafkaListenerSpec {
        Objects.requireNonNull(topics, "topics");
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(containerFactory, "containerFactory");
        Objects.requireNonNull(ordering, "ordering");
        Objects.requireNonNull(backpressure, "backpressure");
        Objects.requireNonNull(ackMode, "ackMode");
        Objects.requireNonNull(exceptionPolicy, "exceptionPolicy");
        Objects.requireNonNull(deadLetterTopic, "deadLetterTopic");

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String topic : topics) {
            if (topic != null && !topic.isBlank()) normalized.add(topic.trim());
        }
        topics = List.copyOf(normalized);
        groupId = groupId.trim();
        containerFactory = containerFactory.trim();
        deadLetterTopic = deadLetterTopic.trim();

        if (topics.isEmpty()) throw new IllegalArgumentException("at least one Kafka topic is required");
        if (groupId.isEmpty()) throw new IllegalArgumentException("Kafka groupId is required for explicit adapter materialization");
        if (concurrency < 1 || queueCapacity < 1 || maxInFlight < 1 || maxPollRecords < 1) {
            throw new IllegalArgumentException("Kafka capacities/concurrency must be positive");
        }
        if (pollTimeoutMillis < 1) throw new IllegalArgumentException("pollTimeoutMillis must be positive");
        if (retryAttempts < 0 || retryBackoffMillis < 0) {
            throw new IllegalArgumentException("retry values must be non-negative");
        }
        if (exceptionPolicy == KafkaListener.ExceptionPolicy.DEAD_LETTER && deadLetterTopic.isEmpty()) {
            throw new IllegalArgumentException("DEAD_LETTER requires deadLetterTopic");
        }
    }
}
