/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Actual M3JDK primitive map/set operations versus independent JDK value oracles.
 * Boxed keys and JDK collections exist only inside this test, never the runtime path.
 */
public final class M3IndexMaskKeyedSelfTest {
    private static long assertions;

    private M3IndexMaskKeyedSelfTest() { }

    public static void main(String[] args) {
        for (int seed = 0; seed < 64; seed++) exercise(seed);
        boundaries();
        storage();
        System.out.println("PASS M3IndexMaskKeyedSelfTest checks=" + assertions);
    }

    private static void exercise(int seed) {
        Random random = new Random(0x4d334b45594544L + seed);
        M3LongHashSet set = new M3LongHashSet(0);
        M3LongLongHashMap map = new M3LongLongHashMap(0);
        Set<Long> expectedSet = new HashSet<>();
        Map<Long, Long> expectedMap = new HashMap<>();

        for (long key : new long[] {Long.MIN_VALUE, Long.MAX_VALUE, 0L, -1L, 1L}) {
            check(set.add(key) == expectedSet.add(key), "initial set");
            check(map.put(key, transform(key)) == (expectedMap.put(key, transform(key)) == null),
                    "initial map");
        }

        for (int step = 0; step < 1024; step++) {
            long key = value(random, step);
            if ((step & 7) == 0) {
                check(set.remove(key) == expectedSet.remove(key), "set churn removal");
                check(map.remove(key) == (expectedMap.remove(key) != null),
                        "map churn removal");
            } else {
                check(set.add(key) == expectedSet.add(key), "set insert");
                long result = transform(key) ^ step;
                boolean newKey = !expectedMap.containsKey(key);
                expectedMap.put(key, result);
                check(map.put(key, result) == newKey, "map insert/replacement");
            }
            if ((step % 37) != 0) continue;

            long[] beforeSet = set.toArray();
            long[] beforeMap = map.keysToArray();
            byte[] setStates = new byte[beforeSet.length];
            byte[] mapStates = new byte[beforeMap.length];
            int mask = random.nextInt(256);
            for (int i = 0; i < setStates.length; i++) setStates[i] = (byte) random.nextInt(256);
            for (int i = 0; i < mapStates.length; i++) mapStates[i] = (byte) random.nextInt(256);
            long[] selectedSet = selectionOracle(beforeSet, setStates, mask);
            long[] selectedMap = selectionOracle(beforeMap, mapStates, mask);

            M3LongIterator setIterator = set.iterator();
            M3LongIterator mapIterator = map.keyIterator();
            check(set.retainByStateMask(setStates, mask)
                    == beforeSet.length - selectedSet.length, "set removed count");
            check(map.retainByStateMask(mapStates, mask)
                    == beforeMap.length - selectedMap.length, "map removed count");

            for (int i = 0; i < beforeSet.length; i++) {
                if (!matches(setStates[i], mask)) expectedSet.remove(beforeSet[i]);
            }
            for (int i = 0; i < beforeMap.length; i++) {
                if (!matches(mapStates[i], mask)) expectedMap.remove(beforeMap[i]);
            }
            eq(selectedSet, set.toArray());
            eq(selectedMap, map.keysToArray());

            if (selectedSet.length != beforeSet.length) {
                fails(ConcurrentModificationException.class, setIterator::hasNext);
            } else {
                check(setIterator.hasNext() == (beforeSet.length != 0), "no-op set cursor");
            }
            if (selectedMap.length != beforeMap.length) {
                fails(ConcurrentModificationException.class, mapIterator::hasNext);
            } else {
                check(mapIterator.hasNext() == (beforeMap.length != 0), "no-op map cursor");
            }
            for (long present : set.toArray()) {
                check(expectedSet.contains(present), "set survivor");
            }
            for (long present : map.keysToArray()) {
                check(expectedMap.containsKey(present), "map survivor");
                check(map.getOrDefault(present, -9L) == expectedMap.get(present),
                        "mapped value preserved");
            }
            check(set.size() == expectedSet.size(), "set cardinality");
            check(map.size() == expectedMap.size(), "map cardinality");
            // Exercise tombstone slot reuse immediately after mask compaction.
            long reinserted = value(random, step + 203);
            check(set.add(reinserted) == expectedSet.add(reinserted), "set reuse");
            long replacement = transform(reinserted);
            boolean newKey = !expectedMap.containsKey(reinserted);
            expectedMap.put(reinserted, replacement);
            check(map.put(reinserted, replacement) == newKey, "map reuse");
        }
        check(set.size() == expectedSet.size(), "final set cardinality");
        check(map.size() == expectedMap.size(), "final map cardinality");
        for (Long key : expectedSet) check(set.contains(key), "final set membership");
        for (Map.Entry<Long, Long> e : expectedMap.entrySet()) {
            check(map.containsKey(e.getKey()), "final map membership");
            check(map.getOrDefault(e.getKey(), -9L) == e.getValue(), "final map value");
        }
    }

