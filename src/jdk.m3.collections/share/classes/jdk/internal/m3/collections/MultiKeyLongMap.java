/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

/**
 * Two-key primitive map composed over the canonical packed table owner.
 *
 * <p>This receiver adds the Commons MultiKeyMap-shaped primitive API without
 * creating a competing hash directory. Its authoritative representation is
 * {@link PackedLongLongTable}: parallel long row, column and value lanes plus
 * one primitive open-addressed pair index. The table owns slot growth,
 * deletion repair and group filtering. There are no boxed key pairs, chained
 * entries or per-key map objects.</p>
 *
 * <p>Single-pair operations are expected O(1); removing all values with a
 * matching first key is O(n), using the existing staged bit-mask filter.
 * Zero is the return for an absent mapping in primitive put/remove, so
 * callers use containsKey to distinguish missing from a stored zero.
 * This M3-specific two-long subset is not a complete Commons MultiKeyMap
 * replacement and is not synchronized.</p>
 */
public final class MultiKeyLongMap {
    /** Historic public inventory constants preserved unchanged. */
    static final long STATE_OCCUPIED = 1L << 0;
    static final long STATE_DELETED = 1L << 1;

    /** Parent-owned primitive lanes live exclusively inside this shared owner. */
    private final PackedLongLongTable entries = new PackedLongLongTable();

    public MultiKeyLongMap() { }

    /** Historical inventory token; the actual lanes are now composed. */
    public static String scaffoldDescriptor() {
        return "M3MultiKeyLongMap|MULTIKEY_LONG_MAP|LONG|OCCUPIED,DELETED|long[] key1Lane;long[] key2Lane;long[] valueLane";
    }

    public long getOrDefault(long key1, long key2, long defaultValue) {
        return entries.getOrDefault(key1, key2, defaultValue);
    }

    public boolean containsKey(long key1, long key2) {
        return entries.contains(key1, key2);
    }

    /** Replace or insert a pair, returning its old value or zero if absent. */
    public long put(long key1, long key2, long value) {
        long previous = entries.getOrDefault(key1, key2, 0L);
        entries.put(key1, key2, value);
        return previous;
    }

    /** Delete a pair and return its old value or zero if absent. */
    public long remove(long key1, long key2) {
        long previous = entries.getOrDefault(key1, key2, 0L);
        entries.remove(key1, key2);
        return previous;
    }

    /**
     * Delete the whole first-key group. The canonical table probes and
     * captures the complete rejection bitset before applying any removal.
     */
    public boolean removeAll(long key1) {
        return entries.filterInPlace((row, column, value) -> row != key1) != 0;
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void clear() {
        entries.clear();
    }
}
