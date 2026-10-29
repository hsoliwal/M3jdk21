<<<<<<< HEAD
/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashSet;
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

/** Independent JDK-owner differential and historical-contract reconciliation proof. */
public final class M3FilteredNavigationReconciliationContract {
    private static long checks;
    private static long failures;
    private static String first;
    private static String scopeFirst;

    private M3FilteredNavigationReconciliationContract() { }

    public static void main(String[] args) {
        String selected = args.length == 0 ? "all" : args[0];
        if ("tape".equals(selected)) {
            System.out.println("stable-read-tape-sha256=" + digest(stableReadTape()));
            return;
        }
        run(selected, "navigation", M3FilteredNavigationReconciliationContract::navigation);
        run(selected, "entries", M3FilteredNavigationReconciliationContract::entryMembership);
        run(selected, "readonly", M3FilteredNavigationReconciliationContract::readOnlyEndpoints);
        run(selected, "depth", M3FilteredNavigationReconciliationContract::deepFilters);
        System.out.println("TOTAL checks=" + checks + " failures=" + failures);
        if (failures != 0) throw new AssertionError(first);
    }

    private static void run(String selected, String scope, Runnable proof) {
        if (!"all".equals(selected) && !scope.equals(selected)) return;
        long beginChecks = checks;
        long beginFailures = failures;
        scopeFirst = null;
        try { proof.run(); }
        catch (RuntimeException | Error error) { fail(scope + " threw " + error.getClass().getName()); }
        System.out.println(scope + " checks=" + (checks - beginChecks)
                + " failures=" + (failures - beginFailures) + " first=" + scopeFirst);
    }

    private static void navigation() {
        for (int rank = -5; rank <= 5; rank++) {
            final int nullRank = rank;
            Comparator<Integer> order = (a, b) -> Integer.compare(
                    a == null ? nullRank : a, b == null ? nullRank : b);
            NavigableSet<Integer> source = new TreeSet<>(order);
            for (int value = -4; value <= 4; value++) if (value != rank) source.add(value);
            source.add(null);
            List<Integer> before = new ArrayList<>(source);
            List<NavigableSet<Integer>> owners = List.of(source, source.descendingSet(),
                    source.subSet(-3, true, 3, true), source.subSet(-3, true, 3, true).descendingSet(),
                    source.headSet(2, false), source.tailSet(-2, false),
                    source.subSet(-3, true, 3, true).headSet(2, false).tailSet(-2, false));
            for (int mask = 0; mask < 16; mask++) {
                final int flags = mask;
                Predicate<Integer> visible = value -> value == null ? (flags & 8) != 0
                        : (flags & (1 << Math.floorMod(value, 3))) != 0;
                for (int position = 0; position < owners.size(); position++) {
                    NavigableSet<Integer> owner = owners.get(position);
                    NavigableSet<Integer> view = M3FilteredViews.filter(owner, visible);
                    NavigableSet<Integer> oracle = new TreeSet<>(owner.comparator());
                    for (Integer value : owner) if (visible.test(value)) oracle.add(value);
                    List<Integer> queries = new ArrayList<>();
                    for (int query = -6; query <= 6; query++) queries.add(query);
                    queries.add(null);
                    for (Integer query : queries) {
                        String context = "rank=" + rank + " owner=" + position + " mask=" + mask + " query=" + query;
                        neighbors(oracle, view, query, context);
                    }
                    same(new ArrayList<>(oracle), new ArrayList<>(view), "navigation encounter order");
                    same(oracle.size(), view.size(), "navigation size");
                    same(oracle.isEmpty(), view.isEmpty(), "navigation emptiness");
                    NavigableSet<Integer> nested = M3FilteredViews.filter(view, value -> value == null || value != -1);
                    NavigableSet<Integer> nestedOracle = new TreeSet<>(oracle.comparator());
                    for (Integer value : oracle) if (value == null || value != -1) nestedOracle.add(value);
                    for (Integer query : queries) neighbors(nestedOracle, nested, query, "nested navigation");
                }
            }
            same(before, new ArrayList<>(source), "navigation source untouched");
        }
        for (Comparator<Integer> order : Arrays.<Comparator<Integer>>asList(null, Comparator.naturalOrder(),
                Comparator.reverseOrder(), (a, b) -> Integer.compare(Objects.requireNonNull(a), Objects.requireNonNull(b)))) {
            NavigableSet<Integer> source = new TreeSet<>(order);
            source.addAll(List.of(-2, -1, 0, 1, 2));
            NavigableSet<Integer> filtered = M3FilteredViews.filter(source, value -> {
                if (value == null) throw new AssertionError("fabricated predicate null");
                return value != 0;
            });
            NavigableSet<Integer> oracle = new TreeSet<>(order);
            oracle.addAll(List.of(-2, -1, 1, 2));
            for (int query = -4; query <= 4; query++) neighbors(oracle, filtered, query, "nonnull comparator");
            expect(NullPointerException.class, () -> filtered.higher(null), "owner null refusal");
        }
        RuntimeException token = new IllegalStateException("predicate identity");
        NavigableSet<Integer> source = new TreeSet<>(Comparator.nullsFirst(Comparator.naturalOrder()));
        source.addAll(Arrays.asList(null, 1));
        NavigableSet<Integer> view = M3FilteredViews.filter(source, value -> { throw token; });
        identicalThrow(token, () -> view.ceiling(null), "nullable predicate throwable");
    }

