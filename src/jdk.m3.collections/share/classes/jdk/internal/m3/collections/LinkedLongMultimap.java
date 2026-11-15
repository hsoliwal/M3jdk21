/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import jdk.m3.collections.LongIntHashMap;
import jdk.m3.collections.LongLongConsumer;

/**
 * Primitive long multimap with per-key and global insertion order.
 *
 * <p>The existing primitive hash owner indexes keys to their first occurrence.
 * Payload and topology remain in this owner's primitive lanes; there is no
 * object, collection, boxed key or entry per mapping or distinct key. Removing
 * a key clears and recycles all of its slots. Every long is a valid key/value.
 *
 * <p>Returned arrays are detached snapshots. Traversal calls the supplied
 * primitive consumer directly and detects structural changes by that callback.
 * This owner is not thread-safe and does not implement Guava's live-view API.
 * The insertion-order algorithm shares the existing {@link PackedSlotLinkAtoms}
 * rather than storing or manufacturing linked nodes.
 */
public final class LinkedLongMultimap {
    static final long STATE_OCCUPIED = 1L;
    static final long STATE_DELETED = 2L;
    private static final int NONE = -1;
    private static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;

    private final LongIntHashMap headByKey = new LongIntHashMap(0);
    private long[] keyLane = new long[0];
    private long[] valueLane = new long[0];
    private int[] globalPrevLane = new int[0];
    private int[] globalNextLane = new int[0];
    private int[] keyPrevLane = new int[0];
    private int[] keyNextLane = new int[0];
    /** At an active key head: its tail. At a free slot: the next free slot. */
    private int[] hashIndexLane = new int[0];
    /** Two bits per physical slot: unused, occupied or deleted. */
    private long[] stateMaskWords = new long[0];
    private int size;
    private int used;
    private int first = NONE;
    private int last = NONE;
    private int free = NONE;
    private int modCount;

    public LinkedLongMultimap() { }

    /** Retained compatibility descriptor; it does not describe admission status. */
    public static String scaffoldDescriptor() {
        return "M3LinkedLongMultimap|LINKED_LONG_MULTIMAP|LONG|OCCUPIED,DELETED|long[] keyLane;long[] valueLane;int[] globalPrevLane;int[] globalNextLane;int[] keyPrevLane;int[] keyNextLane;int[] hashIndexLane";
    }

    /** Appends an occurrence, including a duplicate pair, to both encounter orders. */
    public boolean put(long key, long value) {
        if (size == MAX_CAPACITY) {
            throw new OutOfMemoryError("LinkedLongMultimap capacity exhausted");
        }
        int keyHead = headByKey.getOrDefault(key, NONE);
        int row = free != NONE ? free : used;
        if (free == NONE) {
            ensureCapacity(row + 1);
        }
        // The only remaining fallible allocation is the primitive key directory.
        // Publish no live slot or link until that insertion has succeeded.
        if (keyHead == NONE) {
            headByKey.put(key, row);
        }
        if (free != NONE) {
            free = hashIndexLane[row];
        } else {
            used++;
        }
        keyLane[row] = key;
        valueLane[row] = value;
        hashIndexLane[row] = NONE;
        long global = PackedSlotLinkAtoms.linkLast(
                globalPrevLane, globalNextLane, first, last, row);
        first = PackedSlotLinkAtoms.first(global);
        last = PackedSlotLinkAtoms.last(global);
        if (keyHead == NONE) {
            keyHead = row;
            keyPrevLane[row] = NONE;
            keyNextLane[row] = NONE;
        } else {
            PackedSlotLinkAtoms.linkLast(keyPrevLane, keyNextLane,
                    keyHead, hashIndexLane[keyHead], row);
        }
        hashIndexLane[keyHead] = row;
        setState(row, STATE_OCCUPIED);
        size++;
        modCount++;
        return true;
    }

    /** Returns a detached copy in per-key insertion order, or an empty array. */
    public long[] get(long key) {
        int head = headByKey.getOrDefault(key, NONE);
        int count = 0;
        for (int row = head; row != NONE; row = keyNextLane[row]) {
            count++;
        }
        long[] copy = new long[count];
        for (int row = head, index = 0; row != NONE; row = keyNextLane[row]) {
            copy[index++] = valueLane[row];
        }
        return copy;
    }

