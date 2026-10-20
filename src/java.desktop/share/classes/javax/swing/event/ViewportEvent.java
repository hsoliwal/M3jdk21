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

package javax.swing.event;

import java.io.Serial;
import java.util.EventObject;

/**
 * Sent to a {@link ViewportListener} when the window of rows a view shows changes. Rows are indices in the view's
 * flattened visible order: list indices for a JList, rows for a JTree, view rows for a JTable. Ranges are int pairs,
 * never one object per row: {@code [start0, end0, start1, end1, ...]}, ascending and coalesced, with inclusive ends,
 * and an empty array when there is nothing. {@link #getEntered()} uses the current indices.
 * {@link #getLeft()} uses the indices of the previous event, the window the listener was last told about, so a row
 * that a model change only moved is in neither. The fields and constants are those of SWT's {@code ViewportEvent}.
 *
 * @see javax.swing.JComponent#addViewportListener
 */
public class ViewportEvent extends EventObject {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The view scrolled. */
    public static final int SCROLL = 1;

    /** The viewport changed size. */
    public static final int RESIZE = 2;

    /** Row sizes or the layout changed without a model change (row height, font, orientation). */
    public static final int LAYOUT = 4;

    /** The model inserted, removed or changed rows. */
    public static final int MODEL_DELTA = 8;

    /** The view became part-visible; the event carries the difference from the last window reported. */
    public static final int VISIBILITY = 16;

    /** A tree node was expanded or collapsed. */
    public static final int EXPAND = 32;

    private static final int[] NONE = {};

    private final int firstVisible;
    private final int lastVisible;
    private final int[] entered;
    private final int[] left;
    private final boolean extentsChanged;
    private final int cause;

    /**
     * Creates an event.
     *
     * @param source the view
     * @param firstVisible the first row shown, -1 when none
     * @param lastVisible the last row shown, -1 when none
     * @param entered the rows that came into the window, as inclusive pairs in the current indices; null for none
     * @param left the rows that went out of the window, as inclusive pairs in the previous event's indices; null for
     *        none
     * @param extentsChanged whether the scroll extents (the view's size) changed
     * @param cause a bit mask of {@link #SCROLL}, {@link #RESIZE}, {@link #LAYOUT}, {@link #MODEL_DELTA},
     *        {@link #VISIBILITY} and {@link #EXPAND}
     * @throws IllegalArgumentException if {@code source} is null or a pair array has an odd length
     */
    public ViewportEvent(Object source, int firstVisible, int lastVisible, int[] entered, int[] left,
            boolean extentsChanged, int cause) {
        super(source);
        this.firstVisible = firstVisible;
        this.lastVisible = lastVisible;
        this.entered = pairs(entered);
        this.left = pairs(left);
        this.extentsChanged = extentsChanged;
        this.cause = cause;
    }

    private static int[] pairs(int[] p) {
        if (p == null || p.length == 0) {
            return NONE;
        }
        if ((p.length & 1) != 0) {
            throw new IllegalArgumentException("pairs of an odd length " + p.length);
        }
        return p.clone();
    }

    /**
     * The first row shown.
     *
     * @return the row, -1 when nothing is shown
     */
    public int getFirstVisible() {
        return firstVisible;
    }

    /**
     * The last row shown.
     *
     * @return the row, -1 when nothing is shown
     */
    public int getLastVisible() {
        return lastVisible;
    }

    /**
     * The rows that came into the window, in the current indices.
     *
     * @return inclusive pairs {@code [start0, end0, ...]}, ascending; a new array, empty when none
     */
    public int[] getEntered() {
        return entered.length == 0 ? NONE : entered.clone();
    }

    /**
     * The rows that went out of the window, in the indices of the previous event.
     *
     * @return inclusive pairs {@code [start0, end0, ...]}, ascending; a new array, empty when none
     */
    public int[] getLeft() {
        return left.length == 0 ? NONE : left.clone();
    }

    /**
     * Whether the scroll extents (the size of the view in its viewport) changed.
     *
     * @return whether the extents changed
     */
    public boolean isExtentsChanged() {
        return extentsChanged;
    }

    /**
     * What changed.
     *
     * @return a bit mask of {@link #SCROLL}, {@link #RESIZE}, {@link #LAYOUT}, {@link #MODEL_DELTA},
     *         {@link #VISIBILITY} and {@link #EXPAND}
     */
    public int getCause() {
        return cause;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(getClass().getName()).append("[first=").append(firstVisible)
                .append(",last=").append(lastVisible).append(",entered=").append(java.util.Arrays.toString(entered))
                .append(",left=").append(java.util.Arrays.toString(left)).append(",extentsChanged=")
                .append(extentsChanged).append(",cause=");
        String[] names = {"SCROLL", "RESIZE", "LAYOUT", "MODEL_DELTA", "VISIBILITY", "EXPAND"};
        String sep = "";
        for (int i = 0; i < names.length; i++) {
            if ((cause & (1 << i)) != 0) {
                b.append(sep).append(names[i]);
                sep = "|";
            }
        }
        return b.append(']').toString();
    }
}
