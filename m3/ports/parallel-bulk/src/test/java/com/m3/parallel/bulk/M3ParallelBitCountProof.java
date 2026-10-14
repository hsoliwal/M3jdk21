/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 */
package com.m3.parallel.bulk;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import jdk.internal.vm.parallel.BulkExecution;
import jdk.internal.vm.parallel.BulkScheduler;
import jdk.internal.vm.parallel.BulkTask;

/** Standalone real-source Java21 differential gate; independent of Maven/JUnit. */
public final class M3ParallelBitCountProof {
    private static long checks;

    private M3ParallelBitCountProof() {}

    private static void require(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("bitcount differential check=" + checks);
    }

    private static BulkTask task(int size) {
        return new BulkTask(M3ParallelBitCountProvider.OPERATION_ID, size, 0,
                List.of("bits"), List.of("counts"));
    }

    public static void main(String[] args) {
        var provider = M3ParallelBitCountProvider.reference();
        var scheduler = new BulkScheduler(List.of(provider));
        require(scheduler.providerIds().equals(List.of("m3-bitcount-java")));
        int[] special = {0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE,
                0x5555_5555, 0xaaaa_aaaa, 0x0f0f_0f0f, 1};
        int[] before = special.clone();
        BulkExecution edge = scheduler.execute(task(special.length), Map.of("bits", special));
        require(Arrays.equals((int[]) edge.output("counts"),
                new int[] {0, 32, 1, 31, 16, 16, 16, 1}));
        require(Arrays.equals(special, before));
        require(edge.receiptRoot().matches("[0-9a-f]{64}"));
        require(!provider.supports(new BulkTask("unreviewed", 1, 0,
                List.of("bits"), List.of("counts"))));
        reject(() -> provider.execute(task(2), Map.of("bits", new int[1])));
        reject(() -> provider.execute(task(1), Map.of("bits", new long[1])));
        reject(() -> provider.execute(task(1),
                Map.of("bits", new int[1], "unexpected", new int[1])));

        Random random = new Random(0x4d33424954434f55L);
        for (int trial = 0; trial < 160; trial++) {
            int length = 1 + random.nextInt(640);
            int[] input = random.ints(length).toArray();
            int[] copy = input.clone();
            int[] result = (int[]) scheduler.execute(task(length),
                    Map.of("bits", input)).output("counts");
            for (int i = 0; i < length; i++) {
                require(result[i] == Integer.bitCount(copy[i]));
            }
            require(Arrays.equals(input, copy));
        }
        System.out.println("PASS real-source checks=" + checks);
    }

    private static void reject(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            checks++;
            return;
        }
        throw new AssertionError("missing required input rejection");
    }
}
