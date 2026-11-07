/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections.interop;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.collections4.multiset.HashMultiSet;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;

/**
 * Reference model for the members restored by mergeloss case 01 (qualification/mergeloss-01-15-20261106): the
 * real Guava, Apache Commons Collections and Eclipse Collections implementations (the interop module's declared
 * dependencies) and plain java.util collections define the expected results of the restored factories and
 * collectors over seeded workloads, sequential and parallel. Prints one summary line per member and a total;
 * a mutant of a restored member is killed when any member reports failures.
 *
 * <p>usage: MergelossInteropReference ROUNDS SEED (the m3_arrays JNI library via -Dm3.arrays.native.path)</p>
 */
public final class MergelossInteropReference {
    private static final Map<String, long[]> TALLY = new TreeMap<>();
    private static String member = "?";

    private MergelossInteropReference() { }

    public static void main(String[] args) {
        int rounds = Integer.parseInt(args[0]);
        long seed = Long.decode(args[1]);
        for (int round = 0; round < rounds; round++) {
            Random random = new Random(seed + round * 0x9E3779B97F4A7C15L);
            List<Integer> data = data(random, 1 + random.nextInt(3000), 1 + random.nextInt(80), true);
            guarded("toGuavaMultiset", () -> countCollectors(data));
            guarded("toApacheListValuedMap", () -> multivalueCollectors(data, random));
            guarded("toGuavaBiMap", () -> bidiCollectors(random));
            guarded("toEclipseOrderedMap", () -> orderedMapCollector(random));
            guarded("guavaMultiset", () -> guavaFactory(random));
            guarded("apacheMultiSet", () -> apacheMultiSetFactory(random));
            guarded("apacheBag", () -> apacheBagFactory(random));
            guarded("eclipseBag", () -> eclipseBagFactory(random));
            guarded("eclipseOrderedMap", () -> eclipseOrderedMapFactory(random));
        }
        long checks = 0, failures = 0;
        for (var row : TALLY.entrySet()) {
            System.out.println("MEMBER " + row.getKey() + " checks=" + row.getValue()[0] + " failures=" + row.getValue()[1]);
            checks += row.getValue()[0];
            failures += row.getValue()[1];
        }
        System.out.println("REFERENCE checks=" + checks + " failures=" + failures);
    }

    private static List<Integer> data(Random random, int size, int distinct, boolean nulls) {
        List<Integer> data = new ArrayList<>(size);
        for (int i = 0; i < size; i++) data.add(nulls && random.nextInt(20) == 0 ? null : random.nextInt(distinct));
        return data;
    }

    private static void guarded(String name, Runnable body) {
        member = name;
        try {
            body.run();
        } catch (RuntimeException | AssertionError failure) {
            check(false, "unexpected " + failure);
        }
    }

    private static void check(boolean ok, Object detail) {
        long[] row = TALLY.computeIfAbsent(member, k -> new long[2]);
        row[0]++;
        if (!ok) {
            if (row[1] < 3) System.out.println("MISMATCH " + member + " " + detail);
            row[1]++;
        }
    }

    private static void same(Object expected, Object actual, String what) {
        check(Objects.equals(expected, actual), what + " expected=" + expected + " actual=" + actual);
    }

    private static Class<?> thrown(Runnable body) {
        try {
            body.run();
            return null;
        } catch (RuntimeException failure) {
            return failure.getClass();
        }
    }

    /** toGuavaMultiset/toApacheMultiSet/toApacheBag/toEclipseBag against real-library counts, both stream modes. */
    @SuppressWarnings("deprecation")
    private static void countCollectors(List<Integer> data) {
        Multiset<Integer> expected = LinkedHashMultiset.create(data);
        HashMultiSet<Integer> apacheReference = new HashMultiSet<>(data);
        HashBag<Integer> bagReference = new HashBag<>(data);
        var eclipseReference = org.eclipse.collections.impl.bag.mutable.HashBag.newBag(data);
        for (boolean parallel : new boolean[] {false, true}) {
            member = "toGuavaMultiset";
            Multiset<Integer> guava = (parallel ? data.parallelStream() : data.stream())
                    .collect(M3LibraryCollectors.toGuavaMultiset());
            same(expected.size(), guava.size(), "size");
            same(new ArrayList<>(expected.elementSet()), new ArrayList<>(guava.elementSet()), "elementSet order");
            for (Integer e : expected.elementSet()) same(expected.count(e), guava.count(e), "count " + e);
            member = "toApacheMultiSet";
            MultiSet<Integer> apache = (parallel ? data.parallelStream() : data.stream())
                    .collect(M3LibraryCollectors.toApacheMultiSet());
            same(apacheReference.size(), apache.size(), "size");
            same(apacheReference.uniqueSet(), new LinkedHashSet<>(apache.uniqueSet()), "uniqueSet");
            for (Integer e : apacheReference.uniqueSet()) same(apacheReference.getCount(e), apache.getCount(e), "count " + e);
            member = "toApacheBag";
            Bag<Integer> bag = (parallel ? data.parallelStream() : data.stream())
                    .collect(M3LibraryCollectors.toApacheBag());
            same(bagReference.size(), bag.size(), "size");
            for (Integer e : bagReference.uniqueSet()) same(bagReference.getCount(e), bag.getCount(e), "count " + e);
            same(bagReference.uniqueSet().size(), bag.uniqueSet().size(), "distinct");
            member = "toEclipseBag";
            MutableBag<Integer> eclipse = (parallel ? data.parallelStream() : data.stream())
                    .collect(M3LibraryCollectors.toEclipseBag());
            same(eclipseReference.size(), eclipse.size(), "size");
            same(eclipseReference.sizeDistinct(), eclipse.sizeDistinct(), "distinct");
            eclipseReference.forEachWithOccurrences((e, n) -> same(n, eclipse.occurrencesOf(e), "count " + e));
        }
    }

