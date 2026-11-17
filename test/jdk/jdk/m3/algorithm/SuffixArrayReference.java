// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.Objects;

/**
 * StringAlgorithms.suffixArray as on master at ef5c909856a4 (doubling with a comparison merge sort
 * per round), kept as the reference of SuffixArrayOwnerTest; test only.
 */
final class SuffixArrayReference {
    private SuffixArrayReference() {
    }

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
