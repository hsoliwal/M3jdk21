/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Differential oracle for the empty-hash-map value lookup atom; dependency free. */
public final class M3LongLongHashMapEmptyValueProof {
    private static long checks;
    private static long checksum;

    private M3LongLongHashMapEmptyValueProof() { }

    private static void equal(boolean expected, boolean actual, String operation) {
        checks++;
        if (expected != actual) {
            throw new AssertionError(operation + " case " + checks + ": expected "
                    + expected + " actual " + actual);
        }
        checksum = Long.rotateLeft(checksum ^ (actual ? 0xa0761d6478bd642fL : checks), 13);
    }

    private static void equalSize(int expected, LongLongHashMap actual) {
        checks++;
        if (actual.size() != expected) {
            throw new AssertionError("size mismatch at case " + checks);
        }
    }

    public static void main(String[] args) {
        LongLongHashMap largeEmpty = new LongLongHashMap(32768);
        for (long value : new long[]{0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE}) {
            equal(false, largeEmpty.containsValue(value), "new empty large map");
        }
        largeEmpty.put(Long.MIN_VALUE, 0L);
        equal(true, largeEmpty.containsValue(0L), "zero value present");
        equal(false, largeEmpty.containsValue(Long.MAX_VALUE), "missing value in nonempty map");
        largeEmpty.remove(Long.MIN_VALUE);
        equal(false, largeEmpty.containsValue(0L), "empty after last removal");
        largeEmpty.put(Long.MAX_VALUE, Long.MIN_VALUE);
        equal(true, largeEmpty.containsValue(Long.MIN_VALUE), "minimum value present");
        largeEmpty.clear();
        equal(false, largeEmpty.containsValue(Long.MIN_VALUE), "empty after clear");
        equalSize(0, largeEmpty);

        LongLongHashMap actual = new LongLongHashMap(128);
        Map<Long, Long> oracle = new HashMap<>();
        Random random = new Random(0x7057060FFL);
        for (int operation = 0; operation < 80_000; operation++) {
            long key = operation % 97 == 0 ? Long.MIN_VALUE
                    : operation % 97 == 1 ? Long.MAX_VALUE : random.nextInt(521) - 260L;
            long value = operation % 17 == 0 ? 0L
                    : operation % 17 == 1 ? Long.MIN_VALUE : random.nextInt(2047) - 1023L;
            switch (operation % 8) {
                case 0, 1 -> {
                    boolean wasPresent = oracle.containsKey(key);
                    equal(!wasPresent, actual.put(key, value), "put");
                    oracle.put(key, value);
                }
                case 2 -> {
                    boolean wasPresent = oracle.containsKey(key);
                    equal(wasPresent, actual.remove(key), "remove");
                    oracle.remove(key);
                }
                case 3 -> {
                    boolean expected = oracle.containsKey(key)
                            && oracle.get(key).longValue() == value;
                    equal(expected, actual.replace(key, value, Long.MAX_VALUE), "replace");
                    if (expected) oracle.put(key, Long.MAX_VALUE);
                }
                case 4, 5 -> equal(oracle.containsValue(value),
                        actual.containsValue(value), "value lookup");
                case 6 -> {
                    equal(oracle.containsKey(key), actual.containsKey(key), "key lookup");
                    equal(oracle.getOrDefault(key, -773L),
                            actual.getOrDefault(key, -773L), "getOrDefault");
                }
                case 7 -> {
                    if (operation % 127 == 7) {
                        actual.clear();
                        oracle.clear();
                    }
                    equal(oracle.containsValue(0L), actual.containsValue(0L), "zero value");
                }
                default -> throw new AssertionError("unreachable");
            }
            if ((operation & 127) == 0) equalSize(oracle.size(), actual);
        }
        actual.clear();
        equal(false, actual.containsValue(0L), "final cleared empty");
        equal(false, actual.containsValue(Long.MIN_VALUE), "final cleared missing");
        equalSize(0, actual);

        System.out.printf("M3_EMPTY_VALUE_LOOKUP_PASS checks=%d checksum=%016x%n",
                checks, checksum);
    }

    private static void equal(long expected, long actual, String operation) {
        checks++;
        if (expected != actual) {
            throw new AssertionError(operation + " mismatch at case " + checks);
        }
        checksum = Long.rotateLeft(checksum ^ expected, 11);
    }
}
