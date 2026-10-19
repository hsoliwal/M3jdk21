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
 * The rows of a list model as pieces of the rows measured at the last layout: each piece is a run
 * of consecutive old rows, or a run of new rows ({@link #NEW}) that must be measured. List data
 * events edit the pieces in O(pieces); the next layout copies the old measurements piece by piece
 * in one O(n) pass and measures only the new runs. A piece table, as in text editors (and the gap
 * buffer of {@code javax.swing.text.GapContent}); chosen over shifting the lanes on every event by
 * measurement (m3/docs/swing-viewport/ALGORITHM_CATALOGUE.tsv). Design: DESIGN.md section 10.
 */
//FLLDCIM: M3 swing-viewport leaf 4 - piece table over the rows measured at the last layout
final class M3RowPieces {

    /** The start of a piece of rows that were not measured at the last layout. */
    static final int NEW = -1;

    private int[] start = new int[8];
    private int[] len = new int[8];
    private int count;
    private int size;

    /** Forgets every edit: rows {@code 0 .. n - 1} are the old rows in order. */
    void reset(int n) {
        count = 0;
        size = n;
        if (n > 0) {
            start[0] = 0;
            len[0] = n;
            count = 1;
        }
    }

    /** The number of rows after the edits. */
    int size() {
        return size;
    }

    /** The number of pieces. */
    int count() {
        return count;
    }

    /** The old row of the first row of piece {@code i}, or {@link #NEW}. */
    int start(int i) {
        return start[i];
    }

    /** The number of rows in piece {@code i}. */
    int length(int i) {
        return len[i];
    }

    /** Inserts {@code k > 0} new rows before row {@code at}, {@code 0 <= at <= size()}. */
    void insert(int at, int k) {
        int i = split(at);
        if (i > 0 && start[i - 1] == NEW) {
            len[i - 1] += k;
        } else if (i < count && start[i] == NEW) {
            len[i] += k;
        } else {
            open(i);
            start[i] = NEW;
            len[i] = k;
        }
        size += k;
    }

    /** Removes rows {@code from .. to}, {@code 0 <= from <= to < size()}. */
    void remove(int from, int to) {
        int i = split(from);
        int j = split(to + 1);
        System.arraycopy(start, j, start, i, count - j);
        System.arraycopy(len, j, len, i, count - j);
        count -= j - i;
        size -= to - from + 1;
    }

    /** Marks rows {@code from .. to} as new, {@code 0 <= from <= to < size()}. */
    void change(int from, int to) {
        remove(from, to);
        insert(from, to - from + 1);
    }

    /** The index of the piece that starts at row {@code row}, splitting the piece that holds it. */
    private int split(int row) {
        int first = 0;
        for (int i = 0; i < count; i++) {
            int l = len[i];
            if (row == first) {
                return i;
            }
            if (row < first + l) {
                int head = row - first;
                open(i + 1);
                start[i + 1] = start[i] == NEW ? NEW : start[i] + head;
                len[i + 1] = l - head;
                len[i] = head;
                return i + 1;
            }
            first += l;
        }
        return count;
    }

    /** Makes room for one piece at index {@code i}. */
    private void open(int i) {
        if (count == start.length) {
            start = java.util.Arrays.copyOf(start, count * 2);
            len = java.util.Arrays.copyOf(len, count * 2);
        }
        System.arraycopy(start, i, start, i + 1, count - i);
        System.arraycopy(len, i, len, i + 1, count - i);
        count++;
    }
}
