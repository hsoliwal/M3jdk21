// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.Objects;

/**
 * Immutable primitive hash index from UUID bits to first vector position.
 *
 * <p>Stores two long key columns, one int value column and a byte occupancy column. No UUID
 * objects or UUID strings are retained by the index.</p>
 */
//FLLDCIM: RELOCATE - donor blob 5a0ad78d3171 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/recipe/immutable-pools-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/immutable-pools-20261106/relocate.py v2
public final class M3Uuid128HashIndex {
    private final long[] most;
    private final long[] least;
    private final int[] positions;
    private final byte[] used;
    private final int mask;
    private final int size;

    private M3Uuid128HashIndex(
            long[] most, long[] least, int[] positions, byte[] used, int size) {
        this.most = most;
        this.least = least;
        this.positions = positions;
        this.used = used;
        this.mask = most.length - 1;
        this.size = size;
    }

    public static M3Uuid128HashIndex build(M3Uuid128Vector values, M3PoolProgressMonitor monitor) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(monitor, "monitor");
        int tableSize = tableSize(values.size());
        long[] most = new long[tableSize];
        long[] least = new long[tableSize];
        int[] positions = new int[tableSize];
        byte[] used = new byte[tableSize];
        int mask = tableSize - 1;
        int unique = 0;

        monitor.begin("Build primitive UUID128 hash index", values.size());
        try {
            for (int position = 0; position < values.size(); position++) {
                monitor.checkCancelled();
                long high = values.mostSignificantBitsAt(position);
                long low = values.leastSignificantBitsAt(position);
                int slot = slot(high, low, mask);
                while (used[slot] != 0) {
                    if (most[slot] == high && least[slot] == low) {
                        slot = -1;
                        break;
                    }
                    slot = (slot + 1) & mask;
                }
                if (slot >= 0) {
                    used[slot] = 1;
                    most[slot] = high;
                    least[slot] = low;
                    positions[slot] = position;
                    unique++;
                }
                monitor.worked(1);
            }
        } finally {
            monitor.done();
        }
        return new M3Uuid128HashIndex(most, least, positions, used, unique);
    }

    public int size() { return size; }
    public int capacity() { return most.length; }

    public int firstPosition(M3Uuid128 value) {
        Objects.requireNonNull(value, "value");
        return firstPosition(value.mostSignificantBits(), value.leastSignificantBits());
    }

    public int firstPosition(long mostSignificantBits, long leastSignificantBits) {
        int slot = slot(mostSignificantBits, leastSignificantBits, mask);
        for (int probes = 0; probes < used.length; probes++) {
            if (used[slot] == 0) return -1;
            if (most[slot] == mostSignificantBits && least[slot] == leastSignificantBits) {
                return positions[slot];
            }
            slot = (slot + 1) & mask;
        }
        return -1;
    }

    public boolean contains(M3Uuid128 value) {
        return firstPosition(value) >= 0;
    }

    public long primitivePayloadBytes() {
        return (long) most.length * Long.BYTES
            + (long) least.length * Long.BYTES
            + (long) positions.length * Integer.BYTES
            + used.length;
    }

    private static int tableSize(int entries) {
        if (entries <= 0) return 1;
        if (entries > (1 << 29)) throw new IllegalArgumentException("UUID index too large");
        int required = Math.max(2, entries << 1);
        int size = 1;
        while (size < required) size <<= 1;
        return size;
    }

    private static int slot(long high, long low, int mask) {
        long value = high ^ Long.rotateLeft(low, 29);
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return ((int) (value ^ (value >>> 32))) & mask;
    }
}
