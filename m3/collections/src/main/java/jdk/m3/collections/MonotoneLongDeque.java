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
package jdk.m3.collections;

import java.util.NoSuchElementException;

/**
 * Bounded monotone deque over (sequenceIndex,value) primitive lanes.
 *
 * <p>MIN removes larger/equal tail values; MAX removes smaller/equal tail
 * values. Equal values therefore keep the newest index, which dominates the
 * older one for future sliding-window eviction.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/MonotoneLongDeque.java}
 * blob {@code 3da42b034216a57d057c3bed4534dfac64a38abc} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class MonotoneLongDeque {
    public enum Mode { MIN, MAX }

    private final Mode mode;
    private final long[] indexes;
    private final long[] values;
    private int head;
    private int size;
    private long lastOfferedIndex = Long.MIN_VALUE;
    private boolean hasOffered;

    public MonotoneLongDeque(int capacity, Mode mode) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity <= 0");
        this.mode = java.util.Objects.requireNonNull(mode, "mode");
        indexes = new long[capacity];
        values = new long[capacity];
    }

    public int capacity() { return values.length; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public Mode mode() { return mode; }
    public long payloadBytes() {
        return (long) values.length * Long.BYTES * 2L;
    }

    public void offer(long sequenceIndex, long value) {
        if (hasOffered && sequenceIndex <= lastOfferedIndex) {
            throw new IllegalArgumentException("sequence indexes must strictly increase");
        }

        while (size != 0 && dominated(peekLastValue(), value)) {
            removeLast();
        }
        if (size == values.length) {
            throw new IllegalStateException("monotone deque capacity exhausted");
        }

        int tail = physical(size);
        indexes[tail] = sequenceIndex;
        values[tail] = value;
        size++;
        lastOfferedIndex = sequenceIndex;
        hasOffered = true;
    }

    /** Evicts every entry whose sequence index is less than minIndexInclusive. */
    public void evictBefore(long minIndexInclusive) {
        while (size != 0 && indexes[head] < minIndexInclusive) {
            indexes[head] = 0L;
            values[head] = 0L;
            head = (head + 1) % values.length;
            size--;
        }
    }

    /** Convenience sliding-window operation: evict first, then offer. */
    public void slide(long sequenceIndex, long value, long minIndexInclusive) {
        evictBefore(minIndexInclusive);
        offer(sequenceIndex, value);
    }

    public long firstValue() {
        if (size == 0) throw new NoSuchElementException();
        return values[head];
    }

    public long firstIndex() {
        if (size == 0) throw new NoSuchElementException();
        return indexes[head];
    }

    //FLLDCIM: MRL - census3 P3-006, donor MIndexMonotoneLongDeque blob 4ec10eec0a07: tail accessors (the tail is the latest offer under either equal-value policy)
    public long lastIndex() {
        requireNonEmpty();
        return indexes[physical(size - 1)];
    }

    public long lastValue() {
        requireNonEmpty();
        return values[physical(size - 1)];
    }

    public void clear() {
        M3RingLiveClear.clear(indexes, head, size);
        M3RingLiveClear.clear(values, head, size);
        head = 0;
        size = 0;
        lastOfferedIndex = Long.MIN_VALUE;
        hasOffered = false;
    }

    private long peekLastValue() {
        return values[physical(size - 1)];
    }

    private void removeLast() {
        int tail = physical(size - 1);
        indexes[tail] = 0L;
        values[tail] = 0L;
        size--;
    }

    private int physical(int logicalOffset) {
        int index = head + logicalOffset;
        return index >= values.length ? index - values.length : index;
    }

    //FLLDCIM: MRL - census3 P3-006, donor MIndexMonotoneLongDeque blob 4ec10eec0a07: empty guard of the restored tail accessors
    private void requireNonEmpty() {
        if (size == 0) {
            throw new NoSuchElementException("empty monotone deque");
        }
    }

    private boolean dominated(long retained, long incoming) {
        return mode == Mode.MIN ? retained >= incoming : retained <= incoming;
    }
}
