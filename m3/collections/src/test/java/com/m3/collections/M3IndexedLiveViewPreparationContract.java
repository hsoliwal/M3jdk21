// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Java 21 differential contract for the original M3 indexed dynamic-array
 * owner. No SWT/EMF substitute, no native callback, no second element store.
 */
public final class M3IndexedLiveViewPreparationContract {
    private M3IndexedLiveViewPreparationContract() { }
    private static long checks;

    public static void main(String[] args) {
        ordinaryViewPreservesParent();
        callbackInvalidatesDuringFilter();
        callbackInvalidatesDuringComparator();
        callbackRecursesDuringFilter();
        callbackRecursesDuringComparator();
        comparatorFailureKeepsIndexUnpublished();
        parentWriteDuringFilterIsRejected();
        seededJdkListDifferential();
        System.out.println("M3_INDEXED_LIVE_PREPARATION_PASS checks=" + checks);
    }

    private static void ordinaryViewPreservesParent() {
        M3ArrayList<Integer> owner = new M3ArrayList<>(Arrays.asList(3, null, 2, 2));
        var view = M3IndexedLiveView.filteredSorted(owner,
                v -> v == null || v >= 2, Comparator.nullsFirst(Integer::compareTo));
        same(view, Arrays.asList(null, 2, 2, 3), "filtered stable sorted output");
        check(Arrays.equals(view.physicalIndices(), new int[] {1, 2, 3, 0}),
                "physical slot order, duplicate stability and nulls");
        same(owner, Arrays.asList(3, null, 2, 2), "parent values and order not changed");
        int[] detached = view.physicalIndices();
        detached[0] = -99;
        check(view.physicalIndex(0) == 1, "primitive offset result detached");
        var iterator = view.iterator();
        check(iterator.hasNext(), "iterator opens on admitted stable positions");
        view.invalidate();
        expect(ConcurrentModificationException.class, iterator::hasNext,
                "invalidated iterator does not reuse stale physical order");
        same(view, Arrays.asList(null, 2, 2, 3), "explicit refresh");
        expect(UnsupportedOperationException.class, view::clear, "read only clear");
        expect(UnsupportedOperationException.class, () -> view.addAll(List.of()),
                "read only no-op mutation");
        expect(UnsupportedOperationException.class, () -> view.reversed().clear(),
                "reversed wrapper remains read only");
    }

    private static void callbackInvalidatesDuringFilter() {
        M3ArrayList<Integer> owner = new M3ArrayList<>(List.of(7, 3, 5));
        AtomicBoolean once = new AtomicBoolean(true);
        AtomicReference<M3IndexedLiveView<Integer>> reference = new AtomicReference<>();
        var view = M3IndexedLiveView.filteredSorted(owner, value -> {
            if (once.getAndSet(false)) reference.get().invalidate();
            return value >= 3;
        }, Integer::compare);
        reference.set(view);
        expect(ConcurrentModificationException.class, view::size,
                "predicate invalidation vetoes index publication");
        same(view, List.of(3, 5, 7), "failed preparation can be retried");
        check(Arrays.equals(view.physicalIndices(), new int[] {1, 2, 0}),
                "stable positions after retry");
    }

    private static void callbackInvalidatesDuringComparator() {
        M3ArrayList<Integer> owner = new M3ArrayList<>(List.of(4, 2));
        AtomicBoolean once = new AtomicBoolean(true);
        AtomicReference<M3IndexedLiveView<Integer>> reference = new AtomicReference<>();
        Comparator<Integer> comparator = (a, b) -> {
            if (once.getAndSet(false)) reference.get().invalidate();
            return Integer.compare(a, b);
        };
        var view = M3IndexedLiveView.sorted(owner, comparator);
        reference.set(view);
        expect(ConcurrentModificationException.class, view::size,
                "last comparator callback invalidation vetoes publication");
        same(view, List.of(2, 4), "comparator retry uses original payload");
        check(Arrays.equals(view.physicalIndices(), new int[] {1, 0}),
                "stable indirect comparator order");
    }

    private static void callbackRecursesDuringFilter() {
        M3ArrayList<Integer> owner = new M3ArrayList<>(List.of(3, 2, 4));
        AtomicBoolean once = new AtomicBoolean(true);
        AtomicReference<M3IndexedLiveView<Integer>> reference = new AtomicReference<>();
        var view = M3IndexedLiveView.filtered(owner, v -> {
            if (once.getAndSet(false)) reference.get().size();
            return (v & 1) == 0;
        });
        reference.set(view);
        expect(ConcurrentModificationException.class, view::size,
                "recursive predicate request is bounded and refused");
        same(view, List.of(2, 4), "recursive failure does not poison next rebuild");
    }

