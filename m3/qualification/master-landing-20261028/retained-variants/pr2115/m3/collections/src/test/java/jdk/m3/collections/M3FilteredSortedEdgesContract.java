/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026: reconcile comparator-entry acceptance with the qualified stored-representative contract.
 */
package jdk.m3.collections;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Independent navigation oracles and frozen entry callback compatibility checks. */
public final class M3FilteredSortedEdgesContract {
    private static long checks;

    private M3FilteredSortedEdgesContract() { }

    private static void equal(Object expected, Object actual, String message) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void require(boolean accepted, String message) {
        equal(true, accepted, message);
    }

    /** A minimized witness: null is retained between -1 and 1 and is filtered out. */
    public static void navigationWitness() {
        Comparator<Integer> order = Comparator.comparingInt(value -> value == null ? 0 : value);
        NavigableSet<Integer> source = new TreeSet<>(order);
        source.addAll(Arrays.asList(-2, -1, null, 1, 2));
        NavigableSet<Integer> view = M3FilteredViews.filter(source, Objects::nonNull);
        equal(-1, view.lower(1), "interior-null lower witness");
        equal(-1, view.floor(0), "interior-null floor witness");
        equal(1, view.ceiling(0), "interior-null ceiling witness");
        equal(1, view.higher(-1), "interior-null higher witness");
    }

    /**
     * Minimized comparator-alias witness admitted by the stored-representative successor.
     * Entry membership uses the owner's comparator and tests only its matched representative.
     */
    public static void entryWitness() {
        NavigableMap<String, String> source = new TreeMap<>(Comparator.comparingInt(String::length));
        source.put("aa", "value");
        Map.Entry<String, String> alias = Map.entry("zz", "value");
        require(source.entrySet().contains(alias), "backing TreeMap comparator membership");
        NavigableMap<String, String> view = M3FilteredViews.filterEntries(source,
                (key, value) -> key.startsWith("a"));
        require(view.containsKey("zz"), "stored representative admitted");
        require(view.entrySet().contains(alias), "comparator-equivalent entry witness");
    }

    private static List<Predicate<Integer>> predicates() {
        return List.of(value -> true, value -> false, Objects::nonNull,
                Objects::isNull, value -> value == null || (value & 1) == 0,
                value -> value != null && (value & 1) != 0);
    }

    private static List<Integer> probes() {
        List<Integer> values = new ArrayList<>();
        for (int value = -6; value <= 6; value++) values.add(value);
        values.add(null);
        return values;
    }

    private static List<NavigableSet<Integer>> sourceViews(NavigableSet<Integer> source) {
        List<Integer> ordered = new ArrayList<>(source);
        Integer low = ordered.get(1);
        Integer high = ordered.get(ordered.size() - 2);
        NavigableSet<Integer> range = source.subSet(low, true, high, true);
        return List.of(source, source.descendingSet(), range, range.descendingSet(),
                range.tailSet(low, false).headSet(high, false),
                source.headSet(high, false), source.tailSet(low, false),
                source.subSet(low, false, low, false));
    }

    private static void checkNavigation(NavigableSet<Integer> expected,
            NavigableSet<Integer> actual, List<Integer> queries, String label) {
        equal(new ArrayList<>(expected), new ArrayList<>(actual), label + " iteration");
        equal(expected.isEmpty(), actual.isEmpty(), label + " empty");
        for (Integer query : queries) {
            equal(expected.lower(query), actual.lower(query), label + " lower " + query);
            equal(expected.floor(query), actual.floor(query), label + " floor " + query);
            equal(expected.ceiling(query), actual.ceiling(query), label + " ceiling " + query);
            equal(expected.higher(query), actual.higher(query), label + " higher " + query);
            equal(expected.contains(query), actual.contains(query), label + " contains " + query);
        }
    }

