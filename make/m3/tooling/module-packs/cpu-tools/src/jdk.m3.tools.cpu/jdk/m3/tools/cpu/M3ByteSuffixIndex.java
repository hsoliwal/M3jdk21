// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Immutable unsigned-byte suffix index, built from a single defensively copied lane.
 * Sorting, longest-common-prefix, and suffix-rank minima use primitive arrays only;
 * no per-suffix objects, retained caller arrays, native dependencies, or global caches.
 */
public final class M3ByteSuffixIndex {
    public static final int MAX_TEXT = 131_072;
    public static final int MAX_PATTERN_LENGTH = 4096;
    public static final int MAX_BATCH = 8192;
    public static final int MAX_PACKED_BYTES = 2_000_000;

    private final byte[] text;
    private final int[] suffixes;
    private final int[] minTree;
    private final int treeBase;
    private final long distinctSubstrings;
    // Three arrays (text, suffixes, rank-minimum tree) are leased per JNI call.
    // A future DOOP pool must never recycle this index with a nonzero count.
    private volatile int activeNativeArrayBorrows;
    private static final AtomicIntegerFieldUpdater<M3ByteSuffixIndex> NATIVE_BORROWS =
            AtomicIntegerFieldUpdater.newUpdater(M3ByteSuffixIndex.class,
                    "activeNativeArrayBorrows");

    private M3ByteSuffixIndex(byte[] text, int[] suffixes, int[] minTree,
                              int treeBase, long distinctSubstrings) {
        this.text = text;
        this.suffixes = suffixes;
        this.minTree = minTree;
        this.treeBase = treeBase;
        this.distinctSubstrings = distinctSubstrings;
    }

    /** Freeze a binary input and construct its ascending lexicographic suffix offsets. */
    public static M3ByteSuffixIndex copyOf(byte[] source) {
        Objects.requireNonNull(source, "source");
        if (source.length > MAX_TEXT) {
            throw new IllegalArgumentException("text exceeds maximum length");
        }
        byte[] owned = source.clone();
        int[] suffixes = buildSuffixArray(owned);
        int base = 1;
        while (base < suffixes.length) base <<= 1;
        int[] minTree = new int[base << 1];
        Arrays.fill(minTree, Integer.MAX_VALUE);
        for (int i = 0; i < suffixes.length; i++) minTree[base + i] = suffixes[i];
        for (int i = base - 1; i != 0; i--) {
            minTree[i] = Math.min(minTree[i << 1], minTree[(i << 1) + 1]);
        }
        return new M3ByteSuffixIndex(owned, suffixes, minTree, base,
                distinctSubstringTotal(owned, suffixes));
    }

    public int size() { return text.length; }

    /** Detached input snapshot. */
    public byte[] bytes() { return text.clone(); }

    /** Detached lexicographically ordered offsets of nonempty suffixes. */
    public int[] suffixArray() { return suffixes.clone(); }

    /** Exact number of distinct nonempty binary substrings of this one input. */
    public long distinctSubstringCount() { return distinctSubstrings; }

    /** Number of overlapping byte-pattern matches, including n+1 empty-pattern boundaries. */
    public int count(byte[] pattern) {
        validatePattern(pattern);
        if (pattern.length == 0) return text.length + 1;
        int begin = lowerBound(pattern, 0, pattern.length);
        // For a miss there is no suffix interval to bound on the right.
        return matchesAtLowerBound(begin, pattern, 0, pattern.length)
                ? upperBound(pattern, 0, pattern.length) - begin : 0;
    }

    /** Lowest input byte offset matching the pattern, or -1; empty pattern starts at 0. */
    public int first(byte[] pattern) {
        validatePattern(pattern);
        if (pattern.length == 0) return 0;
        int first = lowerBound(pattern, 0, pattern.length);
        if (!matchesAtLowerBound(first, pattern, 0, pattern.length)) return -1;
        int end = upperBound(pattern, 0, pattern.length);
        return minSuffixOffset(first, end);
    }