    /** Removes a complete key chain and returns its detached encounter-order values. */
    public long[] removeAll(long key) {
        int head = headByKey.getOrDefault(key, NONE);
        long[] removed = get(key); // allocate before changing the live owner
        if (head == NONE) {
            return removed;
        }
        headByKey.remove(key);
        for (int row = head; row != NONE; ) {
            int next = keyNextLane[row];
            long endpoints = PackedSlotLinkAtoms.unlink(
                    globalPrevLane, globalNextLane, first, last, row);
            first = PackedSlotLinkAtoms.first(endpoints);
            last = PackedSlotLinkAtoms.last(endpoints);
            keyLane[row] = 0L;
            valueLane[row] = 0L;
            globalPrevLane[row] = globalNextLane[row] = NONE;
            keyPrevLane[row] = keyNextLane[row] = NONE;
            hashIndexLane[row] = free;
            free = row;
            setState(row, STATE_DELETED);
            row = next;
        }
        size -= removed.length;
        modCount++;
        return removed;
    }

    public boolean containsKey(long key) {
        return headByKey.containsKey(key);
    }

    public boolean containsEntry(long key, long value) {
        for (int row = headByKey.getOrDefault(key, NONE);
                row != NONE; row = keyNextLane[row]) {
            if (valueLane[row] == value) {
                return true;
            }
        }
        return false;
    }

    /** Distinct keys ordered by their first live occurrence in global order. */
    public long[] keysToArray() {
        long[] keys = new long[headByKey.size()];
        int index = 0;
        for (int row = first; row != NONE; row = globalNextLane[row]) {
            if (keyPrevLane[row] == NONE) {
                keys[index++] = keyLane[row];
            }
        }
        return keys;
    }

    /** Direct primitive traversal; callback mutation throws after its side effect. */
    public void forEach(LongLongConsumer action) {
        Objects.requireNonNull(action, "action");
        int expected = modCount;
        for (int row = first; row != NONE; row = globalNextLane[row]) {
            action.accept(keyLane[row], valueLane[row]);
            if (modCount != expected) {
                throw new ConcurrentModificationException();
            }
        }
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    /** Clears live values and reusable metadata while retaining allocated capacity. */
    public void clear() {
        if (used == 0) {
            return;
        }
        Arrays.fill(keyLane, 0, used, 0L);
        Arrays.fill(valueLane, 0, used, 0L);
        Arrays.fill(globalPrevLane, 0, used, 0);
        Arrays.fill(globalNextLane, 0, used, 0);
        Arrays.fill(keyPrevLane, 0, used, 0);
        Arrays.fill(keyNextLane, 0, used, 0);
        Arrays.fill(hashIndexLane, 0, used, 0);
        Arrays.fill(stateMaskWords, 0L);
        headByKey.clear();
        size = used = 0;
        first = last = free = NONE;
        modCount++;
    }

    private void setState(int row, long state) {
        int word = row >>> 5;
        int shift = (row & 31) << 1;
        stateMaskWords[word] = (stateMaskWords[word] & ~(3L << shift))
                | (state << shift);
    }

    /** Stage every lane before publishing a larger capacity. */
    private void ensureCapacity(int required) {
        if (required <= keyLane.length) {
            return;
        }
        int capacity = (int) Math.min(MAX_CAPACITY,
                Math.max(required, Math.max(8L,
                        (long) keyLane.length + (keyLane.length >>> 1) + 1)));
        long[] keys = Arrays.copyOf(keyLane, capacity);
        long[] values = Arrays.copyOf(valueLane, capacity);
        int[] globalPrevious = Arrays.copyOf(globalPrevLane, capacity);
        int[] globalNext = Arrays.copyOf(globalNextLane, capacity);
        int[] keyPrevious = Arrays.copyOf(keyPrevLane, capacity);
        int[] keyNext = Arrays.copyOf(keyNextLane, capacity);
        int[] indexes = Arrays.copyOf(hashIndexLane, capacity);
        long[] states = Arrays.copyOf(stateMaskWords,
                (int) (((long) capacity + 31) >>> 5));
        keyLane = keys;
        valueLane = values;
        globalPrevLane = globalPrevious;
        globalNextLane = globalNext;
        keyPrevLane = keyPrevious;
        keyNextLane = keyNext;
        hashIndexLane = indexes;
        stateMaskWords = states;
    }
}
