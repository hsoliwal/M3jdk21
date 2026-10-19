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

/**
 * Where {@code scrollRowToVisible(row, alignment)} places a row of a
 * {@link JList}, {@link JTable} or {@link JTree} inside its viewport.
 * <p>
 * The stock {@code scrollRectToVisible} family performs the smallest scroll
 * that shows a row ({@link #MIN_DISTANCE}); the other alignments place the
 * row at the top, in the middle or at the bottom of the viewport, clamped to
 * the content, so an application can show "row 12,345 at the top" with one
 * call instead of computing view positions.
 * <p>
 * This is the Swing counterpart of the SWT enhanced API type of the same
 * name.
 *
 * @see JList#scrollRowToVisible(int, M3Scrolling)
 * @see JTable#scrollRowToVisible(int, M3Scrolling)
 * @see JTree#scrollRowToVisible(int, M3Scrolling)
 * @since 21
 */
public enum M3Scrolling {

    /** The top edge of the row at the top of the viewport. */
    TOP,

    /** The row centered in the viewport (within one pixel). */
    CENTER,

    /** The bottom edge of the row at the bottom of the viewport. */
    BOTTOM,

    /**
     * The smallest scroll that shows the whole row: no scroll when it is
     * already fully visible, otherwise the row is placed at the top when it
     * was above the viewport and at the bottom when it was below it. This is
     * the rule of the stock {@code scrollRectToVisible}.
     */
    MIN_DISTANCE
}
