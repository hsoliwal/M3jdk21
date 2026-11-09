// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

/**
 * Distance atoms over the family's outputs: Hamming on 64-bit fingerprints, exact Jaccard on
 * sorted distinct shingle lanes, L-Hash similarity as in {@code cpu_lhash.py similarity}.
 *
 * <p>Effects: none. Failure: {@link #jaccard} requires sorted distinct lanes (no check, as the
 * merge walk is the hot path); unsorted input gives a wrong ratio, never an exception.</p>
 */
//FLLDCIM: HPCPOP — bitCount and merge walks; no allocation except the int lane of hammingAll
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/f7b9599a63da1c1264ecacdcb4c9286e055a0948/synexia-openrewrite-recipes/crates/m3-hash-family-20261030/src/main/java/com/synexia/rewrite/hash/M3Distances.java blob 6a73fa37ebe1; recipe m3/indexdb/recipe/hash-family-20261106/relocate.py v3
public final class M3HashDistances {
    public static final String HAMMING = "HAMMING";
    public static final String JACCARD = "JACCARD";
    public static final int VERSION = 1;

    private M3HashDistances() { }

    public static int hamming(long left, long right) {
        return Long.bitCount(left ^ right);
    }

    /** Hamming distance of {@code query} to each of {@code candidates[0, size)}. */
    public static int[] hammingAll(long query, long[] candidates, int size) {
        int[] distances = new int[size];
        for (int i = 0; i < size; i++) distances[i] = Long.bitCount(query ^ candidates[i]);
        return distances;
    }

    /** Exact Jaccard of two sorted distinct lanes; two empty lanes are identical (1.0). */
    public static double jaccard(long[] left, int leftSize, long[] right, int rightSize) {
        int i = 0;
        int j = 0;
        int both = 0;
        while (i < leftSize && j < rightSize) {
            if (left[i] == right[j]) { both++; i++; j++; }
            else if (left[i] < right[j]) i++;
            else j++;
        }
        int union = leftSize + rightSize - both;
        return union == 0 ? 1.0 : (double) both / union;
    }

    /** cpu_lhash.py similarity: 1 on H1 match, else 0.4 H2 + 0.3 H3 + 0.2 H4 + 0.1 window Jaccard. */
    public static double lhashSimilarity(long[] left, long[] leftWindows, long[] right, long[] rightWindows) {
        if (left[0] == right[0]) return 1.0;
        double score = 0.0;
        if (left[1] == right[1]) score += 0.4;
        if (left[2] == right[2]) score += 0.3;
        if (left[3] == right[3]) score += 0.2;
        long[] a = M3HashLanes.sortedDistinct(leftWindows, leftWindows.length);
        long[] b = M3HashLanes.sortedDistinct(rightWindows, rightWindows.length);
        double windows = (a.length + b.length == 0) ? 0.0 : jaccard(a, a.length, b, b.length);
        return score + 0.1 * windows;
    }
}
