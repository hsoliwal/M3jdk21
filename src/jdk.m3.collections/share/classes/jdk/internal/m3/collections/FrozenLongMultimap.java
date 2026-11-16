/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import jdk.m3.collections.LongIntHashMap;
import jdk.m3.collections.LongLongConsumer;

/**
 * Immutable primitive multimap with stable key grouping and packed value runs.
 *
 * <p>Keys retain first-occurrence order; each key's values retain their input
 * order, including duplicates. A prefix-offset lane maps a key ordinal to its
 * half-open value run. The existing primitive hash owner provides key-to-ordinal
 * lookup, without a collection, box or node per key/value. Input and returned
 * arrays are never retained as externally mutable backing storage.
 *
 * <p>This is the existing primitive CSR API, not a Guava interface replacement.
 * It accepts all long bit patterns and has no JNI or external runtime dependency.
 */
public final class FrozenLongMultimap {
    static final long STATE_FROZEN = 1L;
    private final long[] keyLane;
    private final int[] valueOffsetLane;
    private final long[] valueLane;
    private final long[] stateMaskWords;
    private final LongIntHashMap keyOrdinals;
    private final int size;

    /** Creates a valid immutable empty multimap. */
    public FrozenLongMultimap() {
        this(new long[0], new int[] {0}, new long[0], new LongIntHashMap(0));
    }

    private FrozenLongMultimap(long[] keys, int[] offsets, long[] values,
            LongIntHashMap ordinals) {
        keyLane = keys;
        valueOffsetLane = offsets;
        valueLane = values;
        keyOrdinals = ordinals;
        size = values.length;
        stateMaskWords = new long[(int) (((long) keys.length + 63) >>> 6)];
        Arrays.fill(stateMaskWords, -1L);
        if ((keys.length & 63) != 0) {
            stateMaskWords[stateMaskWords.length - 1] = (1L << (keys.length & 63)) - 1;
        }
    }

    /** Retained compatibility descriptor; it does not describe admission status. */
    public static String scaffoldDescriptor() {
        return "M3FrozenLongMultimap|FROZEN_LONG_MULTIMAP|LONG|FROZEN|long[] keyLane;int[] valueOffsetLane;long[] valueLane";
    }

    /**
     * Copies parallel pair arrays into stable key groups using counting/prefix
     * offsets. Expected linear work with suitable hashes; all temporary arrays
     * and the new owner remain unpublished until construction completes.
     */
    public static FrozenLongMultimap copyOf(long[] keys, long[] values) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(values, "values");
        if (keys.length != values.length) {
            throw new IllegalArgumentException("key/value array lengths differ");
        }
        if (keys.length == 0) {
            return new FrozenLongMultimap();
        }
        LongIntHashMap ordinals = new LongIntHashMap(0);
        long[] encountered = new long[keys.length];
        int[] counts = new int[keys.length];
        int distinct = distinctOf(keys, ordinals, encountered, counts);
        int[] offsets = new int[distinct + 1];
        for (int i = 0; i < distinct; i++) {
            offsets[i + 1] = offsets[i] + counts[i];
        }
        int[] next = Arrays.copyOf(offsets, distinct);
        long[] packedValues = new long[values.length];
        for (int i = 0; i < keys.length; i++) {
            int ordinal = ordinals.get(keys[i]);
            packedValues[next[ordinal]++] = values[i];
        }
        return new FrozenLongMultimap(Arrays.copyOf(encountered, distinct),
                offsets, packedValues, ordinals);
    }

    private static int distinctOf(long[] keys, LongIntHashMap ordinals, long[] encountered, int[] counts) {
        int distinct = 0;
        for (long key : keys) {
            int ordinal = ordinals.getOrDefault(key, -1);
            if (ordinal < 0) {
                ordinal = distinct++;
                encountered[ordinal] = key;
                ordinals.put(key, ordinal);
            }
            counts[ordinal]++;
        }
        // One extra cell holds the terminal prefix boundary.
        if (distinct == Integer.MAX_VALUE) {
            throw new OutOfMemoryError("FrozenLongMultimap offset capacity exhausted");
        }
        return distinct;
    }

    public int size() { return size; }
    public int keyCount() { return keyLane.length; }

    public boolean containsKey(long key) {
        return keyOrdinals.containsKey(key);
    }

    public boolean containsEntry(long key, long value) {
        int ordinal = keyOrdinals.getOrDefault(key, -1);
        if (ordinal < 0) {
            return false;
        }
        for (int i = valueOffsetLane[ordinal]; i < valueOffsetLane[ordinal + 1]; i++) {
            if (valueLane[i] == value) {
                return true;
            }
        }
        return false;
    }

    public int valueCount(long key) {
        int ordinal = keyOrdinals.getOrDefault(key, -1);
        return ordinal < 0 ? 0 : valueOffsetLane[ordinal + 1] - valueOffsetLane[ordinal];
    }

    public long keyAt(int keyOrdinal) {
        return keyLane[Objects.checkIndex(keyOrdinal, keyLane.length)];
    }

    /** Includes the terminal boundary at {@code keyCount()}, equal to {@code size()}. */
    public int valueOffsetAt(int keyOrdinal) {
        return valueOffsetLane[Objects.checkIndex(keyOrdinal, valueOffsetLane.length)];
    }

    public long valueAt(int valueOrdinal) {
        return valueLane[Objects.checkIndex(valueOrdinal, valueLane.length)];
    }

    public long[] get(long key) {
        int ordinal = keyOrdinals.getOrDefault(key, -1);
        return ordinal < 0 ? new long[0] : Arrays.copyOfRange(valueLane,
                valueOffsetLane[ordinal], valueOffsetLane[ordinal + 1]);
    }

    /** Traverses groups in key encounter order, then each key's value encounter order. */
    public void forEach(LongLongConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int key = 0; key < keyLane.length; key++) {
            for (int value = valueOffsetLane[key]; value < valueOffsetLane[key + 1]; value++) {
                action.accept(keyLane[key], valueLane[value]);
            }
        }
    }

    public boolean put(long key, long value) {
        throw new UnsupportedOperationException("immutable multimap");
    }

    public long[] removeAll(long key) {
        throw new UnsupportedOperationException("immutable multimap");
    }
}
