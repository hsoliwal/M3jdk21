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
import java.util.UUID;
import jdk.internal.m3.collections.TokenInterner;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * AbstractEventStore - Base implementation for IEventStore.
 *
 * <p>Provides event sourcing with template method pattern:
 * <ul>
 *   <li>Append-only, immutable event log</li>
 *   <li>Thread-safe via CopyOnWriteArrayList</li>
 *   <li>Replay for time travel</li>
 * </ul>
 *
 * <h2>Template Method Pattern</h2>
 * <p>Subclasses can override:
 * <ul>
 *   <li>{@link #doAppend(IStoredEvent)} - Custom append logic</li>
 *   <li>{@link #doReplay()} - Custom replay logic</li>
 *   <li>{@link #onAppend(IStoredEvent)} - Callback after append</li>
 *   <li>{@link #onReplay()} - Callback before replay</li>
 * </ul>
 *
 * <h2>Event Sourcing Principles</h2>
 * <ul>
 *   <li>Events are immutable - no updates</li>
 *   <li>Append-only - no deletes</li>
 *   <li>Replay = time travel</li>
 * </ul>
 *
 * @since ContextOS 1.1
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/eip/AbstractEventStore.java}
 * blob {@code 2e6d3a61e68f447c121cbd3b3d9e6d27e955e4c4} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractEventStore implements IEventStore {

    // --- INTERNED STRING CONSTANTS ---
    private static final TokenInterner INTERNER = TokenInterner.getInstance();

    private static final String ERR_EVENT_NULL = INTERNER.intern("event cannot be null");
    private static final String PREFIX_EVENTSTORE = INTERNER.intern("EventStore[events=");
    private static final String BRACKET_CLOSE = INTERNER.intern("]");

    // --- STATE ---
    private final List<IStoredEvent> log = new CopyOnWriteArrayList<>();

    /** Create an event store. */
    protected AbstractEventStore() {
        // Default constructor
    }

    @Override
    public void append(IStoredEvent event) {
        if (event == null) {
            throw new NullPointerException(ERR_EVENT_NULL);
        }

        // Allow subclass to handle append
        if (doAppend(event)) {
            return;
        }

        log.add(event);
        onAppend(event);
    }

    @Override
    public Stream<IStoredEvent> replay() {
        onReplay();

        // Allow subclass to handle replay
        Optional<Stream<IStoredEvent>> customReplay = doReplay();
        if (customReplay.isPresent()) {
            return customReplay.get();
        }

        return new ArrayList<>(log).stream();
    }

    @Override
    public Stream<IStoredEvent> replayFrom(UUID fromId) {
        if (fromId == null) {
            return replay();
        }

        // Find the index of the event with matching ID
        List<IStoredEvent> snapshot = new ArrayList<>(log);
        int startIndex = java.util.stream.IntStream.range(0, snapshot.size())
                .filter(i -> snapshot.get(i).id().equals(fromId))
                .findFirst()
                .orElse(-1);

        if (startIndex < 0 || startIndex >= snapshot.size() - 1) {
            return Stream.empty();
        }

        return snapshot.stream().skip(startIndex + 1);
    }

    @Override
    public long count() {
        return log.size();
    }

    // --- Template Methods (Override for Custom Behavior) ---

    /**
     * Custom append logic.
     *
     * @param event the event to append
     * @return true if handled (skip default), false to use default
     */
    protected boolean doAppend(IStoredEvent event) {
        return false; // Default: use standard append
    }

    /**
     * Custom replay logic.
     *
     * @return custom stream if handled, empty to use default
     */
    protected Optional<Stream<IStoredEvent>> doReplay() {
        return Optional.empty(); //FLLDCIM: MRL — FM-36 return null -> Optional.empty()
    }

    /**
     * Callback after event is appended.
     *
     * @param event the appended event
     */
    protected void onAppend(IStoredEvent event) {
        // Default: no-op
    }

    /** Callback before replay starts. */
    protected void onReplay() {
        // Default: no-op
    }

    // --- Accessors for Subclasses ---

    /**
     * Get the internal log (read-only).
     *
     * @return unmodifiable view of the log
     */
    protected List<IStoredEvent> getLog() {
        return Collections.unmodifiableList(log);
    }

    /**
     * Get the last event.
     *
     * @return optional containing last event
     */
    protected Optional<IStoredEvent> lastEvent() {
        return log.isEmpty() ? Optional.empty() : Optional.of(log.get(log.size() - 1));
    }

    /**
     * Get the first event.
     *
     * @return optional containing first event
     */
    protected Optional<IStoredEvent> firstEvent() {
        return log.isEmpty() ? Optional.empty() : Optional.of(log.get(0));
    }

    /**
     * Check if store is empty.
     *
     * @return true if no events
     */
    protected boolean isEmpty() {
        return log.isEmpty();
    }

    /**
     * Get events by type.
     *
     * @param type event type string
     * @return stream of events matching type
     */
    protected Stream<IStoredEvent> byType(String type) {
        return replay().filter(e -> e.type().equals(type));
    }

    @Override
    public String toString() {
        return PREFIX_EVENTSTORE + log.size() + BRACKET_CLOSE;
    }
}
