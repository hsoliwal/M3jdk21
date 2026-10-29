// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Objects;

/** Immutable primitive long-key Bloom membership prefilter.
 * False positives are possible, false negatives for inserted keys are not.
 * Never use a positive answer as an authorization or correctness proof.
 */
public final class M3BloomIndex {
    private static final long SEED_A = 0x9e3779b97f4a7c15L;
    private static final long SEED_B = 0x94d049bb133111ebL;
    private final long[] lane;
    private final int bitCount, rounds, keys;

    private M3BloomIndex(long[] lane, int bitCount, int rounds, int keys) {
        this.lane = lane;
        this.bitCount = bitCount;
        this.rounds = rounds;
        this.keys = keys;
    }

    /** Bit count must be a power of two in [64, 2^24], hashes 1..8. */
    public static M3BloomIndex build(long[] values, int bitCount, int hashes) {
        Objects.requireNonNull(values, "values");
        if (bitCount < 64 || bitCount > (1 << 24) || (bitCount & (bitCount - 1)) != 0
                || hashes < 1 || hashes > 8 || values.length > 2_000_000)
            throw new IllegalArgumentException("Bloom geometry");
        long[] bits = new long[bitCount >>> 6];
        for (long value : values) {
            long h1 = mix(value ^ SEED_A), h2 = mix(value ^ SEED_B) | 1L;
            for (int i = 0; i < hashes; i++) {
                int at = (int)(h1 + (long)i * h2) & (bitCount - 1);
                bits[at >>> 6] |= 1L << (at & 63);
            }
        }
        return new M3BloomIndex(bits, bitCount, hashes, values.length);
    }

    /** Possible membership; always check an authoritative index for positive results. */
    public boolean mightContain(long value) {
        long h1 = mix(value ^ SEED_A), h2 = mix(value ^ SEED_B) | 1L;
        for (int i = 0; i < rounds; i++) {
            int at = (int)(h1 + (long)i * h2) & (bitCount - 1);
            if ((lane[at >>> 6] & (1L << (at & 63))) == 0) return false;
        }
        return true;
    }

    public int bitCount() { return bitCount; }
    public int hashes() { return rounds; }
    public int insertedCount() { return keys; }
    public long[] snapshotBits() { return lane.clone(); }

    private static long mix(long x) {
        x ^= x >>> 30; x *= 0xbf58476d1ce4e5b9L;
        x ^= x >>> 27; x *= 0x94d049bb133111ebL;
        return x ^ (x >>> 31);
    }
}
