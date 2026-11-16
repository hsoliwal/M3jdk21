/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.Objects;
import jdk.internal.m3.collections.LaneSearch;

/**
 * Immutable paired long lanes sorted by ascending key.
 *
 * <p>Construction copies both inputs, preserves key/value association and rejects
 * duplicate keys, including duplicates whose values happen to match. Ordinal
 * access and primitive callbacks use ascending key order. This M3 API does not
 * implement the boxed java.util.Map interface.
 *
 * <p>The existing PrimitiveFrozenMap paired heap-sort kernel performs construction;
 * LaneSearch performs lookup. Retained payload is exactly two long arrays,
 * with a shared frozen state word and no entry/node or hash-index objects.
 * JDK immutable-map operations and the existing PackedFrozenLongMap are the
 * contract references. No third-party source or runtime dependency is added.
 */
public final class FrozenLongMap {
    static final long STATE_FROZEN = 1L << 0;
    private static final long[] EMPTY_LANE = new long[0];
    private static final long[] FROZEN_STATE = {STATE_FROZEN};

    private final long[] keyLane;
    private final long[] valueLane;
    private final long[] stateMaskWords = FROZEN_STATE;
    private final int size;

    /** Creates an empty immutable map. */
    public FrozenLongMap() {
        this(EMPTY_LANE, EMPTY_LANE);
    }

    private FrozenLongMap(long[] keys, long[] values) {
        keyLane = keys;
        valueLane = values;
        size = keys.length;
    }

    /** Historical descriptor identity; the implementation needs no hash-index lane. */
    public static String scaffoldDescriptor() {
        return "M3FrozenLongMap|FROZEN_LONG_MAP|LONG|FROZEN|long[] keyLane;long[] valueLane;int[] hashIndexLane";
    }

    public static FrozenLongMap copyOf(long[] keys, long[] values) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(values, "values");
        if (keys.length != values.length) throw new IllegalArgumentException("lane lengths");
        long[] copiedKeys = keys.clone();
        long[] copiedValues = values.clone();
        PrimitiveFrozenMap.heapSortPairs(PrimitiveKind.LONG, PrimitiveKind.LONG,
                copiedKeys, copiedValues, copiedKeys.length);
        for (int index = 1; index < copiedKeys.length; index++) {
            if (copiedKeys[index - 1] == copiedKeys[index]) {
                throw new IllegalArgumentException("duplicate key: " + copiedKeys[index]);
            }
        }
        return new FrozenLongMap(copiedKeys, copiedValues);
    }

    public int size() {
        return size;
    }

    public boolean containsKey(long key) {
        int index = LaneSearch.lowerBound(keyLane, 0, size, key);
        return index < size && keyLane[index] == key;
    }

    public long getOrDefault(long key, long defaultValue) {
        int index = LaneSearch.lowerBound(keyLane, 0, size, key);
        return index < size && keyLane[index] == key ? valueLane[index] : defaultValue;
    }

    public long keyAt(int ordinal) {
        return keyLane[Objects.checkIndex(ordinal, size)];
    }

    public long valueAt(int ordinal) {
        return valueLane[Objects.checkIndex(ordinal, size)];
    }

    public void forEach(LongLongConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int index = 0; index < size; index++) action.accept(keyLane[index], valueLane[index]);
    }

    /** Always rejected, including an already-present key with an equal value. */
    public long put(long key, long value) {
        throw new UnsupportedOperationException("M3FrozenLongMap is immutable");
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containsValue(long value) {
        for (long candidate : valueLane) if (candidate == value) return true;
        return false;
    }

    public long[] keysToArray() {
        return keyLane.clone();
    }

    public long[] valuesToArray() {
        return valueLane.clone();
    }

    /** Always rejected, including an absent key. */
    public long remove(long key) {
        throw new UnsupportedOperationException("M3FrozenLongMap is immutable");
    }

}
