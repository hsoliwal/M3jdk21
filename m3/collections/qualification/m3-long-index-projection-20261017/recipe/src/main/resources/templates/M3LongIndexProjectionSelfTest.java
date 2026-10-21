/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Dependency-free Java 21 reference and metamorphic oracle for indexed projections. */
public final class M3LongIndexProjectionSelfTest {
    private static long checks;
    private M3LongIndexProjectionSelfTest() { }

    private static void check(boolean condition, String why) {
        checks++;
        if (!condition) throw new AssertionError(why);
    }

    private static void expect(Class<? extends Throwable> type, Runnable task) {
        checks++;
        try { task.run(); }
        catch (Throwable problem) {
            if (type.isInstance(problem)) return;
            throw new AssertionError("expected " + type + ", got " + problem, problem);
        }
        throw new AssertionError("expected " + type);
    }

    private static void verify(long[] source, int from, int count, boolean descending) {
        M3LongIndexProjection base = M3LongIndexProjection.snapshot(source);
        M3LongIndexProjection view = base.slice(from, count);
        long[] original = source.clone();
        Arrays.fill(source, 5L); // snapshot must not alias caller storage
        check(Arrays.equals(original, base.toArray()), "source isolation");
        List<Integer> oracle = new ArrayList<>();
        for (int i = from; i < from + count; i++) oracle.add(i);
        Comparator<Integer> comparison = Comparator.comparingLong(i -> original[i]);
        if (descending) comparison = comparison.reversed();
        oracle.sort(comparison); // stable JDK reference oracle, boxed only in test
        M3LongIndexProjection sorted = descending ? view.sortedDescending() : view.sortedAscending();
        check(sorted.size() == count, "sorted size");
        for (int i = 0; i < count; i++) {
            check(sorted.physicalIndex(i) == oracle.get(i), "stable indirect index");
            check(sorted.get(i) == original[oracle.get(i)], "sorted value");
        }
        check(Arrays.equals(view.toArray(), Arrays.copyOfRange(original, from, from + count)),
                "sort cannot change existing views");
        int[] escaped = sorted.physicalIndices();
        if (escaped.length > 0) escaped[0] = -1;
        check(sorted.size() == 0 || sorted.physicalIndex(0) >= 0, "index isolation");
        byte[] states = new byte[count];
        List<Integer> filteredOracle = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            states[i] = (byte) ((i % 4 == 0 || i % 4 == 3) ? 0x85 : 0x01);
            if ((states[i] & 0x84) == 0x84) filteredOracle.add(oracle.get(i));
        }
        M3LongIndexProjection filtered = sorted.filtered(states, 0x84,
                M3IndexMaskCompactor.Backend.JAVA);
        check(filtered.size() == filteredOracle.size(), "mask count");
        check(filtered.order() == sorted.order(), "mask retains sorted contract");
        for (int i = 0; i < filtered.size(); i++) {
            check(filtered.physicalIndex(i) == filteredOracle.get(i), "masked position");
        }
        check(filtered.slice(0, filtered.size()).size() == filtered.size(), "slice identity");
        check(filtered.slice(filtered.size(), 0).isEmpty(), "empty tail");
        expect(IndexOutOfBoundsException.class, () -> view.slice(-1, 0));
        expect(IndexOutOfBoundsException.class, () -> view.slice(count, 1));
        expect(IndexOutOfBoundsException.class, () -> view.get(count));
        expect(IllegalArgumentException.class, () -> view.filtered(new byte[count + 1], 0,
                M3IndexMaskCompactor.Backend.JAVA));
        expect(IllegalArgumentException.class, () -> view.filtered(new byte[count], 256,
                M3IndexMaskCompactor.Backend.JAVA));
        if (M3IndexMaskCompactor.nativeAvailable()) {
            M3LongIndexProjection nativeResult = sorted.filtered(states, 0x84,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            check(Arrays.equals(nativeResult.physicalIndices(), filtered.physicalIndices()),
                    "native/Java parity");
        } else {
            expect(UnsatisfiedLinkError.class, () -> sorted.filtered(states, 0x84,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
    }

    public static void main(String[] args) {
        expect(NullPointerException.class, () -> M3LongIndexProjection.snapshot(null));
        check(M3LongIndexProjection.snapshot(new long[0]).sortedAscending().isEmpty(), "empty");
        long[] extremes = {Long.MIN_VALUE, 1, 1, 0, Long.MAX_VALUE, -1, Long.MIN_VALUE};
        verify(extremes.clone(), 0, extremes.length, false);
        verify(extremes.clone(), 1, 4, true);
        Random random = new Random(0x13579bdf42L);
        for (int repeat = 0; repeat < 350; repeat++) {
            int length = random.nextInt(140);
            long[] values = new long[length];
            for (int i = 0; i < length; i++) values[i] = random.nextInt(19) - 9L;
            int from = random.nextInt(length + 1);
            int count = random.nextInt(length - from + 1);
            verify(values, from, count, (repeat & 1) != 0);
        }
        System.out.println("PASS M3LongIndexProjectionSelfTest checks=" + checks);
    }
}
