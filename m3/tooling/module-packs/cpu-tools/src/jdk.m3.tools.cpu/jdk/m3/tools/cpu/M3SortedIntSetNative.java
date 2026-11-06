// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Optional JNI counterpart to {@link M3SortedIntSet#intersection(M3SortedIntSet)}.
 * <p>Only explicit callers enter this class, and only an explicit absolute library
 * path enables the native function. The pure Java owner has no native dependency.
 * No Java-heap pointer is retained across the JNI call.
 */
public final class M3SortedIntSetNative {
    private static final String LIBRARY_PROPERTY = "m3.cpu.sorted.native.path";
    private static final boolean ENABLED = load();

    private M3SortedIntSetNative() { }

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
     * Exact signed-order intersection, with a fresh detached output.
     * Throws if no native backend was explicitly configured on first class use.
     */
    public static int[] intersection(M3SortedIntSet left, M3SortedIntSet right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (!ENABLED) throw new IllegalStateException("native backend not configured");
        return intersection0(left.ownedValues(), right.ownedValues());
    }

    private static native int[] intersection0(int[] left, int[] right);
}
