// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

/**
 * Linear-space witness for LCS of signed integer sequences.
 * Supports HackerRank's actual-sequence contract, not only an LCS length.
 * No public product JNI or automatic native dispatch is introduced.
 */
//FLLDCIM: RELOCATE - donor blob f160180d8263 at commit 78cf2ad4c7e5 (repository, path, URL and digests: m3/algorithms/recipe/catalogue-kernels-20261107/PROVENANCE.tsv); recipe m3/algorithms/recipe/catalogue-kernels-20261107/relocate.py v1
public final class M3IntLcsWitness {
    private static final int MAX_LENGTH = 4_096;
    private static final long MAX_CELLS = 8_388_608L;

    private M3IntLcsWitness() { }

    private static void admit(int[] a, int[] b) {
        if (a == null || b == null) throw new NullPointerException("integer sequences");
        if (a.length > MAX_LENGTH || b.length > MAX_LENGTH
                || (long) a.length * b.length > MAX_CELLS) {
            throw new IllegalArgumentException("bounded integer LCS witnesses only");
        }
    }

    /** O(a.length*b.length) time and O(min(a.length,b.length)) work space, excluding output. */
    public static int[] javaWitness(int[] a, int[] b) {
        admit(a, b);
        if (a.length == 0 || b.length == 0) return new int[0];
        // Hirschberg's second-dimension vector is chosen as the shorter input.
        // This swap does not change the accepted witness contract.
        int[] left = a.length >= b.length ? a : b;
        int[] right = a.length >= b.length ? b : a;
        int[] output = new int[right.length];
        int count = build(left, 0, left.length, right, 0, right.length, output, 0);
        return Arrays.copyOf(output, count);
    }

    private static int split(int[] a, int begin, int mid, int end,
                             int[] b, int bBegin, int bEnd) {
        int length = bEnd - bBegin;
        int[] forward = new int[length + 1];
        int[] backward = new int[length + 1];
        for (int ai = begin; ai < mid; ai++) {
            int northwest = 0;
            for (int j = 0; j < length; j++) {
                int prior = forward[j + 1];
                if (a[ai] == b[bBegin + j]) forward[j + 1] = northwest + 1;
                else forward[j + 1] = Math.max(forward[j], prior);
                northwest = prior;
            }
        }
        for (int ai = end - 1; ai >= mid; ai--) {
            int southeast = 0;
            for (int j = length - 1; j >= 0; j--) {
                int prior = backward[j];
                if (a[ai] == b[bBegin + j]) backward[j] = southeast + 1;
                else backward[j] = Math.max(prior, backward[j + 1]);
                southeast = prior;
            }
        }
        int best = -1;
        int index = 0;
        for (int j = 0; j <= length; j++) {
            int score = forward[j] + backward[j];
            // Earliest split in b breaks ties; no lexicographic claim.
            if (score > best) { best = score; index = j; }
        }
        return bBegin + index;
    }

    private static int build(int[] a, int begin, int end, int[] b,
                             int bBegin, int bEnd, int[] output, int used) {
        if (begin == end || bBegin == bEnd) return used;
        if (end - begin == 1) {
            for (int j = bBegin; j < bEnd; j++) {
                if (a[begin] == b[j]) { output[used++] = a[begin]; break; }
            }
            return used;
        }
        int mid = begin + (end - begin) / 2;
        int cut = split(a, begin, mid, end, b, bBegin, bEnd);
        int next = build(a, begin, mid, b, bBegin, cut, output, used);
        return build(a, mid, end, b, cut, bEnd, output, next);
    }

    /** Independent rolling-row length oracle, not derived from Hirschberg split logic. */
    public static int referenceLength(int[] a, int[] b) {
        admit(a, b);
        int[] shorter = a.length <= b.length ? a : b;
        int[] longer = a.length <= b.length ? b : a;
        int[] row = new int[shorter.length + 1];
        for (int value : longer) {
            int diagonal = 0;
            for (int j = 0; j < shorter.length; j++) {
                int old = row[j + 1];
                if (value == shorter[j]) row[j + 1] = diagonal + 1;
                else row[j + 1] = Math.max(row[j], old);
                diagonal = old;
            }
        }
        return row[shorter.length];
    }

    /** Test-only subsequence predicate; input equality is exact signed int equality. */
    public static boolean isSubsequence(int[] needle, int[] haystack) {
        int found = 0;
        for (int value : haystack) if (found < needle.length && value == needle[found]) found++;
        return found == needle.length;
    }

}
