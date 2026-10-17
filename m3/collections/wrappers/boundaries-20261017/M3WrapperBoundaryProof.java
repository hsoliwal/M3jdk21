/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/** Executable oracle for existing wrapper owners; no replacement collection backend. */
public final class M3WrapperBoundaryProof {
    private static long checks;
    private M3WrapperBoundaryProof() { }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }
    private static void equal(Object expected, Object actual) {
        check(Objects.equals(expected, actual), "expected=" + expected + ", actual=" + actual);
    }
    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        try {
            operation.run();
        } catch (Throwable error) {
            check(type.isInstance(error), "unexpected " + error);
            return;
        }
        throw new AssertionError("missing " + type.getName());
    }
    private static final class Sequential<E> extends AbstractSequentialList<E> {
        private final LinkedList<E> data;
        private long indexedReads;
        Sequential(Collection<E> values) { data = new LinkedList<>(values); }
        @Override public int size() { return data.size(); }
        @Override public ListIterator<E> listIterator(int index) { return data.listIterator(index); }
        @Override public E get(int index) { indexedReads++; return super.get(index); }
    }
    private static void listOracle() {
        Random random = new Random(0x6d335f776170L);
        Function<Integer, Integer> mapping = value -> value == null ? null : -value;
        for (int trial = 0; trial < 500; trial++) {
            List<Integer> data = new ArrayList<>();
            for (int i = 0, n = random.nextInt(129); i < n; i++) {
                data.add(random.nextInt(8) == 0 ? null : random.nextInt(17) - 8);
            }
            for (List<Integer> source : List.of(new ArrayList<>(data), new LinkedList<>(data))) {
                List<Integer> expected = source.stream().map(mapping).toList();
                List<Integer> actual = M3MappedViews.list(source, mapping);
                equal(expected, actual);
                equal(expected, new ArrayList<>(actual));
                equal(expected.reversed(), actual.reversed());
                equal(source instanceof RandomAccess, actual instanceof RandomAccess);
                ListIterator<Integer> iterator = actual.listIterator(actual.size());
                for (int i = expected.size() - 1; i >= 0; i--) {
                    equal(i + 1, iterator.nextIndex());
                    equal(i, iterator.previousIndex());
                    equal(expected.get(i), iterator.previous());
                }
                expect(NoSuchElementException.class, iterator::previous);
                int from = random.nextInt(data.size() + 1);
                int to = from + random.nextInt(data.size() - from + 1);
                equal(expected.subList(from, to), actual.subList(from, to));
                if (!data.isEmpty()) {
                    source.set(0, 314);
                    equal(-314, actual.getFirst());
                    List<Integer> tail = actual.subList(0, actual.size());
                    source.set(0, 271);
                    equal(-271, tail.getFirst());
                }
                expect(UnsupportedOperationException.class, () -> actual.add(1));
                expect(UnsupportedOperationException.class, actual::clear);
                expect(UnsupportedOperationException.class, () -> actual.listIterator().add(1));
                expect(IndexOutOfBoundsException.class, () -> actual.subList(-1, 0));
                expect(IllegalArgumentException.class, () -> actual.subList(1, 0));
            }
        }
        Sequential<Integer> source = new Sequential<>(Collections.nCopies(20000, 7));
        List<Integer> mapped = M3MappedViews.list(source, value -> value + 1);
        long start = System.nanoTime();
        long sum = 0;
        for (int value : mapped) sum += value;
        long elapsed = System.nanoTime() - start;
        equal(160000L, sum);
        equal(0L, source.indexedReads);
        equal(List.of(8, 8, 8), mapped.subList(2, 5));
        equal(0L, source.indexedReads);
        System.out.println("M3_WRAPPER_COST elements=20000 indexed_gets=" + source.indexedReads
                + " elapsed_ns=" + elapsed + " (one local observation, not a benchmark claim)");
        List<Integer> mutable = new LinkedList<>(List.of(1, 2, 3));
        Iterator<Integer> active = M3MappedViews.list(mutable, Function.identity()).iterator();
        mutable.add(4);
        expect(ConcurrentModificationException.class, active::next);
    }
    private static void flattenOracle() {
        Random random = new Random(0x6d335f666c6174L);
        for (int trial = 0; trial < 700; trial++) {
            List<List<Integer>> groups = new ArrayList<>();
            List<Integer> expected = new ArrayList<>();
            for (int i = 0, n = random.nextInt(70); i < n; i++) {
                List<Integer> group = new ArrayList<>();
                for (int j = 0, size = random.nextInt(8); j < size; j++) {
                    group.add(random.nextInt(7) == 0 ? null : random.nextInt(13));
                }
                groups.add(group); expected.addAll(group);
            }
            Collection<Integer> actual = M3MappedViews.flatMapped(groups, Function.identity());
            equal(expected.size(), actual.size());
            equal(expected.isEmpty(), actual.isEmpty());
            equal(expected, new ArrayList<>(actual));
            equal(expected, actual.stream().toList());
            equal((long) expected.size(), actual.parallelStream().count());
            ArrayList<Integer> splitValues = new ArrayList<>();
            drainSplit(actual.spliterator(), splitValues, 0);
            equal(expected, splitValues);
            expect(UnsupportedOperationException.class, () -> actual.add(1));
            expect(UnsupportedOperationException.class, actual::clear);
            Iterator<Integer> iterator = actual.iterator();
            iterator.forEachRemaining(ignored -> { });
            expect(NoSuchElementException.class, iterator::next);
            expect(NullPointerException.class, () -> actual.spliterator().tryAdvance(null));
        }
    }
    private static <E> void drainSplit(Spliterator<E> remaining, List<E> output, int depth) {
        Spliterator<E> prefix = depth < 12 ? remaining.trySplit() : null;
        if (prefix != null) drainSplit(prefix, output, depth + 1);
        remaining.forEachRemaining(output::add);
    }
    private static void metadataAndFailures() {
        AtomicInteger sourceIterations = new AtomicInteger();
        AtomicInteger maps = new AtomicInteger();
        List<Integer> owner = new ArrayList<>(List.of(4, 5));
        Collection<Integer> noSize = new AbstractCollection<>() {
            @Override public int size() { throw new AssertionError("source size prepass"); }
            @Override public Iterator<Integer> iterator() {
                sourceIterations.incrementAndGet(); return owner.iterator();
            }
        };
        Collection<Integer> flat = M3MappedViews.flatMapped(noSize, value -> {
            maps.incrementAndGet(); return Arrays.asList(value, null, value);
        });
        Spliterator<Integer> cursor = flat.spliterator();
        equal(Long.MAX_VALUE, cursor.estimateSize());
        equal(-1L, cursor.getExactSizeIfKnown());
        check(!cursor.hasCharacteristics(Spliterator.SIZED), "unknown cardinality");
        equal(0, cursor.characteristics() & (Spliterator.SIZED | Spliterator.SUBSIZED
                | Spliterator.SORTED | Spliterator.DISTINCT | Spliterator.NONNULL));
        equal(0, maps.get()); equal(0, sourceIterations.get());
        expect(NullPointerException.class, () -> cursor.tryAdvance(null));
        equal(0, maps.get()); equal(0, sourceIterations.get());
        owner.add(6); // delayed binding retains changes before traversal
        List<Integer> results = new ArrayList<>();
        cursor.forEachRemaining(results::add);
        equal(Arrays.asList(4, null, 4, 5, null, 5, 6, null, 6), results);
        equal(3, maps.get()); equal(1, sourceIterations.get());
        Collection<Integer> enormous = M3MappedViews.flatMapped(List.of(1), ignored ->
                () -> new Iterator<Integer>() {
                    private int consumed;
                    @Override public boolean hasNext() { return true; }
                    @Override public Integer next() {
                        if (++consumed > 3) throw new AssertionError("unexpected eager traversal");
                        return 17;
                    }
                });
        check(!enormous.isEmpty(), "prefix emptiness query");
        equal(17, enormous.stream().findFirst().orElseThrow());
        equal(List.of(17, 17), enormous.stream().limit(2).toList());
        RuntimeException marker = new IllegalStateException("mapper marker");
        Collection<Integer> broken = M3MappedViews.flatMapped(List.of(1), value -> { throw marker; });
        try { broken.stream().findFirst(); throw new AssertionError("lost failure"); }
        catch (RuntimeException error) { check(error == marker, "exception identity"); }
        expect(NullPointerException.class, () -> M3MappedViews.flatMapped(List.of(1), x -> null).isEmpty());
        Collection<Integer> empty = M3MappedViews.flatMapped(List.<Integer>of(), x -> List.of(x));
        check(empty.isEmpty(), "empty");
        expect(NullPointerException.class, () -> empty.spliterator().tryAdvance(null));
        expect(UnsupportedOperationException.class, () -> flat.iterator().remove());
    }
    /** Executes the actual public boundary rather than shrinking MAX_VALUE for a test. */
    private static void overflow() {
        long[] consumed = {0};
        long cardinality = (long) Integer.MAX_VALUE + 5;
        Iterable<Integer> huge = () -> new Iterator<>() {
            private long left = cardinality;
            @Override public boolean hasNext() { return left > 0; }
            @Override public Integer next() {
                if (left == 0) throw new NoSuchElementException();
                left--; consumed[0]++; return 1;
            }
        };
        long start = System.nanoTime();
        equal(Integer.MAX_VALUE, M3MappedViews.flatMapped(List.of(huge), Function.identity()).size());
        equal((long) Integer.MAX_VALUE, consumed[0]);
        System.out.println("M3_WRAPPER_SATURATION_PASS consumed=" + consumed[0]
                + " elapsed_ns=" + (System.nanoTime() - start));
    }
    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("overflow")) overflow();
        else { listOracle(); flattenOracle(); metadataAndFailures(); }
        System.out.println("M3_WRAPPER_BOUNDARIES_PASS checks=" + checks);
    }
}
