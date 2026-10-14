/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 */
package com.m3.parallel.bulk;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import jdk.internal.vm.parallel.BulkExecution;
import jdk.internal.vm.parallel.BulkTask;

final class M3ParallelNativeBitCountProviderTest {
    private static BulkTask task(int count) {
        return new BulkTask(M3ParallelNativeBitCountProvider.OPERATION_ID, count, 0,
                List.of("bits"), List.of("counts"));
    }

    @Test
    void realJniMatchesJavaOracleAndProtectsCallerArrays() {
        Assumptions.assumeTrue(!System.getProperty(
                M3ParallelNativeBitCountProvider.LIBRARY_PROPERTY, "").isBlank(),
                "dedicated required-JNI lane supplies a native library");
        var provider = new M3ParallelNativeBitCountProvider();
        int[] source = {0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE,
                0x5555_5555, 0xaaaa_aaaa, 0x0f0f_0f0f, 1};
        BulkExecution result = provider.execute(task(source.length), Map.of("bits", source));
        int[] output = (int[]) result.output("counts");
        assertArrayEquals(new int[] {0, 32, 1, 31, 16, 16, 16, 1}, output);
        assertEquals(provider.id(), result.provider());
        assertTrue(result.receiptRoot().matches("[0-9a-f]{64}"));
        assertNotSame(source, output);
        output[0] = 100;
        assertEquals(0, ((int[]) result.output("counts"))[0]);
        assertArrayEquals(new int[] {0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE,
                0x5555_5555, 0xaaaa_aaaa, 0x0f0f_0f0f, 1}, source);
    }

    @Test
    void refusesUnknownOpOrWrongSchema() {
        Assumptions.assumeTrue(!System.getProperty(
                M3ParallelNativeBitCountProvider.LIBRARY_PROPERTY, "").isBlank(),
                "dedicated required-JNI lane supplies a native library");
        var provider = new M3ParallelNativeBitCountProvider();
        assertFalse(provider.supports(new BulkTask("unreviewed", 1, 0,
                List.of("bits"), List.of("counts"))));
        assertFalse(provider.supports(new BulkTask(M3ParallelNativeBitCountProvider.OPERATION_ID,
                1, 1, List.of("bits"), List.of("counts"))));
        assertThrows(IllegalArgumentException.class,
                () -> provider.execute(task(2), Map.of("bits", new int[1])));
        assertThrows(IllegalArgumentException.class,
                () -> provider.execute(task(1), Map.of("bits", new long[1])));
    }

    @Test
    void deterministicSeededDifferential() {
        Assumptions.assumeTrue(!System.getProperty(
                M3ParallelNativeBitCountProvider.LIBRARY_PROPERTY, "").isBlank(),
                "dedicated required-JNI lane supplies a native library");
        var provider = new M3ParallelNativeBitCountProvider();
        Random random = new Random(0x4d334e4154495645L);
        for (int trial = 0; trial < 160; trial++) {
            int n = 1 + random.nextInt(640);
            int[] source = random.ints(n).toArray();
            int[] original = source.clone();
            BulkExecution output = provider.execute(task(n), Map.of("bits", source));
            int[] actual = (int[]) output.output("counts");
            int[] expected = new int[n];
            for (int i = 0; i < n; i++) expected[i] = Integer.bitCount(original[i]);
            assertArrayEquals(expected, actual);
            assertArrayEquals(original, source);
            assertEquals(output.receiptRoot(),
                    provider.execute(task(n), Map.of("bits", original)).receiptRoot());
        }
    }
}