    /** Every match location in ascending text-offset order; never aliases index storage. */
    public int[] positions(byte[] pattern) {
        validatePattern(pattern);
        if (pattern.length == 0) {
            int[] all = new int[text.length + 1];
            for (int i = 0; i < all.length; i++) all[i] = i;
            return all;
        }
        int begin = lowerBound(pattern, 0, pattern.length);
        if (!matchesAtLowerBound(begin, pattern, 0, pattern.length)) return new int[0];
        int end = upperBound(pattern, 0, pattern.length);
        int[] answer = Arrays.copyOfRange(suffixes, begin, end);
        Arrays.sort(answer);
        return answer;
    }

    /**
     * Packed queries emit (count, lowest-text-offset) pairs, in supplied query order.
     * An empty query matches every boundary and therefore returns (size+1,0).
     */
    public int[] queryBatch(byte[] packed, int[] offsets) {
        int count = validateBatch(packed, offsets);
        int[] answer = new int[count << 1];
        for (int q = 0; q < count; q++) {
            int from = offsets[q];
            int len = offsets[q + 1] - from;
            if (len == 0) {
                answer[q << 1] = text.length + 1;
                answer[(q << 1) + 1] = 0;
            } else {
                int begin = lowerBound(packed, from, len);
                if (!matchesAtLowerBound(begin, packed, from, len)) {
                    answer[q << 1] = 0;
                    answer[(q << 1) + 1] = -1;
                    continue;
                }
                int end = upperBound(packed, from, len);
                answer[q << 1] = end - begin;
                answer[(q << 1) + 1] = minSuffixOffset(begin, end);
            }
        }
        return answer;
    }

    static int validateBatch(byte[] packed, int[] offsets) {
        Objects.requireNonNull(packed, "packed");
        Objects.requireNonNull(offsets, "offsets");
        if (packed.length > MAX_PACKED_BYTES || offsets.length == 0
                || offsets.length - 1 > MAX_BATCH) {
            throw new IllegalArgumentException("invalid packed query batch");
        }
        if (offsets[0] != 0 || offsets[offsets.length - 1] != packed.length) {
            throw new IllegalArgumentException("offsets must cover packed input");
        }
        for (int i = 1; i < offsets.length; i++) {
            int len = offsets[i] - offsets[i - 1];
            if (len < 0 || len > MAX_PATTERN_LENGTH) {
                throw new IllegalArgumentException("invalid query length");
            }
        }
        return offsets.length - 1;
    }

