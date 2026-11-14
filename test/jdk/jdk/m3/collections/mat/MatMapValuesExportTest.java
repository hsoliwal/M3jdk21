/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Primitive map exports preserve sorted-key order and independent snapshot ownership
 * @modules jdk.m3.collections/jdk.internal.m3.collections.mat
 * @run main MatMapValuesExportTest
 */

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import jdk.internal.m3.collections.mat.MatAdaptiveIntIntMap;
import jdk.internal.m3.collections.mat.MatAdaptiveLongLongMap;
import jdk.internal.m3.collections.mat.MatCollectionPolicy;
import jdk.internal.m3.collections.mat.MatStorageKind;

public final class MatMapValuesExportTest {
    private static final long SEED = 0x4d41545f4558504cL;
    private static final int[][] INT_ENCODINGS = new int[3][3];
    private static final int[][] LONG_ENCODINGS = new int[3][3];
    private static long checks;
    private static long digest = 0xcbf29ce484222325L;
    private static int cases;

    public static void main(String[] args) {
        verifyClassOrigins();
        for (MatCollectionPolicy policy : MatCollectionPolicy.values()) {
            exerciseInt("empty", new int[0], policy, "SORTED", false);
            exerciseLong("empty", new long[0], policy, "SORTED");
            for (int key : new int[] {Integer.MIN_VALUE, 0, Integer.MAX_VALUE}) {
                exerciseInt("singleton-" + key, new int[] {key}, policy,
                        singletonEncoding(policy), false);
            }
            for (long key : new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}) {
                exerciseLong("singleton-" + key, new long[] {key}, policy,
                        singletonEncoding(policy));
            }
            for (int size : new int[] {17, 129}) {
                exerciseInt("sparse-" + size, sparseInts(size), policy,
                        sparseEncoding(policy, size), false);
                exerciseLong("sparse-" + size, sparseLongs(size), policy,
                        sparseEncoding(policy, size));
            }
            for (int size : new int[] {63, 64, 65}) {
                exerciseInt("word-size-" + size, denseInts(-32, size, -1),
                        policy, "DIRECT", false);
                exerciseLong("word-size-" + size, denseLongs(-32, size, -1),
                        policy, "DIRECT");
            }
            int[] intBases = {Integer.MIN_VALUE, -97, Integer.MAX_VALUE - 192};
            long[] longBases = {Long.MIN_VALUE, -97, Long.MAX_VALUE - 192};
            for (int hole = 63; hole <= 65; hole++) {
                int index = hole - 63;
                exerciseInt("direct-hole-" + hole,
                        denseInts(intBases[index], 193, hole), policy, "DIRECT", false);
                exerciseLong("direct-hole-" + hole,
                        denseLongs(longBases[index], 193, hole), policy, "DIRECT");
            }
            exerciseInt("direct-three-holes", denseInts(-97, 193, 3),
                    policy, "DIRECT", false);
            exerciseLong("direct-three-holes", denseLongs(-97, 193, 3),
                    policy, "DIRECT");
            // The int API also permits direct storage to be disabled explicitly.
            exerciseInt("disabled-direct", denseInts(-32, 65, -1), policy,
                    sparseEncoding(policy, 65), true);
        }
        verifyCoverage("int", INT_ENCODINGS);
        verifyCoverage("long", LONG_ENCODINGS);
        System.out.printf(Locale.ROOT,
                "MatMapValuesExportTest PASS cases=%d checks=%d digest=%016x seed=%016x%n",
                cases, checks, digest, SEED);
    }

    private static void verifyClassOrigins() {
        String expectedModule = System.getProperty("mat.test.expectedModule");
        for (Class<?> type : new Class<?>[] {
                MatAdaptiveIntIntMap.class, MatAdaptiveLongLongMap.class}) {
            String module = type.getModule().getName();
            System.out.println("class=" + type.getName() + " module=" + module
                    + " origin=" + type.getResource(type.getSimpleName() + ".class"));
            if (expectedModule != null && !expectedModule.equals(module)) {
                throw new AssertionError(type.getName() + " expected module="
                        + expectedModule + " actual=" + module);
            }
        }
    }

    private static String singletonEncoding(MatCollectionPolicy policy) {
        return policy == MatCollectionPolicy.MIN_MEMORY ? "SORTED" : "DIRECT";
    }

    private static String sparseEncoding(MatCollectionPolicy policy, int size) {
        return switch (policy) {
            case MIN_MEMORY -> "SORTED";
            case BALANCED -> size < 128 ? "SORTED" : "HASH";
            case FAST_LOOKUP -> "HASH";
        };
    }

    private static int[] sparseInts(int size) {
        int[] keys = new int[size];
        keys[0] = Integer.MIN_VALUE;
        keys[1] = Integer.MAX_VALUE;
        for (int i = 2; i < size; i++) {
            keys[i] = (int) (0x9e3779b9L * i);
        }
        return keys;
    }

    private static long[] sparseLongs(int size) {
        long[] keys = new long[size];
        keys[0] = Long.MIN_VALUE;
        keys[1] = Long.MAX_VALUE;
        for (int i = 2; i < size; i++) {
            keys[i] = 0x9e3779b97f4a7c15L * i;
        }
        return keys;
    }

    private static int[] denseInts(int base, int span, int hole) {
        int[] keys = new int[span];
        int count = 0;
        for (int offset = 0; offset < span; offset++) {
            if (included(offset, hole)) {
                keys[count++] = base + offset;
            }
        }
        return Arrays.copyOf(keys, count);
    }

    private static long[] denseLongs(long base, int span, int hole) {
        long[] keys = new long[span];
        int count = 0;
        for (int offset = 0; offset < span; offset++) {
            if (included(offset, hole)) {
                keys[count++] = base + offset;
            }
        }
        return Arrays.copyOf(keys, count);
    }

    private static boolean included(int offset, int hole) {
        if (hole < 0) {
            return true;
        }
        if (hole == 3 && offset >= 63 && offset <= 65) {
            return false;
        }
        return offset != hole && offset != 31 && offset != 95
                && offset != 127 && offset != 128 && offset != 129;
    }

    private static int[] insertionOrder(int size, String label) {
        int[] order = new int[size];
        for (int i = 0; i < size; i++) {
            order[i] = i;
        }
        Random random = new Random(SEED ^ label.hashCode());
        for (int i = size - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int old = order[i];
            order[i] = order[j];
            order[j] = old;
        }
        return order;
    }

    private static int intValue(int index) {
        return switch (index % 8) {
            case 0 -> 0;
            case 1 -> -1;
            case 2 -> Integer.MIN_VALUE;
            case 3 -> Integer.MAX_VALUE;
            default -> index * -104729;
        };
    }

    private static long longValue(int index) {
        return switch (index % 8) {
            case 0 -> 0;
            case 1 -> -1;
            case 2 -> Long.MIN_VALUE;
            case 3 -> Long.MAX_VALUE;
            default -> index * -0x1_0000_0001L;
        };
    }

    private static void exerciseInt(String name, int[] input, MatCollectionPolicy policy,
                                    String expectedEncoding, boolean disableDirect) {
        String label = "int/" + policy + "/" + name;
        trace(label);
        cases++;
        MatAdaptiveIntIntMap.Builder builder = MatAdaptiveIntIntMap.builder(policy);
        if (disableDirect) {
            builder.maxDirectSpan(0);
        }
        TreeMap<Integer, Integer> oracle = new TreeMap<>();
        for (int index : insertionOrder(input.length, name)) {
            int key = input[index];
            int value = intValue(index);
            // Duplicate puts use distinct values, including a final zero.
            builder.put(key, ~value).put(key, value);
            oracle.put(key, ~value);
            oracle.put(key, value);
        }
        MatAdaptiveIntIntMap first = builder.build();
        check(first.encoding().name().equals(expectedEncoding), label + " encoding");
        INT_ENCODINGS[policy.ordinal()][first.encoding().ordinal()]++;
        verifyInt(first, oracle, label + "/first");

        TreeMap<Integer, Integer> frozen = new TreeMap<>(oracle);
        if (!oracle.isEmpty()) {
            int key = oracle.firstKey();
            int replacement = ~oracle.get(key);
            builder.put(key, replacement);
            oracle.put(key, replacement);
        }
        int absent = 0;
        while (oracle.containsKey(absent)) {
            absent++;
        }
        builder.put(absent, Integer.MIN_VALUE);
        oracle.put(absent, Integer.MIN_VALUE);
        MatAdaptiveIntIntMap second = builder.build();
        check(first != second, label + " independent builds");
        verifyInt(first, frozen, label + "/after-builder-reuse");
        verifyInt(second, oracle, label + "/rebuilt");
    }

    private static void exerciseLong(String name, long[] input, MatCollectionPolicy policy,
                                     String expectedEncoding) {
        String label = "long/" + policy + "/" + name;
        trace(label);
        cases++;
        MatAdaptiveLongLongMap.Builder builder = MatAdaptiveLongLongMap.builder(policy);
        TreeMap<Long, Long> oracle = new TreeMap<>();
        for (int index : insertionOrder(input.length, name)) {
            long key = input[index];
            long value = longValue(index);
            builder.put(key, ~value).put(key, value);
            oracle.put(key, ~value);
            oracle.put(key, value);
        }
        MatAdaptiveLongLongMap first = builder.build();
        check(first.encoding().name().equals(expectedEncoding), label + " encoding");
        LONG_ENCODINGS[policy.ordinal()][first.encoding().ordinal()]++;
        verifyLong(first, oracle, policy, label + "/first");

        TreeMap<Long, Long> frozen = new TreeMap<>(oracle);
        if (!oracle.isEmpty()) {
            long key = oracle.firstKey();
            long replacement = ~oracle.get(key);
            builder.put(key, replacement);
            oracle.put(key, replacement);
        }
        long absent = 0;
        while (oracle.containsKey(absent)) {
            absent++;
        }
        builder.put(absent, Long.MIN_VALUE);
        oracle.put(absent, Long.MIN_VALUE);
        MatAdaptiveLongLongMap second = builder.build();
        check(first != second, label + " independent builds");
        verifyLong(first, frozen, policy, label + "/after-builder-reuse");
        verifyLong(second, oracle, policy, label + "/rebuilt");
    }

    private static void verifyInt(MatAdaptiveIntIntMap map, TreeMap<Integer, Integer> oracle,
                                  String label) {
        equal(oracle.size(), map.size(), label + " size");
        equal(oracle.size(), map.plan().size(), label + " plan size");
        MatStorageKind expectedKind = switch (map.encoding()) {
            case DIRECT -> MatStorageKind.MAP_DIRECT;
            case SORTED -> MatStorageKind.MAP_SORTED;
            case HASH -> MatStorageKind.MAP_HASH;
        };
        check(map.plan().kind() == expectedKind, label + " plan kind");
        long payload = map.primitivePayloadBytes();
        int[] expectedKeys = new int[oracle.size()];
        int[] expectedValues = new int[oracle.size()];
        int index = 0;
        for (var entry : oracle.entrySet()) {
            expectedKeys[index] = entry.getKey();
            expectedValues[index++] = entry.getValue();
        }
        int[] keys = map.keys();
        int[] values = map.valuesBySortedKey();
        int[] otherKeys = map.keys();
        int[] otherValues = map.valuesBySortedKey();
        check(keys != otherKeys && values != otherValues && keys != values,
                label + " fresh arrays including empty exports");
        equal(expectedKeys, keys, label + " ordered keys");
        equal(expectedValues, values, label + " values in oracle key order");
        for (int i = 0; i < keys.length; i++) {
            keys[i] = ~keys[i];
            values[i] = ~values[i];
        }
        equal(expectedKeys, otherKeys, label + " keys independent of prior export");
        equal(expectedValues, otherValues, label + " values independent of prior export");
        equal(expectedKeys, map.keys(), label + " keys detached from map");
        equal(expectedValues, map.valuesBySortedKey(), label + " values detached from map");
        equal(payload, map.primitivePayloadBytes(), label + " retained payload unchanged");
        for (var entry : oracle.entrySet()) {
            int key = entry.getKey();
            int expected = entry.getValue();
            check(map.containsKey(key), label + " present key " + key);
            equal(expected, map.get(key), label + " point lookup " + key);
            equal(expected, map.getOrDefault(key, ~expected), label + " present default " + key);
        }
        for (int key : intProbes(oracle)) {
            if (!oracle.containsKey(key)) {
                check(!map.containsKey(key), label + " absent key " + key);
                equal(0, map.getOrDefault(key, 0), label + " zero fallback " + key);
                equal(Integer.MIN_VALUE, map.getOrDefault(key, Integer.MIN_VALUE),
                        label + " negative fallback " + key);
                try {
                    map.get(key);
                    throw new AssertionError(label + " missing get returned for " + key);
                } catch (IllegalArgumentException expected) {
                    check(expected.getClass() == IllegalArgumentException.class,
                            label + " missing exception class");
                    check(("missing key: " + key).equals(expected.getMessage()),
                            label + " missing exception message");
                }
            }
        }
    }

    private static void verifyLong(MatAdaptiveLongLongMap map, TreeMap<Long, Long> oracle,
                                   MatCollectionPolicy policy, String label) {
        equal(oracle.size(), map.size(), label + " size");
        check(map.policy() == policy, label + " policy preserved");
        long payload = map.primitivePayloadBytes();
        long[] expectedKeys = new long[oracle.size()];
        long[] expectedValues = new long[oracle.size()];
        int index = 0;
        for (var entry : oracle.entrySet()) {
            expectedKeys[index] = entry.getKey();
            expectedValues[index++] = entry.getValue();
        }
        long[] keys = map.keys();
        long[] values = map.valuesBySortedKey();
        long[] otherKeys = map.keys();
        long[] otherValues = map.valuesBySortedKey();
        check(keys != otherKeys && values != otherValues && keys != values,
                label + " fresh arrays including empty exports");
        equal(expectedKeys, keys, label + " ordered keys");
        equal(expectedValues, values, label + " values in oracle key order");
        for (int i = 0; i < keys.length; i++) {
            keys[i] = ~keys[i];
            values[i] = ~values[i];
        }
        equal(expectedKeys, otherKeys, label + " keys independent of prior export");
        equal(expectedValues, otherValues, label + " values independent of prior export");
        equal(expectedKeys, map.keys(), label + " keys detached from map");
        equal(expectedValues, map.valuesBySortedKey(), label + " values detached from map");
        equal(payload, map.primitivePayloadBytes(), label + " retained payload unchanged");
        for (var entry : oracle.entrySet()) {
            long key = entry.getKey();
            long expected = entry.getValue();
            check(map.containsKey(key), label + " present key " + key);
            equal(expected, map.get(key), label + " point lookup " + key);
            equal(expected, map.getOrDefault(key, ~expected), label + " present default " + key);
        }
        for (long key : longProbes(oracle)) {
            if (!oracle.containsKey(key)) {
                check(!map.containsKey(key), label + " absent key " + key);
                equal(0, map.getOrDefault(key, 0), label + " zero fallback " + key);
                equal(Long.MIN_VALUE, map.getOrDefault(key, Long.MIN_VALUE),
                        label + " negative fallback " + key);
                try {
                    map.get(key);
                    throw new AssertionError(label + " missing get returned for " + key);
                } catch (IllegalArgumentException expected) {
                    check(expected.getClass() == IllegalArgumentException.class,
                            label + " missing exception class");
                    check(("missing key: " + key).equals(expected.getMessage()),
                            label + " missing exception message");
                }
            }
        }
    }

    private static TreeSet<Integer> intProbes(TreeMap<Integer, Integer> oracle) {
        TreeSet<Integer> probes = new TreeSet<>(Arrays.asList(
                Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -1, 0, 1,
                63, 64, 65, Integer.MAX_VALUE - 1, Integer.MAX_VALUE));
        if (!oracle.isEmpty()) {
            int first = oracle.firstKey();
            int last = oracle.lastKey();
            if (first != Integer.MIN_VALUE) {
                probes.add(first - 1);
            }
            if (last != Integer.MAX_VALUE) {
                probes.add(last + 1);
            }
            long span = (long) last - first;
            if (span <= 256) {
                for (long offset = 0; offset <= span; offset++) {
                    probes.add((int) (first + offset));
                }
            }
        }
        return probes;
    }

    private static TreeSet<Long> longProbes(TreeMap<Long, Long> oracle) {
        TreeSet<Long> probes = new TreeSet<>(Arrays.asList(
                Long.MIN_VALUE, Long.MIN_VALUE + 1, -1L, 0L, 1L,
                63L, 64L, 65L, Long.MAX_VALUE - 1, Long.MAX_VALUE));
        if (!oracle.isEmpty()) {
            long first = oracle.firstKey();
            long last = oracle.lastKey();
            if (first != Long.MIN_VALUE) {
                probes.add(first - 1);
            }
            if (last != Long.MAX_VALUE) {
                probes.add(last + 1);
            }
            // An overflowed difference is negative and cannot enter the bounded scan.
            long span = last - first;
            if (span >= 0 && span <= 256) {
                for (int offset = 0; offset <= span; offset++) {
                    probes.add(first + offset);
                }
            }
        }
        return probes;
    }

    private static void verifyCoverage(String width, int[][] observed) {
        int[] totals = new int[3];
        for (MatCollectionPolicy policy : MatCollectionPolicy.values()) {
            int[] row = observed[policy.ordinal()];
            check(Arrays.stream(row).sum() > 0, width + " exercised " + policy);
            for (int encoding = 0; encoding < totals.length; encoding++) {
                totals[encoding] += row[encoding];
            }
            System.out.printf(Locale.ROOT,
                    "%s policy=%s initialMaps DIRECT=%d SORTED=%d HASH=%d%n",
                    width, policy, row[0], row[1], row[2]);
        }
        for (int encoding = 0; encoding < totals.length; encoding++) {
            check(totals[encoding] > 0, width + " actual encoding " + encoding);
        }
    }

    private static void equal(int[] expected, int[] actual, String label) {
        check(Arrays.equals(expected, actual), label);
        mix(actual.length);
        for (int value : actual) {
            mix(value);
        }
    }

    private static void equal(long[] expected, long[] actual, String label) {
        check(Arrays.equals(expected, actual), label);
        mix(actual.length);
        for (long value : actual) {
            mix(value);
        }
    }

    private static void equal(long expected, long actual, String label) {
        check(expected == actual, label + ": expected=" + expected + " actual=" + actual);
        mix(actual);
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label + " at check=" + checks);
        }
        mix(1);
    }

    private static void trace(String label) {
        for (int i = 0; i < label.length(); i++) {
            mix(label.charAt(i));
        }
    }

    private static void mix(long value) {
        digest = (digest ^ value) * 0x100000001b3L;
    }
}
