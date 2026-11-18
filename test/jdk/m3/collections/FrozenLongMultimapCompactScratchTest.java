/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Compact scratch lanes preserve the frozen long multimap CSR contract
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm FrozenLongMultimapCompactScratchTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import jdk.internal.m3.collections.FrozenLongMultimap;

/** Independent insertion-group and value-order oracle for scratch-lane ownership. */
public final class FrozenLongMultimapCompactScratchTest {
    private static long checks;

    private static void check(boolean accepted, String where) {
        checks++;
        if (!accepted) throw new AssertionError(where);
    }

    private static void rejects(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("wrong exception " + failure, failure);
        }
        throw new AssertionError("missing expected " + kind.getName());
    }

    private static void compare(long[] keys, long[] values) {
        long[] initialKeys = keys.clone();
        long[] initialValues = values.clone();
        LinkedHashMap<Long, ArrayList<Long>> expected = new LinkedHashMap<>();
        for (int i = 0; i < keys.length; i++) {
            expected.computeIfAbsent(keys[i], unused -> new ArrayList<>()).add(values[i]);
        }
        FrozenLongMultimap owner = FrozenLongMultimap.copyOf(keys, values);
        check(Arrays.equals(keys, initialKeys), "input keys changed");
        check(Arrays.equals(values, initialValues), "input values changed");
        check(owner.size() == keys.length, "total count");
        check(owner.keyCount() == expected.size(), "key count");

        int ordinal = 0;
        int offset = 0;
        ArrayList<Long> projectedKeys = new ArrayList<>();
        ArrayList<Long> projectedValues = new ArrayList<>();
        for (Map.Entry<Long, ArrayList<Long>> entry : expected.entrySet()) {
            long key = entry.getKey();
            ArrayList<Long> group = entry.getValue();
            check(owner.keyAt(ordinal) == key, "key encounter order");
            check(owner.containsKey(key), "key membership");
            check(owner.valueCount(key) == group.size(), "group count");
            check(owner.valueOffsetAt(ordinal) == offset, "prefix boundary");
            long[] returned = owner.get(key);
            check(returned.length == group.size(), "get length");
            for (int i = 0; i < group.size(); i++) {
                long value = group.get(i);
                check(returned[i] == value, "get value order");
                check(owner.valueAt(offset++) == value, "packed value lane");
                projectedKeys.add(key);
                projectedValues.add(value);
            }
            if (!group.isEmpty()) {
                check(owner.containsEntry(key, group.get(0)), "first value membership");
                check(owner.containsEntry(key, group.get(group.size() - 1)), "last value membership");
                returned[0] ^= 0xA4C7B3D8L;
                check(owner.get(key)[0] == group.get(0), "detached output");
            }
            ordinal++;
        }
        check(offset == owner.size(), "packed size");
        check(owner.valueOffsetAt(owner.keyCount()) == owner.size(), "terminal prefix");
        ArrayList<Long> traversedKeys = new ArrayList<>();
        ArrayList<Long> traversedValues = new ArrayList<>();
        owner.forEach((key, value) -> {
            traversedKeys.add(key);
            traversedValues.add(value);
        });
        check(projectedKeys.equals(traversedKeys), "forEach keys");
        check(projectedValues.equals(traversedValues), "forEach value order");
        for (long missingKey : new long[] {Long.MIN_VALUE, Long.MAX_VALUE, -1, 0, 1}) {
            check(owner.containsKey(missingKey) == expected.containsKey(missingKey), "key probe");
            if (!expected.containsKey(missingKey)) {
                check(owner.valueCount(missingKey) == 0, "empty group count");
                check(owner.get(missingKey).length == 0, "empty group copy");
                check(!owner.containsEntry(missingKey, 7), "missing pair");
            }
        }
        if (keys.length > 0) {
            keys[0] ^= 0x55AA55AAL;
            values[0] ^= 0x55AA55AAL;
            check(owner.keyAt(0) == expected.keySet().iterator().next(), "detached input");
        }
        rejects(IndexOutOfBoundsException.class, () -> owner.keyAt(-1));
        rejects(IndexOutOfBoundsException.class, () -> owner.valueOffsetAt(owner.keyCount() + 1));
        rejects(IndexOutOfBoundsException.class, () -> owner.valueAt(owner.size()));
        rejects(UnsupportedOperationException.class, () -> owner.put(8, 9));
        rejects(UnsupportedOperationException.class, () -> owner.removeAll(8));
    }

    private static long[] keys(int length, int kind, Random random) {
        long[] keys = new long[length];
        for (int index = 0; index < length; index++) {
            keys[index] = switch (kind) {
                case 0 -> 0L;
                case 1 -> index % 8;
                case 2 -> index % 32;
                case 3 -> index % 33;
                case 4 -> index;
                case 5 -> index % 3 == 0 ? Long.MIN_VALUE :
                        index % 3 == 1 ? Long.MAX_VALUE : 0L;
                case 6 -> random.nextLong();
                default -> random.nextInt(64) - 32;
            };
        }
        return keys;
    }

    public static void main(String[] arguments) {
        rejects(NullPointerException.class, () -> FrozenLongMultimap.copyOf(null, new long[0]));
        rejects(NullPointerException.class, () -> FrozenLongMultimap.copyOf(new long[0], null));
        rejects(IllegalArgumentException.class,
                () -> FrozenLongMultimap.copyOf(new long[2], new long[1]));
        check(new FrozenLongMultimap().valueOffsetAt(0) == 0, "empty initial prefix");
        for (int n : new int[] {0, 1, 2, 31, 32, 33, 511, 512, 513,
                4096, 65536}) {
            Random random = new Random(0xC5A20261116L ^ n);
            for (int kind = 0; kind < 8; kind++) {
                long[] keys = keys(n, kind, random);
                long[] values = new long[n];
                for (int i = 0; i < n; i++) values[i] = i % 17 == 0
                        ? Long.MIN_VALUE : i % 19 == 0 ? Long.MAX_VALUE : random.nextInt(64);
                compare(keys, values);
            }
        }
        Random random = new Random(0xF20261116L);
        for (int i = 0; i < 320; i++) {
            int n = random.nextInt(1600);
            long[] keys = keys(n, i % 8, random), values = new long[n];
            for (int j = 0; j < n; j++) values[j] = random.nextLong();
            compare(keys, values);
        }
        System.out.println("PASS FrozenLongMultimapCompactScratchTest checks=" + checks);
    }
}
