// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Scalar-oracle differential test; immutable inputs and guard regions are part of the oracle. */
public final class M3MaskWindowSparseContract {
    private static long checks;
    private M3MaskWindowSparseContract() { }
    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("mask-window check " + checks);
    }
    private static void trial(int[] rows, byte[] states, int first, int required, int excluded,
                              int offset, int limit, M3IndexMaskCompactor.Backend backend) {
        int[] expected = new int[offset + limit + 7];
        Arrays.fill(expected, 0x2468ace);
        int[] actual = expected.clone();
        int[] originalRows = rows.clone();
        byte[] originalStates = states.clone();
        int next = first, written = 0;
        while (next < rows.length && written < limit) {
            int flags = states[next] & 255;
            if ((flags & required) == required && (flags & excluded) == 0) expected[offset + written++] = rows[next];
            next++;
        }
        long result = M3IndexMaskWindow.scan(rows, states, first, required, excluded, actual, offset, limit, backend);
        check(M3IndexMaskWindow.nextIndex(result) == next);
        check(M3IndexMaskWindow.written(result) == written);
        check(Arrays.equals(expected, actual));
        check(Arrays.equals(rows, originalRows));
        check(Arrays.equals(states, originalStates));
    }
    private static void refuses(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable failure) {
            check(type.isInstance(failure)); return;
        }
        throw new AssertionError("expected " + type);
    }
    public static void main(String[] args) {
        boolean nativeMode = args.length > 0 && args[0].equals("native");
        var backend = nativeMode ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA;
        if (nativeMode) check(M3IndexMaskCompactor.nativeAvailable());
        Random random = new Random(0x4d334a444b21L);
        int[] rows = new int[256];
        byte[] states = new byte[256];
        for (int i = 0; i < rows.length; i++) { rows[i] = random.nextInt(17) - 8; states[i] = (byte) i; }
        for (int required = 0; required < 256; required++) {
            for (int excluded = 0; excluded < 256; excluded++) {
                trial(rows, states, required % 7, required, excluded, 3, 137, backend);
            }
        }
        for (int length : new int[] {0, 1, 63, 64, 127, 128, 129, 255, 256, 257, 4095, 4096, 4097, 8193}) {
            rows = new int[length]; states = new byte[length];
            for (int i = 0; i < length; i++) rows[i] = random.nextInt();
            for (int mode = 0; mode < 5; mode++) {
                for (int i = 0; i < length; i++) states[i] = (byte) (mode == 0 ? 0 : mode == 1 ? 255 : mode == 2 ? (i % 521 == 0 ? 128 : 0) : random.nextInt(256));
                for (int limit : new int[] {0, 1, 2, 127, 128, 129, 4096}) {
                    for (int first : new int[] {0, length / 2, length}) {
                        trial(rows, states, first, 0, 0, 2, limit, backend);
                        trial(rows, states, first, 128, 0, 2, limit, backend);
                        trial(rows, states, first, 128, 128, 2, limit, backend);
                        trial(rows, states, first, 0, 127, 2, limit, backend);
                    }
                }
            }
        }
        int[] source = {Integer.MIN_VALUE, 7, 7, Integer.MAX_VALUE};
        byte[] flags = {0, 1, -1, 2};
        int[] guard = {11, 12, 13, 14, 15};
        int[] frozen = guard.clone();
        for (int mask : new int[] {-1, 256, Integer.MAX_VALUE}) {
            refuses(IllegalArgumentException.class, () -> M3IndexMaskWindow.scan(source, flags, 0, mask, 0, guard, 0, 1, backend));
            refuses(IllegalArgumentException.class, () -> M3IndexMaskWindow.scan(source, flags, 0, 0, mask, guard, 0, 1, backend));
        }
        refuses(IllegalArgumentException.class, () -> M3IndexMaskWindow.scan(source, flags, 0, 0, 0, source, 0, 1, backend));
        refuses(IllegalArgumentException.class, () -> M3IndexMaskWindow.scan(source, new byte[3], 0, 0, 0, guard, 0, 1, backend));
        refuses(IndexOutOfBoundsException.class, () -> M3IndexMaskWindow.scan(source, flags, -1, 0, 0, guard, 0, 1, backend));
        refuses(IndexOutOfBoundsException.class, () -> M3IndexMaskWindow.scan(source, flags, 5, 0, 0, guard, 0, 1, backend));
        refuses(IndexOutOfBoundsException.class, () -> M3IndexMaskWindow.scan(source, flags, 0, 0, 0, guard, 5, 1, backend));
        refuses(IllegalArgumentException.class, () -> M3IndexMaskWindow.scan(source, flags, 0, 0, 0, guard, 0, 4097, backend));
        refuses(NullPointerException.class, () -> M3IndexMaskWindow.scan(source, flags, 0, 0, 0, guard, 0, 1, null));
        check(Arrays.equals(frozen, guard));
        if (args.length > 0 && args[0].equals("missing")) {
            check(!M3IndexMaskCompactor.nativeAvailable());
            for (int limit : new int[] {0, 1}) {
                refuses(UnsatisfiedLinkError.class, () -> M3IndexMaskWindow.scan(source, flags, 0, 0, 0, guard, 0, limit, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
                refuses(UnsatisfiedLinkError.class, () -> M3IndexMaskWindow.scan(source, flags, 0, 1, 1, guard, 0, limit, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            }
        }
        System.out.println("PASS M3MaskWindowSparseContract backend=" + backend + " checks=" + checks);
    }
}
