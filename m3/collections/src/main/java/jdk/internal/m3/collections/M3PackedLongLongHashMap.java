// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongUnaryOperator;
import jdk.m3.collections.M3LongLongConsumer;

/**
 * Open-addressed primitive {@code long -> long} map.
 *
 * <p>Retained state is three flat lanes: {@code long[] keys}, {@code long[] values},
 * and {@code byte[] states}. No {@code Map.Entry}, bucket node or wrapper is retained
 * per mapping.
 * Backing lanes are allocated on first insertion. {@link #capacity()} reports the
 * reserved logical capacity even while empty storage is shared. Allocation failure
 * can therefore occur at first insertion rather than construction.
 * Rehashing constructs all replacement lanes before publication. Allocation failure
 * during explicit growth/trim preserves the old map; optional tombstone compaction
 * after a successful removal may be skipped on allocation failure. This does not
 * make callback-based filter operations transactional or this map thread-safe.
 */
//FLLDCIM: RELOCATE - donor blob 26c302f1cb89 at commit d5ad5090d87e (repository, path, URL and digests: m3/collections/recipe/four-library-bags-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/four-library-bags-20261106/relocate.py v1
public final class M3PackedLongLongHashMap implements M3PackedLongLongMap {
    private static final byte EMPTY = M3PackedHashAtoms.EMPTY;
    private static final byte OCCUPIED = M3PackedHashAtoms.OCCUPIED;
    private static final byte DELETED = M3PackedHashAtoms.DELETED;
    private static final long[] EMPTY_LONGS = new long[0];
    private static final byte[] EMPTY_STATES = new byte[0];

    private long[] keys;
    private long[] values;
    private byte[] states;
    private int size;
    private int used;
    private int reservedCapacity;

    public M3PackedLongLongHashMap() {
        this(8);
    }

    public M3PackedLongLongHashMap(int expectedSize) {
        int capacity = M3PackedSupport.tableSizeForExpected(expectedSize);
        keys = EMPTY_LONGS;
        values = EMPTY_LONGS;
        states = EMPTY_STATES;
        reservedCapacity = capacity;
    }

    /**
     * Associates key and value.
     *
     * @return true when a new key was inserted, false when an existing value was replaced
     */
    public boolean put(long key, long value) {
        allocateIfNeeded();
        if (M3PackedHashAtoms.pressure(used, reservedCapacity)) {
            int existing = M3PackedHashAtoms.findIndex(keys, states, key);
            if (existing >= 0) {
                values[existing] = value;
                return false;
            }
            rehash(M3PackedHashAtoms.capacityAfterPressure(size, keys.length));
        }
        int slot = M3PackedHashAtoms.findOrInsertionIndex(keys, states, key);
        if (slot >= 0) {
            values[slot] = value;
            return false;
        }
        int target = ~slot;
        boolean reusedDeleted = states[target] == DELETED;
        keys[target] = key;
        values[target] = value;
        states[target] = OCCUPIED;
        size++;
        if (!reusedDeleted) {
            used++;
        }
        return true;
    }

    public boolean containsKey(long key) {
        return findIndex(key) >= 0;
    }

    public long getOrDefault(long key, long defaultValue) {
        int index = findIndex(key);
        return index < 0 ? defaultValue : values[index];
    }

    public long getOrThrow(long key) {
        int index = findIndex(key);
        if (index < 0) {
            throw new IllegalArgumentException("missing key: " + key);
        }
        return values[index];
    }

    public boolean replace(long key, long expectedValue, long newValue) {
        int index = findIndex(key);
        if (index < 0 || values[index] != expectedValue) {
            return false;
        }
        values[index] = newValue;
        return true;
    }

    public long computeIfAbsent(long key, LongUnaryOperator mapping) {
        Objects.requireNonNull(mapping, "mapping");
        int index = findIndex(key);
        if (index >= 0) {
            return values[index];
        }
        long value = mapping.applyAsLong(key);
        put(key, value);
        return value;
    }

