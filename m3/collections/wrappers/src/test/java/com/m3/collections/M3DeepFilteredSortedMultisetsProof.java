/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.function.Predicate;

/** Finite deep-composition proof using the retained counted-owner domain and a TreeMap oracle. */
public final class M3DeepFilteredSortedMultisetsProof {
    private static long checks;

    private M3DeepFilteredSortedMultisetsProof() { }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual))
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
    }

    private static <T extends Throwable> T expect(Class<T> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            check(type.isInstance(failure), "wrong failure: " + failure);
            return type.cast(failure);
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static <E> M3IndexSortedMultiset<E> source(Comparator<? super E> comparator) {
        return new M3IndexSortedMultiset<>(
                new M3FilteredSortedMultisetContract.Domain<E>(), comparator);
    }

    private static <E> M3FilteredSortedMultisets.View<E> chain(
            M3IndexSortedMultiset<E> source, Predicate<? super E> predicate,
            int depth, boolean readOnly) {
        M3FilteredSortedMultisets.View<E> view = readOnly
                ? M3FilteredSortedMultisets.readOnlyFiltered(source, predicate)
                : M3FilteredSortedMultisets.filtered(source, predicate);
        for (int i = 1; i < depth; i++) view = view.filtered(predicate);
        return view;
    }

    private static <E> List<E> values(Iterable<E> source) {
        ArrayList<E> result = new ArrayList<>();
        for (E value : source) result.add(value);
        return result;
    }

    private static void deepCountsAndEarlierViews(int depth) {
        M3IndexSortedMultiset<Integer> backing = source(null);
        backing.add(2, Integer.MAX_VALUE);
        backing.add(3, 11);
        backing.add(4, 7);
        int[] calls = {0};
        Predicate<Integer> even = value -> { calls[0]++; return (value & 1) == 0; };
        M3FilteredSortedMultisets.View<Integer> view =
                M3FilteredSortedMultisets.filtered(backing, even);
        M3FilteredSortedMultisets.View<Integer> earlier = view;
        for (int i = 1; i < depth; i++) {
            view = view.filtered(even);
            if (i + 1 == depth / 2) earlier = view;
        }
        check(view.space() == backing.space(), "deep view substituted domain owner");
        calls[0] = 0;
        equal(Integer.MAX_VALUE, view.count(2), "deep count");
        equal(depth, calls[0], "duplicate predicate occurrences were collapsed");
        calls[0] = 0;
        equal(0, view.count(3), "hidden count");
        equal(1, calls[0], "short circuit crossed rejecting first atom");
        calls[0] = 0;
        equal((long) Integer.MAX_VALUE + 7L, view.longSize(), "counted total");
        equal(2 * depth + 1, calls[0], "large groups expanded during total");
        equal(Integer.MAX_VALUE, view.size(), "saturated Collection size");
        equal(2, view.distinctSize(), "distinct count");
        equal(4, view.elementAtOccurrence(Integer.MAX_VALUE), "large ordinal selection");
        equal((long) Integer.MAX_VALUE, view.rankOf(4, false), "large rank");
        long[] total = {0};
        int[] groups = {0};
        calls[0] = 0;
        view.forEachIdCount((id, count) -> {
            check(id >= 0, "unexpected null ID");
            groups[0]++;
            total[0] += count;
        });
        equal(2, groups[0], "group traversal count");
        equal((long) Integer.MAX_VALUE + 7L, total[0], "group traversal total");
        equal(2 * depth + 1, calls[0], "group traversal repeated predicates per occurrence");
        calls[0] = 0;
        equal(Integer.MAX_VALUE, earlier.count(2), "earlier view changed");
        equal(depth / 2, calls[0], "later carry mutated earlier predicate forest");
        M3FilteredSortedMultisets.View<Integer> rejected = view.filtered(value -> false);
        equal(0, rejected.count(2), "last atom rejection");
        equal(Integer.MAX_VALUE, view.count(2), "later append mutated its parent");
        check(view.space().size() == 3, "counted traversal admitted new domain values");
    }

    private static void orderedCallbacksAndFailures() {
        M3IndexSortedMultiset<Integer> backing = source(null);
        backing.add(1, 3);
        ArrayList<String> trace = new ArrayList<>();
        Predicate<Integer> a = value -> { trace.add("a"); return true; };
        Predicate<Integer> b = value -> { trace.add("b"); return true; };
        var earlier = M3FilteredSortedMultisets.filtered(backing, a).filtered(b);
        var complete = earlier.filtered(b).filtered(value -> { trace.add("c"); return true; });
        equal(3, complete.count(1), "ordered count");
        equal(List.of("a", "b", "b", "c"), trace, "ordered duplicate occurrences");
        trace.clear();
        equal(3, earlier.count(1), "earlier callback view");
        equal(List.of("a", "b"), trace, "later filters changed prior callback sequence");
        var rejected = earlier.filtered(value -> { trace.add("reject"); return false; })
                .filtered(value -> { throw new AssertionError("short circuit failed"); });
        trace.clear();
        equal(0, rejected.count(1), "rejected group");
        equal(List.of("a", "b", "reject"), trace, "rejection callback order");
        IllegalStateException marker = new IllegalStateException("exact callback sentinel");
        var throwing = earlier.filtered(value -> { trace.add("throw"); throw marker; })
                .filtered(value -> { throw new AssertionError("callback after failure"); });
        trace.clear();
        check(expect(IllegalStateException.class, () -> throwing.count(1)) == marker,
                "callback exception object replaced");
        equal(List.of("a", "b", "throw"), trace, "failure callback order");
        Iterator<Integer> iterator = throwing.iterator();
        check(expect(IllegalStateException.class, iterator::hasNext) == marker,
                "iterator replaced callback failure");
        check(expect(IllegalStateException.class, iterator::hasNext) != marker,
                "failed iterator reran predicate instead of rejecting reuse");
        expect(NullPointerException.class, () -> complete.filtered(null));
        equal(3, backing.count(1), "failed callbacks changed count");

        M3IndexSortedMultiset<Integer> changing = source(null);
        changing.add(2, 1);
        int[] later = {0};
        var mutatingReject = M3FilteredSortedMultisets.filtered(changing, value -> {
            changing.add(3, 1);
            return false;
        }).filtered(value -> { later[0]++; return true; });
        expect(ConcurrentModificationException.class, () -> mutatingReject.count(2));
        equal(0, later[0], "rejecting mutation ran later predicate");
        equal(1, changing.count(3), "completed callback effect was rolled back");
    }

    private static void rangesAndReadOnly(int depth) {
        M3IndexSortedMultiset<Integer> backing = source(null);
        for (int value = 0; value < 10; value++) backing.add(value, value + 1);
        var view = chain(backing, value -> (value & 1) == 0, depth, false);
        var range = view.headMultiset(8, false).tailMultiset(2, true)
                .subMultiset(2, true, 6, true).descendingMultiset()
                .filtered(value -> true);
        check(range.space() == backing.space(), "range changed domain identity");
        equal(List.of(6, 4, 2), values(range.elementSet()), "descending live range order");
        check(range.comparator().compare(6, 4) < 0, "descending comparator not retained");
        equal(7L, range.rankOf(4, false), "descending strict rank");
        equal(12L, range.rankOf(4, true), "descending inclusive rank");
        equal(6, range.elementAtOccurrence(0), "descending first ordinal");
        equal(4, range.elementAtOccurrence(7), "descending next group ordinal");
        backing.setCount(4, 9);
        equal(19L, range.longSize(), "range stopped being live");
        equal(9, range.remove(4, 2), "range previous count");
        equal(7, backing.count(4), "range removal did not use backing group");
        expect(IllegalArgumentException.class, () -> range.add(8, 1));
        equal(9, backing.count(8), "out-of-range mutation changed backing");
        equal(false, range.removeAll(List.of(3, 8)), "range removed hidden or external key");
        equal(new M3IndexSortedMultiset.Entry<>(6, 7), range.pollFirstEntry(), "range endpoint count");
        equal(0, backing.count(6), "poll did not remove complete group");
        range.clear();
        equal(0, backing.count(2), "range clear missed accepted group");
        equal(0, backing.count(4), "range clear missed second accepted group");
        equal(4, backing.count(3), "range clear removed hidden group");
        equal(9, backing.count(8), "range clear removed external group");

        int[] calls = {0};
        var readOnly = chain(backing, value -> { calls[0]++; return (value & 1) == 0; },
                depth, true);
        var descendant = readOnly.tailMultiset(2, true).headMultiset(8, false)
                .descendingMultiset().filtered(value -> true);
        check(descendant.readOnly(), "range/filter lost read-only state");
        check(descendant.space() == backing.space(), "read-only descendant copied domain");
        calls[0] = 0;
        expect(UnsupportedOperationException.class, () -> descendant.add(4, 0));
        expect(UnsupportedOperationException.class, () -> descendant.remove(4, 0));
        expect(UnsupportedOperationException.class, () -> descendant.setCount(4, 0));
        expect(UnsupportedOperationException.class, () -> descendant.setCount(4, 99, 0));
        expect(UnsupportedOperationException.class, () -> descendant.addAll(List.of()));
        expect(UnsupportedOperationException.class, () -> descendant.removeAll(List.of()));
        expect(UnsupportedOperationException.class, () -> descendant.retainAll(descendant));
        expect(UnsupportedOperationException.class, () -> descendant.removeIf(value -> false));
        expect(UnsupportedOperationException.class, () -> descendant.elementSet().removeAll(List.of()));
        expect(UnsupportedOperationException.class, () -> descendant.entrySet().removeAll(List.of()));
        expect(UnsupportedOperationException.class, descendant::pollFirstEntry);
        expect(UnsupportedOperationException.class, () -> readOnly.headMultiset(-1, false).clear());
        equal(0, calls[0], "read-only no-op write evaluated predicates");
        backing.setCount(4, 6);
        equal(6, descendant.count(4), "read-only range ceased to be live");
    }

    private static void iteratorGroupsAndEpoch(int depth) {
        M3IndexSortedMultiset<Integer> backing = source(null);
        backing.add(2, 3);
        backing.add(3, 4);
        backing.add(4, 2);
        int[] calls = {0};
        var view = chain(backing, value -> { calls[0]++; return (value & 1) == 0; },
                depth, false);
        Iterator<Integer> iterator = view.iterator();
        check(iterator.hasNext(), "first group missing");
        check(iterator.hasNext(), "repeated lookahead changed group");
        equal(depth, calls[0], "lookahead reran group predicates");
        for (int i = 0; i < 3; i++) equal(2, iterator.next(), "first occurrence group");
        equal(depth, calls[0], "predicate evaluated per occurrence");
        check(iterator.hasNext(), "second group missing");
        equal(2 * depth + 1, calls[0], "hidden-group short circuit changed");
        equal(4, iterator.next(), "second group first occurrence");
        equal(4, iterator.next(), "second group last occurrence");
        check(!iterator.hasNext(), "unexpected occurrence");
        equal(2 * depth + 1, calls[0], "end lookahead repeated predicates");
        expect(UnsupportedOperationException.class, iterator::remove);
        Iterator<Integer> stale = view.iterator();
        backing.add(3, 1);
        expect(ConcurrentModificationException.class, stale::hasNext);
        Spliterator<Integer> spliterator = view.spliterator();
        check(spliterator.hasCharacteristics(Spliterator.SORTED | Spliterator.ORDERED),
                "ordered/sorted characteristics lost");
        check(!spliterator.hasCharacteristics(Spliterator.SIZED)
                && !spliterator.hasCharacteristics(Spliterator.DISTINCT),
                "counted filtered spliterator advertised invalid characteristics");
        check(spliterator.trySplit() == null, "unexpected split payload");
        check(spliterator.getComparator() == view.comparator(), "spliterator comparator changed");
    }

    private record Key(int value) { }

    private static void representativesAndNull(int depth) {
        M3IndexSortedMultiset<Key> source = source(Comparator.comparingInt(Key::value));
        Key retained = new Key(2);
        Key query = new Key(2);
        source.add(retained, 2);
        source.add(query, 3);
        var view = chain(source, key -> (key.value() & 1) == 0, depth, false);
        equal(5, view.count(query), "comparator-equivalent query count");
        check(view.firstEntry().element() == retained, "entry replaced stored representative");
        check(view.iterator().next() == retained, "occurrence replaced stored representative");
        check(view.elementSet().first() == retained, "element set replaced stored representative");
        equal(5, view.remove(query, 1), "query removal previous count");
        check(view.firstEntry().element() == retained, "count mutation replaced representative");

        Comparator<Integer> comparator = Comparator.nullsFirst(Comparator.reverseOrder());
        M3IndexSortedMultiset<Integer> nullable = source(comparator);
        nullable.add(null, 2);
        nullable.add(0, 1);
        nullable.add(1, 4);
        nullable.add(2, 3);
        var selected = chain(nullable, value -> value == null || (value & 1) == 0,
                depth, false);
        equal(Arrays.asList(null, 2, 0), values(selected.elementSet()), "null/comparator groups");
        equal(Arrays.asList(0, 2, null), values(selected.descendingMultiset().elementSet()),
                "descending null/comparator groups");
        equal(2, selected.count(null), "null count");
        equal(2L, selected.rankOf(2, false), "null prefix rank");
        equal(2, selected.elementAtOccurrence(2), "null prefix ordinal");
    }

    private static void differential(int seeds) {
        for (int seed = 0; seed < seeds; seed++) {
            Random random = new Random(0x53A7B19DL + seed);
            M3IndexSortedMultiset<Integer> source = source(null);
            TreeMap<Integer, Integer> oracle = new TreeMap<>();
            var view = M3FilteredSortedMultisets.filtered(source, value -> true);
            ArrayList<int[]> restrictions = new ArrayList<>();
            for (int level = 0; level < 32 + seed % 32; level++) {
                if (level % 8 == 0) {
                    int modulus = 3 + random.nextInt(5);
                    int forbidden = random.nextInt(modulus);
                    restrictions.add(new int[] {modulus, forbidden});
                    view = view.filtered(value -> value % modulus != forbidden);
                } else {
                    view = view.filtered(value -> true);
                }
            }
            for (int operation = 0; operation < 24; operation++) {
                int key = random.nextInt(16), count = random.nextInt(9);
                source.setCount(key, count);
                if (count == 0) oracle.remove(key); else oracle.put(key, count);
                ArrayList<Integer> groups = new ArrayList<>();
                ArrayList<Integer> occurrences = new ArrayList<>();
                for (var entry : oracle.entrySet()) {
                    boolean accepted = true;
                    for (int[] rule : restrictions)
                        if (entry.getKey() % rule[0] == rule[1]) { accepted = false; break; }
                    if (accepted) {
                        groups.add(entry.getKey());
                        for (int i = 0; i < entry.getValue(); i++) occurrences.add(entry.getKey());
                    }
                }
                equal(groups, values(view.elementSet()), "differential distinct groups");
                equal(occurrences, values(view), "differential occurrence order");
                equal((long) occurrences.size(), view.longSize(), "differential total");
                equal(groups.size(), view.distinctSize(), "differential distinct count");
                for (int value = 0; value < 16; value++) {
                    int expected = groups.contains(value) ? oracle.get(value) : 0;
                    equal(expected, view.count(value), "differential count");
                }
                long below = 0;
                for (int value : occurrences) if (value < key) below++;
                equal(below, view.rankOf(key, false), "differential strict rank");
                if (!occurrences.isEmpty()) {
                    equal(occurrences.getFirst(), view.elementAtOccurrence(0), "differential first ordinal");
                    equal(occurrences.getLast(), view.elementAtOccurrence(occurrences.size() - 1L),
                            "differential last ordinal");
                }
            }
        }
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--baseline-overflow")) {
            int depth = Integer.parseInt(args[1]);
            M3IndexSortedMultiset<Integer> source = source(null);
            source.add(2, 1);
            var view = chain(source, value -> true, depth, false);
            try {
                view.count(2);
            } catch (StackOverflowError expected) {
                System.out.println("Original M3FilteredSortedMultisets overflow reproduced at depth=" + depth);
                return;
            }
            throw new AssertionError("original nested predicate chain did not overflow");
        }
        int depth = args.length == 0 ? 65536 : Integer.parseInt(args[0]);
        int seeds = args.length < 2 ? 64 : Integer.parseInt(args[1]);
        if (depth < 4 || (depth & 1) != 0 || seeds < 1) throw new IllegalArgumentException("proof bounds");
        deepCountsAndEarlierViews(depth);
        orderedCallbacksAndFailures();
        rangesAndReadOnly(depth);
        iteratorGroupsAndEpoch(depth);
        representativesAndNull(Math.min(depth, 128));
        differential(seeds);
        System.out.println("M3_DEEP_FILTER_SORTED_MULTISET_PASS checks=" + checks
                + " depth=" + depth + " seeds=" + seeds);
    }
}
