/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/** Standalone deterministic Map-semantics proof; no JUnit or external dependency. */
public final class M3Flat3MapContract {
    private static long assertions;
    private M3Flat3MapContract() { }

    private static void check(boolean condition, String problem) {
        assertions++;
        if (!condition) throw new AssertionError(problem);
    }

    private static final class Key {
        final int id;
        final int hash;
        boolean failHash;
        Key(int id, int hash) { this.id = id; this.hash = hash; }
        @Override public int hashCode() {
            if (failHash) throw new Controlled();
            return hash;
        }
        @Override public boolean equals(Object other) { return other instanceof Key k && id == k.id; }
        @Override public String toString() { return "Key(" + id + ")"; }
    }

    private static final class Controlled extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static int capacity(Object map, String field) {
        try {
            Field f = map.getClass().getDeclaredField(field);
            f.setAccessible(true);
            if (f.get(map) instanceof Object[] v) return v.length;
            if (f.get(map) instanceof long[] v) return v.length;
            throw new AssertionError("not a lane: " + field);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static void checkState(M3Flat3Map<Key, Integer> map, int expectedSize) {
        check(map.size() == expectedSize, "size");
        check(map.isEmpty() == (expectedSize == 0), "empty");
        check(capacity(map, "keyLane") == capacity(map, "valueLane"), "lane alignment");
        int cap = capacity(map, "keyLane");
        int words = capacity(map, "stateMaskWords");
        check(words == (cap <= 3 ? 0 : (cap + 31) >>> 5), "packed two-bit state capacity");
        check(map.size() <= cap, "live capacity");
    }

    private static void compare(M3Flat3Map<Key, Integer> actual,
                                Map<Key, Integer> reference, Key[] universe) {
        checkState(actual, reference.size());
        for (Key key : universe) {
            check(actual.containsKey(key) == reference.containsKey(key), "containsKey: " + key);
            check(Objects.equals(actual.get(key), reference.get(key)), "get: " + key);
        }
        check(actual.containsKey(null) == reference.containsKey(null), "null-key contains");
        check(Objects.equals(actual.get(null), reference.get(null)), "null-key get");
        for (Integer candidate : new Integer[]{null, -10, 0, 1, 4, 11, 9999}) {
            check(actual.containsValue(candidate) == reference.containsValue(candidate),
                    "containsValue: " + candidate);
        }
    }

    private static void firstThreeAndTransition() {
        M3Flat3Map<Key, Integer> map = new M3Flat3Map<>();
        HashMap<Key, Integer> expected = new HashMap<>();
        Key[] keys = {new Key(1, 1), new Key(2, 1), new Key(3, 1), new Key(4, 1)};
        check(map.size() == 0 && map.isEmpty(), "new empty");
        check(capacity(map, "keyLane") == 0, "initial no lane");
        for (int i = 0; i < 3; i++) {
            check(Objects.equals(map.put(keys[i], i), expected.put(keys[i], i)), "flat add");
            compare(map, expected, keys);
            check(capacity(map, "keyLane") == 3, "flat capacity remains three");
        }
        check(Objects.equals(map.put(keys[1], 99), expected.put(keys[1], 99)), "flat replace");
        compare(map, expected, keys);
        check(capacity(map, "keyLane") == 3, "flat replacement avoids promotion");
        check(Objects.equals(map.put(keys[3], null), expected.put(keys[3], null)), "promotion add");
        check(capacity(map, "keyLane") == 8, "fourth key enters hash");
        compare(map, expected, keys);
        check(Objects.equals(map.remove(keys[1]), expected.remove(keys[1])), "hashed delete");
        check(Objects.equals(map.put(keys[1], -10), expected.put(keys[1], -10)), "tombstone reuse");
        compare(map, expected, keys);
        for (Key key : keys) {
            check(Objects.equals(map.remove(key), expected.remove(key)), "delete all");
        }
        compare(map, expected, keys);
        check(capacity(map, "keyLane") == 0, "release hash on last removal");
        map.put(keys[0], 1);
        check(capacity(map, "keyLane") == 3, "reenter flat after empty");
        map.clear();
        checkState(map, 0);
        check(capacity(map, "keyLane") == 0, "clear releases lanes");
    }

    private static void structuralEdgeCases() {
        M3Flat3Map<Key, Integer> map = new M3Flat3Map<>();
        HashMap<Key, Integer> expected = new HashMap<>();
        Key[] keys = new Key[48];
        for (int i = 0; i < keys.length; i++) keys[i] = new Key(i, i % 3 == 0 ? 0 : 1);
        check(Objects.equals(map.put(null, null), expected.put(null, null)), "null insert");
        for (Key key : keys) {
            check(Objects.equals(map.put(key, key.id), expected.put(key, key.id)), "collision add");
            compare(map, expected, keys);
        }
        for (int i = 0; i < keys.length; i += 2) {
            check(Objects.equals(map.remove(keys[i]), expected.remove(keys[i])), "tombstone delete");
        }
        compare(map, expected, keys);
        for (int i = 0; i < keys.length; i += 2) {
            check(Objects.equals(map.put(keys[i], -i), expected.put(keys[i], -i)), "tombstone replace");
        }
        compare(map, expected, keys);
        check(Objects.equals(map.put(null, 9999), expected.put(null, 9999)), "null overwrite");
        check(map.containsValue(9999), "present special value");
        check(Objects.equals(map.remove(null), expected.remove(null)), "null remove");
        compare(map, expected, keys);
    }

    private static void allocationFailureAtomicity() {
        M3Flat3Map<Key, Integer> map = new M3Flat3Map<>();
        Key[] keys = new Key[6];
        for (int i = 0; i < keys.length; i++) keys[i] = new Key(i, i + 100);
        for (int i = 0; i < 5; i++) map.put(keys[i], i);
        checkState(map, 5);
        int originalCapacity = capacity(map, "keyLane");
        keys[2].failHash = true;
        boolean failed = false;
        try { map.put(keys[5], 5); } catch (Controlled e) { failed = true; }
        finally { keys[2].failHash = false; }
        check(failed, "rehash failure propagated");
        checkState(map, 5);
        check(capacity(map, "keyLane") == originalCapacity, "no partial rehash publication");
        for (int i = 0; i < 5; i++) {
            check(map.containsKey(keys[i]), "retained old key");
            check(map.get(keys[i]) == i, "retained old value");
        }
        check(!map.containsKey(keys[5]), "failed key not inserted");
        map.put(keys[5], 5);
        checkState(map, 6);
        M3Flat3Map<Key, Integer> small = new M3Flat3Map<>();
        for (int i = 0; i < 3; i++) small.put(keys[i], i);
        keys[1].failHash = true;
        failed = false;
        try { small.put(keys[3], 3); } catch (Controlled e) { failed = true; }
        finally { keys[1].failHash = false; }
        check(failed, "transition hash failure propagated");
        checkState(small, 3);
        check(capacity(small, "keyLane") == 3, "transition remains inline on failure");
        for (int i = 0; i < 3; i++) check(small.get(keys[i]) == i, "transition old value");
    }

    /** Check all packed slots through multiple word boundaries and tombstone reuse. */
    private static void packedWordBoundaries() {
        M3Flat3Map<Key, Integer> actual = new M3Flat3Map<>();
        HashMap<Key, Integer> model = new HashMap<>();
        Key[] keys = new Key[130];
        for (int i = 0; i < keys.length; i++) {
            // Deliberately exercise long probe runs crossing 31/32 and 63/64.
            keys[i] = new Key(2000 + i, i % 4 == 0 ? 7 : 31);
        }
        for (int i = 0; i < keys.length; i++) {
            check(Objects.equals(actual.put(keys[i], i), model.put(keys[i], i)),
                    "packed boundary insertion");
            if (i % 17 == 0 || i == keys.length - 1) {
                compare(actual, model, keys);
                inspectPacking(actual);
            }
        }
        for (int i = 0; i < keys.length; i += 3) {
            check(Objects.equals(actual.remove(keys[i]), model.remove(keys[i])),
                    "packed boundary removal");
            if (i % 23 == 0) inspectPacking(actual);
        }
        inspectPacking(actual);
        compare(actual, model, keys);
        for (int i = 0; i < keys.length; i += 3) {
            check(Objects.equals(actual.put(keys[i], -i), model.put(keys[i], -i)),
                    "packed boundary reinsert");
        }
        inspectPacking(actual);
        compare(actual, model, keys);
    }

    private static void inspectPacking(M3Flat3Map<Key, Integer> map) {
        try {
            Field kField = map.getClass().getDeclaredField("keyLane");
            Field vField = map.getClass().getDeclaredField("valueLane");
            Field sField = map.getClass().getDeclaredField("stateMaskWords");
            kField.setAccessible(true);
            vField.setAccessible(true);
            sField.setAccessible(true);
            Object[] keys = (Object[]) kField.get(map);
            Object[] values = (Object[]) vField.get(map);
            long[] words = (long[]) sField.get(map);
            check(keys.length == values.length, "packed key/value alignment");
            if (keys.length <= 3) {
                check(words.length == 0, "no state allocation for inline entries");
                return;
            }
            check(words.length == ((keys.length + 31) >>> 5), "packed word count");
            int live = 0;
            for (int slot = 0; slot < keys.length; slot++) {
                long state = (words[slot >>> 5] >>> ((slot & 31) << 1)) & 3L;
                check(state <= 2, "two-bit state encoding");
                if (state == 1) {
                    live++;
                    check(map.containsKey(keys[slot]), "every physical live key reachable");
                } else {
                    check(keys[slot] == null, "no dangling key in empty/deleted slot");
                    check(values[slot] == null, "no dangling value in empty/deleted slot");
                }
            }
            check(live == map.size(), "packed live count equals logical size");
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static void randomDifferential() {
        final int keyCount = 64, steps = 30000, seeds = 12;
        Key[] universe = new Key[keyCount];
        for (int i = 0; i < keyCount; i++) universe[i] = new Key(i, i % 5 == 0 ? 0 : 0x15);
        for (int seed = 0; seed < seeds; seed++) {
            Random rng = new Random(seed * 104729L + 12345);
            HashMap<Key, Integer> reference = new HashMap<>();
            M3Flat3Map<Key, Integer> actual = new M3Flat3Map<>();
            for (int step = 0; step < steps; step++) {
                int choice = rng.nextInt(11);
                Key key = rng.nextInt(20) == 0 ? null : universe[rng.nextInt(keyCount)];
                Integer value = rng.nextInt(12) == 0 ? null : rng.nextInt(25) - 12;
                switch (choice) {
                    case 0, 1, 2, 3 -> check(Objects.equals(actual.put(key, value),
                            reference.put(key, value)), "differential put");
                    case 4, 5 -> check(Objects.equals(actual.remove(key), reference.remove(key)),
                            "differential remove");
                    case 6 -> check(Objects.equals(actual.get(key), reference.get(key)), "differential get");
                    case 7 -> check(actual.containsKey(key) == reference.containsKey(key), "differential contains");
                    case 8 -> check(actual.containsValue(value) == reference.containsValue(value),
                            "differential contains value");
                    case 9 -> { actual.clear(); reference.clear(); }
                    case 10 -> check(actual.isEmpty() == reference.isEmpty(), "differential empty");
                    default -> throw new AssertionError("unreachable");
                }
                check(actual.size() == reference.size(), "differential size");
                if (step % 41 == 0) compare(actual, reference, universe);
            }
            compare(actual, reference, universe);
        }
    }

    public static void main(String[] args) {
        String before = "M3Flat3Map|FLAT3_MAP|OBJECT|OCCUPIED,REGIME_FLAT,REGIME_HASH|Object[] keyLane;Object[] valueLane";
        check(M3Flat3Map.scaffoldDescriptor().equals(before), "metadata descriptor unchanged");
        firstThreeAndTransition();
        structuralEdgeCases();
        allocationFailureAtomicity();
        packedWordBoundaries();
        randomDifferential();
        System.out.println("M3_FLAT3MAP_CONTRACT_PASS assertions=" + assertions);
    }
}
