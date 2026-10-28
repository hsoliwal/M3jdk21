/*
 * @test
 * @summary Indexed array sorted views retain original comparator through splits and ranges
 * @modules java.base/jdk.internal.m3
 * @run main/othervm jdk.internal.m3.IndexedViewSortedSpliteratorTest
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package jdk.internal.m3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Spliterator;

public final class IndexedViewSortedSpliteratorTest {
    private record Item(int key, int serial) { }
    private static int checks;

    private static void check(boolean good, String label) {
        checks++;
        if (!good) throw new AssertionError(label + " check=" + checks);
    }
    private static void expect(Class<? extends Throwable> type, Runnable callback, String label) {
        checks++;
        try { callback.run(); }
        catch (Throwable problem) {
            if (type.isInstance(problem)) return;
            throw new AssertionError(label + ": wrong failure", problem);
        }
        throw new AssertionError(label + ": missing failure");
    }
    private static <E> List<E> drain(Spliterator<E> cursor) {
        ArrayList<E> out = new ArrayList<>();
        cursor.forEachRemaining(out::add);
        return out;
    }
    private static <E> void sorted(Spliterator<E> cursor, Comparator<? super E> comparator) {
        check(cursor.hasCharacteristics(Spliterator.SORTED | Spliterator.ORDERED
                | Spliterator.SIZED | Spliterator.SUBSIZED), "sorted metadata");
        check(!cursor.hasCharacteristics(Spliterator.DISTINCT), "not distinct");
        check(cursor.getComparator() == comparator, "original comparator identity");
    }

    public static void main(String[] ignored) {
        M3IndexedArray<Item> owner = new M3IndexedArray<>();
        Item a = new Item(2, 0), b = new Item(1, 1), c = new Item(2, 2), d = new Item(3, 3);
        long ha = owner.append(a);
        owner.append(b);
        long hc = owner.append(c);
        owner.append(d);
        Comparator<Item> comparator = Comparator.comparingInt(Item::key);
        List<Item> view = owner.sorted(comparator);
        Spliterator<Item> suffix = view.spliterator();
        sorted(suffix, comparator);
        Spliterator<Item> prefix = suffix.trySplit();
        check(prefix != null, "split exists");
        sorted(prefix, comparator);
        List<Item> values = drain(prefix);
        values.addAll(drain(suffix));
        check(values.equals(List.of(b, a, c, d)), "stable order after split");
        Spliterator<Item> nested = view.subList(1, 4).subList(0, 2).spliterator();
        sorted(nested, comparator);
        check(drain(nested).equals(List.of(a, c)), "stable range");
        Spliterator<Item> predicate = owner.filteredSorted(x -> x.key() >= 2,
                comparator).spliterator();
        sorted(predicate, comparator);
        check(drain(predicate).equals(List.of(a, c, d)), "filtered sorted");

        Spliterator<Item> unsorted = owner.filtered(x -> true).spliterator();
        check(!unsorted.hasCharacteristics(Spliterator.SORTED), "unsorted not sorted");
        expect(IllegalStateException.class, unsorted::getComparator, "unsorted comparator");
        owner.retain(ha);
        owner.retain(hc);
        int required = M3IndexedArray.LIVE | M3IndexedArray.LEASED;
        Spliterator<Item> masked = owner.maskedSortedStates(required,
                M3IndexedArray.SHARED, comparator).spliterator();
        sorted(masked, comparator);
        check(drain(masked).equals(List.of(a, c)), "masked sorted");
        Spliterator<Item> plainMask = owner.maskedStates(required, 0).spliterator();
        check(!plainMask.hasCharacteristics(Spliterator.SORTED), "mask not sorted");
        expect(IllegalStateException.class, plainMask::getComparator, "mask comparator");
        Spliterator<Item> boundState = owner.maskedSortedStates(required, 0,
                comparator).spliterator();
        check(boundState.estimateSize() == 2, "state bind");
        owner.release(ha);
        expect(ConcurrentModificationException.class,
                () -> boundState.tryAdvance(v -> { }), "state fail-fast");
        Spliterator<Item> boundValues = owner.sorted(comparator).spliterator();
        check(boundValues.estimateSize() == 4, "value bind");
        owner.append(new Item(0, 4));
        expect(ConcurrentModificationException.class, boundValues::trySplit,
                "structural fail-fast");
        System.out.println("IndexedViewSortedSpliteratorTest PASS checks=" + checks);
    }

    private IndexedViewSortedSpliteratorTest() { }
}
