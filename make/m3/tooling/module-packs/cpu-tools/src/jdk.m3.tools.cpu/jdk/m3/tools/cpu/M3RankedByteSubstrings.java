// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable lexicographic rank/select over distinct nonempty substrings of one binary text.
 * All payloads use primitive arrays; no per-substring Entry/Node/Cell survives construction.
 * Ranks are one-based, byte comparison is unsigned, and selection results are detached.
 */
public final class M3RankedByteSubstrings {
    public static final int MAX_BATCH = 32_768;

    private final byte[] text;
    private final int[] suffixes;
    private final int[] lcp;
    private final long[] cumulative;
    private final long total;

    private M3RankedByteSubstrings(byte[] text, int[] suffixes, int[] lcp,
                                   long[] cumulative, long total) {
        this.text = text;
        this.suffixes = suffixes;
        this.lcp = lcp;
        this.cumulative = cumulative;
        this.total = total;
    }

    /** Freeze caller bytes and reuse the established Wave29 suffix-index ordering. */
    public static M3RankedByteSubstrings copyOf(byte[] source) {
        M3ByteSuffixIndex owner = M3ByteSuffixIndex.copyOf(source);
        byte[] text = owner.bytes();
        int[] suffixes = owner.suffixArray();
        int n = text.length;
        int[] adjacent = new int[n];
        int[] inverse = new int[n];
        for (int i = 0; i < n; i++) inverse[suffixes[i]] = i;
        int matched = 0;
        for (int i = 0; i < n; i++) {
            int rank = inverse[i];
            if (rank == 0) { matched = 0; continue; }
            int previous = suffixes[rank - 1];
            while (i + matched < n && previous + matched < n
                    && text[i + matched] == text[previous + matched]) matched++;
            adjacent[rank] = matched;
            if (matched > 0) matched--;
        }
        long[] prefix = new long[n];
        long total = 0;
        for (int i = 0; i < n; i++) {
            total += n - suffixes[i] - adjacent[i];
            prefix[i] = total;
        }
        if (total != owner.distinctSubstringCount()) {
            throw new AssertionError("suffix-array distinct-count invariant");
        }
        return new M3RankedByteSubstrings(text, suffixes, adjacent, prefix, total);
    }

    public int size() { return text.length; }
    public long distinctCount() { return total; }

    /** One existing text occurrence as (offset,length), not necessarily first text offset. */
    public int[] selectSpan(long rank) {
        int index = selectIndex(rank);
        long previous = index == 0 ? 0 : cumulative[index - 1];
        int length = lcp[index] + (int)(rank - previous);
        return new int[] {suffixes[index], length};
    }

    /** A caller-owned byte array containing the k-th distinct nonempty binary substring. */
    public byte[] select(long rank) {
        int[] span = selectSpan(rank);
        return Arrays.copyOfRange(text, span[0], span[0] + span[1]);
    }

    /** Returns the 1-based lexicographic distinct rank, or -1 if no such nonempty string. */
    public long rankOf(byte[] pattern) {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.length == 0 || pattern.length > text.length) return -1;
        int lo = 0, hi = suffixes.length;
        while (lo < hi) {
            int mid = lo + ((hi - lo) >>> 1);
            if (compareSuffixPrefix(suffixes[mid], pattern) < 0) lo = mid + 1;
            else hi = mid;
        }
        if (lo == suffixes.length || compareSuffixPrefix(suffixes[lo], pattern) != 0)
            return -1;
        long previous = lo == 0 ? 0 : cumulative[lo - 1];
        if (pattern.length <= lcp[lo]) {
            throw new AssertionError("first matching suffix must introduce this prefix");
        }
        return previous + pattern.length - lcp[lo];
    }

    /** Batch of detached (offset,length) spans in the original rank query order. */
    public int[] selectSpans(long[] ranks) {
        validateBatch(ranks);
        int[] output = new int[ranks.length * 2];
        for (int i = 0; i < ranks.length; i++) {
            int index = selectIndex(ranks[i]);
            long previous = index == 0 ? 0 : cumulative[index - 1];
            output[i * 2] = suffixes[index];
            output[i * 2 + 1] = lcp[index] + (int)(ranks[i] - previous);
        }
        return output;
    }

    void validateBatch(long[] ranks) {
        Objects.requireNonNull(ranks, "ranks");
        if (ranks.length > MAX_BATCH) throw new IllegalArgumentException("rank batch too large");
        for (long rank : ranks) validateRank(rank);
    }

    private void validateRank(long rank) {
        if (rank < 1 || rank > total) throw new IllegalArgumentException("rank out of range");
    }

    private int selectIndex(long rank) {
        validateRank(rank);
        int low = 0, high = cumulative.length;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (cumulative[middle] < rank) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private int compareSuffixPrefix(int offset, byte[] pattern) {
        for (int i = 0; i < pattern.length; i++) {
            if (offset + i == text.length) return -1;
            int a = text[offset + i] & 0xff;
            int b = pattern[i] & 0xff;
            if (a != b) return Integer.compare(a, b);
        }
        return 0;
    }

    /* JNI facade receives fresh copies only, never direct references to canonical lanes. */
    int[] suffixCopy() { return suffixes.clone(); }
    int[] lcpCopy() { return lcp.clone(); }
    long[] cumulativeCopy() { return cumulative.clone(); }
}
