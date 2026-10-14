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
import org.junit.jupiter.api.Test;
import jdk.internal.vm.parallel.BulkExecution;
import jdk.internal.vm.parallel.BulkScheduler;
import jdk.internal.vm.parallel.BulkTask;

final class M3ParallelBitCountProviderTest {
    private static BulkTask task(int n) {
        return new BulkTask(M3ParallelBitCountProvider.OPERATION_ID, n, 0,
                List.of(M3ParallelBitCountProvider.INPUT),
                List.of(M3ParallelBitCountProvider.OUTPUT));
    }

    @Test
    void bitCountIncludesSignBitsAndZeroWithoutAliasing() {
        M3ParallelBitCountProvider provider = M3ParallelBitCountProvider.reference();
        BulkScheduler scheduler = new BulkScheduler(List.of(provider));
        assertEquals(List.of("m3-bitcount-java"), scheduler.providerIds());
        int[] source = {0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE,
                0x5555_5555, 0xaaaa_aaaa, 0x0f0f_0f0f, 1};
        int[] before = source.clone();
        BulkExecution result = scheduler.execute(task(source.length),
                Map.of(M3ParallelBitCountProvider.INPUT, source));
        assertArrayEquals(new int[] {0, 32, 1, 31, 16, 16, 16, 1},
                (int[]) result.output(M3ParallelBitCountProvider.OUTPUT));
        assertArrayEquals(before, source);
        assertNotSame(source, result.output(M3ParallelBitCountProvider.OUTPUT));
        assertTrue(result.receiptRoot().matches("[0-9a-f]{64}"));
    }

    @Test
    void refusesUnreviewedOperationsAndMalformedInputs() {
        var provider = M3ParallelBitCountProvider.reference();
        assertFalse(provider.supports(new BulkTask("m3.other", 1, 0,
                List.of("bits"), List.of("counts"))));
        assertFalse(provider.supports(new BulkTask(M3ParallelBitCountProvider.OPERATION_ID, 1, 1,
                List.of("bits"), List.of("counts"))));
        assertFalse(provider.supports(new BulkTask(M3ParallelBitCountProvider.OPERATION_ID,
                M3ParallelBitCountProvider.MAX_ITEMS + 1L, 0,
                List.of("bits"), List.of("counts"))));
        assertThrows(IllegalArgumentException.class, () -> provider.execute(task(1),
                Map.of("bits", new int[1], "extra", new int[1])));
        assertThrows(IllegalArgumentException.class, () -> provider.execute(task(2),
                Map.of("bits", new int[1])));
        assertThrows(IllegalArgumentException.class, () -> provider.execute(task(1),
                Map.of("bits", new long[1])));
    }

    @Test
    void seededDifferentialWithIndependentReference() {
        M3ParallelBitCountProvider provider = M3ParallelBitCountProvider.reference();
        Random rng = new Random(0x4d33424954434f55L);
        for (int trial = 0; trial < 160; trial++) {
            int count = 1 + rng.nextInt(640);
            int[] input = rng.ints(count).toArray();
            int[] expected = new int[count];
            for (int i = 0; i < count; i++) expected[i] = Integer.bitCount(input[i]);
            BulkExecution result = provider.execute(task(count), Map.of("bits", input));
            int[] actual = (int[]) result.output("counts");
            assertArrayEquals(expected, actual);
            assertEquals("m3-bitcount-java", result.provider());
        }
    }
}
