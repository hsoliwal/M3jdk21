/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable primitive bijection preserving its input pair ordinals.
 *
 * <p>Private long lanes contain each key and value once. The existing packed
 * row index stores only integer ordinals and handles zero and all signed-long
 * values without a payload sentinel. A cached reciprocal inverse swaps the
 * same two payload lanes and their indexes; no payload is copied for a view.
 *
 * <p>The selected Guava ImmutableBiMap frozen-bijection lineage is retained
 * from the existing FROZEN_LONG_BIMAP owner and LIBRARY_API_MATRIX.tsv, run
 * 20261017-m3-to-public/LIB-SUPERSET. This is a primitive projection of that
 * selected contract, not a Guava interface or a public JDK map replacement.
 */
public final class FrozenLongBiMap {
    static final long STATE_FROZEN = 1L << 0;
    private static final long[] EMPTY = new long[0];

    private final long[] keyLane;
    private final long[] valueLane;
    private final PackedLongRowIndex forwardIndex;
    private final PackedLongRowIndex inverseIndex;
    private final long[] stateMaskWords;
    private final int size;
    private final FrozenLongBiMap inverse;

    public FrozenLongBiMap() {
        this(EMPTY, EMPTY, new PackedLongRowIndex(0), new PackedLongRowIndex(0), null);
    }

    private FrozenLongBiMap(long[] keys, long[] values, PackedLongRowIndex forward,
            PackedLongRowIndex reverse, FrozenLongBiMap inverseOf) {
        keyLane = keys;
        valueLane = values;
        forwardIndex = forward;
        inverseIndex = reverse;
        size = keys.length;
        if (inverseOf != null) {
            stateMaskWords = inverseOf.stateMaskWords;
        } else {
            int tail = size & 63;
            stateMaskWords = new long[(size >>> 6) + (tail == 0 ? 0 : 1)];
            Arrays.fill(stateMaskWords, -1L);
            if (tail != 0) stateMaskWords[stateMaskWords.length - 1] = (1L << tail) - 1;
        }
        inverse = inverseOf != null ? inverseOf : size == 0 ? this
                : new FrozenLongBiMap(values, keys, reverse, forward, this);
    }

    /** Retained compatibility descriptor; packed indexes own the integer lanes. */
    public static String scaffoldDescriptor() {
        return "M3FrozenLongBiMap|FROZEN_LONG_BIMAP|LONG|FROZEN|long[] keyLane;long[] valueLane;int[] forwardIndexLane;int[] inverseIndexLane";
    }

    public static FrozenLongBiMap copyOf(long[] keys, long[] values) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(values, "values");
        if (keys.length != values.length) {
            throw new IllegalArgumentException("key/value length mismatch");
        }
        // Refuse unsupported index sizes before allocating detached payloads.
        PackedLongRowIndex.capacityFor(keys.length);
        long[] keyCopy = keys.clone();
        long[] valueCopy = values.clone();
        PackedLongRowIndex forward = new PackedLongRowIndex(keyCopy.length);
        PackedLongRowIndex reverse = reverseOf(keyCopy, valueCopy, forward);
        return new FrozenLongBiMap(keyCopy, valueCopy, forward, reverse, null);
    }

    private static PackedLongRowIndex reverseOf(long[] keyCopy, long[] valueCopy, PackedLongRowIndex forward) {
        PackedLongRowIndex reverse = new PackedLongRowIndex(keyCopy.length);
        for (int ordinal = 0; ordinal < keyCopy.length; ordinal++) {
            if (forward.find(keyCopy[ordinal], keyCopy) >= 0) {
                throw new IllegalArgumentException("duplicate key");
            }
            if (reverse.find(valueCopy[ordinal], valueCopy) >= 0) {
                throw new IllegalArgumentException("duplicate value");
            }
            forward.insert(ordinal, keyCopy);
            reverse.insert(ordinal, valueCopy);
        }
        return reverse;
    }

    public int size() { return size; }
    public boolean containsKey(long key) { return forwardIndex.find(key, keyLane) >= 0; }
    public boolean containsValue(long value) { return inverseIndex.find(value, valueLane) >= 0; }

    public long getOrDefault(long key, long defaultValue) {
        int ordinal = forwardIndex.find(key, keyLane);
        return ordinal < 0 ? defaultValue : valueLane[ordinal];
    }

    public long inverseGetOrDefault(long value, long defaultKey) {
        int ordinal = inverseIndex.find(value, valueLane);
        return ordinal < 0 ? defaultKey : keyLane[ordinal];
    }

    public long keyAt(int ordinal) { return keyLane[Objects.checkIndex(ordinal, size)]; }
    public long valueAt(int ordinal) { return valueLane[Objects.checkIndex(ordinal, size)]; }
    public FrozenLongBiMap inverse() { return inverse; }

    public long put(long key, long value) {
        throw new UnsupportedOperationException("immutable bimap");
    }

    public long forcePut(long key, long value) {
        throw new UnsupportedOperationException("immutable bimap");
    }

    public long remove(long key) {
        throw new UnsupportedOperationException("immutable bimap");
    }
}
