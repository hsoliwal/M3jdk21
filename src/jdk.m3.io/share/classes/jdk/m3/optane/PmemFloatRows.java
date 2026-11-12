// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.util.Arrays;
import java.util.Objects;

/**
 * Non-owning view of a dense, row-major little-endian IEEE 754 FP32 table.
 * Rows have no padding; offsets and row identities are 64-bit. The view neither
 * retains raw pointers nor closes its region. Closing the region invalidates all
 * subsequent operations, including empty gathers. Each operation holds the
 * region owner's read lease across every row and blocks same-owner writes and close.
 * External processes and distinct mapping aliases must not mutate or truncate the
 * file during an operation, as with other {@link PmemRegion} reads.
 *
 * <p>This is a storage copy operation: finite values, signed zeros, infinities and
 * NaNs are all accepted without arithmetic, normalization or finite-value filtering.
 * Signaling-NaN payload preservation is subject to the JVM's
 * {@link Float#intBitsToFloat(int)} semantics. Numeric consumers that require finite
 * inputs must validate them separately.</p>
 *
 * <p>Only caller-owned heap {@code float[]} output is accepted, so output cannot
 * alias mapped storage or the {@code long[]} selection. The view retains no input or
 * output arrays. FILE uses at most 64 KiB of byte scratch and one reusable buffer;
 * native backends use one JNI call per operation with bounded native windows.
 * Gather additionally snapshots the requested IDs into one primitive array of
 * {@code 8 * count} payload bytes, without any per-row objects.</p>
 */
public final class PmemFloatRows {
    private final PmemRegion region;
    private final long byteOffset;
    private final long rows;
    private final int columns;

    /**
     * Create a view wholly contained in an open SDK-owned region. Zero rows are
     * allowed; columns must be positive. Shape multiplication and mapping bounds
     * are checked without overflow. No table data is read by construction.
     *
     * @throws IllegalArgumentException if columns is nonpositive or the region is foreign
     * @throws IndexOutOfBoundsException if the shape or byte range does not fit
     * @throws IllegalStateException if the region is closed
     */
    public PmemFloatRows(PmemRegion region, long byteOffset, long rowCount, int columns) {
        this.region = Objects.requireNonNull(region, "region");
        if (columns <= 0) throw new IllegalArgumentException("columns must be positive");
        long rowBytes = (long) columns * Float.BYTES;
        if (rowCount < 0 || rowCount > Long.MAX_VALUE / rowBytes)
            throw new IndexOutOfBoundsException("float table shape overflows");
        long bytes = rowCount * rowBytes;
        PmemMapping.withReadLease(region, () -> {
            long length = region.length();
            if (byteOffset < 0 || byteOffset > length || bytes > length - byteOffset)
                throw new IndexOutOfBoundsException("float table exceeds region");
            return null;
        });
        this.byteOffset = byteOffset;
        this.rows = rowCount;
        this.columns = columns;
    }

    /** Number of rows in this view, including identities beyond the int range. */
    public long rowCount() { return rows; }

    /** Number of FP32 values per row. */
    public int columns() { return columns; }

    /**
     * Copy one complete row to {@code target[targetOffset .. targetOffset+columns)}.
     * Invalid row IDs or output ranges are rejected before target mutation.
     */
    public void read(long rowId, float[] target, int targetOffset) {
        Objects.requireNonNull(target, "target");
        checkRow(rowId);
        checkTarget(target, targetOffset, 1);
        PmemMapping.floatRowsLE(region, byteOffset, rows, columns,
                rowId, null, 1, target, targetOffset);
    }

    /**
     * Copy selected complete rows contiguously in selection order, preserving
     * duplicates. All selection, row-ID and output bounds are checked before any
     * output is written. The selection slice is copied once and is not retained.
     * The caller must not mutate the selection while it is being snapshotted or
     * concurrently use the output array during this operation.
     *
     * <p>Argument and closed-owner rejection leave target unchanged. An I/O failure
     * after a successful preflight may leave a copied prefix, matching ordinary
     * region reads; this method is not a transaction or a durability operation.</p>
     */
    public void gather(long[] rowIds, int rowOffset, int count,
                       float[] target, int targetOffset) {
        Objects.requireNonNull(rowIds, "rowIds");
        Objects.requireNonNull(target, "target");
        Objects.checkFromIndexSize(rowOffset, count, rowIds.length);
        checkTarget(target, targetOffset, count);
        long[] selected = Arrays.copyOfRange(rowIds, rowOffset, rowOffset + count);
        for (long rowId : selected) checkRow(rowId);
        PmemMapping.floatRowsLE(region, byteOffset, rows, columns,
                0, selected, count, target, targetOffset);
    }

    private void checkRow(long rowId) {
        if (rowId < 0 || rowId >= rows)
            throw new IndexOutOfBoundsException("float row ID outside table");
    }

    private void checkTarget(float[] target, int offset, int count) {
        // Division checks the required element count before any int multiplication.
        if (offset < 0 || offset > target.length || count > (target.length - offset) / columns)
            throw new IndexOutOfBoundsException("float output range outside array");
    }
}
