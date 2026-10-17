/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Seeded reference-map differential test with no external test dependency. */
public final class M3LongBiMapSelfTest {
    private M3LongBiMapSelfTest() { }

    private static void check(boolean ok, String reason) {
        if (!ok) {
            throw new AssertionError(reason);
        }
    }

    private static long sample(Random random, int step) {
        return switch (step % 13) {
            case 0 -> 0L;
            case 1 -> Long.MIN_VALUE;
            case 2 -> Long.MAX_VALUE;
            default -> random.nextInt(121) - 60L;
        };
    }

    private static void put(Map<Long, Long> left, Map<Long, Long> right,
            long key, long value, boolean forced) {
        Long prior = right.get(value);
        if (prior != null && prior.longValue() != key) {
            if (!forced) {
                throw new IllegalArgumentException("duplicate value");
            }
            left.remove(prior);
            right.remove(value);
        }
        Long old = left.put(key, value);
        if (old != null && old.longValue() != value) {
            right.remove(old);
        }
        right.put(value, key);
    }

    private static void compare(M3LongBiMap actual, Map<Long, Long> left,
            Map<Long, Long> right) {
        actual.verifyInvariants();
        check(actual.size() == left.size(), "size");
        check(actual.inverse().size() == right.size(), "inverse size");
        check(actual.inverse().inverse() == actual, "stable inverse identity");
        check(actual.allocatedPrimitiveBytes() >= 2L * actual.size() * 17L,
                "primitive storage accounting");
        for (Map.Entry<Long, Long> entry : left.entrySet()) {
            long k = entry.getKey();
            long v = entry.getValue();
            check(actual.containsKey(k), "missing key");
            check(actual.containsValue(v), "missing value");
            check(actual.getOrDefault(k, -777L) == v, "forward lookup");
            check(actual.inverse().getOrDefault(v, -777L) == k, "reverse lookup");
        }
        long[] keys = actual.keysToArray();
        Arrays.sort(keys);
        long[] expected = left.keySet().stream().mapToLong(Long::longValue).sorted().toArray();
        check(Arrays.equals(keys, expected), "key snapshot");
        int[] visited = {0};
        actual.forEach((k, v) -> {
            check(left.containsKey(k) && left.get(k) == v, "forEach");
            visited[0]++;
        });
        check(visited[0] == left.size(), "iteration count");
        check(right.size() == left.size(), "oracle cardinality");
    }

    public static void run() {
        M3LongBiMap actual = new M3LongBiMap();
        Map<Long, Long> left = new HashMap<>();
        Map<Long, Long> right = new HashMap<>();
        Random random = new Random(0xB1D1C7103L);
        for (int step = 0; step < 12_000; step++) {
            boolean inverse = random.nextBoolean();
            M3LongBiMap view = inverse ? actual.inverse() : actual;
            Map<Long, Long> expectedKeys = inverse ? right : left;
            Map<Long, Long> expectedValues = inverse ? left : right;
            long key = sample(random, step);
            long value = sample(random, step + 5);
            int operation = random.nextInt(11);
            if (operation <= 3) {
                boolean conflict = expectedValues.containsKey(value)
                        && expectedValues.get(value).longValue() != key;
                boolean before = expectedKeys.containsKey(key);
                if (conflict) {
                    boolean rejected = false;
                    try {
                        view.put(key, value);
                    } catch (IllegalArgumentException ex) {
                        rejected = true;
                    }
                    check(rejected, "duplicate must be rejected before mutation");
                } else {
                    check(view.put(key, value) == !before, "put inserted flag");
                    put(expectedKeys, expectedValues, key, value, false);
                }
            } else if (operation <= 5) {
                boolean before = expectedKeys.containsKey(key);
                check(view.forcePut(key, value) == !before, "forcePut inserted flag");
                put(expectedKeys, expectedValues, key, value, true);
            } else if (operation <= 7) {
                boolean before = expectedKeys.containsKey(key);
                check(view.remove(key) == before, "remove result");
                if (before) {
                    long old = expectedKeys.remove(key);
                    expectedValues.remove(old);
                }
            } else if (operation == 8 && step % 373 == 0) {
                view.clear();
                expectedKeys.clear();
                expectedValues.clear();
            } else {
                check(view.containsKey(key) == expectedKeys.containsKey(key), "containsKey");
                check(view.containsValue(value) == expectedValues.containsKey(value), "containsValue");
                check(view.getOrDefault(key, -777L)
                        == expectedKeys.getOrDefault(key, -777L), "getOrDefault");
            }
            if (step % 17 == 0) {
                compare(actual, left, right);
            }
        }
        compare(actual, left, right);
        check(actual.inverse().inverse() == actual, "inverse involution");
        boolean negativeRejected = false;
        try {
            new M3LongBiMap(-1);
        } catch (IllegalArgumentException ex) {
            negativeRejected = true;
        }
        check(negativeRejected, "negative sizing refusal");
        if (actual.isEmpty()) {
            actual.put(1L, 2L);
        }
        boolean detected = false;
        try {
            actual.forEach((key, value) -> actual.put(key, value + 700_000L));
        } catch (ConcurrentModificationException ex) {
            detected = true;
        }
        check(detected, "callback mutation detection");
    }

    public static void main(String[] args) {
        run();
        System.out.println("M3LongBiMapSelfTest: PASS, 12000 deterministic operations");
    }
}
