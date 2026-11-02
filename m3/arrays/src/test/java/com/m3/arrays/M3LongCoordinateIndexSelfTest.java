// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;
import java.util.Map;

/** Execute with javac/java directly; JUnit calls the same oracle. */
public final class M3LongCoordinateIndexSelfTest {
    private M3LongCoordinateIndexSelfTest() {}

    public static void main(String[] args) {
        run();
    }

    public static void run() {
        Random random = new Random(0x4d334c434f4f5244L);
        int cases = 0;
        boolean nativeOn = M3ArrayNative.isLoaded()
                || System.getProperty("m3.arrays.native.path") != null;
        for (int iteration = 0; iteration < 1700; iteration++) {
            TreeMap<Long,Long> reference = new TreeMap<>();
            int n = random.nextInt(175);
            for (int i = 0; i < n; i++) {
                long key = switch (random.nextInt(7)) {
                    case 0 -> Long.MIN_VALUE + random.nextInt(500);
                    case 1 -> Long.MAX_VALUE - random.nextInt(500);
                    case 2 -> (long) Integer.MAX_VALUE + random.nextInt(1000);
                    case 3 -> (1L << 50) + random.nextInt(1000);
                    default -> random.nextInt(6000) - 3000;
                };
                long amount = random.nextInt(101) - 50;
                reference.put(key, amount);
            }
            long[] keys = new long[reference.size()];
            long[] values = new long[keys.length];
            int index = 0;
            for (Map.Entry<Long,Long> item : reference.entrySet()) {
                // Oracle-only TreeMap; the tested production implementation is entry-free.
                keys[index] = item.getKey();
                values[index++] = item.getValue();
            }
            M3LongCoordinateIndex actual = new M3LongCoordinateIndex(keys, values);
            assertThat(actual.size() == keys.length, "size");
            if (keys.length > 0) {
                keys[0]++; values[0]++;
            }
            assertThat(Arrays.equals(actual.keysToArray(), reference.keySet().stream()
                    .mapToLong(Long::longValue).toArray()), "detached keys");
            cases++;
            for (int trial = 0; trial < 45; trial++) {
                long key = switch (trial % 9) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> (long) Integer.MAX_VALUE;
                    case 3 -> 0L;
                    default -> random.nextInt(6000) - 3000;
                };
                int lower = reference.headMap(key, false).size();
                int upper = reference.headMap(key, true).size();
                assertThat(actual.lowerBound(key) == lower, "lower bound");
                assertThat(actual.upperBound(key) == upper, "upper bound");
                assertThat(actual.contains(key) == reference.containsKey(key), "membership");
                assertThat(actual.valueOrDefault(key, 98765) == reference.getOrDefault(key,98765L),
                        "zero is a legal stored value");
                cases += 4;
                if (nativeOn) {
                    assertThat(actual.lowerBoundRequiredNative(key) == lower, "JNI lower bound");
                    assertThat(actual.upperBoundRequiredNative(key) == upper, "JNI upper bound");
                    cases += 2;
                }
                long another = random.nextInt(6000) - 3000;
                long from = Math.min(key, another);
                long to = Math.max(key, another);
                int expectedCount = reference.subMap(from, true, to, false).size();
                long expectedSum = 0;
                for (long val : reference.subMap(from,true,to,false).values()) {
                    expectedSum += val;
                }
                assertThat(actual.countInRange(from,to) == expectedCount, "range count");
                assertThat(actual.sumInRange(from,to) == expectedSum, "range sum");
                int[] ordinals = actual.ordinalsInRange(from,to);
                assertThat(ordinals[1] - ordinals[0] == expectedCount, "range ordinals");
                cases += 3;
            }
        }
        long[] edge = {Long.MIN_VALUE, 0L, Long.MAX_VALUE};
        M3LongCoordinateIndex extremes = new M3LongCoordinateIndex(edge,
                new long[] {-1, 0, 1});
        assertThat(extremes.lowerBound(Long.MIN_VALUE) == 0, "min bound");
        assertThat(extremes.upperBound(Long.MAX_VALUE) == 3, "max bound");
        assertThat(extremes.countInRange(Long.MIN_VALUE,Long.MAX_VALUE) == 2,
                "upper-excluded maximum");
        assertThat(extremes.sumAll() == 0, "sum all");
        fail(IllegalArgumentException.class, () -> new M3LongCoordinateIndex(new long[] {1, 1},
                new long[] {3, 4}));
        fail(IllegalArgumentException.class, () -> new M3LongCoordinateIndex(new long[] {2, 1},
                new long[] {3, 4}));
        fail(IllegalArgumentException.class, () -> extremes.sumInRange(7, 2));
        fail(ArithmeticException.class, () -> new M3LongCoordinateIndex(new long[] {1, 2},
                new long[] {Long.MAX_VALUE, 1}));
        fail(ArithmeticException.class, () -> new M3LongCoordinateIndex(new long[] {1, 2, 3},
                new long[] {Long.MIN_VALUE, -1, 1}));
        M3SparseLongIntArray sparse = new M3SparseLongIntArray(Long.MAX_VALUE);
        sparse.set(0L, 42);
        sparse.set((long) Integer.MAX_VALUE + 1L, -8);
        sparse.set(Long.MAX_VALUE-1, 9);
        M3LongCoordinateIndex materialized = M3LongCoordinateIndex.fromSparse(sparse);
        assertThat(materialized.size()==3, "sparse size");
        assertThat(materialized.keyAt(0)==0L, "sparse first");
        assertThat(materialized.keyAt(2)==Long.MAX_VALUE-1, "sparse last");
        assertThat(materialized.sumAll()==43, "sparse sum");
        sparse.clear();
        assertThat(materialized.size()==3, "sparse detached after clear");
        cases += 9;
        System.out.println("M3_LONG_COORDINATE_ORACLE_PASS checks="+cases+" native="+nativeOn);
    }

    private static void fail(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch(Throwable e) { if(type.isInstance(e))return; throw new AssertionError("wrong exception",e); }
        throw new AssertionError("expected: "+type);
    }
    private static void assertThat(boolean valid, String detail) {
        if (!valid) throw new AssertionError(detail);
    }
}
