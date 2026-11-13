// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.nio.file.Path;
import java.util.Objects;

/** Explicit opt-in JNI rank selection. No default native load and no implicit fallback. */
public final class M3RankedByteSubstringsNative {
    private static final boolean ENABLED;
    static {
        String path = System.getProperty("m3.cpu.ranked.native.path");
        if (path == null || path.isEmpty()) {
            ENABLED = false;
        } else {
            if (!Path.of(path).isAbsolute())
                throw new IllegalArgumentException("native library path must be absolute");
            System.load(path);
            ENABLED = true;
        }
    }

    private M3RankedByteSubstringsNative() { }

    public static boolean isAvailable() { return ENABLED; }

    /** Exact detached (offset,length) pairs; validate the rank contract first. */
    public static int[] selectSpans(M3RankedByteSubstrings index, long[] ranks) {
        Objects.requireNonNull(index, "index");
        index.validateBatch(ranks);
        if (!ENABLED) throw new IllegalStateException("ranked native backend not configured");
        return select0(index.suffixCopy(), index.lcpCopy(), index.cumulativeCopy(), ranks);
    }

    private static native int[] select0(int[] suffixes, int[] lcp,
                                       long[] cumulative, long[] ranks);
}
