/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Enum-keyed primitive-long map that hashes ordinals while sparse and switches to the dense
 * ordinal-indexed {@link M3EnumLongMap} once the fixed domain is the cheaper representation.
 *
 * <p>This is the adaptive variant deferred from {@link M3EnumLongMap}, adapted from the
 * first-party Synexia donor {@code com.synexia.primitives.AdaptiveEnumPrimitiveMap} onto the
 * single {@code long} value lane of the proving ground. The backing is chosen by retained
 * primitive bytes: {@code n} mappings stay sparse in an {@link M3IntLongHashMap} while
 * {@link M3IntLongHashMap#payloadBytesFor(int) payloadBytesFor(n)} is below
 * {@link #directPayloadBytes()}, and the insertion that reaches {@link #directSwitchSize()}
 * migrates every mapping to the dense lane. Removal never migrates implicitly;
 * {@link #compact()} re-applies the rule to the current size and shrinks a sparse table.
 * Neither mode retains a key reference or a boxed value per mapping.</p>
 *
 * <p>The external contract is that of {@link M3EnumLongMap}: ordinal iteration order in both
 * modes (sparse traversal walks the ordinal domain and probes each constant, so it allocates
 * nothing and costs {@code O(keyCapacity)}), {@link ClassCastException} for a key of another
 * enum type and {@link NullPointerException} for a {@code null} key on {@code put}, and absence
 * for both on reads and removals. Not a {@link java.util.EnumMap} replacement;
 * {@code java.util.EnumMap} remains the public contract owner.</p>
 */
public final class M3AdaptiveEnumLongMap<E extends Enum<E>> {
    private final Class<E> keyType;
    private final int domainSize;
    private M3IntLongHashMap sparse;
    private M3EnumLongMap<E> direct;

    public M3AdaptiveEnumLongMap(Class<E> keyType) {
        this(keyType, 0);
    }

    /** @param expectedSize mapping count the backing is sized for, {@code 0..keyCapacity()} */
    public M3AdaptiveEnumLongMap(Class<E> keyType, int expectedSize) {
        this.keyType = Objects.requireNonNull(keyType, "keyType");
        E[] constants = keyType.getEnumConstants();
        if (constants == null) {
            throw new IllegalArgumentException("keyType is not an enum: " + keyType.getName());
        }
        domainSize = constants.length;
        if (expectedSize < 0 || expectedSize > domainSize) {
            throw new IllegalArgumentException(
                    "expectedSize must be 0.." + domainSize + ": " + expectedSize);
        }
        if (shouldUseDirect(expectedSize)) {
            direct = new M3EnumLongMap<>(keyType);
        } else {
            sparse = new M3IntLongHashMap(expectedSize);
        }
    }

    public Class<E> keyType() {
        return keyType;
    }

    /** Number of constants in the key domain. */
    public int keyCapacity() {
        return domainSize;
    }

    public int size() {
        return direct != null ? direct.size() : sparse.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    /** Whether the dense ordinal-indexed lane is active. */
    public boolean isDirect() {
        return direct != null;
    }

    /** Retained primitive payload of the active backing. */
    public long payloadBytes() {
        return direct != null ? direct.payloadBytes() : sparse.payloadBytes();
    }

    /** Primitive payload the dense lane retains for this key domain, whatever the size. */
    public long directPayloadBytes() {
        return directPayloadBytes(domainSize);
    }

    /** Smallest mapping count at which the dense lane is chosen; {@code 0} when it always is. */
    public int directSwitchSize() {
        int low = 0;
        int high = domainSize;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (shouldUseDirect(mid)) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }

    public boolean containsKey(E key) {
        if (key == null || !isValidKey(key)) {
            return false;
        }
        return direct != null ? direct.containsKey(key) : sparse.containsKey(key.ordinal());
    }

    public boolean containsValue(long value) {
        return direct != null ? direct.containsValue(value) : sparse.containsValue(value);
    }

    public long getOrDefault(E key, long defaultValue) {
        if (key == null || !isValidKey(key)) {
            return defaultValue;
        }
        return direct != null
                ? direct.getOrDefault(key, defaultValue)
                : sparse.getOrDefault(key.ordinal(), defaultValue);
    }

    /** Mapped value; throws {@link NoSuchElementException} when the key is absent. */
    public long getAsLong(E key) {
        int ordinal = ordinal(key);
        if (direct != null) {
            return direct.getAsLong(key);
        }
        if (!sparse.containsKey(ordinal)) {
            throw new NoSuchElementException(key.name());
        }
        return sparse.getOrDefault(ordinal, 0L);
    }

    /** Associates {@code value}; returns the previous value, or {@code absentValue} if none. */
    public long put(E key, long value, long absentValue) {
        int ordinal = ordinal(key);
        if (direct != null) {
            return direct.put(key, value, absentValue);
        }
        if (sparse.containsKey(ordinal)) {
            long previous = sparse.getOrDefault(ordinal, 0L);
            sparse.put(ordinal, value);
            return previous;
        }
        if (shouldUseDirect(sparse.size() + 1)) {
            migrateToDirect();
            return direct.put(key, value, absentValue);
        }
        sparse.put(ordinal, value);
        return absentValue;
    }

    /** Associates {@code value}; returns {@code true} when the key was absent. */
    public boolean put(E key, long value) {
        int ordinal = ordinal(key);
        if (direct != null) {
            return direct.put(key, value);
        }
        if (sparse.containsKey(ordinal)) {
            sparse.put(ordinal, value);
            return false;
        }
        if (shouldUseDirect(sparse.size() + 1)) {
            migrateToDirect();
            return direct.put(key, value);
        }
        return sparse.put(ordinal, value);
    }

    public boolean putIfAbsent(E key, long value) {
        int ordinal = ordinal(key);
        if (direct != null) {
            return direct.putIfAbsent(key, value);
        }
        if (sparse.containsKey(ordinal)) {
            return false;
        }
        return put(key, value);
    }

    /** Removes the mapping; returns the previous value, or {@code absentValue} if none. */
    public long remove(E key, long absentValue) {
        if (key == null || !isValidKey(key)) {
            return absentValue;
        }
        if (direct != null) {
            return direct.remove(key, absentValue);
        }
        int ordinal = key.ordinal();
        if (!sparse.containsKey(ordinal)) {
            return absentValue;
        }
        long previous = sparse.getOrDefault(ordinal, 0L);
        sparse.remove(ordinal);
        return previous;
    }

    /** Removes the mapping; returns {@code true} when a mapping was present. */
    public boolean removeKey(E key) {
        if (key == null || !isValidKey(key)) {
            return false;
        }
        return direct != null ? direct.removeKey(key) : sparse.remove(key.ordinal());
    }

    /** Removes every mapping; the active backing is kept. */
    public void clear() {
        if (direct != null) {
            direct.clear();
        } else {
            sparse.clear();
        }
    }

    /**
     * Re-chooses the backing for the current size: sparse when its table would retain fewer
     * bytes than the dense lane, dense otherwise; a sparse table that could be smaller is rebuilt.
     */
    public void compact() {
        int currentSize = size();
        if (direct != null) {
            if (!shouldUseDirect(currentSize)) {
                migrateToSparse();
            }
            return;
        }
        if (shouldUseDirect(currentSize)) {
            migrateToDirect();
            return;
        }
        if (M3IntLongHashMap.payloadBytesFor(currentSize) < sparse.payloadBytes()) {
            sparse = copySparse(currentSize);
        }
    }

    /** Visits present mappings in ordinal order without materializing keys. */
    public void forEachOrdinal(M3EnumLongMap.OrdinalLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (direct != null) {
            direct.forEachOrdinal(consumer);
            return;
        }
        int remaining = sparse.size();
        for (int ordinal = 0; remaining > 0 && ordinal < domainSize; ordinal++) {
            if (sparse.containsKey(ordinal)) {
                consumer.accept(ordinal, sparse.getOrDefault(ordinal, 0L));
                remaining--;
            }
        }
    }

    /** Visits present mappings in ordinal order; sparse mode resolves keys through one universe copy per call. */
    public void forEach(M3EnumLongMap.EntryConsumer<E> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (direct != null) {
            direct.forEach(consumer);
            return;
        }
        E[] universe = keyType.getEnumConstants();
        forEachOrdinal((ordinal, value) -> consumer.accept(universe[ordinal], value));
    }

    /** Present values in ordinal order. */
    public long[] values() {
        long[] result = new long[size()];
        int[] cursor = {0};
        forEachOrdinal((ordinal, value) -> result[cursor[0]++] = value);
        return result;
    }

    /** Present keys in ordinal order. */
    @SuppressWarnings("unchecked")
    public E[] keys() {
        if (direct != null) {
            return direct.keys();
        }
        E[] universe = keyType.getEnumConstants();
        E[] result = (E[]) java.lang.reflect.Array.newInstance(keyType, sparse.size());
        int[] cursor = {0};
        forEachOrdinal((ordinal, value) -> result[cursor[0]++] = universe[ordinal]);
        return result;
    }

    private boolean shouldUseDirect(int expectedSize) {
        return directPayloadBytes(domainSize)
                <= M3IntLongHashMap.payloadBytesFor(Math.max(1, expectedSize));
    }

    private static long directPayloadBytes(int domainSize) {
        int words = Math.max(1, (domainSize + 63) >>> 6);
        return (long) domainSize * Long.BYTES + (long) words * Long.BYTES;
    }

    private void migrateToDirect() {
        M3EnumLongMap<E> next = new M3EnumLongMap<>(keyType);
        E[] universe = keyType.getEnumConstants();
        sparse.forEach((ordinal, value) -> next.put(universe[ordinal], value));
        direct = next;
        sparse = null;
    }

    private void migrateToSparse() {
        M3IntLongHashMap next = new M3IntLongHashMap(direct.size());
        direct.forEachOrdinal(next::put);
        sparse = next;
        direct = null;
    }

    private M3IntLongHashMap copySparse(int expectedSize) {
        M3IntLongHashMap next = new M3IntLongHashMap(expectedSize);
        sparse.forEach(next::put);
        return next;
    }

    private boolean isValidKey(E key) {
        return key.getDeclaringClass() == keyType;
    }

    private int ordinal(E key) {
        Objects.requireNonNull(key, "key");
        if (!isValidKey(key)) {
            throw new ClassCastException(key.getDeclaringClass().getName() + " != " + keyType.getName());
        }
        return key.ordinal();
    }
}
