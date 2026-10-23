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

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.CellRendererPane;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;

/**
 * The M3 measurement path of {@code BasicListUI.updateLayoutState} ({@code -Dm3.swing.viewport=true},
 * off by default). The stock method measures every row on every layout, so one inserted row costs a
 * renderer call per row of the list. Here the heights and widths measured at the last layout are
 * kept, list data events are recorded as {@link M3RowPieces}, and the next layout measures only the
 * inserted and changed rows, plus the last kept row as a probe, measured in the stock order so that
 * the renderer ends configured for the last row as in the stock loop. A probe that measures
 * differently, or any change of the model, renderer, font, orientation or renderer properties that
 * set sizes, measures every row as the stock method does.
 * <p>
 * Only {@code DefaultListCellRenderer} and its {@code UIResource} take this path: their size
 * depends on the value, the list's font and orientation and the renderer's own properties, not on
 * the row index. Vertical layout, variable row heights, an enabled list and a UI class in
 * {@code java.desktop} are required; everything else keeps the stock loop. The one known
 * difference from stock is a value changed without a list data event (a {@code ListModel}
 * contract violation): stock re-measures it at the next layout after any later change, this path
 * only when its row gets an event or every row is re-measured. Design and proof:
 * m3/docs/swing-viewport/DESIGN.md section 10.
 */
//FLLDCIM: M3 swing-viewport leaf 4 - JList measures only the rows that list data events touched
public final class M3ListMeasure {

    /** Pieces beyond this many make the next layout measure every row. */
    static final int MAX_PIECES = 1024;

    private static final Module DESKTOP = M3ListMeasure.class.getModule();
    private static final int[] NONE = new int[0];

    private final M3RowPieces pieces = new M3RowPieces();
    private boolean valid;
    private int[] heights = NONE;
    private int[] widths = NONE;
    private int cellWidth;
    private Object model;
    private Object renderer;
    private Font font;
    private ComponentOrientation orientation;
    private Object htmlDisable;
    private Object labelUI;
    private Dimension preferred;
    private int gap;
    private int textPositions;
    private int alignments;
    private int remeasured;
    private int fullMeasuresOnDeltas;

    /** Creates the per-UI state; the first layout measures every row. */
    public M3ListMeasure() {
    }

    /**
     * Keeps the kept sizes usable while {@code ok}; a delta event that makes them unusable counts as
     * a full measure caused by a delta (the rule 24 counter).
     */
    private boolean track(boolean ok) {
        if (valid && !ok) {
            fullMeasuresOnDeltas++;
        }
        valid = valid && ok;
        return valid;
    }

    /** Full measures that delta events caused (untrackable indices, the piece cap, a probe mismatch), for the differentials. */
    public int fullMeasuresOnDeltas() {
        return fullMeasuresOnDeltas;
    }

    /** Records {@code ListDataListener.intervalAdded}. */
    public void added(int index0, int index1) {
        int a = Math.min(index0, index1);
        int k = Math.max(index0, index1) - a + 1;
        if (track(a >= 0 && a <= pieces.size() && k > 0)) {
            pieces.insert(a, k);
            track(pieces.count() <= MAX_PIECES);
        }
    }

    /** Records {@code ListDataListener.intervalRemoved}. */
    public void removed(int index0, int index1) {
        int a = Math.min(index0, index1);
        int b = Math.max(index0, index1);
        if (track(a >= 0 && b < pieces.size())) {
            pieces.remove(a, b);
            track(pieces.count() <= MAX_PIECES);
        }
    }

    /** Records {@code ListDataListener.contentsChanged}; an index below 0 means every row. */
    public void changed(int index0, int index1) {
        int a = Math.min(index0, index1);
        int b = Math.max(index0, index1);
        if (track(a >= 0 && b < pieces.size())) {
            pieces.change(a, b);
            track(pieces.count() <= MAX_PIECES);
        }
    }

    /**
     * Measures the rows of {@code list} for {@code ui}'s {@code updateLayoutState}; false leaves the
     * layout to the stock loop. When true, {@link #cellWidth()} and {@link #cellHeights()} hold the
     * values the stock loop would have computed.
     */
    public boolean measure(Object ui, JList<Object> list, CellRendererPane pane) {
        ListCellRenderer<Object> r = list.getCellRenderer();
        if (!M3ListRows.ENABLED || ui.getClass().getModule() != DESKTOP || list.getFixedCellHeight() != -1
                || !list.isEnabled() || r == null || r.getClass() != DefaultListCellRenderer.class
                && r.getClass() != DefaultListCellRenderer.UIResource.class) {
            valid = false;
            return false;
        }
        DefaultListCellRenderer label = (DefaultListCellRenderer) r;
        ListModel<Object> m = list.getModel();
        int n = m.getSize();
        if (!(valid && pieces.size() == n && sameInputs(list, label) && remeasure(list, m, label, pane, n))) {
            measureAll(list, m, label, pane, n);
        }
        int fixed = list.getFixedCellWidth();
        int w = fixed;
        if (fixed == -1) {
            for (int i = 0; i < n; i++) {
                w = Math.max(widths[i], w);
            }
        }
        cellWidth = w;
        return true;
    }

