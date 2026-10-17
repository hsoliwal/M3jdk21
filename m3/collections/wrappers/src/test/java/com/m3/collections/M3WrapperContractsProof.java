/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.Method;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;

/** Independent finite JDK oracles; optional allocation-free, >2^31 occurrence size proof. */
public final class M3WrapperContractsProof {
    private static long checks;
    private M3WrapperContractsProof() { }
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    private static void equal(Object wanted, Object actual, String message) {
        check(Objects.equals(wanted, actual), message + ": expected=" + wanted + ", actual=" + actual);
    }
    private static <X extends Throwable> X thrown(Class<X> type, Runnable operation) {
        try { operation.run(); }
        catch (Throwable error) {
            check(type.isInstance(error), "wrong exception: " + error);
            return type.cast(error);
        }
        throw new AssertionError("missing " + type.getSimpleName());
    }
    private static void metadata(Spliterator<?> cursor, boolean distinct) {
        check(cursor.hasCharacteristics(Spliterator.DISTINCT) == distinct, "Set DISTINCT contract lost");
        check(!cursor.hasCharacteristics(Spliterator.SIZED), "filtered size guessed");
        check(!cursor.hasCharacteristics(Spliterator.SUBSIZED), "filtered child size guessed");
        equal(-1L, cursor.getExactSizeIfKnown(), "exact filtered cardinality");
    }
    private static <T> void drain(Spliterator<T> cursor, List<T> output, boolean distinct, int depth) {
        metadata(cursor, distinct);
        check(depth < 32, "non-terminating source splits");
        Spliterator<T> prefix = cursor.trySplit();
        if (prefix != null) {
            drain(prefix, output, distinct, depth + 1);
            drain(cursor, output, distinct, depth + 1);
        } else cursor.forEachRemaining(output::add);
    }
    private static final class Key {
        private static int hashes;
        private final int id;
        private Key(int id) { this.id = id; }
        @Override public int hashCode() { hashes++; return id; }
        @Override public boolean equals(Object other) { return other instanceof Key k && id == k.id; }
    }
    private static void projectionMetadata() {
        Map<Key, Integer> owner = new LinkedHashMap<>();
        for (int i = 0; i < 200; i++) owner.put(new Key(i), i % 3);
        AtomicInteger predicates = new AtomicInteger();
        Map<Key, Integer> view = M3FilteredMaps.filtered(owner, (k, v) -> {
            predicates.incrementAndGet(); return (k.id & 1) == 0;
        });
        Spliterator<Key> keys = view.keySet().spliterator();
        Spliterator<Map.Entry<Key, Integer>> entries = view.entrySet().spliterator();
        Spliterator<Integer> values = view.values().spliterator();
        metadata(keys, true); metadata(entries, true); metadata(values, false);
        equal(0, predicates.get(), "metadata evaluated predicate");
        thrown(IllegalStateException.class, entries::getComparator);
        thrown(IllegalStateException.class, keys::getComparator);
        thrown(NullPointerException.class, () -> keys.tryAdvance(null));
        thrown(NullPointerException.class, () -> entries.forEachRemaining(null));
        equal(0, predicates.get(), "null consumer advanced cursor");
        List<Key> splitKeys = new ArrayList<>(); drain(keys, splitKeys, true, 0);
        equal(100, splitKeys.size(), "split keys lost/duplicated");
        equal(100, new LinkedHashSet<>(splitKeys).size(), "duplicate split keys");
        List<Map.Entry<Key, Integer>> splitEntries = new ArrayList<>();
        drain(entries, splitEntries, true, 0);
        equal(splitKeys, splitEntries.stream().map(Map.Entry::getKey).toList(), "entry/key split order");
        equal(100L, view.values().parallelStream().count(), "value duplicates removed");
        equal(3L, view.values().parallelStream().distinct().count(), "values falsely DISTINCT");
        Key.hashes = 0;
        equal(100L, view.keySet().stream().distinct().count(), "key distinct count");
        equal(0, Key.hashes, "redundant key rehash after DISTINCT guarantee");
        Key.hashes = 0;
        equal(100L, view.entrySet().stream().distinct().count(), "entry distinct count");
        equal(0, Key.hashes, "redundant entry rehash after DISTINCT guarantee");
        var stream = view.keySet().stream();
        Key later = new Key(202); owner.put(later, 7);
        check(stream.toList().contains(later), "owner not live before traversal");
    }
    private static final class SortedOnly<K, V> extends AbstractMap<K, V> implements SortedMap<K, V> {
        private final SortedMap<K, V> owner;
        private SortedOnly(SortedMap<K, V> owner) { this.owner = owner; }
        @Override public Set<Entry<K, V>> entrySet() { return owner.entrySet(); }
        @Override public V put(K k, V v) { return owner.put(k, v); }
        @Override public Comparator<? super K> comparator() { return owner.comparator(); }
        @Override public K firstKey() { return owner.firstKey(); }
        @Override public K lastKey() { return owner.lastKey(); }
        @Override public SortedMap<K, V> subMap(K f, K t) { return new SortedOnly<>(owner.subMap(f, t)); }
        @Override public SortedMap<K, V> headMap(K t) { return new SortedOnly<>(owner.headMap(t)); }
        @Override public SortedMap<K, V> tailMap(K f) { return new SortedOnly<>(owner.tailMap(f)); }
    }
    private static Map<Integer, Integer> owner(int kind) {
        return switch (kind) {
            case 0 -> new LinkedHashMap<>();
            case 1 -> new HashMap<>();
            case 2 -> new TreeMap<>();
            case 3 -> new TreeMap<>(Comparator.reverseOrder());
            case 4 -> new SortedOnly<>(new TreeMap<>());
            default -> throw new AssertionError(kind);
        };
    }
    private static void randomized() {
        for (int seed = 0; seed < 192; seed++) for (int kind = 0; kind < 5; kind++) {
            Random random = new Random(0x33c011ecL + seed);
            Map<Integer, Integer> source = owner(kind);
            for (int i = 0; i < 150; i++) source.put(random.nextInt(97) - 48,
                    random.nextInt(7) == 0 ? null : random.nextInt(9) - 4);
            BiPredicate<Integer, Integer> first = (k, v) -> (k & 1) == 0;
            BiPredicate<Integer, Integer> second = (k, v) -> v == null || v >= 0;
            Map<Integer, Integer> view = M3FilteredMaps.filtered(
                    M3FilteredMaps.filtered(source, first), second);
            Map<Integer, Integer> oracle = new LinkedHashMap<>();
            source.forEach((k, v) -> { if (first.test(k, v) && second.test(k, v)) oracle.put(k, v); });
            equal(oracle, view, "map equality"); equal(view, oracle, "map equality reverse");
            equal(oracle.hashCode(), view.hashCode(), "map hash");
            equal(oracle.size(), view.size(), "map count");
            equal(oracle.isEmpty(), view.isEmpty(), "map empty");
            for (Integer value : new Integer[] {null, -4, -1, 0, 1, 4, 100})
                equal(oracle.containsValue(value), view.containsValue(value), "value membership");
            metadata(view.entrySet().spliterator(), true);
            metadata(view.keySet().spliterator(), true);
            metadata(view.values().spliterator(), false);
            equal(new ArrayList<>(oracle.values()), view.values().stream().toList(), "value order");
            List<Map.Entry<Integer, Integer>> split = new ArrayList<>();
            drain(view.entrySet().spliterator(), split, true, 0);
            equal(oracle.entrySet(), new LinkedHashSet<>(split), "entry split membership");
            if (kind != 1) equal(new ArrayList<>(oracle.entrySet()), split, "entry split prefix order");
            equal(oracle.size(), split.size(), "entry split cardinality");
            equal(oracle.values().stream().distinct().count(), view.values().parallelStream().distinct().count(), "parallel duplicate values");
            for (Map.Entry<Integer, Integer> e : view.entrySet()) {
                Integer saved = source.get(e.getKey());
                thrown(IllegalArgumentException.class, () -> e.setValue(-100));
                equal(saved, source.get(e.getKey()), "guard leak");
                break;
            }
            Map<Integer, Integer> hidden = new LinkedHashMap<>();
            source.forEach((k, v) -> { if (!first.test(k, v) || !second.test(k, v)) hidden.put(k, v); });
            view.values().removeIf(v -> v != null && v == 0);
            hidden.forEach((k, v) -> {
                check(source.containsKey(k), "hidden mapping lost");
                equal(v, source.get(k), "hidden mapping changed");
            });
            view.clear(); equal(hidden, source, "clear crossed filter boundary");
        }
    }
    private static void sortedAndPurposeWrappers() {
        Comparator<String> cmp = Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER);
        NavigableMap<String, Integer> owner = new TreeMap<>(cmp);
        owner.put(null, 2); owner.put("Alpha", 4); owner.put("beta", -1); owner.put("Gamma", 6);
        NavigableMap<String, Integer> view = M3FilteredMaps.filtered(owner, (k, v) -> v >= 0);
        check(view.comparator() == cmp, "comparator identity lost");
        equal(null, view.firstKey(), "null first key");
        equal("Gamma", view.lastKey(), "last visible key");
        equal(4, view.get("ALPHA"), "stored key equivalence lost");
        metadata(view.entrySet().spliterator(), true);
        NavigableMap<String, Integer> range = view.tailMap("alpha", true).descendingMap();
        equal(List.of("Gamma", "Alpha"), range.keySet().stream().toList(), "descending bounded keys");
        metadata(range.entrySet().spliterator(), true);
        NavigableSet<String> keys = range.navigableKeySet();
        check(keys.spliterator().hasCharacteristics(Spliterator.SORTED | Spliterator.ORDERED | Spliterator.DISTINCT), "sorted key metadata");
        equal(keys.comparator(), keys.spliterator().getComparator(), "sorted comparator");
        equal(List.of("Alpha", "Gamma"), keys.reversed().stream().toList(), "JDK21 reversed keys");
        thrown(IllegalArgumentException.class, () -> view.entrySet().removeIf(e -> { e.setValue(-7); return false; }));
        equal(2, owner.get(null), "predicate received unsafe raw entry");
        Map<String, Integer> readOnly = Collections.unmodifiableMap(view);
        thrown(UnsupportedOperationException.class, () -> readOnly.entrySet().iterator().next().setValue(10));
        Map<String, Integer> checked = Collections.checkedMap(view, String.class, Integer.class);
        checked.put("Delta", 10); equal(10, view.get("delta"), "checked write-through");
        Map<String, Integer> synchronizedView = Collections.synchronizedMap(view);
        synchronized (synchronizedView) {
            equal(view.hashCode(), synchronizedView.hashCode(), "synchronized raw hash");
            equal(view.size(), synchronizedView.size(), "synchronized count");
        }
        range.clear(); check(owner.containsKey("beta"), "hidden sorted key removed");
        equal(2, owner.get(null), "out-of-range null key removed");
        record Row(int key, String name) { }
        var a = new Row(2, "a"); var b = new Row(2, "b"); var c = new Row(1, "c");
        List<Row> rows = new ArrayList<>(List.of(a, b, c, a));
        List<Row> snapshot = M3CollectionViews.filteredSortedSnapshot(rows, r -> true, Comparator.comparingInt(Row::key));
        equal(List.of(c, a, b, a), snapshot, "stable duplicate sorted snapshot");
        check(snapshot.get(1) == a && snapshot.get(3) == a, "snapshot reference identity");
        rows.clear(); equal(4, snapshot.size(), "detached snapshot became live");
        thrown(UnsupportedOperationException.class, () -> snapshot.add(c));
        NavigableSet<Integer> setOwner = new TreeSet<>(List.of(-3,-2,-1,0,1,2,3));
        NavigableSet<Integer> filtered = M3CollectionViews.filtered(setOwner, x -> (x & 1) == 0);
        equal(List.of(2,0,-2), filtered.descendingSet().stream().toList(), "filtered sorted set order");
        equal(0, filtered.subSet(-1, true, 2, false).getFirst(), "filtered sorted range");
        filtered.clear(); equal(Set.of(-3,-1,1,3), setOwner, "filtered set hidden survival");
    }
    private static void exceptionAndScanOrder() {
        Map<Integer, Integer> owner = new LinkedHashMap<>();
        owner.put(0, null); owner.put(1, 11); owner.put(2, 12); owner.put(3, 13);
        List<Integer> visits = new ArrayList<>();
        Map<Integer, Integer> view = M3FilteredMaps.filtered(owner, (k, v) -> { visits.add(k); return (k & 1) == 0; });
        equal(2, view.size(), "scan size"); equal(List.of(0,1,2,3), visits, "size predicate order");
        visits.clear(); check(!view.isEmpty(), "nonempty"); equal(List.of(0), visits, "empty short circuit");
        visits.clear(); check(view.containsValue(null), "visible null"); equal(List.of(0), visits, "membership short circuit");
        visits.clear(); view.hashCode(); equal(List.of(0,1,2,3), visits, "hash predicate order");
        RuntimeException sentinel = new IllegalStateException("predicate sentinel");
        Map<Integer, Integer> failing = M3FilteredMaps.filtered(owner, (k,v) -> { if (k == 2) throw sentinel; return true; });
        check(thrown(RuntimeException.class, failing::size) == sentinel, "size exception identity");
        check(thrown(RuntimeException.class, failing::hashCode) == sentinel, "hash exception identity");
        check(thrown(RuntimeException.class, () -> failing.containsValue(99)) == sentinel, "value exception identity");
    }
    private static void arithmeticBoundaries() throws Exception {
        Method atom = M3CollectionViews.class.getDeclaredMethod("incrementSize", int.class);
        atom.setAccessible(true);
        for (int n : new int[] {0,1,2,1024,Integer.MAX_VALUE-2,Integer.MAX_VALUE-1,Integer.MAX_VALUE}) {
            int wanted = (int) Math.min(Integer.MAX_VALUE, ((long) n)+1L);
            equal(wanted, atom.invoke(null, n), "saturating increment boundary");
        }
    }
    private static final class Repeated extends AbstractCollection<Integer> {
        private final long occurrences;
        private Repeated(long occurrences) { this.occurrences = occurrences; }
        @Override public int size() { throw new AssertionError("source.size prepass"); }
        @Override public Iterator<Integer> iterator() {
            return new Iterator<>() {
                private long remaining = occurrences;
                @Override public boolean hasNext() { return remaining != 0; }
                @Override public Integer next() {
                    if (remaining == 0) throw new NoSuchElementException();
                    remaining--; return Integer.valueOf(1);
                }
            };
        }
    }
    private static void large() {
        long occurrences = (long) Integer.MAX_VALUE + 17;
        long[] calls = {0};
        Collection<Integer> view = M3CollectionViews.filtered(new Repeated(occurrences), n -> { calls[0]++; return true; });
        equal(Integer.MAX_VALUE, view.size(), "Collection.size overflow");
        equal(occurrences, calls[0], "size skipped predicate occurrences");
        System.out.println("M3_WRAPPER_LARGE_PASS occurrences=" + occurrences + " checks=" + checks);
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--large")) { large(); return; }
        projectionMetadata(); randomized(); sortedAndPurposeWrappers(); exceptionAndScanOrder(); arithmeticBoundaries();
        System.out.println("M3_WRAPPER_CONTRACTS_PASS checks=" + checks + " seeds=192 ownerProfiles=5");
    }
}
