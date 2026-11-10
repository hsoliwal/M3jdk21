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
package jdk.internal.m3.paradigm.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.time.Instant;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * IEventStore - Event Sourcing pattern implementation.
 *
 * <p>From Grand Java Patterns Proof:
 * <pre>
 *   Event Sourcing:
 *   - Append-only. No mutation.
 *   - Single DB log.
 *   - Replay = time travel.
 *
 *   Replace with Postgres / RocksDB / LMDB trivially.
 * </pre>
 *
 * <p>Event types:
 * <ul>
 *   <li>LineIngested - raw input received</li>
 *   <li>GlyphExtracted - semantic analysis complete</li>
 *   <li>MemoryStored - projected to memory</li>
 * </ul>
 *
 * @since Grand Java Patterns Proof - Event Sourcing Section
 */
@ISpec(
    value = "Event Sourcing - append-only log with replay capability (time travel)",
    constraints = {
        "Events are immutable - no updates allowed",
        "Append-only, single DB log",
        "Replay = time travel"
    },
    invariants = {
        "append() adds event to end of log",
        "replay() returns events in append order",
        "count() returns total event count"
    },
    examples = {
        "IEventStore.inMemory()",
        "store.append(event)",
        "store.replay().forEach(e -> process(e))"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/eip/IEventStore.java}
 * blob {@code 7ffc0c39c46815012277250121c7f1c9b8ae9709} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IEventStore {

    /**
     * Append an event to the store.
     * Events are immutable - no updates allowed.
     *
     * @param event event to append
     */
    void append(IStoredEvent event);

    /**
     * Replay all events in order.
     *
     * @return stream of events in append order
     */
    Stream<IStoredEvent> replay();

    /**
     * Replay events from a specific point.
     *
     * @param fromId start from this event ID (exclusive)
     * @return stream of events after the given ID
     */
    Stream<IStoredEvent> replayFrom(UUID fromId);

    /**
     * Get the count of stored events.
     *
     * @return event count
     */
    long count();

    // --- EVENT INTERFACE ---

    /** Base interface for stored events. */
    interface IStoredEvent {
        UUID id();
        Instant timestamp();
        String type();
    }

    /** Line ingested event - raw transcript line. */
    record LineIngested(
            UUID id,
            Instant timestamp,
            String line
    ) implements IStoredEvent {
        public LineIngested(String line) {
            this(UUID.randomUUID(), Instant.now(), line);
        }

        @Override
        public String type() { return "LINE_INGESTED"; }
    }

    /** Glyph extracted event - semantic analysis. */
    record GlyphExtracted(
            UUID id,
            Instant timestamp,
            UUID sourceId,
            String intentGlyph,
            String emotionGlyph,
            String correctnessGlyph,
            String directionGlyph
    ) implements IStoredEvent {
        @Override
        public String type() { return "GLYPH_EXTRACTED"; }
    }

    /** Memory stored event - persisted to memory. */
    record MemoryStored(
            UUID id,
            Instant timestamp,
            UUID sourceId,
            Map<String, Object> memory
    ) implements IStoredEvent {
        @Override
        public String type() { return "MEMORY_STORED"; }
    }

    // --- FACTORY ---

    /**
     * Create an in-memory event store.
     *
     * @return in-memory store
     */
    static IEventStore inMemory() {
        return new InMemoryEventStore();
    }

    // --- DEFAULT IMPLEMENTATION ---

    /**
     * Simple in-memory implementation.
     * Thread-safe, suitable for testing.
     */
    class InMemoryEventStore implements IEventStore {
        private final List<IStoredEvent> log = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void append(IStoredEvent event) {
            log.add(event);
        }

        @Override
        public Stream<IStoredEvent> replay() {
            return new ArrayList<>(log).stream();
        }

        @Override
        public Stream<IStoredEvent> replayFrom(UUID fromId) {
            return log.stream()
                    .dropWhile(e -> !e.id().equals(fromId))
                    .skip(1);
        }

        @Override
        public long count() {
            return log.size();
        }
    }
}
