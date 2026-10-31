// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;

/**
 * Original-owner differential proof: sorted projected physical indexes expose
 * their comparator; filtered encounter-order projections never claim sorted.
 */
public final class IndexedLiveSortedSpliteratorContract {
    private record Item(int id, int bucket) { }
    private static long checks;

    private IndexedLiveSortedSpliteratorContract() { }

    private static void check(boolean accepted, String label) {
        checks++;
        if (!accepted) throw new AssertionError(label + " check=" + checks);
    }

    private static void expectThrows(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("unexpected " + failure, failure);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    private static void metadataAndLateBinding() {
        M3ArrayList<Item> owner = new M3ArrayList<>();
        Item first = new Item(1, 2);
        Item second = new Item(2, 1);
        Item tie = new Item(3, 1);
        owner.add(first);
        owner.add(second);
        Comparator<Item> ascending = Comparator.comparingInt(Item::bucket);
        M3IndexedLiveView<Item> view = M3IndexedLiveView.sorted(owner, ascending);
        Spliterator<Item> cursor = view.spliterator();
        check((cursor.characteristics() & Spliterator.ORDERED) != 0, "sorted order");
        check((cursor.characteristics() & Spliterator.SORTED) != 0, "sorted characteristic");
        check((cursor.characteristics() & Spliterator.SIZED) == 0, "live view size is unknown");
        check((cursor.characteristics() & Spliterator.DISTINCT) == 0, "duplicates not elided");
        check(cursor.estimateSize() == Long.MAX_VALUE, "late size bound");
        check(cursor.getComparator() == ascending, "exact source comparator identity");
        check(cursor.trySplit() == null, "no unsupported parallel splitting");

        // Spliterator was created before this structural modification. First
        // binding must prepare the latest owner, not a stale copied array.
        owner.add(tie);
        List<Item> observed = new ArrayList<>();
        cursor.forEachRemaining(observed::add);
        check(observed.size() == 3, "late-bound size");
        check(observed.get(0) == second && observed.get(1) == tie
                && observed.get(2) == first, "stable comparator ties");
        check(owner.get(0) == first, "original Object[] owner unchanged");

        Spliterator<Item> filtered = M3IndexedLiveView.filtered(
                owner, element -> element.bucket() >= 1).spliterator();
        check((filtered.characteristics() & Spliterator.ORDERED) != 0, "filtered order");
        check((filtered.characteristics() & Spliterator.SORTED) == 0, "filtered not sorted");
        expectThrows(IllegalStateException.class, filtered::getComparator);

        // Natural-order metadata must return null, not an arbitrary comparator.
        M3ArrayList<Integer> numbers = new M3ArrayList<>(List.of(4, 1, 3, 2));
        Spliterator<Integer> natural = M3IndexedLiveView.sorted(numbers, null).spliterator();
        check((natural.characteristics() & Spliterator.SORTED) != 0, "natural-order sorted");
        check(natural.getComparator() == null, "natural-order comparator is null");
        List<Integer> result = new ArrayList<>();
        natural.forEachRemaining(result::add);
        check(result.equals(List.of(1, 2, 3, 4)), "natural sorted values");

        // A cursor already bound to its owner must preserve fail-fast epoch
        // semantics rather than silently delivering a different index order.
        Spliterator<Item> bound = view.spliterator();
        check(bound.tryAdvance(element -> { }), "bind cursor");
        owner.set(0, new Item(10, 7));
        expectThrows(ConcurrentModificationException.class,
                () -> bound.tryAdvance(element -> { }));
    }

    private static void randomizedStableDifferential() {
        Random random = new Random(0x5107ED);
        Comparator<Item> order = Comparator.comparingInt(Item::bucket).reversed();
        for (int trial = 0; trial < 750; trial++) {
            M3ArrayList<Item> owner = new M3ArrayList<>();
            int count = random.nextInt(45);
            for (int i = 0; i < count; i++) {
                owner.add(new Item(trial * 100 + i, random.nextInt(11)));
            }
            List<Item> expected = new ArrayList<>(owner);
            expected.sort(order);
            M3IndexedLiveView<Item> projected = M3IndexedLiveView.sorted(owner, order);
            Spliterator<Item> spliterator = projected.spliterator();
            check((spliterator.characteristics() & Spliterator.SORTED) != 0,
                    "random sorted characteristic");
            check(spliterator.getComparator() == order, "random comparator identity");
            List<Item> actual = new ArrayList<>();
            spliterator.forEachRemaining(actual::add);
            check(actual.size() == expected.size(), "random output cardinality");
            for (int i = 0; i < actual.size(); i++) {
                check(actual.get(i) == expected.get(i), "stable primitive index tie");
            }
            M3IndexedLiveView<Item> filtered = M3IndexedLiveView.filteredSorted(
                    owner, item -> (item.bucket() & 1) == 0, order);
            List<Item> expectedFiltered = new ArrayList<>();
            for (Item item : owner) {
                if ((item.bucket() & 1) == 0) expectedFiltered.add(item);
            }
            expectedFiltered.sort(order);
            Spliterator<Item> selected = filtered.spliterator();
            check((selected.characteristics() & Spliterator.SORTED) != 0,
                    "filtered sorted characteristic");
            check(selected.getComparator() == order, "filtered sorted comparator");
            List<Item> actualFiltered = new ArrayList<>();
            selected.forEachRemaining(actualFiltered::add);
            check(actualFiltered.size() == expectedFiltered.size(), "selected count");
            for (int i = 0; i < actualFiltered.size(); i++) {
                check(actualFiltered.get(i) == expectedFiltered.get(i),
                        "selected stable sorted identity");
            }
        }
    }

    public static void main(String[] ignored) {
        metadataAndLateBinding();
        randomizedStableDifferential();
        System.out.println("INDEXED_LIVE_SORTED_SPLITERATOR_PASS checks=" + checks);
    }
}
