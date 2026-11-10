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
package jdk.m3.collections.text;

import java.util.Arrays;
import java.util.Objects;

/**
 * Primitive suffix array and Kasai LCP over arbitrary signed int tokens.
 *
 * <p>Construction uses rank-doubling with stable counting passes: O(n log n) time and O(n)
 * primitive workspace, with no suffix objects or boxed comparators retained.
 * Two flat LCP boundary lanes precompute the implicit binary-search tree in O(n) time.
 * Prefix-range lookup then uses O(pattern length + log n) token comparisons, rather than
 * restarting the same prefix at each midpoint. No spelling, alternate interner or query cache
 * is introduced; inputs remain integer coordinates in the caller's chosen alphabet.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/IntSuffixArray.java}
 * blob {@code ac64fa5286fc5ac109247bd7292abda4ca252c65} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IntSuffixArray {
    public record Range(int fromRankInclusive, int toRankExclusive) {
        public Range {
            if (fromRankInclusive < 0 || toRankExclusive < fromRankInclusive) {
                throw new IllegalArgumentException("invalid suffix range");
            }
        }

        public int size() {
            return toRankExclusive - fromRankInclusive;
        }

        public boolean isEmpty() {
            return fromRankInclusive == toRankExclusive;
        }
    }

    private final int[] tokens;
    private final int[] suffixAtRank;
    private final int[] rankOfSuffix;
    private final int[] lcpAtRank;
    private final int[] lcpToLeftBoundary;
    private final int[] lcpToRightBoundary;

    public IntSuffixArray(int[] source) {
        Objects.requireNonNull(source, "source");
        tokens = source.clone();
        int length = tokens.length;
        suffixAtRank = new int[length];
        rankOfSuffix = new int[length];
        lcpAtRank = new int[length];
        lcpToLeftBoundary = new int[length];
        lcpToRightBoundary = new int[length];
        if (length == 0) {
            return;
        }

        int[] sortedTokens = tokens.clone();
        Arrays.sort(sortedTokens);
        int unique = 0;
        for (int value : sortedTokens) {
            if (unique == 0 || value != sortedTokens[unique - 1]) {
                sortedTokens[unique++] = value;
            }
        }

        int[] ranks = rankOfSuffix;
        for (int suffix = 0; suffix < length; suffix++) {
            ranks[suffix] = Arrays.binarySearch(
                    sortedTokens, 0, unique, tokens[suffix]);
            suffixAtRank[suffix] = suffix;
        }

        // Reuse construction workspace for the final search metadata; no additional peak lanes.
        int[] scratch = lcpToLeftBoundary;
        int[] nextRanks = lcpToRightBoundary;
        int[] counts = new int[Math.addExact(length, 1)];
        int classes = unique;

        // Even when every first token is unique, suffixAtRank must be sorted by that rank.
        countingSort(suffixAtRank, scratch, ranks, 0, classes, counts);
        System.arraycopy(scratch, 0, suffixAtRank, 0, length);

        int step = 1;
        while (step < length && classes < length) {
            countingSort(
                    suffixAtRank, scratch, ranks,
                    step, classes, counts);
            countingSort(
                    scratch, suffixAtRank, ranks,
                    0, classes, counts);

            int nextClasses = 1;
            nextRanks[suffixAtRank[0]] = 0;
            for (int rank = 1; rank < length; rank++) {
                int previous = suffixAtRank[rank - 1];
                int current = suffixAtRank[rank];
                if (!samePair(previous, current, step, ranks)) {
                    nextClasses++;
                }
                nextRanks[current] = nextClasses - 1;
            }

            int[] rankSwap = ranks;
            ranks = nextRanks;
            nextRanks = rankSwap;
            classes = nextClasses;

            if (step > length / 2) {
                break;
            }
            step <<= 1;
        }

        if (ranks != rankOfSuffix) {
            System.arraycopy(ranks, 0, rankOfSuffix, 0, length);
        }
        // Rebuild from suffix order as a final representation invariant.
        for (int rank = 0; rank < length; rank++) {
            rankOfSuffix[suffixAtRank[rank]] = rank;
        }

        int common = 0;
        for (int suffix = 0; suffix < length; suffix++) {
            int rank = rankOfSuffix[suffix];
            if (rank == 0) {
                continue;
            }
            int previousSuffix = suffixAtRank[rank - 1];
            while (suffix + common < length
                    && previousSuffix + common < length
                    && tokens[suffix + common]
                    == tokens[previousSuffix + common]) {
                common++;
            }
            lcpAtRank[rank] = common;
            if (common != 0) {
                common--;
            }
        }
        precomputeLcpBoundaries(-1, length);
    }

    public int length() {
        return tokens.length;
    }

    public int tokenAt(int index) {
        return tokens[Objects.checkIndex(index, tokens.length)];
    }

    public int suffixAtRank(int rank) {
        return suffixAtRank[Objects.checkIndex(rank, suffixAtRank.length)];
    }

    public int rankOfSuffix(int suffix) {
        return rankOfSuffix[Objects.checkIndex(suffix, rankOfSuffix.length)];
    }

    /** LCP with the preceding suffix rank; rank zero has LCP zero. */
    public int lcpAtRank(int rank) {
        return lcpAtRank[Objects.checkIndex(rank, lcpAtRank.length)];
    }

    public int[] suffixes() {
        return suffixAtRank.clone();
    }

    public int[] ranks() {
        return rankOfSuffix.clone();
    }

    public int[] lcp() {
        return lcpAtRank.clone();
    }

    public Range prefixRange(int[] pattern) {
        Objects.requireNonNull(pattern, "pattern");
        int low = lowerBound(pattern, false);
        int high = lowerBound(pattern, true);
        return new Range(low, high);
    }

    public boolean contains(int[] pattern) {
        return !prefixRange(pattern).isEmpty();
    }

    public long payloadBytes() {
        return Integer.BYTES * ((long) tokens.length + suffixAtRank.length
                + rankOfSuffix.length + lcpAtRank.length
                + lcpToLeftBoundary.length + lcpToRightBoundary.length);
    }

    /**
     * Precompute precisely the midpoint tree used by lowerBound. Each edge interval contributes
     * its minimum adjacent LCP once; virtual boundary ranks -1 and length have common length zero.
     * Recursion depth is logarithmic, independent of token repetition or individual word length.
     */
    private int precomputeLcpBoundaries(int low, int high) {
        if (low + 1 == high) {
            return low < 0 || high == length() ? 0 : lcpAtRank[high];
        }
        int middle = (low + high) >>> 1;
        int left = precomputeLcpBoundaries(low, middle);
        int right = precomputeLcpBoundaries(middle, high);
        lcpToLeftBoundary[middle] = left;
        lcpToRightBoundary[middle] = right;
        return Math.min(left, right);
    }

    private int lowerBound(int[] pattern, boolean afterEqualPrefix) {
        int low = -1;
        int high = suffixAtRank.length;
        int leftMatched = 0;
        int rightMatched = 0;
        while (low + 1 < high) {
            int middle = (low + high) >>> 1;
            boolean useLeft = leftMatched >= rightMatched;
            int known = useLeft ? leftMatched : rightMatched;
            int shared = useLeft ? lcpToLeftBoundary[middle] : lcpToRightBoundary[middle];
            int compared;
            int matched;
            if (shared < known) {
                // The midpoint diverges before the boundary's known match with the query.
                compared = useLeft ? 1 : -1;
                matched = shared;
            } else if (shared > known) {
                // It shares the boundary's first mismatch, hence also that boundary's side.
                if (useLeft) low = middle;
                else high = middle;
                continue;
            } else {
                long comparison = compareSuffixFrom(suffixAtRank[middle], pattern, known);
                compared = (int) comparison;
                matched = (int) (comparison >>> Integer.SIZE);
            }
            boolean advance = afterEqualPrefix ? compared <= 0 : compared < 0;
            if (advance) {
                low = middle;
                leftMatched = matched;
            } else {
                high = middle;
                rightMatched = matched;
            }
        }
        return high;
    }

    /** Pack exact matched length and signed comparison into a primitive result, with no object. */
    private long compareSuffixFrom(int suffix, int[] pattern, int matched) {
        int available = tokens.length - suffix;
        int limit = Math.min(available, pattern.length);
        while (matched < limit) {
            int compared = Integer.compare(tokens[suffix + matched], pattern[matched]);
            if (compared != 0) return packComparison(matched, compared);
            matched++;
        }
        return packComparison(matched, pattern.length <= available ? 0 : -1);
    }

    private static long packComparison(int matched, int compared) {
        return ((long) matched << Integer.SIZE) | Integer.toUnsignedLong(compared);
    }

    private static boolean samePair(
            int first,
            int second,
            int step,
            int[] ranks) {
        if (ranks[first] != ranks[second]) {
            return false;
        }
        int firstSecond = first + step < ranks.length
                ? ranks[first + step] : -1;
        int secondSecond = second + step < ranks.length
                ? ranks[second + step] : -1;
        return firstSecond == secondSecond;
    }

    private static void countingSort(
            int[] input,
            int[] output,
            int[] ranks,
            int offset,
            int classes,
            int[] counts) {
        int keys = classes + 1;
        Arrays.fill(counts, 0, keys, 0);
        for (int suffix : input) {
            counts[key(suffix, offset, ranks)]++;
        }
        int total = 0;
        for (int key = 0; key < keys; key++) {
            int count = counts[key];
            counts[key] = total;
            total += count;
        }
        for (int suffix : input) {
            int key = key(suffix, offset, ranks);
            output[counts[key]++] = suffix;
        }
    }

    private static int key(int suffix, int offset, int[] ranks) {
        int at = suffix + offset;
        return at < ranks.length ? ranks[at] + 1 : 0;
    }
}