    private static void callbackRecursesDuringComparator() {
        M3ArrayList<Integer> owner = new M3ArrayList<>(List.of(3, 2, 1));
        AtomicBoolean once = new AtomicBoolean(true);
        AtomicReference<M3IndexedLiveView<Integer>> reference = new AtomicReference<>();
        Comparator<Integer> comparison = (a, b) -> {
            if (once.getAndSet(false)) reference.get().physicalIndex(0);
            return Integer.compare(a, b);
        };
        var view = M3IndexedLiveView.sorted(owner, comparison);
        reference.set(view);
        expect(ConcurrentModificationException.class, view::size,
                "recursive comparator request is bounded and refused");
        same(view, List.of(1, 2, 3), "comparator retry and full payload preserved");
    }

    private static void comparatorFailureKeepsIndexUnpublished() {
        M3ArrayList<Integer> owner = new M3ArrayList<>(List.of(9, 3, 6));
        AtomicBoolean once = new AtomicBoolean(true);
        var view = M3IndexedLiveView.sorted(owner, (a, b) -> {
            if (once.getAndSet(false)) throw new IllegalArgumentException("comparator failure");
            return Integer.compare(a, b);
        });
        expect(IllegalArgumentException.class, view::size,
                "original comparator exception preserved");
        same(view, List.of(3, 6, 9), "no partial sorted index retained after exception");
    }

    private static void parentWriteDuringFilterIsRejected() {
        M3ArrayList<Integer> owner = new M3ArrayList<>(List.of(1, 2, 3));
        AtomicBoolean once = new AtomicBoolean(true);
        var view = M3IndexedLiveView.filtered(owner, value -> {
            if (once.getAndSet(false)) owner.set(0, 8);
            return (value & 1) == 0;
        });
        expect(ConcurrentModificationException.class, view::size,
                "parent element epoch changed within predicate");
        same(view, List.of(8, 2), "rebuild observes committed original owner mutation");
    }

    private static void seededJdkListDifferential() {
        Random random = new Random(0x51A11DEDL);
        M3ArrayList<Integer> owner = new M3ArrayList<>();
        ArrayList<Integer> oracle = new ArrayList<>();
        Comparator<Integer> order = Comparator.nullsFirst(Integer::compareTo);
        var view = M3IndexedLiveView.filteredSorted(owner,
                value -> value == null || (value & 1) == 0, order);
        for (int step = 0; step < 1_500; step++) {
            int n = oracle.size();
            int action = n > 70 ? 2 : random.nextInt(4);
            Integer value = random.nextInt(9) == 0 ? null : random.nextInt(35) - 17;
            switch (action) {
                case 0 -> {
                    int at = random.nextInt(n + 1);
                    owner.add(at, value);
                    oracle.add(at, value);
                }
                case 1 -> {
                    if (n > 0) {
                        int at = random.nextInt(n);
                        same(owner.set(at, value), oracle.set(at, value), "set original owner");
                    }
                }
                case 2 -> {
                    if (n > 0) {
                        int at = random.nextInt(n);
                        same(owner.remove(at), oracle.remove(at), "remove original owner");
                    }
                }
                default -> {
                    owner.add(value);
                    oracle.add(value);
                }
            }
            same(owner, oracle, "exact original value order and nulls");
            ArrayList<Integer> expected = new ArrayList<>();
            for (Integer candidate : oracle) {
                if (candidate == null || (candidate & 1) == 0) expected.add(candidate);
            }
            expected.sort(order);
            same(view, expected, "filtered/sorted JDK list differential");
            int[] physical = view.physicalIndices();
            check(physical.length == expected.size(), "logical/physical cardinality");
            for (int i = 0; i < physical.length; i++) {
                same(owner.get(physical[i]), expected.get(i),
                        "primitive parent offset identifies exact reference/value");
            }
        }
    }

    private static void same(Object actual, Object expected, String label) {
        check(Objects.equals(actual, expected), label + ": expected=" + expected
                + ", actual=" + actual);
    }

    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        checks++;
    }

    private static void expect(Class<? extends Throwable> kind, Runnable run, String label) {
        try {
            run.run();
        } catch (Throwable actual) {
            if (!kind.isInstance(actual)) throw new AssertionError(label, actual);
            checks++;
            return;
        }
        throw new AssertionError(label + ": expected " + kind.getName());
    }
}
