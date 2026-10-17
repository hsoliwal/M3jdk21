// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Derived from Synexia PackedHashMap.Table / M3JDK21 M3LinkedHashMap.Table and M3SlotArrays.
// Modified 2026: inline the private slot atoms, store values/weights directly, reuse free links.
// Exact donor commits, hashes and changes are recorded in provenance.json and the recipe crate.
package com.m3.collections.parallel;

import java.util.Arrays;
import java.util.Objects;

/**
 * Explicit bounded access-order retention for immutable/version-guarded plans or other values.
 * Keys must include every semantic discriminator (source identity/version, equality, algorithm,
 * backend, etc.). This cache does not infer those, refresh stale plans or provide a source lock.
 * Weights are caller-supplied nonnegative retention estimates, not measured total process bytes.
 * Eviction releases cache ownership only; it never closes a value still held by another caller.
 * No loader or removal listener runs inside this synchronized owner. Key hashing/equality must
 * remain stable, non-reentrant and well-behaved. Null keys work; values are nonnull.
 *
 * <p>The existing M3 slot-table atoms are specialized here, not exposed as another map API.
 * Keys and actual values occupy two reference lanes; weights, hash/index and LRU/free links
 * are primitive lanes. No boxed slot IDs, Entry/Node objects or map-view adapter are retained.
 * Expected constant-time lookup/eviction; collisions can still produce linear-time lookup.
 */
public final class M3PlanCache<K, V> {
    public record Statistics(int entries, long weight, int maximumEntries, long maximumWeight) { }
    private static final Object[] EMPTY_OBJECTS = new Object[0];
    private static final int[] EMPTY_INTS = new int[0];
    private static final long[] EMPTY_LONGS = new long[0];
    private final int maximumEntries;
    private final long maximumWeight;
    private Object[] keys = EMPTY_OBJECTS, values = EMPTY_OBJECTS;
    private long[] weights = EMPTY_LONGS;
    private int[] hashes = EMPTY_INTS, before = EMPTY_INTS, after = EMPTY_INTS,
            positions = EMPTY_INTS, index = EMPTY_INTS;
    private int first = -1, last = -1, free = -1, used, occupied, size;
    private long weight;

    public M3PlanCache(int maximumEntries, long maximumWeight) {
        if (maximumEntries < 0 || maximumWeight < 0) throw new IllegalArgumentException("negative cache capacity");
        this.maximumEntries = maximumEntries; this.maximumWeight = maximumWeight;
    }
    /** A successful lookup promotes that entry to most recently used. */
    public synchronized V get(K key) {
        int slot = find(key, hash(key));
        if (slot < 0) return null;
        moveLast(slot); return value(slot);
    }
    /**
     * Admit a value, evicting least-recently-used entries until both budgets fit.
     * Invalid/oversized values are refused before mutation, including replacement of an old value.
     * A zero-entry cache refuses insertion, and zero-weight entries are still count-bounded.
     */
    public synchronized V put(K key, V value, long retentionWeight) {
        Objects.requireNonNull(value);
        if (retentionWeight < 0 || retentionWeight > maximumWeight || maximumEntries == 0)
            throw new IllegalArgumentException("value exceeds cache admission budget");
        int h = hash(key), slot = find(key, h);
        V old = slot < 0 ? null : release(slot);
        // Subtraction avoids overflowing long when the configured budget is Long.MAX_VALUE.
        while (size >= maximumEntries || weight > maximumWeight - retentionWeight) release(first);
        insert(key, value, h, retentionWeight); return old;
    }
    public synchronized V invalidate(K key) {
        int slot = find(key, hash(key)); return slot < 0 ? null : release(slot);
    }
    /** Release LRU cache ownership under memory pressure; configured admission budgets are unchanged. */
    public synchronized int trimTo(long targetWeight) {
        if (targetWeight < 0) throw new IllegalArgumentException("negative trim target");
        int removed = 0;
        while (weight > targetWeight) { release(first); removed++; }
        return removed;
    }
    /** Drop all references and metadata capacity owned by this cache, including zero-weight entries. */
    public synchronized void clear() {
        keys = EMPTY_OBJECTS; values = EMPTY_OBJECTS; weights = EMPTY_LONGS;
        hashes = EMPTY_INTS; before = EMPTY_INTS; after = EMPTY_INTS; positions = EMPTY_INTS; index = EMPTY_INTS;
        first = -1; last = -1; free = -1; used = 0; occupied = 0; size = 0; weight = 0L;
    }
    public synchronized Statistics explain() {
        return new Statistics(size, weight, maximumEntries, maximumWeight);
    }

