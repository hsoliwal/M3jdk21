// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

import java.nio.file.Path;
import java.util.Objects;
import jdk.m3.arrays.ArrayNative;

/** Required native bounded decode for first-encounter integer codebooks. */
public final class IntCodebookNative {
    public static final int MAX_ITEMS = 4096;
    private IntCodebookNative() {}

    /**
     * Decodes exact LSB-first packed IDs. Both element count, dictionary count
     * and encoded byte count must fit 4096. Invalid IDs and nonzero tail
     * padding are rejected by native code before any result becomes visible.
     */
    public static int[] decodeRequired(byte[] indices, int[] dictionary,
                                       int length, int bits) {
        Objects.requireNonNull(indices, "indices");
        Objects.requireNonNull(dictionary, "dictionary");
        if (length < 0 || length > MAX_ITEMS || dictionary.length == 0
                || dictionary.length > MAX_ITEMS || bits < 0 || bits > 12
                || indices.length > MAX_ITEMS
                || ((long) length * bits + 7L) / 8L != indices.length) {
            throw new IllegalArgumentException("invalid dictionary JNI batch geometry");
        }
        int requiredBits = dictionary.length == 1 ? 0
                : Integer.SIZE - Integer.numberOfLeadingZeros(dictionary.length - 1);
        if (requiredBits != bits) throw new IllegalArgumentException("noncanonical dictionary width");
        if (!ArrayNative.isLoaded()) {
            String configured = System.getProperty("m3.arrays.native.path");
            if (configured == null || configured.isBlank()) {
                throw new UnsatisfiedLinkError("M3 arrays JNI is not loaded");
            }
            ArrayNative.load(Path.of(configured));
        }
        return nativeDecode(indices, dictionary, length, bits);
    }

    private static native int[] nativeDecode(byte[] indices, int[] dictionary,
                                             int length, int bits);
}
