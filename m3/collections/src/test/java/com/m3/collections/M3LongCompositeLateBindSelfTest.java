// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Random;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.function.LongPredicate;
import java.util.stream.StreamSupport;

/** Bounded contract oracle: stream/spliterator acquisition never opens source iterators. */
public final class M3LongCompositeLateBindSelfTest {
    private static int checks;
    private M3LongCompositeLateBindSelfTest() { }

    public static void main(String[] args) {
        lazyUnion();
        lazyConcatenation();
        lazySetAlgebra();
        mutationAfterBindingIsStillFailFast();
        randomizedUnion();
        System.out.println("PASS M3LongCompositeLateBindSelfTest checks=" + checks);
    }

    private static void lazyUnion() {
        CountedSorted left = new CountedSorted(1, 3);
        CountedSorted right = new CountedSorted(2, 3);
        M3LongSortedSet union = M3LongCompositeViews.sortedUnion(left, right);
        var stream = union.longStream();
        Spliterator.OfLong direct = union.longSpliterator();
        eq(left.iteratorCalls + right.iteratorCalls, 0);
        check(direct.trySplit() == null);
        check(direct.estimateSize() == Long.MAX_VALUE);
        check(direct.getComparator() == null);
        check((direct.characteristics() &
                (Spliterator.ORDERED | Spliterator.SORTED | Spliterator.DISTINCT)) ==
                (Spliterator.ORDERED | Spliterator.SORTED | Spliterator.DISTINCT));
        eq(left.iteratorCalls + right.iteratorCalls, 0);
        left.add(0);
        right.add(4);
        arr(stream.toArray(), 0, 1, 2, 3, 4);
        eq(left.iteratorCalls + right.iteratorCalls, 2);
        left.add(5);
        arr(StreamSupport.longStream(direct, false).toArray(), 0, 1, 2, 3, 4, 5);
        eq(left.iteratorCalls + right.iteratorCalls, 4);
        arr(M3LongCompositeViews.sortedUnion().longStream().toArray());
    }

    private static void lazyConcatenation() {
        CountedSorted first = new CountedSorted(1, 3);
        CountedSorted second = new CountedSorted(3, 5);
        var concat = M3LongCompositeViews.concat(first, second);
        var stream = concat.longStream();
        Spliterator.OfLong cursor = concat.longSpliterator();
        eq(first.iteratorCalls + second.iteratorCalls, 0);
        first.add(0);
        second.add(4);
        arr(stream.toArray(), 0, 1, 3, 3, 4, 5);
        first.add(-1);
        arr(StreamSupport.longStream(cursor, false).toArray(), -1, 0, 1, 3, 3, 4, 5);
        arr(M3LongCompositeViews.concat().longStream().toArray());
    }

    private static void lazySetAlgebra() {
        CountedSorted left = new CountedSorted(1, 2, 4);
        CountedSorted right = new CountedSorted(2, 4);
        var intersection = M3LongCompositeViews.sortedIntersection(left, right).longSpliterator();
        var difference = M3LongCompositeViews.sortedDifference(left, right).longSpliterator();
        var xor = M3LongCompositeViews.sortedSymmetricDifference(left, right).longSpliterator();
        eq(left.iteratorCalls + right.iteratorCalls, 0);
        right.add(1);
        right.add(6);
        arr(StreamSupport.longStream(intersection, false).toArray(), 1, 2, 4);
        arr(StreamSupport.longStream(difference, false).toArray());
        arr(StreamSupport.longStream(xor, false).toArray(), 6);
        check(left.iteratorCalls + right.iteratorCalls == 6);
    }

    private static void mutationAfterBindingIsStillFailFast() {
        CountedSorted first = new CountedSorted(1, 3, 5);
        CountedSorted second = new CountedSorted(2, 4, 6);
        var cursor = M3LongCompositeViews.sortedUnion(first, second).longSpliterator();
        check(cursor.tryAdvance((long value) -> eq(value, 1)));
        first.add(7);
        boolean failed = false;
        try {
            while (cursor.tryAdvance((long value) -> { })) { }
        } catch (ConcurrentModificationException expected) {
            failed = true;
        }
        check(failed);
    }

    private static void randomizedUnion() {
        Random random = new Random(0x4D33434F4D504F53L);
        for (int round = 0; round < 256; round++) {
            CountedSorted a = new CountedSorted();
            CountedSorted b = new CountedSorted();
            TreeSet<Long> reference = new TreeSet<>();
            for (int i = 0; i < 16; i++) {
                long v = random.nextInt(49) - 24;
                if ((i & 1) == 0) a.add(v); else b.add(v);
                reference.add(v);
            }
            var stream = M3LongCompositeViews.sortedUnion(a, b).longStream();
            eq(a.iteratorCalls + b.iteratorCalls, 0);
            long mutation = round - 128L;
            a.add(mutation);
            reference.add(mutation);
            arr(stream.toArray(), reference.stream().mapToLong(Long::longValue).toArray());
        }
    }

    private static void eq(long actual, long wanted) {
        checks++;
        if (actual != wanted) throw new AssertionError("expected=" + wanted + " actual=" + actual);
    }
    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("composite cursor contract");
    }
    private static void arr(long[] actual, long... expected) {
        checks++;
        if (!Arrays.equals(actual, expected)) {
            throw new AssertionError("expected=" + Arrays.toString(expected)
                + " actual=" + Arrays.toString(actual));
        }
    }

    private static final class CountedSorted implements M3LongSortedSet {
        final M3LongTreeSet owner = new M3LongTreeSet();
        int iteratorCalls;
        CountedSorted(long... values) { for (long v : values) owner.add(v); }
        @Override public int size() { return owner.size(); }
        @Override public boolean isEmpty() { return owner.isEmpty(); }
        @Override public boolean contains(long v) { return owner.contains(v); }
        @Override public void clear() { owner.clear(); }
        @Override public M3LongIterator iterator() {
            iteratorCalls++;
            return owner.iterator();
        }
        @Override public long[] toArray() { return owner.toArray(); }
        @Override public boolean add(long v) { return owner.add(v); }
        @Override public boolean remove(long v) { return owner.remove(v); }
        @Override public long first() { return owner.first(); }
        @Override public long last() { return owner.last(); }
        @Override public long floor(long v, long fallback) { return owner.floor(v, fallback); }
        @Override public long ceiling(long v, long fallback) { return owner.ceiling(v, fallback); }
        @Override public long lower(long v, long fallback) { return owner.lower(v, fallback); }
        @Override public long higher(long v, long fallback) { return owner.higher(v, fallback); }
        @Override public int filterInPlace(LongPredicate predicate) {
            return owner.filterInPlace(predicate);
        }
    }
}
