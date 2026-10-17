// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.stream.StreamSupport;

/** Executable public-API oracle for live map cursor projections. */
public final class M3FilteredMapsCursorProof {
    private static int checks;

    private M3FilteredMapsCursorProof() { }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }

    private static <T> List<T> consume(Spliterator<T> source) {
        ArrayList<T> out = new ArrayList<>();
        source.forEachRemaining(out::add);
        return out;
    }

    private static <T> List<T> splitConsume(Spliterator<T> source) {
        Spliterator<T> prefix = source.trySplit();
        ArrayList<T> out = new ArrayList<>();
        if (prefix != null) prefix.forEachRemaining(out::add);
        source.forEachRemaining(out::add);
        return out;
    }

    private static <T> Map<T, Integer> multiset(Iterable<T> values) {
        Map<T, Integer> result = new HashMap<>();
        for (T value : values) result.merge(value, 1, Integer::sum);
        return result;
    }

    private static void expectIllegalComparator(Spliterator<?> source) {
        try {
            source.getComparator();
            throw new AssertionError("mapped cursor falsely reported SORTED");
        } catch (IllegalStateException expected) {
            checks++;
        }
    }

    private static void round(int seed, Map<Integer, Integer> source) {
        BiPredicate<Integer, Integer> admitted = (key, value) ->
                (key & 3) != 0 && (value == null || value % 5 != 0);
        Map<Integer, Integer> view = M3FilteredMaps.filtered(source, admitted);
        ArrayList<Integer> expectedKeys = new ArrayList<>();
        ArrayList<Integer> expectedValues = new ArrayList<>();
        ArrayList<String> expectedEntries = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : source.entrySet()) {
            if (admitted.test(entry.getKey(), entry.getValue())) {
                expectedKeys.add(entry.getKey());
                expectedValues.add(entry.getValue());
                expectedEntries.add(entry.toString());
            }
        }
        ArrayList<String> observedEntries = new ArrayList<>();
        view.entrySet().spliterator().forEachRemaining(e -> observedEntries.add(e.toString()));
        check(expectedEntries.equals(observedEntries), "entry projection order " + seed);
        check(expectedKeys.equals(consume(view.keySet().spliterator())), "key projection " + seed);
        check(expectedValues.equals(consume(view.values().spliterator())), "value projection " + seed);
        check(multiset(expectedKeys).equals(multiset(
                splitConsume(view.keySet().spliterator()))), "key splitting " + seed);
        check(multiset(expectedValues).equals(multiset(
                splitConsume(view.values().spliterator()))), "value splitting " + seed);
        List<Integer> parallel = StreamSupport.stream(view.keySet().spliterator(), true).toList();
        check(multiset(expectedKeys).equals(multiset(parallel)), "parallel key projection " + seed);
        Spliterator<Integer> keys = view.keySet().spliterator();
        Spliterator<Integer> values = view.values().spliterator();
        Spliterator<Map.Entry<Integer, Integer>> entries = view.entrySet().spliterator();
        if (source instanceof NavigableMap<?, ?> navigation) {
            // Existing navigableKeySet() reuses the original sorted collection
            // view rather than generic Map Entry::getKey projection; preserve it.
            check(keys.hasCharacteristics(Spliterator.SORTED | Spliterator.DISTINCT
                    | Spliterator.ORDERED), "navigable key characteristics lost");
            check(Objects.equals(keys.getComparator(), navigation.comparator()),
                    "navigable key comparator identity lost");
        } else {
            check((keys.characteristics() & (Spliterator.SORTED | Spliterator.DISTINCT
                    | Spliterator.CONCURRENT | Spliterator.IMMUTABLE | Spliterator.NONNULL)) == 0,
                    "generic mapped keys falsely advertised stronger characteristics");
        }
        check((values.characteristics() & (Spliterator.SORTED | Spliterator.DISTINCT
                | Spliterator.CONCURRENT | Spliterator.IMMUTABLE | Spliterator.NONNULL)) == 0,
                "mapped values falsely advertised stronger characteristics");
        check((entries.characteristics() & (Spliterator.SORTED | Spliterator.DISTINCT
                | Spliterator.CONCURRENT | Spliterator.IMMUTABLE | Spliterator.NONNULL)) == 0,
                "mapped entries falsely advertised stronger characteristics");
        if (!(source instanceof NavigableMap<?, ?>)) expectIllegalComparator(keys);
        expectIllegalComparator(values);
        expectIllegalComparator(entries);
        try {
            keys.tryAdvance(null);
            throw new AssertionError("null action accepted");
        } catch (NullPointerException expected) {
            checks++;
        }
        check(expectedKeys.equals(consume(keys)), "null action consumed a key");
        if (source instanceof NavigableMap<?, ?>) {
            check(view instanceof NavigableMap<?, ?>, "navigable capability erased");
            @SuppressWarnings("unchecked")
            NavigableMap<Integer, Integer> navigable = (NavigableMap<Integer, Integer>) view;
            check(Objects.equals(navigable.firstEntry() == null ? null :
                    navigable.firstEntry().getKey(),
                    expectedKeys.isEmpty() ? null : expectedKeys.getFirst()),
                    "navigation and projected order diverged");
        }
    }

    private static Map<Integer, Integer> map(int seed, int kind) {
        Map<Integer, Integer> result = switch (kind) {
            case 0 -> new LinkedHashMap<>();
            case 1 -> new HashMap<>();
            case 2 -> new TreeMap<>();
            default -> new TreeMap<>(Comparator.<Integer>reverseOrder());
        };
        int length = seed % 37;
        for (int i = 0; i < length; i++) {
            int key = ((i * 17 + seed) % 89) - 44;
            result.put(key, i % 7 == 0 ? null : i - 25);
        }
        return result;
    }

    public static void main(String[] arguments) {
        for (int kind = 0; kind < 4; kind++) {
            for (int seed = 0; seed < 128; seed++) {
                round(seed, map(seed, kind));
            }
        }
        Map<Integer, Integer> owner = new LinkedHashMap<>();
        owner.put(1, null);
        owner.put(2, 7);
        AtomicInteger calls = new AtomicInteger();
        Map<Integer, Integer> selection = M3FilteredMaps.filtered(owner,
                (key, value) -> { calls.incrementAndGet(); return true; });
        Spliterator<Integer> cursor = selection.keySet().spliterator();
        cursor.estimateSize();
        cursor.characteristics();
        cursor.trySplit();
        check(calls.get() == 0, "metadata eagerly invoked filter");
        check(new HashSet<>(selection.keySet()).equals(Set.of(1, 2)), "live selection");
        owner.put(3, 9);
        check(selection.containsKey(3), "owner write invisible after new traversal");
        System.out.println("M3_MAP_CURSOR_INTEGRATION_PASS checks=" + checks);
    }
}
