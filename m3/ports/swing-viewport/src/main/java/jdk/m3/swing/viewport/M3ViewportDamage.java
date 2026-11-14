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
 * Damage of a Swing viewport (M3-DAMAGE): rectangles are added, the bounding box is taken and cleared.
 * Empty rectangles are ignored. Int lanes only (DOOP). Same answers as the bounding box of the SWT fork's
 * {@code ViewportDamageRegion} behind {@code org.eclipse.swt.widgets.M3DamageTracker}.
 */
public final class M3ViewportDamage {
    private int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;
    private long area;

    /** Adds a damaged rectangle; {@code width <= 0 || height <= 0} is ignored. */
    public void add(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        x0 = Math.min(x0, x);
        y0 = Math.min(y0, y);
        x1 = Math.max(x1, x + width);
        y1 = Math.max(y1, y + height);
        area += (long) width * height;
    }

    public boolean isEmpty() {
        return x0 > x1;
    }

    /** Sum of the added rectangles' areas since the last take (overlaps counted twice). */
    public long addedArea() {
        return area;
    }

    /** Writes {@code x, y, width, height} of the bounding box and clears; false (out untouched) when empty. */
    public boolean take(int[] out) {
        if (isEmpty()) return false;
        out[0] = x0;
        out[1] = y0;
        out[2] = x1 - x0;
        out[3] = y1 - y0;
        x0 = y0 = Integer.MAX_VALUE;
        x1 = y1 = Integer.MIN_VALUE;
        area = 0;
        return true;
    }
}
