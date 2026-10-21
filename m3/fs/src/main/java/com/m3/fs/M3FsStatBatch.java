/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.util.Arrays;
import java.util.Objects;

/**
 * Detached native M3FS metadata rows, held as primitive columns rather than inode objects.
 * A live handle samples each requested path independently; only an openSnapshot handle
 * provides a retained read view. No cross-provider or transactional guarantee is implied.
 * A result owns an independent copy of every row; API Stat objects are created on demand.
 */
public final class M3FsStatBatch {
    /** Match bits describe the returned row, never a speculative existence check. */
    public static final int EXISTS = 1;
    public static final int FILE = 2;
    public static final int DIRECTORY = 4;
    public static final int SYMLINK = 8;
    public static final int NOT_FOUND = 16;
    public static final int ERROR = 32;

    /** JNI row: native status, inode, size, generation, mtime, kind, mode, links, block size. */
    static final int WIDTH = 9;
    public static final int OK = 0;
    public static final int STATUS_NOT_FOUND = 1;

    private final long[] columns;
    private final int[] flags;

    M3FsStatBatch(long[] nativeColumns) {
        Objects.requireNonNull(nativeColumns, "nativeColumns");
        if (nativeColumns.length % WIDTH != 0) {
            throw new IllegalArgumentException("invalid native metadata geometry");
        }
        columns = nativeColumns.clone();
        flags = new int[columns.length / WIDTH];
        for (int row = 0; row < flags.length; row++) {
            int offset = row * WIDTH;
            int code = Math.toIntExact(columns[offset]);
            if (code < 0 || code > 13) {
                throw new IllegalArgumentException("unknown native metadata status");
            }
            if (code == STATUS_NOT_FOUND) {
                flags[row] = NOT_FOUND;
            } else if (code != OK) {
                flags[row] = ERROR;
            } else {
                long kind = columns[offset + 5];
                flags[row] = switch (Math.toIntExact(kind)) {
                    case 1 -> EXISTS | FILE;
                    case 2 -> EXISTS | DIRECTORY;
                    case 3 -> EXISTS | SYMLINK;
                    default -> throw new IllegalArgumentException("unknown inode kind");
                };
                validateStat(offset);
            }
        }
    }

    private void validateStat(int offset) {
        if (columns[offset + 1] <= 0 || columns[offset + 2] < 0
                || columns[offset + 3] < 1 || columns[offset + 4] < 0
                || columns[offset + 6] < 0 || columns[offset + 6] > 07777
                || columns[offset + 7] < 0 || columns[offset + 7] > Integer.MAX_VALUE
                || columns[offset + 8] <= 0 || columns[offset + 8] > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid native stat fields");
        }
    }

    public int size() { return flags.length; }

    public int status(int row) {
        return Math.toIntExact(columns[Objects.checkIndex(row, size()) * WIDTH]);
    }

    public int stateMask(int row) { return flags[Objects.checkIndex(row, size())]; }

    /** Inherits the native per-row error code; a missing file is not an I/O failure. */
    public boolean exists(int row) { return (stateMask(row) & EXISTS) != 0; }

    /** Materializes a record only when requested, and refuses absent or failed rows. */
    public M3FS.Stat stat(int row) {
        int offset = Objects.checkIndex(row, size()) * WIDTH;
        if (status(row) != OK) {
            throw new IllegalStateException("metadata row has no stat: " + status(row));
        }
        return new M3FS.Stat(
                columns[offset + 1], columns[offset + 2], columns[offset + 3],
                columns[offset + 4], M3FS.Kind.fromNative(columns[offset + 5]),
                Math.toIntExact(columns[offset + 6]), Math.toIntExact(columns[offset + 7]),
                Math.toIntExact(columns[offset + 8]));
    }

    /** Stable encounter-order selection over bit-mask columns, without entry objects. */
    public int[] selectRows(int requiredBits, int forbiddenBits) {
        if ((requiredBits & forbiddenBits) != 0) return new int[0];
        int count = 0;
        for (int state : flags) {
            if (accept(state, requiredBits, forbiddenBits)) count++;
        }
        int[] result = new int[count];
        int cursor = 0;
        for (int row = 0; row < flags.length; row++) {
            if (accept(flags[row], requiredBits, forbiddenBits)) result[cursor++] = row;
        }
        return result;
    }

    private static boolean accept(int state, int required, int forbidden) {
        return (state & required) == required && (state & forbidden) == 0;
    }

    /**
     * Stable sorted primitive indices for existing files only. Equal sizes preserve
     * caller path order; no filename comparator, inode object or boxed row is created.
     */
    public int[] fileRowsBySize(boolean descending) {
        int[] selected = selectRows(EXISTS | FILE, ERROR);
        int n = selected.length;
        if (n < 2) return selected;
        int[] scratch = new int[n];
        int[] source = selected;
        int[] target = scratch;
        for (long width = 1; width < n; width <<= 1) {
            for (long at = 0; at < n; at += 2 * width) {
                int left = (int) at;
                int middle = (int) Math.min(n, at + width);
                int right = middle;
                int end = (int) Math.min(n, at + 2 * width);
                int out = left;
                while (left < middle && right < end) {
                    int comparison = Long.compare(sizeOf(source[left]), sizeOf(source[right]));
                    if (descending) comparison = -comparison;
                    target[out++] = comparison <= 0 ? source[left++] : source[right++];
                }
                while (left < middle) target[out++] = source[left++];
                while (right < end) target[out++] = source[right++];
            }
            int[] swap = source;
            source = target;
            target = swap;
        }
        if (source != selected) System.arraycopy(source, 0, selected, 0, n);
        return selected;
    }

    private long sizeOf(int row) { return columns[row * WIDTH + 2]; }

    /** Columns and status masks are retained only in this detached result. */
    public long retainedPrimitiveBytes() {
        return (long) columns.length * Long.BYTES + (long) flags.length * Integer.BYTES;
    }

    @Override public String toString() {
        return "M3FsStatBatch[size=" + size() + ", successes="
                + Arrays.stream(flags).filter(flag -> (flag & EXISTS) != 0).count() + "]";
    }
}