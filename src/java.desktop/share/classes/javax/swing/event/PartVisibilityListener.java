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
 * Notified when a component becomes visible to the user as part of the screen, or stops being so
 * (M3JDK, brief rule 25; the Swing side of SWT's {@code PartVisibilityListener}). A component is
 * <em>part-visible</em> when {@link javax.swing.JComponent#isPartVisible()} answers true: it is showing,
 * it and every ancestor up to its window have a non-zero size, no ancestor is marked part-hidden, and
 * its window is not iconified.
 * <p>
 * One event is sent per real transition, only to components that have a listener and whose state
 * changed, ancestors before descendants. Nothing is sent when the first listener is added, for a change
 * that leaves the state as it was, or when the component is removed from a hierarchy that was not
 * showing anyway. A view uses it to defer work while hidden: model events update its data, and the
 * one repaint happens on {@link #partVisible}.
 *
 * @see javax.swing.JComponent#addPartVisibilityListener
 * @see PartVisibilityEvent
 * @see PartVisibilityAdapter
 */
public interface PartVisibilityListener extends EventListener {

    /**
     * The component became part-visible.
     *
     * @param e the event; its cause and origin say what changed
     */
    void partVisible(PartVisibilityEvent e);

    /**
     * The component stopped being part-visible.
     *
     * @param e the event; its cause and origin say what changed
     */
    void partHidden(PartVisibilityEvent e);
}
