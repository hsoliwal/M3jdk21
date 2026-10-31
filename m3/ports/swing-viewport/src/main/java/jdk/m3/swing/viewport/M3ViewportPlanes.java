/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.swing.viewport;

/**
 * Retained planes of a Swing viewport (M3-PLANES): one origin, and for each scroll the mask of dirty planes.
 * A vertical scroll never dirties HEADER, a horizontal scroll never dirties FROZEN; the first call only sets
 * the origin. Same contract and bit values as the SWT fork's {@code ViewportRuntime} planes behind
 * {@code org.eclipse.swt.widgets.M3Planes}.
 */
public final class M3ViewportPlanes {
    public static final int BODY = 1, FROZEN = 1 << 1, HEADER = 1 << 2, EDITOR = 1 << 3, SCROLLBAR = 1 << 4, FEEDBACK = 1 << 5,
            ALL = BODY | FROZEN | HEADER | EDITOR | SCROLLBAR | FEEDBACK;
    private double originX, originY;
    private boolean initialized;

    /** Sets the origin without dirtying any plane. */
    public void initialize(double x, double y) {
        requireFinite(x, y);
        originX = x;
        originY = y;
        initialized = true;
    }

    /** Moves the origin; returns the dirty plane mask (0 when nothing moved or on the first call). */
    public int scrollTo(double x, double y) {
        requireFinite(x, y);
        if (!initialized) {
            initialize(x, y);
            return 0;
        }
        boolean horizontal = Double.doubleToLongBits(originX) != Double.doubleToLongBits(x);
        boolean vertical = Double.doubleToLongBits(originY) != Double.doubleToLongBits(y);
        originX = x;
        originY = y;
        return (horizontal ? BODY | HEADER | EDITOR | FEEDBACK | SCROLLBAR : 0) | (vertical ? BODY | FROZEN | EDITOR | FEEDBACK | SCROLLBAR : 0);
    }

    public double originX() { return originX; }

    public double originY() { return originY; }

    private static void requireFinite(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("non-finite origin");
    }
}
