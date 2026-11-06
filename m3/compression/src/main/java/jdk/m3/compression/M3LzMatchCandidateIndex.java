// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compression;

import java.util.Arrays;
import java.util.Objects;

/**
 * Bounded, reusable, single-probe match candidate table for four-byte LZ prefixes.
 * Only primitive arrays are retained: positions, exact four-byte keys, and epochs.
 * This is a match-finder *candidate*, not a guarantee of a best/longest match.
 * One owner thread operates the table at a time; no automatic codec routing occurs.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/main/java/com/synexia/algorithms/compression/M3LzMatchCandidateIndex.java blob b5e0fc7d2726; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class M3LzMatchCandidateIndex {
    private static final int MIN_TABLE_BITS = 8;
    private static final int MAX_TABLE_BITS = 20;
    private static final int KEY_BYTES = Integer.BYTES;

    private final int[] positions;
    private final int[] keys;
    private final int[] epochs;
    private final int mask;
    private int epoch;
    private byte[] input;

    /** Allocate exactly three fixed-width primitive arrays, 256..1,048,576 slots. */
    public M3LzMatchCandidateIndex(int tableBits) {
        if (tableBits < MIN_TABLE_BITS || tableBits > MAX_TABLE_BITS) {
            throw new IllegalArgumentException("LZ table bits must be 8..20");
        }
        int slots = 1 << tableBits;
        positions = new int[slots];
        keys = new int[slots];
        epochs = new int[slots];
        mask = slots - 1;
    }

    /** Start one block. Epoch isolation avoids a full-table clear on normal reuse. */
    public void begin(byte[] bytes) {
        input = Objects.requireNonNull(bytes, "bytes");
        nextEpoch();
    }

    /** Release the block and invalidate the table's prior positions. */
    public void clear() {
        input = null;
        nextEpoch();
    }

    /**
     * Return the previous candidate byte offset or -1, then record this position.
     * Equal four-byte keys are required: hash collisions never establish equality.
     * Candidate distance must be positive and no greater than maximumDistance.
     * A caller must independently evaluate longer matches via M3LzByteKernels.
     */
    public int findAndRecord(int position, int maximumDistance) {
        byte[] bytes = input;
        if (bytes == null) {
            throw new IllegalStateException("begin(byte[]) is required");
        }
        if (maximumDistance < 1 || position < 0 || position > bytes.length - KEY_BYTES) {
            throw new IllegalArgumentException("invalid LZ candidate range");
        }
        int key = keyAt(bytes, position);
        int slot = mix(key) & mask;
        int candidate = epochs[slot] == epoch && keys[slot] == key
                ? positions[slot] : -1;
        positions[slot] = position;
        keys[slot] = key;
        epochs[slot] = epoch;
        if (candidate < 0 || candidate >= position
                || position - candidate > maximumDistance) {
            return -1;
        }
        // The caller may have modified a mutable source array since recording.
        return keyAt(bytes, candidate) == key ? candidate : -1;
    }

    private void nextEpoch() {
        epoch++;
        if (epoch == 0) {
            Arrays.fill(epochs, 0);
            epoch = 1;
        }
    }

    private static int keyAt(byte[] data, int offset) {
        return (data[offset] & 0xff)
                | ((data[offset + 1] & 0xff) << 8)
                | ((data[offset + 2] & 0xff) << 16)
                | ((data[offset + 3] & 0xff) << 24);
    }

    private static int mix(int word) {
        word ^= word >>> 16;
        word *= 0x7feb352d;
        word ^= word >>> 15;
        word *= 0x846ca68b;
        return word ^ (word >>> 16);
    }
}
