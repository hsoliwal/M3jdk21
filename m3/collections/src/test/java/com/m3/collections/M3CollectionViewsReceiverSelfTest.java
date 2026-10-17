// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generic JDK and actual M3 owner differential: no second stored key/value
 * table or per-element retained wrapper inside the live filtered descriptors.
 */
public final class M3CollectionViewsReceiverSelfTest {
    private static long checks;

    private M3CollectionViewsReceiverSelfTest() { }

    public static void main(String[] args) throws Exception {
        for (int seed = 0; seed < 128; seed++) {
            navigableErasedFactory(seed);
            orderedSetBackedMutation(seed);
        }
        storedComparatorRepresentative();
        mapErasureAndHiddenBindings();
        snapshotsDoNotReevaluateSource();
        noRetainedPayloadArray();
        System.out.println("PASS M3CollectionViewsReceiverSelfTest checks=" + checks);
    }

    private static void navigableErasedFactory(int seed) {
        Random random = new Random(0x4d33434f4c4c5649L ^ seed);
        NavigableSet<Integer> source = new M3TreeSet<>();
        NavigableSet<Integer> oracle = new TreeSet<>();
        for (int i = 0; i < 128; i++) {
            int candidate = random.nextInt(161) - 80;
            source.add(candidate);
            oracle.add(candidate);
        }
        // Runtime capability is preserved even across an erased Collection type.
        Collection<Integer> erased = source;
        Collection<Integer> generic = M3CollectionViews.filtered(
                erased, value -> (value & 1) == 0);
        check(generic instanceof NavigableSet<?>, "erased navigable factory");
        @SuppressWarnings("unchecked")
        NavigableSet<Integer> actual = (NavigableSet<Integer>) generic;
        NavigableSet<Integer> selected = new TreeSet<>();
        for (int value : oracle) if ((value & 1) == 0) selected.add(value);
        eq(new ArrayList<>(actual), new ArrayList<>(selected));
        check(actual.size() == selected.size(), "set size");
        check(actual.hashCode() == selected.hashCode(), "JDK set hash");
        check(actual.equals(selected), "JDK set equality");
        for (int query = -90; query <= 90; query += 7) {
            check(Objects.equals(actual.lower(query), selected.lower(query)), "lower");
            check(Objects.equals(actual.floor(query), selected.floor(query)), "floor");
            check(Objects.equals(actual.ceiling(query), selected.ceiling(query)), "ceiling");
            check(Objects.equals(actual.higher(query), selected.higher(query)), "higher");
        }
        var spliterator = actual.spliterator();
        check(spliterator.hasCharacteristics(Spliterator.SORTED | Spliterator.ORDERED),
                "sorted view traversal characteristics");
        check(!spliterator.hasCharacteristics(Spliterator.SIZED),
                "predicate view must not claim exact size");
        check(spliterator.getComparator() == null, "natural comparator");

        check(source.add(1000), "backing stays writable");
        check(actual.contains(1000), "late backing insert visible");
        check(actual.remove(1000), "view write-through removal");
        check(!source.contains(1000), "owner mutation applied");
        fails(IllegalArgumentException.class, () -> actual.add(1001));
        check(!source.contains(1001), "rejected admission is not stored");
        // Visible clear must preserve hidden odd backing entries.
        actual.clear();
        for (Integer key : source) check((key & 1) != 0, "hidden keys retained");
    }

    private static void orderedSetBackedMutation(int seed) {
        Random random = new Random(0x4d334c494e4b4544L + seed);
        Set<Integer> source = new M3LinkedHashSet<>();
        Set<Integer> oracle = new LinkedHashSet<>();
        for (int i = 0; i < 80; i++) {
            int key = random.nextInt(111) - 55;
            source.add(key);
            oracle.add(key);
        }
        Set<Integer> view = M3CollectionViews.filtered(source, x -> x >= 0);
        List<Integer> expected = new ArrayList<>();
        for (Integer key : oracle) if (key >= 0) expected.add(key);
        eq(new ArrayList<>(view), expected);
        check(view.size() == expected.size(), "ordered selected cardinality");
        check(view.stream().count() == expected.size(), "JDK selected stream");
        view.clear();
        for (Integer key : source) check(key < 0, "remove only admitted keys");
    }

