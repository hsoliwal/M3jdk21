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
package javax.swing;

import java.util.Arrays;

/**
 * An immutable range of row or item indices, from {@code start} to {@code end},
 * both included; the range is empty when {@code end < start}.
 * <p>
 * Selections and visible rows of {@link JList}, {@link JTable} and {@link JTree}
 * are reported as ranges ({@code getSelectionRanges}, {@code getVisibleRange}),
 * so a selection of a million consecutive rows is one object, not a million
 * indices. The normal form of a set of indices is an ascending array of
 * disjoint, non-adjacent ranges ({@link #coalesce(int[])}).
 * <p>
 * This is the Swing counterpart of the SWT enhanced API type of the same name.
 *
 * @since 21
 */
public final class M3ItemRange {

    /** The empty range. */
    public static final M3ItemRange EMPTY = new M3ItemRange(0, -1);

    private static final M3ItemRange[] NO_RANGES = new M3ItemRange[0];

    private final int start;
    private final int end;

    /**
     * Constructs the range of the indices from {@code start} to {@code end},
     * both included; the range is empty when {@code end < start}.
     *
     * @param start the first index, not negative
     * @param end the last index
     * @throws IllegalArgumentException if {@code start} is negative
     */
    public M3ItemRange(int start, int end) {
        if (start < 0) {
            throw new IllegalArgumentException("negative start: " + start);
        }
        this.start = start;
        this.end = end;
    }

    /**
     * Returns the range of the indices from {@code start} to {@code end},
     * both included, or {@link #EMPTY} when {@code end < start}.
     *
     * @param start the first index, not negative
     * @param end the last index
     * @return the range
     * @throws IllegalArgumentException if {@code start} is negative
     */
    public static M3ItemRange of(int start, int end) {
        if (start < 0) {
            throw new IllegalArgumentException("negative start: " + start);
        }
        return end < start ? EMPTY : new M3ItemRange(start, end);
    }

    /**
     * Returns the first index.
     *
     * @return the first index
     */
    public int start() {
        return start;
    }

    /**
     * Returns the last index; smaller than {@link #start()} for an empty range.
     *
     * @return the last index
     */
    public int end() {
        return end;
    }

    /**
     * Returns the number of indices in the range.
     *
     * @return the size, 0 when empty
     */
    public int size() {
        return end < start ? 0 : end - start + 1;
    }

    /**
     * Returns whether the range has no index.
     *
     * @return {@code true} when empty
     */
    public boolean isEmpty() {
        return end < start;
    }

    /**
     * Returns whether the range contains an index.
     *
     * @param index the index
     * @return {@code true} when {@code start <= index <= end}
     */
    public boolean contains(int index) {
        return start <= index && index <= end;
    }

    /**
     * Returns the indices common to this range and another one.
     *
     * @param other the other range
     * @return the intersection, possibly {@link #EMPTY}
     * @throws NullPointerException if {@code other} is null
     */
    public M3ItemRange intersection(M3ItemRange other) {
        return of(Math.max(start, other.start), Math.min(end, other.end));
    }

    /**
     * Returns the number of indices in the ranges.
     *
     * @param ranges the ranges
     * @return the sum of their sizes
     * @throws NullPointerException if the array or an element is null
     */
    public static int countOf(M3ItemRange[] ranges) {
        int count = 0;
        for (M3ItemRange range : ranges) {
            count += range.size();
        }
        return count;
    }

    /**
     * Returns the normal form of a set of indices: ascending, disjoint,
     * non-adjacent ranges. Repeated indices count once.
     *
     * @param indices the indices, in any order
     * @return the ranges
     * @throws NullPointerException if the array is null
     * @throws IllegalArgumentException if an index is negative
     */
    public static M3ItemRange[] coalesce(int[] indices) {
        if (indices.length == 0) {
            return NO_RANGES;
        }
        int[] sorted = indices.clone();
        Arrays.sort(sorted);
        if (sorted[0] < 0) {
            throw new IllegalArgumentException("negative index: " + sorted[0]);
        }
        int runs = 1;
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i] > sorted[i - 1] + 1) {
                runs++;
            }
        }
        M3ItemRange[] ranges = new M3ItemRange[runs];
        int run = 0;
        int first = sorted[0];
        for (int i = 1; i <= sorted.length; i++) {
            if (i < sorted.length && sorted[i] <= sorted[i - 1] + 1) {
                continue;
            }
            ranges[run++] = new M3ItemRange(first, sorted[i - 1]);
            if (i < sorted.length) {
                first = sorted[i];
            }
        }
        return ranges;
    }

    /*
     * The runs of a selection model as ranges: the bit set of a
     * DefaultListSelectionModel is walked run by run, any other model is
     * asked index by index between its minimum and maximum.
     */
    static M3ItemRange[] ranges(ListSelectionModel model) {
        if (model.getClass() == DefaultListSelectionModel.class) {
            return ((DefaultListSelectionModel) model).m3SelectionRanges();
        }
        int min = model.getMinSelectionIndex();
        int max = model.getMaxSelectionIndex();
        if (min < 0 || max < min) {
            return NO_RANGES;
        }
        int[] runs = new int[8];
        int count = 0;
        int index = min;
        while (index <= max) {
            while (index <= max && !model.isSelectedIndex(index)) {
                index++;
            }
            if (index > max) {
                break;
            }
            int first = index;
            while (index <= max && model.isSelectedIndex(index)) {
                index++;
            }
            if (count + 2 > runs.length) {
                runs = Arrays.copyOf(runs, runs.length * 2);
            }
            runs[count++] = first;
            runs[count++] = index - 1;
        }
        return fromRuns(runs, count);
    }

    /* Ranges from (first, last) pairs packed in an int lane. */
    static M3ItemRange[] fromRuns(int[] runs, int count) {
        M3ItemRange[] ranges = new M3ItemRange[count / 2];
        for (int i = 0; i < ranges.length; i++) {
            ranges[i] = new M3ItemRange(runs[2 * i], runs[2 * i + 1]);
        }
        return ranges;
    }

    /*
     * Replaces the selection of a model with the ranges, as one adjusting
     * change: listeners see a single valueChanged with the union of the
     * changed indices.
     */
    static void select(ListSelectionModel model, M3ItemRange[] ranges, int rowCount) {
        for (M3ItemRange range : ranges) {
            if (!range.isEmpty() && range.end >= rowCount) {
                throw new IllegalArgumentException("range " + range + " beyond row count " + rowCount);
            }
        }
        boolean adjusting = model.getValueIsAdjusting();
        model.setValueIsAdjusting(true);
        try {
            model.clearSelection();
            for (M3ItemRange range : ranges) {
                if (!range.isEmpty()) {
                    model.addSelectionInterval(range.start, range.end);
                }
            }
        } finally {
            model.setValueIsAdjusting(adjusting);
        }
    }

    /**
     * Returns whether another object is a range with the same indices; all
     * empty ranges are equal.
     *
     * @param object the object
     * @return {@code true} when equal
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof M3ItemRange other)) {
            return false;
        }
        if (isEmpty() && other.isEmpty()) {
            return true;
        }
        return start == other.start && end == other.end;
    }

    @Override
    public int hashCode() {
        return isEmpty() ? -1 : start * 31 + end;
    }

    @Override
    public String toString() {
        return isEmpty() ? "M3ItemRange []" : "M3ItemRange [" + start + ".." + end + "]";
    }
}
