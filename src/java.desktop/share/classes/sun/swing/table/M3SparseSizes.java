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

package sun.swing.table;

import java.util.Arrays;

import javax.swing.RowSorter;
import javax.swing.SizeSequence;

import sun.security.action.GetBooleanAction;

/**
 * A {@code SizeSequence} whose default size is implied: only the entries whose size differs from the
 * default are stored, as a sorted int lane of indices with a lane of sizes, so n rows with k custom
 * sizes cost O(k) instead of 4n bytes ({@code -Dm3.swing.viewport=true}, off by default). {@code JTable}
 * keeps one for its view rows in place of {@code rowModel} and, with a sorter, one for the model rows in
 * place of {@code SortManager.modelRowSizes}: the same two stores as stock, each sparse.
 * <p>
 * Every answer is the stock {@code SizeSequence}'s, including those outside the range: {@code getSize}
 * is 0 there, {@code getPosition} is 0 before the first entry and the total after the last, and
 * {@code getIndex} is 0 before position 0 and the length at or after the total. Two stock behaviours
 * are not mirrored because they corrupt the stock tree: {@code setSize} of a negative index is
 * ignored here, and the inserts and removals are checked against the length. The default size is at
 * least 1, as a {@code JTable} row height is.
 * <p>
 * Above {@link #LIMIT} custom sizes the sparse form loses (an insert in the middle of the lanes and the
 * prefix rebuilt after each change are O(k), against O(log n) for the stock tree), so the store moves to a
 * dense {@code SizeSequence}, the stock structure, and stays there until {@link #setSizes}. Design and
 * proof: m3/docs/swing-viewport/DESIGN.md section 15.
 */
//FLLDCIM: M3 swing-viewport leaf 8 - JTable row heights as one sparse store per order, no dense copies
public final class M3SparseSizes {

    /** Whether {@code JTable} keeps its row heights here; read once from {@code m3.swing.viewport}. */
    public static final boolean ENABLED = GetBooleanAction.privilegedGetProperty("m3.swing.viewport");

    /** The most custom sizes kept sparse: 64 KB of lanes; above it the stock dense tree is used. */
    public static final int LIMIT = 4096;

    private int length;
    /** The stock dense tree once more than {@link #LIMIT} sizes are custom, else null. */
    private SizeSequence dense;
    private int defaultSize;
    private int[] index = new int[4];
    private int[] size = new int[4];
    private long[] prefix = new long[5];
    private int count;
    private boolean dirty;

    /** {@code new SizeSequence(length, defaultSize)}; the default is at least 1. */
    public M3SparseSizes(int length, int defaultSize) {
        if (length < 0 || defaultSize < 1) {
            throw new IllegalArgumentException("length " + length + ", default size " + defaultSize);
        }
        this.length = length;
        this.defaultSize = defaultSize;
    }

    /** {@code SizeSequence.getSizes()}. */
    public int[] getSizes() {
        if (dense != null) {
            return dense.getSizes();
        }
        int[] sizes = new int[length];
        Arrays.fill(sizes, defaultSize);
        for (int j = 0; j < count; j++) {
            sizes[index[j]] = size[j];
        }
        return sizes;
    }

    /** The number of entries, as {@code getSizes().length}. */
    public int length() {
        return length;
    }

    /** The default size. */
    public int defaultSize() {
        return defaultSize;
    }

    /** The number of sparse entries (sizes that differ from the default); -1 once the store is dense. */
    public int count() {
        return dense != null ? -1 : count;
    }

    /** {@code SizeSequence.getSize}: 0 outside the range. */
    public int getSize(int i) {
        if (i < 0 || i >= length) {
            return 0;
        }
        if (dense != null) {
            return dense.getSize(i);
        }
        int j = find(i);
        return j >= 0 ? size[j] : defaultSize;
    }

    /** {@code SizeSequence.getPosition}: 0 at or before 0, the total at or after the length. */
    public int getPosition(int i) {
        if (i <= 0) {
            return 0;
        }
        if (i >= length) {
            i = length;
        }
        if (dense != null) {
            return dense.getPosition(i);
        }
        build();
        int j = find(i);
        int below = j >= 0 ? j : -1 - j;
        return (int) (i * (long) defaultSize + prefix[below]);
    }

    /** {@code SizeSequence.getIndex}: 0 before position 0, the length at or after the total. */
    public int getIndex(int position) {
        if (position < 0 || length == 0) {
            return 0;
        }
        if (dense != null) {
            return dense.getIndex(position);
        }
        build();
        if (position >= total()) {
            return length;
        }
        int lo = 0;
        int hi = count;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (start(mid) <= position) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        int row = 0;
        long at = 0;
        if (lo > 0) {
            int j = lo - 1;
            long s = start(j);
            if (position < s + size[j]) {
                return index[j];
            }
            row = index[j] + 1;
            at = s + size[j];
        }
        return row + (int) ((position - at) / defaultSize);
    }