    private static void storedComparatorRepresentative() {
        M3TreeSet<String> source = new M3TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        source.add("Alpha");
        source.add("beta");
        SortedSet<String> erased = source;
        SortedSet<String> view = M3CollectionViews.filtered(erased,
                item -> item.startsWith("A"));
        check(view instanceof NavigableSet<?>, "sorted-set erased runtime capability");
        check(view.contains("ALPHA"), "comparator key stored representative lookup");
        check(!view.contains("beta"), "hidden comparator-equivalent key");
        check(view.comparator() == String.CASE_INSENSITIVE_ORDER,
                "underlying comparator identity");
        check(view.remove("alpha"), "remove stored comparator-equivalent representative");
        check(source.isEmpty() == false && source.contains("beta"),
                "hidden key remains in source");
    }

    private static void mapErasureAndHiddenBindings() {
        M3TreeMap<Integer, Integer> owner = new M3TreeMap<>();
        owner.put(1, 10); owner.put(2, 20);
        owner.put(3, 30); owner.put(4, 40);
        Map<Integer, Integer> erased = owner;
        Map<Integer, Integer> selected = M3FilteredMaps.filtered(erased,
                (key, value) -> (key & 1) == 0 && value != null && value > 0);
        check(selected instanceof NavigableMap<?, ?>, "erased Map retains sorted navigation");
        @SuppressWarnings("unchecked")
        NavigableMap<Integer, Integer> nav = (NavigableMap<Integer, Integer>) selected;
        eq(new ArrayList<>(nav.keySet()), List.of(2, 4));
        check(nav.floorKey(3) == 2, "floor hidden key");
        check(nav.ceilingKey(3) == 4, "ceiling hidden key");
        check(nav.remove(1) == null && owner.containsKey(1),
                "hidden key cannot be removed through the filter");
        fails(IllegalArgumentException.class, () -> nav.put(5, 50));
        check(!owner.containsKey(5), "rejected map write does not admit key");
        check(nav.put(6, 60) == null, "visible map write");
        check(owner.get(6) == 60, "owner observes visible map write");
        nav.clear();
        check(owner.size() == 2 && owner.containsKey(1) && owner.containsKey(3),
                "map clear removes visible only");
    }

    private static void snapshotsDoNotReevaluateSource() {
        var source = new ArrayList<>(Arrays.asList("b", "A", "a", "B"));
        AtomicInteger calls = new AtomicInteger();
        Collection<String> counted = new java.util.AbstractCollection<>() {
            @Override public Iterator<String> iterator() {
                calls.incrementAndGet();
                return source.iterator();
            }
            @Override public int size() {
                throw new AssertionError("source size need not be queried");
            }
        };
        List<String> snapshot = M3CollectionViews.sortedSnapshot(
                counted, String.CASE_INSENSITIVE_ORDER);
        eq(snapshot, List.of("A", "a", "b", "B"));
        check(calls.get() == 1, "single pass and stable ordering");
        source.add("0");
        eq(snapshot, List.of("A", "a", "b", "B"));
        fails(UnsupportedOperationException.class, () -> snapshot.add("irrelevant"));
    }

    private static void noRetainedPayloadArray() {
        Collection<Integer> owner = new M3TreeSet<>();
        Collection<Integer> filtered = M3CollectionViews.filtered(owner, x -> x > 0);
        Map<Integer, Integer> underlying = new M3TreeMap<>();
        Map<Integer, Integer> mapped = M3FilteredMaps.filtered(underlying,
                (k, v) -> k > 0);
        for (Class<?> type : new Class<?>[] {filtered.getClass(), mapped.getClass()}) {
            for (Field f : type.getDeclaredFields()) {
                check(!f.getType().isArray(), "live wrapper must not retain element arrays");
                check(!f.getType().getName().contains("Entry[]"),
                        "no retained entry lane");
            }
        }
    }

    private static void eq(Object left, Object right) {
        checks++;
        if (!Objects.equals(left, right)) {
            throw new AssertionError("expected " + right + " but was " + left);
        }
    }

    private static void check(boolean state, String message) {
        checks++;
        if (!state) throw new AssertionError(message);
    }

    private static void fails(Class<? extends Throwable> expected, Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("expected " + expected + " got " + failure,
                    failure);
        }
        throw new AssertionError("expected " + expected.getSimpleName());
    }
}
