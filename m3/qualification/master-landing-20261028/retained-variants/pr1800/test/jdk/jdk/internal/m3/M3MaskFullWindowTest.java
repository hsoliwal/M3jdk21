/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Preserve dense and pending JNI mask windows, bounds and resumable cursors
 * @modules java.base/jdk.internal.m3
 * @compile --patch-module java.base=${test.src} M3MaskFullWindowTest.java
 * @run main/othervm --patch-module java.base=${test.classes} -Xcheck:jni jdk.internal.m3.M3MaskFullWindowTest
 * @run main/othervm --patch-module java.base=${test.classes} -Xint -Xcheck:jni jdk.internal.m3.M3MaskFullWindowTest
 */
package jdk.internal.m3;

import java.util.Arrays;

/** Actual bootstrap owner; optional library path is only a bounded host harness input. */
public final class M3MaskFullWindowTest {
    private static final int GUARD = 0x5aa55aa5;
    private static long digest = 0x4d3346554c4cL;
    private static long cases;
    private static long checks;
    private M3MaskFullWindowTest() { }
    private static void check(boolean value, String reason) {
        checks++; if (!value) throw new AssertionError(reason);
    }
    private static void mix(long value) {
        digest = Long.rotateLeft(digest, 7) ^ value;
        digest *= 0x9e3779b97f4a7c15L;
    }
    private static void vector(int size, int pattern, int first, int budget, int limit,
                               int offset, M3IndexMaskWindow.Backend backend, boolean bounded) {
        int[] rows = new int[size]; byte[] states = new byte[size];
        for (int i = 0; i < size; i++) {
            rows[i] = switch (i % 5) {
                case 0 -> Integer.MIN_VALUE;
                case 1 -> Integer.MAX_VALUE;
                case 2 -> -1;
                default -> i / 3;
            };
            states[i] = switch (pattern) {
                case 0 -> 1;
                case 1 -> (byte) 128;
                case 2 -> (byte) (i == first + 127 ? 0 : 1);
                case 3 -> (byte) (i % 129 == 128 ? 0 : 1);
                case 4 -> (byte) (i % 64 == 0 ? 1 : 0);
                default -> (byte) (i % 2 == 0 ? 1 : 0);
            };
        }
        int required = pattern == 1 ? 128 : 1;
        int excluded = pattern == 1 ? 1 : 128;
        int[] actual = new int[offset + limit + 7]; Arrays.fill(actual, GUARD);
        int[] expected = actual.clone();
        int[] savedRows = rows.clone(); byte[] savedStates = states.clone();
        int end = bounded ? first + Math.min(budget, size - first) : size;
        int next = first, count = 0;
        while (next < end && count < limit) {
            int flags = Byte.toUnsignedInt(states[next]);
            if ((flags & required) == required && (flags & excluded) == 0) expected[offset + count++] = rows[next];
            next++;
        }
        long wanted = ((long) next << 32) | Integer.toUnsignedLong(count);
        long result = bounded
                ? M3IndexMaskWindow.scanBounded(rows, states, first, budget, required, excluded, actual, offset, limit, backend)
                : M3IndexMaskWindow.scan(rows, states, first, required, excluded, actual, offset, limit, backend);
        check(result == wanted, "cursor/count");
        check(Arrays.equals(actual, expected), "stable full/pending output and guards");
        check(Arrays.equals(rows, savedRows), "row input mutation");
        check(Arrays.equals(states, savedStates), "state input mutation");
        mix(result); for (int value : actual) mix(value); cases++;
    }
    private static void resume(M3IndexMaskWindow.Backend backend, int limit, int budget) {
        int size = 4097;
        int[] rows = new int[size]; byte[] states = new byte[size];
        for (int i = 0; i < size; i++) { rows[i] = i / 3; states[i] = (byte) (i % 129 == 128 ? 0 : 1); }
        int[] actual = new int[size]; int next = 0, count = 0, calls = 0;
        while (next < size) {
            int[] window = new int[limit + 2]; Arrays.fill(window, GUARD);
            long result = M3IndexMaskWindow.scanBounded(rows, states, next, budget, 1, 128,
                    window, 1, limit, backend);
            int cursor = M3IndexMaskWindow.nextIndex(result), written = M3IndexMaskWindow.written(result);
            check(cursor > next && cursor <= Math.min(size, next + budget), "resumption progress");
            check(window[0] == GUARD && window[limit + 1] == GUARD, "resumption guards");
            System.arraycopy(window, 1, actual, count, written);
            count += written; next = cursor; calls++;
            check(calls <= size, "bounded resumption");
        }
        int wanted = 0;
        for (int i = 0; i < size; i++) if (states[i] != 0) check(actual[wanted++] == rows[i], "resumption values");
        check(count == wanted, "resumption final count"); mix(count); cases++;
    }
    public static void main(String[] args) {
        if (args.length > 0) System.load(args[0]);
        check(M3IndexMaskWindow.nativeAvailable(), "required actual JNI symbols missing");
        int[] sizes = {0, 1, 127, 128, 129, 255, 256, 257, 511, 512, 513, 4095, 4096, 4097};
        int[] limits = {0, 1, 127, 128, 129, 255, 256, 4096};
        int[] budgets = {0, 1, 127, 128, 129, 255, 256, 4096, Integer.MAX_VALUE};
        for (M3IndexMaskWindow.Backend backend : M3IndexMaskWindow.Backend.values()) {
            for (int size : sizes) for (int pattern = 0; pattern < 6; pattern++)
                for (int first : new int[]{0, Math.min(3, size)}) for (int limit : limits) {
                    vector(size, pattern, first, size - first, limit, 5, backend, false);
                    for (int budget : budgets) vector(size, pattern, first, budget, limit, 3, backend, true);
                }
            for (int limit : new int[]{1, 127, 128, 129, 256})
                for (int budget : new int[]{1, 127, 128, 129, 257, 4096}) resume(backend, limit, budget);
        }
        System.out.println("MASK_FULL_WINDOW_PASS cases=" + cases + " checks=" + checks + " digest=" + Long.toHexString(digest));
    }
}
