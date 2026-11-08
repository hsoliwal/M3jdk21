/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.tooling.search;

/** Explicit opt-in JNI batch bounds: copies one sorted range per call, never stores Java heap pointers. */
public final class M3NativeSortedIntBounds {
    static {
        String library = System.getProperty("m3.native.sorted.search.library");
        if (library == null || library.isBlank()) {
            throw new IllegalStateException("m3.native.sorted.search.library must name a verified native library");
        }
        System.load(library);
    }

    private M3NativeSortedIntBounds() {
    }

    /**
     * JNI counterpart of {@link M3SortedIntBounds#bounds(int[], int, int, int[])}.
     *
     * @param sorted non-null, nondecreasing signed integer array
     * @param from lower inclusive index
     * @param to upper exclusive index
     * @param keys non-null batch of search targets
     * @return new interleaved lower/upper absolute bounds
     * @throws NullPointerException for null inputs
     * @throws IndexOutOfBoundsException for invalid source range
     * @throws IllegalArgumentException when result count would overflow
     */
    public static int[] bounds(int[] sorted, int from, int to, int[] keys) {
        M3SortedIntBounds.validate(sorted, from, to, keys);
        return nativeBounds(sorted, from, to, keys);
    }

    /** JNI entrypoint is private: no extra supported public ABI beyond the M3 opt-in API. */
    private static native int[] nativeBounds(int[] sorted, int from, int to, int[] keys);
}