    private static void neighbors(NavigableSet<Integer> oracle, NavigableSet<Integer> view,
            Integer query, String context) {
        same(oracle.lower(query), view.lower(query), context + " lower");
        same(oracle.floor(query), view.floor(query), context + " floor");
        same(oracle.ceiling(query), view.ceiling(query), context + " ceiling");
        same(oracle.higher(query), view.higher(query), context + " higher");
    }

    private static void entryMembership() {
        Comparator<String> nullMiddle = (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(
                a == null ? "Beta" : a, b == null ? "Beta" : b);
        List<Comparator<String>> orders = List.of(String.CASE_INSENSITIVE_ORDER,
                Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER),
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER), nullMiddle);
        for (int orderIndex = 0; orderIndex < orders.size(); orderIndex++) {
            NavigableMap<String, Object> source = new TreeMap<>(orders.get(orderIndex));
            String alpha = new String("Alpha");
            source.put(alpha, "visible");
            source.put("Gamma", null);
            source.put("Delta", "hidden");
            if (orderIndex != 0) source.put(null, "nullable");
            List<NavigableMap<String, Object>> owners = List.of(source, source.descendingMap(),
                    source.subMap("Alpha", true, "Gamma", true), source.headMap("Gamma", false),
                    source.tailMap("Alpha", false));
            for (NavigableMap<String, Object> owner : owners) {
                for (boolean acceptNullKey : List.of(false, true)) {
                    List<String> observedKeys = new ArrayList<>();
                    java.util.function.BiPredicate<String, Object> filter = (key, value) -> {
                        observedKeys.add(key);
                        return (acceptNullKey || key != null) && !"hidden".equals(value);
                    };
                    NavigableMap<String, Object> oracle = new TreeMap<>(owner.comparator());
                    for (Map.Entry<String, Object> entry : owner.entrySet())
                        if ((acceptNullKey || entry.getKey() != null) && !"hidden".equals(entry.getValue()))
                            oracle.put(entry.getKey(), entry.getValue());
                    SortedMap<String, Object> sortedOnly = Collections.unmodifiableSortedMap(owner);
                    List<SortedMap<String, Object>> views = List.of(M3FilteredViews.filterEntries(owner, filter),
                            M3FilteredViews.filterEntries(sortedOnly, filter));
                    for (SortedMap<String, Object> view : views) {
                        for (String key : Arrays.asList("Alpha", "ALPHA", "alpha", "GAMMA", "gamma", "DELTA", "missing", "BETA", null)) {
                            if (key == null && orderIndex == 0) continue;
                            for (Object value : Arrays.asList("visible", new String("visible"), "hidden", "nullable", null, 7)) {
                                Map.Entry<String, Object> probe = new AbstractMap.SimpleImmutableEntry<>(key, value);
                                observedKeys.clear();
                                boolean expected = oracle.entrySet().contains(probe);
                                same(expected, view.entrySet().contains(probe), "entry comparator membership");
                                if (owner.containsKey(key)) {
                                    String stored = owner.floorEntry(key).getKey();
                                    same(1, observedKeys.size(), "one stored membership predicate");
                                    if (!observedKeys.isEmpty()) check(observedKeys.getFirst() == stored, "predicate sees stored key identity");
                                } else same(0, observedKeys.size(), "absent key does not invoke predicate");
                            }
                        }
                        same(false, view.entrySet().contains("not an entry"), "non-entry probe");
                        same(false, view.entrySet().contains(null), "null entry probe");
                        same(new ArrayList<>(oracle.entrySet()), new ArrayList<>(view.entrySet()), "stored iteration unchanged");
                    }
                }
            }
            NavigableMap<String, Object> live = M3FilteredViews.filterEntries(source, (key, value) -> !"hidden".equals(value));
            source.put("ALPHA", "hidden");
            same(false, live.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>("alpha", "hidden")), "live hidden replacement");
            source.put("alpha", null);
            same(true, live.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>("ALPHA", null)), "live nullable replacement");
            source.remove("ALPHA");
            same(false, live.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>("alpha", null)), "live absence distinct from null");
        }
        NavigableMap<String, Object> source = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String stored = new String("Alpha");
        source.put(stored, "value");
        List<String> effects = new ArrayList<>();
        NavigableMap<String, Object> view = M3FilteredViews.filterEntries(source, (key, value) -> {
            check(key == stored, "entry predicate stored reference");
            effects.add("predicate");
            return true;
        });
        Map.Entry<String, Object> probe = new AbstractMap.SimpleImmutableEntry<>("ALPHA", "value") {
            private static final long serialVersionUID = 1L;
            @Override public String getKey() { effects.add("key"); return super.getKey(); }
            @Override public Object getValue() { effects.add("value"); return super.getValue(); }
        };
        same(true, view.entrySet().contains(probe), "adversarial entry accepted");
        same(List.of("key", "predicate", "value"), effects, "entry callback order and demand");
        RuntimeException token = new IllegalStateException("entry predicate identity");
        NavigableMap<String, Object> failing = M3FilteredViews.filterEntries(source, (key, value) -> { throw token; });
        identicalThrow(token, () -> failing.entrySet().contains(probe), "entry predicate throwable");
        check(source.firstKey() == stored, "entry lookup retains source key identity");
        Map.Entry<String, Object> throwingValue = new AbstractMap.SimpleImmutableEntry<>("ALPHA", "value") {
            private static final long serialVersionUID = 1L;
            @Override public Object getValue() { throw token; }
        };
        identicalThrow(token, () -> view.entrySet().contains(throwingValue), "entry value getter throwable");
        NavigableMap<String, Object> hidden = M3FilteredViews.filterEntries(source, (key, value) -> false);
        same(false, hidden.entrySet().contains(throwingValue), "hidden entry skips value getter");
        NavigableMap<String, Object> empty = M3FilteredViews.filterEntries(new TreeMap<>(String.CASE_INSENSITIVE_ORDER), (key, value) -> { throw token; });
        same(false, empty.entrySet().contains(throwingValue), "absent entry skips predicate and value getter");
        Map.Entry<String, Object> throwingKey = new AbstractMap.SimpleImmutableEntry<>("ALPHA", "value") {
            private static final long serialVersionUID = 1L;
            @Override public String getKey() { throw token; }
        };
        identicalThrow(token, () -> view.entrySet().contains(throwingKey), "entry key getter throwable");
        NavigableMap<String, Object> comparatorOwner = new TreeMap<>((a, b) -> {
            if ("explode".equals(a) || "explode".equals(b)) throw token;
            return String.CASE_INSENSITIVE_ORDER.compare(a, b);
        });
        comparatorOwner.put("Alpha", "value");
        NavigableMap<String, Object> comparatorView = M3FilteredViews.filterEntries(comparatorOwner, (key, value) -> true);
        identicalThrow(token, () -> comparatorView.entrySet().contains(Map.entry("explode", "value")), "owner comparator throwable");
    }

    private static void readOnlyEndpoints() {
        for (boolean populated : List.of(false, true)) {
            NavigableSet<Integer> source = new TreeSet<>();
            if (populated) source.addAll(List.of(1, 2, 3));
            for (boolean visible : List.of(false, true)) {
                long[] callbacks = {0};
                Predicate<Integer> predicate = value -> { callbacks[0]++; return visible; };
                SortedSet<Integer> sorted = M3FilteredViews.filter(Collections.unmodifiableSortedSet(source), predicate);
                NavigableSet<Integer> nav = M3FilteredViews.filter(source, predicate);
                List<SortedSet<Integer>> views = List.of(sorted, sorted.reversed(), sorted.reversed().reversed(),
                        sorted.headSet(3), sorted.tailSet(1), sorted.subSet(1, 3),
                        sorted.reversed().headSet(1), sorted.reversed().tailSet(3), sorted.reversed().subSet(3, 1),
                        nav, nav.descendingSet(), nav.reversed(), nav.headSet(3, false), nav.tailSet(1, true),
                        nav.subSet(1, true, 3, false), nav.descendingSet().subSet(3, true, 1, false));
                List<Integer> before = new ArrayList<>(source);
                for (SortedSet<Integer> view : views) {
                    callbacks[0] = 0;
                    expect(UnsupportedOperationException.class, view::removeFirst, "strict removeFirst");
                    expect(UnsupportedOperationException.class, view::removeLast, "strict removeLast");
                    expect(UnsupportedOperationException.class, () -> view.addFirst(9), "strict addFirst");
                    expect(UnsupportedOperationException.class, () -> view.addLast(9), "strict addLast");
                    expect(UnsupportedOperationException.class, () -> view.add(9), "strict add");
                    expect(UnsupportedOperationException.class, () -> view.remove(99), "strict absent remove");
                    expect(UnsupportedOperationException.class, () -> view.addAll(List.of()), "strict empty addAll");
                    expect(UnsupportedOperationException.class, () -> view.removeAll(List.of()), "strict empty removeAll");
                    expect(UnsupportedOperationException.class, () -> view.retainAll(List.of()), "strict retainAll");
                    expect(UnsupportedOperationException.class, () -> view.removeIf(value -> { throw new AssertionError("mutation callback"); }), "strict removeIf");
                    expect(UnsupportedOperationException.class, view::clear, "strict clear");
                    expect(UnsupportedOperationException.class, () -> view.iterator().remove(), "strict iterator remove");
                    same(0L, callbacks[0], "readonly refusal precedes predicate");
                    same(before, new ArrayList<>(source), "readonly source untouched");
                }
                expect(UnsupportedOperationException.class, nav::pollFirst, "strict pollFirst");
                expect(UnsupportedOperationException.class, nav::pollLast, "strict pollLast");
            }
        }
    }

    private static void deepFilters() {
        final int depth = 65_536;
        for (int family = 0; family < 4; family++) {
            long[] calls = {0};
            Predicate<Integer> predicate = value -> { calls[0]++; return true; };
            Collection<Integer> view;
            if (family == 0) {
                view = new ArrayList<>(List.of(1));
                for (int i = 0; i < depth; i++) view = M3FilteredViews.filter(view, predicate);
            } else if (family == 1) {
                Set<Integer> set = new LinkedHashSet<>(List.of(1));
                for (int i = 0; i < depth; i++) set = M3FilteredViews.filter(set, predicate);
                view = set;
            } else if (family == 2) {
                SortedSet<Integer> set = new TreeSet<>(List.of(1));
                for (int i = 0; i < depth; i++) set = M3FilteredViews.filter(set, predicate);
                view = set;
            } else {
                NavigableSet<Integer> set = new TreeSet<>(List.of(1));
                for (int i = 0; i < depth; i++) set = M3FilteredViews.filter(set, predicate);
                view = set;
            }
            same(1, view.iterator().next(), "deep traversal family=" + family);
            same((long) depth, calls[0], "deep predicate order/demand");
            calls[0] = 0;
            same(1, view.size(), "deep size");
            same((long) depth, calls[0], "deep size demand");
            calls[0] = 0;
            same(false, view.isEmpty(), "deep emptiness");
            same((long) depth, calls[0], "deep empty demand");
            if (family < 2) {
                calls[0] = 0;
                same(false, view.contains(2), "deep absent membership");
                same(0L, calls[0], "deep absence demand");
            }
        }
        for (boolean set : List.of(false, true)) {
            for (int depthAtRead : List.of(1, 2, 8, 64, 512)) {
                long[] calls = {0};
                Collection<Integer> view = set ? new LinkedHashSet<>(List.of(1)) : new ArrayList<>(List.of(1));
                for (int i = 0; i < depthAtRead; i++) {
                    Predicate<Integer> predicate = value -> { calls[0]++; return true; };
                    view = set ? M3FilteredViews.filter((Set<Integer>) view, predicate) : M3FilteredViews.filter(view, predicate);
                }
                same(true, view.contains(1), "deep present membership");
                same((long) depthAtRead * (depthAtRead + 1) / 2, calls[0], "historical triangular membership demand");
            }
        }
    }

    /** Emit independently on old/new owners; equality proves unchanged ordinary callback traces. */
    private static String stableReadTape() {
        StringBuilder tape = new StringBuilder();
        for (boolean set : List.of(false, true)) {
            Collection<Integer> view = set ? new LinkedHashSet<>(List.of(1, 2, 3, 4)) : new ArrayList<>(List.of(1, 2, 3, 4));
            for (int index = 0; index < 6; index++) {
                final int layer = index;
                Predicate<Integer> predicate = value -> {
                    tape.append('P').append(layer).append(':').append(value).append(';');
                    return layer != 2 || value != 2;
                };
                view = set ? M3FilteredViews.filter((Set<Integer>) view, predicate) : M3FilteredViews.filter(view, predicate);
            }
            tape.append(view.size()).append(view.isEmpty()).append(view.contains(1)).append(view.contains(2));
            tape.append(Arrays.toString(view.toArray()));
            view.forEach(value -> tape.append('V').append(value));
            Iterator<Integer> cursor = view.iterator();
            tape.append(cursor.hasNext()).append(cursor.hasNext()).append(cursor.next());
        }
        SortedSet<Integer> backing = Collections.unmodifiableSortedSet(new TreeSet<>(List.of(1, 2, 3, 4)));
        SortedSet<Integer> view = M3FilteredViews.filter(backing, value -> {
            tape.append('S').append(value).append(';');
            return value % 2 == 1;
        }).reversed();
        tape.append(view.size()).append(view.isEmpty()).append(view.contains(3)).append(view.first()).append(view.last());
        tape.append(Arrays.toString(view.toArray())).append(Arrays.toString(view.toArray(Integer[]::new)));
        tape.append(view.containsAll(List.of(3, 1))).append(view.hashCode()).append(view.toString());
        view.forEach(value -> tape.append('F').append(value));
        tape.append(view.headSet(1).size()).append(view.tailSet(3).size()).append(view.subSet(3, 1).size());
        view.spliterator().forEachRemaining(value -> tape.append('R').append(value));
        return tape.toString();
    }

    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action, String context) {
        checks++;
        try { action.run(); fail(context + " returned normally"); }
        catch (RuntimeException | Error error) {
            if (!type.isInstance(error)) fail(context + " expected=" + type.getName() + " actual=" + error.getClass().getName());
        }
    }

    private static void identicalThrow(Throwable expected, Runnable action, String context) {
        checks++;
        try { action.run(); fail(context + " returned normally"); }
        catch (RuntimeException | Error error) { if (error != expected) fail(context + " changed throwable identity"); }
    }

    private static void same(Object expected, Object actual, String context) {
        checks++;
        if (!Objects.equals(expected, actual)) fail(context + " expected=" + expected + " actual=" + actual);
    }

    private static void check(boolean condition, String context) {
        checks++;
        if (!condition) fail(context);
    }

    private static void fail(String message) {
        failures++;
        if (first == null) first = message;
        if (scopeFirst == null) scopeFirst = message;
    }
}
||||||| merged common ancestors
=======
/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashSet;
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

