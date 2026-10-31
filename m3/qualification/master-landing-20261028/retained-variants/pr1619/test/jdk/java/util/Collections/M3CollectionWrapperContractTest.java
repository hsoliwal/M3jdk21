/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Contract baseline for sorted, backed, checked and unmodifiable collection views.
 * @run main M3CollectionWrapperContractTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.List;
import java.util.Set;

/**
 * Standalone Java 21 behavioral oracle for array/index/mask wrapper convergence.
 * This test only qualifies public JDK contracts; it does not assert M3 storage adoption.
 */
public final class M3CollectionWrapperContractTest {
    private M3CollectionWrapperContractTest() { }

    public static void main(String[] args) {
        sortedLiveViews();
        filteredViewsAreNotSnapshots();
        checkedViewsEnforceRuntimeType();
        unmodifiableViewsRejectMutation();
        System.out.println("M3CollectionWrapperContractTest: PASS (4 cases)");
    }

    private static void sortedLiveViews() {
        NavigableSet<Integer> backing = new TreeSet<>(Comparator.reverseOrder());
        backing.addAll(List.of(1, 3, 5, 7, 9));
        NavigableSet<Integer> sub = backing.subSet(7, true, 3, false);
        require(sub.equals(Set.of(5, 7)), "reverse comparator subset membership");
        require(sub.first() == 7 && sub.last() == 5, "reverse comparator iteration order");
        backing.add(6);
        require(sub.contains(6) && sub.size() == 3, "subset is a backed live view");
        NavigableSet<Integer> descending = sub.descendingSet();
        require(descending.first() == 5 && descending.last() == 7,
                "descending view reverses encounter order");
        descending.remove(6);
        require(!backing.contains(6), "nested view remove mutates backing owner");
    }

    private static void filteredViewsAreNotSnapshots() {
        // An array-backed selection predicate is an optional index, not a copied payload owner.
        List<String> backing = new ArrayList<>(Arrays.asList("a", "bbb", "cc"));
        List<String> filtered = new java.util.AbstractList<>() {
            @Override public String get(int index) {
                int selected = 0;
                for (String value : backing) {
                    if ((value.length() & 1) == 0 && selected++ == index) return value;
                }
                throw new IndexOutOfBoundsException(index);
            }
            @Override public int size() {
                int count = 0;
                for (String value : backing) if ((value.length() & 1) == 0) count++;
                return count;
            }
        };
        require(filtered.equals(List.of("cc")), "filter predicate initial content");
        backing.add("dddd");
        require(filtered.equals(List.of("cc", "dddd")), "live filter sees backing append");
        backing.set(2, "odd");
        require(filtered.equals(List.of("dddd")), "live filter sees predicate invalidation");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void checkedViewsEnforceRuntimeType() {
        List<String> backing = new ArrayList<>();
        List<String> checked = Collections.checkedList(backing, String.class);
        checked.add("ok");
        List raw = checked;
        expect(ClassCastException.class, () -> raw.add(13), "checked add rejects wrong type");
        require(backing.equals(List.of("ok")), "failed checked add does not mutate backing");
    }

    private static void unmodifiableViewsRejectMutation() {
        NavigableSet<Integer> backing = new TreeSet<>(List.of(1, 2, 3, 4));
        NavigableSet<Integer> frozen = Collections.unmodifiableNavigableSet(backing);
        NavigableSet<Integer> nested = frozen.subSet(2, true, 4, false);
        expect(UnsupportedOperationException.class, () -> nested.remove(2),
                "nested unmodifiable view rejects mutation");
        backing.add(5);
        require(frozen.contains(5), "unmodifiable is backed, not snapshotted");
        expect(UnsupportedOperationException.class, frozen::pollFirst,
                "unmodifiable navigation rejects mutation");
    }

    private static void expect(Class<? extends Throwable> expected, Runnable action, String label) {
        try {
            action.run();
        } catch (Throwable actual) {
            require(expected.isInstance(actual), label + ": unexpected " + actual);
            return;
        }
        throw new AssertionError(label + ": expected " + expected.getName());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
