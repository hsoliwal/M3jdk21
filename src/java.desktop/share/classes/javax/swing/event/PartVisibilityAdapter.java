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

import java.util.function.Consumer;

/**
 * An empty {@link PartVisibilityListener}, to override only the method needed, with two factory
 * methods for a single callback (the same helpers as SWT's {@code PartVisibilityAdapter}).
 *
 * @see javax.swing.JComponent#addPartVisibilityListener
 */
public abstract class PartVisibilityAdapter implements PartVisibilityListener {

    /** Creates an adapter whose methods do nothing. */
    protected PartVisibilityAdapter() {
    }

    @Override
    public void partVisible(PartVisibilityEvent e) {
    }

    @Override
    public void partHidden(PartVisibilityEvent e) {
    }

    /**
     * A listener that calls {@code c} when the component becomes part-visible.
     *
     * @param c the callback; not null
     * @return the listener
     * @throws NullPointerException if {@code c} is null
     */
    public static PartVisibilityListener partVisibleAdapter(Consumer<PartVisibilityEvent> c) {
        java.util.Objects.requireNonNull(c);
        return new PartVisibilityAdapter() {
            @Override
            public void partVisible(PartVisibilityEvent e) {
                c.accept(e);
            }
        };
    }

    /**
     * A listener that calls {@code c} when the component stops being part-visible.
     *
     * @param c the callback; not null
     * @return the listener
     * @throws NullPointerException if {@code c} is null
     */
    public static PartVisibilityListener partHiddenAdapter(Consumer<PartVisibilityEvent> c) {
        java.util.Objects.requireNonNull(c);
        return new PartVisibilityAdapter() {
            @Override
            public void partHidden(PartVisibilityEvent e) {
                c.accept(e);
            }
        };
    }
}
