/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Adaptive dense-long frequency counting preserves the frozen bag contract
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm FrozenLongBagDenseCountingTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import jdk.internal.m3.collections.FrozenLongBag;

/** Independent sorted-count oracle covering the histogram and original sort paths. */
public final class FrozenLongBagDenseCountingTest {
    private static long checks;

    private static void check(boolean actual, String where) {
        checks++;
        if (!actual) throw new AssertionError(where);
    }

    private static void rejects(Class<? extends Throwable> expected, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("wrong exception " + failure, failure);
        }
        throw new AssertionError("missing " + expected.getName());
    }

    private static void compare(long[] data) {
        long[] original = data.clone();
        TreeMap<Long, Integer> expected = new TreeMap<>();
        for (long value : data) expected.merge(value, 1, Integer::sum);
        FrozenLongBag owner = FrozenLongBag.copyOf(data);
        check(Arrays.equals(data, original), "input changed");
        check(owner.size() == data.length, "size");
        check(owner.distinctSize() == expected.size(), "distinct size");

        long[] expectedValues = new long[expected.size()];
        int ordinal = 0;
        for (Map.Entry<Long, Integer> entry : expected.entrySet()) {
            expectedValues[ordinal] = entry.getKey();
            check(owner.elementAt(ordinal) == entry.getKey(), "element ordinal");
            check(owner.countAt(ordinal) == entry.getValue(), "count ordinal");
            check(owner.occurrencesOf(entry.getKey()) == entry.getValue(), "occurrences");
            check(owner.contains(entry.getKey()), "membership");
            ordinal++;
        }
        check(Arrays.equals(expectedValues, owner.distinctToArray()), "signed sorted lanes");
        ArrayList<Long> visited = new ArrayList<>();
        owner.forEachDistinct(visited::add);
        check(visited.equals(expected.keySet().stream().toList()), "consumer encounter order");
        for (long absent : new long[] {Long.MIN_VALUE, Long.MAX_VALUE, -1, 0, 1, 128, -128}) {
            check(owner.occurrencesOf(absent) == expected.getOrDefault(absent, 0), "probe count");
        }
        long[] detached = owner.distinctToArray();
        if (detached.length != 0) {
            detached[0] ^= 0x55aa55aaL;
            check(Arrays.equals(expectedValues, owner.distinctToArray()), "detached export");
            data[0] ^= 0x55aa55aaL;
            check(Arrays.equals(expectedValues, owner.distinctToArray()), "detached ingress");
        }
        rejects(IndexOutOfBoundsException.class, () -> owner.elementAt(-1));
        rejects(IndexOutOfBoundsException.class, () -> owner.countAt(owner.distinctSize()));
        rejects(UnsupportedOperationException.class, () -> owner.addOccurrences(7, 1));
        rejects(UnsupportedOperationException.class, () -> owner.removeOccurrences(7, 1));
    }

    private static long[] fixture(int length, int kind, Random random) {
        long[] values = new long[length];
        for (int index = 0; index < length; index++) {
            values[index] = switch (kind) {
                case 0 -> 0;
                case 1 -> random.nextInt(41) - 20;
                case 2 -> Long.MIN_VALUE + random.nextInt(50);
                case 3 -> Long.MAX_VALUE - random.nextInt(50);
                case 4 -> random.nextLong();
                case 5 -> (index & 1) == 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
                case 6 -> random.nextInt(Math.max(1, length / 2));
                default -> index % 7 == 0 ? -1 : index;
            };
        }
        return values;
    }

    public static void main(String[] args) {
        rejects(NullPointerException.class, () -> FrozenLongBag.copyOf(null));
        rejects(NullPointerException.class, () -> new FrozenLongBag().forEachDistinct(null));
        for (int length : new int[] {0, 1, 2, 7, 255, 511, 512, 513,
                1023, 2048, 4096, 8192, 65535, 65536}) {
            Random random = new Random(0xF20E20261116L ^ length);
            for (int kind = 0; kind < 8; kind++) {
                compare(fixture(length, kind, random));
            }
        }
        Random random = new Random(0x5F2050111618L);
        for (int run = 0; run < 450; run++) {
            compare(fixture(random.nextInt(2800), run % 8, random));
        }
        System.out.println("PASS FrozenLongBagDenseCountingTest checks=" + checks);
    }
}
