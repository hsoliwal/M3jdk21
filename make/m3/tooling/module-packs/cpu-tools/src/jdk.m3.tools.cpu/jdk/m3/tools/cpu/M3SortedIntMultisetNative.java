// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.nio.file.Path;
import java.util.Objects;

/** Optional explicit JNI parity path; the Java multiset never loads native code. */
public final class M3SortedIntMultisetNative {
    private static final String LIBRARY_PROPERTY = "m3.cpu.multiset.native.path";
    private static final boolean ENABLED = load();

    private M3SortedIntMultisetNative() { }

    private static boolean load() {
        String path = System.getProperty(LIBRARY_PROPERTY);
        if (path == null) return false;
        if (path.isBlank() || !Path.of(path).isAbsolute()) {
            throw new IllegalArgumentException("native library requires an absolute path");
        }
        System.load(path);
        return true;
    }

    /** Preserve duplicate counts and signed order; output is detached from both inputs. */
    public static int[] intersection(M3SortedIntMultiset left, M3SortedIntMultiset right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (!ENABLED) throw new IllegalStateException("native backend not configured");
        return intersection0(left.ownedValues(), right.ownedValues());
    }

    private static native int[] intersection0(int[] left, int[] right);
}
