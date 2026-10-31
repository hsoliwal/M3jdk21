/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

/**
 * Primitive, half-open row coordinates shared by the existing M3 viewport owners.
 * No row objects, entries, listeners, controls or rendering surfaces are created.
 * All coordinates are view indices; a sorter/filter may map them to model indices.
 */
public final class M3ViewportIndexWindow {
    public static final int FIRST_VISIBLE = 0;
    public static final int END_VISIBLE = 1;
    public static final int FIRST_RESIDENT = 2;
    public static final int END_RESIDENT = 3;
    public static final int LANES = 4;

    private M3ViewportIndexWindow() { }

    /**
     * Writes [visibleStart, visibleEnd, residentStart, residentEnd] to a caller-owned
     * int[] at {@code offset}. Each pair is half-open and bounded by the logical count.
     * A zero-height or hidden viewport produces zero resident work. Inputs and capacity
     * are validated before the first write; additions use long to avoid int overflow.
     */
    public static void project(int count, int first, int visible, int overscan,
                               int[] output, int offset) {
        if (output == null) throw new NullPointerException("output");
        if (count < 0 || first < 0 || visible < 0 || overscan < 0) {
            throw new IllegalArgumentException("negative row geometry");
        }
        if (offset < 0 || offset > output.length - LANES) {
            throw new IndexOutOfBoundsException("output window");
        }
        int start = Math.min(first, count);
        int end = visible == 0 ? start : (int)Math.min((long)count, (long)start + visible);
        int residentStart = visible == 0 || start == end ? start : Math.max(0, start - overscan);
        int residentEnd = visible == 0 || start == end ? end
                : (int)Math.min((long)count, (long)end + overscan);
        output[offset + FIRST_VISIBLE] = start;
        output[offset + END_VISIBLE] = end;
        output[offset + FIRST_RESIDENT] = residentStart;
        output[offset + END_RESIDENT] = residentEnd;
    }
}
