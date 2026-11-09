/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package sun.swing.list;

/**
 * {@link M3RowOffsets} as one {@code int} prefix lane: {@code start[r]} is the wrapped sum of the
 * first {@code r} heights. One pass builds it (O(n)); {@link #startOf} is a load and
 * {@link #firstEndingAfter} a binary search (O(log n)). The heights are not updated in place:
 * {@code BasicListUI} allocates a new {@code cellHeights} array on every layout and
 * {@link M3ListRows} builds a new lane for it. Chosen over {@code javax.swing.SizeSequence}, a
 * Fenwick tree and sqrt blocks by measurement (m3/docs/swing-viewport/ALGORITHM_CATALOGUE.tsv).
 */
//FLLDCIM: M3 swing-viewport leaf 3 - prefix lane over BasicListUI.cellHeights, same sums as the stock loops
public final class M3PrefixRowOffsets implements M3RowOffsets {

    private final int[] start;
    private final boolean monotone;

    /** Builds the lane over {@code heights}, which the caller does not change afterwards. */
    public M3PrefixRowOffsets(int[] heights) {
        int n = heights.length;
        int[] s = new int[n + 1];
        int sum = 0;
        long exact = 0;
        boolean nonNegative = true;
        for (int i = 0; i < n; i++) {
            int h = heights[i];
            nonNegative &= h >= 0;
            exact += h;
            sum += h;
            s[i + 1] = sum;
        }
        start = s;
        monotone = nonNegative && exact <= Integer.MAX_VALUE;
    }

    @Override
    public int rowCount() {
        return start.length - 1;
    }

    @Override
    public int startOf(int row) {
        return start[row];
    }

    @Override
    public boolean monotone() {
        return monotone;
    }

    @Override
    public int firstEndingAfter(long offset) {
        int lo = 1;
        int hi = start.length - 1;
        int row = hi;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (start[mid] > offset) {
                row = mid - 1;
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }
        return row;
    }
}
