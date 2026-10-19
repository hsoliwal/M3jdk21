/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongPredicate;

/** Bounded oracle for persistent, stack-safe default LongPredicate.and composition. */
public final class M3LongDeepFilterSelfTest {
    private static long assertions;
    private M3LongDeepFilterSelfTest() { }
    private static void check(boolean valid, String what) {
        assertions++;
        if (!valid) throw new AssertionError(what);
    }
    private static void equal(long expected, long actual, String what) {
        assertions++;
        if (expected != actual) throw new AssertionError(what + " expected " + expected + " actual " + actual);
    }
    private static void array(long[] expected, long[] actual, String what) {
        assertions++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(what + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }
    private static void throwsType(Class<? extends Throwable> expected, Runnable operation) {
        assertions++;
        try { operation.run(); }
        catch (Throwable thrown) {
            if (expected.isInstance(thrown)) return;
            throw new AssertionError("wrong exception", thrown);
        }
        throw new AssertionError("expected " + expected.getName());
    }

    private static void deepGeneric() {
        SortedFixture backing = new SortedFixture(-1, 2, 4, 6);
        AtomicInteger calls = new AtomicInteger();
        LongPredicate onlyPositive = value -> { calls.incrementAndGet(); return value > 0; };
        M3LongCollection view = new M3LongFilteredView(backing, onlyPositive);
        final int depth = 100_000;
        for (int n = 1; n < depth; n++) view = new M3LongFilteredView(view, onlyPositive);
        equal(0, calls.get(), "construction does not evaluate any predicate");
        check(view.contains(2), "deep contains stack safety");
        equal(depth, calls.get(), "all positive predicates evaluated");
        check(!view.contains(-1), "deep short circuit negative");
        equal(depth + 1L, calls.get(), "negative stops at first predicate");
        array(new long[]{2, 4, 6}, view.longStream().toArray(), "deep generic stream");
        equal(3, view.size(), "deep size");
        M3LongCollection previous = view;
        view = new M3LongFilteredView(previous, value -> value != 4);
        array(new long[]{2, 6}, view.toArray(), "appended selector");
        array(new long[]{2, 4, 6}, previous.toArray(), "previous immutable view preserved");
        backing.add(8);
        array(new long[]{2, 6, 8}, view.toArray(), "live owner after creation");
        throwsType(UnsupportedOperationException.class, view::clear);
    }

    private static void deepSorted() {
        SortedFixture backing = new SortedFixture(-5, -1, 2, 3, 4, 8);
        LongPredicate positive = value -> value >= 0;
        M3LongSortedSet view = M3LongSortedViews.filter(backing, positive);
        final int depth = 100_000;
        for (int n = 1; n < depth; n++) view = M3LongSortedViews.filter(view, positive);
        array(new long[]{2, 3, 4, 8}, view.longStream().toArray(), "deep sorted stream");
        equal(2, view.first(), "deep sorted first");
        equal(8, view.last(), "deep sorted last");
        equal(3, view.higher(2, -1), "deep sorted higher");
        equal(4, view.floor(5, -1), "deep sorted floor");
        equal(2, view.ceiling(-2, -1), "deep sorted ceiling");
        check(!view.contains(-5), "deep sorted excludes hidden");
        check(backing.contains(-5), "hidden negative retained in backing");
        check(view.add(10), "deep sorted live add");
        throwsType(IllegalArgumentException.class, () -> viewForRefusal(backing).add(-7));
        check(view.remove(3), "deep sorted live remove");
        check(backing.contains(-5) && backing.contains(-1), "hidden survivors after removal");
        equal(2, view.filterInPlace(value -> value != 2 && value != 4), "deep selected filter removal");
        array(new long[]{-5, -1, 8, 10}, backing.toArray(), "in-place filter keeps hidden rows");
        view.clear();
        array(new long[]{-5, -1}, backing.toArray(), "deep clear affects only visible rows");
    }
    private static M3LongSortedSet viewForRefusal(SortedFixture backing) {
        return M3LongSortedViews.filter(backing, value -> value >= 0);
    }

    /** Custom and() may intentionally use OR and must keep virtual dispatch. */
    private static void customAnd() {
        AtomicInteger merges = new AtomicInteger();
        StringBuilder evaluations = new StringBuilder();
        class Disjunctive implements LongPredicate {
            private final LongPredicate value;
            Disjunctive(LongPredicate value) { this.value = value; }
            public boolean test(long candidate) { return value.test(candidate); }
            @Override public LongPredicate and(LongPredicate other) {
                merges.incrementAndGet();
                return new Disjunctive(candidate -> test(candidate) || other.test(candidate));
            }
        }
        LongPredicate a = new Disjunctive(v -> { evaluations.append('A'); return v == 2; });
        LongPredicate b = v -> { evaluations.append('B'); return v == 3; };
        LongPredicate c = v -> { evaluations.append('C'); return v == 4; };
        SortedFixture backing = new SortedFixture(2, 3, 4);
        M3LongCollection first = new M3LongFilteredView(backing, a);
        M3LongCollection second = new M3LongFilteredView(first, b);
        M3LongCollection third = new M3LongFilteredView(second, c);
        equal(2, merges.get(), "custom and() virtual dispatch called twice");
        array(new long[]{2, 3, 4}, third.toArray(), "custom OR semantics preserved");
        equal(0, merges.get() - 2, "no extra and during traversal");
        check(evaluations.toString().contains("ABC"), "custom evaluation trace");
        SortedFixture other = new SortedFixture(2, 3, 4);
        M3LongSortedSet s = M3LongSortedViews.filter(other,
                new Disjunctive(v -> v == 2));
        s = M3LongSortedViews.filter(s, v -> v == 3);
        s = M3LongSortedViews.filter(s, v -> v == 4);
        array(new long[]{2, 3, 4}, s.toArray(), "sorted custom and override");
        equal(4, merges.get(), "sorted dispatch count");
        LongPredicate throwing = new LongPredicate() {
            public boolean test(long value) { return true; }
            @Override public LongPredicate and(LongPredicate other) { throw new IllegalStateException("and called"); }
        };
        M3LongCollection original = new M3LongFilteredView(backing, throwing);
        throwsType(IllegalStateException.class,
                () -> new M3LongFilteredView(original, v -> true));
        M3LongSortedSet sorted = M3LongSortedViews.filter(other, throwing);
        throwsType(IllegalStateException.class,
                () -> M3LongSortedViews.filter(sorted, v -> true));
        LongPredicate nullResult = new LongPredicate() {
            public boolean test(long value) { return true; }
            @Override public LongPredicate and(LongPredicate other) { return null; }
        };
        M3LongCollection nullable = new M3LongFilteredView(backing, nullResult);
        M3LongCollection nulled = new M3LongFilteredView(nullable, v -> true);
        throwsType(NullPointerException.class, () -> nulled.contains(2));
    }

    private static void differential() {
        Random random = new Random(0x4d33444545504cL);
        for (int iteration = 0; iteration < 256; iteration++) {
            SortedFixture backing = new SortedFixture();
            for (int n = 0; n < 48; n++) backing.add(random.nextInt(120) - 60);
            M3LongSortedSet current = backing;
            LongPredicate reference = value -> true;
            for (int n = 0; n < 50; n++) {
                long cutoff = random.nextInt(120) - 60;
                boolean above = random.nextBoolean();
                LongPredicate next = above ? value -> value >= cutoff : value -> value <= cutoff;
                current = M3LongSortedViews.filter(current, next);
                reference = reference.and(next);
            }
            LongPredicate oracle = reference;
            long[] expected = backing.keys.stream().mapToLong(Long::longValue).filter(oracle).toArray();
            array(expected, current.toArray(), "seeded sorted filter parity");
            for (int j = -65; j < 65; j += 13) {
                check(current.contains(j) == (backing.contains(j) && oracle.test(j)), "seeded contains");
            }
            M3LongCollection generic = backing;
            for (int n = 0; n < 50; n++) generic = new M3LongFilteredView(generic, value -> value != 1000);
            array(backing.toArray(), generic.toArray(), "seeded generic filter parity");
        }
    }

    public static void main(String[] args) {
        deepGeneric();
        deepSorted();
        customAnd();
        differential();
        System.out.println("M3_DEEP_FILTER_PASS assertions=" + assertions + " depth=100000");
    }

    private static final class SortedFixture implements M3LongSortedSet {
        private final NavigableSet<Long> keys = new TreeSet<>();
        SortedFixture(long... values) { for (long value : values) keys.add(value); }
        @Override public boolean add(long value) { return keys.add(value); }
        @Override public boolean remove(long value) { return keys.remove(value); }
        @Override public void clear() { keys.clear(); }
        @Override public int size() { return keys.size(); }
        @Override public boolean isEmpty() { return keys.isEmpty(); }
        @Override public boolean contains(long value) { return keys.contains(value); }
        @Override public long[] toArray() { return keys.stream().mapToLong(Long::longValue).toArray(); }
        @Override public M3LongIterator iterator() {
            Iterator<Long> delegate = keys.iterator();
            return new M3LongIterator() {
                @Override public boolean hasNext() { return delegate.hasNext(); }
                @Override public long nextLong() { return delegate.next(); }
            };
        }
        @Override public long first() { return keys.first(); }
        @Override public long last() { return keys.last(); }
        @Override public long floor(long value, long fallback) {
            Long got = keys.floor(value); return got == null ? fallback : got;
        }
        @Override public long ceiling(long value, long fallback) {
            Long got = keys.ceiling(value); return got == null ? fallback : got;
        }
        @Override public long lower(long value, long fallback) {
            Long got = keys.lower(value); return got == null ? fallback : got;
        }
        @Override public long higher(long value, long fallback) {
            Long got = keys.higher(value); return got == null ? fallback : got;
        }
        @Override public int filterInPlace(LongPredicate keep) {
            Objects.requireNonNull(keep, "keep");
            int deleted = 0;
            for (Iterator<Long> iter = keys.iterator(); iter.hasNext();) {
                if (!keep.test(iter.next())) { iter.remove(); deleted++; }
            }
            return deleted;
        }
    }
}