    private static void boundaries() {
        M3LongHashSet set = new M3LongHashSet();
        M3LongLongHashMap map = new M3LongLongHashMap();
        set.add(0); set.add(Long.MIN_VALUE);
        map.put(0, Long.MAX_VALUE); map.put(Long.MIN_VALUE, 0);
        long[] priorSet = set.toArray(), priorMap = map.keysToArray();
        for (int invalid : new int[] {-1, 256, Integer.MAX_VALUE}) {
            fails(IllegalArgumentException.class,
                    () -> set.retainByStateMask(new byte[] {0, 0}, invalid));
            fails(IllegalArgumentException.class,
                    () -> map.retainByStateMask(new byte[] {0, 0}, invalid));
        }
        fails(NullPointerException.class, () -> set.retainByStateMask(null, 1));
        fails(NullPointerException.class, () -> map.retainByStateMask(null, 1));
        fails(IllegalArgumentException.class, () -> set.retainByStateMask(new byte[1], 1));
        fails(IllegalArgumentException.class, () -> map.retainByStateMask(new byte[3], 1));
        eq(priorSet, set.toArray()); eq(priorMap, map.keysToArray());
        // Zero mask matches even zero-state slots and does not invalidate cursors.
        M3LongIterator a = set.iterator(), b = map.keyIterator();
        check(set.retainByStateMask(new byte[] {0, 0}, 0) == 0, "zero mask set");
        check(map.retainByStateMask(new byte[] {0, 0}, 0) == 0, "zero mask map");
        check(a.hasNext() && b.hasNext(), "zero mask cursors");
        // All entries can be removed without moving or allocating key/value arrays.
        check(set.retainByStateMask(new byte[] {0, 0}, 255) == 2, "empty set");
        check(map.retainByStateMask(new byte[] {0, 0}, 255) == 2, "empty map");
        check(set.isEmpty() && map.isEmpty(), "empty owners");
        check(set.add(0), "reinsert zero");
        check(map.put(0, 0), "reinsert zero map");
        check(set.contains(0) && map.containsKey(0), "zero sentinel independence");
    }

    private static void storage() {
        for (Class<?> owner : new Class<?>[] {M3LongHashSet.class, M3LongLongHashMap.class}) {
            for (Field field : owner.getDeclaredFields()) {
                Class<?> type = field.getType();
                if (type.isArray()) {
                    check(type.getComponentType().isPrimitive(), "no reference array");
                }
            }
        }
    }

    private static long value(Random random, int step) {
        return switch (step % 17) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            default -> random.nextInt(101) - 50L;
        };
    }

    private static long transform(long key) {
        return key ^ 0x5e735eed19L;
    }

    private static long[] selectionOracle(long[] source, byte[] states, int mask) {
        long[] temporary = new long[source.length];
        int used = 0;
        for (int i = 0; i < source.length; i++) {
            if (matches(states[i], mask)) temporary[used++] = source[i];
        }
        return Arrays.copyOf(temporary, used);
    }

    private static boolean matches(byte state, int mask) {
        return ((state & 255) & mask) == mask;
    }

    private static void check(boolean ok, String reason) {
        assertions++;
        if (!ok) throw new AssertionError(reason);
    }

    private static void eq(long[] expected, long[] actual) {
        assertions++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("expected " + Arrays.toString(expected)
                    + " actual " + Arrays.toString(actual));
        }
    }

    private static void fails(Class<? extends Throwable> type, Runnable operation) {
        assertions++;
        try { operation.run(); }
        catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError("wrong failure", thrown);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }
}
