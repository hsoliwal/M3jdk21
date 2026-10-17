/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ConcurrentModificationException;
import java.util.Objects;

/**
 * Primitive one-to-one long map with a stable, live inverse view.
 *
 * <p>Reuses two existing {@link M3LongLongHashMap} owners. Both use parallel
 * {@code long[]} key/value lanes and {@code byte[]} occupancy flags, with no
 * retained {@code Map.Entry} or per-mapping nodes. This is a primitive
 * counterpart, NOT an implementation of Guava BiMap, Eclipse BiMap, Apache
 * Commons BidiMap, or a replacement for {@code java.util.Map}.</p>
 *
 * <p>Expected O(1) lookup/update, worst-case O(n) due to hash collisions or
 * rehashing. Iteration order is unspecified; mutation is not thread-safe.</p>
 */
public final class M3LongBiMap {
    private final M3LongLongHashMap forward;
    private final M3LongLongHashMap reverse;
    private final boolean inverted;
    private final M3LongBiMap inverse;
    private final Mutation mutation;

    private static final class Mutation {
        private int revision;
    }

    public M3LongBiMap() {
        this(0);
    }

    public M3LongBiMap(int expectedSize) {
        forward = new M3LongLongHashMap(expectedSize);
        reverse = new M3LongLongHashMap(expectedSize);
        mutation = new Mutation();
        inverted = false;
        inverse = new M3LongBiMap(forward, reverse, mutation, this);
    }

    private M3LongBiMap(M3LongLongHashMap forward, M3LongLongHashMap reverse,
            Mutation mutation, M3LongBiMap inverse) {
        this.forward = forward;
        this.reverse = reverse;
        this.mutation = mutation;
        this.inverse = inverse;
        this.inverted = true;
    }

    private M3LongLongHashMap keys() {
        return inverted ? reverse : forward;
    }

    private M3LongLongHashMap values() {
        return inverted ? forward : reverse;
    }

    /** Inverse mutations affect this view immediately; inverse().inverse() == this. */
    public M3LongBiMap inverse() {
        return inverse;
    }

    public int size() {
        return forward.size();
    }

    public boolean isEmpty() {
        return forward.isEmpty();
    }

    public boolean containsKey(long key) {
        return keys().containsKey(key);
    }

    public boolean containsValue(long value) {
        return values().containsKey(value);
    }

    public long getOrDefault(long key, long fallback) {
        return keys().getOrDefault(key, fallback);
    }

    /**
     * Inserts one pair, or replaces its existing key's value.
     *
     * @return true iff the key did not previously exist
     * @throws IllegalArgumentException if another key already owns the value;
     *     collision rejection leaves both indexes unchanged
     */
    public boolean put(long key, long value) {
        M3LongLongHashMap byKey = keys();
        M3LongLongHashMap byValue = values();
        boolean present = byKey.containsKey(key);
        long old = present ? byKey.getOrDefault(key, 0L) : 0L;
        if (present && old == value) {
            return false;
        }
        if (byValue.containsKey(value)) {
            throw new IllegalArgumentException("value already belongs to another key");
        }
        if (present) {
            byValue.remove(old);
        }
        byKey.put(key, value);
        byValue.put(value, key);
        mutation.revision++;
        return !present;
    }

    /**
     * Like put, but first evicts the other key currently owning this value.
     *
     * @return true iff the supplied key was previously absent
     */
    public boolean forcePut(long key, long value) {
        boolean absent = !containsKey(key);
        M3LongLongHashMap byValue = values();
        if (byValue.containsKey(value)) {
            long oldKey = byValue.getOrDefault(value, 0L);
            if (oldKey != key) {
                remove(oldKey);
            }
        }
        put(key, value);
        return absent;
    }

    public boolean remove(long key) {
        M3LongLongHashMap byKey = keys();
        if (!byKey.containsKey(key)) {
            return false;
        }
        long value = byKey.getOrDefault(key, 0L);
        byKey.remove(key);
        values().remove(value);
        mutation.revision++;
        return true;
    }

    public void clear() {
        if (!isEmpty()) {
            forward.clear();
            reverse.clear();
            mutation.revision++;
        }
    }

    /** Copies primitive keys; does not expose the mutable backing arrays. */
    public long[] keysToArray() {
        return keys().keysToArray();
    }

    /** Visits primitive pairs without creating retained entry objects. */
    public void forEach(M3LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        int expectedRevision = mutation.revision;
        keys().forEach((key, value) -> {
            consumer.accept(key, value);
            if (mutation.revision != expectedRevision) {
                throw new ConcurrentModificationException();
            }
        });
    }

    /** Capacity of the existing two open-addressed flag/index maps. */
    public long allocatedPrimitiveBytes() {
        return (long) (forward.capacity() + reverse.capacity())
                * (2L * Long.BYTES + Byte.BYTES);
    }

    /** Verifies bijection in both directions; intended for tests/debugging. */
    public void verifyInvariants() {
        if (forward.size() != reverse.size()) {
            throw new IllegalStateException("bidirectional cardinality drift");
        }
        forward.forEach((key, value) -> {
            if (!reverse.containsKey(value)
                    || reverse.getOrDefault(value, 0L) != key) {
                throw new IllegalStateException("forward/reverse mismatch");
            }
        });
        reverse.forEach((value, key) -> {
            if (!forward.containsKey(key)
                    || forward.getOrDefault(key, 0L) != value) {
                throw new IllegalStateException("reverse/forward mismatch");
            }
        });
    }
}