    /** toApacheListValuedMap/toApacheSetValuedMap against Apache ArrayList/HashSet valued maps (order via java.util). */
    private static void multivalueCollectors(List<Integer> data, Random random) {
        int modulus = 1 + random.nextInt(9);
        Function<Integer, Integer> key = v -> v == null ? null : v % modulus;
        ArrayListValuedHashMap<Integer, Integer> lists = new ArrayListValuedHashMap<>();
        HashSetValuedHashMap<Integer, Integer> sets = new HashSetValuedHashMap<>();
        Map<Integer, LinkedHashSet<Integer>> order = new HashMap<>();
        for (Integer v : data) {
            lists.put(key.apply(v), v);
            sets.put(key.apply(v), v);
            order.computeIfAbsent(key.apply(v), k -> new LinkedHashSet<>()).add(v);
        }
        for (boolean parallel : new boolean[] {false, true}) {
            member = "toApacheListValuedMap";
            ListValuedMap<Integer, Integer> m3Lists = (parallel ? data.parallelStream() : data.stream())
                    .collect(M3LibraryCollectors.toApacheListValuedMap(key, Function.identity()));
            same(lists.size(), m3Lists.size(), "size");
            same(lists.keySet(), new LinkedHashSet<>(m3Lists.keySet()), "keys");
            for (Integer k : lists.keySet()) same(lists.get(k), new ArrayList<>(m3Lists.get(k)), "values " + k);
            member = "toApacheSetValuedMap";
            SetValuedMap<Integer, Integer> m3Sets = (parallel ? data.parallelStream() : data.stream())
                    .collect(M3LibraryCollectors.toApacheSetValuedMap(key, Function.identity()));
            same(sets.size(), m3Sets.size(), "size");
            for (Integer k : sets.keySet()) {
                same(sets.get(k), new LinkedHashSet<>(m3Sets.get(k)), "set " + k);
                same(new ArrayList<>(order.get(k)), new ArrayList<>(m3Sets.get(k)), "encounter order " + k);
            }
        }
        member = "toApacheListValuedMap";
        same(NullPointerException.class, thrown(() -> M3LibraryCollectors.toApacheListValuedMap(null, Function.identity())), "null keys");
        member = "toApacheSetValuedMap";
        same(NullPointerException.class, thrown(() -> M3LibraryCollectors.toApacheSetValuedMap(Function.identity(), null)), "null values");
    }

