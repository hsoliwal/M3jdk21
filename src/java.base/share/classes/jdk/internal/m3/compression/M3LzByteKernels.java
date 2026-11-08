// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.util.Arrays;
import java.util.Objects;

/**
 * Format-neutral LZ byte operations. Every offset is relative to the supplied array.
 * This class owns neither a dictionary nor a compressed-wire format.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/main/java/com/synexia/algorithms/compression/M3LzByteKernels.java blob 9510f829bb44; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class M3LzByteKernels {
    private M3LzByteKernels() {}

    /** Return the length of the equal prefix, bounded by {@code maximum}. */
    public static int commonPrefix(byte[] input, int left, int right, int maximum) {
        Objects.requireNonNull(input, "input");
        checkWindow(input.length, left, maximum);
        checkWindow(input.length, right, maximum);
        return uncheckedPrefix(input, left, right, maximum);
    }

    /**
     * Match a batch of windows without per-match objects. All arrays are read-only.
     * The result has exactly one length per input lane.
     */
    public static int[] commonPrefixes(byte[] input, int[] left, int[] right, int[] maximum) {
        checkBatch(input, left, right, maximum);
        // Shared positions make every validated lane an identity comparison.
        // Clone preserves result ownership, including an empty or aliased maximum.
        if (left == right) {
            return maximum.clone();
        }
        int[] results = new int[left.length];
        for (int i = 0; i < results.length; i++) {
            results[i] = uncheckedPrefix(input, left[i], right[i], maximum[i]);
        }
        return results;
    }

    /**
     * Expand a backward LZ reference, including overlaps (distance may be 1).
     * Bytes before {@code target} must already be initialized by the caller.
     */
    public static void copyMatch(byte[] output, int target, int distance, int length) {
        Objects.requireNonNull(output, "output");
        checkWindow(output.length, target, length);
        if (distance <= 0 || distance > target) {
            throw new IllegalArgumentException("back-reference must point to prior output");
        }
        if (length == 0) {
            return;
        }
        int copied = Math.min(distance, length);
        System.arraycopy(output, target - distance, output, target, copied);
        while (copied < length) {
            int next = Math.min(copied, length - copied);
            System.arraycopy(output, target, output, target + copied, next);
            copied += next;
        }
    }

    static void checkBatch(byte[] input, int[] left, int[] right, int[] maximum) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(maximum, "maximum");
        if (left.length != right.length || left.length != maximum.length) {
            throw new IllegalArgumentException("match lane lengths differ");
        }
        for (int i = 0; i < left.length; i++) {
            checkWindow(input.length, left[i], maximum[i]);
            checkWindow(input.length, right[i], maximum[i]);
        }
    }

    private static int uncheckedPrefix(byte[] input, int left, int right, int maximum) {
        // Both windows have already been validated by commonPrefix/checkBatch.
        // An identical range is equal without touching its bytes.
        if (left == right || maximum == 0) {
            return maximum;
        }
        int mismatch = Arrays.mismatch(input, left, left + maximum,
                input, right, right + maximum);
        return mismatch < 0 ? maximum : mismatch;
    }

    private static void checkWindow(int capacity, int position, int length) {
        if (length < 0 || position < 0 || position > capacity - length) {
            throw new IllegalArgumentException("invalid byte window");
        }
    }
}