    private static void checkSetViews(NavigableSet<Integer> source,
            Predicate<Integer> visible, List<Integer> queries) {
        NavigableSet<Integer> expected = new TreeSet<>(source.comparator());
        for (Integer value : source) if (visible.test(value)) expected.add(value);
        NavigableSet<Integer> readonly = M3FilteredViews.filter(source, visible);
        checkNavigation(expected, readonly, queries, "readonly");
        checkNavigation(expected.descendingSet(), readonly.descendingSet(), queries, "reverse");
        checkNavigation(expected, M3FilteredNavigableSet.view(source, visible), queries, "facade");
        checkNavigation(expected, M3MutableFilteredViews.filter(source, visible), queries, "mutable");
        checkNavigation(expected, M3CollectionViews.filtered(source, visible), queries, "collection");
        NavigableMap<Integer, String> map = new TreeMap<>(source.comparator());
        for (Integer value : source) map.put(value, value == null ? null : "v" + value);
        NavigableMap<Integer, String> filteredMap = M3FilteredViews.filterEntries(map,
                (key, value) -> visible.test(key));
        checkNavigation(expected, filteredMap.navigableKeySet(), queries, "map keys");
        checkNavigation(expected.descendingSet(), filteredMap.descendingKeySet(), queries, "map reverse keys");
    }

    /** Exhausts a bounded matrix of interior ranks, direction, null presence, predicates and views. */
    public static void navigationMatrix() {
        List<Integer> queries = probes();
        for (int nullSlot = -4; nullSlot <= 3; nullSlot++) {
            int rank = 2 * nullSlot + 1;
            Comparator<Integer> ascending = Comparator.comparingInt(value -> value == null ? rank : 2 * value);
            for (Comparator<Integer> order : List.of(ascending, ascending.reversed())) {
                for (boolean withNull : List.of(false, true)) {
                    NavigableSet<Integer> source = new TreeSet<>(order);
                    for (int value = -4; value <= 4; value++) source.add(value);
                    if (withNull) source.add(null);
                    for (NavigableSet<Integer> range : sourceViews(source)) {
                        for (Predicate<Integer> visible : predicates()) checkSetViews(range, visible, queries);
                    }
                }
            }
        }
    }

    /** The view remains live through null insertion/removal and ranges created after filtering. */
    public static void liveRanges() {
        Comparator<Integer> order = Comparator.comparingInt(value -> value == null ? 0 : value);
        NavigableSet<Integer> source = new TreeSet<>(order);
        source.addAll(List.of(-3, -2, -1, 1, 2, 3));
        NavigableSet<Integer> readonly = M3FilteredViews.filter(source, Objects::nonNull);
        NavigableSet<Integer> nested = readonly.subSet(-3, false, 3, false)
                .descendingSet().subSet(2, true, -2, true);
        NavigableMap<Integer, String> map = new TreeMap<>(order);
        source.forEach(key -> map.put(key, "v" + key));
        NavigableMap<Integer, String> mapView = M3FilteredViews.filterEntries(map,
                (key, value) -> key != null).subMap(-3, false, 3, false).descendingMap();
        for (int pass = 0; pass < 3; pass++) {
            if (pass == 1) { source.add(null); map.put(null, null); }
            if (pass == 2) { source.remove(null); map.remove(null); source.remove(-1); map.remove(-1); }
            NavigableSet<Integer> expected = new TreeSet<>(order);
            for (Integer key : source.subSet(-2, true, 2, true)) if (key != null) expected.add(key);
            checkNavigation(expected.descendingSet(), nested, probes(), "live nested");
            checkNavigation(expected.descendingSet(), mapView.navigableKeySet(), probes(), "live map range");
        }
    }

    private static Object outcome(Supplier<?> operation) {
        try {
            return operation.get();
        } catch (RuntimeException failure) {
            return failure.getClass();
        }
    }

    /** Linear independent oracle: owner admission, then the comparator-matched stored entry. */
    private static boolean expectedEntryMembership(SortedMap<String, String> source,
            BiPredicate<String, String> visible, Object candidate) {
        if (!(candidate instanceof Map.Entry<?, ?> probe)) return false;
        Object query = probe.getKey();
        // Preserve the source's null/type/comparator exception contract even when every
        // stored mapping is filtered out; an empty copied map has a different admission path.
        if (!source.containsKey(query)) return false;
        String key = (String) query;
        Comparator<? super String> order = source.comparator();
        for (Map.Entry<String, String> stored : source.entrySet()) {
            boolean sameKey = order == null ? Objects.equals(key, stored.getKey())
                    : order.compare(key, stored.getKey()) == 0;
            if (sameKey) {
                return visible.test(stored.getKey(), stored.getValue())
                        && Objects.equals(stored.getValue(), probe.getValue());
            }
        }
        throw new AssertionError("owner admitted a key without a stored representative");
    }