    /** toGuavaBiMap/toApacheBidiMap against HashBiMap/DualLinkedHashBidiMap; duplicates refused in every partition. */
    private static void bidiCollectors(Random random) {
        int n = 1 + random.nextInt(2000);
        List<Integer> keys = new ArrayList<>();
        for (int i = 0; i < n; i++) keys.add(i * 3 + random.nextInt(3));
        Collections.shuffle(keys, random);
        Function<Integer, String> value = k -> "v" + k;
        HashBiMap<Integer, String> guavaReference = HashBiMap.create();
        DualLinkedHashBidiMap<Integer, String> apacheReference = new DualLinkedHashBidiMap<>();
        for (Integer k : keys) { guavaReference.put(k, value.apply(k)); apacheReference.put(k, value.apply(k)); }
        for (boolean parallel : new boolean[] {false, true}) {
            member = "toGuavaBiMap";
            BiMap<Integer, String> guava = (parallel ? keys.parallelStream() : keys.stream())
                    .collect(M3LibraryCollectors.toGuavaBiMap(Function.identity(), value));
            same(guavaReference, guava, "map");
            same(guavaReference.inverse(), guava.inverse(), "inverse");
            same(keys, new ArrayList<>(guava.keySet()), "encounter order");
            member = "toApacheBidiMap";
            BidiMap<Integer, String> apache = (parallel ? keys.parallelStream() : keys.stream())
                    .collect(M3LibraryCollectors.toApacheBidiMap(Function.identity(), value));
            same(apacheReference, apache, "map");
            same(new ArrayList<>(apacheReference.keySet()), new ArrayList<>(apache.keySet()), "encounter order");
            for (Integer k : keys) same(apacheReference.getKey(value.apply(k)), apache.getKey(value.apply(k)), "inverse");
            int modulus = 1 + random.nextInt(Math.max(1, n - 1));
            boolean duplicates = n > modulus;
            member = "toGuavaBiMap";
            same(duplicates ? IllegalStateException.class : null, thrown(() -> (parallel ? keys.parallelStream() : keys.stream())
                    .collect(M3LibraryCollectors.toGuavaBiMap(k -> k % modulus, value))), "duplicate key");
            same(duplicates ? IllegalStateException.class : null, thrown(() -> (parallel ? keys.parallelStream() : keys.stream())
                    .collect(M3LibraryCollectors.toGuavaBiMap(Function.identity(), k -> k % modulus))), "duplicate value");
            member = "toApacheBidiMap";
            same(duplicates ? IllegalStateException.class : null, thrown(() -> (parallel ? keys.parallelStream() : keys.stream())
                    .collect(M3LibraryCollectors.toApacheBidiMap(Function.identity(), k -> k % modulus))), "duplicate value");
            same(NullPointerException.class, thrown(() -> keys.stream()
                    .collect(M3LibraryCollectors.toApacheBidiMap(Function.identity(), k -> null))), "null value");
        }
    }

    /** toEclipseOrderedMap against OrderedMapAdapter over java.util.LinkedHashMap. */
    private static void orderedMapCollector(Random random) {
        int n = 1 + random.nextInt(500);
        List<Integer> keys = new ArrayList<>(new LinkedHashSet<>(data(random, n, 4 * n, false)));
        MutableOrderedMap<Integer, String> expected = OrderedMapAdapter.adapt(new LinkedHashMap<>());
        for (Integer k : keys) expected.put(k, "v" + k);
        for (boolean parallel : new boolean[] {false, true}) {
            MutableOrderedMap<Integer, String> map = (parallel ? keys.parallelStream() : keys.stream())
                    .collect(M3LibraryCollectors.toEclipseOrderedMap(Function.identity(), k -> "v" + k));
            same(new ArrayList<>(expected.keySet()), new ArrayList<>(map.keySet()), "key order");
            same(expected.getFirst(), map.getFirst(), "first");
            same(expected.getLast(), map.getLast(), "last");
            same(expected, map, "equals");
            if (keys.size() > 1) {
                same(IllegalStateException.class, thrown(() -> (parallel ? keys.parallelStream() : keys.stream())
                        .collect(M3LibraryCollectors.toEclipseOrderedMap(k -> 0, k -> "v" + k))), "duplicate key");
            }
        }
    }

    /** guavaMultiset() against LinkedHashMultiset under seeded add/remove/setCount streams (return values too). */
    private static void guavaFactory(Random random) {
        Multiset<Integer> m3 = M3LibraryCollections.guavaMultiset();
        Multiset<Integer> reference = LinkedHashMultiset.create();
        same(true, M3LibraryCollections.<Integer>guavaMultiset() != m3, "independent instances");
        for (int op = 0; op < 400; op++) {
            Integer e = random.nextInt(12) == 0 ? null : random.nextInt(25);
            int n = random.nextInt(4);
            switch (random.nextInt(5)) {
                case 0 -> same(reference.add(e, n), m3.add(e, n), "add");
                case 1 -> same(reference.remove(e, n), m3.remove(e, n), "remove");
                case 2 -> same(reference.setCount(e, n), m3.setCount(e, n), "setCount");
                case 3 -> { int old = reference.count(e); same(reference.setCount(e, old, n), m3.setCount(e, old, n), "cas"); }
                default -> same(reference.add(e), m3.add(e), "add1");
            }
            same(reference.size(), m3.size(), "size");
            same(reference.count(e), m3.count(e), "count");
        }
        same(new ArrayList<>(reference.elementSet()), new ArrayList<>(m3.elementSet()), "elementSet order");
        same(reference, m3, "equals");
        same(reference.hashCode(), m3.hashCode(), "hashCode");
    }

