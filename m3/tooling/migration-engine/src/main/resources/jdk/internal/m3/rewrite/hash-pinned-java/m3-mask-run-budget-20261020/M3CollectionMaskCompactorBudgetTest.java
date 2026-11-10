/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file
 * that accompanied this code).
 */

/*
 * @test
 * @summary Deletion-mask run-budget optimization preserves array identities and ranges
 * @modules java.base/jdk.internal.mindex
 * @run main M3CollectionMaskCompactorBudgetTest
 */
import java.util.Arrays;
import java.util.Random;
import jdk.internal.mindex.M3CollectionMaskCompactor;

public class M3CollectionMaskCompactorBudgetTest {
    private static int scenarios;

    private static void verify(int length, int from, int suffix, int mode, long seed) {
        Object[] original = new Object[from + length + suffix];
        for (int i = 0; i < original.length; i++) original[i] = new Object();
        Object[] actual = original.clone();
        Object[] expected = original.clone();
        long[] deletion = new long[(length + 63) >>> 6];
        Random random = new Random(seed);
        for (int i = 0; i < length; i++) {
            boolean remove = switch (mode) {
                case 0 -> false;
                case 1 -> true;
                case 2 -> (i & 1) == 0;
                case 3 -> (i & 1) != 0;
                case 4 -> (i % 127) >= 60 && (i % 127) <= 68;
                case 5 -> i == 63 || i == 64 || i == 127 || i == 128;
                case 6 -> random.nextInt(16) == 0;
                case 7 -> random.nextInt(16) != 0;
                case 8 -> i > (length >>> 1) && (i & 3) == 0;
                default -> throw new AssertionError(mode);
            };
            if (remove) deletion[i >>> 6] |= 1L << (i & 63);
        }
        // Garbage in a mask word after the logical range must not be inspected.
        if (length > 0 && (length & 63) != 0) deletion[deletion.length - 1] |= -1L << (length & 63);
        int end = from;
        for (int i = 0; i < length; i++) {
            if ((deletion[i >>> 6] & (1L << (i & 63))) == 0) expected[end++] = expected[from + i];
        }
        int result = M3CollectionMaskCompactor.compact(actual, from, from + length, deletion);
        if (result != end) throw new AssertionError("survivor end: " + mode + "/" + length);
        for (int i = 0; i < end; i++) {
            if (actual[i] != expected[i]) throw new AssertionError("identity at " + i);
        }
        for (int i = from + length; i < actual.length; i++) {
            if (actual[i] != original[i]) throw new AssertionError("suffix overwritten at " + i);
        }
        for (int i = 0; i < from; i++) {
            if (actual[i] != original[i]) throw new AssertionError("prefix overwritten at " + i);
        }
        scenarios++;
    }

    private static void failures() {
        Object[] values = new Object[3];
        try { M3CollectionMaskCompactor.compact(null, 0, 0, new long[0]); throw new AssertionError(); }
        catch (NullPointerException expected) { }
        try { M3CollectionMaskCompactor.compact(values, 0, 0, null); throw new AssertionError(); }
        catch (NullPointerException expected) { }
        try { M3CollectionMaskCompactor.compact(values, -1, 2, new long[1]); throw new AssertionError(); }
        catch (IndexOutOfBoundsException expected) { }
        try { M3CollectionMaskCompactor.compact(values, 1, 4, new long[1]); throw new AssertionError(); }
        catch (IndexOutOfBoundsException expected) { }
        try { M3CollectionMaskCompactor.compact(values, 0, 2, new long[0]); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { }
        if (M3CollectionMaskCompactor.compact(values, 0, 0, new long[0]) != 0) throw new AssertionError();
    }

    public static void main(String[] args) {
        failures();
        int[] lengths = {0, 1, 2, 31, 63, 64, 65, 127, 128, 129,
                         255, 256, 511, 512, 513, 1023, 1024, 4095,
                         4096, 8191, 8192, 65536};
        for (int length : lengths) {
            for (int mode = 0; mode < 9; mode++) {
                for (int pad = 0; pad < 3; pad++) {
                    verify(length, pad, pad + 2, mode, 0x5a5a1234L + length * 37L + mode * 7L + pad);
                }
            }
        }
        Random random = new Random(0x9e3779b97f4a7c15L);
        for (int n = 0; n < 600; n++) {
            verify(random.nextInt(18000), random.nextInt(5), 3, random.nextInt(9), random.nextLong());
        }
        System.out.println("M3 compaction differential scenarios=" + scenarios);
    }
}