    private static void checkEntries(SortedMap<String, String> source,
            BiPredicate<String, String> visible) {
        SortedMap<String, String> expected = new TreeMap<>(source.comparator());
        source.forEach((key, value) -> { if (visible.test(key, value)) expected.put(key, value); });
        SortedMap<String, String> actual = source instanceof NavigableMap<String, String> navigable
                ? M3FilteredViews.filterEntries(navigable, visible)
                : M3FilteredViews.filterEntries(source, visible);
        List<String> keys = Arrays.asList(null, "a", "A", "z", "bb", "BB", "zz", "aaa",
                "AAA", "zzz", "bbbb", "BBBB", "zzzz", "aaaaa", "AAAAA", "zzzzz", "missing");
        for (String key : keys) {
            for (String value : Arrays.asList(null, "v1", "v2", "v4", "v5", "absent")) {
                Map.Entry<String, String> probe = new SimpleImmutableEntry<>(key, value);
                equal(outcome(() -> expectedEntryMembership(source, visible, probe)),
                        outcome(() -> actual.entrySet().contains(probe)), "entry membership " + probe);
            }
        }
        equal(false, actual.entrySet().contains(null), "null is not an entry");
        equal(false, actual.entrySet().contains("a"), "non-entry membership");
        Map.Entry<Integer, String> incompatible = new SimpleImmutableEntry<>(42, "v1");
        equal(outcome(() -> expectedEntryMembership(source, visible, incompatible)),
                outcome(() -> actual.entrySet().contains(incompatible)),
                "incompatible entry key preserves owner admission");
        equal(new ArrayList<>(expected.entrySet()), new ArrayList<>(actual.entrySet()), "entry projection");
    }

    /** Comparator membership, owner admission, stored representatives and SortedMap facades. */
    public static void membershipMatrix() {
        entryWitness();
        Comparator<String> length = Comparator.comparingInt(String::length);
        Comparator<String> interior = Comparator.comparingInt(key -> key == null ? 5 : key.length() * 2);
        List<Comparator<String>> comparators = List.of(length, String.CASE_INSENSITIVE_ORDER,
                Comparator.nullsFirst(length), Comparator.nullsLast(length), interior);
        List<BiPredicate<String, String>> filters = List.of((key, value) -> true,
                (key, value) -> false, (key, value) -> key == null || key.startsWith("a"),
                (key, value) -> key != null && value == null);
        for (int index = 0; index < comparators.size(); index++) {
            NavigableMap<String, String> source = new TreeMap<>(comparators.get(index));
            source.put("a", "v1"); source.put("bb", "v2"); source.put("aaa", null);
            source.put("bbbb", "v4"); source.put("aaaaa", "v5");
            if (index >= 2) source.put(null, null);
            List<String> ordered = new ArrayList<>(source.keySet());
            NavigableMap<String, String> range = source.subMap(ordered.get(1), true,
                    ordered.get(ordered.size() - 2), true);
            for (SortedMap<String, String> view : List.of(source, source.descendingMap(), range,
                    range.descendingMap(), Collections.unmodifiableSortedMap(source))) {
                for (BiPredicate<String, String> filter : filters) checkEntries(view, filter);
            }
        }
    }

