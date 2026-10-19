/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable list-multimap {@code long -> long*} in compressed-sparse-row (CSR) form: sorted distinct key lane, offset
 * lane of length {@code keyCount + 1}, and one value lane holding each key's values contiguously in insertion order.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-synexia-to-public/LIB-SUPERSET, leaf LS3):
 * <ul>
 *   <li>GUAVA {@code ImmutableListMultimap}: {@code size} = key/value pairs (duplicates kept), {@code get(key)} = that
 *       key's values in insertion order, {@code containsKey}/{@code containsEntry}, {@code keySet().size()},
 *       {@code hashCode} = {@code asMap().hashCode()}, mutators throw {@link UnsupportedOperationException}. Key
 *       iteration order is ascending here (Guava keeps first-insertion key order: NOT claimed). Guava
 *       {@code ImmutableSetMultimap} (pair de-duplication) is NOT claimed.</li>
 * </ul>
 *
 * <p>DOOP mapping: lane kind LONG; {@code keyLane}, {@code valueOffsetLane} (CSR: key ordinal {@code k} owns value
 * offsets {@code [valueOffsetLane[k], valueOffsetLane[k + 1])}) and {@code valueLane}, plus the shared FROZEN owner
 * state word. Stored nodes: NONE (no per-key collection objects, no Map.Entry). Per-element allocation: 0 for
 * queries and {@code forEach}; {@link #get} returns a defensive copy of one key's run. Build is O(n log n) with no
 * boxing: sort + dedupe keys, count per key, prefix-sum offsets, stable scatter. Donor: first-party Synexia
 * {@code PackedLongLongMultiMap} / {@code LongLongMultiMap} pattern, adapted. Not a {@code java.util} replacement.
 */
public final class M3FrozenLongMultimap {
    /** Owner state bit {@code FROZEN}: every slot of all lanes is immutable. */
    static final long STATE_FROZEN = 1L << 0;
    private static final long[] EMPTY_LANE = new long[0];
    private static final int[] EMPTY_OFFSETS = {0};
    private static final long[] FROZEN_STATE = {STATE_FROZEN};
    private static final M3FrozenLongMultimap EMPTY = new M3FrozenLongMultimap();

    /** Storage lane {@code keyLane}: sorted distinct keys. */
    private final long[] keyLane;
    /** Storage lane {@code valueOffsetLane}: CSR offsets, length keyCount + 1. */
    private final int[] valueOffsetLane;
    /** Storage lane {@code valueLane}: values grouped by key ordinal, insertion order within a key. */
    private final long[] valueLane;
    /** Owner state word (FROZEN); shared, never mutated. */
    private final long[] stateMaskWords;
    /** Live key slot count. */
    private final int size;

    /** Creates an empty frozen multimap. */
    public M3FrozenLongMultimap() {
        this(EMPTY_LANE, EMPTY_OFFSETS, EMPTY_LANE, 0);
    }

    private M3FrozenLongMultimap(long[] keyLane, int[] valueOffsetLane, long[] valueLane, int size) {
        this.keyLane = keyLane;
        this.valueOffsetLane = valueOffsetLane;
        this.valueLane = valueLane;
        this.stateMaskWords = FROZEN_STATE;
        this.size = size;
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3FrozenLongMultimap|FROZEN_LONG_MULTIMAP|LONG|FROZEN|long[] keyLane;int[] valueOffsetLane;long[] valueLane|RECEIVED";
    }

    /** Guava ImmutableListMultimap from parallel (key, value) pairs; duplicates kept, per-key order preserved. */
    public static M3FrozenLongMultimap copyOf(long[] keys, long[] values) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(values, "values");
        if (keys.length != values.length) throw new IllegalArgumentException("keys and values differ in length");
        int n = keys.length;
        if (n == 0) return EMPTY;
        long[] distinct = keys.clone();
        Arrays.sort(distinct);
        int keyCount = 1;
        for (int i = 1; i < n; i++) {
            if (distinct[i] != distinct[keyCount - 1]) distinct[keyCount++] = distinct[i];
        }
        int[] offsets = new int[keyCount + 1];
        for (int i = 0; i < n; i++) offsets[Arrays.binarySearch(distinct, 0, keyCount, keys[i]) + 1]++;
        for (int k = 0; k < keyCount; k++) offsets[k + 1] += offsets[k];
        int[] cursor = Arrays.copyOf(offsets, keyCount);
        long[] lane = new long[n];
        for (int i = 0; i < n; i++) lane[cursor[Arrays.binarySearch(distinct, 0, keyCount, keys[i])]++] = values[i];
        return new M3FrozenLongMultimap(keyCount == n ? distinct : Arrays.copyOf(distinct, keyCount), offsets, lane, keyCount);
    }

    /** Multimap.size: number of key/value pairs. */
    public int size() {
        return valueOffsetLane[size];
    }

    /** Multimap.keySet().size. */
    public int keyCount() {
        return size;
    }

    /** Multimap.containsKey. */
    public boolean containsKey(long key) {
        return Arrays.binarySearch(keyLane, 0, size, key) >= 0;
    }

    /** Multimap.containsEntry. */
    public boolean containsEntry(long key, long value) {
        int k = Arrays.binarySearch(keyLane, 0, size, key);
        if (k < 0) return false;
        for (int i = valueOffsetLane[k]; i < valueOffsetLane[k + 1]; i++) {
            if (valueLane[i] == value) return true;
        }
        return false;
    }

    /** Multimap.get(key).size: 0 when absent. */
    public int valueCount(long key) {
        int k = Arrays.binarySearch(keyLane, 0, size, key);
        return k < 0 ? 0 : valueOffsetLane[k + 1] - valueOffsetLane[k];
    }

    /** Key at the ascending key ordinal. */
    public long keyAt(int keyOrdinal) {
        return keyLane[Objects.checkIndex(keyOrdinal, size)];
    }

    /** CSR offset of the key ordinal; {@code valueOffsetAt(keyCount())} is the pair count. */
    public int valueOffsetAt(int keyOrdinal) {
        return valueOffsetLane[Objects.checkIndex(keyOrdinal, size + 1)];
    }

    /** Value at the CSR value ordinal. */
    public long valueAt(int valueOrdinal) {
        return valueLane[Objects.checkIndex(valueOrdinal, valueOffsetLane[size])];
    }

    /** Multimap.get: copy of the key's values in insertion order (empty when absent). */
    public long[] get(long key) {
        int k = Arrays.binarySearch(keyLane, 0, size, key);
        return k < 0 ? EMPTY_LANE : Arrays.copyOfRange(valueLane, valueOffsetLane[k], valueOffsetLane[k + 1]);
    }

    /** Multimap.forEach: ascending keys, insertion order within a key. */
    public void forEach(M3LongLongConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int k = 0; k < size; k++) {
            for (int i = valueOffsetLane[k]; i < valueOffsetLane[k + 1]; i++) action.accept(keyLane[k], valueLane[i]);
        }
    }

    /** Immutable: always throws {@link UnsupportedOperationException}. */
    public boolean put(long key, long value) {
        throw new UnsupportedOperationException("M3FrozenLongMultimap is immutable");
    }

    /** Immutable: always throws {@link UnsupportedOperationException}. */
    public long[] removeAll(long key) {
        throw new UnsupportedOperationException("M3FrozenLongMultimap is immutable");
    }

    /** Equal to another frozen multimap with the same keys and per-key value sequences. */
    @Override public boolean equals(Object other) {
        return other == this || other instanceof M3FrozenLongMultimap map
                && Arrays.equals(keyLane, 0, size, map.keyLane, 0, map.size)
                && Arrays.equals(valueOffsetLane, 0, size + 1, map.valueOffsetLane, 0, map.size + 1)
                && Arrays.equals(valueLane, 0, size(), map.valueLane, 0, map.size());
    }

    /** ListMultimap hash: {@code asMap().hashCode()} = sum of {@code Long.hashCode(key) ^ List.hashCode(values)}. */
    @Override public int hashCode() {
        int hash = 0;
        for (int k = 0; k < size; k++) {
            int list = 1;
            for (int i = valueOffsetLane[k]; i < valueOffsetLane[k + 1]; i++) list = 31 * list + Long.hashCode(valueLane[i]);
            hash += Long.hashCode(keyLane[k]) ^ list;
        }
        return hash;
    }

    /** {@code {k=[v, ...], ...}} rendering with ascending keys. */
    @Override public String toString() {
        StringBuilder text = new StringBuilder(size * 10 + 2).append('{');
        for (int k = 0; k < size; k++) {
            if (k > 0) text.append(", ");
            text.append(keyLane[k]).append("=[");
            for (int i = valueOffsetLane[k]; i < valueOffsetLane[k + 1]; i++) {
                if (i > valueOffsetLane[k]) text.append(", ");
                text.append(valueLane[i]);
            }
            text.append(']');
        }
        return text.append('}').toString();
    }

    int stateWord() {
        return (int) stateMaskWords[0];
    }
}