    // Unchanged donor atoms (visibility/indentation only): find, vacant, rehash, unlink, hash, doubled.
    private int find(Object key, int hash) {
        if (index.length == 0) { return -1; }
        for (int p = hash & (index.length - 1); index[p] != 0; p = (p + 1) & (index.length - 1)) {
            int s = index[p] - 1;
            if (s >= 0 && hashes[s] == hash && Objects.equals(key, keys[s])) { return s; }
        }
        return -1;
    }
    private int vacant(int hash) {
        int p = hash & (index.length - 1);
        while (index[p] > 0) { p = (p + 1) & (index.length - 1); } return p;
    }
    private void rehash(int capacity) {
        int[] replacement = new int[capacity];
        for (int s = first; s >= 0; s = after[s]) {
            int p = hashes[s] & (capacity - 1);
            while (replacement[p] != 0) { p = (p + 1) & (capacity - 1); }
            replacement[p] = s + 1; positions[s] = p;
        }
        index = replacement; occupied = size;
    }
    private void grow() {
        int n = (int) Math.min(Math.min(Integer.MAX_VALUE - 8L, maximumEntries),
                Math.max(8L, used + (used >> 1) + 1L));
        if (n <= used) throw new IllegalStateException("cache slot capacity exhausted");
        // Allocate every replacement before publishing any lane, as the donor does.
        Object[] k = Arrays.copyOf(keys, n), v = Arrays.copyOf(values, n);
        long[] w = Arrays.copyOf(weights, n);
        int[] h = Arrays.copyOf(hashes, n), b = Arrays.copyOf(before, n),
                a = Arrays.copyOf(after, n), p = Arrays.copyOf(positions, n);
        keys = k; values = v; weights = w; hashes = h; before = b; after = a; positions = p;
    }
    private void insert(K key, V value, int hash, long retentionWeight) {
        if (free < 0 && used == keys.length) grow();
        if (index.length == 0) rehash(16);
        else if (occupied + 1 >= index.length - index.length / 3) {
            rehash(size + 1 >= index.length - index.length / 3 ? doubled(index.length) : index.length);
        }
        int p = vacant(hash), s;
        if (free < 0) { s = used++; } else { s = free; free = after[s]; }
        if (index[p] == 0) occupied++;
        keys[s] = key; values[s] = value; hashes[s] = hash; weights[s] = retentionWeight;
        index[p] = s + 1; positions[s] = p; linkLast(s); size++; weight += retentionWeight;
    }
    private void linkLast(int s) {
        before[s] = last; after[s] = -1;
        if (last < 0) { first = s; } else { after[last] = s; }
        last = s;
    }
    private void unlink(int s) {
        int b = before[s], a = after[s];
        if (b < 0) { first = a; } else { after[b] = a; }
        if (a < 0) { last = b; } else { before[a] = b; }
    }
    private void moveLast(int s) {
        if (s == last) return;
        unlink(s); linkLast(s);
    }
    private V release(int s) {
        V result = value(s); weight -= weights[s]; weights[s] = 0L;
        unlink(s); index[positions[s]] = -1; keys[s] = null; values[s] = null;
        before[s] = -1; positions[s] = -1; after[s] = free; free = s; size--; return result;
    }
    private static int hash(Object key) { int h = key == null ? 0 : key.hashCode(); return h ^ h >>> 16; }
    private static int doubled(int old) {
        if (old >= 1 << 30) { throw new OutOfMemoryError("hash index capacity exhausted"); }
        return old << 1;
    }
    @SuppressWarnings("unchecked") private V value(int slot) { return (V) values[slot]; }

    /** Package-local proof hook; no new public cache API and no production traversal allocation. */
    synchronized void verifyInvariants() {
        if (keys.length != values.length || keys.length != weights.length || keys.length != hashes.length ||
                keys.length != before.length || keys.length != after.length || keys.length != positions.length ||
                used > keys.length || used > maximumEntries || size < 0 || size > maximumEntries ||
                weight < 0 || weight > maximumWeight || (index.length & (index.length - 1)) != 0)
            throw new AssertionError("cache lane/budget invariant");
        boolean[] seen = new boolean[used]; int count = 0, previous = -1, buckets = 0, filled = 0;
        long total = 0L;
        for (int s = first; s >= 0; s = after[s]) {
            if (s >= used || seen[s] || before[s] != previous || positions[s] < 0 ||
                    positions[s] >= index.length || index[positions[s]] != s + 1 ||
                    find(keys[s], hashes[s]) != s || values[s] == null || weights[s] < 0)
                throw new AssertionError("cache live slot");
            seen[s] = true; previous = s; count++; total = Math.addExact(total, weights[s]);
        }
        if (count != size || previous != last || total != weight) throw new AssertionError("cache size/weight");
        for (int p = 0; p < index.length; p++) {
            if (index[p] != 0) filled++;
            if (index[p] > 0) {
                int s = index[p] - 1;
                if (s >= used || !seen[s] || positions[s] != p) throw new AssertionError("cache bucket");
                buckets++;
            } else if (index[p] < -1) throw new AssertionError("cache tombstone");
        }
        if (buckets != size || filled != occupied || index.length != 0 && filled == index.length)
            throw new AssertionError("cache index occupancy");
        for (int s = free; s >= 0; s = after[s]) {
            if (s >= used || seen[s] || keys[s] != null || values[s] != null || weights[s] != 0 ||
                    before[s] != -1 || positions[s] != -1) throw new AssertionError("cache released slot");
            seen[s] = true; count++;
        }
        if (count != used) throw new AssertionError("cache slot ownership");
    }
}
