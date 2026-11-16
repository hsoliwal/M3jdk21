/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.Objects;
import jdk.internal.m3.collections.LaneSearch;

/**
 * Immutable paired int lanes sorted by ascending key.
 *
 * <p>Construction copies both inputs, preserves key/value association and rejects
 * duplicate keys, including duplicates whose values happen to match. Ordinal
 * access and primitive callbacks use ascending key order. This M3 API does not
 * implement the boxed java.util.Map interface.
 *
 * <p>The existing PrimitiveFrozenMap paired heap-sort kernel performs construction;
 * LaneSearch performs lookup. Retained payload is exactly two int arrays,
 * with a shared frozen state word and no entry/node or hash-index objects.
 * JDK immutable-map operations and the existing PackedFrozenLongMap are the
 * contract references. No third-party source or runtime dependency is added.
 */
public final class FrozenIntMap {
    static final long STATE_FROZEN = 1L << 0;
    private static final int[] EMPTY_LANE = new int[0];
    private static final long[] FROZEN_STATE = {STATE_FROZEN};

    private final int[] keyLane;
    private final int[] valueLane;
    private final long[] stateMaskWords = FROZEN_STATE;
    private final int size;

    /** Creates an empty immutable map. */
    public FrozenIntMap() {
        this(EMPTY_LANE, EMPTY_LANE);
    }

    private FrozenIntMap(int[] keys, int[] values) {
        keyLane = keys;
        valueLane = values;
        size = keys.length;
    }

    /** Historical descriptor identity; the implementation needs no hash-index lane. */
    public static String scaffoldDescriptor() {
        return "M3FrozenIntMap|FROZEN_INT_MAP|INT|FROZEN|int[] keyLane;int[] valueLane;int[] hashIndexLane";
    }

    public static FrozenIntMap copyOf(int[] keys, int[] values) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(values, "values");
        if (keys.length != values.length) throw new IllegalArgumentException("lane lengths");
        int[] copiedKeys = keys.clone();
        int[] copiedValues = values.clone();
        PrimitiveFrozenMap.heapSortPairs(PrimitiveKind.INT, PrimitiveKind.INT,
                copiedKeys, copiedValues, copiedKeys.length);
        for (int index = 1; index < copiedKeys.length; index++) {
            if (copiedKeys[index - 1] == copiedKeys[index]) {
                throw new IllegalArgumentException("duplicate key: " + copiedKeys[index]);
            }
        }
        return new FrozenIntMap(copiedKeys, copiedValues);
    }

    public int size() {
        return size;
    }

    public boolean containsKey(int key) {
        int index = LaneSearch.lowerBound(keyLane, 0, size, key);
        return index < size && keyLane[index] == key;
    }

    public int getOrDefault(int key, int defaultValue) {
        int index = LaneSearch.lowerBound(keyLane, 0, size, key);
        return index < size && keyLane[index] == key ? valueLane[index] : defaultValue;
    }

    public int keyAt(int ordinal) {
        return keyLane[Objects.checkIndex(ordinal, size)];
    }

    public int valueAt(int ordinal) {
        return valueLane[Objects.checkIndex(ordinal, size)];
    }

    public void forEach(IntIntConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int index = 0; index < size; index++) action.accept(keyLane[index], valueLane[index]);
    }

    /** Always rejected, including an already-present key with an equal value. */
    public int put(int key, int value) {
        throw new UnsupportedOperationException("M3FrozenIntMap is immutable");
    }

}