    private static void validatePattern(byte[] pattern) {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.length > MAX_PATTERN_LENGTH) {
            throw new IllegalArgumentException("pattern too long");
        }
    }

    /**
     * Borrow immutable index arrays for one synchronous native invocation.
     * The callee must not retain, modify, recycle, pool or expose them after
     * return. This index remains their strong Java owner while JNI runs.
     * The C kernel uses Get...ArrayElements and releases input borrows with
     * JNI_ABORT on all success/error paths. This is not a JNI critical region.
     */
    @FunctionalInterface
    interface NativeBatchQuery {
        int[] invoke(byte[] ownerText, int[] ownerSuffixes, int[] rankMinimumTree,
                     int leafBase, byte[] packed, int[] offsets);
    }

    /** Number of primitive owner-array exposures currently in synchronous use. */
    int activeNativeArrayBorrowCount() {
        return activeNativeArrayBorrows;
    }

    int[] withBorrowedNativeState(NativeBatchQuery nativeCall,
                                  byte[] packed, int[] offsets) {
        Objects.requireNonNull(nativeCall, "nativeCall");
        // Compact atomic field rather than an AtomicInteger allocated per index.
        // Reserve exactly three array borrows; nested/concurrent calls compose.
        int observed;
        do {
            observed = NATIVE_BORROWS.get(this);
            if (observed > Integer.MAX_VALUE - 3) {
                throw new IllegalStateException("native array borrow count exceeded");
            }
        } while (!NATIVE_BORROWS.compareAndSet(this, observed, observed + 3));
        try {
            return nativeCall.invoke(text, suffixes, minTree, treeBase, packed, offsets);
        } finally {
            NATIVE_BORROWS.addAndGet(this, -3);
        }
    }

    /* Package-private copy exports for the optional JNI facade only. */
    int[] minTreeCopy() { return minTree.clone(); }
    int treeBase() { return treeBase; }

    private int minSuffixOffset(int low, int high) {
        low += treeBase;
        high += treeBase;
        int minimum = Integer.MAX_VALUE;
        while (low < high) {
            if ((low & 1) != 0) minimum = Math.min(minimum, minTree[low++]);
            if ((high & 1) != 0) minimum = Math.min(minimum, minTree[--high]);
            low >>>= 1;
            high >>>= 1;
        }
        return minimum;
    }

    /** The comparison is unsigned and treats a suffix prefix match as equality. */
    private int compareSuffix(int offset, byte[] pattern, int from, int len) {
        for (int i = 0; i < len; i++) {
            if (offset + i == text.length) return -1;
            int a = text[offset + i] & 0xff;
            int b = pattern[from + i] & 0xff;
            if (a != b) return Integer.compare(a, b);
        }
        return 0;
    }

    private int lowerBound(byte[] pattern, int from, int len) {
        int low = 0;
        int high = suffixes.length;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (compareSuffix(suffixes[mid], pattern, from, len) < 0) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    /** A lower bound identifies a hit only when its suffix has the exact prefix. */
    private boolean matchesAtLowerBound(int candidate, byte[] pattern, int from, int len) {
        return candidate < suffixes.length
                && compareSuffix(suffixes[candidate], pattern, from, len) == 0;
    }

    private int upperBound(byte[] pattern, int from, int len) {
        int low = 0;
        int high = suffixes.length;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (compareSuffix(suffixes[mid], pattern, from, len) <= 0) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    /** Construct suffix offsets with radix-sorted doubling ranks, without boxing. */
    private static int[] buildSuffixArray(byte[] source) {
        int n = source.length;
        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;
        if (n <= 1) return order;
        int[] rank = new int[n];
        int[] nextRank = new int[n];
        int[] temporary = new int[n];
        int[] counts = new int[Math.max(257, n + 1)];
        for (int i = 0; i < n; i++) rank[i] = (source[i] & 0xff) + 1;
        int maxRank = 256;
        for (int width = 1; width < n; width <<= 1) {
            stableRadixPass(order, temporary, rank, width, n, maxRank, true, counts);
            stableRadixPass(temporary, order, rank, width, n, maxRank, false, counts);
            int classes = 1;
            nextRank[order[0]] = classes;
            for (int j = 1; j < n; j++) {
                int left = order[j - 1];
                int right = order[j];
                int secondLeft = left + width < n ? rank[left + width] : 0;
                int secondRight = right + width < n ? rank[right + width] : 0;
                if (rank[left] != rank[right] || secondLeft != secondRight) classes++;
                nextRank[right] = classes;
            }
            int[] swap = rank;
            rank = nextRank;
            nextRank = swap;
            maxRank = classes;
            if (classes == n) break;
        }
        return order;
    }

    /** A stable counting pass over rank 0..maxRank, with zero reserved for the sentinel. */
    private static void stableRadixPass(int[] input, int[] output, int[] rank,
                                        int width, int n, int maxRank,
                                        boolean second, int[] counts) {
        Arrays.fill(counts, 0, maxRank + 1, 0);
        for (int position : input) {
            int key = second ? (position + width < n ? rank[position + width] : 0)
                    : rank[position];
            counts[key]++;
        }
        int consumed = 0;
        for (int key = 0; key <= maxRank; key++) {
            int size = counts[key];
            counts[key] = consumed;
            consumed += size;
        }
        for (int position : input) {
            int key = second ? (position + width < n ? rank[position + width] : 0)
                    : rank[position];
            output[counts[key]++] = position;
        }
    }

    /** Kasai adjacent-suffix LCP accumulation without temporary string objects. */
    private static long distinctSubstringTotal(byte[] source, int[] order) {
        int n = order.length;
        long total = ((long) n * (n + 1)) / 2;
        int[] inverse = new int[n];
        for (int r = 0; r < n; r++) inverse[order[r]] = r;
        int matched = 0;
        for (int i = 0; i < n; i++) {
            int r = inverse[i];
            if (r == n - 1) { matched = 0; continue; }
            int j = order[r + 1];
            while (i + matched < n && j + matched < n
                    && source[i + matched] == source[j + matched]) matched++;
            total -= matched;
            if (matched > 0) matched--;
        }
        return total;
    }
}
