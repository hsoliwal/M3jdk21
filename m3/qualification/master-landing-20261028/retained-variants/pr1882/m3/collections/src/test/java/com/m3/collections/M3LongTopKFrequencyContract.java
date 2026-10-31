// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;

/**
 * Original Java 21 and JNI LC #347 Top K Frequent Elements oracle.
 * Java-only metadata structures below are test reference code; M3 production
 * retains strictly primitive arrays and bounded native frequency heap pages.
 */
public final class M3LongTopKFrequencyContract {
    private static long checks;
    private M3LongTopKFrequencyContract() { }

    private static void equal(long[] expected, long[] actual, String why) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(why + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }

    private static long[] oracle(long[] sorted, int limit) {
        Map<Long, Integer> frequencies = new TreeMap<>();
        for (long item : sorted) frequencies.merge(item, 1, Integer::sum);
        List<Map.Entry<Long, Integer>> entries = new ArrayList<>(frequencies.entrySet());
        entries.sort((a, b) -> {
            int order = Integer.compare(b.getValue(), a.getValue());
            return order != 0 ? order : Long.compare(a.getKey(), b.getKey());
        });
        return entries.stream().limit(limit).mapToLong(Map.Entry::getKey).toArray();
    }

    private static void expect(Class<? extends Throwable> type, Runnable task) {
        checks++;
        try {
            task.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("wrong exception " + error, error);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    private static void verify(long[] sorted, int limit, boolean requireNative) {
        long[] copy = sorted.clone();
        long[] expected = oracle(sorted, limit);
        equal(expected, M3LongRankBatchNative.topKFrequentJava(sorted, limit),
                "pure primitive Java run-frequency heap");
        equal(expected, M3LongRankBatchNative.topKFrequent(sorted, limit),
                "optional native dispatcher");
        equal(expected, M3LongRankIndex.copyOf(sorted).topKFrequent(limit),
                "original public rank snapshot owner");
        if (requireNative) {
            equal(expected, M3LongRankBatchNative.topKFrequent(sorted, limit, true),
                    "required original m3rankbatch JNI ABI");
        }
        equal(copy, sorted, "original signed-long payload stays unchanged");
    }

    public static long contracts(boolean requireNative) {
        checks = 0;
        if (requireNative && !M3LongRankBatchNative.nativeAvailable()) {
            throw new AssertionError("m3rankbatch JNI requested but unavailable");
        }
        long[][] edges = {
            {}, {1}, {1, 1, 1}, {Long.MIN_VALUE, Long.MAX_VALUE},
            {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MAX_VALUE},
            {-4, -4, -2, -2, -1, 0, 0, 7, 7, 7, 7},
            {Long.MIN_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE},
            {0, 0, 1, 1, 2, 2, 3, 3, 4, 4}
        };
        for (long[] edge : edges) {
            for (int limit : new int[] {0, 1, 2, 3, 6, 100}) {
                verify(edge, limit, requireNative);
            }
        }
        Random random = new Random(0x347C0DEL);
        for (int size : new int[] {
                0, 1, 2, 3, 31, 32, 63, 64, 127, 255, 256, 257,
                511, 512, 513, 1024, 4096}) {
            for (int round = 0; round < 45; round++) {
                long[] sorted = new long[size];
                for (int i = 0; i < size; i++) {
                    int item = random.nextInt(16);
                    sorted[i] = switch (item) {
                        case 0 -> Long.MIN_VALUE;
                        case 1 -> Long.MAX_VALUE;
                        case 2 -> random.nextLong();
                        default -> random.nextInt(55) - 27L;
                    };
                }
                Arrays.sort(sorted);
                for (int limit : new int[] {0, 1, 2, 5, size / 2, size + 3}) {
                    verify(sorted, limit, requireNative);
                }
            }
        }
        for (int limit : new int[] {-1, -100}) {
            expect(IllegalArgumentException.class,
                    () -> M3LongRankBatchNative.topKFrequentJava(new long[0], limit));
            expect(IllegalArgumentException.class,
                    () -> M3LongRankBatchNative.topKFrequent(new long[0], limit));
            expect(IllegalArgumentException.class,
                    () -> M3LongRankIndex.copyOf(new long[0]).topKFrequent(limit));
        }
        expect(NullPointerException.class,
                () -> M3LongRankBatchNative.topKFrequent(null, 1));
        expect(NullPointerException.class,
                () -> M3LongRankBatchNative.topKFrequentJava(null, 1));
        if (!requireNative && !M3LongRankBatchNative.nativeAvailable()) {
            expect(UnsatisfiedLinkError.class,
                    () -> M3LongRankBatchNative.topKFrequent(new long[0], 0, true));
        }
        System.out.println("PASS M3_LONG_TOPK_FREQUENCY checks=" + checks
                + " nativeRequired=" + requireNative);
        return checks;
    }

    public static void main(String[] args) {
        boolean nativeRequired = args.length == 1
                && "--native-required".equals(args[0]);
        if (args.length > 1 || args.length == 1 && !nativeRequired) {
            throw new IllegalArgumentException("expected optional --native-required");
        }
        contracts(nativeRequired);
    }
}
