// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Differential contract for adaptive primitive row-ID selection in M3SortedView. */
public final class M3SortedViewAdaptiveSelectionTest {
    private static long checks;
    private static void check(boolean good) {
        checks++;
        if (!good) throw new AssertionError("check=" + checks);
    }

    private static void compare(int n, int step, int remainder) {
        long[] keys = new long[n];
        for (int i = 0; i < n; i++) keys[i] = (i * 37L % 191L) - 95L;
        AtomicInteger calls = new AtomicInteger();
        try (var view = M3SortedView.<Long>byIndex(n, i -> keys[i],
                () -> 7L, i -> { calls.incrementAndGet(); return i % step == remainder; },
                (a, b) -> Long.compare(keys[a], keys[b]), 48L * n, null)) {
            ArrayList<Integer> expected = new ArrayList<>();
            for (int i = 0; i < n; i++) if (i % step == remainder) expected.add(i);
            expected.sort(Comparator.comparingLong(i -> keys[i]));
            check(calls.get() == n && view.size() == expected.size());
            for (int i = 0; i < expected.size(); i++) {
                check(view.sourceIndex(i) == expected.get(i));
                check(view.get(i) == keys[expected.get(i)]);
            }
            check(calls.get() == n);
        }
    }

    public static void main(String[] args) {
        for (int n = 0; n < 360; n++) {
            compare(n, 1, 0);
            compare(n, 5, 0);
            compare(n, 17, 2);
            compare(n, 1024, 1023);
        }
        int million = 1_048_576;
        AtomicInteger seen = new AtomicInteger(), reads = new AtomicInteger();
        try (var empty = M3SortedView.<Long>byIndex(million,
                i -> { reads.incrementAndGet(); return (long) i; },
                () -> 1L, i -> { seen.incrementAndGet(); return false; },
                (a, b) -> { throw new AssertionError("empty comparator"); },
                48L * million, null)) {
            check(empty.size() == 0 && seen.get() == million && reads.get() == 0);
        }
        seen.set(0);
        try (var sparse = M3SortedView.<Long>byIndex(million,
                i -> (long) i, () -> 1L,
                i -> { seen.incrementAndGet(); return i == million - 1; },
                (a, b) -> { throw new AssertionError("single-item comparator"); },
                48L * million, null)) {
            check(sparse.size() == 1 && sparse.sourceIndex(0) == million - 1);
            check(sparse.get(0) == million - 1L && seen.get() == million);
        }
        seen.set(0);
        try {
            M3SortedView.<Long>byIndex(3000, i -> (long)i, () -> 1L,
                    i -> {
                        seen.incrementAndGet();
                        if (i == 777) throw new IllegalStateException("witness");
                        return i % 3 == 0;
                    }, Integer::compare, 48L * 3000, null);
            throw new AssertionError("missing predicate failure");
        } catch (IllegalStateException expected) {
            check("witness".equals(expected.getMessage()) && seen.get() == 778);
        }
        var revision = new java.util.concurrent.atomic.AtomicLong(1);
        List<Long> source = Arrays.asList(1L, 3L, 2L, 3L, 1L);
        try (var view = M3SortedView.by(source, Comparator.naturalOrder(), revision::get)) {
            check(view.equals(List.of(1L, 1L, 2L, 3L, 3L)));
            check(view.sourceIndex(0) == 0 && view.sourceIndex(1) == 4);
            revision.incrementAndGet();
            try {
                view.size();
                throw new AssertionError("revision must fail");
            } catch (ConcurrentModificationException expected) {
                check(true);
            }
        }
        System.out.println("M3SortedViewAdaptiveSelectionTest PASS checks=" + checks);
    }
}
