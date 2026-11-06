// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * MinHash signatures: {@code k} universal-hash permutations over a 64-bit shingle lane.
 *
 * <p>Input: a shingle lane (any order, duplicates allowed) and a {@link Permutations} family.
 * Output: {@code long[k]} signature, {@code sig[i] = min over x of (a_i * x + b_i) mod p},
 * {@code p = 2^61 - 1}. Effects: none. Failure: an empty lane gives a signature of {@code p}
 * (no element), so two empty sets estimate Jaccard 1. The family is derived from a seed through
 * {@link SplittableRandom}, so signatures are reproducible; the discriminator replaces it with a
 * non-random family (a = 1, b = i) and the Jaccard bound must then fail. Pattern donor: Lucene
 * 10.5.2 MinHashFilter (blob 9cf546b27db1c8d28fb6851f821c2d9373a8cf4a, Apache-2.0): rehash per
 * hash index and keep the minimum; its 128-bit Murmur3 and bucket rotation are not reused.</p>
 */
//FLLDCIM: MINHASH — k permutations as one long lane; Jaccard estimate = equal rows / k
//FLLDCIM: HPCPOP — mulmod 2^61-1 via multiplyHigh; no per-element objects
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/f7b9599a63da1c1264ecacdcb4c9286e055a0948/synexia-openrewrite-recipes/crates/m3-hash-family-20261030/src/main/java/com/synexia/rewrite/hash/M3MinHash.java blob b0c2a0b70926; recipe m3/indexdb/recipe/hash-family-20261106/relocate.py v3
public final class M3MinHash {
    public static final String ALGORITHM = "MINHASH";
    public static final int VERSION = 1;
    public static final int DEFAULT_K = 128;
    public static final long DEFAULT_SEED = 0x4d334d494e48L;
    public static final long MERSENNE_61 = (1L << 61) - 1;

    /** One universal-hash family: {@code h_i(x) = (a[i] * x + b[i]) mod 2^61-1}. */
    public record Permutations(long[] a, long[] b) {
        public int k() { return a.length; }

        public static Permutations of(int k, long seed) {
            SplittableRandom random = new SplittableRandom(seed);
            long[] a = new long[k];
            long[] b = new long[k];
            for (int i = 0; i < k; i++) {
                a[i] = 1 + random.nextLong(MERSENNE_61 - 1);
                b[i] = random.nextLong(MERSENNE_61);
            }
            return new Permutations(a, b);
        }
    }

    private M3MinHash() { }

    public static long[] signature(long[] shingles, int size, Permutations permutations) {
        int k = permutations.k();
        long[] signature = new long[k];
        Arrays.fill(signature, MERSENNE_61);
        long[] a = permutations.a();
        long[] b = permutations.b();
        for (int s = 0; s < size; s++) {
            long x = shingles[s] & MERSENNE_61;
            for (int i = 0; i < k; i++) {
                long h = affine(a[i], x, b[i]);
                if (h < signature[i]) signature[i] = h;
            }
        }
        return signature;
    }

    /** Estimated Jaccard: fraction of equal rows. */
    public static double jaccardEstimate(long[] left, long[] right) {
        if (left.length != right.length) throw new IllegalArgumentException("signature length");
        int equal = 0;
        for (int i = 0; i < left.length; i++) {
            if (left[i] == right[i]) equal++;
        }
        return left.length == 0 ? 1.0 : (double) equal / left.length;
    }

    /** {@code (a * x + b) mod 2^61-1} for {@code a, x, b < 2^61}. */
    static long affine(long a, long x, long b) {
        long high = Math.multiplyHigh(a, x);
        long low = a * x;
        long reduced = (high << 3) | (low >>> 61);
        reduced += low & MERSENNE_61;
        reduced += b;
        while (reduced >= MERSENNE_61) reduced -= MERSENNE_61;
        return reduced;
    }
}
