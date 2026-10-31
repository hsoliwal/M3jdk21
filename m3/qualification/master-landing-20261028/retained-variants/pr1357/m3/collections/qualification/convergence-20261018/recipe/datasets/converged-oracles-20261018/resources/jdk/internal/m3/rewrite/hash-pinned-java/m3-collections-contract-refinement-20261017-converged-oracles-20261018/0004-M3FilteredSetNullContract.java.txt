/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Independent finite oracle for the readonly set interior-null navigation atom.
 *
 * <p>The expected contents and navigation results come from separately populated
 * JDK sets. Membership masks, comparators, ranges, directions and predicates are
 * enumerated deterministically. The compatibility facade and a readonly filter
 * over the existing composition view are included without changing their owners.
 * Map navigation and mutable-view algorithms are outside this acceptance scope.
 * The broader rejected navigation oracle remains in canonical research evidence.</p>
 */
public final class M3FilteredSetNullContract {
    private enum Surface { READONLY, FACADE, OVER_COMPOSITION }

    private static final Integer[] VALUES = {null, -3, -1, 0, 1, 3};
    private static final Comparator<Integer> MIDDLE =
            Comparator.comparingInt(value -> value == null ? 2 : value);
    private static long checks;

    private M3FilteredSetNullContract() { }

    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void identical(Object expected, Object actual, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label + " changed reference identity");
    }

    private static void throwsSame(Throwable expected, Runnable action, String label) {
        checks++;
        try {
            action.run();
        } catch (Throwable actual) {
            if (actual == expected) return;
            throw new AssertionError(label + " changed throwable identity", actual);
        }
        throw new AssertionError(label + " swallowed " + expected.getClass().getSimpleName());
    }

    private static void throwsType(Class<? extends Throwable> type, Runnable action, String label) {
        checks++;
        try {
            action.run();
        } catch (Throwable actual) {
            if (type.isInstance(actual)) return;
            throw new AssertionError(label + " wrong throwable type", actual);
        }
        throw new AssertionError(label + " did not throw " + type.getSimpleName());
    }

    private static <E> NavigableSet<E> owner(Surface surface, NavigableSet<E> source) {
        return surface == Surface.OVER_COMPOSITION
                ? M3CollectionViews.filtered(source, value -> true) : source;
    }

    private static <E> NavigableSet<E> view(Surface surface, NavigableSet<E> source,
            Predicate<? super E> predicate) {
        NavigableSet<E> backing = owner(surface, source);
        return surface == Surface.FACADE ? M3FilteredNavigableSet.view(backing, predicate)
                : M3FilteredViews.filter(backing, predicate);
    }

    private static List<Comparator<Integer>> orders() {
        return Arrays.asList(null, Comparator.reverseOrder(),
                Comparator.nullsFirst(Comparator.naturalOrder()),
                Comparator.nullsLast(Comparator.naturalOrder()),
                Comparator.nullsFirst(Comparator.reverseOrder()),
                Comparator.nullsLast(Comparator.reverseOrder()), MIDDLE, MIDDLE.reversed());
    }

    private static List<Integer> probes(boolean nullable) {
        ArrayList<Integer> values = new ArrayList<>(List.of(-9, -3, -2, -1, 0, 1, 2, 3, 9));
        if (nullable) values.add(null);
        return values;
    }

    private static Predicate<Integer> predicate(int kind) {
        return switch (kind) {
            case 0 -> value -> true;
            case 1 -> Objects::nonNull;
            case 2 -> value -> value == null || (value & 1) == 0;
            case 3 -> value -> value != null && value > 0;
            case 4 -> value -> false;
            default -> throw new IllegalArgumentException("predicate kind");
        };
    }

    private static int compare(Comparator<? super Integer> order, int left, int right) {
        return order == null ? Integer.compare(left, right) : order.compare(left, right);
    }

    private static NavigableSet<Integer> range(NavigableSet<Integer> source, int kind) {
        boolean ascending = compare(source.comparator(), -2, 2) < 0;
        int from = ascending ? -2 : 2;
        int to = ascending ? 2 : -2;
        return switch (kind) {
            case 0 -> source;
            case 1 -> source.subSet(from, false, to, true);
            case 2 -> source.subSet(from, true, to, false);
            case 3 -> source.subSet(from, true, to, true).headSet(0, true).tailSet(from, false);
            case 4 -> source.headSet(0, false);
            case 5 -> source.tailSet(0, true);
            case 6 -> source.subSet(0, false, 0, false);
            case 7 -> source.subSet(0, true, 0, true);
            default -> throw new IllegalArgumentException("range kind");
        };
    }

    private static <E> E navigate(NavigableSet<E> set, E key, int relation) {
        return switch (relation) {
            case 0 -> set.lower(key);
            case 1 -> set.floor(key);
            case 2 -> set.ceiling(key);
            case 3 -> set.higher(key);
            default -> throw new IllegalArgumentException("navigation relation");
        };
    }

    private static void checkSet(NavigableSet<Integer> source, NavigableSet<Integer> actual,
            Predicate<Integer> predicate, boolean nullable, String label) {
        NavigableSet<Integer> expected = new TreeSet<>(source.comparator());
        for (Integer stored : source) if (predicate.test(stored)) expected.add(stored);
        identical(source.comparator(), actual.comparator(), label + " comparator");
        equal(new ArrayList<>(expected), new ArrayList<>(actual), label + " values");
        equal(new ArrayList<>(expected.descendingSet()),
                new ArrayList<>(actual.descendingSet()), label + " descending values");
        equal(expected.size(), actual.size(), label + " size");
        equal(expected.isEmpty(), actual.isEmpty(), label + " empty");
        for (Integer probe : probes(nullable)) {
            equal(expected.contains(probe), actual.contains(probe), label + " membership " + probe);
            for (int relation = 0; relation < 4; relation++) {
                equal(navigate(expected, probe, relation), navigate(actual, probe, relation),
                        label + " relation=" + relation + " probe=" + probe);
            }
        }
        if (expected.isEmpty()) {
            throwsType(NoSuchElementException.class, actual::first, label + " empty first");
            throwsType(NoSuchElementException.class, actual::last, label + " empty last");
        } else {
            equal(expected.first(), actual.first(), label + " first");
            equal(expected.last(), actual.last(), label + " last");
        }
    }

    private static void setMatrix() {
        List<Comparator<Integer>> orders = orders();
        for (Surface surface : Surface.values()) {
            for (int orderIndex = 0; orderIndex < orders.size(); orderIndex++) {
                boolean nullable = orderIndex >= 2;
                for (int membership = 0; membership < (1 << VALUES.length); membership++) {
                    if (!nullable && (membership & 1) != 0) continue;
                    NavigableSet<Integer> root = new TreeSet<>(orders.get(orderIndex));
                    for (int index = 0; index < VALUES.length; index++) {
                        if ((membership & (1 << index)) != 0) root.add(VALUES[index]);
                    }
                    List<Integer> unchanged = new ArrayList<>(root);
                    for (int bounds = 0; bounds < 8; bounds++) {
                        NavigableSet<Integer> bounded = range(root, bounds);
                        for (int direction = 0; direction < 2; direction++) {
                            NavigableSet<Integer> source = direction == 0
                                    ? bounded : bounded.descendingSet();
                            for (int selection = 0; selection < 5; selection++) {
                                Predicate<Integer> predicate = predicate(selection);
                                String label = surface + "/" + orderIndex + "/" + membership
                                        + "/" + bounds + "/" + direction + "/" + selection;
                                checkSet(source, view(surface, source, predicate), predicate, nullable, label);
                            }
                        }
                    }
                    equal(unchanged, new ArrayList<>(root), surface + " owner unchanged");
                }
            }
        }
    }

    private static void nestedAndLiveViews() {
        for (Surface surface : Surface.values()) {
            for (Comparator<Integer> order : orders().subList(2, orders().size())) {
                NavigableSet<Integer> root = new TreeSet<>(order);
                root.addAll(Arrays.asList(-3, -1, null, 1, 3));
                for (int bounds = 0; bounds < 8; bounds++) {
                    NavigableSet<Integer> source = range(root, bounds);
                    for (int direction = 0; direction < 2; direction++) {
                        NavigableSet<Integer> ordered = direction == 0
                                ? source : source.descendingSet();
                        Predicate<Integer> first = value -> value == null || value >= -1;
                        Predicate<Integer> second = value -> value == null || value <= 1;
                        NavigableSet<Integer> nested = view(surface, view(surface, ordered, first), second);
                        checkSet(ordered, nested, first.and(second), true, "nested " + surface);
                    }
                }
                NavigableSet<Integer> source = range(root, 1).descendingSet();
                Predicate<Integer> selection = Objects::nonNull;
                NavigableSet<Integer> selected = view(surface, source, selection);
                checkSet(source, selected, selection, true, "live before");
                root.remove(null);
                root.add(2);
                checkSet(source, selected, selection, true, "live null replaced by equivalent value");
                root.remove(-1);
                root.add(0);
                checkSet(source, selected, selection, true, "live owner change");
                root.clear();
                checkSet(source, selected, selection, true, "live cleared bounded owner");
            }
        }
    }

    private static final class ObservedSet extends TreeSet<Integer> {
        private static final long serialVersionUID = 1L;
        private final ArrayList<String> observations = new ArrayList<>();

        ObservedSet(Comparator<Integer> order, List<Integer> values) {
            super(order);
            addAll(values);
        }

        @Override public boolean isEmpty() { observations.add("isEmpty"); return super.isEmpty(); }
        @Override public Integer first() { observations.add("first"); return super.first(); }
        @Override public Integer last() { observations.add("last"); return super.last(); }
    }

    private static void historicalExtremeProbes() {
        Comparator<Integer> rejectNull = (left, right) -> {
            if (left == null || right == null) throw new AssertionError("fabricated null comparison");
            return Integer.compare(left, right);
        };
        for (Surface surface : List.of(Surface.READONLY, Surface.FACADE)) {
            ObservedSet firstNull = new ObservedSet(Comparator.nullsFirst(Comparator.naturalOrder()),
                    Arrays.asList(null, 1, 3));
            equal(null, view(surface, firstNull, value -> true).ceiling(9), "first-null absence");
            equal(List.of("isEmpty", "first"), firstNull.observations, "first-null probe order");
            ObservedSet lastNull = new ObservedSet(Comparator.nullsLast(Comparator.naturalOrder()),
                    Arrays.asList(1, 3, null));
            equal(null, view(surface, lastNull, value -> true).floor(-9), "last-null absence");
            equal(List.of("isEmpty", "first", "last"), lastNull.observations, "last-null probe order");
            ObservedSet middleNull = new ObservedSet(MIDDLE, Arrays.asList(-1, null, 3));
            equal(-1, view(surface, middleNull, Objects::nonNull).floor(2), "interior-null continuation");
            equal(List.of("isEmpty", "first", "last"), middleNull.observations,
                    "interior-null retains extreme probe order");
            ObservedSet nonNull = new ObservedSet(rejectNull, List.of(1, 3));
            equal(null, view(surface, nonNull, value -> true).higher(9), "null-rejecting absence");
            equal(List.of("isEmpty", "first", "last"), nonNull.observations,
                    "nonnull absence retains extreme probe order");
            ObservedSet empty = new ObservedSet(rejectNull, List.of());
            equal(null, view(surface, empty, value -> true).lower(0), "empty source");
            equal(List.of("isEmpty"), empty.observations, "empty guard short-circuits extrema");
        }
    }

    private static void predicateFailures() {
        for (Surface surface : List.of(Surface.READONLY, Surface.FACADE)) {
            for (int direction = 0; direction < 2; direction++) {
                NavigableSet<Integer> root = new TreeSet<>(List.of(1, 3, 5));
                List<Integer> seen = new ArrayList<>();
                IllegalArgumentException failure = new IllegalArgumentException("predicate");
                Predicate<Integer> selection = key -> {
                    seen.add(key);
                    if (key == 3) throw failure;
                    return false;
                };
                NavigableSet<Integer> selected = view(surface, root.subSet(1, true, 5, true), selection);
                int relation = direction == 0 ? 2 : 1;
                int probe = direction == 0 ? -9 : 9;
                List<Integer> expected = direction == 0 ? List.of(1, 3) : List.of(5, 3);
                throwsSame(failure, () -> navigate(selected, probe, relation), "predicate identity");
                equal(expected, seen, "predicate directional encounter order");
                seen.clear();
                NavigableSet<Integer> nested = view(surface, selected,
                        key -> { throw new AssertionError("outer predicate after inner failure"); });
                throwsSame(failure, () -> navigate(nested, probe, relation), "nested predicate identity");
                equal(expected, seen, "nested predicate directional encounter order");
            }
            NavigableSet<Integer> source = new TreeSet<>(List.of(1, 3, 5));
            List<Integer> seen = new ArrayList<>();
            NavigableSet<Integer> inner = view(surface, source, key -> {
                seen.add(key);
                throw new AssertionError("predicate outside natural strict query range");
            });
            NavigableSet<Integer> outer = view(surface, inner,
                    key -> { throw new AssertionError("outer predicate without a candidate"); });
            equal(null, outer.lower(0), "nested natural lower outside range");
            equal(null, outer.lower(1), "nested natural lower excludes query key");
            equal(List.of(), seen, "no new natural-order empty-domain predicate probes");

            IllegalArgumentException nullFailure = new IllegalArgumentException("retained null predicate");
            NavigableSet<Integer> nullable = new TreeSet<>(MIDDLE);
            nullable.addAll(Arrays.asList(-1, null, 3));
            NavigableSet<Integer> selected = view(surface, nullable, value -> {
                if (value == null) throw nullFailure;
                return true;
            });
            throwsSame(nullFailure, () -> selected.floor(2), "interior-null predicate identity");
            throwsSame(nullFailure, () -> selected.ceiling(2), "interior-null predicate identity forward");
        }
    }

    private static final class FaultOrder implements Comparator<Integer> {
        private final IllegalArgumentException failure;
        private int comparisons;
        private int failAt;

        FaultOrder(IllegalArgumentException failure) { this.failure = failure; }
        void arm(int target) { comparisons = 0; failAt = target; }

        @Override public int compare(Integer left, Integer right) {
            if (++comparisons == failAt) throw failure;
            return MIDDLE.compare(left, right);
        }
    }

    private record FaultFixture(FaultOrder order, NavigableSet<Integer> selected) { }

    private static FaultFixture faultFixture(Surface surface, IllegalArgumentException failure) {
        FaultOrder order = new FaultOrder(failure);
        NavigableSet<Integer> source = new TreeSet<>(order);
        source.addAll(Arrays.asList(-1, null, 3));
        NavigableSet<Integer> selected = view(surface, source, Objects::nonNull);
        order.arm(0);
        return new FaultFixture(order, selected);
    }

    private static void comparatorFailures() {
        for (Surface surface : List.of(Surface.READONLY, Surface.FACADE)) {
            for (Integer probe : probes(true)) {
                for (int relation = 0; relation < 4; relation++) {
                    IllegalArgumentException failure = new IllegalArgumentException("comparator");
                    FaultFixture control = faultFixture(surface, failure);
                    NavigableSet<Integer> expected = new TreeSet<>(MIDDLE);
                    expected.addAll(List.of(-1, 3));
                    equal(navigate(expected, probe, relation), navigate(control.selected(), probe, relation),
                            "fault control independent result");
                    int comparisons = control.order().comparisons;
                    int selectedRelation = relation;
                    for (int call = 1; call <= comparisons; call++) {
                        FaultFixture fault = faultFixture(surface, failure);
                        fault.order().arm(call);
                        throwsSame(failure, () -> navigate(fault.selected(), probe, selectedRelation),
                                "comparator failure at consumed call " + call);
                    }
                }
            }
        }
    }

    private static final class PoisonedSet extends TreeSet<Integer> {
        private static final long serialVersionUID = 1L;
        private final IllegalArgumentException failure;
        PoisonedSet(Comparator<? super Integer> order, IllegalArgumentException failure) {
            super(order);
            this.failure = failure;
        }
        @Override public Iterator<Integer> iterator() { throw failure; }
        @Override public Iterator<Integer> descendingIterator() { throw failure; }
    }

    private static final class FaultingRangeSet extends TreeSet<Integer> {
        private static final long serialVersionUID = 1L;
        private final IllegalArgumentException failure;
        FaultingRangeSet(IllegalArgumentException failure) {
            super(MIDDLE);
            this.failure = failure;
            addAll(Arrays.asList(-1, null, 3));
        }
        @Override public NavigableSet<Integer> headSet(Integer to, boolean inclusive) {
            super.headSet(to, inclusive);
            return new PoisonedSet(comparator(), failure);
        }
        @Override public NavigableSet<Integer> tailSet(Integer from, boolean inclusive) {
            super.tailSet(from, inclusive);
            return new PoisonedSet(comparator(), failure);
        }
    }

    private static void continuationIteratorFailures() {
        for (Surface surface : List.of(Surface.READONLY, Surface.FACADE)) {
            IllegalArgumentException failure = new IllegalArgumentException("continuation iterator");
            NavigableSet<Integer> selected = view(surface, new FaultingRangeSet(failure), Objects::nonNull);
            throwsSame(failure, () -> selected.floor(2), "backward interior-null iterator identity");
            throwsSame(failure, () -> selected.ceiling(2), "forward interior-null iterator identity");
        }
    }

    private record Item(int rank, String label) { }

    private static void representativesAndMutationRefusal() {
        for (Surface surface : Surface.values()) {
            Comparator<Item> order = Comparator.comparingInt(Item::rank);
            Item retained = new Item(2, "retained");
            Item alias = new Item(2, "alias");
            Item hidden = new Item(1, "hidden");
            NavigableSet<Item> source = new TreeSet<>(order);
            source.addAll(List.of(hidden, retained));
            List<Item> seen = new ArrayList<>();
            NavigableSet<Item> selected = view(surface, source, item -> {
                seen.add(item);
                return item == retained;
            });
            for (int relation : new int[] {1, 2}) {
                seen.clear();
                identical(retained, navigate(selected, alias, relation), "retained representative");
                equal(List.of(retained), seen, "stored candidate tested once");
            }
            for (boolean empty : List.of(false, true)) {
                NavigableSet<Integer> root = new TreeSet<>(MIDDLE);
                if (!empty) root.addAll(Arrays.asList(-1, null, 3));
                NavigableSet<Integer> readonly = view(surface, root, value -> true);
                List<Integer> before = new ArrayList<>(root);
                throwsType(UnsupportedOperationException.class, () -> readonly.add(9), "add refuses");
                throwsType(UnsupportedOperationException.class, () -> readonly.remove(9), "absent remove refuses");
                throwsType(UnsupportedOperationException.class, () -> readonly.addAll(List.of()), "empty addAll refuses");
                throwsType(UnsupportedOperationException.class, () -> readonly.removeAll(List.of()), "empty removeAll refuses");
                throwsType(UnsupportedOperationException.class, () -> readonly.retainAll(List.of()), "retainAll refuses");
                throwsType(UnsupportedOperationException.class, () -> readonly.removeIf(value -> false), "removeIf refuses");
                throwsType(UnsupportedOperationException.class, readonly::clear, "clear refuses");
                throwsType(UnsupportedOperationException.class, readonly::pollFirst, "pollFirst refuses");
                throwsType(UnsupportedOperationException.class, readonly::pollLast, "pollLast refuses");
                throwsType(UnsupportedOperationException.class, () -> readonly.iterator().remove(), "iterator removal refuses");
                throwsType(UnsupportedOperationException.class, () -> readonly.descendingIterator().remove(),
                        "descending iterator removal refuses");
                throwsType(UnsupportedOperationException.class, () -> readonly.descendingSet().clear(),
                        "descending clear refuses");
                throwsType(UnsupportedOperationException.class, () -> readonly.headSet(3, true).clear(),
                        "range clear refuses");
                equal(before, new ArrayList<>(root), "mutation refusals preserve owner");
            }
        }
    }

    private record Outcome(Object value, Class<? extends Throwable> failure) { }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Outcome rawOutcome(NavigableSet source, Object query, int relation) {
        try {
            return new Outcome(navigate(source, query, relation), null);
        } catch (Throwable failure) {
            return new Outcome(null, failure.getClass());
        }
    }

    private static void unchangedEmptyAdmission() {
        List<Comparator<Integer>> orders = Arrays.asList(null, Comparator.naturalOrder(),
                Comparator.nullsFirst(Comparator.naturalOrder()), MIDDLE);
        for (Surface surface : Surface.values()) {
            for (Comparator<Integer> order : orders) {
                NavigableSet<Integer> root = new TreeSet<>(order);
                List<NavigableSet<Integer>> ranges = List.of(root, root.headSet(10, false),
                        root.tailSet(-10, true), root.subSet(-10, true, 10, false));
                for (NavigableSet<Integer> bounded : ranges) {
                    for (int direction = 0; direction < 2; direction++) {
                        NavigableSet<Integer> source = direction == 0 ? bounded : bounded.descendingSet();
                        NavigableSet<Integer> admissionOwner = owner(surface, source);
                        NavigableSet<Integer> selected = view(surface, source,
                                value -> { throw new AssertionError("predicate on an empty owner"); });
                        for (Object query : Arrays.asList(null, new Object(), "wrong-comparable", -99, 0, 99)) {
                            for (int relation = 0; relation < 4; relation++) {
                                equal(rawOutcome(admissionOwner, query, relation),
                                        rawOutcome(selected, query, relation), "unchanged empty admission");
                            }
                        }
                    }
                }
            }
        }
    }

    private static void baselineInteriorNull() {
        NavigableSet<Integer> source = new TreeSet<>(MIDDLE);
        source.addAll(Arrays.asList(-1, null, 3));
        equal(-1, M3FilteredViews.filter(source, Objects::nonNull).floor(2),
                "interior-null candidate must continue to retained predecessor");
    }

    public static void runAll() {
        baselineInteriorNull();
        setMatrix();
        nestedAndLiveViews();
        historicalExtremeProbes();
        predicateFailures();
        comparatorFailures();
        continuationIteratorFailures();
        representativesAndMutationRefusal();
        unchangedEmptyAdmission();
    }

    public static void main(String[] args) {
        String mode = args.length == 0 ? "--all" : args[0];
        switch (mode) {
            case "--all" -> runAll();
            case "--baseline-interior-null" -> baselineInteriorNull();
            default -> throw new IllegalArgumentException("--all or --baseline-interior-null required");
        }
        System.out.println("PASS readonly-set-null mode=" + mode + " checks=" + checks);
    }
}