    /** {@code SizeSequence.setSize}; an index at or beyond the length changes nothing, a negative one is ignored. */
    public void setSize(int i, int s) {
        if (i < 0 || i >= length) {
            return;
        }
        if (dense == null && count == LIMIT && s != defaultSize && find(i) < 0) {
            toDense();
        }
        if (dense != null) {
            dense.setSize(i, s);
            return;
        }
        int j = find(i);
        if (s == defaultSize) {
            if (j >= 0) {
                remove(j, 1);
            }
        } else if (j >= 0) {
            size[j] = s;
            dirty = true;
        } else {
            insert(-1 - j, i, s);
        }
    }

    /** {@code SizeSequence.insertEntries}: {@code n} entries of {@code value} before {@code start}. */
    public void insertEntries(int start, int n, int value) {
        if (start < 0 || start > length || n < 0) {
            throw new IndexOutOfBoundsException("insert " + n + " at " + start + " of " + length);
        }
        if (dense == null && value != defaultSize && count + (long) n > LIMIT) {
            toDense();
        }
        if (dense != null) {
            dense.insertEntries(start, n, value);
            length += n;
            return;
        }
        int j = lowerBound(start);
        for (int k = j; k < count; k++) {
            index[k] += n;
        }
        length += n;
        dirty = true;
        if (value != defaultSize) {
            for (int k = 0; k < n; k++) {
                insert(j + k, start + k, value);
            }
        }
    }

    /** {@code SizeSequence.removeEntries}: the {@code n} entries from {@code start}. */
    public void removeEntries(int start, int n) {
        if (start < 0 || n < 0 || start + n > length) {
            throw new IndexOutOfBoundsException("remove " + n + " at " + start + " of " + length);
        }
        if (dense != null) {
            dense.removeEntries(start, n);
            length -= n;
            return;
        }
        int from = lowerBound(start);
        int to = lowerBound(start + n);
        if (to > from) {
            remove(from, to - from);
        }
        for (int k = from; k < count; k++) {
            index[k] -= n;
        }
        length -= n;
        dirty = true;
    }

    /** The package-private {@code SizeSequence.setSizes(int, int)}: every entry {@code value}, {@code n} of them. */
    public void setSizes(int n, int value) {
        if (n < 0 || value < 1) {
            throw new IllegalArgumentException("length " + n + ", size " + value);
        }
        dense = null;
        count = 0;
        length = n;
        defaultSize = value;
        dirty = true;
    }

    /** Moves the store to the stock dense tree (more than {@link #LIMIT} custom sizes). */
    private void toDense() {
        dense = new SizeSequence(getSizes());
        count = 0;
        index = new int[4];
        size = new int[4];
        prefix = new long[5];
    }

    /**
     * The stock {@code SortManager.setViewRowHeightsFromModel}: this store in model order mapped to a
     * new one over {@code viewCount} view rows with {@code viewDefault}, through the sorter; rows the
     * sorter hides are left out.
     */
    public M3SparseSizes inViewOrder(int viewCount, int viewDefault, RowSorter<?> sorter) {
        M3SparseSizes view = new M3SparseSizes(viewCount, viewDefault);
        if (dense != null) {
            for (int m = 0; m < length; m++) {
                int v = sorter.convertRowIndexToView(m);
                if (v != -1) {
                    view.setSize(v, dense.getSize(m));
                }
            }
            return view;
        }
        for (int j = 0; j < count; j++) {
            int v = sorter.convertRowIndexToView(index[j]);
            if (v != -1) {
                view.setSize(v, size[j]);
            }
        }
        return view;
    }

    private long total() {
        return length * (long) defaultSize + prefix[count];
    }

    /** The position of entry {@code j}. */
    private long start(int j) {
        return index[j] * (long) defaultSize + prefix[j];
    }

    private void build() {
        if (dirty) {
            if (prefix.length < count + 1) {
                prefix = new long[count + 1];
            }
            long sum = 0;
            for (int j = 0; j < count; j++) {
                prefix[j] = sum;
                sum += size[j] - defaultSize;
            }
            prefix[count] = sum;
            dirty = false;
        }
    }

    private int find(int i) {
        int lo = 0;
        int hi = count - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (index[mid] < i) {
                lo = mid + 1;
            } else if (index[mid] > i) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return -(lo + 1);
    }

    /** The first stored entry at or after row {@code i}. */
    private int lowerBound(int i) {
        int j = find(i);
        return j >= 0 ? j : -1 - j;
    }

    private void insert(int j, int i, int s) {
        if (count == index.length) {
            index = Arrays.copyOf(index, count * 2);
            size = Arrays.copyOf(size, count * 2);
        }
        System.arraycopy(index, j, index, j + 1, count - j);
        System.arraycopy(size, j, size, j + 1, count - j);
        index[j] = i;
        size[j] = s;
        count++;
        dirty = true;
    }

    private void remove(int j, int n) {
        System.arraycopy(index, j + n, index, j, count - j - n);
        System.arraycopy(size, j + n, size, j, count - j - n);
        count -= n;
        dirty = true;
    }
}
