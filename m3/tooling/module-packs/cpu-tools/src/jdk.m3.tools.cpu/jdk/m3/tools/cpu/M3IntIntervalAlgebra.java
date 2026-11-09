// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.util.Objects;

/**
 * Pure half-open integer interval set algebra over the existing immutable M3IntIntervalSet.
 * Every operator uses two monotone primitive endpoint lanes, never boxed interval nodes.
 * Inputs remain untouched; the returned owner is always a newly admitted detached set.
 */
public final class M3IntIntervalAlgebra {
    private M3IntIntervalAlgebra() { }

    /** Normalized union: overlapping or adjacent half-open intervals are coalesced. */
    public static M3IntIntervalSet union(M3IntIntervalSet left, M3IntIntervalSet right) {
        int[] a = Objects.requireNonNull(left, "left").toPairs();
        int[] b = Objects.requireNonNull(right, "right").toPairs();
        int[] out = new int[unionInto(a, b, null)];
        if (unionInto(a, b, out) != out.length) throw new AssertionError("immutable snapshot drift");
        return M3IntIntervalSet.copyOf(out);
    }

    /** Normalized two-pointer intersection with exclusive right endpoints. */
    public static M3IntIntervalSet intersection(M3IntIntervalSet left, M3IntIntervalSet right) {
        int[] a = Objects.requireNonNull(left, "left").toPairs();
        int[] b = Objects.requireNonNull(right, "right").toPairs();
        int[] out = new int[intersectionInto(a, b, null)];
        if (intersectionInto(a, b, out) != out.length) throw new AssertionError("immutable snapshot drift");
        return M3IntIntervalSet.copyOf(out);
    }

    /** Left-minus-right membership, preserving cut fragments of normalized intervals. */
    public static M3IntIntervalSet difference(M3IntIntervalSet left, M3IntIntervalSet right) {
        int[] a = Objects.requireNonNull(left, "left").toPairs();
        int[] b = Objects.requireNonNull(right, "right").toPairs();
        int[] out = new int[differenceInto(a, b, null)];
        if (differenceInto(a, b, out) != out.length) throw new AssertionError("immutable snapshot drift");
        return M3IntIntervalSet.copyOf(out);
    }

    /** Uncovered portions of [startInclusive,endExclusive), rejecting reversed queries. */
    public static M3IntIntervalSet complementWithin(M3IntIntervalSet set, int startInclusive,
                                                     int endExclusive) {
        Objects.requireNonNull(set, "set");
        if (startInclusive > endExclusive) throw new IllegalArgumentException("reversed query");
        return difference(M3IntIntervalSet.copyOf(new int[]{startInclusive, endExclusive}), set);
    }

    private static int emit(int[] out, int used, int start, int end) {
        if (start >= end) return used;
        if (used >= 2 * M3IntIntervalSet.MAX_PAIRS) {
            throw new IllegalArgumentException("interval result too large");
        }
        if (out != null) { out[used] = start; out[used + 1] = end; }
        return used + 2;
    }

    private static int unionInto(int[] a, int[] b, int[] out) {
        int i = 0, j = 0, used = 0;
        int pendingStart = 0, pendingEnd = 0;
        boolean pending = false;
        while (i < a.length || j < b.length) {
            final int start, end;
            if (j == b.length || (i < a.length && a[i] <= b[j])) {
                start = a[i]; end = a[i + 1]; i += 2;
            } else {
                start = b[j]; end = b[j + 1]; j += 2;
            }
            if (!pending) {
                pending = true; pendingStart = start; pendingEnd = end;
            } else if (start <= pendingEnd) {
                pendingEnd = Math.max(pendingEnd, end);
            } else {
                used = emit(out, used, pendingStart, pendingEnd);
                pendingStart = start; pendingEnd = end;
            }
        }
        return pending ? emit(out, used, pendingStart, pendingEnd) : used;
    }

    private static int intersectionInto(int[] a, int[] b, int[] out) {
        int i = 0, j = 0, used = 0;
        while (i < a.length && j < b.length) {
            int start = Math.max(a[i], b[j]);
            int end = Math.min(a[i + 1], b[j + 1]);
            used = emit(out, used, start, end);
            if (a[i + 1] < b[j + 1]) i += 2;
            else if (a[i + 1] > b[j + 1]) j += 2;
            else { i += 2; j += 2; }
        }
        return used;
    }

    private static int differenceInto(int[] a, int[] b, int[] out) {
        int j = 0, used = 0;
        for (int i = 0; i < a.length; i += 2) {
            int cursor = a[i], end = a[i + 1];
            while (j < b.length && b[j + 1] <= cursor) j += 2;
            int k = j;
            while (k < b.length && b[k] < end) {
                if (b[k] > cursor) {
                    used = emit(out, used, cursor, Math.min(b[k], end));
                }
                if (b[k + 1] >= end) { cursor = end; break; }
                cursor = Math.max(cursor, b[k + 1]);
                k += 2;
            }
            used = emit(out, used, cursor, end);
            j = k;
        }
        return used;
    }
}
