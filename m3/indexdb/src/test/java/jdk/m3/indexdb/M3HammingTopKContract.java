// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/** Independent complete-sort oracle for every public Hamming top-k output contract. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-algo/src/test/java/com/synexia/algorithms/agent/TopKByHammingHeapSortProof.java blob 32aa8b666031; recipe m3/indexdb/recipe/hamming-topk-20261106/relocate.py v2
public final class M3HammingTopKContract {
    private static final Comparator<M3HammingTopK.Match> ORDER =
            Comparator.comparingInt(M3HammingTopK.Match::distance)
                    .thenComparingInt(M3HammingTopK.Match::index);
    private static long comparisons;
    private static long calls;

    private M3HammingTopKContract() {}

    public static void main(String[] args) {
        Random random = new Random(0x4d33544845415053L);
        for (int trial = 0; trial < 2_500; trial++) {
            int n = trial % 20 == 0 ? 2048 : random.nextInt(256);
            long query = random.nextLong();
            long[] values = new long[n];
            int[] distances = new int[n];
            for (int i = 0; i < n; i++) {
                values[i] = switch (trial % 6) {
                    case 0 -> query;
                    case 1 -> query ^ 1;
                    case 2 -> query ^ (1L << (i & 63));
                    case 3 -> -1L;
                    case 4 -> random.nextLong();
                    default -> (i & 1) == 0 ? query : ~query;
                };
                distances[i] = Long.bitCount(query ^ values[i]);
            }
            int[] ks = {0, 1, 2, 3, n / 2, n, n + 5};
            for (int k : ks) {
                check(query, values, distances, k);
            }
        }
        for (int n : new int[]{0, 1, 2, 3, 64, 1024, 4096, 16384}) {
            long[] values = new long[n];
            int[] distances = new int[n];
            for (int i = 0; i < n; i++) values[i] = 1L << (i & 63);
            Arrays.fill(distances, 1);
            check(0L, values, distances, n);
            check(0L, values, distances, Math.min(n, 7));
        }
        refuses(() -> M3HammingTopK.nearest(0L, null, 1), NullPointerException.class);
        refuses(() -> M3HammingTopK.nearest(0L, new long[1], -1), IllegalArgumentException.class);
        refuses(() -> M3HammingTopK.fromDistances(null, new long[0], 0), NullPointerException.class);
        refuses(() -> M3HammingTopK.fromDistances(new int[1], new long[0], 0), IllegalArgumentException.class);
        refuses(() -> M3HammingTopK.fromDistances(new int[]{-1}, new long[]{0L}, 1), IllegalArgumentException.class);
        refuses(() -> M3HammingTopK.fromDistances(new int[]{65}, new long[]{0L}, 1), IllegalArgumentException.class);
        // Historical contract: k=0 validates lengths and k, but not prepared distance values.
        checkEquals(0, M3HammingTopK.fromDistances(new int[]{65}, new long[]{0L}, 0).length);
        System.out.println("M3_TOPK_HEAPSORT_DIFFERENTIAL_PASS cases=" + calls
                + " comparisons=" + comparisons + " seed=0x4d33544845415053");
    }

    private static void check(long query, long[] values, int[] distances, int k) {
        long[] frozenValues = values.clone();
        int[] frozenDistances = distances.clone();
        M3HammingTopK.Match[] expected = new M3HammingTopK.Match[values.length];
        for (int i = 0; i < values.length; i++) {
            expected[i] = new M3HammingTopK.Match(i, distances[i], values[i]);
        }
        Arrays.sort(expected, ORDER);
        expected = Arrays.copyOf(expected, Math.min(k, values.length));
        var stream = M3HammingTopK.nearest(query, values, k);
        var prepared = M3HammingTopK.fromDistances(distances, values, k);
        if (!Arrays.equals(expected, stream) || !Arrays.equals(expected, prepared)) {
            throw new AssertionError("topk oracle failure k=" + k + " n=" + values.length);
        }
        if (!Arrays.equals(frozenValues, values) || !Arrays.equals(frozenDistances, distances)) {
            throw new AssertionError("input mutated");
        }
        calls += 2;
        comparisons += expected.length;
    }

    private static void refuses(Runnable runnable, Class<? extends Throwable> expected) {
        try { runnable.run(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("wrong refusal: " + failure, failure);
        }
        throw new AssertionError("missing refusal: " + expected.getName());
    }

    private static void checkEquals(int expected, int actual) {
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
    }
}
