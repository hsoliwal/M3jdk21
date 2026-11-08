/*******************************************************************************
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
package jdk.internal.m3.collections;

import java.util.Objects;

/**
 * Primitive algebra over ascending unique signed long arrays.
 *
 * <p>Allocating operations count, then emit into one exact-sized detached array.
 * Into operations reuse caller storage and validate capacity before any write.
 * Inputs must remain unchanged across both passes; sorting is a caller invariant.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongSetOps.java}
 * blob {@code 0512830b2ad2fd9640888f340e3f19ad6faf048b} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedLongSetOps {
    private M3PackedLongSetOps() { }

    public static long[] unionSorted(long[] left, long[] right) {
        return result(left, right, Mode.UNION);
    }

    public static long[] intersectionSorted(long[] left, long[] right) {
        return result(left, right, Mode.INTERSECTION);
    }

    public static long[] differenceSorted(long[] left, long[] right) {
        return result(left, right, Mode.DIFFERENCE);
    }

    public static long[] symmetricDifferenceSorted(long[] left, long[] right) {
        return result(left, right, Mode.SYMMETRIC_DIFFERENCE);
    }

    /** Tests overlap, stopping at the first common value without allocating a result. */
    public static boolean intersectsSorted(long[] left, long[] right) {
        requireInputs(left, right);
        int a = 0;
        int b = 0;
        while (a < left.length && b < right.length) {
            if (left[a] == right[b]) return true;
            if (left[a] < right[b]) a++;
            else b++;
        }
        return false;
    }

    /** Exact count without allocating a result; cannot exceed either input length. */
    public static int intersectionCardinalitySorted(long[] left, long[] right) {
        requireInputs(left, right);
        return (int) merge(left, right, null, 0, Mode.INTERSECTION);
    }

    /** Tests right subset-of left, including the empty-set identity. */
    public static boolean containsAllSorted(long[] left, long[] right) {
        requireInputs(left, right);
        int a = 0;
        int b = 0;
        while (a < left.length && b < right.length) {
            if (left[a] < right[b]) a++;
            else if (left[a] == right[b]) { a++; b++; }
            else return false;
        }
        return b == right.length;
    }

    /** Writes ascending union; rejects input aliases and insufficient capacity before writing. */
    public static int unionSortedInto(long[] left, long[] right, long[] target, int offset) {
        return into(left, right, target, offset, Mode.UNION);
    }

    /** Writes ascending intersection; returns count and leaves other target positions unchanged. */
    public static int intersectionSortedInto(long[] left, long[] right, long[] target, int offset) {
        return into(left, right, target, offset, Mode.INTERSECTION);
    }

    /** Writes left-minus-right; shares the failure-atomic validation contract of unionSortedInto. */
    public static int differenceSortedInto(long[] left, long[] right, long[] target, int offset) {
        return into(left, right, target, offset, Mode.DIFFERENCE);
    }

    /** Writes ascending symmetric difference without allocating workspace. */
    public static int symmetricDifferenceSortedInto(
            long[] left, long[] right, long[] target, int offset) {
        return into(left, right, target, offset, Mode.SYMMETRIC_DIFFERENCE);
    }

    private static long[] result(long[] left, long[] right, Mode mode) {
        requireInputs(left, right);
        int count = checkedSize(merge(left, right, null, 0, mode));
        long[] target = new long[count];
        merge(left, right, target, 0, mode);
        return target;
    }

    private static int into(long[] left, long[] right, long[] target, int offset, Mode mode) {
        requireInputs(left, right);
        Objects.requireNonNull(target, "target");
        if (target == left || target == right) {
            throw new IllegalArgumentException("target aliases an input");
        }
        Objects.checkFromIndexSize(offset, 0, target.length);
        int count = checkedSize(merge(left, right, null, 0, mode));
        Objects.checkFromIndexSize(offset, count, target.length);
        merge(left, right, target, offset, mode);
        return count;
    }

    private static void requireInputs(long[] left, long[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
    }

    private static int checkedSize(long size) {
        if (size > M3PackedSupport.MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError("required result array too large");
        }
        return (int) size;
    }

    /* One merge skeleton serves count and emission. Only count can exceed int range;
     * emission is reached after exact preflight. No arrays are allocated here. */
    private static long merge(long[] left, long[] right, long[] target, int offset, Mode mode) {
        int a = 0;
        int b = 0;
        long write = 0L;
        while (a < left.length && b < right.length) {
            long lv = left[a];
            long rv = right[b];
            boolean emit;
            long value;
            if (lv < rv) {
                emit = mode != Mode.INTERSECTION;
                value = lv;
                a++;
            } else if (rv < lv) {
                emit = mode == Mode.UNION || mode == Mode.SYMMETRIC_DIFFERENCE;
                value = rv;
                b++;
            } else {
                emit = mode == Mode.UNION || mode == Mode.INTERSECTION;
                value = lv;
                a++;
                b++;
            }
            if (emit) {
                if (target != null) target[offset + (int) write] = value;
                write++;
            }
        }
        if (mode != Mode.INTERSECTION) {
            if (target != null) {
                System.arraycopy(left, a, target, offset + (int) write, left.length - a);
            }
            write += left.length - a;
        }
        if (mode == Mode.UNION || mode == Mode.SYMMETRIC_DIFFERENCE) {
            if (target != null) {
                System.arraycopy(right, b, target, offset + (int) write, right.length - b);
            }
            write += right.length - b;
        }
        return write;
    }

    private enum Mode {
        UNION,
        INTERSECTION,
        DIFFERENCE,
        SYMMETRIC_DIFFERENCE
    }
}
