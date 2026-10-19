/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable {@code long -> long} map over two parallel lanes sorted by key.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-synexia-to-public/LIB-SUPERSET, leaf LS2):
 * <ul>
 *   <li>JDK {@code Map.ofEntries}/{@code Map.copyOf}: immutable; mutators throw {@link UnsupportedOperationException};
 *       duplicate keys rejected with {@link IllegalArgumentException}; {@code equals}/{@code hashCode} per
 *       {@code java.util.Map} (sum of {@code Long.hashCode(key) ^ Long.hashCode(value)}).</li>
 *   <li>GUAVA {@code ImmutableSortedMap}: ascending key order, {@code keySet().asList().get} / {@code values().asList().get}
 *       as {@link #keyAt}/{@link #valueAt}; GUAVA {@code ImmutableMap}: membership, value lookup, hash and duplicate-key
 *       refusal (insertion order is NOT claimed: this owner iterates by ascending key).</li>
 * </ul>
 * Each provider contract is qualified only against that provider's own oracle; no equivalence is inferred.
 *
 * <p>DOOP mapping: lane kind LONG; {@code keyLane} (sorted distinct keys) and {@code valueLane} (value at the same
 * ordinal) plus the shared FROZEN owner state word; offsets are sorted key ordinals located by binary search (the
 * scaffold's planned hash index lane is not needed for the sorted contract). Stored nodes: NONE; no {@code Map.Entry}
 * is stored or created. Per-element allocation: 0 (construction allocates the two lanes; lookups, ordinal access and
 * {@code forEach} allocate nothing). Donor: first-party Synexia {@code PackedFrozenLongMap} (blob 50729509d916),
 * adapted. Not a {@code java.util} replacement.
 */
public final class M3FrozenLongMap {
    /** Owner state bit {@code FROZEN}: every slot of both lanes is immutable. */
    static final long STATE_FROZEN = 1L << 0;
    private static final long[] EMPTY_LANE = new long[0];
    private static final long[] FROZEN_STATE = {STATE_FROZEN};
    private static final M3FrozenLongMap EMPTY = new M3FrozenLongMap();

    /** Storage lane {@code keyLane}: sorted distinct keys. */
    private final long[] keyLane;
    /** Storage lane {@code valueLane}: value of the key at the same ordinal. */
    private final long[] valueLane;
    /** Owner state word (FROZEN); shared, never mutated; occupancy is implicit (dense lanes). */
    private final long[] stateMaskWords;
    /** Live slot count. */
    private final int size;

    /** Creates an empty frozen map. */
    public M3FrozenLongMap() {
        this(EMPTY_LANE, EMPTY_LANE, 0);
    }

    private M3FrozenLongMap(long[] keyLane, long[] valueLane, int size) {
        this.keyLane = keyLane;
        this.valueLane = valueLane;
        this.stateMaskWords = FROZEN_STATE;
        this.size = size;
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3FrozenLongMap|FROZEN_LONG_MAP|LONG|FROZEN|long[] keyLane;long[] valueLane|RECEIVED";
    }

    /** JDK Map.ofEntries / Guava ImmutableSortedMap.copyOf over parallel arrays; duplicate keys are refused. */
    public static M3FrozenLongMap copyOf(long[] keys, long[] values) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(values, "values");
        if (keys.length != values.length) throw new IllegalArgumentException("keys and values differ in length");
        int n = keys.length;
        if (n == 0) return EMPTY;
        long[] sortedKeys = keys.clone();
        Arrays.sort(sortedKeys);
        for (int i = 1; i < n; i++) {
            if (sortedKeys[i] == sortedKeys[i - 1]) throw new IllegalArgumentException("duplicate key: " + sortedKeys[i]);
        }
        long[] sortedValues = new long[n];
        for (int i = 0; i < n; i++) sortedValues[Arrays.binarySearch(sortedKeys, keys[i])] = values[i];
        return new M3FrozenLongMap(sortedKeys, sortedValues, n);
    }

    /** Map.size. */
    public int size() {
        return size;
    }

    /** Map.isEmpty. */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Map.containsKey: binary search over the sorted key lane. */
    public boolean containsKey(long key) {
        return Arrays.binarySearch(keyLane, 0, size, key) >= 0;
    }

    /** Map.containsValue: linear scan of the value lane. */
    public boolean containsValue(long value) {
        for (int i = 0; i < size; i++) {
            if (valueLane[i] == value) return true;
        }
        return false;
    }

    /** Map.getOrDefault. */
    public long getOrDefault(long key, long defaultValue) {
        int index = Arrays.binarySearch(keyLane, 0, size, key);
        return index >= 0 ? valueLane[index] : defaultValue;
    }

    /** ImmutableSortedMap.keySet().asList().get. */
    public long keyAt(int ordinal) {
        return keyLane[Objects.checkIndex(ordinal, size)];
    }

    /** ImmutableSortedMap.values().asList().get. */
    public long valueAt(int ordinal) {
        return valueLane[Objects.checkIndex(ordinal, size)];
    }

    /** Map.keySet().toArray in ascending key order (defensive copy). */
    public long[] keysToArray() {
        return Arrays.copyOf(keyLane, size);
    }

    /** Map.values().toArray in ascending key order (defensive copy). */
    public long[] valuesToArray() {
        return Arrays.copyOf(valueLane, size);
    }

    /** Map.forEach in ascending key order, without Map.Entry. */
    public void forEach(M3LongLongConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) action.accept(keyLane[i], valueLane[i]);
    }

    /** Immutable: always throws {@link UnsupportedOperationException}. */
    public long put(long key, long value) {
        throw new UnsupportedOperationException("M3FrozenLongMap is immutable");
    }

    /** Immutable: always throws {@link UnsupportedOperationException}. */
    public long remove(long key) {
        throw new UnsupportedOperationException("M3FrozenLongMap is immutable");
    }

    /** Equal to another frozen map with the same mappings. */
    @Override public boolean equals(Object other) {
        return other == this || other instanceof M3FrozenLongMap map
                && Arrays.equals(keyLane, 0, size, map.keyLane, 0, map.size)
                && Arrays.equals(valueLane, 0, size, map.valueLane, 0, map.size);
    }

    /** java.util.Map hash contract: sum of {@code Long.hashCode(key) ^ Long.hashCode(value)}. */
    @Override public int hashCode() {
        int hash = 0;
        for (int i = 0; i < size; i++) hash += Long.hashCode(keyLane[i]) ^ Long.hashCode(valueLane[i]);
        return hash;
    }

    /** Ascending {@code {k=v, ...}} rendering (as TreeMap / ImmutableSortedMap). */
    @Override public String toString() {
        if (size == 0) return "{}";
        StringBuilder text = new StringBuilder(size * 8 + 2).append('{');
        for (int i = 0; i < size; i++) {
            if (i > 0) text.append(", ");
            text.append(keyLane[i]).append('=').append(valueLane[i]);
        }
        return text.append('}').toString();
    }

    int stateWord() {
        return (int) stateMaskWords[0];
    }
}
