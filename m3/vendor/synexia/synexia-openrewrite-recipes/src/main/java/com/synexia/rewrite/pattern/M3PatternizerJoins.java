// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.pattern;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;

/**
 * Sorted unique row-ID intersections; ephemeral query projection, not a new graph owner.
 * Arrays must remain read-only throughout the call, including within progress/cancellation
 * callbacks. JNI may pin or copy them; no address is retained after the call. Callbacks are
 * synchronous, and a thrown exception cancels the result rather than returning partial output.
 */
public final class M3PatternizerJoins {
    private M3PatternizerJoins() { }

    @FunctionalInterface
    public interface Intersection {
        int[] apply(int[] left, int[] right, BooleanSupplier canceled, LongConsumer progress);
    }

    public static final Intersection JAVA = M3PatternizerJoins::javaIntersection;

    /** Explicit opt-in only. Missing or incompatible native libraries fail; no silent fallback. */
    public static synchronized Intersection nativeBackend(Path library) {
        Objects.requireNonNull(library, "library");
        if (!library.isAbsolute()) throw new IllegalArgumentException("absolute native library required");
        System.load(library.normalize().toString());
        if (abi() != 1) throw new IllegalStateException("pattern join ABI mismatch");
        return (left, right, canceled, progress) -> {
            check(canceled);
            return intersect0(left, right,
                    () -> Thread.currentThread().isInterrupted() || canceled != null && canceled.getAsBoolean(),
                    progress);
        };
    }

    public static void check(BooleanSupplier canceled) {
        if (Thread.currentThread().isInterrupted() || canceled != null && canceled.getAsBoolean()) {
            throw new CancellationException("patternizer canceled");
        }
    }

    private static void validate(int[] values, BooleanSupplier canceled) {
        Objects.requireNonNull(values, "posting");
        int previous = -1;
        for (int i = 0; i < values.length; i++) {
            if ((i & 4095) == 0) check(canceled);
            if (values[i] <= previous) throw new IllegalArgumentException("posting must be sorted unique nonnegative");
            previous = values[i];
        }
    }

    private static int[] javaIntersection(int[] left, int[] right,
                                          BooleanSupplier canceled, LongConsumer progress) {
        check(canceled);
        validate(left, canceled);
        validate(right, canceled);
        int[] out = new int[Math.min(left.length, right.length)];
        int i = 0, j = 0, size = 0;
        long work = 0;
        while (i < left.length && j < right.length) {
            if ((work++ & 4095) == 0) check(canceled);
            if (left[i] < right[j]) i++;
            else if (left[i] > right[j]) j++;
            else { out[size++] = left[i++]; j++; }
        }
        if (progress != null) progress.accept(work);
        check(canceled);
        return Arrays.copyOf(out, size);
    }

    private static native int abi();
    private static native int[] intersect0(int[] left, int[] right,
                                           BooleanSupplier canceled, LongConsumer progress);
}
