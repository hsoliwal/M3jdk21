// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

/** Four-way saturating TinyLFU-style frequency estimate over packed 4-bit counters.
 * Approximate admission signal only, not a complete cache, eviction policy or thread-safe owner.
 */
public final class M3FrequencySketch {
    private static final int ROWS = 4;
    private static final long COUNTER_MASK = 0x7777777777777777L;
    private static final long[] SEEDS = {
            0x9e3779b97f4a7c15L, 0xbf58476d1ce4e5b9L,
            0x94d049bb133111ebL, 0xd6e8feb86659fd93L
    };
    private final int width, sampleWindow;
    private final long[] counters;
    private int sample;

    public M3FrequencySketch(int widthPower, int sampleWindow) {
        if (widthPower < 4 || widthPower > 16 || sampleWindow < 1 || sampleWindow > 10_000_000)
            throw new IllegalArgumentException("frequency sketch budget");
        width = 1 << widthPower;
        this.sampleWindow = sampleWindow;
        counters = new long[ROWS * width >>> 4];
    }

    /** Adds one observation, saturating each of four counters at 15. */
    public void observe(long key) {
        for (int row = 0; row < ROWS; row++) {
            int position = row * width + ((int) mix(key ^ SEEDS[row]) & (width - 1));
            int slot = position >>> 4, shift = (position & 15) << 2;
            long count = (counters[slot] >>> shift) & 15L;
            if (count < 15) counters[slot] += 1L << shift;
        }
        if (++sample == sampleWindow) {
            for (int word = 0; word < counters.length; word++)
                counters[word] = (counters[word] >>> 1) & COUNTER_MASK;
            sample = 0;
        }
    }

    public int estimate(long key) {
        int estimate = 15;
        for (int row = 0; row < ROWS; row++) {
            int position = row * width + ((int) mix(key ^ SEEDS[row]) & (width - 1));
            estimate = Math.min(estimate,
                    (int)((counters[position >>> 4] >>> ((position & 15) << 2)) & 15));
        }
        return estimate;
    }

    public boolean admit(long candidate, long victim) {
        return estimate(candidate) >= estimate(victim);
    }
    public int samplesInCurrentWindow() { return sample; }
    public long[] packedSnapshot() { return counters.clone(); }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xbf58476d1ce4e5b9L;
        value ^= value >>> 27;
        value *= 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
