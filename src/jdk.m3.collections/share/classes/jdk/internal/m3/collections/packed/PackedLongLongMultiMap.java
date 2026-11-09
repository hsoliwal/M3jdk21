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
package jdk.internal.m3.collections.packed;

import jdk.internal.m3.collections.LongLongMultiMap;
import jdk.m3.collections.LongLongConsumer;
import jdk.m3.collections.LongLongPredicate;

import java.util.Arrays;
import java.util.Objects;

/**
 * Primitive one-to-many {@code long -> long} multimap without per-value collection objects.
 *
 * <p>Each distinct key is indexed in packed primitive hash maps. Values live in shared
 * primitive entry lanes ({@code long[] keys}, {@code long[] values}, {@code int[] next},
 * {@code byte[] states}). Removed slots are recycled through a primitive free-list.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLongMultiMap.java}
 * blob {@code 524d685cb973df79038c5db7577e996745a637c3} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (U1 batch (v6): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLongMultiMap implements LongLongMultiMap {
    private static final byte FREE = 0;
    private static final byte LIVE = 1;

    private final PackedLongLongHashMap heads;
    private final PackedLongLongHashMap tails;
    private final PackedLongLongHashMap counts;

    private long[] entryKeys;
    private long[] entryValues;
    private int[] next;
    private byte[] states;
    private int allocated;
    private int freeHead = -1;
    private int size;

    public PackedLongLongMultiMap() {
        this(8, 16);
    }

    public PackedLongLongMultiMap(int expectedKeys, int expectedValues) {
        if (expectedKeys < 0 || expectedValues < 0) {
            throw new IllegalArgumentException("expected sizes");
        }
        heads = new PackedLongLongHashMap(expectedKeys);
        tails = new PackedLongLongHashMap(expectedKeys);
        counts = new PackedLongLongHashMap(expectedKeys);
        int capacity = Math.max(1, expectedValues);
        entryKeys = new long[capacity];
        entryValues = new long[capacity];
        next = new int[capacity];
        Arrays.fill(next, -1);
        states = new byte[capacity];
    }

    public void add(long key, long value) {
        int slot = allocateSlot();
        entryKeys[slot] = key;
        entryValues[slot] = value;
        next[slot] = -1;
        states[slot] = LIVE;

        long tailPlusOne = tails.getOrDefault(key, 0L);
        if (tailPlusOne == 0L) {
            heads.put(key, slot + 1L);
        } else {
            next[(int) tailPlusOne - 1] = slot;
        }
        tails.put(key, slot + 1L);
        counts.addTo(key, 1L, 0L);
        size++;
    }

    public boolean containsKey(long key) {
        return heads.containsKey(key);
    }

    public boolean containsEntry(long key, long value) {
        int index = headIndex(key);
        while (index >= 0) {
            if (entryValues[index] == value) {
                return true;
            }
            index = next[index];
        }
        return false;
    }

    public int valueCount(long key) {
        long count = counts.getOrDefault(key, 0L);
        if (count > Integer.MAX_VALUE) {
            throw new IllegalStateException("value count exceeds int range");
        }
        return (int) count;
    }

    public long[] values(long key) {
        int count = valueCount(key);
        long[] result = new long[count];
        int index = headIndex(key);
        int write = 0;
        while (index >= 0) {
            result[write++] = entryValues[index];
            index = next[index];
        }
        return result;
    }

    public boolean remove(long key, long value) {
        int previous = -1;
        int current = headIndex(key);
        while (current >= 0) {
            if (entryValues[current] == value) {
                unlink(key, previous, current);
                return true;
            }
            previous = current;
            current = next[current];
        }
        return false;
    }

    public int removeAll(long key) {
        int current = headIndex(key);
        if (current < 0) {
            return 0;
        }
        int removed = 0;
        while (current >= 0) {
            int nextIndex = next[current];
            recycleSlot(current);
            current = nextIndex;
            removed++;
        }
        heads.remove(key);
        tails.remove(key);
        counts.remove(key);
        size -= removed;
        return removed;
    }

    /**
     * Two-phase primitive filtering. Predicates observe the untouched multimap,
     * including recycled-slot order. Commit compacts surviving physical slots
     * into the same lanes and rebuilds the three primitive indexes in one pass.
     * No temporary per-survivor pair lanes, Entry, Node or list are retained.
     */
    public int filterInPlace(LongLongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        if (size == 0) {
            return 0;
        }
        int oldAllocated = allocated;
        // Index decisions by LIVE ordinal, not historical high-water slots.
        long[] rejected = new long[(int) ((size + 63L) >>> 6)];
        int seen = 0;
        int removed = 0;
        for (int read = 0; read < oldAllocated; read++) {
            if (states[read] == LIVE) {
                if (!predicate.test(entryKeys[read], entryValues[read])) {
                    rejected[seen >>> 6] |= 1L << (seen & 63);
                    removed++;
                }
                seen++;
            }
        }
        if (removed == 0) {
            return 0;
        }

        // The predicate phase finished successfully: only now change authority.
        heads.clear();
        tails.clear();
        counts.clear();
        int write = 0;
        seen = 0;
        for (int read = 0; read < oldAllocated; read++) {
            if (states[read] != LIVE) {
                continue;
            }
            boolean drop = (rejected[seen >>> 6] & (1L << (seen & 63))) != 0L;
            seen++;
            if (drop) {
                continue;
            }
            long key = entryKeys[read];
            entryKeys[write] = key;
            entryValues[write] = entryValues[read];
            next[write] = -1;
            states[write] = LIVE;
            long previousTail = tails.getOrDefault(key, 0L);
            if (previousTail == 0L) {
                heads.put(key, write + 1L);
            } else {
                next[(int) previousTail - 1] = write;
            }
            tails.put(key, write + 1L);
            counts.addTo(key, 1L, 0L);
            write++;
        }
        Arrays.fill(states, write, oldAllocated, FREE);
        Arrays.fill(next, write, oldAllocated, -1);
        allocated = write;
        freeHead = -1;
        size = write;
        return removed;
    }

    public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < allocated; i++) {
            if (states[i] == LIVE) {
                consumer.accept(entryKeys[i], entryValues[i]);
            }
        }
    }

    public int distinctKeyCount() {
        return heads.size();
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        heads.clear();
        tails.clear();
        counts.clear();
        Arrays.fill(states, FREE);
        Arrays.fill(next, -1);
        allocated = 0;
        freeHead = -1;
        size = 0;
    }

    private void unlink(long key, int previous, int current) {
        int successor = next[current];
        if (previous < 0) {
            if (successor < 0) {
                heads.remove(key);
                tails.remove(key);
                counts.remove(key);
            } else {
                heads.put(key, successor + 1L);
                counts.addTo(key, -1L, 0L);
            }
        } else {
            next[previous] = successor;
            if (successor < 0) {
                tails.put(key, previous + 1L);
            }
            counts.addTo(key, -1L, 0L);
        }
        recycleSlot(current);
        size--;
    }

    private int headIndex(long key) {
        long headPlusOne = heads.getOrDefault(key, 0L);
        return headPlusOne == 0L ? -1 : (int) headPlusOne - 1;
    }

    private int allocateSlot() {
        if (freeHead >= 0) {
            int slot = freeHead;
            freeHead = next[slot];
            return slot;
        }
        ensureCapacity(allocated + 1);
        return allocated++;
    }

    private void recycleSlot(int slot) {
        states[slot] = FREE;
        next[slot] = freeHead;
        freeHead = slot;
    }

    private void ensureCapacity(int minimum) {
        if (minimum <= entryKeys.length) {
            return;
        }
        int oldLength = entryKeys.length;
        int capacity = PackedFlatArrays.growthCapacity(oldLength, minimum);
        entryKeys = Arrays.copyOf(entryKeys, capacity);
        entryValues = Arrays.copyOf(entryValues, capacity);
        next = Arrays.copyOf(next, capacity);
        Arrays.fill(next, oldLength, capacity, -1);
        states = Arrays.copyOf(states, capacity);
    }
}
