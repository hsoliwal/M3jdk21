/* *****************************************************************************
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
package jdk.internal.m3.collections.bits;

import java.util.BitSet;

/**
 * Bloom filter with primitive double-hashing hot paths and no per-probe lambda traversal.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/ds/BloomFilter.java}
 * blob {@code 6836b30dc0bbba69f8bd4959bdd7180db2b655ae} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (algorithms.ds batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class BloomFilter<T> {
    private final BitSet bits;
    private final int numBits;
    private final int numHashFunctions;
    private int count;

    private BloomFilter(int numBits, int numHashFunctions) {
        this.bits = new BitSet(numBits);
        this.numBits = numBits;
        this.numHashFunctions = numHashFunctions;
    }

    public static <T> BloomFilter<T> create(int expectedElements, double falsePositiveRate) {
        int bits = optimalNumBits(expectedElements, falsePositiveRate);
        return new BloomFilter<>(bits, optimalNumHashFunctions(expectedElements, bits));
    }

    public static <T> BloomFilter<T> create(int numBits, int numHashFunctions) {
        return new BloomFilter<>(numBits, numHashFunctions);
    }

    public static <T> BloomFilter<T> create() {
        return new BloomFilter<>(10_000, 7);
    }

    public BloomFilter<T> add(T item) {
        long baseHash = mix64(item == null ? 0L : item.hashCode());
        long deltaHash = mix64(baseHash + 0x9e3779b97f4a7c15L);
        for (int seed = 0; seed < numHashFunctions; seed++) {
            bits.set(position(baseHash, deltaHash, seed));
        }
        count++;
        return this;
    }

    public boolean mightContain(T item) {
        long baseHash = mix64(item == null ? 0L : item.hashCode());
        long deltaHash = mix64(baseHash + 0x9e3779b97f4a7c15L);
        for (int seed = 0; seed < numHashFunctions; seed++) {
            if (!bits.get(position(baseHash, deltaHash, seed))) return false;
        }
        return true;
    }

    public double estimatedFalsePositiveRate() {
        double bitsSet = bits.cardinality();
        return Math.pow(bitsSet / numBits, numHashFunctions);
    }

    public int count() {
        return count;
    }

    public int bitsSet() {
        return bits.cardinality();
    }

    public int numBits() {
        return numBits;
    }

    public int numHashFunctions() {
        return numHashFunctions;
    }

    public void clear() {
        bits.clear();
        count = 0;
    }

    public BloomFilter<T> merge(BloomFilter<T> other) {
        if (numBits != other.numBits || numHashFunctions != other.numHashFunctions) {
            throw new IllegalArgumentException("Filters must have same configuration");
        }
        BloomFilter<T> merged = new BloomFilter<>(numBits, numHashFunctions);
        merged.bits.or(bits);
        merged.bits.or(other.bits);
        merged.count = count + other.count;
        return merged;
    }

    public static int optimalNumBits(int expectedElements, double falsePositiveRate) {
        return (int) Math.ceil(
                -expectedElements * Math.log(falsePositiveRate) / (Math.log(2) * Math.log(2)));
    }

    public static int optimalNumHashFunctions(int expectedElements, int numBits) {
        return Math.max(1, (int) Math.round((double) numBits / expectedElements * Math.log(2)));
    }

    private int position(long baseHash, long deltaHash, int seed) {
        return Math.floorMod(baseHash + seed * deltaHash, numBits);
    }

    private static long mix64(long value) {
        long mixed = value;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return mixed & Long.MAX_VALUE;
    }

    @Override
    public String toString() {
        return String.format(
                "M3BloomFilter[bits=%d, hashes=%d, count=%d, fpr=%.4f]",
                numBits,
                numHashFunctions,
                count,
                estimatedFalsePositiveRate());
    }
}
