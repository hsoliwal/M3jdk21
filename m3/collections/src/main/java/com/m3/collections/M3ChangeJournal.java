/*******************************************************************************
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
package com.m3.collections;

import java.util.Objects;

/**
 * Bounded, parent-owned primitive change history for indexed M3 state.
 *
 * <p>This journal is an optional sidecar, not an object store, notification bus,
 * workspace transaction log or scheduler. The caller supplies an owner-issued
 * identity (including its generation where applicable), a full 32-bit change
 * mask and two unsigned byte state words. Recording an event does not validate
 * that identity against its owner or send callbacks. Existing M3LazyEvents,
 * provider notifications and Eclipse/EMF contracts retain their authority.</p>
 *
 * <p>Three existing M3 primitive lanes retain at most {@code capacity} events:
 * one long identity and two int words per row. Sequence numbers are implicit,
 * strictly increasing, and never recycled. Overwritten history is reported as
 * a gap; consumers must reobserve their authoritative source after a gap.
 * Each synchronized snapshot returns a detached, bounded primitive page with
 * no per-event Entry/Notification allocations.</p>
 */
public final class M3ChangeJournal {
    private final int capacity;
    private final M3LongArrayList identities;
    private final M3IntArrayList changeMasks;
    private final M3IntArrayList statePairs;
    private long newestSequence;

    public M3ChangeJournal(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
        identities = new M3LongArrayList(capacity);
        changeMasks = new M3IntArrayList(capacity);
        statePairs = new M3IntArrayList(capacity);
    }

    public int capacity() { return capacity; }

    public synchronized int retainedCount() { return identities.size(); }

    public synchronized long newestSequence() { return newestSequence; }

    /** Zero only before the first recorded event. */
    public synchronized long oldestRetainedSequence() {
        return newestSequence == 0L ? 0L : newestSequence - identities.size() + 1L;
    }

    /**
     * Commit one change after validating both unsigned 8-bit object-state values.
     * Full-width change masks (including the sign bit) and every long identity
     * are preserved. Sequence overflow refuses before any logical mutation.
     * Array growth is prepared before extending any lane.
     *
     * @return the new monotone, journal-local sequence
     */
    public synchronized long record(long identity, int changeMask, int oldState, int newState) {
        if ((oldState & ~0xff) != 0 || (newState & ~0xff) != 0) {
            throw new IllegalArgumentException("states must be unsigned bytes");
        }
        long next = Math.addExact(newestSequence, 1L);
        int physical = (int) ((next - 1L) % capacity);
        int statePair = (oldState << 8) | newState;
        if (identities.size() < capacity) {
            int required = identities.size() + 1;
            identities.ensureCapacity(required);
            changeMasks.ensureCapacity(required);
            statePairs.ensureCapacity(required);
            identities.add(identity);
            changeMasks.add(changeMask);
            statePairs.add(statePair);
        } else {
            identities.set(physical, identity);
            changeMasks.set(physical, changeMask);
            statePairs.set(physical, statePair);
        }
        newestSequence = next;
        return next;
    }

    /**
     * Return at most {@code maxEvents} entries strictly after a prior cursor.
     * A cursor belongs to this journal, begins at zero, and must not be ahead
     * of its newest event. A gap signals loss of at least one retained event;
     * the caller must resynchronize rather than infer an unchanged source.
     */
    public synchronized Snapshot snapshotAfter(long afterSequence, int maxEvents) {
        if (maxEvents < 1) throw new IllegalArgumentException("maxEvents must be positive");
        if (afterSequence < 0L || afterSequence > newestSequence) {
            throw new IllegalArgumentException("invalid or future journal cursor");
        }
        if (afterSequence == newestSequence) {
            return new Snapshot(afterSequence, 0L,
                    new long[0], new int[0], new int[0]);
        }
        long oldest = oldestRetainedSequence();
        long missed = afterSequence < oldest - 1L ? oldest - afterSequence - 1L : 0L;
        long first = missed > 0L ? oldest : afterSequence + 1L;
        int count = (int) Math.min((long) maxEvents, newestSequence - first + 1L);
        long[] identitiesPage = new long[count];
        int[] masksPage = new int[count];
        int[] statesPage = new int[count];
        for (int i = 0; i < count; i++) {
            int physical = (int) ((first + i - 1L) % capacity);
            identitiesPage[i] = identities.get(physical);
            masksPage[i] = changeMasks.get(physical);
            statesPage[i] = statePairs.get(physical);
        }
        return new Snapshot(first + count - 1L, missed,
                identitiesPage, masksPage, statesPage);
    }

    /** An immutable detached primitive page; accessors never expose its arrays. */
    public static final class Snapshot {
        private final long nextCursor;
        private final long missedEvents;
        private final long[] identities;
        private final int[] changeMasks;
        private final int[] statePairs;

        private Snapshot(long nextCursor, long missedEvents, long[] identities,
                int[] changeMasks, int[] statePairs) {
            this.nextCursor = nextCursor;
            this.missedEvents = missedEvents;
            this.identities = Objects.requireNonNull(identities);
            this.changeMasks = Objects.requireNonNull(changeMasks);
            this.statePairs = Objects.requireNonNull(statePairs);
        }

        public int size() { return identities.length; }
        public boolean overrun() { return missedEvents != 0L; }
        public long missedEvents() { return missedEvents; }
        public long nextCursor() { return nextCursor; }
        public long sequence(int index) {
            return nextCursor - identities.length + 1L + Objects.checkIndex(index, size());
        }
        public long identity(int index) { return identities[Objects.checkIndex(index, size())]; }
        public int changeMask(int index) { return changeMasks[Objects.checkIndex(index, size())]; }
        public int oldState(int index) { return (statePairs[Objects.checkIndex(index, size())] >>> 8) & 0xff; }
        public int newState(int index) { return statePairs[Objects.checkIndex(index, size())] & 0xff; }
    }
}