    /** apacheMultiSet() against HashMultiSet (old-count returns). */
    private static void apacheMultiSetFactory(Random random) {
        MultiSet<Integer> m3 = M3LibraryCollections.apacheMultiSet();
        MultiSet<Integer> reference = new HashMultiSet<>();
        for (int op = 0; op < 400; op++) {
            Integer e = random.nextInt(12) == 0 ? null : random.nextInt(25);
            int n = random.nextInt(4);
            switch (random.nextInt(4)) {
                case 0 -> same(reference.add(e, n), m3.add(e, n), "add");
                case 1 -> same(reference.remove(e, n), m3.remove(e, n), "remove");
                case 2 -> same(reference.setCount(e, n), m3.setCount(e, n), "setCount");
                default -> same(reference.add(e), m3.add(e), "add1");
            }
            same(reference.size(), m3.size(), "size");
            same(reference.getCount(e), m3.getCount(e), "count");
        }
        same(reference.uniqueSet(), new java.util.HashSet<>(m3.uniqueSet()), "uniqueSet");
        same(reference, m3, "equals");
    }

    /** apacheBag() against HashBag (legacy remove(element) removes every copy). */
    @SuppressWarnings("deprecation")
    private static void apacheBagFactory(Random random) {
        Bag<Integer> m3 = M3LibraryCollections.apacheBag();
        Bag<Integer> reference = new HashBag<>();
        for (int op = 0; op < 400; op++) {
            Integer e = random.nextInt(12) == 0 ? null : random.nextInt(25);
            int n = 1 + random.nextInt(3);
            switch (random.nextInt(4)) {
                case 0 -> same(reference.add(e, n), m3.add(e, n), "add");
                case 1 -> same(reference.remove(e, n), m3.remove(e, n), "remove n");
                case 2 -> same(reference.remove(e), m3.remove(e), "remove all");
                default -> same(reference.add(e), m3.add(e), "add1");
            }
            same(reference.size(), m3.size(), "size");
            same(reference.getCount(e), m3.getCount(e), "count");
        }
        same(reference.uniqueSet(), new java.util.HashSet<>(m3.uniqueSet()), "uniqueSet");
    }

    /** eclipseBag() against Eclipse HashBag. */
    private static void eclipseBagFactory(Random random) {
        MutableBag<Integer> m3 = M3LibraryCollections.eclipseBag();
        MutableBag<Integer> reference = org.eclipse.collections.impl.bag.mutable.HashBag.newBag();
        for (int op = 0; op < 400; op++) {
            Integer e = random.nextInt(12) == 0 ? null : random.nextInt(25);
            int n = random.nextInt(4);
            switch (random.nextInt(4)) {
                case 0 -> same(reference.addOccurrences(e, n), m3.addOccurrences(e, n), "addOccurrences");
                case 1 -> same(reference.removeOccurrences(e, n), m3.removeOccurrences(e, n), "removeOccurrences");
                case 2 -> same(reference.setOccurrences(e, n), m3.setOccurrences(e, n), "setOccurrences");
                default -> same(reference.add(e), m3.add(e), "add1");
            }
            same(reference.size(), m3.size(), "size");
            same(reference.occurrencesOf(e), m3.occurrencesOf(e), "occurrences");
        }
        same(reference.sizeDistinct(), m3.sizeDistinct(), "distinct");
        same(reference, m3, "equals");
    }

    /** eclipseOrderedMap() against OrderedMapAdapter over java.util.LinkedHashMap (null key/value supported). */
    private static void eclipseOrderedMapFactory(Random random) {
        MutableOrderedMap<Integer, String> m3 = M3LibraryCollections.eclipseOrderedMap();
        MutableOrderedMap<Integer, String> reference = OrderedMapAdapter.adapt(new LinkedHashMap<>());
        same(true, M3LibraryCollections.<Integer, String>eclipseOrderedMap() != m3, "independent instances");
        for (int op = 0; op < 400; op++) {
            Integer k = random.nextInt(12) == 0 ? null : random.nextInt(40);
            String v = random.nextInt(15) == 0 ? null : "v" + random.nextInt(9);
            switch (random.nextInt(4)) {
                case 0, 1 -> same(reference.put(k, v), m3.put(k, v), "put");
                case 2 -> same(reference.remove(k), m3.remove(k), "remove");
                default -> same(reference.containsKey(k), m3.containsKey(k), "containsKey");
            }
        }
        same(new ArrayList<>(reference.keySet()), new ArrayList<>(m3.keySet()), "key order");
        same(new ArrayList<>(reference.values()), new ArrayList<>(m3.values()), "value order");
        if (!reference.isEmpty()) {
            same(reference.getFirst(), m3.getFirst(), "first");
            same(reference.getLast(), m3.getLast(), "last");
        }
        Supplier<Set<Integer>> keys = m3::keySet;
        same(reference.keySet(), keys.get(), "keySet");
    }
}
