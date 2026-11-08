/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.search;

/** Opt-in native signed-long bounds backed by bounded C17/JNI primitive array regions. */
public final class M3NativeSortedLongBounds {
    private static volatile boolean nativeLibraryLoaded;

    /** Native library initialization must not run before the Java argument contract is checked. */
    private static void ensureNativeLoaded() {
        if (nativeLibraryLoaded) {
            return;
        }
        synchronized (M3NativeSortedLongBounds.class) {
            if (nativeLibraryLoaded) {
                return;
            }
            String path = System.getProperty("m3.native.sorted.long.search.library");
            if (path == null || path.isBlank()) {
                throw new IllegalStateException(
                        "m3.native.sorted.long.search.library must name a verified native library");
            }
            System.load(path);
            nativeLibraryLoaded = true;
        }
    }

    private M3NativeSortedLongBounds() {
    }

    /**
     * JNI counterpart to {@link M3SortedLongBounds#bounds(long[], int, int, long[])}.
     * Native use must be enabled explicitly and is not automatically selected for performance.
     *
     * @param sorted signed-long source sorted within the requested range
     * @param from start inclusive
     * @param to end exclusive
     * @param keys signed-long query batch
     * @return new interleaved absolute lower/upper bound pairs
     * @throws NullPointerException on null source or keys
     * @throws IndexOutOfBoundsException on invalid range
     * @throws IllegalArgumentException when the result count is too large
     */
    public static int[] bounds(long[] sorted, int from, int to, long[] keys) {
        M3SortedLongBounds.validate(sorted, from, to, keys);
        ensureNativeLoaded();
        return nativeBounds(sorted, from, to, keys);
    }

    /** Private JNI ABI boundary: an external Java consumer cannot invoke this symbol directly. */
    private static native int[] nativeBounds(long[] sorted, int from, int to, long[] keys);
}
