// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable rank/select of the lexicographically ordered union of distinct
 * nonempty binary substrings across several documents. Ordering is unsigned.
 * Packed bytes and suffix/cumulative arrays are the only canonical payloads;
 * the generalized suffix-array builder uses primitive, temporary work lanes.
 */
public final class M3MultiRankedByteSubstrings {
    public static final int MAX_DOCUMENTS = 64;
    public static final int MAX_TOTAL_BYTES = 131_072;
    public static final int MAX_BATCH = 32_768;

    private final byte[] packed;
    private final int[] docStarts;
    private final int[] docEnds;
    private final int[] suffixDocuments;
    private final int[] suffixPackedOffsets;
    private final int[] lcp;
    private final long[] cumulative;
    private final long total;

    private M3MultiRankedByteSubstrings(byte[] packed, int[] docStarts, int[] docEnds,
                                         int[] suffixDocuments, int[] suffixPackedOffsets,
                                         int[] lcp, long[] cumulative, long total) {
        this.packed = packed;
        this.docStarts = docStarts;
        this.docEnds = docEnds;
        this.suffixDocuments = suffixDocuments;
        this.suffixPackedOffsets = suffixPackedOffsets;
        this.lcp = lcp;
        this.cumulative = cumulative;
        this.total = total;
    }

    /** Freeze the documents and construct a generalized sentinel-delimited suffix array. */
    public static M3MultiRankedByteSubstrings copyOf(byte[][] sources) {
        Objects.requireNonNull(sources, "sources");
        if (sources.length > MAX_DOCUMENTS)
            throw new IllegalArgumentException("too many documents");
        int bytes = 0;
        for (byte[] document : sources) {
            Objects.requireNonNull(document, "document");
            if (document.length > MAX_TOTAL_BYTES - bytes)
                throw new IllegalArgumentException("total text exceeds maximum length");
            bytes += document.length;
        }
        byte[] packed = new byte[bytes];
        int[] starts = new int[sources.length];
        int[] ends = new int[sources.length];
        int[] symbols = new int[bytes + sources.length];
        int[] endAt = new int[symbols.length];
        int[] docAt = new int[symbols.length];
        int byteCursor = 0;
        int symbolCursor = 0;
        for (int d = 0; d < sources.length; d++) {
            byte[] source = sources[d];
            starts[d] = byteCursor;
            System.arraycopy(source, 0, packed, byteCursor, source.length);
            byteCursor += source.length;
            ends[d] = byteCursor;
            int endSymbol = symbolCursor + source.length;
            for (int i = 0; i < source.length; i++) {
                symbols[symbolCursor] = (source[i] & 0xff) + 1;
                endAt[symbolCursor] = endSymbol;
                docAt[symbolCursor] = d;
                symbolCursor++;
            }
            // Zero is strictly below every mapped unsigned input byte.
            symbols[symbolCursor++] = 0;
        }
        int[] sorted = buildSuffixArray(symbols);
        int[] positions = new int[bytes];
        int[] docIds = new int[bytes];
        int[] packedOffsets = new int[bytes];
        int[] inverse = new int[symbols.length];
        Arrays.fill(inverse, -1);
        int used = 0;
        for (int position : sorted) {
            if (symbols[position] == 0) continue;
            int doc = docAt[position];
            positions[used] = position;
            docIds[used] = doc;
            packedOffsets[used] = position - doc;
            inverse[position] = used;
            used++;
        }
        if (used != bytes) throw new AssertionError("generalized suffix cardinality");

        int[] adjacent = new int[bytes];
        int matched = 0;
        for (int pos = 0; pos < symbols.length; pos++) {
            if (symbols[pos] == 0) { matched = 0; continue; }
            int rank = inverse[pos];
            if (rank == 0) { matched = 0; continue; }
            int previous = positions[rank - 1];
            while (pos + matched < endAt[pos] && previous + matched < endAt[previous]
                    && symbols[pos + matched] == symbols[previous + matched]) {
                matched++;
            }
            adjacent[rank] = matched;
            if (matched != 0) matched--;
        }
        long[] prefix = new long[bytes];
        long count = 0;
        for (int rank = 0; rank < bytes; rank++) {
            int remaining = ends[docIds[rank]] - packedOffsets[rank];
            if (adjacent[rank] < 0 || adjacent[rank] > remaining)
                throw new AssertionError("bounded LCP invariant");
            count += remaining - adjacent[rank];
            prefix[rank] = count;
        }
        return new M3MultiRankedByteSubstrings(packed, starts, ends, docIds,
                packedOffsets, adjacent, prefix, count);
    }

    public int documentCount() { return docStarts.length; }
    public int totalBytes() { return packed.length; }
    public long distinctCount() { return total; }

    /** Copy of the k-th distinct nonempty substring, one-based rank. */
    public byte[] select(long rank) {
        int[] span = selectSpan(rank);
        int start = docStarts[span[0]] + span[1];
        return Arrays.copyOfRange(packed, start, start + span[2]);
    }