/**
 * Independent JDK-owner differential and historical-contract reconciliation proof.
 * Recovery adaptation, 2026-10-19: #1368 backing entry membership precedes
 * visibility. Wrong-value/absent probes suppress the predicate, while getter
 * order and getter failures remain the backing owner's observable contract.
 */
public final class M3FilteredNavigationReconciliationContract {
    private static long checks;
    private static long failures;
    private static String first;
    private static String scopeFirst;

    private M3FilteredNavigationReconciliationContract() { }

    public static void main(String[] args) {
        String selected = args.length == 0 ? "all" : args[0];
        if ("tape".equals(selected)) {
            System.out.println("stable-read-tape-sha256=" + digest(stableReadTape()));
            return;
        }
        run(selected, "navigation", M3FilteredNavigationReconciliationContract::navigation);
        run(selected, "entries", M3FilteredNavigationReconciliationContract::entryMembership);
        run(selected, "readonly", M3FilteredNavigationReconciliationContract::readOnlyEndpoints);
        run(selected, "depth", M3FilteredNavigationReconciliationContract::deepFilters);
        System.out.println("TOTAL checks=" + checks + " failures=" + failures);
        if (failures != 0) throw new AssertionError(first);
    }

    private static void run(String selected, String scope, Runnable proof) {
        if (!"all".equals(selected) && !scope.equals(selected)) return;
        long beginChecks = checks;
        long beginFailures = failures;
        scopeFirst = null;
        try { proof.run(); }
        catch (RuntimeException | Error error) { fail(scope + " threw " + error.getClass().getName()); }
        System.out.println(scope + " checks=" + (checks - beginChecks)
                + " failures=" + (failures - beginFailures) + " first=" + scopeFirst);
    }

