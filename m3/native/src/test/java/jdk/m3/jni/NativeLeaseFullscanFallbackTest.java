// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Java/JNI preference parity; C11 missing-export fixture is a separate executable proof. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/bee80508c334ec11d9f82ad05106b556f6036546/synexia-primitives/src/test/java/com/synexia/primitives/M3NativeLeaseFullscanFallbackTest.java blob efaeb2b05ee9; recipe m3/native/qualification/twin-fixes-20261107/apply.py v1
final class NativeLeaseFullscanFallbackTest {
    @Test
    void nativePreferenceMatchesJavaAcrossBoundedStates() {
        long[] ids = {1, 2, 3, 4, 5, 6, 7};
        int[] leases = new int[ids.length];
        int[] pins = new int[ids.length];
        byte[] states = {0, 0, 0, 1, 2, 0, 0};
        long[] reference = new long[ids.length];
        long[] candidate = new long[ids.length];
        for (int round = 0; round < 10_000; round++) {
            leases[2] = round & 1;
            pins[1] = (round >>> 1) & 1;
            pins[6] = (round >>> 2) & 1;
            Arrays.fill(reference, -17L);
            Arrays.fill(candidate, -17L);
            int n = NativeLeaseScan.collect(ids, leases, pins, states,
                    ids.length, reference, false);
            assertEquals(n, NativeLeaseScan.collect(ids, leases, pins, states,
                    ids.length, candidate, true));
            assertArrayEquals(reference, candidate);
        }
    }

    @Test
    void emptyAndFullscanKeepStablePrefix() {
        long[] ids = {1, 2, 3, 4};
        int[] leases = {0, 1, 0, 0};
        int[] pins = {0, 0, 1, 0};
        byte[] states = {0, 0, 0, 0};
        long[] out = {-9, -9, -9, -9};
        assertEquals(0, NativeLeaseScan.collect(ids, leases, pins, states,
                0, new long[0], true));
        assertEquals(2, NativeLeaseScan.collect(ids, leases, pins, states,
                ids.length, out, true));
        assertArrayEquals(new long[]{1, 4, -9, -9}, out);
    }

    @Test
    void invalidPreflightRefusesPartialOutput() {
        long[] ids = {1, 2, 3};
        int[] leases = {0, -1, 0};
        int[] pins = {0, 0, 0};
        byte[] states = {0, 0, 0};
        long[] output = {88, 88, 88};
        assertThrows(IllegalArgumentException.class,
                () -> NativeLeaseScan.collect(ids, leases, pins, states,
                        ids.length, output, true));
        assertArrayEquals(new long[]{88, 88, 88}, output);
        leases[1] = 0;
        assertThrows(IllegalArgumentException.class,
                () -> NativeLeaseScan.collect(ids, leases, pins, states,
                        ids.length, new long[2], true));
        assertThrows(IllegalArgumentException.class,
                () -> NativeLeaseScan.collect(ids, leases, pins, states,
                        ids.length, ids, true));
    }

    @Test
    void boundedWindowCursorMatchesJavaAndProgresses() {
        long[] ids = {1, 2, 3, 4, 5, 6};
        int[] leases = {0, 1, 0, 0, 0, 0};
        int[] pins = {0, 0, 0, 1, 0, 0};
        byte[] states = {0, 0, 0, 0, 2, 0};
        int first = 0;
        int count = 0;
        while (first < ids.length) {
            long[] javaOut = {-77, -77};
            long[] nativeOut = {-77, -77};
            long expected = NativeLeaseScan.collectWindow(ids, leases, pins,
                    states, ids.length, first, javaOut, false);
            long candidate = NativeLeaseScan.collectWindow(ids, leases, pins,
                    states, ids.length, first, nativeOut, true);
            assertEquals(expected, candidate);
            assertArrayEquals(javaOut, nativeOut);
            int next = NativeLeaseScan.nextIndex(candidate);
            assertTrue(next > first);
            count += NativeLeaseScan.written(candidate);
            first = next;
        }
        assertEquals(3, count);
    }
}
