// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.stream.StreamSupport;

/**
 * Contract and random-oracle proof for backed sorted wrapper late binding.
 * Only this test oracle boxes: the actual M3 wrapper remains primitive-backed.
 */
public final class M3LongSortedLateBindingSelfTest {
    private static long checks;
    private M3LongSortedLateBindingSelfTest() { }

    public static void main(String[] args) {
        filteredStreamBindsOnlyOnTraversal();
        unmodifiableIsStillLiveBeforeTraversal();
        nestedRangesAndExceptionBoundaries();
        randomDifferential();
        System.out.println("M3_SORTED_LATE_BINDING_PASS checks=" + checks);
    }

    private static void filteredStreamBindsOnlyOnTraversal() {
        CountedSorted owner = new CountedSorted(1, 2, 3);
        M3LongSortedSet filtered = M3LongSortedViews.filter(owner, v -> (v & 1) == 0);
        var first = filtered.longStream();
        var second = filtered.parallelLongStream();
        Spliterator.OfLong direct = filtered.longSpliterator();
        eq(owner.cursorCalls, 0, "creation did not obtain a backing cursor");
        eq(owner.sizeCalls, 0, "creation did not scan source size");
        eq(direct.estimateSize(), Long.MAX_VALUE, "unknown filtered cardinality");
        eq(direct.characteristics(),
                Spliterator.ORDERED | Spliterator.SORTED | Spliterator.DISTINCT | Spliterator.NONNULL,
                "sorted natural-long metadata");
        check(direct.getComparator() == null, "natural comparator remains null");
        check(direct.trySplit() == null, "never allocate boxed split buffers");
        eq(owner.cursorCalls, 0, "metadata and failed split do not bind");

        owner.add(4);
        arr(first.toArray(), 2, 4);
        eq(owner.cursorCalls, 1, "one cursor at first terminal traversal");

        owner.add(6);
        arr(second.toArray(), 2, 4, 6);
        eq(owner.cursorCalls, 2, "parallel stream honors nonsplitting backing");

        owner.add(8);
        arr(StreamSupport.longStream(direct, false).toArray(), 2, 4, 6, 8);
        eq(owner.cursorCalls, 3, "direct spliterator binds exactly once");
        eq(owner.sizeCalls, 0, "sorted metadata never calls source size()");
    }

    private static void unmodifiableIsStillLiveBeforeTraversal() {
        CountedSorted owner = new CountedSorted(Long.MIN_VALUE, 0);
        M3LongSortedSet view = M3LongSortedViews.unmodifiable(owner);
        var stream = view.longStream();
        Spliterator.OfLong split = view.longSpliterator();
        eq(owner.cursorCalls, 0, "unmodifiable view remains unbound");
        check(split.trySplit() == null, "unsplittable unmodifiable traversal");
        eq(split.estimateSize(), Long.MAX_VALUE, "unmodifiable owner has no eager count");
        owner.add(Long.MAX_VALUE);
        arr(stream.toArray(), Long.MIN_VALUE, 0, Long.MAX_VALUE);
        eq(owner.cursorCalls, 1, "read-only first traversal sees additions");
        owner.add(7);
        arr(StreamSupport.longStream(split, false).toArray(),
                Long.MIN_VALUE, 0, 7, Long.MAX_VALUE);
        eq(owner.cursorCalls, 2, "late direct unmodifiable stream");
        refuses(UnsupportedOperationException.class, () -> view.add(9));
        refuses(UnsupportedOperationException.class, () -> view.remove(0));
        refuses(UnsupportedOperationException.class, view::clear);
        check(owner.contains(0), "readonly mutation refusal preserves owner");
        eq(owner.sizeCalls, 0, "unmodifiable cursor does not count before traversal");
    }

