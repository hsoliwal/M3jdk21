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
package jdk.internal.m3.collections;

import java.util.NoSuchElementException;

/**
 * Fixed-capacity primitive monotone deque for streaming/sliding-window extrema.
 *
 * <p>Indices must be nonnegative and strictly increasing. Equal values are retained, so
 * the oldest equal candidate wins. Call {@link #evictBefore(int)} before offering when
 * a fixed window advances. No allocation occurs after construction.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/PackedLongMonotoneDeque.java}
 * blob {@code 9f0acf2b8b48b59317cdf0d99ee70e0eb0fd146e} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedLongMonotoneDeque {
    public enum Mode {
        MIN,
        MAX
    }

    private final long[] values;
    private final int[] indexes;
    private final Mode mode;
    private int head;
    private int size;
    private int lastOfferedIndex = -1;

    public M3PackedLongMonotoneDeque(int capacity, Mode mode) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity");
        }
        this.mode = java.util.Objects.requireNonNull(mode, "mode");
        values = new long[capacity];
        indexes = new int[capacity];
    }

    public int capacity() {
        return values.length;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public long payloadBytes() {
        return 12L * values.length;
    }

    public void evictBefore(int minimumIndexInclusive) {
        if (minimumIndexInclusive < 0) {
            throw new IllegalArgumentException("minimumIndexInclusive");
        }
        while (size != 0 && indexes[head] < minimumIndexInclusive) {
            head = (head + 1) % values.length;
            size--;
        }
        if (size == 0) {
            head = 0;
        }
    }

    /**
     * Adds a new candidate while discarding dominated tail values.
     *
     * <p>If capacity would still be exceeded after domination, the method throws before
     * changing deque contents.
     */
    public void offer(int index, long value) {
        if (index < 0 || index <= lastOfferedIndex) {
            throw new IllegalArgumentException("indices must be strictly increasing");
        }

        int dominated = 0;
        while (dominated < size) {
            int tail = slot(size - dominated - 1);
            long existing = values[tail];
            boolean remove = mode == Mode.MIN ? value < existing : value > existing;
            if (!remove) {
                break;
            }
            dominated++;
        }
        if (size - dominated == values.length) {
            throw new IllegalStateException("monotone deque capacity exceeded; evict window first");
        }

        size -= dominated;
        int write = slot(size);
        indexes[write] = index;
        values[write] = value;
        size++;
        lastOfferedIndex = index;
    }

    public int firstIndex() {
        requireNonEmpty();
        return indexes[head];
    }

    public long firstValue() {
        requireNonEmpty();
        return values[head];
    }

    public int lastIndex() {
        requireNonEmpty();
        return indexes[slot(size - 1)];
    }

    public long lastValue() {
        requireNonEmpty();
        return values[slot(size - 1)];
    }

    public void clear() {
        head = 0;
        size = 0;
        lastOfferedIndex = -1;
    }

    private int slot(int logicalOffset) {
        return (head + logicalOffset) % values.length;
    }

    private void requireNonEmpty() {
        if (size == 0) {
            throw new NoSuchElementException("empty monotone deque");
        }
    }
}
