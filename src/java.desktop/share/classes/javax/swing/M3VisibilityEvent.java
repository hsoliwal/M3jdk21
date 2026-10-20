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

import java.io.Serial;
import java.util.EventObject;

/**
 * A change of the viewport of a {@link JList}, {@link JTable} or
 * {@link JTree}: rows that entered or left the visible range, new extents,
 * or the viewport becoming hidden or shown. The SWT twin is
 * {@code org.eclipse.swt.widgets.M3VisibilityObserver}, whose
 * {@code Event.detail} values have the same names.
 * <p>
 * A listener receives, in order, {@link #LEFT} for the rows that left the
 * visible range and {@link #ENTERED} for the rows that entered it (each as at
 * most two ranges, {@link #getStart()} to {@link #getEnd()} inclusive), then
 * {@link #EXTENTS} when the extents changed. {@link #HIDDEN} and
 * {@link #SHOWN} mark the hidden periods of the viewport: nothing else is
 * announced while it is hidden, and a show announces {@code SHOWN}, the
 * window delta since the last announced range and the new extents. An
 * application prefetches what entered, releases what left and re-plans on new
 * extents, instead of polling the scroll position.
 *
 * @see JTable#addVisibilityListener(M3VisibilityListener)
 * @since 21
 */
public final class M3VisibilityEvent extends EventObject {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The rows from {@code start} to {@code end} came into view. */
    public static final int ENTERED = 0;
    /** The rows from {@code start} to {@code end} left the view. */
    public static final int LEFT = 1;
    /**
     * The extents of the viewport changed: {@link #getWidth()} and
     * {@link #getHeight()} are the size of the viewport, {@link #getCount()}
     * the rows.
     */
    public static final int EXTENTS = 2;
    /** The viewport became hidden; nothing else follows until {@link #SHOWN}. */
    public static final int HIDDEN = 3;
    /**
     * The viewport became visible. During this event {@code isViewportDirty()}
     * answers whether the rows changed while it was hidden; the window delta
     * and the extents follow.
     */
    public static final int SHOWN = 4;

    private final int detail;
    private final int start;
    private final int end;
    private final int width;
    private final int height;
    private final int count;

    /**
     * Creates an event.
     *
     * @param source the component
     * @param detail one of {@link #ENTERED}, {@link #LEFT}, {@link #EXTENTS},
     *        {@link #HIDDEN}, {@link #SHOWN}
     * @param start the first row of a range, else 0
     * @param end the last row of a range, else -1
     * @param width the width of the viewport, for {@link #EXTENTS}
     * @param height the height of the viewport, for {@link #EXTENTS}
     * @param count the rows, for {@link #EXTENTS}
     * @throws IllegalArgumentException if {@code source} is null or
     *         {@code detail} is not one of the five
     */
    public M3VisibilityEvent(JComponent source, int detail, int start, int end, int width, int height, int count) {
        super(source);
        if (detail < ENTERED || detail > SHOWN) {
            throw new IllegalArgumentException("detail " + detail);
        }
        this.detail = detail;
        this.start = start;
        this.end = end;
        this.width = width;
        this.height = height;
        this.count = count;
    }

    /**
     * Returns what changed.
     *
     * @return one of {@link #ENTERED}, {@link #LEFT}, {@link #EXTENTS},
     *         {@link #HIDDEN}, {@link #SHOWN}
     */
    public int getDetail() {
        return detail;
    }

    /**
     * Returns the first row of the range.
     *
     * @return the first row of {@link #ENTERED} or {@link #LEFT}, else 0
     */
    public int getStart() {
        return start;
    }

    /**
     * Returns the last row of the range, inclusive.
     *
     * @return the last row of {@link #ENTERED} or {@link #LEFT}, else -1
     */
    public int getEnd() {
        return end;
    }

    /**
     * Returns the width of the viewport.
     *
     * @return the width, for {@link #EXTENTS}
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of the viewport.
     *
     * @return the height, for {@link #EXTENTS}
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the rows (the visible rows of a tree).
     *
     * @return the rows, for {@link #EXTENTS}
     */
    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        String name = switch (detail) {
            case ENTERED -> "ENTERED " + start + ".." + end;
            case LEFT -> "LEFT " + start + ".." + end;
            case EXTENTS -> "EXTENTS " + width + "x" + height + ", " + count + " rows";
            case HIDDEN -> "HIDDEN";
            default -> "SHOWN";
        };
        return getClass().getName() + "[" + name + "]";
    }
}
