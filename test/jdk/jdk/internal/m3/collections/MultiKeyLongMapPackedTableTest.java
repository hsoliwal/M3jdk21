/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Composed two-long-key map shares canonical indexed lanes and masked group removal
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm --add-opens=jdk.m3.collections/jdk.internal.m3.collections=ALL-UNNAMED MultiKeyLongMapPackedTableTest
 */
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import jdk.internal.m3.collections.MultiKeyLongMap;
import jdk.internal.m3.collections.PackedLongLongTable;

public final class MultiKeyLongMapPackedTableTest {
    private record Pair(long first, long second) { }

    private static long assertions;

    public static void main(String[] args) throws Exception {
        emptyAndExtremeKeys();
        randomizedDifferential();
        repeatedGroupCompaction();
        System.out.println("MultiKeyLongMapPackedTableTest PASS " + assertions + " assertions");
    }

    private static void emptyAndExtremeKeys() throws Exception {
        MultiKeyLongMap map = new MultiKeyLongMap();
        check(map.size() == 0 && map.isEmpty(), "initial size");
        check(map.getOrDefault(0, 0, 123) == 123, "missing default");
        check(!map.containsKey(0, 0), "missing zero");
        check(map.remove(0, 0) == 0, "missing remove");
        check(!map.removeAll(0), "missing group");
        check(MultiKeyLongMap.scaffoldDescriptor().equals(
                "M3MultiKeyLongMap|MULTIKEY_LONG_MAP|LONG|OCCUPIED,DELETED|long[] key1Lane;long[] key2Lane;long[] valueLane"),
                "historic descriptor");
        check(map.put(0, 0, 0) == 0, "put zero key/value");
        check(map.containsKey(0, 0), "zero mapping distinguishable");
        check(map.put(0, 0, Long.MIN_VALUE) == 0, "replace zero");
        check(map.getOrDefault(0, 0, -1) == Long.MIN_VALUE, "extreme long value");
        check(map.put(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE) == 0, "extreme tuple");
        check(map.put(Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE) == 0, "reverse extreme tuple");
        check(map.put(Long.MIN_VALUE, Long.MIN_VALUE, 17) == 0, "same extreme components");
        check(map.getOrDefault(Long.MIN_VALUE, Long.MAX_VALUE, -1) == Long.MAX_VALUE,
                "extreme component order");
        check(map.getOrDefault(Long.MAX_VALUE, Long.MIN_VALUE, -1) == Long.MIN_VALUE,
                "reverse component order");
        check(map.size() == 4, "unique pairs");
        check(map.removeAll(Long.MIN_VALUE), "group remove");
        check(map.size() == 2, "group cardinality");
        check(!map.containsKey(Long.MIN_VALUE, Long.MIN_VALUE), "group removed first");
        check(!map.containsKey(Long.MIN_VALUE, Long.MAX_VALUE), "group removed second");
        check(map.containsKey(Long.MAX_VALUE, Long.MIN_VALUE), "other group retained");
        map.clear();
        check(map.isEmpty() && map.size() == 0, "cleared");
        verifyStorage(map, Map.of());
    }

    private static void randomizedDifferential() throws Exception {
        for (int seed = 0; seed < 64; seed++) {
            Random random = new Random(0x4D334D554C54494BL ^ (seed * 0x9E3779B97F4A7C15L));
            MultiKeyLongMap actual = new MultiKeyLongMap();
            Map<Pair, Long> expected = new HashMap<>();
            for (int step = 0; step < 10_000; step++) {
                long a = randomLongKey(random);
                long b = randomLongKey(random);
                Pair key = new Pair(a, b);
                long value = randomLongKey(random);
                int op = random.nextInt(100);
                if (op < 38) {
                    check(actual.put(a, b, value) == expected.getOrDefault(key, 0L),
                            "put old value");
                    expected.put(key, value);
                } else if (op < 55) {
                    check(actual.remove(a, b) == expected.getOrDefault(key, 0L),
                            "remove old value");
                    expected.remove(key);
                } else if (op < 66) {
                    boolean removed = expected.entrySet().removeIf(entry -> entry.getKey().first() == a);
                    check(actual.removeAll(a) == removed, "removeAll group");
                } else if (op < 80) {
                    check(actual.containsKey(a, b) == expected.containsKey(key), "presence");
                } else if (op < 93) {
                    check(actual.getOrDefault(a, b, value) == expected.getOrDefault(key, value),
                            "get default");
                } else if (op < 95) {
                    expected.clear();
                    actual.clear();
                } else {
                    check(actual.isEmpty() == expected.isEmpty(), "isEmpty");
                }
                check(actual.size() == expected.size(), "cardinality");
                if ((step & 127) == 0) verifyStorage(actual, expected);
            }
            verifyStorage(actual, expected);
        }
    }

    private static void repeatedGroupCompaction() throws Exception {
        MultiKeyLongMap actual = new MultiKeyLongMap();
        Map<Pair, Long> oracle = new HashMap<>();
        for (int group = 0; group < 32; group++) {
            for (int value = 0; value < 200; value++) {
                long key1 = group % 7 - 3;
                long key2 = group * 200L + value;
                check(actual.put(key1, key2, value) == 0L, "bulk insert");
                oracle.put(new Pair(key1, key2), (long) value);
            }
            verifyStorage(actual, oracle);
        }
        for (long first = -3; first <= 3; first++) {
            check(actual.removeAll(first), "bulk group removal");
            long group = first;
            oracle.entrySet().removeIf(e -> e.getKey().first() == group);
            verifyStorage(actual, oracle);
        }
        check(actual.size() == 0 && actual.isEmpty(), "all groups emptied");
        check(!actual.removeAll(0), "repeated empty removal");
        // Refill after tombstone-free backward-shift repair and a clear.
        for (int i = 0; i < 10_000; i++) {
            actual.put(i % 17, -i, i);
            oracle.put(new Pair(i % 17, -i), (long) i);
        }
        verifyStorage(actual, oracle);
        actual.clear();
        oracle.clear();
        verifyStorage(actual, oracle);
    }

    private static long randomLongKey(Random random) {
        return switch (random.nextInt(9)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            case 3 -> -1L;
            case 4 -> random.nextInt(7) - 3;
            case 5 -> random.nextInt(100) - 50;
            default -> random.nextLong();
        };
    }

    private static void verifyStorage(MultiKeyLongMap map, Map<Pair, Long> expected)
            throws Exception {
        PackedLongLongTable table = (PackedLongLongTable) field(map, "entries");
        check(table.size() == map.size(), "one authoritative primitive owner");
        long[] a = table.rowKeysToArray();
        long[] b = table.columnKeysToArray();
        long[] values = table.valuesToArray();
        check(a.length == b.length && b.length == values.length, "aligned exact snapshots");
        check(a.length == expected.size(), "physical live count");
        Set<Pair> seen = new HashSet<>();
        for (int i = 0; i < a.length; i++) {
            Pair key = new Pair(a[i], b[i]);
            check(seen.add(key), "no duplicate primitive pair");
            check(expected.containsKey(key), "only expected pair survives");
            check(Objects.equals(expected.get(key), values[i]), "value correspondence");
            check(map.containsKey(a[i], b[i]), "index finds owned pair");
        }
        check(seen.equals(expected.keySet()), "exact live key set");
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static void check(boolean okay, String label) {
        assertions++;
        if (!okay) throw new AssertionError(label + " at assertion " + assertions);
    }
}