    private static void nestedRangesAndExceptionBoundaries() {
        CountedSorted owner = new CountedSorted(-4, -3, -2, -1, 0, 1, 2, 3);
        StringBuilder order = new StringBuilder();
        M3LongSortedSet first = M3LongSortedViews.filter(owner, v -> {
            order.append("A");
            return v >= 0;
        });
        M3LongSortedSet next = M3LongSortedViews.filter(first, v -> {
            order.append("B");
            return (v & 1) != 0;
        });
        var stream = next.longStream();
        check(order.length() == 0 && owner.cursorCalls == 0,
                "nested filters execute no predicates at stream construction");
        owner.add(5);
        arr(stream.toArray(), 1, 3, 5);
        eq(order.toString(), "AAAAABABABABAB", "ordered predicate short circuit");

        Spliterator.OfLong nullProbe = next.longSpliterator();
        int prior = owner.cursorCalls;
        refuses(NullPointerException.class,
                () -> nullProbe.tryAdvance((LongConsumer) null));
        eq(owner.cursorCalls, prior, "null action refuses before source binding");
        arr(StreamSupport.longStream(nullProbe, false).toArray(), 1, 3, 5);

        CountedSorted bounded = new CountedSorted(Long.MIN_VALUE, -3, 0, 3,
                Long.MAX_VALUE);
        M3LongSortedSet range = M3LongSortedViews.subSet(bounded, -4, Long.MAX_VALUE);
        var pending = range.longStream();
        eq(bounded.cursorCalls, 0, "range view late stream");
        bounded.add(4);
        arr(pending.toArray(), -3, 0, 3, 4);
        eq(bounded.cursorCalls, 1, "range cursor constructed on demand");
        refuses(IllegalArgumentException.class,
                () -> M3LongSortedViews.subSet(bounded, 10, -10));
    }

    private static void randomDifferential() {
        Random random = new Random(0x4d33534f52544544L);
        for (int iteration = 0; iteration < 256; iteration++) {
            CountedSorted owner = new CountedSorted();
            for (int n = 0; n < 48; n++) owner.add(random.nextInt(160) - 80);
            long mask = random.nextInt(32);
            LongPredicate predicate = value -> ((value ^ mask) & 7L) <= 3;
            M3LongSortedSet view = M3LongSortedViews.filter(owner, predicate);
            var pending = view.longStream();
            long added = 200L + iteration;
            owner.add(added);
            arr(pending.toArray(),
                    owner.keys.stream().mapToLong(Long::longValue).filter(predicate).toArray());
            eq(owner.cursorCalls, 1, "seeded result uses a single source iterator");
            check(view.longSpliterator().hasCharacteristics(Spliterator.SORTED |
                    Spliterator.DISTINCT | Spliterator.ORDERED), "seeded sorted shape");
        }
    }

    private static void arr(long[] actual, long... expected) {
        checks++;
        if (!Arrays.equals(actual, expected)) {
            throw new AssertionError("expected " + Arrays.toString(expected)
                    + " actual " + Arrays.toString(actual));
        }
    }
    private static void eq(long actual, long expected, String label) {
        checks++;
        if (actual != expected) {
            throw new AssertionError(label + ": expected " + expected + " got " + actual);
        }
    }
    private static void eq(String actual, String expected, String label) {
        checks++;
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError(label + ": expected " + expected + " got " + actual);
        }
    }
    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message);
    }
    private static void refuses(Class<? extends Throwable> type, Runnable run) {
        checks++;
        try { run.run(); }
        catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError("wrong refusal", thrown);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    private static final class CountedSorted implements M3LongSortedSet {
        private final NavigableSet<Long> keys = new TreeSet<>();
        private int cursorCalls;
        private int sizeCalls;
        CountedSorted(long... entries) { for (long value : entries) keys.add(value); }
        @Override public int size() { sizeCalls++; return keys.size(); }
        @Override public boolean contains(long value) { return keys.contains(value); }
        @Override public boolean isEmpty() { return keys.isEmpty(); }
        @Override public void clear() { keys.clear(); }
        @Override public long[] toArray() {
            return keys.stream().mapToLong(Long::longValue).toArray();
        }
        @Override public M3LongIterator iterator() {
            cursorCalls++;
            Iterator<Long> cursor = keys.iterator();
            return new M3LongIterator() {
                @Override public boolean hasNext() { return cursor.hasNext(); }
                @Override public long nextLong() { return cursor.next(); }
            };
        }
        @Override public boolean add(long value) { return keys.add(value); }
        @Override public boolean remove(long value) { return keys.remove(value); }
        @Override public long first() { return keys.first(); }
        @Override public long last() { return keys.last(); }
        @Override public long lower(long value, long fallback) {
            Long answer = keys.lower(value);
            return answer == null ? fallback : answer;
        }
        @Override public long higher(long value, long fallback) {
            Long answer = keys.higher(value);
            return answer == null ? fallback : answer;
        }
        @Override public long floor(long value, long fallback) {
            Long answer = keys.floor(value);
            return answer == null ? fallback : answer;
        }
        @Override public long ceiling(long value, long fallback) {
            Long answer = keys.ceiling(value);
            return answer == null ? fallback : answer;
        }
        @Override public int filterInPlace(LongPredicate keep) {
            Objects.requireNonNull(keep, "keep");
            int removed = 0;
            for (Iterator<Long> it = keys.iterator(); it.hasNext();) {
                if (!keep.test(it.next())) { it.remove(); removed++; }
            }
            return removed;
        }
    }
}