    private static void navigation() {
        for (int rank = -5; rank <= 5; rank++) {
            final int nullRank = rank;
            Comparator<Integer> order = (a, b) -> Integer.compare(
                    a == null ? nullRank : a, b == null ? nullRank : b);
            NavigableSet<Integer> source = new TreeSet<>(order);
            for (int value = -4; value <= 4; value++) if (value != rank) source.add(value);
            source.add(null);
            List<Integer> before = new ArrayList<>(source);
            List<NavigableSet<Integer>> owners = List.of(source, source.descendingSet(),
                    source.subSet(-3, true, 3, true), source.subSet(-3, true, 3, true).descendingSet(),
                    source.headSet(2, false), source.tailSet(-2, false),
                    source.subSet(-3, true, 3, true).headSet(2, false).tailSet(-2, false));
            for (int mask = 0; mask < 16; mask++) {
                final int flags = mask;
                Predicate<Integer> visible = value -> value == null ? (flags & 8) != 0
                        : (flags & (1 << Math.floorMod(value, 3))) != 0;
                for (int position = 0; position < owners.size(); position++) {
                    NavigableSet<Integer> owner = owners.get(position);
                    NavigableSet<Integer> view = M3FilteredViews.filter(owner, visible);
                    NavigableSet<Integer> oracle = new TreeSet<>(owner.comparator());
                    for (Integer value : owner) if (visible.test(value)) oracle.add(value);
                    List<Integer> queries = new ArrayList<>();
                    for (int query = -6; query <= 6; query++) queries.add(query);
                    queries.add(null);
                    for (Integer query : queries) {
                        String context = "rank=" + rank + " owner=" + position + " mask=" + mask + " query=" + query;
                        neighbors(oracle, view, query, context);
                    }
                    same(new ArrayList<>(oracle), new ArrayList<>(view), "navigation encounter order");
                    same(oracle.size(), view.size(), "navigation size");
                    same(oracle.isEmpty(), view.isEmpty(), "navigation emptiness");
                    NavigableSet<Integer> nested = M3FilteredViews.filter(view, value -> value == null || value != -1);
                    NavigableSet<Integer> nestedOracle = new TreeSet<>(oracle.comparator());
                    for (Integer value : oracle) if (value == null || value != -1) nestedOracle.add(value);
                    for (Integer query : queries) neighbors(nestedOracle, nested, query, "nested navigation");
                }
            }
            same(before, new ArrayList<>(source), "navigation source untouched");
        }
        for (Comparator<Integer> order : Arrays.<Comparator<Integer>>asList(null, Comparator.naturalOrder(),
                Comparator.reverseOrder(), (a, b) -> Integer.compare(Objects.requireNonNull(a), Objects.requireNonNull(b)))) {
            NavigableSet<Integer> source = new TreeSet<>(order);
            source.addAll(List.of(-2, -1, 0, 1, 2));
            NavigableSet<Integer> filtered = M3FilteredViews.filter(source, value -> {
                if (value == null) throw new AssertionError("fabricated predicate null");
                return value != 0;
            });
            NavigableSet<Integer> oracle = new TreeSet<>(order);
            oracle.addAll(List.of(-2, -1, 1, 2));
            for (int query = -4; query <= 4; query++) neighbors(oracle, filtered, query, "nonnull comparator");
            expect(NullPointerException.class, () -> filtered.higher(null), "owner null refusal");
        }
        RuntimeException token = new IllegalStateException("predicate identity");
        NavigableSet<Integer> source = new TreeSet<>(Comparator.nullsFirst(Comparator.naturalOrder()));
        source.addAll(Arrays.asList(null, 1));
        NavigableSet<Integer> view = M3FilteredViews.filter(source, value -> { throw token; });
        identicalThrow(token, () -> view.ceiling(null), "nullable predicate throwable");
    }

