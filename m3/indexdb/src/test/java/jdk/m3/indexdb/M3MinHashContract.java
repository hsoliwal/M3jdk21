// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import java.util.Locale;
import java.util.SplittableRandom;

/**
 * MinHash contract: on 1,000 random shingle-set pairs the Jaccard estimate (k = 128) is within
 * {@value #ERROR_MAX} of the exact Jaccard on average and on at least {@value #WITHIN_FRACTION_MIN}
 * of the pairs, with the per-pair maximum below {@value #ERROR_PAIR_MAX}; banded LSH (32 x 4)
 * recalls at least {@value #RECALL_MIN} of {@value #PLANTED} planted near-duplicates
 * (Jaccard 0.75..0.95) hidden among as many unrelated sets.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/f7b9599a63da1c1264ecacdcb4c9286e055a0948/synexia-openrewrite-recipes/crates/m3-hash-family-20261030/src/test/java/com/synexia/rewrite/hash/M3MinHashContract.java blob e782eeb03d22; recipe m3/indexdb/recipe/hash-family-20261106/relocate.py v3
public final class M3MinHashContract {
    static final int PAIRS = 1000;
    static final int PLANTED = 200;
    static final double ERROR_MAX = 0.05;
    static final double ERROR_PAIR_MAX = 0.20;
    static final double WITHIN_FRACTION_MIN = 0.80;
    static final double RECALL_MIN = 0.95;
    private static final long SEED = 0x4d33434f4e5452L;

    private M3MinHashContract() { }

    public static void main(String[] args) {
        M3MinHash.Permutations permutations = M3MinHash.Permutations.of(M3MinHash.DEFAULT_K, M3MinHash.DEFAULT_SEED);
        SplittableRandom random = new SplittableRandom(SEED);
        double sum = 0;
        double max = 0;
        int within = 0;
        for (int pair = 0; pair < PAIRS; pair++) {
            long[] left = set(random, 100 + random.nextInt(900));
            long[] right = overlap(random, left, random.nextDouble());
            double exact = M3HashDistances.jaccard(left, left.length, right, right.length);
            double estimate = M3MinHash.jaccardEstimate(M3MinHash.signature(left, left.length, permutations),
                    M3MinHash.signature(right, right.length, permutations));
            double error = Math.abs(exact - estimate);
            sum += error;
            max = Math.max(max, error);
            if (error <= ERROR_MAX) within++;
        }
        System.out.printf(Locale.ROOT, "INFO minhash k=%d pairs=%d meanAbsError=%.4f maxAbsError=%.4f within0.05=%d/%d%n",
                M3MinHash.DEFAULT_K, PAIRS, sum / PAIRS, max, within, PAIRS);
        check(sum / PAIRS <= ERROR_MAX, "mean Jaccard error above bound");
        check(within >= WITHIN_FRACTION_MIN * PAIRS, "too many pairs outside 0.05");
        check(max <= ERROR_PAIR_MAX, "a pair error above 0.20");
        recall(permutations, random);
        System.out.println("M3MinHashContract PASS");
    }

    static void recall(M3MinHash.Permutations permutations, SplittableRandom random) {
        long[][] signatures = new long[PLANTED * 3][];
        for (int i = 0; i < PLANTED; i++) {
            long[] base = set(random, 200 + random.nextInt(800));
            long[] near = overlap(random, base, 0.75 + 0.2 * random.nextDouble());
            signatures[i] = M3MinHash.signature(base, base.length, permutations);
            signatures[PLANTED + i] = M3MinHash.signature(near, near.length, permutations);
            long[] other = set(random, 200 + random.nextInt(800));
            signatures[2 * PLANTED + i] = M3MinHash.signature(other, other.length, permutations);
        }
        long[] pairs = M3MinHashLsh.candidatePairs(signatures, M3MinHashLsh.DEFAULT_BANDS, M3MinHashLsh.DEFAULT_ROWS);
        int found = 0;
        for (int i = 0; i < PLANTED; i++) {
            if (java.util.Arrays.binarySearch(pairs, ((long) i << 32) | (PLANTED + i)) >= 0) found++;
        }
        System.out.printf(Locale.ROOT, "INFO lsh bands=%d rows=%d planted=%d recalled=%d candidatePairs=%d recall=%.4f%n",
                M3MinHashLsh.DEFAULT_BANDS, M3MinHashLsh.DEFAULT_ROWS, PLANTED, found, pairs.length, (double) found / PLANTED);
        check(found >= RECALL_MIN * PLANTED, "LSH recall below bound");
    }

    /** Sorted distinct random set of {@code size} shingle hashes. */
    static long[] set(SplittableRandom random, int size) {
        long[] lane = new long[size];
        for (int i = 0; i < size; i++) lane[i] = random.nextLong();
        return M3HashLanes.sortedDistinct(lane, size);
    }

    /** Keeps each element of {@code base} with probability {@code keep}, then adds fresh ones to the same size. */
    static long[] overlap(SplittableRandom random, long[] base, double keep) {
        long[] lane = new long[base.length];
        int count = 0;
        for (long value : base) {
            if (random.nextDouble() < keep) lane[count++] = value;
        }
        while (count < base.length) lane[count++] = random.nextLong();
        return M3HashLanes.sortedDistinct(lane, count);
    }

    static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
}
