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

import java.util.EventListener;

/**
 * Receives the viewport changes of a {@link JList}, {@link JTable} or
 * {@link JTree}: the window delta, the extents, and the hidden and shown
 * transitions ({@link M3VisibilityEvent}). The SWT twin is a
 * {@code Listener} added with
 * {@code org.eclipse.swt.widgets.M3VisibilityObserver.addListener}.
 *
 * @see JTable#addVisibilityListener(M3VisibilityListener)
 * @since 21
 */
public interface M3VisibilityListener extends EventListener {

    /**
     * Called when rows entered or left the visible range, the extents
     * changed, or the viewport became hidden or shown.
     *
     * @param event the change
     */
    void visibilityChanged(M3VisibilityEvent event);
}
