/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Preserve mask-window cursors, unsigned states and JNI results after predicate preparation
 * @modules java.base/jdk.internal.m3
 * @compile --patch-module java.base=${test.src} M3MaskPreparedPredicateTest.java
 * @run main/othervm --patch-module java.base=${test.classes} -Xcheck:jni jdk.internal.m3.M3MaskPreparedPredicateTest native
 * @run main/othervm --patch-module java.base=${test.classes} -Xint -Xcheck:jni jdk.internal.m3.M3MaskPreparedPredicateTest native
 */
package jdk.internal.m3;

import java.util.Arrays;
import java.util.Random;

/** Actual-owner differential fixture. The optional library argument is for host qualification only. */
public final class M3MaskPreparedPredicateTest {
    private static final int SENTINEL = 0x5a5aa55a;
    private static long checks;
    private static long digest = 0x4d334a4e4921L;
    private static long vectors;
    private static long truthCases;

    private M3MaskPreparedPredicateTest() { }

    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }

    private static void observe(long value) {
        digest = Long.rotateLeft(digest, 11) ^ value;
        digest *= 0x9e3779b97f4a7c15L;
    }

    private static long reference(int[] rows, byte[] states, int first, int end,
            int required, int excluded, int[] output, int offset, int limit) {
        int next = first, count = 0;
        while (next < end && count < limit) {
            int flags = Byte.toUnsignedInt(states[next]);
            if ((flags & required) == required && (flags & excluded) == 0) {
                output[offset + count++] = rows[next];
            }
            next++;
        }
        return ((long) next << 32) | Integer.toUnsignedLong(count);
    }

    private static void vector(int[] rows, byte[] states, int first, int budget,
            int required, int excluded, int offset, int limit,
            M3IndexMaskWindow.Backend backend, boolean bounded, boolean internal) {
        int[] actual = new int[offset + limit + 7];
        Arrays.fill(actual, SENTINEL);
        int[] expected = actual.clone();
        int[] originalRows = rows.clone();
        byte[] originalStates = states.clone();
        int end = bounded ? first + Math.min(budget, rows.length - first) : rows.length;
        long wanted = reference(rows, states, first, end, required, excluded, expected, offset, limit);
        long got;
        if (internal) {
            got = M3IndexMaskWindow.scanJava(rows, states, first, required,
                    excluded, actual, offset, limit);
        } else if (bounded) {
            got = M3IndexMaskWindow.scanBounded(rows, states, first, budget, required,
                    excluded, actual, offset, limit, backend);
        } else {
            got = M3IndexMaskWindow.scan(rows, states, first, required,
                    excluded, actual, offset, limit, backend);
        }
        check(got == wanted, "packed cursor at vector " + vectors);
        check(Arrays.equals(actual, expected), "stable output/untouched window at vector " + vectors);
        check(Arrays.equals(rows, originalRows), "row input changed");
        check(Arrays.equals(states, originalStates), "state input changed");
        observe(got);
        for (int row : actual) observe(row);
        vectors++;
    }

    private static void exhaustive(M3IndexMaskWindow.Backend backend) {
        int[] rows = new int[256];
        byte[] states = new byte[256];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = i / 3 - 41;
            states[i] = (byte) i;
        }
        for (int required = 0; required < 256; required++) {
            for (int excluded = 0; excluded < 256; excluded++) {
                // Exercise all 16,777,216 truth cases through the original internal atom,
                // including contradictory masks which public dispatch short-circuits.
                vector(rows, states, 0, 256, required, excluded, 3, 256, backend, false, true);
                truthCases += 256;
                vector(rows, states, 0, 256, required, excluded, 3, 256, backend, false, false);
                vector(rows, states, 0, 256, required, excluded, 3, 256, backend, true, false);
            }
        }
    }

    private static void randomWindows(M3IndexMaskWindow.Backend backend) {
        Random random = new Random(0x4d334a4e4921L);
        int[] edges = {0, 1, 2, 63, 64, 65, 127, 128, 129, 255, 256, 257, 4095, 4096, 4097};
        for (int sample = 0; sample < 12_000; sample++) {
            int n = sample < edges.length ? edges[sample] : random.nextInt(1026);
            int[] rows = new int[n];
            byte[] states = new byte[n];
            random.nextBytes(states);
            for (int i = 0; i < n; i++) {
                rows[i] = switch (sample % 4) {
                    case 0 -> i / 3;
                    case 1 -> -(i / 3);
                    case 2 -> random.nextInt(17) - 8;
                    default -> random.nextInt();
                };
            }
            int first = random.nextInt(n + 1);
            int budget = sample % 4 == 0 ? Integer.MAX_VALUE : edges[random.nextInt(edges.length)];
            int limit = Math.min(4096, edges[random.nextInt(edges.length)]);
            int required = sample % 5 == 0 ? 0 : random.nextInt(256);
            int excluded = sample % 5 == 0 ? 0 : random.nextInt(256);
            if (sample % 5 == 1) excluded &= ~required;
            int offset = random.nextInt(9);
            vector(rows, states, first, budget, required, excluded, offset, limit, backend, true, false);
            vector(rows, states, first, n - first, required, excluded, offset, limit, backend, false, false);
        }
        // Force nonconstant, long scans and all chunk boundaries rather than relying on random density.
        for (int n : edges) {
            int[] rows = new int[n];
            byte[] states = new byte[n];
            for (int i = 0; i < n; i++) { rows[i] = i / 2; states[i] = (byte) (i % 129 == 128 ? 128 : 1); }
            for (int limit : new int[]{0, 1, 127, 128, 129, 4096}) {
                vector(rows, states, 0, Integer.MAX_VALUE, 1, 128, 5, limit, backend, true, false);
                vector(rows, states, 0, Integer.MAX_VALUE, 128, 1, 5, limit, backend, true, false);
            }
        }
    }

    private static void resumption(M3IndexMaskWindow.Backend backend) {
        int n = 4097;
        int[] rows = new int[n];
        byte[] states = new byte[n];
        for (int i = 0; i < n; i++) { rows[i] = i / 4; states[i] = (byte) (i % 7 == 0 ? 3 : 128); }
        int[] wanted = new int[n];
        long result = reference(rows, states, 0, n, 3, 128, wanted, 0, n);
        int wantedCount = (int) result;
        int[] collected = new int[n];
        int cursor = 0, count = 0, calls = 0;
        while (cursor < n) {
            int[] page = new int[3];
            long next = M3IndexMaskWindow.scanBounded(rows, states, cursor, 17, 3, 128,
                    page, 0, page.length, backend);
            int end = M3IndexMaskWindow.nextIndex(next);
            int written = M3IndexMaskWindow.written(next);
            check(end > cursor && end <= Math.min(n, cursor + 17), "resumption progress/budget");
            System.arraycopy(page, 0, collected, count, written);
            cursor = end;
            count += written;
            check(++calls <= n, "bounded calls");
        }
        check(count == wantedCount, "resumption count");
        check(Arrays.equals(Arrays.copyOf(collected, count), Arrays.copyOf(wanted, count)), "resumption order");
        observe(count);
    }

    @FunctionalInterface
    private interface Attempt { void run(); }

    private static void failure(Class<? extends Throwable> type, Attempt attempt) {
        Throwable got = null;
        try { attempt.run(); } catch (Throwable thrown) { got = thrown; }
        check(got != null && got.getClass() == type, "exception type " + type.getName());
        observe(got.getClass().getName().hashCode());
        observe(String.valueOf(got.getMessage()).hashCode());
    }

    private static void validation(M3IndexMaskWindow.Backend backend) {
        int[] rows = {7, 7, 3};
        byte[] states = {1, -128, -1};
        int[] output = {SENTINEL, SENTINEL, SENTINEL};
        failure(NullPointerException.class, () -> M3IndexMaskWindow.scan(null, states, -1, -1, -1, null, -1, -1, null));
        failure(NullPointerException.class, () -> M3IndexMaskWindow.scan(rows, null, -1, -1, -1, null, -1, -1, null));
        failure(NullPointerException.class, () -> M3IndexMaskWindow.scan(rows, states, 0, 0, 0, null, 0, 0, backend));
        failure(NullPointerException.class, () -> M3IndexMaskWindow.scan(rows, states, 0, 0, 0, output, 0, 0, null));
        failure(IllegalArgumentException.class, () -> M3IndexMaskWindow.scan(rows, states, -1, -1, -1, rows, -1, -1, backend));
        failure(IllegalArgumentException.class, () -> M3IndexMaskWindow.scan(rows, new byte[2], 0, 0, 0, output, 0, 0, backend));
        for (int mask : new int[]{-1, 256, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            failure(IllegalArgumentException.class, () -> M3IndexMaskWindow.scan(rows, states, 0, mask, 0, output, 0, 0, backend));
            failure(IllegalArgumentException.class, () -> M3IndexMaskWindow.scan(rows, states, 0, 0, mask, output, 0, 0, backend));
        }
        for (int first : new int[]{-1, 4, Integer.MAX_VALUE}) {
            failure(IndexOutOfBoundsException.class, () -> M3IndexMaskWindow.scan(rows, states, first, 0, 0, output, 0, 0, backend));
        }
        for (int limit : new int[]{-1, 4097, Integer.MAX_VALUE}) {
            failure(IllegalArgumentException.class, () -> M3IndexMaskWindow.scan(rows, states, 0, 0, 0, output, 0, limit, backend));
        }
        failure(IndexOutOfBoundsException.class, () -> M3IndexMaskWindow.scan(rows, states, 0, 0, 0, output, -1, 0, backend));
        failure(IndexOutOfBoundsException.class, () -> M3IndexMaskWindow.scan(rows, states, 0, 0, 0, output, 2, 2, backend));
        failure(IllegalArgumentException.class, () -> M3IndexMaskWindow.scanBounded(rows, states, 0, -1, 0, 0, output, 0, 0, backend));
        check(Arrays.equals(output, new int[]{SENTINEL, SENTINEL, SENTINEL}), "validation changed output");
    }

    public static void main(String[] args) {
        String mode = args.length == 0 ? "native" : args[0];
        if (args.length > 1) System.load(args[1]);
        if (mode.equals("missing")) {
            check(!M3IndexMaskWindow.nativeAvailable(), "missing/partial ABI was admitted");
            failure(UnsatisfiedLinkError.class, () -> M3IndexMaskWindow.scan(new int[0], new byte[0],
                    0, 0, 0, new int[0], 0, 0, M3IndexMaskWindow.Backend.NATIVE_REQUIRED));
            failure(UnsatisfiedLinkError.class, () -> M3IndexMaskWindow.scanBounded(new int[0], new byte[0],
                    0, 0, 0, 0, new int[0], 0, 0, M3IndexMaskWindow.Backend.NATIVE_REQUIRED));
            System.out.println("MISSING_ABI_PASS checks=" + checks + " digest=" + Long.toHexString(digest));
            return;
        }
        M3IndexMaskWindow.Backend backend = mode.equals("java")
                ? M3IndexMaskWindow.Backend.JAVA : M3IndexMaskWindow.Backend.NATIVE_REQUIRED;
        if (backend == M3IndexMaskWindow.Backend.NATIVE_REQUIRED) {
            check(M3IndexMaskWindow.nativeAvailable(), "actual JNI library missing");
        }
        exhaustive(backend);
        randomWindows(backend);
        resumption(backend);
        validation(backend);
        System.out.println("MASK_WINDOW_PASS truthCases=" + truthCases + " vectors=" + vectors
                + " checks=" + checks + " digest=" + Long.toHexString(digest));
    }
}
