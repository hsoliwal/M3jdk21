/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * M3 indexed-path rank/select extension: recipe-generated target candidate.
 */
// Modified 2026 by Hitesh Soliwal and contributors: keep the bulk row-copy
// helper package-private so the existing public API remains unchanged.
package com.m3.collections;

import java.util.Objects;

/**
 * Immutable occurrence-rank index for a strided, mask-filtered primitive row path.
 *
 * <p>The caller owns parallel {@code orderedRows} and {@code physicalOffsets} arrays;
 * each physical offset addresses every aligned {@code statePlanes} array. Every
 * stage requires all bits in its 32-bit required mask and rejects any forbidden
 * bit. Stage evaluation short-circuits from left to right. Row IDs may be
 * negative or repeated: membership and rank are by path <em>occurrence</em>,
 * never by row ID or payload equality. Capture copies only the traversed int
 * row path; values/objects stay in their original owners. Input arrays must
 * remain stable throughout capture; later changes cannot alter this snapshot.
 *
 * <p>Selected occurrences occupy one packed {@code long[]} bitmap and a
 * prefix-count {@code int[]} per 64-position word. Build costs O(n * stages),
 * rank costs O(1), select costs O(log(n/64) + 64), and a source-ordinal window
 * shares the immutable arrays. No per-row entries or retained JNI pointers.
 * This detached index is not a live JDK Set/List/Map view, an object-lifetime
 * owner, a stable generation check, or a concurrency/snapshot transaction.
 */
public final class M3IndexPathRank {
    // Null only for the identity-path mode: row ID is the source ordinal.
    private final int[] rows;
    private final int sourceLength;
    private final long[] selected;
    private final int[] wordPrefix;

    private M3IndexPathRank(int[] rows, int sourceLength, long[] selected, int[] wordPrefix) {
        this.rows = rows;
        this.sourceLength = sourceLength;
        this.selected = selected;
        this.wordPrefix = wordPrefix;
    }

    /**
     * Capture in encounter order through {@code first + ordinal * stride}.
     * Validates all geometry and physical positions before allocating the
     * private snapshot. A zero-count path allows first==orderedRows.length.
     * Overlapping required/forbidden bits simply select no matching entry,
     * exactly as in the existing M3IndexPathMasks source contract.
     */
    public static M3IndexPathRank snapshot(int[] orderedRows, int[] physicalOffsets,
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,
            int first, int count, int stride) {
        Objects.requireNonNull(orderedRows, "orderedRows");
        Objects.requireNonNull(physicalOffsets, "physicalOffsets");
        Objects.requireNonNull(statePlanes, "statePlanes");
        Objects.requireNonNull(requiredMasks, "requiredMasks");
        Objects.requireNonNull(forbiddenMasks, "forbiddenMasks");
        if (orderedRows.length != physicalOffsets.length
                || statePlanes.length == 0
                || statePlanes.length != requiredMasks.length
                || statePlanes.length != forbiddenMasks.length) {
            throw new IllegalArgumentException("unaligned rows, offsets or state mask stages");
        }
        int capacity = Integer.MAX_VALUE;
        for (int i = 0; i < statePlanes.length; i++) {
            capacity = Math.min(capacity,
                    Objects.requireNonNull(statePlanes[i], "statePlanes[" + i + "]").length);
        }
        if (count < 0 || stride == 0 || first < 0 || first > orderedRows.length) {
            throw new IndexOutOfBoundsException("invalid strided path geometry");
        }
        if (count != 0) {
            long last = (long) first + ((long) count - 1) * stride;
            if (first == orderedRows.length || last < 0 || last >= orderedRows.length) {
                throw new IndexOutOfBoundsException("strided path leaves row domain");
            }
        }
        long at = first;
        for (int ordinal = 0; ordinal < count; ordinal++, at += stride) {
            int slot = physicalOffsets[(int) at];
            if (slot < 0 || slot >= capacity) {
                throw new IndexOutOfBoundsException("physical slot outside a state plane");
            }
        }

        int[] copy = new int[count];
        long[] words = new long[(int) (((long) count + 63L) >>> 6)];
        at = first;
        for (int ordinal = 0; ordinal < count; ordinal++, at += stride) {
            int logical = (int) at;
            copy[ordinal] = orderedRows[logical];
            int physical = physicalOffsets[logical];
            boolean keep = true;
            for (int stage = 0; stage < statePlanes.length; stage++) {
                int state = statePlanes[stage][physical];
                if ((state & requiredMasks[stage]) != requiredMasks[stage]
                        || (state & forbiddenMasks[stage]) != 0) {
                    keep = false;
                    break;
                }
            }
            if (keep) words[ordinal >>> 6] |= 1L << (ordinal & 63);
        }
        return new M3IndexPathRank(copy, count, words, prefixCounts(words));
    }