    private static void neighbors(NavigableSet<Integer> oracle, NavigableSet<Integer> view,
            Integer query, String context) {
        same(oracle.lower(query), view.lower(query), context + " lower");
        same(oracle.floor(query), view.floor(query), context + " floor");
        same(oracle.ceiling(query), view.ceiling(query), context + " ceiling");
        same(oracle.higher(query), view.higher(query), context + " higher");
    }

    private static void entryMembership() {
        Comparator<String> nullMiddle = (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(
                a == null ? "Beta" : a, b == null ? "Beta" : b);
        List<Comparator<String>> orders = List.of(String.CASE_INSENSITIVE_ORDER,
                Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER),
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER), nullMiddle);
        for (int orderIndex = 0; orderIndex < orders.size(); orderIndex++) {
            NavigableMap<String, Object> source = new TreeMap<>(orders.get(orderIndex));
            String alpha = new String("Alpha");
            source.put(alpha, "visible");
            source.put("Gamma", null);
            source.put("Delta", "hidden");
            if (orderIndex != 0) source.put(null, "nullable");
            List<NavigableMap<String, Object>> owners = List.of(source, source.descendingMap(),
                    source.subMap("Alpha", true, "Gamma", true), source.headMap("Gamma", false),
                    source.tailMap("Alpha", false));
            for (NavigableMap<String, Object> owner : owners) {
                for (boolean acceptNullKey : List.of(false, true)) {
                    List<String> observedKeys = new ArrayList<>();
                    java.util.function.BiPredicate<String, Object> filter = (key, value) -> {
                        observedKeys.add(key);
                        return (acceptNullKey || key != null) && !"hidden".equals(value);
                    };
                    NavigableMap<String, Object> oracle = new TreeMap<>(owner.comparator());
                    for (Map.Entry<String, Object> entry : owner.entrySet())
                        if ((acceptNullKey || entry.getKey() != null) && !"hidden".equals(entry.getValue()))
                            oracle.put(entry.getKey(), entry.getValue());
                    SortedMap<String, Object> sortedOnly = Collections.unmodifiableSortedMap(owner);
                    List<SortedMap<String, Object>> views = List.of(M3FilteredViews.filterEntries(owner, filter),
                            M3FilteredViews.filterEntries(sortedOnly, filter));
                    for (SortedMap<String, Object> view : views) {
                        for (String key : Arrays.asList("Alpha", "ALPHA", "alpha", "GAMMA", "gamma", "DELTA", "missing", "BETA", null)) {
                            if (key == null && orderIndex == 0) continue;
                            for (Object value : Arrays.asList("visible", new String("visible"), "hidden", "nullable", null, 7)) {
                                Map.Entry<String, Object> probe = new AbstractMap.SimpleImmutableEntry<>(key, value);
                                observedKeys.clear();
                                boolean expected = oracle.entrySet().contains(probe);
                                same(expected, view.entrySet().contains(probe), "entry comparator membership");
                                if (owner.entrySet().contains(probe)) {
                                    String stored = owner.floorEntry(key).getKey();
                                    same(1, observedKeys.size(), "one stored membership predicate");
                                    if (!observedKeys.isEmpty()) check(observedKeys.getFirst() == stored, "predicate sees stored key identity");
                                } else same(0, observedKeys.size(), "absent or wrong-value entry does not invoke predicate");
                            }
                        }
                        same(false, view.entrySet().contains("not an entry"), "non-entry probe");
                        same(false, view.entrySet().contains(null), "null entry probe");
                        same(new ArrayList<>(oracle.entrySet()), new ArrayList<>(view.entrySet()), "stored iteration unchanged");
                    }
                }
            }
            NavigableMap<String, Object> live = M3FilteredViews.filterEntries(source, (key, value) -> !"hidden".equals(value));
            source.put("ALPHA", "hidden");
            same(false, live.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>("alpha", "hidden")), "live hidden replacement");
            source.put("alpha", null);
            same(true, live.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>("ALPHA", null)), "live nullable replacement");
            source.remove("ALPHA");
            same(false, live.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>("alpha", null)), "live absence distinct from null");
        }
        NavigableMap<String, Object> source = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String stored = new String("Alpha");
        source.put(stored, "value");
        List<String> effects = new ArrayList<>();
        NavigableMap<String, Object> view = M3FilteredViews.filterEntries(source, (key, value) -> {
            check(key == stored, "entry predicate stored reference");
            effects.add("predicate");
            return true;
        });
        Map.Entry<String, Object> probe = new AbstractMap.SimpleImmutableEntry<>("ALPHA", "value") {
            private static final long serialVersionUID = 1L;
            @Override public String getKey() { effects.add("key"); return super.getKey(); }
            @Override public Object getValue() { effects.add("value"); return super.getValue(); }
        };
        same(true, source.entrySet().contains(probe), "backing adversarial entry accepted");
        List<String> expectedEffects = new ArrayList<>(effects);
        expectedEffects.add("key");
        expectedEffects.add("predicate");
        effects.clear();
        same(true, view.entrySet().contains(probe), "adversarial entry accepted");
        same(expectedEffects, effects, "backing getter order then stored-key visibility");
        RuntimeException token = new IllegalStateException("entry predicate identity");
        NavigableMap<String, Object> failing = M3FilteredViews.filterEntries(source, (key, value) -> { throw token; });
        identicalThrow(token, () -> failing.entrySet().contains(probe), "entry predicate throwable");
        check(source.firstKey() == stored, "entry lookup retains source key identity");
        Map.Entry<String, Object> throwingValue = new AbstractMap.SimpleImmutableEntry<>("ALPHA", "value") {
            private static final long serialVersionUID = 1L;
            @Override public Object getValue() { throw token; }
        };
        identicalThrow(token, () -> view.entrySet().contains(throwingValue), "entry value getter throwable");
        NavigableMap<String, Object> hidden = M3FilteredViews.filterEntries(source, (key, value) -> false);
        identicalThrow(token, () -> hidden.entrySet().contains(throwingValue),
                "backing value getter failure precedes hidden visibility");
        NavigableMap<String, Object> empty = M3FilteredViews.filterEntries(new TreeMap<>(String.CASE_INSENSITIVE_ORDER), (key, value) -> { throw token; });
        identicalThrow(token, () -> empty.entrySet().contains(throwingValue),
                "empty backing TreeMap retains entry value getter failure");
        Map.Entry<String, Object> throwingKey = new AbstractMap.SimpleImmutableEntry<>("ALPHA", "value") {
            private static final long serialVersionUID = 1L;
            @Override public String getKey() { throw token; }
        };
        identicalThrow(token, () -> view.entrySet().contains(throwingKey), "entry key getter throwable");
        NavigableMap<String, Object> comparatorOwner = new TreeMap<>((a, b) -> {
            if ("explode".equals(a) || "explode".equals(b)) throw token;
            return String.CASE_INSENSITIVE_ORDER.compare(a, b);
        });
        comparatorOwner.put("Alpha", "value");
        NavigableMap<String, Object> comparatorView = M3FilteredViews.filterEntries(comparatorOwner, (key, value) -> true);
        identicalThrow(token, () -> comparatorView.entrySet().contains(Map.entry("explode", "value")), "owner comparator throwable");
    }

    private static void readOnlyEndpoints() {
        for (boolean populated : List.of(false, true)) {
            NavigableSet<Integer> source = new TreeSet<>();
            if (populated) source.addAll(List.of(1, 2, 3));
            for (boolean visible : List.of(false, true)) {
                long[] callbacks = {0};
                Predicate<Integer> predicate = value -> { callbacks[0]++; return visible; };
                SortedSet<Integer> sorted = M3FilteredViews.filter(Collections.unmodifiableSortedSet(source), predicate);
                NavigableSet<Integer> nav = M3FilteredViews.filter(source, predicate);
                List<SortedSet<Integer>> views = List.of(sorted, sorted.reversed(), sorted.reversed().reversed(),
                        sorted.headSet(3), sorted.tailSet(1), sorted.subSet(1, 3),
                        sorted.reversed().headSet(1), sorted.reversed().tailSet(3), sorted.reversed().subSet(3, 1),
                        nav, nav.descendingSet(), nav.reversed(), nav.headSet(3, false), nav.tailSet(1, true),
                        nav.subSet(1, true, 3, false), nav.descendingSet().subSet(3, true, 1, false));
                List<Integer> before = new ArrayList<>(source);
                for (SortedSet<Integer> view : views) {
                    callbacks[0] = 0;
                    expect(UnsupportedOperationException.class, view::removeFirst, "strict removeFirst");
                    expect(UnsupportedOperationException.class, view::removeLast, "strict removeLast");
                    expect(UnsupportedOperationException.class, () -> view.addFirst(9), "strict addFirst");
                    expect(UnsupportedOperationException.class, () -> view.addLast(9), "strict addLast");
                    expect(UnsupportedOperationException.class, () -> view.add(9), "strict add");
                    expect(UnsupportedOperationException.class, () -> view.remove(99), "strict absent remove");
                    expect(UnsupportedOperationException.class, () -> view.addAll(List.of()), "strict empty addAll");
                    expect(UnsupportedOperationException.class, () -> view.removeAll(List.of()), "strict empty removeAll");
                    expect(UnsupportedOperationException.class, () -> view.retainAll(List.of()), "strict retainAll");
                    expect(UnsupportedOperationException.class, () -> view.removeIf(value -> { throw new AssertionError("mutation callback"); }), "strict removeIf");
                    expect(UnsupportedOperationException.class, view::clear, "strict clear");
                    expect(UnsupportedOperationException.class, () -> view.iterator().remove(), "strict iterator remove");
                    same(0L, callbacks[0], "readonly refusal precedes predicate");
                    same(before, new ArrayList<>(source), "readonly source untouched");
                }
                expect(UnsupportedOperationException.class, nav::pollFirst, "strict pollFirst");
                expect(UnsupportedOperationException.class, nav::pollLast, "strict pollLast");
            }
        }
    }

    private static void deepFilters() {
        final int depth = 65_536;
        for (int family = 0; family < 4; family++) {
            long[] calls = {0};
            Predicate<Integer> predicate = value -> { calls[0]++; return true; };
            Collection<Integer> view;
            if (family == 0) {
                view = new ArrayList<>(List.of(1));
                for (int i = 0; i < depth; i++) view = M3FilteredViews.filter(view, predicate);
            } else if (family == 1) {
                Set<Integer> set = new LinkedHashSet<>(List.of(1));
                for (int i = 0; i < depth; i++) set = M3FilteredViews.filter(set, predicate);
                view = set;
            } else if (family == 2) {
                SortedSet<Integer> set = new TreeSet<>(List.of(1));
                for (int i = 0; i < depth; i++) set = M3FilteredViews.filter(set, predicate);
                view = set;
            } else {
                NavigableSet<Integer> set = new TreeSet<>(List.of(1));
                for (int i = 0; i < depth; i++) set = M3FilteredViews.filter(set, predicate);
                view = set;
            }
            same(1, view.iterator().next(), "deep traversal family=" + family);
            same((long) depth, calls[0], "deep predicate order/demand");
            calls[0] = 0;
            same(1, view.size(), "deep size");
            same((long) depth, calls[0], "deep size demand");
            calls[0] = 0;
            same(false, view.isEmpty(), "deep emptiness");
            same((long) depth, calls[0], "deep empty demand");
            if (family < 2) {
                calls[0] = 0;
                same(false, view.contains(2), "deep absent membership");
                same(0L, calls[0], "deep absence demand");
            }
        }
        for (boolean set : List.of(false, true)) {
            for (int depthAtRead : List.of(1, 2, 8, 64, 512)) {
                long[] calls = {0};
                Collection<Integer> view = set ? new LinkedHashSet<>(List.of(1)) : new ArrayList<>(List.of(1));
                for (int i = 0; i < depthAtRead; i++) {
                    Predicate<Integer> predicate = value -> { calls[0]++; return true; };
                    view = set ? M3FilteredViews.filter((Set<Integer>) view, predicate) : M3FilteredViews.filter(view, predicate);
                }
                same(true, view.contains(1), "deep present membership");
                same((long) depthAtRead * (depthAtRead + 1) / 2, calls[0], "historical triangular membership demand");
            }
        }
    }

    /** Emit independently on old/new owners; equality proves unchanged ordinary callback traces. */
    private static String stableReadTape() {
        StringBuilder tape = new StringBuilder();
        for (boolean set : List.of(false, true)) {
            Collection<Integer> view = set ? new LinkedHashSet<>(List.of(1, 2, 3, 4)) : new ArrayList<>(List.of(1, 2, 3, 4));
            for (int index = 0; index < 6; index++) {
                final int layer = index;
                Predicate<Integer> predicate = value -> {
                    tape.append('P').append(layer).append(':').append(value).append(';');
                    return layer != 2 || value != 2;
                };
                view = set ? M3FilteredViews.filter((Set<Integer>) view, predicate) : M3FilteredViews.filter(view, predicate);
            }
            tape.append(view.size()).append(view.isEmpty()).append(view.contains(1)).append(view.contains(2));
            tape.append(Arrays.toString(view.toArray()));
            view.forEach(value -> tape.append('V').append(value));
            Iterator<Integer> cursor = view.iterator();
            tape.append(cursor.hasNext()).append(cursor.hasNext()).append(cursor.next());
        }
        SortedSet<Integer> backing = Collections.unmodifiableSortedSet(new TreeSet<>(List.of(1, 2, 3, 4)));
        SortedSet<Integer> view = M3FilteredViews.filter(backing, value -> {
            tape.append('S').append(value).append(';');
            return value % 2 == 1;
        }).reversed();
        tape.append(view.size()).append(view.isEmpty()).append(view.contains(3)).append(view.first()).append(view.last());
        tape.append(Arrays.toString(view.toArray())).append(Arrays.toString(view.toArray(Integer[]::new)));
        tape.append(view.containsAll(List.of(3, 1))).append(view.hashCode()).append(view.toString());
        view.forEach(value -> tape.append('F').append(value));
        tape.append(view.headSet(1).size()).append(view.tailSet(3).size()).append(view.subSet(3, 1).size());
        view.spliterator().forEachRemaining(value -> tape.append('R').append(value));
        return tape.toString();
    }

    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action, String context) {
        checks++;
        try { action.run(); fail(context + " returned normally"); }
        catch (RuntimeException | Error error) {
            if (!type.isInstance(error)) fail(context + " expected=" + type.getName() + " actual=" + error.getClass().getName());
        }
    }

    private static void identicalThrow(Throwable expected, Runnable action, String context) {
        checks++;
        try { action.run(); fail(context + " returned normally"); }
        catch (RuntimeException | Error error) { if (error != expected) fail(context + " changed throwable identity"); }
    }

    private static void same(Object expected, Object actual, String context) {
        checks++;
        if (!Objects.equals(expected, actual)) fail(context + " expected=" + expected + " actual=" + actual);
    }

    private static void check(boolean condition, String context) {
        checks++;
        if (!condition) fail(context);
    }

    private static void fail(String message) {
        failures++;
        if (first == null) first = message;
        if (scopeFirst == null) scopeFirst = message;
    }
}
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