    /** User callbacks see the owner representative; their exceptions are not range recovery. */
    public static void callbacksAndExceptions() {
        String stored = new String("aa");
        NavigableMap<String, String> source = new TreeMap<>(Comparator.comparingInt(String::length));
        source.put(stored, null);
        int[] calls = {0};
        NavigableMap<String, String> view = M3FilteredViews.filterEntries(source, (key, value) -> {
            require(key == stored, "predicate receives stored key identity");
            equal(null, value, "predicate receives stored value");
            calls[0]++;
            return true;
        });
        equal(true, view.entrySet().contains(new SimpleImmutableEntry<>("zz", null)),
                "comparator alias uses the stored nullable mapping");
        equal(1, calls[0], "one visibility evaluation for entry membership");
        IllegalArgumentException failure = new IllegalArgumentException("predicate failure");
        NavigableMap<String, String> raising = M3FilteredViews.filterEntries(source,
                (key, value) -> { throw failure; });
        try {
            raising.entrySet().contains(new SimpleImmutableEntry<>("zz", null));
            throw new AssertionError("entry callback exception missing");
        } catch (IllegalArgumentException observed) {
            require(observed == failure, "entry callback exception identity");
        }
        Comparator<Integer> order = Comparator.comparingInt(value -> value == null ? 0 : value);
        NavigableSet<Integer> keys = new TreeSet<>(order);
        keys.addAll(Arrays.asList(-1, null, 1));
        NavigableSet<Integer> throwing = M3FilteredViews.filter(keys, key -> {
            if (key == null) throw failure;
            return true;
        });
        try {
            throwing.ceiling(0);
            throw new AssertionError("null callback exception missing");
        } catch (IllegalArgumentException observed) {
            require(observed == failure, "navigation callback exception identity");
        }
        equal(UnsupportedOperationException.class, outcome(() -> view.entrySet().remove(Map.entry("zz", "v"))),
                "read-only entry removal");
        equal(UnsupportedOperationException.class, outcome(() -> view.firstEntry().setValue("changed")),
                "read-only entry projection");
        equal(null, source.get(stored), "source untouched by rejected mutation");
    }

    /** Records the public unmodifiable map's exact predicate and query-entry access trace. */
    public static void entryCompatibilityTrace() {
        List<String> trace = new ArrayList<>();
        int[] comparisons = {0};
        Comparator<String> comparator = (left, right) -> {
            comparisons[0]++;
            return Integer.compare(left.length(), right.length());
        };
        NavigableMap<String, String> source = new TreeMap<>(comparator);
        source.put("a", "v1"); source.put("bb", "v2"); source.put("aaa", "v3");
        NavigableMap<String, String> view = M3FilteredViews.filterEntries(source, (key, value) -> {
            trace.add("predicate:" + key);
            return !key.equals("bb");
        });
        for (String query : Arrays.asList("aaa", "zzz", "zz", "xxxx", null)) {
            trace.clear();
            comparisons[0] = 0;
            Map.Entry<String, String> probe = new SimpleImmutableEntry<>(query, "v3") {
                private static final long serialVersionUID = 1L;
                @Override public String getKey() { trace.add("query-key"); return super.getKey(); }
                @Override public String getValue() { trace.add("query-value"); return super.getValue(); }
                @Override public boolean equals(Object other) {
                    trace.add("equals:" + ((Map.Entry<?, ?>) other).getKey());
                    return super.equals(other);
                }
            };
            boolean admitted = Objects.equals(query, "aaa") || Objects.equals(query, "zzz");
            equal(query == null ? NullPointerException.class : admitted,
                    outcome(() -> view.entrySet().contains(probe)), "entry callback result " + query);
            List<String> expectedTrace = new ArrayList<>(List.of("query-key"));
            if (admitted) expectedTrace.addAll(List.of("predicate:aaa", "query-value"));
            else if (Objects.equals(query, "zz")) expectedTrace.add("predicate:bb");
            equal(expectedTrace, trace, "stored representative and query accessor sequence " + query);
            int expectedComparisons = query == null ? 1 : admitted ? 6 : query.equals("zz") ? 3 : 2;
            equal(expectedComparisons, comparisons[0], "bounded owner comparator demand " + query);
            System.out.println("ENTRY_TRACE query=" + query + " events=" + trace);
        }
        AssertionError valueFailure = new AssertionError("stored value equality sentinel");
        Object effectfulValue = new Object() {
            @Override public boolean equals(Object other) {
                throw valueFailure;
            }
            @Override public int hashCode() { return 0; }
        };
        NavigableMap<String, Object> values = new TreeMap<>(Comparator.comparingInt(String::length));
        values.put("aa", effectfulValue);
        NavigableMap<String, Object> selected = M3FilteredViews.filterEntries(values, (key, value) -> true);
        boolean observedFailure = false;
        try { selected.entrySet().contains(new SimpleImmutableEntry<>("zz", new Object())); }
        catch (AssertionError observed) {
            require(observed == valueFailure, "stored value equality exception identity");
            observedFailure = true;
        }
        require(observedFailure, "visible comparator alias evaluates stored value equality");
        NavigableMap<String, Object> hidden = M3FilteredViews.filterEntries(values, (key, value) -> false);
        equal(false, hidden.entrySet().contains(new SimpleImmutableEntry<>("zz", new Object())),
                "rejected representative does not evaluate value equality");
    }

