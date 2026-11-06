/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.ConcurrentModificationException;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Real owner tests: interval lower-bound, unsigned count, Java/JNI mask parity. */
final class M3LongRangeSetAtomTest {
    @Test
    void binaryLowerBoundRetainsHalfOpenRemovalContracts() {
        M3LongRangeSet set = new M3LongRangeSet();
        BitSet oracle = new BitSet(256);
        Random random = new Random(0x4d3352414e4745L);
        for (int i = 0; i < 24000; i++) {
            int x = random.nextInt(257), y = random.nextInt(257);
            int from = Math.min(x, y), to = Math.max(x, y);
            if ((i & 1) == 0) {
                set.add(from - 128L, to - 128L);
                oracle.set(from, to);
            } else {
                set.remove(from - 128L, to - 128L);
                oracle.clear(from, to);
            }
            if (i % 41 == 0) {
                set.verifyInvariants();
                for (int k = 0; k < 256; k++) {
                    assertEquals(oracle.get(k), set.contains(k - 128L), "member " + k);
                }
                assertEquals(BigInteger.valueOf(oracle.cardinality()), set.cardinality());
            }
        }
        set.clear();
        for (int i = 0; i < 16000; i++) set.add(i * 3L, i * 3L + 1);
        set.remove(3L * 2000, 3L * 14000);
        assertEquals(4000, set.rangeCount());
        assertFalse(set.contains(3L * 2000));
        assertTrue(set.contains(3L * 14000));
        assertThrows(IllegalArgumentException.class, () -> set.remove(2, 1));
        set.verifyInvariants();
    }

    @Test
    void unsignedAccumulationKeepsExactBigIntegerContracts() {
        M3LongRangeSet set = new M3LongRangeSet();
        assertEquals(BigInteger.ZERO, set.cardinality());
        set.add(Long.MIN_VALUE, Long.MAX_VALUE);
        assertEquals(BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE), set.cardinality());
        set.remove(-5, 6);
        assertEquals(BigInteger.ONE.shiftLeft(64).subtract(BigInteger.valueOf(12)), set.cardinality());
        long[] starts = set.startsToArray();
        long[] ends = set.endsToArray();
        BigInteger reference = BigInteger.ZERO;
        for (int i = 0; i < starts.length; i++) {
            reference = reference.add(BigInteger.valueOf(ends[i]).subtract(BigInteger.valueOf(starts[i])));
        }
        assertEquals(reference, set.cardinality());
        set.clear();
        for (int i = 0; i < 20000; i++) set.add(i * 3L, i * 3L + 1);
        assertEquals(BigInteger.valueOf(20000), set.cardinality());
        set.remove(0, 60000);
        assertEquals(BigInteger.ZERO, set.cardinality());
    }

    @Test
    void directJavaMaskCompactionMatchesNativeWhenPresent() {
        M3LongRangeSet set = new M3LongRangeSet();
        for (int i = 0; i < 4096; i++) set.add(3L * i, 3L * i + 1);
        int[] states = new int[set.rangeCount()];
        Random random = new Random(0x5e1ec7);
        for (int i = 0; i < states.length; i++) states[i] = random.nextInt();
        int[][] masks = {{0, 0}, {1, 0}, {0x80000000, 0},
                {0x80000001, 2}, {-1, 0}, {1, 1}, {0, -1}};
        for (int[] mask : masks) {
            int count = 0;
            for (int state : states) {
                if ((state & mask[0]) == mask[0] && (state & mask[1]) == 0) count++;
            }
            M3LongRangeSet javaResult = set.selectedByState(states, mask[0], mask[1],
                    M3IndexPathMasks.Backend.JAVA);
            assertEquals(count, javaResult.rangeCount());
            int written = 0;
            long[] starts = javaResult.startsToArray();
            long[] ends = javaResult.endsToArray();
            for (int i = 0; i < states.length; i++) {
                if ((states[i] & mask[0]) == mask[0] && (states[i] & mask[1]) == 0) {
                    assertEquals(3L * i, starts[written]);
                    assertEquals(3L * i + 1, ends[written++]);
                }
            }
            if (M3IndexPathMasks.nativeAvailable()) {
                M3LongRangeSet nativeResult = set.selectedByState(states, mask[0], mask[1],
                        M3IndexPathMasks.Backend.NATIVE_REQUIRED);
                assertArrayEquals(starts, nativeResult.startsToArray());
                assertArrayEquals(ends, nativeResult.endsToArray());
            } else {
                assertThrows(UnsatisfiedLinkError.class, () -> set.selectedByState(
                        states, mask[0], mask[1], M3IndexPathMasks.Backend.NATIVE_REQUIRED));
            }
        }
        assertEquals(4096, set.rangeCount(), "selection must be detached");
        assertThrows(IllegalArgumentException.class, () -> set.selectedByState(
                new int[0], 0, 0, M3IndexPathMasks.Backend.JAVA));
        assertThrows(ConcurrentModificationException.class, () -> set.forEachRange(
                (start, end) -> set.remove(start, end)));
    }
}