    /** The stock {@code cellWidth} after {@link #measure}. */
    public int cellWidth() {
        return cellWidth;
    }

    /** A new array of the row heights after {@link #measure}, never written afterwards. */
    public int[] cellHeights() {
        return heights;
    }

    /** Rows measured by the last {@link #measure}, for tests and benchmarks. */
    public int remeasured() {
        return remeasured;
    }

    private void measureAll(JList<Object> list, ListModel<Object> m, DefaultListCellRenderer r,
            CellRendererPane pane, int n) {
        int[] h = new int[n];
        int[] w = new int[n];
        for (int i = 0; i < n; i++) {
            Dimension d = size(list, m, r, pane, i);
            w[i] = d.width;
            h[i] = d.height;
        }
        remeasured = n;
        keep(list, r, h, w, n);
    }

    /**
     * Copies the kept rows and measures the new ones; false when nothing was kept or the probe (the
     * last kept row) measures differently, so every row must be measured.
     */
    private boolean remeasure(JList<Object> list, ListModel<Object> m, DefaultListCellRenderer r,
            CellRendererPane pane, int n) {
        int[] h = new int[n];
        int[] w = new int[n];
        int probe = -1;
        for (int i = 0, row = 0; i < pieces.count(); row += pieces.length(i), i++) {
            int s = pieces.start(i);
            if (s != M3RowPieces.NEW) {
                System.arraycopy(heights, s, h, row, pieces.length(i));
                System.arraycopy(widths, s, w, row, pieces.length(i));
                probe = row + pieces.length(i) - 1;
            }
        }
        if (probe < 0) {
            return false;
        }
        if (probe < n - 1 && !same(list, m, r, pane, probe, h, w)) {
            fullMeasuresOnDeltas++;
            return false;
        }
        int count = probe < n - 1 ? 1 : 0;
        for (int i = 0, row = 0; i < pieces.count(); row += pieces.length(i), i++) {
            if (pieces.start(i) == M3RowPieces.NEW) {
                for (int j = row; j < row + pieces.length(i); j++) {
                    Dimension d = size(list, m, r, pane, j);
                    w[j] = d.width;
                    h[j] = d.height;
                }
                count += pieces.length(i);
            }
        }
        if (probe == n - 1 && !same(list, m, r, pane, probe, h, w)) {
            fullMeasuresOnDeltas++;
            return false;
        }
        remeasured = count + (probe == n - 1 ? 1 : 0);
        keep(list, r, h, w, n);
        return true;
    }

    /** Whether row {@code row} still measures as recorded in {@code h} and {@code w}. */
    private static boolean same(JList<Object> list, ListModel<Object> m, DefaultListCellRenderer r,
            CellRendererPane pane, int row, int[] h, int[] w) {
        Dimension d = size(list, m, r, pane, row);
        return d.width == w[row] && d.height == h[row];
    }

    /** The stock measurement of one row. */
    private static Dimension size(JList<Object> list, ListModel<Object> m, DefaultListCellRenderer r,
            CellRendererPane pane, int row) {
        Object value = m.getElementAt(row);
        Component c = r.getListCellRendererComponent(list, value, row, false, false);
        pane.add(c);
        return c.getPreferredSize();
    }

    private void keep(JList<Object> list, DefaultListCellRenderer r, int[] h, int[] w, int n) {
        heights = h;
        widths = w;
        pieces.reset(n);
        model = list.getModel();
        renderer = r;
        font = list.getFont();
        orientation = list.getComponentOrientation();
        htmlDisable = r.getClientProperty("html.disable");
        labelUI = r.getUI();
        preferred = r.isPreferredSizeSet() ? r.getPreferredSize() : null;
        gap = r.getIconTextGap();
        textPositions = r.getHorizontalTextPosition() * 31 + r.getVerticalTextPosition();
        alignments = r.getHorizontalAlignment() * 31 + r.getVerticalAlignment();
        valid = true;
    }

    /** Whether everything besides the values that sets the size of a row is as at the last layout. */
    private boolean sameInputs(JList<Object> list, DefaultListCellRenderer r) {
        return model == list.getModel() && renderer == r && font == list.getFont()
                && orientation == list.getComponentOrientation()
                && htmlDisable == r.getClientProperty("html.disable") && labelUI == r.getUI()
                && java.util.Objects.equals(preferred, r.isPreferredSizeSet() ? r.getPreferredSize() : null)
                && gap == r.getIconTextGap()
                && textPositions == r.getHorizontalTextPosition() * 31 + r.getVerticalTextPosition()
                && alignments == r.getHorizontalAlignment() * 31 + r.getVerticalAlignment();
    }
}
