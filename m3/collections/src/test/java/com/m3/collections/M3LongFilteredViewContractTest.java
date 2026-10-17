/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.function.LongPredicate;
import org.junit.jupiter.api.Test;

/** Focused JDK21 differential, live-view, refusal and allocation-shape tests. */
public final class M3LongFilteredViewContractTest {
    private static long checks;

    public static void main(String[] args) {
        M3LongFilteredViewContractTest tests = new M3LongFilteredViewContractTest();
        tests.composableAndReadOnly();
        tests.lateBindingStreamsAndNoSizeScan();
        tests.fusedNestedPredicatesKeepEvaluationOrder();
        tests.differentialSeeded();
        tests.exceptionBoundaries();
        System.out.println("M3_LONG_FILTER_PASS checks=" + checks);
    }

    @Test
    public void composableAndReadOnly() {
        Counting source = new Counting(-3, -2, -1, 0, 1, 2, 3, Long.MAX_VALUE);
        M3LongCollection view = new M3LongFilteredView(source, v -> (v & 1) != 0);
        expect(Arrays.equals(view.toArray(), new long[]{-3, -1, 1, 3, Long.MAX_VALUE}),
                "primitive filtered result");
        expect(view.size() == 5, "filtered size");
        expect(!view.contains(0) && view.contains(-3), "membership");
        expect(Arrays.equals(view.longStream().toArray(), view.toArray()),
                "primitive stream over M3LongCollection");
        expect(view.parallelLongStream().sum() ==
                Arrays.stream(view.toArray()).sum(), "parallel correctness");
        rejects(UnsupportedOperationException.class, view::clear);
        expect(source.sizeCalls == 0, "no eager backing cardinality");
    }

    @Test
    public void lateBindingStreamsAndNoSizeScan() {
        Counting source = new Counting(1, 2, 3);
        int[] tests = {0};
        M3LongFilteredView filtered = new M3LongFilteredView(source, v -> {
            tests[0]++;
            return (v & 1) == 0;
        });
        var stream = filtered.longStream();
        Spliterator.OfLong split = filtered.longSpliterator();
        expect(source.iteratorCalls == 0 && tests[0] == 0, "no premature source traversal");
        expect(split.estimateSize() == Long.MAX_VALUE, "unsized metadata");
        expect((split.characteristics() & (Spliterator.SIZED | Spliterator.SORTED |
                Spliterator.DISTINCT)) == 0, "do not invent sorted/distinct flags");
        expect(split.trySplit() == null, "no boxed split buffer");
        expect(source.iteratorCalls == 0 && tests[0] == 0, "spliterator remains unbound");
        source.add(4);
        expect(Arrays.equals(stream.toArray(), new long[]{2, 4}), "late-bound source update");
        expect(source.iteratorCalls == 1 && tests[0] == 4, "one cursor and no eager count");
        int baseline = tests[0];
        source.add(6);
        expect(Arrays.equals(filtered.toArray(), new long[]{2, 4, 6}), "live subsequent snapshot");
        expect(tests[0] == baseline + 5, "no redundant predicate size pass");
        source.clearValues();
        expect(filtered.toArray().length == 0 && filtered.isEmpty(), "empty source");
        expect(source.sizeCalls == 0, "never consult source.size");
    }

    @Test
    public void fusedNestedPredicatesKeepEvaluationOrder() {
        Counting source = new Counting(Long.MIN_VALUE, -5, -2, -1, 0, 1, 2, 3,
                4, 5, Long.MAX_VALUE);
        StringBuilder order = new StringBuilder();
        LongPredicate first = n -> { order.append('A'); return n > 0; };
        LongPredicate second = n -> { order.append('B'); return (n & 1) != 0; };
        LongPredicate third = n -> { order.append('C'); return n < 6; };
        M3LongFilteredView one = new M3LongFilteredView(source, first);
        M3LongFilteredView two = new M3LongFilteredView(one, second);
        M3LongFilteredView three = new M3LongFilteredView(two, third);
        expect(Arrays.equals(three.toArray(), new long[]{1,3,5}), "nested result");
        StringBuilder expected = new StringBuilder();
        for (long n : source.snapshot()) {
            expected.append('A');
            if (n > 0) {
                expected.append('B');
                if ((n & 1) != 0) expected.append('C');
            }
        }
        expect(order.toString().equals(expected.toString()), "predicate sequence");
        expect(source.iteratorCalls == 1, "one source traversal despite nested decorators");
        expect(!three.contains(Long.MIN_VALUE) && !three.contains(2)
                && three.contains(3), "nested contains");
    }

    @Test
    public void differentialSeeded() {
        Random random = new Random(0x4d3346494c544552L);
        for (int round = 0; round < 500; round++) {
            Counting source = new Counting();
            int length = random.nextInt(60);
            for (int i=0; i < length; i++) source.add(random.nextLong());
            long mask = random.nextInt(63);
            LongPredicate predicate = n -> ((n ^ mask) & 15) < 7;
            M3LongFilteredView view = new M3LongFilteredView(source, predicate);
            long[] actual = view.toArray();
            long[] expected = Arrays.stream(source.snapshot()).filter(predicate).toArray();
            expect(Arrays.equals(actual, expected), "seeded reference parity");
            expect(view.size() == expected.length, "seeded cardinality");
            expect(Arrays.equals(view.longStream().toArray(), expected), "seeded stream parity");
            expect(source.sizeCalls == 0, "seeded no upstream size");
        }
    }

    @Test
    public void exceptionBoundaries() {
        Counting source = new Counting(1, 2);
        rejects(NullPointerException.class, () -> new M3LongFilteredView(null, v -> true));
        rejects(NullPointerException.class, () -> new M3LongFilteredView(source, null));
        M3LongFilteredView view = new M3LongFilteredView(source, v -> true);
        rejects(NullPointerException.class, () -> view.forEachLong(null));
        rejects(NullPointerException.class, () -> view.longSpliterator().tryAdvance((java.util.function.LongConsumer) null));
        M3LongIterator it = view.iterator();
        expect(it.nextLong() == 1 && it.nextLong() == 2, "iterator sequencing");
        rejects(NoSuchElementException.class, it::nextLong);
        rejects(UnsupportedOperationException.class,
                new M3LongFilteredView(new Counting(), v -> true)::clear);
    }

    private static void expect(boolean valid, String description) {
        checks++;
        if (!valid) throw new AssertionError(description);
    }
    private static void rejects(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable actual) {
            if (type.isInstance(actual)) return;
            throw new AssertionError("expected " + type + " got " + actual, actual);
        }
        throw new AssertionError("expected " + type + " but succeeded");
    }

    private static final class Counting implements M3LongCollection {
        private final List<Long> items = new ArrayList<>();
        private int sizeCalls;
        private int iteratorCalls;
        Counting(long... values) { for (long value : values) items.add(value); }
        void add(long value) { items.add(value); }
        void clearValues() { items.clear(); }
        long[] snapshot() { return items.stream().mapToLong(Long::longValue).toArray(); }
        @Override public int size() {
            sizeCalls++;
            throw new AssertionError("size() must not be queried by filtering");
        }
        @Override public boolean contains(long n) { return items.contains(n); }
        @Override public void clear() { items.clear(); }
        @Override public long[] toArray() { return snapshot(); }
        @Override public M3LongIterator iterator() {
            iteratorCalls++;
            var iterator = items.iterator();
            return new M3LongIterator() {
                @Override public boolean hasNext() { return iterator.hasNext(); }
                @Override public long nextLong() { return iterator.next(); }
            };
        }
    }
}
