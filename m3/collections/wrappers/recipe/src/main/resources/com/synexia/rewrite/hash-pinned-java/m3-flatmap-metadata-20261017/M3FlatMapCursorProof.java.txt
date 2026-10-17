// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/** Executable owner-specific JDK21 flatMap lazy traversal and metadata oracle. */
public final class M3FlatMapCursorProof {
    private static int checks;

    private M3FlatMapCursorProof() { }

    private static void check(boolean value, String description) {
        if (!value) throw new AssertionError(description);
        checks++;
    }

    private static void throwsType(Class<? extends Throwable> expected, Runnable body,
                                   String label) {
        try {
            body.run();
            throw new AssertionError("unexpected acceptance: " + label);
        } catch (Throwable actual) {
            if (!expected.isInstance(actual)) throw new AssertionError(label, actual);
            checks++;
        }
    }

    private static <T> Map<T, Integer> multiset(Collection<T> elements) {
        Map<T, Integer> result = new HashMap<>();
        for (T value : elements) result.merge(value, 1, Integer::sum);
        return result;
    }

    private static void lazyMetadataAndEarlyTermination() {
        AtomicInteger sourceSizeCalls = new AtomicInteger();
        AtomicInteger mapperCalls = new AtomicInteger();
        AtomicInteger sourceIteratorCalls = new AtomicInteger();
        Collection<Integer> source = new AbstractCollection<>() {
            @Override public int size() {
                sourceSizeCalls.incrementAndGet();
                throw new AssertionError("source.size() must never serve flatMap stream metadata");
            }
            @Override public Iterator<Integer> iterator() {
                sourceIteratorCalls.incrementAndGet();
                return List.of(1, 2, 3).iterator();
            }
        };
        Collection<Integer> view = M3CollectionWrappers.flatMappedView(
                source, number -> {
                    mapperCalls.incrementAndGet();
                    return List.of(number, number * 10);
                });
        check(mapperCalls.get() == 0 && sourceIteratorCalls.get() == 0,
                "flatMap construction traversed source");
        Spliterator<Integer> cursor = view.spliterator();
        check(cursor.estimateSize() == Long.MAX_VALUE, "flattened exact size was guessed");
        check((cursor.characteristics() & (Spliterator.SIZED | Spliterator.SUBSIZED
                | Spliterator.SORTED | Spliterator.DISTINCT | Spliterator.NONNULL)) == 0,
                "flattened cursor advertised invalid metadata");
        check(cursor.trySplit() == null, "flatMap cursor must not buffer chunks by splitting");
        check(mapperCalls.get() == 0 && sourceIteratorCalls.get() == 0,
                "metadata bound mapper or iterator");
        check(sourceSizeCalls.get() == 0, "metadata computed source.size()");
        check(view.stream().findFirst().orElseThrow() == 1, "findFirst contract");
        check(mapperCalls.get() == 1, "findFirst invoked mapper beyond first outer item");
        check(sourceIteratorCalls.get() == 1, "findFirst traversed source multiple times");
        mapperCalls.set(0);
        check(view.stream().limit(3).toList().equals(List.of(1, 10, 2)),
                "limit kept the wrong flattened order");
        check(mapperCalls.get() == 2, "limit eagerly flattened unneeded outer items");
        mapperCalls.set(0);
        check(view.stream().count() == 6, "unsized count omitted results");
        check(mapperCalls.get() == 3, "count reran mapper for cardinality");
        check(sourceSizeCalls.get() == 0, "stream counted by source.size()");
        mapperCalls.set(0);
        check(view.parallelStream().toList().equals(List.of(1, 10, 2, 20, 3, 30)),
                "parallel read not equivalent to ordered sequential traversal");
        check(mapperCalls.get() == 3, "parallel stream recomputed mapper");
        throwsType(UnsupportedOperationException.class, view::clear,
                "flatMap read-only mutation guard");
    }

    private static void orderedAndUnorderedCharacteristics() {
        Collection<Integer> list = List.of(3, 1, 2);
        Collection<Integer> linked = new LinkedHashSet<>(list);
        Collection<Integer> unsorted = new HashSet<>(list);
        check(M3CollectionWrappers.flatMappedView(list, List::of).spliterator()
                        .hasCharacteristics(Spliterator.ORDERED),
                "list's encounter order was dropped");
        check(M3CollectionWrappers.flatMappedView(linked, List::of).spliterator()
                        .hasCharacteristics(Spliterator.ORDERED),
                "LinkedHashSet's encounter order was dropped");
        check(!M3CollectionWrappers.flatMappedView(unsorted, List::of).spliterator()
                        .hasCharacteristics(Spliterator.ORDERED),
                "HashSet's unspecified iteration order falsely advertised ORDERED");
        Collection<Integer> repeated = M3CollectionWrappers.flatMappedView(
                List.of(1), n -> Arrays.asList(n, n));
        check(!repeated.spliterator().hasCharacteristics(Spliterator.DISTINCT),
                "flattened duplicate elements falsely advertised DISTINCT");
        Collection<Integer> nullable = M3CollectionWrappers.flatMappedView(
                List.of(1), n -> Arrays.asList(n, null));
        check(!nullable.spliterator().hasCharacteristics(Spliterator.NONNULL),
                "flattened nullable results falsely advertised NONNULL");
        check(new ArrayList<>(nullable).equals(Arrays.asList(1, null)),
                "existing public iterator null behavior changed");
    }

