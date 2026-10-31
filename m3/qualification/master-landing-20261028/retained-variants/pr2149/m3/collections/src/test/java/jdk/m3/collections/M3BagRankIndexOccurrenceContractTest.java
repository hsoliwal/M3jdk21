/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Independent public-contract oracle for occurrence selection in sliced count snapshots. */
final class M3BagRankIndexOccurrenceContractTest {
    private static final Comparator<String> ORDER = Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER);
    private record Group(String key, int count) { }

    private static M3CountedBag<String> owner() {
        M3CountedBag<String> bag = new M3CountedBag<>();
        bag.addOccurrences("c", 4);
        bag.addOccurrences("b", 2);
        bag.addOccurrences(null, 2);
        bag.addOccurrences("B", 3);
        bag.addOccurrences("a", 1);
        return bag;
    }

    private static List<Group> groups() {
        // Stable comparator ties retain the independently known insertion order b, B.
        return List.of(new Group(null, 2), new Group("a", 1), new Group("b", 2),
                new Group("B", 3), new Group("c", 4));
    }

    private static List<String> expand(List<Group> groups) {
        List<String> expected = new ArrayList<>();
        for (Group group : groups) {
            for (int i = 0; i < group.count(); i++) expected.add(group.key());
        }
        return expected;
    }

    private static void refusedRank(M3BagRankIndex<?> view, long rank) {
        IndexOutOfBoundsException failure = assertThrows(IndexOutOfBoundsException.class,
                () -> view.occurrenceAt(rank));
        assertEquals("occurrence " + rank, failure.getMessage());
    }

    private static void check(M3BagRankIndex<String> view, List<Group> expectedGroups) {
        List<String> expected = expand(expectedGroups);
        assertEquals(expectedGroups.size(), view.distinctSize());
        assertEquals((long) expected.size(), view.exactSize());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), view.occurrenceAt(i), "occurrence " + i);
        }
        refusedRank(view, -1);
        refusedRank(view, Long.MIN_VALUE);
        refusedRank(view, expected.size());
        refusedRank(view, Long.MAX_VALUE);
    }

    private static void allNestedSlices(M3BagRankIndex<String> view, List<Group> expected) {
        for (int from = 0; from <= expected.size(); from++) {
            for (int to = from; to <= expected.size(); to++) {
                M3BagRankIndex<String> slice = view.slice(from, to);
                List<Group> sliceGroups = expected.subList(from, to);
                check(slice, sliceGroups);
                for (int nestedFrom = 0; nestedFrom <= sliceGroups.size(); nestedFrom++) {
                    for (int nestedTo = nestedFrom; nestedTo <= sliceGroups.size(); nestedTo++) {
                        check(slice.slice(nestedFrom, nestedTo),
                                sliceGroups.subList(nestedFrom, nestedTo));
                    }
                }
            }
        }
    }

    @Test void nestedSlicesKeepOccurrenceOffsetsAndStableComparatorTies() {
        M3CountedBag<String> bag = owner();
        M3BagRankIndex<String> snapshot = M3BagRankIndex.snapshot(bag, ORDER);
        // Later source writes must not alter any admitted key or count.
        bag.setOccurrences("b", 17);
        bag.removeOccurrences(null, 2);
        bag.addOccurrences("new", 5);
        allNestedSlices(snapshot, groups());
    }

    @Test void reversedFilteredAndMaskedViewsRetainTheirOwnCountGeometry() {
        M3BagRankIndex<String> snapshot = M3BagRankIndex.snapshot(owner(), ORDER);
        List<Group> reverseGroups = new ArrayList<>(groups());
        Collections.reverse(reverseGroups);
        allNestedSlices(snapshot.reversed(), reverseGroups);

        M3BagRankIndex<String> filtered = snapshot.filtered(value -> value == null || !value.equals("b"));
        List<Group> filteredGroups = List.of(groups().get(0), groups().get(1), groups().get(3), groups().get(4));
        allNestedSlices(filtered, filteredGroups);

        byte[] states = {(byte) 0x80, 0, (byte) 0x81, (byte) 0x80, 1};
        M3BagRankIndex<String> masked = snapshot.selected(states, 0x80, 1,
                M3IndexMaskCompactor.Backend.JAVA);
        Arrays.fill(states, (byte) 0);
        allNestedSlices(masked, List.of(groups().get(0), groups().get(3)));
        check(snapshot.reversed().slice(1, 4).reversed(), groups().subList(1, 4));
    }

    @Test void nonzeroSliceBasesAndOccurrenceRanksRemainLongs() {
        long max = Integer.MAX_VALUE;
        M3CountedBag<Integer> bag = new M3CountedBag<>();
        bag.addOccurrences(1, Integer.MAX_VALUE);
        bag.addOccurrences(2, 2);
        bag.addOccurrences(3, Integer.MAX_VALUE);
        bag.addOccurrences(4, 1);
        M3BagRankIndex<Integer> slice = M3BagRankIndex.snapshot(bag, null).slice(1, 4);
        assertEquals(max + 3, slice.exactSize());
        assertEquals(Integer.MAX_VALUE, slice.size());
        assertEquals(2, slice.occurrenceAt(0).intValue());
        assertEquals(2, slice.occurrenceAt(1).intValue());
        assertEquals(3, slice.occurrenceAt(2).intValue());
        assertEquals(3, slice.occurrenceAt(max + 1).intValue());
        assertEquals(4, slice.occurrenceAt(max + 2).intValue());
        refusedRank(slice, max + 3);

        M3BagRankIndex<Integer> nested = slice.slice(1, 2);
        assertEquals(max, nested.exactSize());
        assertEquals(3, nested.occurrenceAt(0).intValue());
        assertEquals(3, nested.occurrenceAt(max - 1).intValue());
        refusedRank(nested, max);

        M3BagRankIndex<Integer> reverse = slice.reversed();
        assertEquals(4, reverse.occurrenceAt(0).intValue());
        assertEquals(3, reverse.occurrenceAt(1).intValue());
        assertEquals(3, reverse.occurrenceAt(max).intValue());
        assertEquals(2, reverse.occurrenceAt(max + 1).intValue());
        assertEquals(2, reverse.occurrenceAt(max + 2).intValue());
        refusedRank(reverse, max + 3);
    }

    @Test void selectionPreservesRepresentativeIdentityWithoutCallbacks() {
        AtomicInteger comparisons = new AtomicInteger();
        AtomicInteger predicates = new AtomicInteger();
        Comparator<String> comparator = (left, right) -> {
            comparisons.incrementAndGet();
            return ORDER.compare(left, right);
        };
        String lower = new String("b");
        String upper = new String("B");
        M3CountedBag<String> bag = new M3CountedBag<>();
        bag.addOccurrences(lower, 2);
        bag.addOccurrences(upper, 3);
        bag.addOccurrences(null, 1);
        M3BagRankIndex<String> view = M3BagRankIndex.snapshot(bag, comparator)
                .filtered(value -> { predicates.incrementAndGet(); return true; })
                .slice(1, 3);
        comparisons.set(0);
        predicates.set(0);
        assertSame(lower, view.occurrenceAt(0));
        assertSame(lower, view.occurrenceAt(1));
        assertSame(upper, view.occurrenceAt(2));
        assertSame(upper, view.occurrenceAt(4));
        refusedRank(view, 5);
        assertEquals(0, comparisons.get());
        assertEquals(0, predicates.get());
    }
}
