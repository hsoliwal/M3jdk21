// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Explicit opt-in JNI batch frequency queries over M3RunLengthPostingIndex.
 * Ordinary index operations are Java-only and never load a native library.
 */
public final class M3RunLengthPostingIndexNative {
    private static final String LIBRARY_PROPERTY = "m3.cpu.runlength.native.path";
    private static final boolean ENABLED = load();

    private M3RunLengthPostingIndexNative() { }

    private static boolean load() {
        String path = System.getProperty(LIBRARY_PROPERTY);
        if (path == null) return false;
        if (path.isBlank() || !Path.of(path).isAbsolute()) {
            throw new IllegalArgumentException("native library requires an absolute path");
        }
        System.load(path);
        return true;
    }

    /**
     * Return one detached count per query, preserving the query order.
     * Query array is snapshotted to prevent retaining a mutable caller alias.
     */
    public static int[] batchCounts(M3RunLengthPostingIndex index, int[] targets) {
        Objects.requireNonNull(index, "index");
        Objects.requireNonNull(targets, "targets");
        if (targets.length > M3RunLengthPostingIndex.MAX_ITEMS) {
            throw new IllegalArgumentException("too many native queries");
        }
        if (!ENABLED) throw new IllegalStateException("native backend not configured");
        return batchCounts0(index.ownedDistinctValues(), index.ownedRunEnds(), targets.clone());
    }

    private static native int[] batchCounts0(int[] values, int[] ends, int[] targets);
}
