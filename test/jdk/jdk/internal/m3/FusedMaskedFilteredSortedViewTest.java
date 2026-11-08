/*
 * Copyright (c) 2026.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package jdk.internal.m3;

import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;

/** Focused live-view contract for fused predicate/state/sort selection. */
public final class FusedMaskedFilteredSortedViewTest {
    private static int checks;

    private FusedMaskedFilteredSortedViewTest() {
    }

    public static void main(String[] args) {
        IndexedArray<Item> store = new IndexedArray<>();
        Item a = new Item(1, 2);
        Item b = new Item(2, 1);
        Item c = new Item(3, 2);
        Item d = new Item(4, 0);

        long ha = store.append(a);
        long hb = store.append(b);
        long hc = store.append(c);
        long hd = store.append(d);

        store.retain(ha);
        store.retain(hb);
        store.retain(hb);
        store.retain(hc);
        store.retain(hd);

        int required = IndexedArray.LIVE | IndexedArray.LEASED;
        int forbidden = IndexedArray.SHARED;

        List<Item> filtered = store.filteredMaskedStates(
                item -> item.id != 4, required, forbidden);
        same(a, filtered.get(0), "filtered first");
        same(c, filtered.get(1), "filtered second");
        equal(2, filtered.size(), "filtered size");

        Comparator<Item> byKey = Comparator.comparingInt(item -> item.key);
        List<Item> sorted = store.filteredMaskedSortedStates(
                item -> item.id != 4, required, forbidden, byKey);
        same(a, sorted.get(0), "stable tie first");
        same(c, sorted.get(1), "stable tie second");

        store.release(hb);
        same(b, sorted.get(0), "shared becomes visible");
        same(a, sorted.get(1), "stable tie remains first");
        same(c, sorted.get(2), "stable tie remains second");

        store.release(ha);
        same(b, sorted.get(0), "live state refresh first");
        same(c, sorted.get(1), "live state refresh second");
        equal(2, sorted.size(), "live state refresh size");

        List<Item> maskOnly = store.maskedSortedStates(required, forbidden, byKey);
        same(d, maskOnly.get(0), "mask-only retains predicate-rejected owner");
        same(b, maskOnly.get(1), "mask-only sorted second");
        same(c, maskOnly.get(2), "mask-only sorted third");

        expect(IllegalArgumentException.class,
                () -> store.filteredMaskedStates(item -> true,
                        IndexedArray.LEASED, IndexedArray.LEASED),
                "overlapping masks");
        expect(NullPointerException.class,
                () -> store.filteredMaskedStates(null, required, forbidden),
                "null predicate");
        expect(NullPointerException.class,
                () -> store.filteredMaskedSortedStates(
                        item -> true, required, forbidden, null),
                "null comparator");

        IndexedArray<Integer> reentrant = new IndexedArray<>();
        reentrant.append(1);
        List<Integer> reentrantView = reentrant.filteredMaskedStates(value -> {
            reentrant.append(2);
            return true;
        }, IndexedArray.LIVE, 0);
        expect(ConcurrentModificationException.class, reentrantView::size,
                "predicate mutation");

        IndexedArray<Integer> comparatorMutation = new IndexedArray<>();
        long h0 = comparatorMutation.append(2);
        comparatorMutation.append(1);
        comparatorMutation.retain(h0);
        List<Integer> comparatorView = comparatorMutation.filteredMaskedSortedStates(
                value -> true, IndexedArray.LIVE, 0, (left, right) -> {
                    comparatorMutation.release(h0);
                    return Integer.compare(left, right);
                });
        expect(ConcurrentModificationException.class, comparatorView::size,
                "comparator state mutation");

        System.out.println("PASS M3FusedMaskedFilteredSortedViewTest checks=" + checks);
    }

    private static void same(Object expected, Object actual, String label) {
        checks++;
        if (expected != actual) {
            throw new AssertionError(label + ": expected identity " + expected
                    + " but was " + actual);
        }
    }

    private static void equal(int expected, int actual, String label) {
        checks++;
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected
                    + " but was " + actual);
        }
    }

    private static void expect(Class<? extends Throwable> type,
            Runnable action, String label) {
        checks++;
        try {
            action.run();
        } catch (Throwable problem) {
            if (type.isInstance(problem)) {
                return;
            }
            throw new AssertionError(label + ": wrong exception " + problem, problem);
        }
        throw new AssertionError(label + ": expected " + type.getName());
    }

    private static final class Item {
        private final int id;
        private final int key;

        private Item(int id, int key) {
            this.id = id;
            this.key = key;
        }

        @Override
        public String toString() {
            return "Item[" + id + "," + key + "]";
        }
    }
}
