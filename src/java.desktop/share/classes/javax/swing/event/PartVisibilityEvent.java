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

import java.awt.Component;
import java.io.Serial;
import java.util.EventObject;

/**
 * Sent to a {@link PartVisibilityListener} when its component becomes part-visible or stops being so.
 * The source is the component; {@link #getCause()} says what kind of change caused the transition and
 * {@link #getOrigin()} which component changed (the source itself, an ancestor or the window). The
 * cause constants have the values of SWT's {@code PartVisibilityEvent}.
 *
 * @see javax.swing.JComponent#isPartVisible()
 */
public class PartVisibilityEvent extends EventObject {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The component or an ancestor was shown or hidden (setVisible, a tab or card selected, the window shown). */
    public static final int VISIBILITY = 1;

    /** The component or an ancestor changed between a zero and a non-zero size. */
    public static final int SIZE = 2;

    /** The window was iconified or deiconified. */
    public static final int MINIMIZE = 3;

    /** The component was moved to another parent, or its hierarchy became displayable or not. */
    public static final int REPARENT = 4;

    /** An ancestor was marked part-hidden or unmarked (a collapsible container collapsed or expanded). */
    public static final int CONTAINER = 5;

    private final int cause;
    private final transient Component origin;

    /**
     * Creates an event.
     *
     * @param source the component whose part visibility changed
     * @param cause one of {@link #VISIBILITY}, {@link #SIZE}, {@link #MINIMIZE}, {@link #REPARENT},
     *        {@link #CONTAINER}
     * @param origin the component whose change caused the transition
     * @throws IllegalArgumentException if {@code source} is null or {@code cause} is not one of the constants
     */
    public PartVisibilityEvent(Component source, int cause, Component origin) {
        super(source);
        if (cause < VISIBILITY || cause > CONTAINER) {
            throw new IllegalArgumentException("cause " + cause);
        }
        this.cause = cause;
        this.origin = origin;
    }

    /**
     * The kind of change that caused the transition.
     *
     * @return one of {@link #VISIBILITY}, {@link #SIZE}, {@link #MINIMIZE}, {@link #REPARENT},
     *         {@link #CONTAINER}
     */
    public int getCause() {
        return cause;
    }

    /**
     * The component whose change caused the transition: the source, an ancestor or the window.
     *
     * @return the origin; null after deserialization
     */
    public Component getOrigin() {
        return origin;
    }

    @Override
    public String toString() {
        String[] names = {"", "VISIBILITY", "SIZE", "MINIMIZE", "REPARENT", "CONTAINER"};
        return getClass().getName() + "[cause=" + names[cause] + ",origin=" + (origin == null ? null
                : origin.getClass().getSimpleName()) + ",source=" + getSource().getClass().getSimpleName() + "]";
    }
}
