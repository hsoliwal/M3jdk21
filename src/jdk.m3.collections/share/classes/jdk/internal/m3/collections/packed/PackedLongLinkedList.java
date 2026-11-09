/* *****************************************************************************
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
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongIterator;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import jdk.internal.m3.collections.LongLinkedSequence;

/**
 * Doubly linked primitive sequence with lazily allocated parallel array segments.
 * No element node or boxed payload is retained. Arbitrary fixed segment sizes,
 * including 1,000 and 5,000, are supported; growth copies directory references only.
 *
 * <p>Handles encode generation and slot+1. A removed slot increments its generation
 * on reuse; an exhausted generation retires the slot permanently. Handles belong
 * only to their originating list. All long payloads are valid, including zero.
 *
 * <p>Known-handle insertion/removal/access is O(1), excluding growth allocation.
 * Logical-position access and value search are O(n). Iterators are fail-fast for
 * structural mutation; replacing a value is nonstructural. Not thread-safe.
 * clear() retains segments and generation history so stale handles stay invalid.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLinkedList.java}
 * blob {@code 9f88b1b9d7263cac24ef9c5a1483c2b89475d092} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLinkedList implements LongLinkedSequence {
    /** Default slots per segment; callers can choose a smaller size for small lists. */
    public static final int DEFAULT_SEGMENT_SIZE = 1024;
    private static final int MAX_SEGMENT_SIZE = 1 << 20;
    private static final int MAX_SLOTS = Integer.MAX_VALUE - 8;
    private static final long[][] EMPTY_VALUES = new long[0][];
    private static final int[][] EMPTY_INTS = new int[0][];

    private final int segmentSize;
    private long[][] values = EMPTY_VALUES;
    private int[][] previous = EMPTY_INTS;
    private int[][] next = EMPTY_INTS;
    private int[][] generations = EMPTY_INTS;
    private int segments;
    private int highWater;
    private int size;
    private int free = -1;
    private int first = -1;
    private int last = -1;
    private long changes;
    private long allocatedSlots;

    /** Creates an empty list without private backing arrays. */
    public PackedLongLinkedList() {
        this(DEFAULT_SEGMENT_SIZE);
    }

    /** Creates an empty list with a fixed segment size in 1..1,048,576. */
    public PackedLongLinkedList(int segmentSize) {
        if (segmentSize < 1 || segmentSize > MAX_SEGMENT_SIZE) {
            throw new IllegalArgumentException("segmentSize must be in 1..1048576");
        }
        this.segmentSize = segmentSize;
    }

    @Override
    public long append(long value) {
        return insertBetween(last, -1, value);
    }

    @Override
    public long prepend(long value) {
        return insertBetween(-1, first, value);
    }

    @Override
    public long insertBefore(long handle, long value) {
        int slot = checkedSlot(handle);
        return insertBetween(read(previous, slot), slot, value);
    }

    @Override
    public long insertAfter(long handle, long value) {
        int slot = checkedSlot(handle);
        return insertBetween(slot, read(next, slot), value);
    }

    private long insertBetween(int before, int after, long value) {
        int slot = allocate(value);
        write(previous, slot, before);
        write(next, slot, after);
        if (before < 0) first = slot;
        else write(next, before, slot);
        if (after < 0) last = slot;
        else write(previous, after, slot);
        size++;
        changes++;
        return handle(slot);
    }

    private int allocate(long value) {
        int slot;
        if (free >= 0) {
            slot = free;
            free = read(next, slot);
            // Only non-exhausted negative generations enter the free list.
            write(generations, slot, 1 - read(generations, slot));
        } else {
            if (highWater == MAX_SLOTS) {
                throw new IllegalStateException("maximum slot address reached");
            }
            slot = highWater;
            ensureSlot(slot);
            highWater++;
            write(generations, slot, 1);
        }
        values[slot / segmentSize][slot % segmentSize] = value;
        return slot;
    }

    private void ensureSlot(int slot) {
        int page = slot / segmentSize;
        if (page < segments) return;
        long[][] v = values;
        int[][] p = previous;
        int[][] n = next;
        int[][] g = generations;
        if (page == v.length) {
            int maxDirectories = (int) ((MAX_SLOTS + (long) segmentSize - 1) / segmentSize);
            int length = (int) Math.min(maxDirectories,
                    Math.max(page + 1L, Math.max(4L, v.length + (v.length >>> 1))));
            v = Arrays.copyOf(v, length);
            p = Arrays.copyOf(p, length);
            n = Arrays.copyOf(n, length);
            g = Arrays.copyOf(g, length);
        }
        int length = (int) Math.min(segmentSize, MAX_SLOTS - (long) page * segmentSize);
        long[] valuePage = new long[length];
        int[] previousPage = new int[length];
        int[] nextPage = new int[length];
        int[] generationPage = new int[length];
        // Commit the complete segment group after all allocations have succeeded.
        v[page] = valuePage;
        p[page] = previousPage;
        n[page] = nextPage;
        g[page] = generationPage;
        values = v;
        previous = p;
        next = n;
        generations = g;
        segments++;
        allocatedSlots += length;
    }

    @Override
    public long removeHandle(long handle) {
        return removeSlot(checkedSlot(handle));
    }

    private long removeSlot(int slot) {
        int before = read(previous, slot);
        int after = read(next, slot);
        if (before < 0) first = after;
        else write(next, before, after);
        if (after < 0) last = before;
        else write(previous, after, before);
        long result = values[slot / segmentSize][slot % segmentSize];
        values[slot / segmentSize][slot % segmentSize] = 0L;
        int generation = read(generations, slot);
        write(generations, slot, -generation);
        write(previous, slot, -1);
        if (generation == Integer.MAX_VALUE) {
            write(next, slot, -1); // Retired: never wrap to a previous handle.
        } else {
            write(next, slot, free);
            free = slot;
        }
        size--;
        changes++;
        return result;
    }

    @Override
    public long valueAt(long handle) {
        int slot = checkedSlot(handle);
        return values[slot / segmentSize][slot % segmentSize];
    }

    @Override
    public long replaceValue(long handle, long value) {
        int slot = checkedSlot(handle);
        long[] page = values[slot / segmentSize];
        int offset = slot % segmentSize;
        long old = page[offset];
        page[offset] = value;
        return old;
    }

    @Override
    public boolean isLiveHandle(long handle) {
        long slot = (handle & 0xffffffffL) - 1;
        int generation = (int) (handle >>> 32);
        return handle > 0 && generation > 0 && slot >= 0 && slot < highWater
                && read(generations, (int) slot) == generation;
    }

    private int checkedSlot(long handle) {
        if (!isLiveHandle(handle)) {
            throw new IllegalArgumentException("invalid or stale handle");
        }
        return (int) ((handle & 0xffffffffL) - 1);
    }

    private long handle(int slot) {
        return slot < 0 ? NONE : ((long) read(generations, slot) << 32) | (slot + 1L);
    }

    @Override
    public long firstHandle() {
        return handle(first);
    }

    @Override
    public long lastHandle() {
        return handle(last);
    }

    @Override
    public long nextHandle(long handle) {
        return handle(read(next, checkedSlot(handle)));
    }

    @Override
    public long previousHandle(long handle) {
        return handle(read(previous, checkedSlot(handle)));
    }

    @Override
    public long get(int index) {
        PackedSupport.checkIndex(index, size);
        int slot;
        if (index < size / 2) {
            slot = first;
            for (int i = 0; i < index; i++) slot = read(next, slot);
        } else {
            slot = last;
            for (int i = size - 1; i > index; i--) slot = read(previous, slot);
        }
        return values[slot / segmentSize][slot % segmentSize];
    }

    @Override
    public boolean contains(long value) {
        for (int slot = first; slot >= 0; slot = read(next, slot)) {
            if (values[slot / segmentSize][slot % segmentSize] == value) return true;
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        while (first >= 0) removeSlot(first);
    }

    @Override
    public long[] toArray() {
        long[] result = new long[size];
        int offset = 0;
        for (int slot = first; slot >= 0; slot = read(next, slot)) {
            result[offset++] = values[slot / segmentSize][slot % segmentSize];
        }
        return result;
    }

    @Override
    public LongIterator iterator() {
        return new LongIterator() {
            private final long expectedChanges = changes;
            private int slot = first;

            private void checkForModification() {
                if (expectedChanges != changes) throw new ConcurrentModificationException();
            }

            @Override
            public boolean hasNext() {
                checkForModification();
                return slot >= 0;
            }

            @Override
            public long nextLong() {
                checkForModification();
                if (slot < 0) throw new NoSuchElementException();
                long result = values[slot / segmentSize][slot % segmentSize];
                slot = read(next, slot);
                return result;
            }
        };
    }

    /** Fixed number of slots per full segment. */
    public int segmentSize() {
        return segmentSize;
    }

    /** Number of allocated segment groups; zero before the first insertion. */
    public int allocatedSegments() {
        return segments;
    }

    /** Physical slot capacity, including free, retired, and not-yet-used slots. */
    public int capacity() {
        return (int) allocatedSlots;
    }

    /** Primitive lane bytes only; excludes array headers, references, and object fields. */
    public long arrayPayloadBytes() {
        return 20L * allocatedSlots;
    }

    private int read(int[][] lane, int slot) {
        return lane[slot / segmentSize][slot % segmentSize];
    }

    private void write(int[][] lane, int slot, int value) {
        lane[slot / segmentSize][slot % segmentSize] = value;
    }
}