    /**
     * Capture an implicit identity path from an already detached, aligned
     * full-width state lane. No 0..n-1 row IDs, physical offsets or copies
     * of either are retained. Each selected row ID is its source ordinal.
     *
     * <p>Package-private so public collection and JNI contracts stay unchanged.
     * State is read once and not retained; later caller changes cannot alter
     * the captured immutable bitmap and prefix ranks.</p>
     */
    static M3IndexPathRank snapshotIdentity(
            int[] states, int requiredMask, int forbiddenMask) {
        Objects.requireNonNull(states, "states");
        int count = states.length;
        long[] words = new long[(int) (((long) count + 63L) >>> 6)];
        for (int ordinal = 0; ordinal < count; ordinal++) {
            int state = states[ordinal];
            if ((state & requiredMask) == requiredMask
                    && (state & forbiddenMask) == 0) {
                words[ordinal >>> 6] |= 1L << (ordinal & 63);
            }
        }
        return new M3IndexPathRank(null, count, words, prefixCounts(words));
    }

    private static int[] prefixCounts(long[] words) {
        int[] prefix = new int[words.length + 1];
        for (int i = 0; i < words.length; i++) {
            prefix[i + 1] = prefix[i] + Long.bitCount(words[i]);
        }
        return prefix;
    }

    private int rowIdAt(int sourceOrdinal) {
        return rows == null ? sourceOrdinal : rows[sourceOrdinal];
    }

    /** Number of captured input occurrences, including filtered-out occurrences. */
    public int sourceSize() { return sourceLength; }

    /** Number of admitted occurrences, including repeated row IDs. */
    public int size() { return wordPrefix[wordPrefix.length - 1]; }

    /** Is the given source ordinal admitted? */
    public boolean containsSourceOrdinal(int ordinal) {
        Objects.checkIndex(ordinal, sourceLength);
        return (selected[ordinal >>> 6] & (1L << (ordinal & 63))) != 0;
    }

    /** Exclusive occurrence rank among the first {@code endExclusive} path positions. */
    public int rank(int endExclusive) {
        Objects.checkFromToIndex(0, endExclusive, sourceLength);
        int wholeWords = endExclusive >>> 6;
        int result = wordPrefix[wholeWords];
        int residue = endExclusive & 63;
        if (residue != 0) {
            result += Long.bitCount(selected[wholeWords] & ((1L << residue) - 1L));
        }
        return result;
    }

