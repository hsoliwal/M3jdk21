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

import java.util.EventListener;

/**
 * Notified when the window of rows a view shows changes (M3JDK, brief rule 25; the Swing side of SWT's
 * {@code ViewportListener}). JList, JTree and JTable, and the views built on them, fire it once per coalesced change:
 * one event per event dispatch turn, after layout and before paint, never while the view is not part-visible
 * ({@link javax.swing.JComponent#isPartVisible()}). The first event after the view becomes part-visible again
 * carries {@link ViewportEvent#VISIBILITY} and the difference from the last window reported. Nothing is fired when a
 * listener is added, and a view without a listener tracks nothing. Other components never fire it.
 * <p>
 * A lazily loading client fetches the {@linkplain ViewportEvent#getEntered() entered} rows and releases the
 * {@linkplain ViewportEvent#getLeft() left} ones; sticky headers and prefetching read the first and last rows.
 *
 * @see javax.swing.JComponent#addViewportListener
 * @see ViewportEvent
 */
public interface ViewportListener extends EventListener {

    /**
     * The window of rows the view shows, or its extents, changed.
     *
     * @param e the event
     */
    void viewportChanged(ViewportEvent e);
}
