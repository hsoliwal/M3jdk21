// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.cpu;

import java.nio.file.Path;
import java.util.Objects;

/** Optional native interval intersection. The pure-Java interval owner never loads JNI. */
public final class M3IntIntervalAlgebraNative {
    private static final boolean ENABLED;
    static {
        String selected = System.getProperty("m3.cpu.interval.algebra.native.path");
        if (selected == null || selected.isBlank()) {
            ENABLED = false;
        } else {
            Path path = Path.of(selected);
            if (!path.isAbsolute()) throw new IllegalArgumentException("native path must be absolute");
            System.load(path.toString());
            ENABLED = true;
        }
    }

    private M3IntIntervalAlgebraNative() { }

    /** Native two-pointer intersection; result revalidated by existing half-open owner. */
    public static M3IntIntervalSet intersection(M3IntIntervalSet left, M3IntIntervalSet right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (!ENABLED) throw new IllegalStateException("native interval algebra backend unavailable");
        return M3IntIntervalSet.copyOf(intersect0(left.toPairs(), right.toPairs()));
    }

    private static native int[] intersect0(int[] leftEndpoints, int[] rightEndpoints);
}
