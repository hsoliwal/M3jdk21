/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Enum-keyed primitive-long map indexed directly by ordinal, with no per-entry object.
 *
 * <p>This is an M3 proving-ground backend for the {@code EnumMap<E, Long>} shape, not a
 * replacement for {@link java.util.EnumMap}. Values live in one {@code long[]} indexed by
 * ordinal and presence is one bit per constant, so a mapping retains no key reference and no
 * boxed value. Iteration order is ordinal order, as in {@link java.util.EnumMap}. Keys of
 * another enum type are rejected with {@link ClassCastException}, {@code null} keys are never
 * present and are rejected by {@link #put} with {@link NullPointerException}, mirroring the JDK
 * contract. There is no fail-fast iteration because {@link java.util.EnumMap} has none.</p>
 *
 * <p>Adapted from the first-party Synexia donor {@code com.synexia.primitives.EnumPrimitiveMap}
 * (exact-width value kinds) to the single {@code long} value lane the proving ground needs; the
 * adaptive sparse/direct variant is deferred until a hashed int-to-long owner exists here.</p>
 */
public final class M3EnumLongMap<E extends Enum<E>> {
    @FunctionalInterface
    public interface OrdinalLongConsumer {
        void accept(int ordinal, long value);
    }

    @FunctionalInterface
    public interface EntryConsumer<E extends Enum<E>> {
        void accept(E key, long value);
    }

    private final Class<E> keyType;
    private final E[] universe;
    private final long[] values;
    private final long[] present;
    private int size;

    public M3EnumLongMap(Class<E> keyType) {
        this.keyType = Objects.requireNonNull(keyType, "keyType");
        E[] constants = keyType.getEnumConstants();
        if (constants == null) {
            throw new IllegalArgumentException("keyType is not an enum: " + keyType.getName());
        }
        universe = constants;
        values = new long[constants.length];
        present = new long[Math.max(1, (constants.length + 63) >>> 6)];
    }

    public Class<E> keyType() {
        return keyType;
    }

    /** Number of constants in the key domain. */
    public int keyCapacity() {
        return universe.length;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** Retained primitive payload: value lane plus presence words. */
    public long payloadBytes() {
        return (long) values.length * Long.BYTES + (long) present.length * Long.BYTES;
    }

    public boolean containsKey(E key) {
        return key != null && isValidKey(key) && containsOrdinal(key.ordinal());
    }

    public boolean containsValue(long value) {
        for (int wordIndex = 0; wordIndex < present.length; wordIndex++) {
            long word = present[wordIndex];
            while (word != 0L) {
                int ordinal = (wordIndex << 6) + Long.numberOfTrailingZeros(word);
                if (values[ordinal] == value) {
                    return true;
                }
                word &= word - 1L;
            }
        }
        return false;
    }

    public long getOrDefault(E key, long defaultValue) {
        if (key == null || !isValidKey(key)) {
            return defaultValue;
        }
        int ordinal = key.ordinal();
        return containsOrdinal(ordinal) ? values[ordinal] : defaultValue;
    }

    /** Mapped value; throws {@link NoSuchElementException} when the key is absent. */
    public long getAsLong(E key) {
        int ordinal = ordinal(key);
        if (!containsOrdinal(ordinal)) {
            throw new NoSuchElementException(key.name());
        }
        return values[ordinal];
    }

    /** Associates {@code value}; returns the previous value, or {@code absentValue} if none. */
    public long put(E key, long value, long absentValue) {
        int ordinal = ordinal(key);
        long bit = 1L << (ordinal & 63);
        int word = ordinal >>> 6;
        boolean contained = (present[word] & bit) != 0L;
        long previous = contained ? values[ordinal] : absentValue;
        values[ordinal] = value;
        if (!contained) {
            present[word] |= bit;
            size++;
        }
        return previous;
    }

    /** Associates {@code value}; returns {@code true} when the key was absent. */
    public boolean put(E key, long value) {
        int ordinal = ordinal(key);
        long bit = 1L << (ordinal & 63);
        int word = ordinal >>> 6;
        boolean added = (present[word] & bit) == 0L;
        values[ordinal] = value;
        if (added) {
            present[word] |= bit;
            size++;
        }
        return added;
    }

    public boolean putIfAbsent(E key, long value) {
        int ordinal = ordinal(key);
        if (containsOrdinal(ordinal)) {
            return false;
        }
        return put(key, value);
    }

    /** Removes the mapping; returns the previous value, or {@code absentValue} if none. */
    public long remove(E key, long absentValue) {
        if (key == null || !isValidKey(key)) {
            return absentValue;
        }
        int ordinal = key.ordinal();
        long bit = 1L << (ordinal & 63);
        int word = ordinal >>> 6;
        if ((present[word] & bit) == 0L) {
            return absentValue;
        }
        long previous = values[ordinal];
        values[ordinal] = 0L;
        present[word] &= ~bit;
        size--;
        return previous;
    }

    /** Removes the mapping; returns {@code true} when a mapping was present. */
    public boolean removeKey(E key) {
        if (key == null || !isValidKey(key)) {
            return false;
        }
        int ordinal = key.ordinal();
        long bit = 1L << (ordinal & 63);
        int word = ordinal >>> 6;
        if ((present[word] & bit) == 0L) {
            return false;
        }
        values[ordinal] = 0L;
        present[word] &= ~bit;
        size--;
        return true;
    }

    public void clear() {
        if (size == 0) {
            return;
        }
        Arrays.fill(values, 0L);
        Arrays.fill(present, 0L);
        size = 0;
    }

    /** Visits present mappings in ordinal order without materializing keys. */
    public void forEachOrdinal(OrdinalLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int wordIndex = 0; wordIndex < present.length; wordIndex++) {
            long word = present[wordIndex];
            while (word != 0L) {
                int ordinal = (wordIndex << 6) + Long.numberOfTrailingZeros(word);
                consumer.accept(ordinal, values[ordinal]);
                word &= word - 1L;
            }
        }
    }

    /** Visits present mappings in ordinal order; keys come from the shared enum universe. */
    public void forEach(EntryConsumer<E> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        forEachOrdinal((ordinal, value) -> consumer.accept(universe[ordinal], value));
    }

    /** Present values in ordinal order. */
    public long[] values() {
        long[] result = new long[size];
        int[] cursor = {0};
        forEachOrdinal((ordinal, value) -> result[cursor[0]++] = value);
        return result;
    }

    /** Present keys in ordinal order. */
    @SuppressWarnings("unchecked")
    public E[] keys() {
        E[] result = (E[]) java.lang.reflect.Array.newInstance(keyType, size);
        int[] cursor = {0};
        forEachOrdinal((ordinal, value) -> result[cursor[0]++] = universe[ordinal]);
        return result;
    }

    private boolean containsOrdinal(int ordinal) {
        return (present[ordinal >>> 6] & (1L << (ordinal & 63))) != 0L;
    }

    private boolean isValidKey(E key) {
        Class<?> declaring = key.getDeclaringClass();
        return declaring == keyType;
    }

    private int ordinal(E key) {
        Objects.requireNonNull(key, "key");
        if (!isValidKey(key)) {
            throw new ClassCastException(key.getDeclaringClass().getName() + " != " + keyType.getName());
        }
        return key.ordinal();
    }
}
