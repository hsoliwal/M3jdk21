// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/** Real JDK providers, nullable bounds and exact user-failure identity. */
final class M3MutableComparatorFailureTest {
    private static final Comparator<Integer> MIDDLE_NULL = (left, right) -> {
        if (left == null) return right == null ? 0 : (right <= 0 ? 1 : -1);
        if (right == null) return left <= 0 ? -1 : 1;
        return Integer.compare(left, right);
    };
    private record Order(String name, Comparator<Integer> comparator, boolean nullable) { }
    private static final List<Order> ORDERS = List.of(
            new Order("natural", null, false),
            new Order("reverse", Comparator.reverseOrder(), false),
            new Order("null-first", Comparator.nullsFirst(Comparator.naturalOrder()), true),
            new Order("null-last", Comparator.nullsLast(Comparator.naturalOrder()), true),
            new Order("null-middle", MIDDLE_NULL, true));

    private static Integer navigate(NavigableSet<Integer> view, Integer query, int operation) {
        return switch (operation) {
            case 0 -> view.lower(query);
            case 1 -> view.floor(query);
            case 2 -> view.ceiling(query);
            default -> view.higher(query);
        };
    }

    private static final class FaultOrder implements Comparator<Integer> {
        private final IllegalArgumentException failure = new IllegalArgumentException("one-shot comparator");
        private final Comparator<Integer> delegate;
        private int remaining = -1;
        private int calls;
        FaultOrder(Comparator<Integer> delegate) { this.delegate = delegate; }
        void arm(int call) { remaining = call; calls = 0; }
        @Override public int compare(Integer left, Integer right) {
            calls++;
            if (remaining == 0) {
                remaining = -1;
                throw failure;
            }
            if (remaining > 0) remaining--;
            return delegate.compare(left, right);
        }
    }

    @Test void oneShotComparatorFailureIsNeverMistakenForRangeRejection() {
        for (boolean reversed : new boolean[] {false, true}) {
            for (int bounds = 0; bounds < 3; bounds++) {
                for (int operation = 0; operation < 4; operation++) {
                    FaultOrder order = new FaultOrder(Comparator.naturalOrder());
                    NavigableSet<Integer> source = new TreeSet<>(order);
                    source.addAll(List.of(0, 1, 2, 3, 4, 5, 6));
                    if (bounds == 1) source = source.subSet(1, false, 5, false);
                    if (bounds == 2) source = source.subSet(1, true, 5, true).subSet(2, false, 4, true);
                    if (reversed) source = source.descendingSet();
                    NavigableSet<Integer> view = M3MutableFilteredViews.filter(source, value -> true);
                    int op = operation;
                    order.arm(0);
                    assertSame(order.failure, assertThrows(IllegalArgumentException.class,
                            () -> navigate(view, 3, op)), "exact one-shot failure identity");
                }
            }
        }
    }

    @Test void everyConsumedComparatorFailureKeepsItsIdentity() {
        for (Comparator<Integer> delegate : List.of(Comparator.<Integer>naturalOrder(), MIDDLE_NULL)) {
            for (boolean reversed : new boolean[] {false, true}) {
                for (int operation = 0; operation < 4; operation++) {
                    FaultOrder measuring = new FaultOrder(delegate);
                    NavigableSet<Integer> baseline = failureFixture(measuring, reversed, delegate == MIDDLE_NULL);
                    measuring.calls = 0;
                    navigate(baseline, 3, operation);
                    int consumed = measuring.calls;
                    assertTrue(consumed > 0);
                    for (int call = 0; call < consumed; call++) {
                        FaultOrder failing = new FaultOrder(delegate);
                        NavigableSet<Integer> view = failureFixture(failing, reversed, delegate == MIDDLE_NULL);
                        failing.arm(call);
                        int op = operation;
                        assertSame(failing.failure, assertThrows(IllegalArgumentException.class,
                                () -> navigate(view, 3, op)), "consumed comparator call " + call);
                    }
                }
            }
        }
    }

    private static NavigableSet<Integer> failureFixture(FaultOrder order, boolean reversed, boolean nullable) {
        NavigableSet<Integer> source = new TreeSet<>(order);
        source.addAll(List.of(-3, -2, -1, 0, 1, 2, 3, 4, 5));
        if (nullable) source.add(null);
        source = source.subSet(-2, true, 5, false);
        if (reversed) source = source.descendingSet();
        return M3MutableFilteredViews.filter(source, value -> value == null || value == 0);
    }