    private static Collection<Integer> source(int kind, List<Integer> values) {
        return switch (kind) {
            case 0 -> new ArrayList<>(values);
            case 1 -> new LinkedHashSet<>(values);
            case 2 -> new HashSet<>(values);
            case 3 -> new TreeSet<>(values);
            default -> new java.util.LinkedList<>(values);
        };
    }

    private static void randomizedDifferential() {
        for (int kind = 0; kind < 5; kind++) {
            for (int seed = 0; seed < 192; seed++) {
                Random random = new Random(kind * 53_121L + seed * 1_073L);
                List<Integer> items = new ArrayList<>();
                for (int i = 0; i < seed % 23; i++) items.add(random.nextInt(17) - 8);
                Collection<Integer> backing = source(kind, items);
                Function<Integer, List<Integer>> mapper = input -> switch (Math.floorMod(input, 4)) {
                    case 0 -> Collections.emptyList();
                    case 1 -> Collections.singletonList(input);
                    case 2 -> Arrays.asList(input, -input);
                    default -> Arrays.asList(input, input, input + 1);
                };
                List<Integer> expected = new ArrayList<>();
                for (Integer value : backing) expected.addAll(mapper.apply(value));
                Collection<Integer> flat = M3CollectionWrappers.flatMappedView(backing, mapper);
                check(new ArrayList<>(flat).equals(expected), "sequential flatMap " + kind + "/" + seed);
                check(flat.stream().toList().equals(expected), "stream traversal " + kind + "/" + seed);
                check(flat.size() == expected.size(), "size contract " + kind + "/" + seed);
                check(flat.stream().count() == expected.size(), "count contract " + kind + "/" + seed);
                check(multiset(flat.parallelStream().toList()).equals(multiset(expected)),
                        "parallel result membership " + kind + "/" + seed);
                check(flat.spliterator().estimateSize() == Long.MAX_VALUE,
                        "unsized estimate contract " + kind + "/" + seed);
                check(flat.spliterator().trySplit() == null,
                        "unsplit buffered traversal " + kind + "/" + seed);
                check(flat.spliterator().hasCharacteristics(Spliterator.ORDERED)
                                == backing.spliterator().hasCharacteristics(Spliterator.ORDERED),
                        "owner ORDERED metadata " + kind + "/" + seed);
                final int frozenSeed = seed;
                throwsType(UnsupportedOperationException.class, () -> flat.add(frozenSeed),
                        "mapped read-only collection");
                backing.clear();
                check(flat.isEmpty(), "live backing mutation " + kind + "/" + seed);
            }
        }
    }

    private static void errorAndConsumerContracts() {
        Collection<Integer> nulls = M3CollectionWrappers.flatMappedView(
                List.of(1), n -> null);
        check(nulls.spliterator().estimateSize() == Long.MAX_VALUE,
                "null mapper result evaluated at metadata time");
        throwsType(NullPointerException.class, () -> nulls.stream().findFirst(),
                "null inner iterable must fail at traversal");
        Collection<Integer> view = M3CollectionWrappers.flatMappedView(
                List.of(2), n -> List.of(n));
        Spliterator<Integer> cursor = view.spliterator();
        throwsType(NullPointerException.class, () -> cursor.tryAdvance(null),
                "null action before consumption");
        check(cursor.tryAdvance(x -> check(x == 2, "value after null action")),
                "cursor was incorrectly consumed on null action");
        check(!cursor.tryAdvance(x -> { throw new AssertionError("exhausted"); }),
                "exhausted cursor returned true");
        throwsType(NullPointerException.class, () -> view.spliterator().forEachRemaining(null),
                "null forEach action before iteration");
        Spliterator<Integer> denied = view.spliterator();
        throwsType(IllegalStateException.class, denied::getComparator,
                "non SORTED flatMap getComparator contract");
        AtomicInteger seen = new AtomicInteger();
        Collection<Integer> failing = M3CollectionWrappers.flatMappedView(
                List.of(3, 4), n -> {
                    if (n == 4) throw new IllegalArgumentException("fail");
                    return List.of(n);
                });
        throwsType(IllegalArgumentException.class,
                () -> failing.spliterator().forEachRemaining(n -> seen.incrementAndGet()),
                "source mapper failure propagated");
        check(seen.get() == 1, "exception altered delivered prefix");
    }

    public static void main(String[] ignored) {
        lazyMetadataAndEarlyTermination();
        orderedAndUnorderedCharacteristics();
        randomizedDifferential();
        errorAndConsumerContracts();
        System.out.println("M3_FLAT_MAP_CURSOR_PASS checks=" + checks);
    }
}
