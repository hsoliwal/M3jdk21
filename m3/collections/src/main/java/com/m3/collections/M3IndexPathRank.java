/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * M3 indexed-path rank/select extension: recipe-generated target candidate.
 * Modified 2026: adaptive sparse rank lookup and dense bitmap-word enumeration.
 */
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
    private final int[] rows;
    private final long[] selected;
    private final int[] wordPrefix;

    private M3IndexPathRank(int[] rows, long[] selected, int[] wordPrefix) {
        this.rows = rows;
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
        int[] prefix = new int[words.length + 1];
        for (int word = 0; word < words.length; word++) {
            prefix[word + 1] = prefix[word] + Long.bitCount(words[word]);
        }
        return new M3IndexPathRank(copy, words, prefix);
    }

    /** Number of captured input occurrences, including filtered-out occurrences. */
    public int sourceSize() { return rows.length; }

    /** Number of admitted occurrences, including repeated row IDs. */
    public int size() { return wordPrefix[wordPrefix.length - 1]; }

    /** Is the given source ordinal admitted? */
    public boolean containsSourceOrdinal(int ordinal) {
        Objects.checkIndex(ordinal, rows.length);
        return (selected[ordinal >>> 6] & (1L << (ordinal & 63))) != 0;
    }

    /** Exclusive occurrence rank among the first {@code endExclusive} path positions. */
    public int rank(int endExclusive) {
        Objects.checkFromToIndex(0, endExclusive, rows.length);
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
    public int selectedRowId(int rank) { return rows[selectedSourceOrdinal(rank)]; }

    /** A defensive, exact-length array containing only selected row IDs. */
    public int[] toArray() { return window(0, sourceSize()).toArray(); }

    /** Zero-copy (of payload and index) half-open source-ordinal window. */
    public Window window(int fromInclusive, int toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, rows.length);
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

        public int[] toArray() {
            int[] result = new int[size()];
            if (result.length == 0) return result;
            int firstWord = from >>> 6;
            int lastWord = (to - 1) >>> 6;
            // Reuse rank/select for sparse windows instead of walking empty words.
            // Each select needs at most this binary-search depth plus 63 bit clears.
            // The long product cannot overflow even for the largest Java array.
            int selectWork = 64 + Integer.SIZE
                    - Integer.numberOfLeadingZeros(owner.selected.length);
            if ((long) lastWord - firstWord + 1 > (long) result.length * selectWork) {
                for (int rank = 0; rank < result.length; rank++) {
                    result[rank] = owner.selectedRowId(firstRank + rank);
                }
                return result;
            }
            int write = 0;
            for (int wordIndex = firstWord; wordIndex <= lastWord; wordIndex++) {
                long bits = owner.selected[wordIndex];
                if (wordIndex == firstWord) bits &= -1L << (from & 63);
                if (wordIndex == lastWord && (to & 63) != 0) {
                    bits &= (1L << (to & 63)) - 1L;
                }
                while (bits != 0) {
                    int sourceOrdinal = (wordIndex << 6) + Long.numberOfTrailingZeros(bits);
                    result[write++] = owner.rows[sourceOrdinal];
                    bits &= bits - 1;
                }
            }
            return result;
        }
    }
}
