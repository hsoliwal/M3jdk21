// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.nio.file.Path;
import java.util.Objects;

/** Optional JNI batch membership; Java interval queries work without a native backend. */
public final class M3IntIntervalSetNative {
    private static final boolean ENABLED;
    static {
        String selected = System.getProperty("m3.cpu.interval.native.path");
        if (selected == null || selected.isBlank()) {
            ENABLED = false;
        } else {
            Path path = Path.of(selected);
            if (!path.isAbsolute()) throw new IllegalArgumentException("native path must be absolute");
            System.load(path.toString());
            ENABLED = true;
        }
    }

    private M3IntIntervalSetNative() { }

    public static byte[] contains(M3IntIntervalSet set, int[] queries) {
        Objects.requireNonNull(set, "set");
        Objects.requireNonNull(queries, "queries");
        if (!ENABLED) throw new IllegalStateException("native interval backend unavailable");
        // Both arrays are detached before JNI; callers cannot mutate the canonical owner.
        return contains0(set.toPairs(), queries.clone());
    }

    private static native byte[] contains0(int[] pairs, int[] queries);
}
