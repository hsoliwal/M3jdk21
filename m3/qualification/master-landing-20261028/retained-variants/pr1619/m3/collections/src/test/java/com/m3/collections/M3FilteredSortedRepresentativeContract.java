/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Stored string references and owner equivalence survive filtered view composition. */
public final class M3FilteredSortedRepresentativeContract {
    private static int checks;

    private M3FilteredSortedRepresentativeContract() { }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void sameFailure(RuntimeException failure, Runnable action) {
        try {
            action.run();
            throw new AssertionError("required callback failure was swallowed");
        } catch (RuntimeException actual) {
            require(actual == failure, "callback failure identity changed");
        }
    }

    private static void readOnly(Runnable action) {
        try {
            action.run();
            throw new AssertionError("read-only mutation admitted");
        } catch (UnsupportedOperationException expected) {
            checks++;
        }
    }

    private static TreeSet<String> owner(Comparator<String> comparator) {
        TreeSet<String> source = new TreeSet<>(comparator);
        source.addAll(List.of(new String("Alpha"), new String("Beta"), new String("Gamma")));
        return source;
    }

    private static List<Collection<String>> overloads(
            NavigableSet<String> source, Predicate<String> predicate) {
        return List.of(
                M3FilteredViews.filter((Collection<String>) source, predicate),
                M3FilteredViews.filter((Set<String>) source, predicate),
                M3FilteredViews.filter((SortedSet<String>) source, predicate),
                M3FilteredViews.filter(source, predicate),
                M3FilteredNavigableSet.view(source, predicate));
    }

    private static void overloadAndRangeMatrix() {
        TreeSet<String> tree = owner(String.CASE_INSENSITIVE_ORDER);
        String retained = tree.ceiling("beta");
        List<NavigableSet<String>> sources = List.of(tree, tree.descendingSet(),
                tree.subSet("a", true, "z", false),
                tree.subSet("alpha", false, "gamma", false),
                tree.descendingSet().subSet("gamma", true, "alpha", false));
        for (NavigableSet<String> source : sources) {
            for (Collection<String> view : overloads(source, value -> value == retained)) {
                require(view.contains(new String("bETA")), "retained comparator class lost");
                require(view.containsAll(List.of("beta", "BETA")), "containsAll aliases");
                require(view.iterator().next() == retained, "stored reference changed");
                require(view.size() == 1, "visible cardinality");
                require(!view.contains("ALPHA"), "hidden or out-of-range key leaked");
                readOnly(() -> view.remove("absent"));
                readOnly(() -> view.addAll(List.of()));
                readOnly(view::clear);
                Collection<String> nested = M3FilteredViews.filter(view, value -> value == retained);
                require(nested.contains("BETA"), "nested Collection lost comparator owner");
            }
        }
        SortedSet<String> plain = Collections.unmodifiableSortedSet(tree);
        require(!(plain instanceof NavigableSet<?>), "fixture must exercise plain SortedSet");
        for (Collection<String> view : List.of(
                M3FilteredViews.filter((Collection<String>) plain, value -> value == retained),
                M3FilteredViews.filter((Set<String>) plain, value -> value == retained),
                M3FilteredViews.filter(plain, value -> value == retained))) {
            require(view.contains("BETA"), "plain SortedSet representative");
        }
        Set<String> set = M3FilteredViews.filter((Set<String>) tree, value -> value == retained);
        for (int depth = 0; depth < 256; depth++) {
            set = M3FilteredViews.filter(set, value -> value == retained);
        }
        require(set.contains("BETA"), "deep private Set owner resolution");
    }