    @Test void nullableSparseBoundedAndReverseResultsAgreeWithIndependentTree() {
        Integer[] values = {-3, -2, -1, 0, null, 1, 2, 3};
        int comparisons = 0;
        for (boolean owned : new boolean[] {false, true}) {
            for (Order order : ORDERS) {
                for (int membership : new int[] {0, 1, 3, 15, 85, 170, 255}) {
                    NavigableSet<Integer> root = owned ? new M3TreeSet<>(order.comparator)
                            : new TreeSet<>(order.comparator);
                    for (int i = 0; i < values.length; i++) {
                        if ((membership & (1 << i)) != 0 && (values[i] != null || order.nullable))
                            root.add(values[i]);
                    }
                    Comparator<? super Integer> cmp = order.comparator == null
                            ? Comparator.naturalOrder() : order.comparator;
                    int low = cmp.compare(-2, 2) < 0 ? -2 : 2;
                    int high = low == -2 ? 2 : -2;
                    for (int bounds = 0; bounds < 3; bounds++) {
                        NavigableSet<Integer> range = bounds == 0 ? root
                                : root.subSet(low, bounds == 1, high, bounds == 2);
                        for (boolean reversed : new boolean[] {false, true}) {
                            NavigableSet<Integer> source = reversed ? range.descendingSet() : range;
                            for (int selection = 0; selection < 4; selection++) {
                                final int choice = selection;
                                Predicate<Integer> visible = value -> switch (choice) {
                                    case 0 -> true;
                                    case 1 -> value == null || (value & 1) == 0;
                                    case 2 -> value != null;
                                    default -> false;
                                };
                                NavigableSet<Integer> expected = new TreeSet<>(source.comparator());
                                for (Integer value : source) if (visible.test(value)) expected.add(value);
                                NavigableSet<Integer> actual = M3MutableFilteredViews.filter(source, visible);
                                assertEquals(new ArrayList<>(expected), new ArrayList<>(actual));
                                for (Integer query : Arrays.asList(-9, -3, -2, -1, 0, null, 1, 2, 3, 9)) {
                                    if (query == null && !order.nullable) continue;
                                    for (int operation = 0; operation < 4; operation++) {
                                        assertEquals(navigate(expected, query, operation),
                                                navigate(actual, query, operation), order.name);
                                        comparisons++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        assertEquals(64512, comparisons);
    }

    private static final class FailingRangeSet extends TreeSet<Integer> {
        private static final long serialVersionUID = 1L;
        private final IllegalArgumentException failure = new IllegalArgumentException("range source");
        private boolean iteratorFailure;
        private NavigableSet<Integer> refused() {
            if (!iteratorFailure) throw failure;
            return new TreeSet<>() {
                private static final long serialVersionUID = 1L;
                @Override public Iterator<Integer> iterator() { throw failure; }
                @Override public Iterator<Integer> descendingIterator() { throw failure; }
            };
        }
        @Override public NavigableSet<Integer> headSet(Integer key, boolean inclusive) { return refused(); }
        @Override public NavigableSet<Integer> tailSet(Integer key, boolean inclusive) { return refused(); }
    }

    @Test void consumedRangeAndIteratorFailuresAreNeverRecovered() {
        for (boolean iteratorFailure : new boolean[] {false, true}) {
            for (int operation = 0; operation < 4; operation++) {
                FailingRangeSet source = new FailingRangeSet();
                source.addAll(List.of(0, 1, 2, 3, 4, 5, 6));
                source.iteratorFailure = iteratorFailure;
                NavigableSet<Integer> view = M3MutableFilteredViews.filter(source, value -> false);
                int op = operation;
                assertSame(source.failure, assertThrows(IllegalArgumentException.class,
                        () -> navigate(view, 3, op)));
            }
        }
    }

    @Test void storedRepresentativesAndPredicateFailureIdentitySurvive() {
        record Key(int rank, String label) { }
        NavigableSet<Key> source = new TreeSet<>(Comparator.comparingInt(Key::rank));
        Key stored = new Key(3, "stored");
        source.add(stored);
        source.add(new Key(7, "other"));
        List<Key> observed = new ArrayList<>();
        NavigableSet<Key> view = M3MutableFilteredViews.filter(source, value -> {
            observed.add(value); return true;
        });
        assertSame(stored, view.floor(new Key(3, "query")));
        assertEquals(List.of(stored), observed);
        IllegalArgumentException failure = new IllegalArgumentException("predicate");
        NavigableSet<Key> failing = M3MutableFilteredViews.filter(source, value -> { throw failure; });
        assertSame(failure, assertThrows(IllegalArgumentException.class, () -> failing.ceiling(stored)));
    }
}
