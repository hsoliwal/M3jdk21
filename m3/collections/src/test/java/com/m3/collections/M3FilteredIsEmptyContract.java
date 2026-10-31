// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Java21 owner contract oracle. Contrast isEmpty against the unchanged public
 * selected iterator for the same backing and predicate; compare complete
 * backing-iterator/predicate traces and exception identities, not only results.
 */
public final class M3FilteredIsEmptyContract {
    private static long checks;

    private M3FilteredIsEmptyContract() { }

    private record Result(boolean matched, Throwable failure, List<String> log) { }

    private static final class Recorded extends AbstractCollection<Integer> {
        final List<Integer> values = new ArrayList<>();
        final List<String> events = new ArrayList<>();
        RuntimeException failIterator;
        RuntimeException failNext;
        int failHasNextAt = -1;
        int nextCalls;
        int hasNextCalls;
        int sizeCalls;

        Recorded(List<Integer> source) { values.addAll(source); }

        @Override public int size() {
            sizeCalls++;
            events.add("SIZE");
            return values.size();
        }

        @Override public Iterator<Integer> iterator() {
            events.add("ITERATOR");
            if (failIterator != null) throw failIterator;
            Iterator<Integer> data = values.iterator();
            return new Iterator<>() {
                @Override public boolean hasNext() {
                    events.add("HAS_NEXT");
                    if (hasNextCalls++ == failHasNextAt)
                        throw new IllegalStateException("cursor hasNext failure");
                    return data.hasNext();
                }
                @Override public Integer next() {
                    events.add("NEXT");
                    if (failNext != null && nextCalls++ == 1) throw failNext;
                    return data.next();
                }
            };
        }

        void reset() {
            events.clear();
            nextCalls = 0;
            hasNextCalls = 0;
            sizeCalls = 0;
        }
    }

    public static void main(String[] args) {
        for (List<Integer> input : List.of(
                List.<Integer>of(),
                List.of(1),
                List.of(1, 2, 3, 4),
                List.of(2, 2, 2),
                Arrays.asList(null, -1, null, 7, 7, 2))) {
            for (int threshold = -2; threshold <= 8; threshold++) {
                verifyRecorded(input, threshold, null, null, -1);
            }
        }
        RuntimeException iteratorFailure = new IllegalArgumentException("source iterator");
        RuntimeException nextFailure = new IllegalStateException("source next");
        RuntimeException predicateFailure = new UnsupportedOperationException("predicate");
        verifyRecorded(List.of(1, 2, 3), 2, iteratorFailure, null, -1);
        verifyRecorded(List.of(1, 2, 3), 2, null, nextFailure, -1);
        verifyRecorded(List.of(1, 2, 3), 2, null, null, 1);
        verifyPredicateFailure(predicateFailure);

        Random random = new Random(0x4d334a444b3231L);
        for (int trial = 0; trial < 2000; trial++) {
            int count = random.nextInt(65);
            List<Integer> values = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                values.add(random.nextInt(10) == 0 ? null : random.nextInt(20) - 10);
            }
            verifyRecorded(values, random.nextInt(22) - 11, null, null, -1);
        }

        verifyInheritedCollections();
        verifyLiveBacking();
        System.out.println("PASS M3FilteredIsEmptyContract checks=" + checks);
    }

    private static void verifyRecorded(List<Integer> source, int threshold,
                                       RuntimeException iteratorFail, RuntimeException nextFail,
                                       int hasNextFailAt) {
        Recorded recorded = new Recorded(source);
        recorded.failIterator = iteratorFail;
        recorded.failNext = nextFail;
        recorded.failHasNextAt = hasNextFailAt;
        Predicate<Integer> filter = x -> {
            recorded.events.add("PREDICATE:" + x);
            return x != null && x >= threshold;
        };
        Collection<Integer> view = M3CollectionViews.filtered((Collection<Integer>) recorded, filter);
        Result before = capture(recorded, () -> view.iterator().hasNext());
        Result after = capture(recorded, () -> !view.isEmpty());
        equal(before, after, "recorded collection");
        must(recorded.sizeCalls == 0, "isEmpty requested backing.size()");
    }

    private static void verifyPredicateFailure(RuntimeException fail) {
        Recorded backing = new Recorded(List.of(1, 2, 3));
        Collection<Integer> view = M3CollectionViews.filtered((Collection<Integer>) backing, x -> {
            backing.events.add("PREDICATE:" + x);
            if (x == 2) throw fail;
            return false;
        });
        equal(capture(backing, () -> view.iterator().hasNext()),
                capture(backing, () -> !view.isEmpty()), "predicate exception");
    }

    private static void verifyInheritedCollections() {
        List<Integer> list = new ArrayList<>(List.of(5, 1, 5, 3));
        var set = new LinkedHashSet<>(list);
        NavigableSet<Integer> navigable = new TreeSet<>(list);
        SortedSet<Integer> sorted = Collections.unmodifiableSortedSet(new TreeSet<>(list));
        for (Collection<Integer> input : List.of(list, set, navigable, sorted)) {
            for (int limit : new int[]{0, 1, 4, 5, 6}) {
                List<Integer> trace = new ArrayList<>();
                Collection<Integer> view = M3CollectionViews.filtered(input, x -> {
                    trace.add(x);
                    return x >= limit;
                });
                boolean observed = view.iterator().hasNext();
                List<Integer> fromIterator = List.copyOf(trace);
                trace.clear();
                boolean fromEmpty = !view.isEmpty();
                must(observed == fromEmpty, "inherited subtype result");
                must(fromIterator.equals(trace), "inherited subtype predicate order");
                checks += 2;
            }
        }
    }

    private static void verifyLiveBacking() {
        List<Integer> backing = new ArrayList<>(List.of(1, 2));
        Collection<Integer> view = M3CollectionViews.filtered(backing, x -> x > 10);
        must(view.isEmpty(), "initially empty");
        backing.add(11);
        must(!view.isEmpty(), "late backing addition visible");
        backing.remove(Integer.valueOf(11));
        must(view.isEmpty(), "late backing deletion visible");
        checks += 3;
    }

    @FunctionalInterface
    private interface Probe { boolean run(); }

    private static Result capture(Recorded source, Probe probe) {
        source.reset();
        try {
            return new Result(probe.run(), null, List.copyOf(source.events));
        } catch (RuntimeException failure) {
            return new Result(false, failure, List.copyOf(source.events));
        }
    }

    private static void equal(Result expected, Result actual, String caseName) {
        must(expected.matched() == actual.matched(), caseName + " result");
        if (expected.failure() != null || actual.failure() != null)
            must(expected.failure() == actual.failure()
                    || expected.failure() != null && actual.failure() != null
                    && expected.failure().getClass() == actual.failure().getClass()
                    && Objects.equals(expected.failure().getMessage(), actual.failure().getMessage()),
                    caseName + " exception type/message");
        must(expected.log().equals(actual.log()), caseName + " iterator/predicate trace");
        checks += 3;
    }

    private static void must(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
