/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;

/** Current complete Java/JNI owners; independent scalar oracle for bounded state masks. */
public final class M3ViewportMaskParityProof {
    private static long checks, digest;
    private M3ViewportMaskParityProof() { }
    private static void equal(long a, long b) {
        checks++; if (a != b) throw new AssertionError(a + " != " + b + " @" + checks);
        digest = digest * 31 + b;
    }
    private static void check(boolean value) { equal(1, value ? 1 : 0); }
    private static boolean matches(byte value, int required, int excluded) {
        int state = value & 255;
        return (state & required) == required && (state & excluded) == 0;
    }
    public static void main(String[] args) {
        checks = 0; digest = 61;
        boolean nativeMode = args.length > 0 && args[0].equals("native");
        var backend = nativeMode ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA;
        if (nativeMode && !M3IndexMaskCompactor.nativeAvailable()) throw new AssertionError("native library required");
        for (int n : new int[]{0, 1, 2, 255, 256, 257, 513}) for (int profile = 0; profile < 3; profile++) {
            int[] rows = new int[n + 5]; long[] longs = new long[n + 5]; byte[] states = new byte[n + 7];
            for (int i = 0; i < rows.length; i++) {
                rows[i] = i % 7 == 0 ? Integer.MIN_VALUE : i % 7 == 1 ? Integer.MAX_VALUE : i % 11;
                longs[i] = i % 7 == 0 ? Long.MIN_VALUE : i % 7 == 1 ? Long.MAX_VALUE : i % 11;
            }
            for (int i = 0; i < states.length; i++) states[i] = profile == 0 ? 0 : profile == 1 ? (byte)255 : (byte)(i * 37);
            int[] savedRows = rows.clone(); long[] savedLongs = longs.clone(); byte[] savedStates = states.clone();
            for (int mask = 0; mask < 256; mask++) {
                int[] ints = M3IndexMaskCompactor.selectWindow(rows, 3, states, 2, n, mask, backend);
                long[] wide = M3IndexMaskCompactor.selectLongWindow(longs, 3, states, 2, n, mask, backend);
                int out = 0;
                for (int i = 0; i < n; i++) if (matches(states[2 + i], mask, 0)) {
                    equal(rows[3 + i], ints[out]); equal(longs[3 + i], wide[out]); out++;
                }
                equal(out, ints.length); equal(out, wide.length);
                check(ints != rows); check(wide != longs);
                for (int excluded : new int[]{0, 1, 128, 255}) {
                    int[] selected = M3IndexMaskCompactor.selectPositions(states, mask, excluded, backend);
                    int k = 0;
                    for (int i = 0; i < states.length; i++) if (matches(states[i], mask, excluded)) equal(i, selected[k++]);
                    equal(k, selected.length); equal(k, M3IndexMaskCompactor.selectedCount(states, mask, excluded));
                }
                byte[] aligned = Arrays.copyOfRange(states, 2, 2 + n);
                int[] logical = Arrays.copyOfRange(rows, 3, 3 + n);
                check(Arrays.equals(ints, M3IndexMaskCompactor.select(logical, aligned, mask, backend)));
                if (nativeMode) check(Arrays.equals(ints, M3IndexMaskCompactor.selectNativeRange(rows, 3, n, aligned, mask)));
                else check(Arrays.equals(ints, M3IndexMaskCompactor.select(logical, aligned, mask, backend)));
            }
            check(Arrays.equals(savedRows, rows)); check(Arrays.equals(savedLongs, longs)); check(Arrays.equals(savedStates, states));
        }
        for (int required : new int[]{-1, 256, Integer.MAX_VALUE}) {
            try { M3IndexMaskCompactor.selectWindow(new int[0], 0, new byte[0], 0, 0, required, backend); throw new AssertionError("mask admission"); }
            catch (IllegalArgumentException expected) { checks++; digest = digest * 31 + 3; }
        }
        System.out.println("VIEWPORT_MASK_PASS checks=" + checks + " digest=" + Long.toUnsignedString(digest));
    }
}