    /** Operation counts detect a replacement that scans the entire owner for an ordinary seek. */
    public static void boundedWork() {
        int[] comparisons = {0};
        int[] evaluations = {0};
        Comparator<Integer> order = (left, right) -> {
            comparisons[0]++;
            return Integer.compare(left == null ? 0 : left, right == null ? 0 : right);
        };
        NavigableSet<Integer> source = new TreeSet<>(order);
        for (int value = -2048; value <= 2048; value++) if (value != 0) source.add(value);
        source.add(null);
        NavigableSet<Integer> view = M3FilteredViews.filter(source, value -> {
            evaluations[0]++;
            return value != null;
        });
        comparisons[0] = 0;
        equal(-1, view.lower(1), "bounded interior-null seek");
        require(comparisons[0] < 160, "ordinary seek must not scan 4097 owner members");
        equal(2, evaluations[0], "only null and next visible candidate evaluated");
        System.out.println("WORK filtered-null-seek comparisons=" + comparisons[0]
                + " predicateEvaluations=" + evaluations[0] + " ownerSize=" + source.size());
        NavigableSet<Integer> sparse = M3FilteredViews.filter(source, value -> {
            evaluations[0]++;
            return value != null && value < 0;
        });
        comparisons[0] = 0;
        evaluations[0] = 0;
        equal(-1, sparse.lower(128), "bounded rejected-member traversal");
        require(comparisons[0] < 160, "rejected run uses ordered iterator, not repeated searches");
        equal(129, evaluations[0], "exact rejected run and first accepted candidate");
    }

    private record NaturalKey(int value) implements Comparable<NaturalKey> {
        private static int comparisons;
        @Override public int compareTo(NaturalKey other) {
            comparisons++;
            return Integer.compare(value, other.value);
        }
    }

    /** Natural order and its reverse cannot contain null and need no disambiguation work. */
    public static void naturalOrderAbsence() {
        NavigableSet<NaturalKey> source = new TreeSet<>();
        source.add(new NaturalKey(1)); source.add(new NaturalKey(3));
        for (NavigableSet<NaturalKey> ordered : List.of(source, source.descendingSet())) {
            NavigableSet<NaturalKey> view = M3FilteredViews.filter(ordered, key -> {
                throw new AssertionError("no-result query must not evaluate a predicate");
            });
            NaturalKey first = ordered.first();
            NaturalKey last = ordered.last();
            NaturalKey.comparisons = 0;
            equal(null, ordered.lower(first), "owner lower absence");
            int ownerLower = NaturalKey.comparisons;
            NaturalKey.comparisons = 0;
            equal(null, view.lower(first), "filtered lower absence");
            equal(ownerLower, NaturalKey.comparisons, "lower has no extra natural comparisons");
            NaturalKey.comparisons = 0;
            equal(null, ordered.higher(last), "owner higher absence");
            int ownerHigher = NaturalKey.comparisons;
            NaturalKey.comparisons = 0;
            equal(null, view.higher(last), "filtered higher absence");
            equal(ownerHigher, NaturalKey.comparisons, "higher has no extra natural comparisons");
        }
    }

    public static void main(String[] arguments) {
        if (arguments.length != 0 && arguments[0].equals("navigation-witness")) navigationWitness();
        else if (arguments.length != 0 && arguments[0].equals("entry-witness")) entryWitness();
        else if (arguments.length != 0 && arguments[0].equals("entry-compatibility")) {
            membershipMatrix(); entryCompatibilityTrace();
        }
        else {
            navigationWitness(); navigationMatrix(); liveRanges();
            membershipMatrix(); callbacksAndExceptions(); entryCompatibilityTrace(); boundedWork();
            naturalOrderAbsence();
        }
        System.out.println("PASS M3_FILTERED_SORTED_EDGES checks=" + checks);
    }
}
