// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.nio.file.Path;
import java.util.Objects;

/** Explicitly opt-in JNI batch suffix-range queries, without Java fallback or implicit load. */
public final class M3ByteSuffixIndexNative {
    private static final boolean ENABLED;

    static {
        String path = System.getProperty("m3.cpu.suffix.native.path");
        if (path == null || path.isEmpty()) {
            ENABLED = false;
        } else {
            if (!Path.of(path).isAbsolute()) {
                throw new IllegalArgumentException("native library path must be absolute");
            }
            System.load(path);
            ENABLED = true;
        }
    }

    private M3ByteSuffixIndexNative() { }

    public static boolean isAvailable() { return ENABLED; }

    /** Return exact detached (count, first) pairs; validate the batch before requiring native. */
    public static int[] queryBatch(M3ByteSuffixIndex index, byte[] packed, int[] offsets) {
        Objects.requireNonNull(index, "index");
        M3ByteSuffixIndex.validateBatch(packed, offsets);
        if (!ENABLED) throw new IllegalStateException("suffix native backend not configured");
        // All three primitive arrays remain strongly owned by the immutable
        // index for the synchronous JNI call. No O(n) Java snapshots per batch.
        // The native kernel only reads and JNI_ABORT-releases borrowed arrays.
        return index.withBorrowedNativeState(M3ByteSuffixIndexNative::query0,
                packed, offsets);
    }

    private static native int[] query0(byte[] text, int[] suffixes, int[] rankMin,
                                       int leafBase, byte[] packed, int[] offsets);
}
