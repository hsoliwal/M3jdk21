/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Stable primitive CSR multimap grouping, snapshots and immutable contracts.
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main FrozenLongMultimapConvergenceTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import jdk.internal.m3.collections.FrozenLongMultimap;

public class FrozenLongMultimapConvergenceTest {
    private record Pair(long key, long value) { }
    private static long checks;

    public static void main(String[] args) {
        emptyAndInvalid();
        verify(new long[] {0, Long.MIN_VALUE, 0, Long.MAX_VALUE, Long.MIN_VALUE, -1},
                new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE, -1, 0, 0});
        Random random = new Random(0x20261115);
        for (int round = 0; round < 1000; round++) {
            int size = round < 100 ? round : random.nextInt(2000);
            long[] keys = new long[size];
            long[] values = new long[size];
            for (int i = 0; i < size; i++) {
                keys[i] = round % 2 == 0 ? random.nextInt(17) - 8 : random.nextLong();
                values[i] = random.nextLong();
            }
            verify(keys, values);
        }
        System.out.println("FROZEN_LONG_MULTIMAP_PASS checks=" + checks + " random_cases=1000");
    }

    private static void emptyAndInvalid() {
        FrozenLongMultimap empty = new FrozenLongMultimap();
        check(empty.size() == 0 && empty.keyCount() == 0, "valid empty constructor");
        check(empty.valueOffsetAt(0) == 0, "empty terminal boundary");
        check(!empty.containsKey(0) && !empty.containsEntry(0, 0), "empty contains");
        array(new long[0], empty.get(0), "empty get");
        expect(UnsupportedOperationException.class, () -> empty.put(0, 0));
        expect(UnsupportedOperationException.class, () -> empty.removeAll(0));
        expect(NullPointerException.class, () -> empty.forEach(null));
        expect(NullPointerException.class, () -> FrozenLongMultimap.copyOf(null, new long[0]));
        expect(NullPointerException.class, () -> FrozenLongMultimap.copyOf(new long[0], null));
        expect(IllegalArgumentException.class,
                () -> FrozenLongMultimap.copyOf(new long[1], new long[0]));
        expect(IndexOutOfBoundsException.class, () -> empty.keyAt(0));
        expect(IndexOutOfBoundsException.class, () -> empty.valueAt(0));
        expect(IndexOutOfBoundsException.class, () -> empty.valueOffsetAt(1));
        expect(IndexOutOfBoundsException.class, () -> empty.valueOffsetAt(-1));
        long[] shared = {1, 2, 1};
        FrozenLongMultimap alias = FrozenLongMultimap.copyOf(shared, shared);
        array(new long[] {1, 1}, alias.get(1), "same source array for both lanes");
    }

    private static void verify(long[] inputKeys, long[] inputValues) {
        long[] keys = inputKeys.clone();
        long[] values = inputValues.clone();
        LinkedHashMap<Long, List<Long>> model = new LinkedHashMap<>();
        for (int i = 0; i < keys.length; i++) {
            model.computeIfAbsent(keys[i], unused -> new ArrayList<>()).add(values[i]);
        }
        FrozenLongMultimap map = FrozenLongMultimap.copyOf(keys, values);
        array(inputKeys, keys, "construction does not mutate source keys");
        array(inputValues, values, "construction does not mutate source values");
        Arrays.fill(keys, 99);
        Arrays.fill(values, 88);
        check(map.size() == inputKeys.length, "pair cardinality");
        check(map.keyCount() == model.size(), "distinct cardinality");
        List<Pair> expected = new ArrayList<>();
        int keyOrdinal = 0;
        int valueOrdinal = 0;
        for (var entry : model.entrySet()) {
            long key = entry.getKey();
            check(map.keyAt(keyOrdinal) == key, "stable first key order");
            check(map.valueOffsetAt(keyOrdinal) == valueOrdinal, "prefix boundary");
            check(map.containsKey(key), "present key");
            check(map.valueCount(key) == entry.getValue().size(), "per-key cardinality");
            long[] row = entry.getValue().stream().mapToLong(Long::longValue).toArray();
            array(row, map.get(key), "stable value group");
            long[] copy = map.get(key);
            if (copy.length > 0) copy[0] ^= -1L;
            array(row, map.get(key), "detached returned run");
            for (long value : row) {
                check(map.valueAt(valueOrdinal++) == value, "packed lane order");
                check(map.containsEntry(key, value), "present duplicate pair");
                expected.add(new Pair(key, value));
            }
            keyOrdinal++;
        }
        check(map.valueOffsetAt(keyOrdinal) == map.size(), "terminal prefix boundary");
        List<Pair> actual = new ArrayList<>();
        map.forEach((key, value) -> actual.add(new Pair(key, value)));
        check(actual.equals(expected), "key-grouped encounter order");
        long absent = 0;
        while (model.containsKey(absent)) absent++;
        check(!map.containsKey(absent), "absent key");
        check(!map.containsEntry(absent, 0), "absent pair");
        check(map.valueCount(absent) == 0, "absent count");
        array(new long[0], map.get(absent), "absent run");
        expect(UnsupportedOperationException.class, () -> map.put(0, 1));
        expect(UnsupportedOperationException.class, () -> map.removeAll(0));
        expect(IndexOutOfBoundsException.class, () -> map.keyAt(map.keyCount()));
        expect(IndexOutOfBoundsException.class, () -> map.valueAt(map.size()));
        expect(IndexOutOfBoundsException.class, () -> map.valueOffsetAt(map.keyCount() + 1));
    }

    private static void array(long[] expected, long[] actual, String message) {
        check(Arrays.equals(expected, actual), message);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type + ", got " + failure);
            return;
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
