// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.util.Arrays;
import java.util.Objects;

/**
 * Opt-in four-way LZ prefix candidate table using the same three primitive
 * array lanes and total slot budget as the existing direct-mapped index.
 * Each bucket retains up to four distinct exact four-byte keys. This class
 * does not select a codec, promise longest matches, or change compressed bytes.
 * One worker owns an instance; input mutation is rechecked before returning.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/main/java/com/synexia/algorithms/compression/M3LzFourWayCandidateIndex.java blob 064633125544; recipe m3/compression/recipe/compression-20261106/relocate.py v3
final class LzFourWayCandidateIndex {
    private static final int MIN_BITS = 8;
    private static final int MAX_BITS = 20;
    private static final int KEY_BYTES = Integer.BYTES;
    private static final int WAYS = 4;

    private final int[] positions;
    private final int[] keys;
    private final int[] epochs;
    private final int bucketMask;
    private int epoch;
    private byte[] input;

    LzFourWayCandidateIndex(int tableBits) {
        if (tableBits < MIN_BITS || tableBits > MAX_BITS) {
            throw new IllegalArgumentException("LZ table bits must be 8..20");
        }
        int slots = 1 << tableBits;
        positions = new int[slots];
        keys = new int[slots];
        epochs = new int[slots];
        bucketMask = (slots / WAYS) - 1;
    }

    void begin(byte[] bytes) {
        input = Objects.requireNonNull(bytes, "bytes");
        nextEpoch();
    }

    void clear() {
        input = null;
        nextEpoch();
    }

    /**
     * Finds one exact prefix at an earlier valid position, then records this
     * position. A slot with the same key is refreshed; otherwise use a vacant
     * epoch slot or evict the smallest recorded position in the bucket.
     * Supports any visit order; increasing positions generally improve reuse.
     */
    int findAndRecord(int position, int maximumDistance) {
        byte[] bytes = input;
        if (bytes == null) {
            throw new IllegalStateException("begin(byte[]) is required");
        }
        if (maximumDistance < 1 || position < 0
                || position > bytes.length - KEY_BYTES) {
            throw new IllegalArgumentException("invalid LZ candidate range");
        }
        int key = keyAt(bytes, position);
        int base = (mix(key) & bucketMask) * WAYS;
        int vacant = -1;
        int oldest = -1;
        int oldestPosition = Integer.MAX_VALUE;
        for (int way = 0; way < WAYS; way++) {
            int slot = base + way;
            if (epochs[slot] != epoch) {
                if (vacant < 0) vacant = slot;
                continue;
            }
            int previous = positions[slot];
            if (previous < oldestPosition) {
                oldestPosition = previous;
                oldest = slot;
            }
            if (keys[slot] == key) {
                // One slot per key: on a hit, refresh in place and avoid
                // scanning the remaining ways on this hot path.
                int candidate = previous >= 0 && previous < position
                        && position - previous <= maximumDistance
                        && keyAt(bytes, previous) == key ? previous : -1;
                positions[slot] = position;
                return candidate;
            }
        }
        int slot = vacant >= 0 ? vacant : oldest;
        positions[slot] = position;
        keys[slot] = key;
        epochs[slot] = epoch;
        return -1;
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
