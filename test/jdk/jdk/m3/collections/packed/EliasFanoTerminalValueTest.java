/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Verify terminal reads, snapshots and compact payloads for Elias-Fano lists
 * @modules jdk.m3.collections/jdk.m3.collections.packed
 * @run main EliasFanoTerminalValueTest
 */

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.SplittableRandom;
import jdk.m3.collections.packed.EliasFanoLongList;

public final class EliasFanoTerminalValueTest {
    private static final long[] ALPHABET = {0, 1, 63, 64, Long.MAX_VALUE - 1, Long.MAX_VALUE};
    private static long checks;

    public static void main(String[] args) {
        for (int n = 0; n <= 7; n++) enumerate(new long[n], 0, 0);
        var random = new SplittableRandom(0xE11A5FA0L);
        for (int n : new int[] {63, 64, 65, 127, 128, 129, 511, 512, 513, 8192}) {
            var dense = new long[n];
            var duplicates = new long[n];
            var gap = new long[n];
            for (int i = 0; i < n; i++) {
                dense[i] = 7L * i;
                duplicates[i] = random.nextLong(100);
            }
            Arrays.sort(duplicates);
            gap[n - 1] = Long.MAX_VALUE;
            verify(dense);
            verify(duplicates);
            verify(gap);
        }

        long[] input = new long[262_144];
        input[input.length - 1] = Long.MAX_VALUE;
        var sparse = new EliasFanoLongList(input);
        input[input.length - 1] = 0;
        for (int i = 0; i < 256; i++) {
            equal(Long.MAX_VALUE, sparse.get(sparse.size() - 1));
            equal(Long.MAX_VALUE, sparse.last());
        }
        equal(0, sparse.get(sparse.size() - 2));
        equal(sparse.size() - 1, sparse.lowerBound(Long.MAX_VALUE));
        check(sparse.contains(Long.MAX_VALUE));

        var singleton = new EliasFanoLongList(new long[] {0});
        check(singleton.payloadBytes() <= 12);
        equal(0, singleton.get(0));
        var empty = new EliasFanoLongList(new long[0]);
        equal(0, empty.payloadBytes());
        expect(NoSuchElementException.class, empty::last);
        expect(IndexOutOfBoundsException.class, () -> empty.get(0));
        expect(IndexOutOfBoundsException.class, () -> singleton.get(-1));
        expect(IndexOutOfBoundsException.class, () -> singleton.get(1));
        expect(NullPointerException.class, () -> new EliasFanoLongList(null));
        expect(IllegalArgumentException.class, () -> new EliasFanoLongList(new long[] {-1}));
        expect(IllegalArgumentException.class, () -> new EliasFanoLongList(new long[] {2, 1}));
        System.out.println("PASS EliasFanoTerminalValueTest checks=" + checks);
    }

    private static void enumerate(long[] values, int index, int minimum) {
        if (index == values.length) {
            verify(values);
            return;
        }
        for (int key = minimum; key < ALPHABET.length; key++) {
            values[index] = ALPHABET[key];
            enumerate(values, index + 1, key);
        }
    }

    private static void verify(long[] values) {
        var list = new EliasFanoLongList(values);
        equal(values.length, list.size());
        check(list.isEmpty() == (values.length == 0));
        for (int i = 0; i < values.length; i++) equal(values[i], list.get(i));
        check(Arrays.equals(values, list.toLongArray()));
        if (values.length > 0) {
            equal(values[0], list.first());
            equal(values[values.length - 1], list.last());
        }
        for (long target : ALPHABET) {
            int expected = 0;
            while (expected < values.length && values[expected] < target) expected++;
            equal(expected, list.lowerBound(target));
            check(list.contains(target) == (expected < values.length && values[expected] == target));
        }
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("Elias-Fano check " + checks);
    }

    private static void equal(long expected, long actual) {
        checks++;
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException failure) {
            check(type.isInstance(failure));
            return;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
