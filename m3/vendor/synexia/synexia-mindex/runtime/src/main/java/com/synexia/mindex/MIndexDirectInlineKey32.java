// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

/**
 * Sparse raw-address front door for frozen MIndex dictionary admission.
 *
 * <p>The retained geometry is a flattened primitive tensor:
 * {@code [byte0][byte1] -> [exact length 0..32] -> optional byte2..byte7 pages}. A unique terminal
 * row co-locates the lexical-rank candidate and every UTF-8 byte in at most four 64-bit lanes.
 * MISS is authoritative. AMBIGUOUS delegates to the existing collision-free exact lookup.</p>
 */
final class MIndexDirectInlineKey32 {
    static final int MAX_BYTES = 32;
    static final int MAX_PREFIX_BYTES = 8;
    static final int LENGTH_LANES = MAX_BYTES + 1;
    static final int BYTE_RADIX = 256;
    static final int DIRECTORY_SIZE = BYTE_RADIX * BYTE_RADIX;

    static final int AMBIGUOUS = -1;
    static final int MISS = -2;

    private static final int REFINEMENT_BIAS = 2;
    private static final int ROW_LONGS = 5;
    private static final VarHandle BYTE_ARRAY_LONG_BE =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);

    private final int[] baseByCoordinate;
    private final long[] dispatchRows;
    private final long[] refinementRows;
    private final int eligibleRanks;
    private final int directRanks;
    private final int unresolvedAmbiguousRows;

    private MIndexDirectInlineKey32(
            int[] baseByCoordinate,
            long[] dispatchRows,
            long[] refinementRows,
            int eligibleRanks,
            int directRanks,
            int unresolvedAmbiguousRows) {
        this.baseByCoordinate = baseByCoordinate;
        this.dispatchRows = dispatchRows;
        this.refinementRows = refinementRows;
        this.eligibleRanks = eligibleRanks;
        this.directRanks = directRanks;
        this.unresolvedAmbiguousRows = unresolvedAmbiguousRows;
    }

    static MIndexDirectInlineKey32 build(byte[] arena, int[] offsets) {
        Objects.requireNonNull(arena, "arena");
        Objects.requireNonNull(offsets, "offsets");
        if (offsets.length == 0 || offsets[0] != 0 || offsets[offsets.length - 1] != arena.length) {
            throw new IllegalArgumentException("invalid dictionary arena offsets");
        }

        int ranks = offsets.length - 1;
        int[] groupCounts = new int[DIRECTORY_SIZE];
        int eligible = 0;
        for (int rank = 0; rank < ranks; rank++) {
            int start = checkedStart(offsets, rank, arena.length);
            int length = offsets[rank + 1] - start;
            if (length <= MAX_BYTES) {
                groupCounts[coordinate(arena, start, length)]++;
                eligible++;
            }
        }

        int[] baseByCoordinate = new int[DIRECTORY_SIZE];
        int blocks = 0;
        for (int coordinate = 0; coordinate < groupCounts.length; coordinate++) {
            if (groupCounts[coordinate] == 0) continue;
            baseByCoordinate[coordinate] =
                    Math.addExact(Math.multiplyExact(blocks, LENGTH_LANES), 1);
            blocks++;
        }

        int[] dispatch = new int[Math.multiplyExact(blocks, LENGTH_LANES)];
        for (int rank = 0; rank < ranks; rank++) {
            int start = offsets[rank];
            int length = offsets[rank + 1] - start;
            if (length > MAX_BYTES) continue;
            int base = baseByCoordinate[coordinate(arena, start, length)] - 1;
            int cell = base + length;
            dispatch[cell] = mergeCandidate(dispatch[cell], rank);
        }

        int initialPages = count(dispatch, AMBIGUOUS);
        int[] refinement = new int[Math.multiplyExact(initialPages, BYTE_RADIX)];
        int nextPage = 0;
        for (int cell = 0; cell < dispatch.length; cell++) {
            if (dispatch[cell] != AMBIGUOUS) continue;
            dispatch[cell] = encodeRefinementBase(nextPage);
            nextPage = Math.addExact(nextPage, BYTE_RADIX);
        }

        if (initialPages != 0) {
            fillDepth(arena, offsets, baseByCoordinate, dispatch, refinement, 2);
            for (int byteIndex = 3; byteIndex < MAX_PREFIX_BYTES; byteIndex++) {
                int previousLength = refinement.length;
                int pages = count(refinement, AMBIGUOUS);
                if (pages == 0) break;

                refinement = Arrays.copyOf(
                        refinement,
                        Math.addExact(
                                previousLength,
                                Math.multiplyExact(pages, BYTE_RADIX)));
                int pageBase = previousLength;
                for (int cell = 0; cell < previousLength; cell++) {
                    if (refinement[cell] != AMBIGUOUS) continue;
                    refinement[cell] = encodeRefinementBase(pageBase);
                    pageBase = Math.addExact(pageBase, BYTE_RADIX);
                }
                fillDepth(
                        arena,
                        offsets,
                        baseByCoordinate,
                        dispatch,
                        refinement,
                        byteIndex);
            }
        }

        long[] dispatchRows = rows(dispatch, arena, offsets);
        long[] refinementRows = rows(refinement, arena, offsets);
        int direct = countPositive(dispatch) + countPositive(refinement);
        int unresolved = count(dispatch, AMBIGUOUS) + count(refinement, AMBIGUOUS);
        if (direct > eligible) {
            throw new IllegalStateException("direct lookup rank coverage exceeds eligible ranks");
        }
        return new MIndexDirectInlineKey32(
                baseByCoordinate,
                dispatchRows,
                refinementRows,
                eligible,
                direct,
                unresolved);
    }

    /**
     * Returns lexical rank >=0, {@link #MISS}, or {@link #AMBIGUOUS}.
     */
    int candidate(byte[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        if (length > MAX_BYTES) return AMBIGUOUS;

        int encodedBase = baseByCoordinate[coordinate(source, offset, length)];
        if (encodedBase == 0) return MISS;

        int dispatchIndex = encodedBase - 1 + length;
        int cell = cell(dispatchRows, dispatchIndex);
        if (!isRefinement(cell)) {
            return finish(dispatchRows, dispatchIndex, cell, source, offset, length);
        }

        if (length <= 2) return AMBIGUOUS;
        int refinementIndex = refinementBase(cell) + Byte.toUnsignedInt(source[offset + 2]);
        cell = cell(refinementRows, refinementIndex);
        if (!isRefinement(cell)) {
            return finish(refinementRows, refinementIndex, cell, source, offset, length);
        }

        if (length <= 3) return AMBIGUOUS;
        refinementIndex = refinementBase(cell) + Byte.toUnsignedInt(source[offset + 3]);
        cell = cell(refinementRows, refinementIndex);
        if (!isRefinement(cell)) {
            return finish(refinementRows, refinementIndex, cell, source, offset, length);
        }

        if (length <= 4) return AMBIGUOUS;
        refinementIndex = refinementBase(cell) + Byte.toUnsignedInt(source[offset + 4]);
        cell = cell(refinementRows, refinementIndex);
        if (!isRefinement(cell)) {
            return finish(refinementRows, refinementIndex, cell, source, offset, length);
        }

        if (length <= 5) return AMBIGUOUS;
        refinementIndex = refinementBase(cell) + Byte.toUnsignedInt(source[offset + 5]);
        cell = cell(refinementRows, refinementIndex);
        if (!isRefinement(cell)) {
            return finish(refinementRows, refinementIndex, cell, source, offset, length);
        }

        if (length <= 6) return AMBIGUOUS;
        refinementIndex = refinementBase(cell) + Byte.toUnsignedInt(source[offset + 6]);
        cell = cell(refinementRows, refinementIndex);
        if (!isRefinement(cell)) {
            return finish(refinementRows, refinementIndex, cell, source, offset, length);
        }

        if (length <= 7) return AMBIGUOUS;
        refinementIndex = refinementBase(cell) + Byte.toUnsignedInt(source[offset + 7]);
        cell = cell(refinementRows, refinementIndex);
        if (!isRefinement(cell)) {
            return finish(refinementRows, refinementIndex, cell, source, offset, length);
        }
        return AMBIGUOUS;
    }

    int eligibleRankCount() {
        return eligibleRanks;
    }

    int directRankCount() {
        return directRanks;
    }

    int ambiguousRankCount() {
        return eligibleRanks - directRanks;
    }

    int unresolvedAmbiguousRowCount() {
        return unresolvedAmbiguousRows;
    }

    int refinementPageCount() {
        return refinementRows.length / (BYTE_RADIX * ROW_LONGS);
    }

    long retainedBytes() {
        return Integer.BYTES * (long) baseByCoordinate.length
                + Long.BYTES * (long) (dispatchRows.length + refinementRows.length);
    }

    private static void fillDepth(
            byte[] arena,
            int[] offsets,
            int[] baseByCoordinate,
            int[] dispatch,
            int[] refinement,
            int byteIndex) {
        for (int rank = 0; rank < offsets.length - 1; rank++) {
            int start = offsets[rank];
            int length = offsets[rank + 1] - start;
            if (length > MAX_BYTES) continue;

            int coarse =
                    baseByCoordinate[coordinate(arena, start, length)] - 1 + length;
            int cell = dispatch[coarse];
            if (!isRefinement(cell)) continue;

            int pageBase = refinementBase(cell);
            boolean reachable = true;
            for (int priorByte = 2; priorByte < byteIndex; priorByte++) {
                int priorCell =
                        refinement[pageBase + byteAt(arena, start, length, priorByte)];
                if (!isRefinement(priorCell)) {
                    reachable = false;
                    break;
                }
                pageBase = refinementBase(priorCell);
            }
            if (!reachable) continue;

            int refined =
                    pageBase + byteAt(arena, start, length, byteIndex);
            refinement[refined] = mergeCandidate(refinement[refined], rank);
        }
    }

    private static long[] rows(int[] cells, byte[] arena, int[] offsets) {
        long[] rows = new long[Math.multiplyExact(cells.length, ROW_LONGS)];
        for (int row = 0; row < cells.length; row++) {
            int cell = cells[row];
            int base = Math.multiplyExact(row, ROW_LONGS);
            rows[base] = cell;
            if (cell <= 0) continue;

            int rank = cell - 1;
            int start = offsets[rank];
            int length = offsets[rank + 1] - start;
            if (length > MAX_BYTES) {
                throw new IllegalStateException("direct row references unsupported key length");
            }
            rows[base + 1] = pack(arena, start, Math.min(8, length));
            if (length > 8) {
                rows[base + 2] = pack(arena, start + 8, Math.min(8, length - 8));
            }
            if (length > 16) {
                rows[base + 3] = pack(arena, start + 16, Math.min(8, length - 16));
            }
            if (length > 24) {
                rows[base + 4] = pack(arena, start + 24, length - 24);
            }
        }
        return rows;
    }

    private static int finish(
            long[] rows,
            int row,
            int cell,
            byte[] source,
            int offset,
            int length) {
        if (cell == 0) return MISS;
        if (cell == AMBIGUOUS || isRefinement(cell)) return AMBIGUOUS;
        int base = Math.multiplyExact(row, ROW_LONGS);

        if (length > 0
                && rows[base + 1] != pack(source, offset, Math.min(8, length))) {
            return MISS;
        }
        if (length <= 8) return cell - 1;

        if (rows[base + 2] != pack(source, offset + 8, Math.min(8, length - 8))) {
            return MISS;
        }
        if (length <= 16) return cell - 1;

        if (rows[base + 3] != pack(source, offset + 16, Math.min(8, length - 16))) {
            return MISS;
        }
        if (length <= 24) return cell - 1;

        return rows[base + 4] == pack(source, offset + 24, length - 24)
                ? cell - 1
                : MISS;
    }

    private static int cell(long[] rows, int row) {
        int base = Math.multiplyExact(row, ROW_LONGS);
        if (base < 0 || base >= rows.length) {
            throw new IllegalStateException("direct lookup row outside frozen geometry");
        }
        return (int) rows[base];
    }

    private static int coordinate(byte[] source, int offset, int length) {
        int first = length == 0 ? 0 : Byte.toUnsignedInt(source[offset]);
        int second = length < 2 ? 0 : Byte.toUnsignedInt(source[offset + 1]);
        return (first << 8) | second;
    }

    private static int byteAt(
            byte[] source, int offset, int length, int byteIndex) {
        return byteIndex < length
                ? Byte.toUnsignedInt(source[offset + byteIndex])
                : 0;
    }

    private static int mergeCandidate(int prior, int rank) {
        int candidate = rank + 1;
        if (prior == 0 || prior == candidate) return candidate;
        return AMBIGUOUS;
    }

    private static boolean isRefinement(int cell) {
        return cell < AMBIGUOUS;
    }

    private static int encodeRefinementBase(int base) {
        return -Math.addExact(base, REFINEMENT_BIAS);
    }

    private static int refinementBase(int encoded) {
        if (!isRefinement(encoded)) {
            throw new IllegalArgumentException("not a refinement cell: " + encoded);
        }
        return Math.subtractExact(-encoded, REFINEMENT_BIAS);
    }

    private static int count(int[] values, int wanted) {
        int count = 0;
        for (int value : values) if (value == wanted) count++;
        return count;
    }

    private static int countPositive(int[] values) {
        int count = 0;
        for (int value : values) if (value > 0) count++;
        return count;
    }

    private static int checkedStart(int[] offsets, int rank, int arenaLength) {
        int start = offsets[rank];
        int end = offsets[rank + 1];
        if (start < 0 || end < start || end > arenaLength) {
            throw new IllegalArgumentException("invalid dictionary offset lane at rank " + rank);
        }
        return start;
    }

    /**
     * Packs 1..8 bytes in big-endian lexical order. Counts below eight are fully unrolled.
     */
    private static long pack(byte[] source, int offset, int count) {
        if (count < 0 || count > Long.BYTES) {
            throw new IllegalArgumentException("count=" + count);
        }
        if (count == 0) return 0L;
        Objects.checkFromIndexSize(offset, count, source.length);
        if (count == Long.BYTES) {
            return (long) BYTE_ARRAY_LONG_BE.get(source, offset);
        }
        return switch (count) {
            case 1 -> (source[offset] & 0xffL) << 56;
            case 2 -> ((source[offset] & 0xffL) << 56)
                    | ((source[offset + 1] & 0xffL) << 48);
            case 3 -> ((source[offset] & 0xffL) << 56)
                    | ((source[offset + 1] & 0xffL) << 48)
                    | ((source[offset + 2] & 0xffL) << 40);
            case 4 -> ((source[offset] & 0xffL) << 56)
                    | ((source[offset + 1] & 0xffL) << 48)
                    | ((source[offset + 2] & 0xffL) << 40)
                    | ((source[offset + 3] & 0xffL) << 32);
            case 5 -> ((source[offset] & 0xffL) << 56)
                    | ((source[offset + 1] & 0xffL) << 48)
                    | ((source[offset + 2] & 0xffL) << 40)
                    | ((source[offset + 3] & 0xffL) << 32)
                    | ((source[offset + 4] & 0xffL) << 24);
            case 6 -> ((source[offset] & 0xffL) << 56)
                    | ((source[offset + 1] & 0xffL) << 48)
                    | ((source[offset + 2] & 0xffL) << 40)
                    | ((source[offset + 3] & 0xffL) << 32)
                    | ((source[offset + 4] & 0xffL) << 24)
                    | ((source[offset + 5] & 0xffL) << 16);
            case 7 -> ((source[offset] & 0xffL) << 56)
                    | ((source[offset + 1] & 0xffL) << 48)
                    | ((source[offset + 2] & 0xffL) << 40)
                    | ((source[offset + 3] & 0xffL) << 32)
                    | ((source[offset + 4] & 0xffL) << 24)
                    | ((source[offset + 5] & 0xffL) << 16)
                    | ((source[offset + 6] & 0xffL) << 8);
            default -> throw new AssertionError(count);
        };
    }
}
