/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Independent selected-source contract; run with java -ea, no JUnit/third-party dependency. */
public final class M3TombstoneReuseSelfTest {
    private static long checks;
    private M3TombstoneReuseSelfTest() { }
    private static void require(boolean condition, String failure) {
        checks++;
        if (!condition) throw new AssertionError(failure);
    }
    private static int mix(long value) {
        long x = value;
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdl;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53l;
        x ^= x >>> 33;
        return (int) x;
    }
    private static long[] colliding(int count) {
        long[] result = new long[count];
        int next = 0;
        for (long candidate = Long.MIN_VALUE; next < count; candidate++) {
            if ((mix(candidate) & 7) == 3) result[next++] = candidate;
        }
        return result;
    }
    private static void boundary() {
        long[] input = colliding(6);
        var set = new M3LongHashSet();
        var map = new M3LongLongHashMap();
        for (int i = 0; i < 5; i++) {
            require(set.add(input[i]), "set initial admission");
            require(map.put(input[i], i + 120L), "map initial admission");
        }
        require(set.remove(input[0]), "set tombstone creation");
        require(map.remove(input[0]), "map tombstone creation");
        var beforeSet = set.iterator();
        var beforeMap = map.keyIterator();
        require(set.add(input[5]), "set tombstone reuse");
        require(map.put(input[5], 999), "map tombstone reuse");
        requireFailsConcurrent(beforeSet, "reusing set slot invalidates iterator");
        requireFailsConcurrent(beforeMap, "reusing map slot invalidates iterator");
        // Old source grows to 16; the recipe-generated source retains eight slots.
        require(set.capacity() == 8, "set allocated array despite reachable deleted slot");
        require(map.capacity() == 8, "map allocated array despite reachable deleted slot");
        require(set.size() == 5 && map.size() == 5, "size after reuse");
        for (int i = 1; i <= 5; i++) {
            require(set.contains(input[i]), "set member lost in probe cluster");
            require(map.containsKey(input[i]), "map key lost in probe cluster");
            require(map.getOrDefault(input[i], -1) == (i == 5 ? 999L : i + 120L), "map value shift");
        }
        var stableSet = set.iterator();
        var stableMap = map.keyIterator();
        require(!set.add(input[5]), "duplicate member not a structural write");
        require(!map.put(input[5], 999), "map replacement not a structural write");
        require(stableSet.hasNext() && stableMap.hasNext(), "duplicate/replacement preserved epochs");
        var setIterator = set.iterator();
        var mapIterator = map.keyIterator();
        require(set.add(Long.MAX_VALUE), "set iterator mutation");
        require(map.put(Long.MAX_VALUE, 77), "map iterator mutation");
        boolean setFailed = false, mapFailed = false;
        try { setIterator.hasNext(); } catch (ConcurrentModificationException expected) { setFailed = true; }
        try { mapIterator.hasNext(); } catch (ConcurrentModificationException expected) { mapFailed = true; }
        require(setFailed && mapFailed, "fail-fast epochs lost");
    }
    private static long key(Random random, int step) {
        return switch (step % 23) {
            case 0 -> 0L;
            case 1 -> Long.MIN_VALUE;
            case 2 -> Long.MAX_VALUE;
            default -> random.nextInt(127) - 63L;
        };
    }
    private static void differential() {
        Random random = new Random(0x534E59584D334A4CL);
        M3LongHashSet actualSet = new M3LongHashSet();
        M3LongLongHashMap actualMap = new M3LongLongHashMap();
        Set<Long> oracleSet = new HashSet<>();
        Map<Long, Long> oracleMap = new HashMap<>();
        for (int step = 0; step < 140_000; step++) {
            long k = key(random, step);
            long v = random.nextLong();
            switch (random.nextInt(10)) {
                case 0, 1, 2 -> require(actualSet.add(k) == oracleSet.add(k), "set add parity " + step);
                case 3 -> require(actualSet.remove(k) == oracleSet.remove(k), "set remove parity " + step);
                case 4 -> require(actualSet.contains(k) == oracleSet.contains(k), "set contains parity " + step);
                case 5, 6 -> require(actualMap.put(k, v) == (oracleMap.put(k, v) == null), "map put parity " + step);
                case 7 -> require(actualMap.remove(k) == (oracleMap.remove(k) != null), "map remove parity " + step);
                case 8 -> require(actualMap.containsKey(k) == oracleMap.containsKey(k), "map contains parity " + step);
                default -> require(actualMap.getOrDefault(k, -888) == oracleMap.getOrDefault(k, -888L), "map value parity " + step);
            }
            if ((step & 127) == 0) {
                require(actualSet.size() == oracleSet.size(), "set size parity");
                require(actualMap.size() == oracleMap.size(), "map size parity");
            }
        }
        for (Long id : oracleSet) require(actualSet.contains(id), "final set ID");
        for (Map.Entry<Long, Long> item : oracleMap.entrySet()) {
            require(actualMap.getOrDefault(item.getKey(), -1) == item.getValue(), "final map pair");
        }
    }
    private static void requireFailsConcurrent(M3LongIterator iterator, String what) {
        try {
            iterator.hasNext();
            throw new AssertionError(what + " did not fail fast");
        } catch (ConcurrentModificationException expected) {
            checks++;
        }
    }
    private static void shape() {
        for (Class<?> type : new Class<?>[] {M3LongHashSet.class, M3LongLongHashMap.class}) {
            for (Field field : type.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                require(!Map.Entry.class.isAssignableFrom(fieldType), "retained Entry " + field);
                if (fieldType.isArray()) {
                    require(fieldType.getComponentType().isPrimitive(), "non-primitive lane " + field);
                }
            }
        }
    }
    public static void main(String[] args) {
        boundary();
        differential();
        shape();
        System.out.println("PASS M3TombstoneReuseSelfTest checks=" + checks
                + " saved_set_array_payload_bytes=72 saved_map_array_payload_bytes=136");
    }
}
