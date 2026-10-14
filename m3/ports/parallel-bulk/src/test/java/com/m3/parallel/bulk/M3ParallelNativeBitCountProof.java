/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 */
package com.m3.parallel.bulk;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jdk.internal.vm.parallel.BulkExecution;
import jdk.internal.vm.parallel.BulkScheduler;
import jdk.internal.vm.parallel.BulkTask;

/** Strict real-JNI differential proof; does not claim GPU execution. */
public final class M3ParallelNativeBitCountProof {
    private static long checks;
    private M3ParallelNativeBitCountProof() {}

    private static void check(boolean valid) {
        checks++;
        if (!valid) throw new AssertionError("native bitcount check=" + checks);
    }

    private static BulkTask task(int length) {
        return new BulkTask(M3ParallelNativeBitCountProvider.OPERATION_ID, length, 0,
                List.of("bits"), List.of("counts"));
    }

    private static void rejected(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("malformed bitcount operation was admitted");
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length > 0 && "missing".equals(arguments[0])) {
            System.clearProperty(M3ParallelNativeBitCountProvider.LIBRARY_PROPERTY);
            try { new M3ParallelNativeBitCountProvider(); }
            catch (UnsatisfiedLinkError expected) { System.out.println("PASS required-native missing-library rejection"); return; }
            throw new AssertionError("missing native library was accepted");
        }
        var provider = new M3ParallelNativeBitCountProvider();
        var scheduler = new BulkScheduler(List.of(provider));
        check(scheduler.providerIds().equals(List.of("m3-bitcount-jni-cpu")));
        int[] edge = {0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE,
                0x55555555, 0xaaaaaaaa, 0x0f0f0f0f, 1};
        int[] clone = edge.clone();
        BulkExecution receipt = scheduler.execute(task(edge.length), Map.of("bits", edge));
        check(Arrays.equals((int[]) receipt.output("counts"),
                new int[] {0, 32, 1, 31, 16, 16, 16, 1}));
        check(Arrays.equals(edge, clone));
        check(receipt.provider().equals(provider.id()));
        check(receipt.receiptRoot().matches("[0-9a-f]{64}"));
        check(receipt.receiptRoot().equals(
                scheduler.execute(task(edge.length), Map.of("bits", clone)).receiptRoot()));
        int[] defensive = (int[]) receipt.output("counts");
        defensive[0] = 999;
        check(((int[]) receipt.output("counts"))[0] == 0);
        rejected(() -> provider.execute(task(3), Map.of("bits", new int[2])));
        rejected(() -> provider.execute(task(1), Map.of("bits", new long[1])));
        rejected(() -> provider.execute(task(1), Map.of("bits", new int[1], "extra", 0)));
        check(!provider.supports(new BulkTask("other", 1, 0, List.of("bits"), List.of("counts"))));
        check(!provider.supports(new BulkTask(M3ParallelNativeBitCountProvider.OPERATION_ID,
                1, 1, List.of("bits"), List.of("counts"))));
        Random random = new Random(0x4d334e4154495645L);
        for (int round = 0; round < 256; round++) {
            int n = 1 + random.nextInt(513);
            int[] values = random.ints(n).toArray();
            int[] before = values.clone();
            int[] actual = (int[]) scheduler.execute(task(n), Map.of("bits", values)).output("counts");
            check(actual.length == n);
            for (int i = 0; i < n; i++) check(actual[i] == Integer.bitCount(before[i]));
            check(Arrays.equals(values, before));
        }
        int[] large = random.ints(M3ParallelNativeBitCountProvider.MAX_ITEMS).toArray();
        int[] actual = (int[]) scheduler.execute(task(large.length), Map.of("bits", large)).output("counts");
        for (int i = 0; i < large.length; i++) check(actual[i] == Integer.bitCount(large[i]));
        try (var pool = Executors.newFixedThreadPool(4)) {
            List<Future<Boolean>> futures = java.util.stream.IntStream.range(0, 8).mapToObj(i ->
                    pool.submit(() -> {
                        int[] test = new int[511];
                        for (int x = 0; x < test.length; x++) test[x] = i * 7919 + x;
                        int[] output = (int[]) provider.execute(task(test.length),
                                Map.of("bits", test)).output("counts");
                        for (int x = 0; x < test.length; x++) {
                            if (output[x] != Integer.bitCount(test[x])) return false;
                        }
                        return true;
                    })).toList();
            for (Future<Boolean> future : futures) check(future.get());
        }
        System.out.println("PASS real JNI signed bitcount checks=" + checks);
    }
}
