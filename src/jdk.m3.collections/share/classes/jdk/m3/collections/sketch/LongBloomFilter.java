/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections.sketch;

import java.util.Arrays;

/**
 * Approximate long membership with a bit-packed long[] and no retained keys.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongBloomFilter.java}
 * blob {@code 306e0e7e908a00f4d48fb00331451d707e9bf1be} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LongBloomFilter {
    private static final double LN2 = Math.log(2.0d);

    private final long[] words;
    private final int bitMask;
    private final int hashCount;
    private long insertions;

    public LongBloomFilter(int bitCount, int hashCount) {
        if (bitCount <= 0) throw new IllegalArgumentException("bitCount <= 0");
        if (hashCount <= 0 || hashCount > 32) {
            throw new IllegalArgumentException("hashCount must be 1..32");
        }
        int normalized = nextPowerOfTwo(bitCount);
        words = new long[Math.max(1, normalized >>> 6)];
        bitMask = (words.length << 6) - 1;
        this.hashCount = hashCount;
    }

    public static LongBloomFilter forExpectedSize(long expectedElements, double falsePositiveProbability) {
        if (expectedElements <= 0) throw new IllegalArgumentException("expectedElements <= 0");
        if (!(falsePositiveProbability > 0.0d && falsePositiveProbability < 1.0d)) {
            throw new IllegalArgumentException("falsePositiveProbability must be in (0,1)");
        }
        double bits = -expectedElements * Math.log(falsePositiveProbability) / (LN2 * LN2);
        if (bits > (1L << 30)) throw new IllegalArgumentException("filter too large");
        int bitCount = (int) Math.max(64L, (long) Math.ceil(bits));
        int hashes = Math.max(1, Math.min(32,
                (int) Math.round((bitCount / (double) expectedElements) * LN2)));
        return new LongBloomFilter(bitCount, hashes);
    }

    public int bitCount() { return words.length << 6; }
    public int hashCount() { return hashCount; }
    public long insertions() { return insertions; }
    public long payloadBytes() { return (long) words.length * Long.BYTES; }

    public boolean add(long value) {
        long h1 = mix64(value);
        long h2 = mix64(h1 ^ 0x9e3779b97f4a7c15L) | 1L;
        boolean changed = false;
        for (int i = 0; i < hashCount; i++) {
            int bit = (int) (h1 + i * h2) & bitMask;
            int word = bit >>> 6;
            long mask = 1L << (bit & 63);
            if ((words[word] & mask) == 0L) {
                words[word] |= mask;
                changed = true;
            }
        }
        insertions++;
        return changed;
    }

    public boolean mightContain(long value) {
        long h1 = mix64(value);
        long h2 = mix64(h1 ^ 0x9e3779b97f4a7c15L) | 1L;
        for (int i = 0; i < hashCount; i++) {
            int bit = (int) (h1 + i * h2) & bitMask;
            if ((words[bit >>> 6] & (1L << (bit & 63))) == 0L) return false;
        }
        return true;
    }

    public double estimatedFalsePositiveProbability() {
        double occupied = 1.0d - Math.exp(-hashCount * insertions / (double) bitCount());
        return Math.pow(occupied, hashCount);
    }

    public void clear() {
        Arrays.fill(words, 0L);
        insertions = 0L;
    }

    private static int nextPowerOfTwo(int value) {
        if (value <= 64) return 64;
        if (value > (1 << 30)) throw new IllegalArgumentException("bitCount too large");
        return Integer.highestOneBit(value - 1) << 1;
    }

    static long mix64(long value) {
        long z = value + 0x9e3779b97f4a7c15L;
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