    /** Fresh (documentIndex, documentRelativeOffset, length), using a valid occurrence. */
    public int[] selectSpan(long rank) {
        int slot = indexOf(rank);
        long preceding = slot == 0 ? 0 : cumulative[slot - 1];
        return new int[] {suffixDocuments[slot],
                suffixPackedOffsets[slot] - docStarts[suffixDocuments[slot]],
                lcp[slot] + (int)(rank - preceding)};
    }

    /** One-based lexicographic rank of the nonempty pattern, or -1 when absent. */
    public long rankOf(byte[] pattern) {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.length == 0 || pattern.length > packed.length) return -1;
        int low = 0;
        int high = cumulative.length;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (compareSuffixPrefix(mid, pattern) < 0) low = mid + 1;
            else high = mid;
        }
        if (low == cumulative.length || compareSuffixPrefix(low, pattern) != 0)
            return -1;
        if (lcp[low] >= pattern.length)
            throw new AssertionError("first matching suffix must introduce prefix");
        return (low == 0 ? 0 : cumulative[low - 1]) + pattern.length - lcp[low];
    }

    /** Detached rank-query triplets in original query order. */
    public int[] selectSpans(long[] ranks) {
        validateBatch(ranks);
        int[] triples = new int[3 * ranks.length];
        for (int i = 0; i < ranks.length; i++) {
            int slot = indexOf(ranks[i]);
            long preceding = slot == 0 ? 0 : cumulative[slot - 1];
            triples[3 * i] = suffixDocuments[slot];
            triples[3 * i + 1] = suffixPackedOffsets[slot] - docStarts[suffixDocuments[slot]];
            triples[3 * i + 2] = lcp[slot] + (int)(ranks[i] - preceding);
        }
        return triples;
    }

    void validateBatch(long[] ranks) {
        Objects.requireNonNull(ranks, "ranks");
        if (ranks.length > MAX_BATCH)
            throw new IllegalArgumentException("rank batch too large");
        for (long rank : ranks) validateRank(rank);
    }

    /* JNI export boundaries copy canonical source-owned arrays. */
    int[] suffixDocumentCopy() { return suffixDocuments.clone(); }
    int[] suffixOffsetCopy() { return suffixPackedOffsets.clone(); }
    int[] documentStartCopy() { return docStarts.clone(); }
    int[] lcpCopy() { return lcp.clone(); }
    long[] cumulativeCopy() { return cumulative.clone(); }

    private int indexOf(long rank) {
        validateRank(rank);
        int low = 0, high = cumulative.length;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (cumulative[middle] < rank) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private void validateRank(long rank) {
        if (rank < 1 || rank > total)
            throw new IllegalArgumentException("rank out of range");
    }

    private int compareSuffixPrefix(int rank, byte[] pattern) {
        int start = suffixPackedOffsets[rank];
        int length = docEnds[suffixDocuments[rank]] - start;
        for (int i = 0; i < pattern.length; i++) {
            if (i == length) return -1;
            int a = packed[start + i] & 0xff;
            int b = pattern[i] & 0xff;
            if (a != b) return Integer.compare(a, b);
        }
        return 0;
    }

    private static int[] buildSuffixArray(int[] symbols) {
        int n = symbols.length;
        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;
        if (n <= 1) return order;
        int[] rank = new int[n];
        int[] nextRank = new int[n];
        int[] temporary = new int[n];
        int[] counts = new int[Math.max(258, n + 1)];
        for (int i = 0; i < n; i++) rank[i] = symbols[i] + 1;
        int maxRank = 257;
        for (int width = 1; width < n; width <<= 1) {
            stableRadixPass(order, temporary, rank, width, n, maxRank, true, counts);
            stableRadixPass(temporary, order, rank, width, n, maxRank, false, counts);
            int classes = 1;
            nextRank[order[0]] = classes;
            for (int i = 1; i < n; i++) {
                int a = order[i - 1], b = order[i];
                int secondA = a + width < n ? rank[a + width] : 0;
                int secondB = b + width < n ? rank[b + width] : 0;
                if (rank[a] != rank[b] || secondA != secondB) classes++;
                nextRank[b] = classes;
            }
            int[] swap = rank; rank = nextRank; nextRank = swap;
            maxRank = classes;
            if (classes == n) break;
        }
        return order;
    }

    private static void stableRadixPass(int[] in, int[] out, int[] rank,
                                        int width, int n, int maxRank,
                                        boolean second, int[] counts) {
        Arrays.fill(counts, 0, maxRank + 1, 0);
        for (int pos : in) {
            int key = second ? (pos + width < n ? rank[pos + width] : 0) : rank[pos];
            counts[key]++;
        }
        int consumed = 0;
        for (int key = 0; key <= maxRank; key++) {
            int count = counts[key]; counts[key] = consumed; consumed += count;
        }
        for (int pos : in) {
            int key = second ? (pos + width < n ? rank[pos + width] : 0) : rank[pos];
            out[counts[key]++] = pos;
        }
    }
}
