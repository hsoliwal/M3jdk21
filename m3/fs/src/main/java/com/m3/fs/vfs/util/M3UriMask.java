/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs.vfs.util;

/** Bounded unsigned-byte membership mask; one primitive long[4] is the only retained state. */
final class M3UriMask {
    private static final int LIMIT = 256;
    private final long[] lanes = new long[LIMIT / Long.SIZE];

    M3UriMask set(final int... indexes) {
        for (final int index : indexes) {
            java.util.Objects.checkIndex(index, LIMIT);
            lanes[index >>> 6] |= 1L << (index & 63);
        }
        return this;
    }

    M3UriMask setInclusive(final int first, final int last) {
        if (first > last) {
            throw new IllegalArgumentException("inverted character interval");
        }
        for (int index = first; index <= last; index++) {
            set(index);
        }
        return this;
    }

    M3UriMask or(final M3UriMask... others) {
        for (final M3UriMask other : others) {
            for (int lane = 0; lane < lanes.length; lane++) {
                lanes[lane] |= other.lanes[lane];
            }
        }
        return this;
    }

    M3UriMask andNot(final M3UriMask other) {
        for (int lane = 0; lane < lanes.length; lane++) {
            lanes[lane] &= ~other.lanes[lane];
        }
        return this;
    }

    M3UriMask clear(final int... indexes) {
        for (final int index : indexes) {
            java.util.Objects.checkIndex(index, LIMIT);
            lanes[index >>> 6] &= ~(1L << (index & 63));
        }
        return this;
    }

    boolean get(final int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(index);
        }
        return index < LIMIT && (lanes[index >>> 6] & (1L << (index & 63))) != 0;
    }
}
