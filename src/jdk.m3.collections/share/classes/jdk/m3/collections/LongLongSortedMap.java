/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.Objects;
import java.util.stream.IntStream;
import jdk.internal.m3.collections.packed.PackedLongLongSortedMap;

/**
 * Mutable navigable primitive long-to-long map contract: the first-party Synexia donor
 * {@code LongLongSortedMap} contract under the M3 family naming (sibling of {@link LongSortedSet}).
 *
 * <p>Keys are ordered by natural signed-long order and every long is a legal key and a legal value,
 * so the navigation calls take a caller-supplied default instead of returning a boxed null;
 * {@code firstKey} and {@code lastKey} throw {@link java.util.NoSuchElementException} on an empty
 * map. This is a target-internal primitive surface; {@link java.util.NavigableMap} remains the
 * public JDK contract owner.</p>
 */
public interface LongLongSortedMap {
    /** Returns an empty sorted-array map (API_UNION U7). */
    static LongLongSortedMap of() { return new PackedLongLongSortedMap(); }

    /** Returns an empty sorted-array map sized for {@code expectedSize} keys (API_UNION U7). */
    static LongLongSortedMap of(int expectedSize) { return new PackedLongLongSortedMap(expectedSize); }

    boolean put(long key, long value);

    boolean containsKey(long key);

    long getOrDefault(long key, long defaultValue);

    long getOrThrow(long key);

    boolean remove(long key);

    long firstKey();

    long lastKey();

    long floorKey(long key, long defaultValue);

    long ceilingKey(long key, long defaultValue);

    long lowerKey(long key, long defaultValue);

    long higherKey(long key, long defaultValue);

    /**
     * Removes every entry the predicate rejects, keeping the entries it accepts.
     *
     * <p>The default works over this contract (a key/value snapshot, then {@link #remove(long)}); implementations with
     * packed storage override it with an in-place compaction.</p>
     *
     * @param predicate retention test over a key and its value
     * @return the number of entries removed
     * @throws NullPointerException if {@code predicate} is null
     */
    default int filterInPlace(LongLongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        long[] keys = keysToArray();
        long[] values = valuesToArray();
        int[] drop = IntStream.range(0, keys.length).filter(i -> !predicate.test(keys[i], values[i])).toArray();
        IntStream.of(drop).forEach(i -> remove(keys[i]));
        return drop.length;
    }

    void forEach(LongLongConsumer consumer);

    int size();

    boolean isEmpty();

    void clear();

    long[] keysToArray();

    long[] valuesToArray();
}
