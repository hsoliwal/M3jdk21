// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.Arrays;
import java.util.Objects;

/** Advanced primitive-index string algorithms over Java UTF-16 code units. */
//FLLDCIM: RELOCATE - donor blob c637a62cf854 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/algorithm-extras-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/algorithm-extras-20261106/relocate.py v1
public final class M3AdvancedStringAlgorithms {
    private M3AdvancedStringAlgorithms() {}

    /** Odd-palindrome radii where radius includes the center character. */
    public static int[] manacherOdd(String value) {
        Objects.requireNonNull(value, "value");
        int n = value.length();
        int[] radius = new int[n];
        int left = 0;
        int right = -1;
        for (int i = 0; i < n; i++) {
            int k = i > right ? 1 : Math.min(radius[left + right - i], right - i + 1);
            while (i - k >= 0 && i + k < n && value.charAt(i - k) == value.charAt(i + k)) k++;
            radius[i] = k;
            if (i + k - 1 > right) {
                left = i - k + 1;
                right = i + k - 1;
            }
        }
        return radius;
    }

    /** Even-palindrome radii centered between i-1 and i. */
    public static int[] manacherEven(String value) {
        Objects.requireNonNull(value, "value");
        int n = value.length();
        int[] radius = new int[n];
        int left = 0;
        int right = -1;
        for (int i = 0; i < n; i++) {
            int k = i > right ? 0 : Math.min(radius[left + right - i + 1], right - i + 1);
            while (i - k - 1 >= 0 && i + k < n
                    && value.charAt(i - k - 1) == value.charAt(i + k)) {
                k++;
            }
            radius[i] = k;
            if (i + k - 1 > right) {
                left = i - k;
                right = i + k - 1;
            }
        }
        return radius;
    }

    /**
     * Suffix array of UTF-16 code-unit indices using doubling + primitive stable merge sort.
     * Complexity is O(n log^2 n) comparisons and O(n) auxiliary primitive storage.
     */
    public static int[] suffixArray(String value) {
        Objects.requireNonNull(value, "value");
        int n = value.length();
        if (n == 0) return new int[0];
        int[] order = new int[n];
        int[] buffer = new int[n];
        int[] rank = new int[n];
        int[] nextRank = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
            rank[i] = value.charAt(i);
        }

        int width = 1;
        while (true) {
            mergeSortSuffixes(order, buffer, rank, width, n);
            int classes = 0;
            nextRank[order[0]] = 0;
            for (int i = 1; i < n; i++) {
                if (compareSuffix(order[i - 1], order[i], rank, width, n) != 0) classes++;
                nextRank[order[i]] = classes;
            }
            int[] swap = rank;
            rank = nextRank;
            nextRank = swap;
            if (classes == n - 1) break;
            if (width >= n) break;
            width = width > n / 2 ? n : width << 1;
        }
        return order;
    }

    /** Kasai LCP array: result[i] is LCP(sa[i], sa[i+1]); length=max(0,n-1). */
    public static int[] lcpArray(String value, int[] suffixArray) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(suffixArray, "suffixArray");
        int n = value.length();
        if (suffixArray.length != n) throw new IllegalArgumentException("suffix array length mismatch");
        if (n <= 1) return new int[0];
        int[] inverse = new int[n];
        boolean[] seen = new boolean[n];
        for (int position = 0; position < n; position++) {
            int suffix = suffixArray[position];
            if (suffix < 0 || suffix >= n || seen[suffix]) {
                throw new IllegalArgumentException("suffix array is not a permutation");
            }
            seen[suffix] = true;
            inverse[suffix] = position;
        }
        int[] lcp = new int[n - 1];
        int length = 0;
        for (int suffix = 0; suffix < n; suffix++) {
            int position = inverse[suffix];
            if (position == n - 1) {
                length = 0;
                continue;
            }
            int other = suffixArray[position + 1];
            while (suffix + length < n
                    && other + length < n
                    && value.charAt(suffix + length) == value.charAt(other + length)) {
                length++;
            }
            lcp[position] = length;
            if (length > 0) length--;
        }
        return lcp;
    }

    public static String longestRepeatedSubstring(String value) {
        Objects.requireNonNull(value, "value");
        int[] suffixes = suffixArray(value);
        int[] lcp = lcpArray(value, suffixes);
        int bestLength = 0;
        int bestStart = 0;
        for (int i = 0; i < lcp.length; i++) {
            if (lcp[i] > bestLength) {
                bestLength = lcp[i];
                bestStart = suffixes[i];
            }
        }
        return value.substring(bestStart, bestStart + bestLength);
    }

    private static void mergeSortSuffixes(
            int[] order,
            int[] buffer,
            int[] rank,
            int width,
            int n) {
        for (int run = 1; run < n; run = run > n / 2 ? n : run << 1) {
            for (int start = 0; start < n; start += run << 1) {
                int middle = Math.min(start + run, n);
                int end = Math.min(start + (run << 1), n);
                int left = start;
                int right = middle;
                int cursor = start;
                while (left < middle && right < end) {
                    if (compareSuffix(order[left], order[right], rank, width, n) <= 0) {
                        buffer[cursor++] = order[left++];
                    } else {
                        buffer[cursor++] = order[right++];
                    }
                }
                while (left < middle) buffer[cursor++] = order[left++];
                while (right < end) buffer[cursor++] = order[right++];
            }
            System.arraycopy(buffer, 0, order, 0, n);
            if (run == n) break;
        }
    }

    private static int compareSuffix(
            int left,
            int right,
            int[] rank,
            int width,
            int n) {
        int comparison = Integer.compare(rank[left], rank[right]);
        if (comparison != 0) return comparison;
        int leftSecond = left + width < n ? rank[left + width] : -1;
        int rightSecond = right + width < n ? rank[right + width] : -1;
        return Integer.compare(leftSecond, rightSecond);
    }
}
