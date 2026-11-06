// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Real owner contract: caller receives fresh, ordered, correctly sized primitive index arrays. */
//FLLDCIM: RELOCATE - donor blob ce374af4b0af at ac5feec00a6d; recipe m3/collections/recipe/entryless-lane-kernel-20261106/relocate.py v3
class M3IndexMaskLaneLazyResultTest {
    @Test void zeroMatchAndZeroWindowReturnIndependentEmptyArrays() {
        long[] keys = {0, 1, Long.MIN_VALUE, Long.MAX_VALUE};
        long[] values = {0, 1, Long.MIN_VALUE, Long.MAX_VALUE};
        byte[] masked = new byte[keys.length];
        int[] a = M3IndexMaskCompactor.selectJava(keys, values, masked, 0, keys.length,
                Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE);
        int[] b = M3IndexMaskCompactor.selectJava(keys, values, masked, 0, keys.length,
                Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE);
        if (a.length != 0 || b.length != 0 || a == b) {
            throw new AssertionError("zero-match result must be fresh and empty");
        }
        int[] empty = M3IndexMaskCompactor.selectJava(keys, values, masked, 4, 0,
                Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE);
        if (empty.length != 0) throw new AssertionError("zero count");
    }

    @Test void denseSelectionIsIndependentFromSourceAndStable() {
        final int length = 4096;
        long[] keys = new long[length], values = new long[length];
        byte[] live = new byte[length];
        Arrays.fill(live, (byte) 3);
        int[] actual = M3IndexMaskCompactor.selectJava(keys, values, live, 0, length,
                Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE);
        if (actual.length != length) throw new AssertionError("dense output length");
        for (int index = 0; index < length; index++) {
            if (actual[index] != index) throw new AssertionError("physical slot order");
        }
        actual[0] = -123;
        int[] next = M3IndexMaskCompactor.selectJava(keys, values, live, 0, length,
                Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE);
        if (next[0] != 0 || actual == next || live[0] != 3) {
            throw new AssertionError("caller-owned result array must be independent");
        }
    }

    @Test void sparseAndPartialWindowsUseExactArrayLengthWithoutReordering() {
        Random random = new Random(0x5A17D00F6EEDL);
        for (int trial = 0; trial < 1500; trial++) {
            int length = switch (trial % 7) {
                case 0 -> 0; case 1 -> 1; case 2 -> 32; case 3 -> 33;
                case 4 -> 64; case 5 -> 257; default -> random.nextInt(1025);
            };
            long[] keys = new long[length], values = new long[length];
            byte[] state = new byte[length];
            for (int i = 0; i < length; i++) {
                keys[i] = random.nextInt(101) - 50;
                values[i] = random.nextInt(101) - 50;
                state[i] = (byte) random.nextInt(256);
            }
            int start = random.nextInt(length + 1);
            int count = random.nextInt(length - start + 1);
            int[] expected = new int[count];
            int found = 0;
            for (int i = start; i < start + count; i++) {
                if ((state[i] & 1) != 0 && keys[i] >= -20 && keys[i] <= 20
                        && values[i] >= -20 && values[i] <= 20) expected[found++] = i;
            }
            int[] result = M3IndexMaskCompactor.selectJava(keys, values, state,
                    start, count, -20, 20, -20, 20);
            if (!Arrays.equals(result, Arrays.copyOf(expected, found))) {
                throw new AssertionError("selection mismatch: trial=" + trial);
            }
        }
    }
}
