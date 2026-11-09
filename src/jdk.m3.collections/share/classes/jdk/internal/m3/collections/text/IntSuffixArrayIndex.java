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
package jdk.internal.m3.collections.text;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable suffix-array + LCP index over int symbols.
 *
 * <p>Construction uses prefix doubling and primitive counting/radix sorting.
 * Retained state is symbols, suffix array, inverse rank and adjacent LCP arrays.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntSuffixArrayIndex.java}
 * blob {@code 6b01624267087b89ff0c3dfecff904666d972f6a} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class IntSuffixArrayIndex {
    private final int[] symbols;
    private final int[] suffixes;
    private final int[] ranks;
    private final int[] lcp;

    public IntSuffixArrayIndex(int[] input) {
        Objects.requireNonNull(input, "input");
        symbols = input.clone();
        suffixes = buildSuffixArray(symbols);
        ranks = new int[symbols.length];
        for (int rank = 0; rank < suffixes.length; rank++) ranks[suffixes[rank]] = rank;
        lcp = buildLcp(symbols, suffixes, ranks);
    }

    public int length() { return symbols.length; }
    public int suffixCount() { return suffixes.length; }
    public long payloadBytes() {
        return (long) (symbols.length + suffixes.length + ranks.length + lcp.length)
                * Integer.BYTES;
    }

    public int symbolAt(int index) {
        return symbols[index];
    }

    public int suffixAtRank(int rank) {
        return suffixes[rank];
    }

    public int rankOfSuffix(int suffixStart) {
        if (suffixStart < 0 || suffixStart >= symbols.length) {
            throw new IndexOutOfBoundsException(suffixStart);
        }
        return ranks[suffixStart];
    }

    /** LCP between suffixAtRank(rank) and suffixAtRank(rank+1). */
    public int lcpAdjacent(int rank) {
        if (rank < 0 || rank >= lcp.length) throw new IndexOutOfBoundsException(rank);
        return lcp[rank];
    }

    public boolean contains(int[] pattern) {
        // Membership requires only the existing first matching suffix probe.
        return anyOccurrence(pattern) >= 0;
    }

    public int anyOccurrence(int[] pattern) {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.length == 0) return 0;
        // An oversized pattern cannot fit inside any suffix.
        if (pattern.length > symbols.length) return -1;
        int lower = lowerBound(pattern);
        return lower < 0 ? -1 : suffixes[lower];
    }

    public int countOccurrences(int[] pattern) {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.length == 0) return symbols.length + 1;
        if (pattern.length > symbols.length) return 0;
        int lower = lowerBound(pattern);
        // A tagged miss from the lower-bound pass proves there is no matching suffix.
        // No extra full-prefix comparison is needed for positive queries.
        if (lower < 0) return 0;
        int upper = upperBound(pattern, lower);
        return upper - lower;
    }

    public int[] suffixArray() { return suffixes.clone(); }
    public int[] lcpArray() { return lcp.clone(); }

    /**
     * Return the first matching suffix rank, or ~insertionRank on a miss.
     * An ordinary lower-bound search necessarily probes at least one matching
     * rank when a matching interval exists, so there is no second prefix probe.
     * The negative encoding is private and changes no public result or shape.
     */
    private int lowerBound(int[] pattern) {
        int lo = 0;
        int hi = suffixes.length;
        boolean matched = false;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            int comparison = compareSuffixWithPattern(suffixes[mid], pattern);
            if (comparison < 0) {
                lo = mid + 1;
            } else {
                matched |= comparison == 0;
                hi = mid;
            }
        }
        return matched ? lo : ~lo;
    }

    private int upperBound(int[] pattern, int lowerBound) {
        // The upper bound can never precede the proven lower bound.
        int lo = lowerBound;
        int hi = suffixes.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (compareSuffixWithPattern(suffixes[mid], pattern) <= 0) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    private int compareSuffixWithPattern(int suffixStart, int[] pattern) {
        int i = suffixStart;
        int p = 0;
        while (i < symbols.length && p < pattern.length) {
            int comparison = Integer.compare(symbols[i], pattern[p]);
            if (comparison != 0) return comparison;
            i++;
            p++;
        }
        if (p == pattern.length) return 0;
        return -1;
    }

    private static int[] buildSuffixArray(int[] values) {
        int n = values.length;
        if (n == 0) return new int[0];

        int[] sortedValues = values.clone();
        Arrays.sort(sortedValues);
        int classes = 0;
        for (int value : sortedValues) {
            if (classes == 0 || sortedValues[classes - 1] != value) {
                sortedValues[classes++] = value;
            }
        }

        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            rank[i] = Arrays.binarySearch(sortedValues, 0, classes, values[i]);
        }

        int[] suffixes = new int[n];
        int[] temporary = new int[n];
        int[] count = new int[Math.max(n + 1, classes + 1)];
        for (int value : rank) count[value]++;
        for (int i = 1; i < classes; i++) count[i] += count[i - 1];
        for (int i = n - 1; i >= 0; i--) suffixes[--count[rank[i]]] = i;

        int[] nextRank = new int[n];
        int width = 1;
        while (width < n && classes < n) {
            countingSort(suffixes, temporary, rank, width, classes, count);
            countingSort(temporary, suffixes, rank, 0, classes, count);

            int nextClasses = 1;
            nextRank[suffixes[0]] = 0;
            for (int i = 1; i < n; i++) {
                int previous = suffixes[i - 1];
                int current = suffixes[i];
                int previousFirst = rank[previous];
                int currentFirst = rank[current];
                int previousSecond = previous + width < n ? rank[previous + width] : -1;
                int currentSecond = current + width < n ? rank[current + width] : -1;
                if (previousFirst != currentFirst || previousSecond != currentSecond) {
                    nextClasses++;
                }
                nextRank[current] = nextClasses - 1;
            }

            int[] swap = rank;
            rank = nextRank;
            nextRank = swap;
            classes = nextClasses;
            if (width > n / 2) width = n;
            else width <<= 1;
        }
        return suffixes;
    }

    private static void countingSort(
            int[] input,
            int[] output,
            int[] rank,
            int offset,
            int classes,
            int[] count) {
        Arrays.fill(count, 0, classes + 1, 0);
        int n = input.length;
        for (int index : input) {
            int key = index + offset < n ? rank[index + offset] + 1 : 0;
            count[key]++;
        }
        for (int i = 1; i <= classes; i++) count[i] += count[i - 1];
        for (int i = input.length - 1; i >= 0; i--) {
            int index = input[i];
            int key = index + offset < n ? rank[index + offset] + 1 : 0;
            output[--count[key]] = index;
        }
    }

    private static int[] buildLcp(int[] values, int[] suffixes, int[] ranks) {
        int n = values.length;
        if (n < 2) return new int[0];
        int[] lcp = new int[n - 1];
        int common = 0;
        for (int i = 0; i < n; i++) {
            int rank = ranks[i];
            if (rank == n - 1) {
                common = 0;
                continue;
            }
            int other = suffixes[rank + 1];
            while (i + common < n
                    && other + common < n
                    && values[i + common] == values[other + common]) {
                common++;
            }
            lcp[rank] = common;
            if (common != 0) common--;
        }
        return lcp;
    }
}