    private static void nullableComparators() {
        for (Comparator<String> order : List.of(
                Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER),
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))) {
            TreeSet<String> source = owner(order);
            source.add(null);
            for (Collection<String> view : overloads(source, Objects::isNull)) {
                require(view.contains(null), "stored null representative lost");
                require(view.iterator().next() == null, "stored null occurrence lost");
                require(!view.contains("ALPHA"), "hidden non-null key leaked");
            }
            Collection<String> nested = M3FilteredViews.filter(
                    M3FilteredViews.filter((Collection<String>) source, value -> true), Objects::isNull);
            require(nested.contains(null), "nested nullable representative");
        }
    }

    private static void callbackAndLiveBacking() {
        TreeSet<String> source = owner(String.CASE_INSENSITIVE_ORDER);
        String retained = source.ceiling("beta");
        List<String> events = new ArrayList<>();
        Set<String> inner = M3FilteredViews.filter((Set<String>) source, value -> {
            require(value != null && source.ceiling(value) == value, "inner received caller alias");
            events.add("inner:" + value);
            return value == retained;
        });
        Set<String> outer = M3FilteredViews.filter(inner, value -> {
            require(value == retained, "outer received caller alias");
            events.add("outer:" + value);
            return true;
        });
        require(!outer.contains("missing"), "absent key admitted");
        require(events.isEmpty(), "membership miss invoked predicate");
        require(!outer.contains("alpha"), "hidden inner key admitted");
        require(events.stream().noneMatch(event -> event.startsWith("outer:")),
                "hidden inner member reached outer predicate");
        events.clear();
        require(outer.contains("BETA"), "visible nested alias rejected");
        require(events.indexOf("inner:Beta") >= 0
                && events.indexOf("outer:Beta") > events.indexOf("inner:Beta"),
                "outer predicate preceded inner membership");
        RuntimeException failure = new IllegalStateException("predicate witness");
        Collection<String> failing = M3FilteredViews.filter((Collection<String>) source, value -> {
            throw failure;
        });
        sameFailure(failure, () -> failing.contains("BETA"));
        source.remove("beta");
        require(!outer.contains("BETA"), "removed backing representative remained visible");
        source.add(retained);
        require(outer.contains("beta"), "restored backing representative stayed hidden");
        String replacement = new String("Beta");
        source.remove("beta");
        source.add(replacement);
        require(!outer.contains("BETA"), "predicate result incorrectly cached for comparator class");
    }

    private static void equalityAndIdentity() {
        String visible = new String("same");
        String hidden = new String("same");
        Set<String> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        identities.add(visible);
        identities.add(hidden);
        Set<String> identityView = M3FilteredViews.filter(identities, value -> value == visible);
        require(identityView.contains(visible), "identity reference lost");
        require(!identityView.contains(hidden), "hidden equal identity leaked");
        require(!identityView.contains(new String("same")), "equals replaced identity");
        Set<String> equalities = new HashSet<>();
        equalities.add(visible);
        Set<String> equalityView = M3FilteredViews.filter(equalities, value -> value == visible);
        require(equalityView.contains(new String("same")), "ordinary equality replaced by identity");
        Collection<String> occurrences = M3FilteredViews.filter(
                new ArrayList<>(List.of(hidden, visible)), value -> value == visible);
        require(occurrences.contains(new String("same")), "later equal occurrence ignored");
    }

    private static void comparatorFailures() {
        RuntimeException failure = new IllegalArgumentException("comparator witness");
        Comparator<String> comparator = (left, right) -> {
            if ("reject".equals(left) || "reject".equals(right)) throw failure;
            return String.CASE_INSENSITIVE_ORDER.compare(left, right);
        };
        TreeSet<String> source = owner(comparator);
        for (Collection<String> view : overloads(source, value -> {
            throw new AssertionError("failed membership reached predicate");
        })) {
            sameFailure(failure, () -> view.contains("reject"));
        }
    }

    private static void sortedMapKeys() {
        TreeMap<String, Integer> source = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String alpha = new String("Alpha");
        String beta = new String("Beta");
        source.put(alpha, 1);
        source.put(beta, null);
        NavigableMap<String, Integer> view = M3FilteredViews.filterEntries(source,
                (key, value) -> key == beta);
        SortedMap<String, Integer> sorted = M3FilteredViews.filterEntries(
                (SortedMap<String, Integer>) source, (key, value) -> key == beta);
        for (SortedMap<String, Integer> candidate : List.of(view, sorted, view.descendingMap(),
                view.subMap("a", true, "z", false))) {
            require(candidate.containsKey("BETA"), "sorted map comparator alias");
            require(candidate.get("BETA") == null, "nullable mapped value changed");
            require(!candidate.containsKey("ALPHA"), "hidden map key leaked");
            require(candidate.entrySet().contains(new java.util.AbstractMap.SimpleImmutableEntry<>("BETA", null)),
                    "entry membership lost comparator class");
            require(candidate.firstKey() == beta, "map retained key identity changed");
            readOnly(() -> candidate.put("BETA", 2));
        }
        require(view.ceilingEntry("a").getKey() == beta, "navigation replaced stored key");
        require(view.descendingMap().floorEntry("a").getKey() == beta, "descending representative");
        require(view.navigableKeySet().contains("BETA"), "map key-set alias");
        NavigableMap<String, Integer> mutable = M3MutableFilteredViews.filterEntries(source,
                (key, value) -> key == beta);
        require(mutable.containsKey("BETA"), "mutable map alias");
        require(mutable.put("BETA", 3) == null && source.get(beta) == 3, "mutable nullable mapping update");
        require(source.ceilingKey("BETA") == beta, "mutable update replaced retained representative");
    }

    private static void generatedAliases() {
        for (int key = 0; key < 256; key++) {
            String text = "Prefix" + key + "Suffix";
            String stored = new String(text);
            String query = ("prefix" + key) + "suffix";
            TreeSet<String> source = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            source.add(stored);
            Set<String> view = M3FilteredViews.filter((Set<String>) source, value -> value == stored);
            require(view.contains(query), "generated alias " + key);
            require(!M3FilteredViews.filter(view, value -> false).contains(query), "generated hidden alias " + key);
        }
    }

    public static int run() {
        checks = 0;
        overloadAndRangeMatrix();
        nullableComparators();
        callbackAndLiveBacking();
        equalityAndIdentity();
        comparatorFailures();
        sortedMapKeys();
        generatedAliases();
        return checks;
    }

    public static void main(String[] arguments) {
        System.out.println("PASS sorted representative checks=" + run());
    }
}
