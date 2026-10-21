// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Independent bit-by-bit receiver oracle for the concurrently added long overload. */
public final class M3RequiredExcludedLongProbe {
    private static long checks;
    private M3RequiredExcludedLongProbe() { }
    private static void require(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("required/excluded long selection");
    }
    private static void sample(long[] rows, byte[] flags, int required, int excluded) {
        long[] expected = new long[rows.length];
        int kept = 0;
        for (int i = 0; i < rows.length; i++) {
            boolean selected = true;
            int bits = Byte.toUnsignedInt(flags[i]);
            for (int bit = 0; bit < 8; bit++) {
                boolean set = (bits >>> bit) % 2 == 1;
                if ((required >>> bit) % 2 == 1 && !set) selected = false;
                if ((excluded >>> bit) % 2 == 1 && set) selected = false;
            }
            if (selected) expected[kept++] = rows[i];
        }
        long[] actual = M3IndexMaskCompactor.selectLong(rows, flags, required, excluded);
        require(actual != rows);
        require(Arrays.equals(Arrays.copyOf(expected, kept), actual));
        if (actual.length != 0) {
            long original = rows[0];
            actual[0] ^= 0x3210L;
            require(rows[0] == original);
        }
    }
    public static void main(String[] args) {
        long[] rows = new long[64];
        byte[] flags = new byte[64];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = switch (i % 5) {
                case 0 -> Long.MIN_VALUE;
                case 1 -> Long.MAX_VALUE;
                default -> i % 7 - 3L;
            };
            flags[i] = (byte) (i * 73 + (i >>> 2));
        }
        long[] originalRows = rows.clone();
        byte[] originalFlags = flags.clone();
        for (int required = 0; required < 256; required++)
            for (int excluded = 0; excluded < 256; excluded++)
                sample(rows, flags, required, excluded);
        require(Arrays.equals(rows, originalRows));
        require(Arrays.equals(flags, originalFlags));
        Random random = new Random(0x4d334c4f4e47L);
        for (int trial = 0; trial < 512; trial++) {
            int size = random.nextInt(1025);
            rows = new long[size]; flags = new byte[size];
            for (int i = 0; i < size; i++) rows[i] = random.nextLong();
            random.nextBytes(flags);
            sample(rows, flags, random.nextInt(256), random.nextInt(256));
        }
        System.out.println("PASS M3RequiredExcludedLongProbe checks=" + checks);
    }
}