    /** Zero-based selected rank to ordinal in the captured strided source path. */
    public int selectedSourceOrdinal(int rank) {
        Objects.checkIndex(rank, size());
        int low = 0;
        int high = selected.length;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (wordPrefix[middle + 1] <= rank) low = middle + 1;
            else high = middle;
        }
        long word = selected[low];
        int remainder = rank - wordPrefix[low];
        while (remainder-- > 0) word &= word - 1;
        return (low << 6) + Long.numberOfTrailingZeros(word);
    }

    /** Stable signed or duplicate row ID at selected rank; no Entry materialized. */
    public int selectedRowId(int rank) { return rowIdAt(selectedSourceOrdinal(rank)); }

    /** A defensive, exact-length array containing only selected row IDs. */
    public int[] toArray() { return window(0, sourceSize()).toArray(); }

    /** Zero-copy (of payload and index) half-open source-ordinal window. */
    public Window window(int fromInclusive, int toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, sourceLength);
        return new Window(this, fromInclusive, toExclusive);
    }

    /** Immutable half-open source-ordinal range with rank/select relative to its start. */
    public static final class Window {
        private final M3IndexPathRank owner;
        private final int from;
        private final int to;
        private final int firstRank;
        private final int lastRank;

        private Window(M3IndexPathRank owner, int from, int to) {
            this.owner = owner;
            this.from = from;
            this.to = to;
            this.firstRank = owner.rank(from);
            this.lastRank = owner.rank(to);
        }

        public int sourceSize() { return to - from; }
        public int size() { return lastRank - firstRank; }

        /** Exclusive selected rank within this window. */
        public int rank(int relativeExclusive) {
            Objects.checkFromToIndex(0, relativeExclusive, sourceSize());
            return owner.rank(from + relativeExclusive) - firstRank;
        }

        /** Selected rank to this window's relative source ordinal. */
        public int selectedSourceOrdinal(int selectedRank) {
            Objects.checkIndex(selectedRank, size());
            return owner.selectedSourceOrdinal(firstRank + selectedRank) - from;
        }

        public int selectedRowId(int selectedRank) {
            Objects.checkIndex(selectedRank, size());
            return owner.selectedRowId(firstRank + selectedRank);
        }

        public Window window(int relativeFrom, int relativeTo) {
            Objects.checkFromToIndex(relativeFrom, relativeTo, sourceSize());
            return owner.window(from + relativeFrom, from + relativeTo);
        }

        /**
         * Fill an existing primitive output lane with selected row IDs at consecutive
         * <em>selected</em> ranks inside this source-ordinal window.
         *
         * <p>The caller owns the output array; source payloads, predicates and
         * comparator objects are never retained or projected into Entry objects.
         * All rank and output coordinates are validated before any write.
         * A zero-count request at the end of this window is valid. The destination
         * must not alias the immutable rank owner's internal row directory.</p>
         *
         * @param selectedFrom inclusive selected rank relative to this window
         * @param output caller-owned destination int array
         * @param outputOffset first element of the destination window
         * @param count number of consecutive selected row IDs to write
         */
        void copySelectedRowIds(
                int selectedFrom, int[] output, int outputOffset, int count) {
            Objects.requireNonNull(output, "output");
            Objects.checkFromIndexSize(selectedFrom, count, size());
            Objects.checkFromIndexSize(outputOffset, count, output.length);
            if (output == owner.rows) {
                throw new IllegalArgumentException("output aliases source row directory");
            }
            if (count == 0) return;

            // A first-page request is already positioned by the immutable source
            // coordinate. Subsequent pages need only one prefix-rank seek.
            int ordinal = selectedFrom == 0
                    ? from : owner.selectedSourceOrdinal(firstRank + selectedFrom);
            int firstWord = ordinal >>> 6;
            int lastWord = (to - 1) >>> 6;
            int copied = 0;
            for (int wordIndex = firstWord; wordIndex <= lastWord && copied < count;
                    wordIndex++) {
                long bits = owner.selected[wordIndex];
                if (wordIndex == firstWord) bits &= -1L << (ordinal & 63);
                if (wordIndex == lastWord && (to & 63) != 0) {
                    bits &= (1L << (to & 63)) - 1L;
                }
                while (bits != 0 && copied < count) {
                    int sourceOrdinal =
                            (wordIndex << 6) + Long.numberOfTrailingZeros(bits);
                    output[outputOffset + copied++] = owner.rowIdAt(sourceOrdinal);
                    bits &= bits - 1;
                }
            }
            if (copied != count) {
                throw new IllegalStateException("immutable selection rank/bitmap mismatch");
            }
        }

        /** Fresh exact-length array; shares the same bitmap word-walk bulk atom. */
        public int[] toArray() {
            int[] result = new int[size()];
            copySelectedRowIds(0, result, 0, result.length);
            return result;
        }

    }
}
