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

final class M3ParallelIntAddProviderTest {
    private static BulkTask task(int n) {
        return new BulkTask(M3ParallelIntAddProvider.OPERATION_ID, n, 0,
                List.of("left", "right"), List.of("sum"));
    }

    @Test
    void serialOracleAndDeterministicProviderSelection() {
        M3ParallelIntAddProvider provider = M3ParallelIntAddProvider.reference();
        BulkScheduler scheduler = new BulkScheduler(List.of(provider));
        assertEquals(List.of("m3-parallel-java"), scheduler.providerIds());
        assertEquals(provider, scheduler.select(task(4)));
        int[] left = {Integer.MAX_VALUE, Integer.MIN_VALUE, -1, 0};
        int[] right = {1, -1, -1, 0};
        BulkExecution result = scheduler.execute(task(4), Map.of("left", left, "right", right));
        assertArrayEquals(new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE, -2, 0},
                (int[]) result.outputs().get("sum"));
        assertArrayEquals(new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE, -1, 0}, left);
        assertNotSame(left, result.outputs().get("sum"));
        assertTrue(result.receiptRoot().matches("[0-9a-f]{64}"));
    }

    @Test
    void refusesUnreviewedShapesWithoutFallback() {
        M3ParallelIntAddProvider provider = M3ParallelIntAddProvider.reference();
        assertFalse(provider.supports(new BulkTask("other", 1, 0,
                List.of("left", "right"), List.of("sum"))));
        assertFalse(provider.supports(new BulkTask(M3ParallelIntAddProvider.OPERATION_ID, 2, 1,
                List.of("left", "right"), List.of("sum"))));
        assertFalse(provider.supports(new BulkTask(M3ParallelIntAddProvider.OPERATION_ID,
                M3ParallelIntAddProvider.MAX_ITEMS + 1L, 0,
                List.of("left", "right"), List.of("sum"))));
        assertThrows(IllegalArgumentException.class, () -> provider.execute(task(1),
                Map.of("left", new int[1], "right", new int[1], "unexpected", new int[1])));
        assertThrows(IllegalArgumentException.class, () -> provider.execute(task(2),
                Map.of("left", new int[2], "right", new int[1])));
    }

    @Test
    void randomDifferentialAgainstIndependentJavaArithmetic() {
        var provider = M3ParallelIntAddProvider.reference();
        Random rng = new Random(0x4d33414444415252L);
        for (int trial = 0; trial < 80; trial++) {
            int length = 1 + rng.nextInt(512);
            int[] left = rng.ints(length).toArray();
            int[] right = rng.ints(length).toArray();
            int[] expected = new int[length];
            for (int i = 0; i < length; i++) expected[i] = left[i] + right[i];
            int[] actual = (int[]) provider.execute(task(length),
                    Map.of("left", left, "right", right)).outputs().get("sum");
            assertArrayEquals(expected, actual);
        }
    }
}
