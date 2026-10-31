// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/** Dependency-free Java 21 differential and complexity-shape oracle. */
public final class M3FilteredNavigableFacadeSelfTest {
    private static long checks;

    private M3FilteredNavigableFacadeSelfTest() { }

    private static void equal(Object expected, Object actual, String why) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(why + ": " + expected + " != " + actual);
        }
    }

    private static void require(boolean value, String why) {
        equal(true, value, why);
    }

    private static void refuses(Runnable action, String why) {
        checks++;
        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError(why + ": mutation was accepted");
    }

    private static <E> List<E> list(Iterable<E> values) {
        ArrayList<E> result = new ArrayList<>();
        values.forEach(result::add);
        return result;
    }

    private static void deterministicFacade() {
        NavigableSet<Integer> source = new TreeSet<>(List.of(1, 2, 3, 4, 5, 6, 7, 8));
        NavigableSet<Integer> facade = M3FilteredNavigableSet.view(source, value -> (value & 1) == 0);
        NavigableSet<Integer> owner = M3FilteredViews.filter(source, value -> (value & 1) == 0);
        equal(list(owner), list(facade), "same canonical projection");
        equal(owner.lower(5), facade.lower(5), "same lower");
        equal(owner.floor(5), facade.floor(5), "same floor");
        equal(owner.ceiling(5), facade.ceiling(5), "same ceiling");
        equal(owner.higher(5), facade.higher(5), "same higher");
        equal(owner.comparator(), facade.comparator(), "same comparator");
        require(facade instanceof M3FilteredNavigableSet<?>, "compatibility type retained");
        require(facade.descendingSet() instanceof M3FilteredNavigableSet<?>,
                "descending compatibility type retained");
        require(facade.subSet(2, true, 8, false) instanceof M3FilteredNavigableSet<?>,
                "range compatibility type retained");
        source.add(10);
        equal(10, facade.last(), "live backing addition");
        source.remove(2);
        equal(4, facade.first(), "live backing removal");
        refuses(() -> facade.add(12), "add");
        refuses(() -> facade.remove(4), "remove");
        refuses(facade::clear, "clear");
        refuses(facade::pollFirst, "pollFirst");
        refuses(facade::pollLast, "pollLast");
        var iterator = facade.iterator();
        iterator.next();
        refuses(iterator::remove, "iterator remove");
    }

    private static void randomizedSemantics() {
        Random random = new Random(0x4d3346696c746572L);
        for (int round = 0; round < 750; round++) {
            Comparator<Integer> comparator = random.nextBoolean()
                    ? Comparator.naturalOrder() : Comparator.reverseOrder();
            TreeSet<Integer> source = new TreeSet<>(comparator);
            TreeMap<Integer, Integer> map = new TreeMap<>(comparator);
            int divisor = 1 + random.nextInt(9);
            int residue = random.nextInt(divisor);
            Predicate<Integer> predicate = value -> Math.floorMod(value, divisor) == residue;
            for (int n = 0; n < random.nextInt(128); n++) {
                int value = random.nextInt(240) - 120;
                source.add(value);
                map.put(value, value % 13 == 0 ? null : value * 17);
            }

            TreeSet<Integer> expected = new TreeSet<>(comparator);
            for (int value : source) if (predicate.test(value)) expected.add(value);
            NavigableSet<Integer> canonical = M3FilteredViews.filter(source, predicate);
            NavigableSet<Integer> facade = M3FilteredNavigableSet.view(source, predicate);
            equal(list(expected), list(canonical), "canonical set order");
            equal(list(expected), list(facade), "facade set order");
            equal(list(expected.descendingSet()), list(facade.descendingSet()), "facade reverse order");

            NavigableMap<Integer, Integer> filteredMap = M3FilteredViews.filterEntries(
                    map, (key, value) -> predicate.test(key) && (value == null || value % 5 != 0));
            TreeMap<Integer, Integer> expectedMap = new TreeMap<>(comparator);
            map.forEach((key, value) -> {
                if (predicate.test(key) && (value == null || value % 5 != 0)) {
                    expectedMap.put(key, value);
                }
            });

            for (int probe = -135; probe <= 135; probe += 15) {
                equal(expected.lower(probe), facade.lower(probe), "facade lower");
                equal(expected.floor(probe), facade.floor(probe), "facade floor");
                equal(expected.ceiling(probe), facade.ceiling(probe), "facade ceiling");
                equal(expected.higher(probe), facade.higher(probe), "facade higher");
                equal(expectedMap.lowerEntry(probe), filteredMap.lowerEntry(probe), "map lower");
                equal(expectedMap.floorEntry(probe), filteredMap.floorEntry(probe), "map floor");
                equal(expectedMap.ceilingEntry(probe), filteredMap.ceilingEntry(probe), "map ceiling");
                equal(expectedMap.higherEntry(probe), filteredMap.higherEntry(probe), "map higher");
            }
            equal(expectedMap.firstEntry(), filteredMap.firstEntry(), "map first");
            equal(expectedMap.lastEntry(), filteredMap.lastEntry(), "map last");
        }
    }

    /** Count comparator work, not elapsed time. */
    private static final class CountingComparator implements Comparator<Integer> {
        private final AtomicLong calls = new AtomicLong();
        @Override public int compare(Integer left, Integer right) {
            calls.incrementAndGet();
            return Integer.compare(left, right);
        }
        long reset() { return calls.getAndSet(0); }
        long calls() { return calls.get(); }
    }

    private static void boundedTraversalWorkShape() {
        CountingComparator order = new CountingComparator();
        TreeSet<Integer> set = new TreeSet<>(order);
        TreeMap<Integer, Integer> map = new TreeMap<>(order);
        for (int value = 0; value < 4096; value++) {
            set.add(value);
            map.put(value, value);
        }
        Predicate<Integer> lastOnly = value -> value == 4095;
        NavigableSet<Integer> selected = M3FilteredViews.filter(set, lastOnly);
        NavigableMap<Integer, Integer> selectedMap =
                M3FilteredViews.filterEntries(map, (key, value) -> key == 4095);

        order.reset();
        equal(4095, selected.ceiling(0), "set bounded navigation result");
        long selectedSetComparisons = order.calls();

        order.reset();
        Integer candidate = set.ceiling(0);
        while (candidate != null && !lastOnly.test(candidate)) candidate = set.higher(candidate);
        equal(4095, candidate, "old repeated-search model result");
        long repeatedSetComparisons = order.calls();
        require(selectedSetComparisons * 8 < repeatedSetComparisons,
                "bounded set traversal avoids repeated logarithmic searches");

        order.reset();
        equal(Map.entry(4095, 4095), selectedMap.ceilingEntry(0),
                "map bounded navigation result");
        long selectedMapComparisons = order.calls();

        order.reset();
        Map.Entry<Integer, Integer> entry = map.ceilingEntry(0);
        while (entry != null && entry.getKey() != 4095) entry = map.higherEntry(entry.getKey());
        equal(Map.entry(4095, 4095), entry, "old repeated map-search model result");
        long repeatedMapComparisons = order.calls();
        require(selectedMapComparisons * 8 < repeatedMapComparisons,
                "bounded map traversal avoids repeated logarithmic searches");

        System.out.println("MEASURE setComparisons bounded=" + selectedSetComparisons
                + " repeated=" + repeatedSetComparisons
                + " mapComparisons bounded=" + selectedMapComparisons
                + " repeated=" + repeatedMapComparisons);
    }

    public static void main(String[] args) {
        deterministicFacade();
        randomizedSemantics();
        boundedTraversalWorkShape();
        System.out.println("PASS M3FilteredNavigableFacadeSelfTest checks=" + checks
                + " randomized=750");
    }
}