    public long addTo(long key, long delta, long initialValue) {
        int index = findIndex(key);
        if (index >= 0) {
            values[index] += delta;
            return values[index];
        }
        long value = initialValue + delta;
        put(key, value);
        return value;
    }

    public boolean remove(long key) {
        int index = findIndex(key);
        if (index < 0) {
            return false;
        }
        states[index] = DELETED;
        size--;
        compactAfterRemoval();
        return true;
    }

    public boolean remove(long key, long expectedValue) {
        int index = findIndex(key);
        if (index < 0 || values[index] != expectedValue) {
            return false;
        }
        states[index] = DELETED;
        size--;
        compactAfterRemoval();
        return true;
    }

    public int filterInPlace(M3LongLongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int before = size;
        for (int i = 0; i < states.length; i++) {
            if (states[i] == OCCUPIED && !predicate.test(keys[i], values[i])) {
                states[i] = DELETED;
                size--;
            }
        }
        if (size == 0) {
            Arrays.fill(states, EMPTY);
            used = 0;
        } else if (size != before) {
            rehash(M3PackedSupport.tableSizeForExpected(size));
        }
        return before - size;
    }

    public void forEach(M3LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < states.length; i++) {
            if (states[i] == OCCUPIED) {
                consumer.accept(keys[i], values[i]);
            }
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int capacity() {
        return reservedCapacity;
    }

    public void clear() {
        Arrays.fill(states, EMPTY);
        size = 0;
        used = 0;
    }

    public void trimToSize() {
        int target = M3PackedSupport.tableSizeForExpected(size);
        if (size == 0) {
            keys = EMPTY_LONGS;
            values = EMPTY_LONGS;
            states = EMPTY_STATES;
            reservedCapacity = target;
            used = 0;
        } else if (target != reservedCapacity || used != size) {
            rehash(target);
        }
    }

    public long[] keysToArray() {
        long[] result = new long[size];
        int write = 0;
        for (int i = 0; i < states.length; i++) {
            if (states[i] == OCCUPIED) {
                result[write++] = keys[i];
            }
        }
        return result;
    }

    /**
     * Values are emitted in exactly the same slot-scan order as {@link #keysToArray()}.
     */
    public long[] valuesToArray() {
        long[] result = new long[size];
        int write = 0;
        for (int i = 0; i < states.length; i++) {
            if (states[i] == OCCUPIED) {
                result[write++] = values[i];
            }
        }
        return result;
    }

    private int findIndex(long key) {
        return size == 0 ? -1 : M3PackedHashAtoms.findIndex(keys, states, key);
    }

    private void compactAfterRemoval() {
        if (size == 0) {
            Arrays.fill(states, EMPTY);
            used = 0;
        } else if (M3PackedHashAtoms.compactTombstones(size, used)) {
            try {
                rehash(keys.length);
            } catch (OutOfMemoryError unavailable) {
                // Removal has already committed. Optional tombstone compaction
                // must not turn it into an apparent failure to a counted owner.
                // Existing lanes remain valid because rehash publishes last.
            }
        }
    }

    private void allocateIfNeeded() {
        if (keys.length != 0) {
            return;
        }
        // Publish all lanes together after every allocation succeeds.
        long[] newKeys = new long[reservedCapacity];
        long[] newValues = new long[reservedCapacity];
        byte[] newStates = new byte[reservedCapacity];
        keys = newKeys;
        values = newValues;
        states = newStates;
    }

    private void rehash(int capacity) {
        // Construct and populate all replacement lanes before changing any field.
        // In particular, failure of allocation two or three preserves lane identity.
        long[] newKeys = new long[capacity];
        long[] newValues = new long[capacity];
        byte[] newStates = new byte[capacity];
        for (int i = 0; i < states.length; i++) {
            if (states[i] == OCCUPIED) {
                int target = M3PackedHashAtoms.rehashInsertionIndex(newKeys, newStates, keys[i]);
                newKeys[target] = keys[i];
                newValues[target] = values[i];
                newStates[target] = OCCUPIED;
            }
        }
        keys = newKeys;
        values = newValues;
        states = newStates;
        reservedCapacity = capacity;
        used = size;
    }
}
