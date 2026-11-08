/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * The Synexia IndexMaskCompactor API unioned into the M3 owner (count, firstEqualBits) for the PrimitiveBig*
 * absorption. firstEqualBits is checked against a naive scan with the documented equality (nonzero booleans, Java
 * narrowing for integral carriers, canonical NaNs, distinct signed zeros); count against selectedCount.
 */
final class IndexMaskCompactorSynexiaApiTest {
    private static final IndexMaskCompactor.Backend JAVA = IndexMaskCompactor.Backend.JAVA;

    @Test void firstEqualBitsMatchesNaiveScanForEveryKind() {
        Random r = new Random(0x1DA5C0L);
        for (int trial = 0; trial < 300; trial++) {
            int n = 1 + r.nextInt(40), from = r.nextInt(n), count = r.nextInt(n - from + 1);
            byte[] b = new byte[n]; short[] s = new short[n]; char[] c = new char[n]; int[] i = new int[n];
            long[] l = new long[n]; float[] f = new float[n]; double[] d = new double[n];
            for (int k = 0; k < n; k++) {
                int v = r.nextInt(5) - 2;
                b[k] = (byte) v; s[k] = (short) v; c[k] = (char) (v & 3); i[k] = v; l[k] = v;
                f[k] = switch (r.nextInt(5)) { case 0 -> Float.NaN; case 1 -> -0.0f; case 2 -> 0.0f; default -> v; };
                d[k] = switch (r.nextInt(5)) { case 0 -> Double.NaN; case 1 -> -0.0d; case 2 -> 0.0d; default -> v; };
            }
            long want = r.nextInt(5) - 2;
            assertEquals(naiveBoolean(b, from, count, want != 0), first(PrimitiveKind.BOOLEAN, b, from, count, want));
            assertEquals(naive(from, count, k -> b[k] == (byte) want), first(PrimitiveKind.BYTE, b, from, count, want));
            assertEquals(naive(from, count, k -> s[k] == (short) want), first(PrimitiveKind.SHORT, s, from, count, want));
            assertEquals(naive(from, count, k -> c[k] == (char) want), first(PrimitiveKind.CHAR, c, from, count, want));
            assertEquals(naive(from, count, k -> i[k] == (int) want), first(PrimitiveKind.INT, i, from, count, want));
            assertEquals(naive(from, count, k -> l[k] == want), first(PrimitiveKind.LONG, l, from, count, want));
            float fw = switch ((int) (want + 2)) { case 0 -> Float.NaN; case 1 -> -0.0f; case 2 -> 0.0f; default -> want; };
            long fBits = Float.floatToIntBits(fw);
            assertEquals(naive(from, count, k -> Float.floatToIntBits(f[k]) == (int) fBits),
                    first(PrimitiveKind.FLOAT, f, from, count, fBits));
            double dw = switch ((int) (want + 2)) { case 0 -> Double.NaN; case 1 -> -0.0d; case 2 -> 0.0d; default -> want; };
            long dBits = Double.doubleToLongBits(dw);
            assertEquals(naive(from, count, k -> Double.doubleToLongBits(d[k]) == dBits),
                    first(PrimitiveKind.DOUBLE, d, from, count, dBits));
        }
    }

    @Test void firstEqualBitsValidatesItsArguments() {
        assertThrows(NullPointerException.class, () -> IndexMaskCompactor.firstEqualBits(null, new int[1], 0, 1, 0, JAVA));
        assertThrows(NullPointerException.class, () -> IndexMaskCompactor.firstEqualBits(PrimitiveKind.INT, null, 0, 1, 0, JAVA));
        assertThrows(NullPointerException.class, () -> IndexMaskCompactor.firstEqualBits(PrimitiveKind.INT, new int[1], 0, 1, 0, null));
        assertThrows(IllegalArgumentException.class, () -> IndexMaskCompactor.firstEqualBits(PrimitiveKind.INT, new long[1], 0, 1, 0, JAVA));
        assertThrows(IndexOutOfBoundsException.class, () -> IndexMaskCompactor.firstEqualBits(PrimitiveKind.INT, new int[3], 2, 2, 0, JAVA));
        assertEquals(-1, IndexMaskCompactor.firstEqualBits(PrimitiveKind.INT, new int[3], 3, 0, 0, JAVA));
        if (!IndexMaskCompactor.nativeAvailable()) {
            assertThrows(UnsatisfiedLinkError.class, () -> IndexMaskCompactor.firstEqualBits(
                    PrimitiveKind.INT, new int[1], 0, 1, 0, IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
    }

    @Test void countIsSelectedCount() {
        Random r = new Random(17L);
        for (int trial = 0; trial < 200; trial++) {
            byte[] states = new byte[r.nextInt(64)];
            r.nextBytes(states);
            int mask = r.nextInt(256);
            assertEquals(IndexMaskCompactor.selectedCount(states, mask, JAVA), IndexMaskCompactor.count(states, mask, JAVA));
            int expected = 0;
            for (byte st : states) if (((st & 0xff) & mask) == mask) expected++;
            assertEquals(expected, IndexMaskCompactor.count(states, mask, JAVA));
        }
        assertThrows(IllegalArgumentException.class, () -> IndexMaskCompactor.count(new byte[1], 0x100, JAVA));
    }

    private static int first(PrimitiveKind kind, Object values, int from, int count, long bits) {
        return IndexMaskCompactor.firstEqualBits(kind, values, from, count, bits, JAVA);
    }

    private interface Hit { boolean at(int k); }

    private static int naive(int from, int count, Hit hit) {
        for (int k = from; k < from + count; k++) if (hit.at(k)) return k;
        return -1;
    }

    private static int naiveBoolean(byte[] b, int from, int count, boolean want) {
        return naive(from, count, k -> (b[k] != 0) == want);
    }
}
