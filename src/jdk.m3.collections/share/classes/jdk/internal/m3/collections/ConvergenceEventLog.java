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
package jdk.internal.m3.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
/**
 * Event Log - append-only convergence history.
 *
 * <p>
 * From ChatGPT: "You can always replay. But you never depend on stored state."
 * </p>
 *
 * <h2>Core Properties</h2>
 * <ul>
 * <li><b>Append-only</b> - Events are never modified or deleted</li>
 * <li><b>Ordered</b> - Events have monotonically increasing sequence
 * numbers</li>
 * <li><b>Complete</b> - Contains entire history (Add/Drop/Consolidate)</li>
 * <li><b>Replayable</b> - Memory can be reconstructed from events</li>
 * </ul>
 *
 * <h2>Invariants</h2>
 * <ul>
 * <li>Sequence numbers are unique and increasing</li>
 * <li>Events are immutable once appended</li>
 * <li>Log order matches event order</li>
 * </ul>
 *
 * @author Hitesh Soliwal (hsoliwal@gmail.com)
 * @see ConvergenceEvent
 * @see ConvergenceProjection
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/convergence/ConvergenceEventLog.java}
 * blob {@code 39c29e902ad1522943b50edc4b2c78594e0ab21d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public class ConvergenceEventLog {
    private static final PlaceholderLogger log = PlaceholderLogger.of(ConvergenceEventLog.class);

    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public ConvergenceEventLog() {
    }


    private final List<ConvergenceEvent> events = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    /**
     * Gets the next sequence number.
     *
     * @return next sequence number (monotonically increasing)
     */
    public long nextSeq() {
        return sequence.incrementAndGet();
    }

    /**
     * Appends an event to the log.
     *
     * <p>
     * Thread-safe: synchronized on the event list.
     * </p>
     *
     * @param event
     *            the event to append
     */
    public synchronized void append(ConvergenceEvent event) {
        events.add(event);
    }

    /**
     * Gets all events (immutable copy).
     *
     * @return list of all events in order
     */
    public synchronized List<ConvergenceEvent> all() {
        return List.copyOf(events);
    }

    /**
     * Gets the number of events in the log.
     *
     * @return event count
     */
    public synchronized int size() {
        return events.size();
    }

    /**
     * Checks if the log is empty.
     *
     * @return true if no events
     */
    public synchronized boolean isEmpty() {
        return events.isEmpty();
    }

    /**
     * Gets events after a specific sequence number.
     *
     * <p>
     * Useful for incremental replay.
     * </p>
     *
     * @param afterSeq
     *            sequence number to start after
     * @return events with seq > afterSeq
     */
    public synchronized List<ConvergenceEvent> after(long afterSeq) {
        return events.stream().filter(e -> e.seq() > afterSeq).toList();
    }

    /**
     * Gets statistics about the event log.
     *
     * @return log statistics
     */
    public synchronized LogStats stats() {
        int[] counts = {0, 0, 0}; // adds, drops, consolidates

        events.forEach(e -> {
            switch (e) {
                case ConvergenceEvent.AddEvent ignored -> counts[0]++;
                case ConvergenceEvent.DropEvent ignored -> counts[1]++;
                case ConvergenceEvent.ConsolidateEvent ignored -> counts[2]++;
            }
        });

        return new LogStats(events.size(), counts[0], counts[1], counts[2], sequence.get());
    }

    /**
     * Event log statistics.
     *
     * @param total
     *            total event count
     * @param adds
     *            add event count
     * @param drops
     *            drop event count
     * @param consolidates
     *            consolidate event count
     * @param lastSeq
     *            last sequence number
     */
    public record LogStats(int total, int adds, int drops, int consolidates, long lastSeq) {
        private static final TokenInterner INTERNER = TokenInterner.getInstance();
        private static final String FORMAT_COMPACT = INTERNER.intern("📊 Events: %d (+%d -%d ~%d) seq=%d");

        /**
         * Creates a compact single-line representation.
         *
         * @return formatted stats line
         */
        public String toCompactLine() {
            return String.format(java.util.Locale.ROOT, FORMAT_COMPACT, total, adds, drops, consolidates, lastSeq);
        }
    }
}
