// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ObjLongConsumer;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/** Differential Java 21 oracle for parent-owned DOOP DAGs and virtual aggregation. */
public final class M3LongDagContract {
    private static long checks;
    private M3LongDagContract() { }

    public static void main(String[] args) {
        liveFilterAndStableSort();
        liveSortedOwnerAndGoFComposition();
        deepPlanAndLargeVirtualAccumulation();
        planCompositionAndStreaming();
        virtualAccumulatorAndCollector();
        batchBoundaries();
        distinctAndBudgetRefusal();
        sourceClosureAndFailures();
        randomizedOracle();
        jdkCollectorAndNativeMaskBoundary();
        System.out.println("M3_LONG_DAG_CHECKS=" + checks + " RESULT=PASS");
    }

    private static void liveFilterAndStableSort() {
        M3LongArrayList owner = M3LongArrayList.copyOf(new long[]{3, 1, 2, -1});
        M3LongDag plan = M3LongDag.filtered(owner, value -> value > 0).map(value -> value * 2);
        owner.add(7);
        eq(new long[]{6, 2, 4, 14}, plan.toArrayBounded(4), "live filtered owner and mapping order");
        eq(new long[]{2, 4, 6, 14}, plan.sortedBounded(4).toArrayBounded(4), "late bounded stable-sort semantics");
        eq(new long[]{3, 1, 2, -1, 7}, owner.toArray(), "sorting must not mutate parent array");
        owner.add(9);
        eq(5, plan.count(), "plan sees late source mutation");
        raises(IllegalStateException.class, () -> plan.sortedBounded(3).count(), "sort budget protects next source value");
        eq(6, owner.size(), "sort refusal leaves backing size intact");
    }

    private static void liveSortedOwnerAndGoFComposition() {
        TestSortedSet source = new TestSortedSet();
        for (long v : new long[] {9, 4, 2, 8, 1, 6}) source.add(v);
        M3LongDag filter = M3LongDag.sortedFiltered(source, value -> (value & 1) == 0);
        eq(new long[] {2, 4, 6, 8}, filter.toArrayBounded(4), "existing live sorted filtering adapter");
        source.add(10);
        eq(new long[] {2, 4, 6, 8, 10}, filter.toArrayBounded(5), "late-bound parent-owned sorted source");
        eq(new long[] {1, 2, 4, 6, 8, 9, 10}, source.toArray(), "parent sorted set remains intact");
        eq(5, filter.count(), "sorted DAG count exact");
    }

    private static void deepPlanAndLargeVirtualAccumulation() {
        M3LongDag source = M3LongDag.from(M3LongArrayList.copyOf(new long[] {1, 2, 3}));
        M3LongDag deep = source;
        for (int i = 0; i < 10_000; i++) deep = deep.skip(0);
        eq(10_001, deep.nodeCount(), "stack-safe DAG node inventory");
        M3LongCollection generated = new M3LongCollection() {
            public int size() { return 100_000; }
            public boolean contains(long value) { return value >= 0 && value < 100_000; }
            public void clear() { throw new UnsupportedOperationException(); }
            public M3LongIterator iterator() { throw new AssertionError("owner stream path required"); }
            public long[] toArray() { throw new AssertionError("unbounded clone forbidden"); }
            public LongStream longStream() { return LongStream.range(0, 100_000); }
        };
        long[] state = M3LongDag.from(generated).virtualAccumulator(() -> new long[2],
                (acc, value) -> {acc[0]++;acc[1] += value;}).evaluate();
        eq(new long[]{100_000, 4_999_950_000L}, state,
                "100k virtual accumulator without input array materialization");
    }

    private static final class TestSortedSet implements M3LongSortedSet {
        private final java.util.TreeSet<Long> values = new java.util.TreeSet<>();
        public boolean add(long value) { return values.add(value); }
        public boolean remove(long value) { return values.remove(value); }
        public int size() { return values.size(); }
        public boolean contains(long value) { return values.contains(value); }
        public void clear() { values.clear(); }
        public long[] toArray() { return values.stream().mapToLong(v -> v).toArray(); }
        public M3LongIterator iterator() {
            java.util.Iterator<Long> iterator = values.iterator();
            return new M3LongIterator() {
                public boolean hasNext() { return iterator.hasNext(); }
                public long nextLong() { return iterator.next(); }
            };
        }
        public long first() { return values.first(); }
        public long last() { return values.last(); }
        public long floor(long value, long fallback) {
            Long n = values.floor(value); return n == null ? fallback : n;
        }
        public long ceiling(long value, long fallback) {
            Long n = values.ceiling(value); return n == null ? fallback : n;
        }
        public long lower(long value, long fallback) {
            Long n = values.lower(value); return n == null ? fallback : n;
        }
        public long higher(long value, long fallback) {
            Long n = values.higher(value); return n == null ? fallback : n;
        }
        public int filterInPlace(java.util.function.LongPredicate predicate) {
            int old = values.size();
            values.removeIf(v -> !predicate.test(v));
            return old - values.size();
        }
    }

    private static void planCompositionAndStreaming() {
        M3LongArrayList owner = M3LongArrayList.copyOf(new long[]{0, 1, 2, 3, 4, 5});
        M3LongDag common = M3LongDag.from(owner);
        M3LongDag child = common.filter(v -> (v & 1) == 0);
        M3LongDag dag = child.map(v -> v + 10).concat(child.map(v -> -v));
        eq(new long[]{10, 12, 14, 0, -2, -4}, dag.toArrayBounded(6), "composite DAG preserves sequential order");
        eq(5, dag.nodeCount(), "identity-aware count shares common node once");
        eq(new long[]{12, 14, 0}, dag.skip(1).limit(3).toArrayBounded(3), "skip and limit compose on DAG");
        eq(List.of(10L, 12L, 14L, 0L, -2L, -4L), dag.stream().boxed().toList(), "JDK LongStream compatibility");
        eq(new long[]{0, 1, 2, 3, 4, 5}, owner.toArray(), "composite plan never mutates source");
        raises(IllegalArgumentException.class, () -> common.skip(-1), "negative skip");
        raises(IllegalArgumentException.class, () -> common.limit(-1), "negative limit");
        raises(NullPointerException.class, () -> common.filter(null), "null strategy");
    }

    private static void virtualAccumulatorAndCollector() {
        M3LongArrayList owner = M3LongArrayList.copyOf(new long[]{3, 5, 7, 9});
        M3LongDag dag = M3LongDag.from(owner);
        AtomicInteger starts = new AtomicInteger();
        AtomicInteger folds = new AtomicInteger();
        M3VirtualAccumulator<long[]> accumulator = dag.virtualAccumulator(() -> {
            starts.incrementAndGet(); return new long[2];
        }, (state, value) -> { folds.incrementAndGet(); state[0]++; state[1] += value; });
        eq(0, starts.get(), "deferred accumulator creation");
        eq(new long[]{4, 24}, accumulator.evaluate(), "accumulate count and sum");
        eq(1, starts.get(), "one seed per evaluation");
        eq(4, folds.get(), "one fold per input row");
        M3VirtualCollector<long[], Long> sum = accumulator.finish(state -> state[1]);
        M3VirtualCollector<long[], Long> max = dag.virtualCollector(() -> new long[]{Long.MIN_VALUE},
                (state, value) -> state[0] = Math.max(state[0], value), state -> state[0]);
        eq(24, sum.evaluate().longValue(), "finish reducer");
        eq(9, max.evaluate().longValue(), "max reducer");
        var tee = sum.tee(max, (s, m) -> s + m);
        eq(33, tee.evaluate().longValue(), "single-pass tee result");
        eq(39, sum.mapResult(value -> value + 15).evaluate().longValue(), "deferred result mapping");
        raises(IllegalArgumentException.class, () -> sum.tee(
                M3LongDag.from(owner).virtualCollector(() -> new long[1],
                        (state, value) -> state[0]++, state -> state[0]), Long::sum),
                "tee must refuse different plan identity");
        owner.add(100);
        eq(124, sum.evaluate().longValue(), "fresh state after live mutation");
        eq(5, starts.get(), "all evaluations allocate fresh accumulator state");
    }

    private static void batchBoundaries() {
        var dag = M3LongDag.from(M3LongArrayList.copyOf(new long[]{1, 2, 3, 4, 5}));
        try (var batches = dag.batches(2)) {
            var result = batches.toList();
            eq(3, result.size(), "three batches");
            eq(new long[]{1, 2}, result.get(0), "first bounded batch");
            eq(new long[]{3, 4}, result.get(1), "second bounded batch");
            eq(new long[]{5}, result.get(2), "partial trailing batch");
        }
        raises(IllegalArgumentException.class, () -> dag.batches(0), "zero batch width");
        raises(IllegalArgumentException.class, () -> dag.batches(65_537), "oversized batch width");
        try (var empty = M3LongDag.from(M3LongArrayList.copyOf(new long[0])).batches(1)) {
            eq(0, empty.count(), "empty batch stream");
        }
    }

    private static void distinctAndBudgetRefusal() {
        M3LongArrayList owner = M3LongArrayList.copyOf(new long[]{Long.MIN_VALUE, 0, 0, Long.MAX_VALUE, Long.MIN_VALUE, 7});
        var plan = M3LongDag.from(owner);
        eq(new long[]{Long.MIN_VALUE, 0, Long.MAX_VALUE, 7},
                plan.distinctBounded(4).toArrayBounded(4), "first-seen order and extreme longs");
        raises(IllegalStateException.class, () -> plan.distinctBounded(3).count(), "distinct budget failure");
        raises(IllegalArgumentException.class, () -> plan.distinctBounded(-1), "negative distinct budget");
        eq(6, owner.size(), "set stage must not alter original collection");
        eq(0, M3LongDag.from(M3LongArrayList.copyOf(new long[0])).sortedBounded(0).count(), "empty sort budget zero");
    }

    private static void sourceClosureAndFailures() {
        AtomicInteger closed = new AtomicInteger();
        M3LongCollection owner = new M3LongCollection() {
            public int size() { return 3; }
            public boolean contains(long value) { return value >= 1 && value <= 3; }
            public void clear() { throw new UnsupportedOperationException(); }
            public M3LongIterator iterator() { throw new AssertionError("stream override expected"); }
            public long[] toArray() { return new long[]{1, 2, 3}; }
            public LongStream longStream() { return LongStream.of(1, 2, 3).onClose(closed::incrementAndGet); }
        };
        var dag = M3LongDag.from(owner);
        raises(IllegalStateException.class, () -> dag.map(v -> { if (v == 2) throw new IllegalStateException(); return v; })
                .virtualAccumulator(() -> new int[1], (state, value) -> state[0]++).evaluate(),
                "callback error propagates");
        eq(1, closed.get(), "callback failure closes upstream");
        raises(IllegalStateException.class, () -> dag.sortedBounded(2).count(), "sorted overflow closes source");
        eq(2, closed.get(), "sorted failure closes upstream");
        try (var batches = dag.batches(2)) { batches.findFirst(); }
        eq(3, closed.get(), "short-circuit batched stream closes upstream");
    }

    private static void randomizedOracle() {
        Random random = new Random(0x6d3a4beL);
        for (int round = 0; round < 120; round++) {
            int count = random.nextInt(400);
            long[] rows = new long[count];
            for (int i = 0; i < count; i++) rows[i] = random.nextInt(101) - 50;
            var owner = M3LongArrayList.copyOf(rows);
            var dag = M3LongDag.from(owner).filter(x -> x % 3 != 0).map(x -> x * 2)
                    .distinctBounded(101).sortedBounded(101);
            long[] expected = Arrays.stream(rows).filter(x -> x % 3 != 0).map(x -> x * 2).distinct().sorted().toArray();
            eq(expected, dag.toArrayBounded(101), "random JDK stream differential " + round);
            eq(rows, owner.toArray(), "immutable owner under composition " + round);
        }
    }

    private static void jdkCollectorAndNativeMaskBoundary() {
        M3LongArrayList rows = M3LongArrayList.copyOf(new long[]{3, 1, -1, 5});
        M3LongDag dag = M3LongDag.filtered(rows, x -> x > 0);
        eq(List.of(3L, 1L, 5L), dag.collect(Collectors.toList()), "JDK Collector bridge");
        int[] order = {3, 0, 2, 1};
        byte[] mask = {(byte) 1, (byte) 0, (byte) 1, (byte) 1};
        eq(new int[]{3, 2, 1}, M3IndexMaskCompactor.select(order, mask, 1,
                M3IndexMaskCompactor.Backend.JAVA), "existing Java mask oracle");
        if (M3IndexMaskCompactor.nativeAvailable()) {
            eq(M3IndexMaskCompactor.select(order, mask, 1, M3IndexMaskCompactor.Backend.JAVA),
                    M3IndexMaskCompactor.select(order, mask, 1, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED),
                    "existing JNI parity (when available)");
        } else {
            raises(UnsatisfiedLinkError.class, () -> M3IndexMaskCompactor.select(order, mask, 1,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED), "missing JNI is explicit");
        }
    }

    private static void eq(long expected, long actual, String message) {
        checks++;
        if (expected != actual) throw new AssertionError(message + ": " + expected + " != " + actual);
    }
    private static void eq(Object expected, Object actual, String message) {
        checks++;
        if (!Objects.equals(expected, actual)) throw new AssertionError(message + ": " + expected + " != " + actual);
    }
    private static void eq(long[] expected, long[] actual, String message) {
        checks++;
        if (!Arrays.equals(expected, actual)) throw new AssertionError(message + ": " + Arrays.toString(expected) + " != " + Arrays.toString(actual));
    }
    private static void eq(int[] expected, int[] actual, String message) {
        checks++;
        if (!Arrays.equals(expected, actual)) throw new AssertionError(message);
    }
    private static void raises(Class<? extends Throwable> type, Runnable action, String message) {
        checks++;
        try { action.run(); }
        catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError(message + " got " + thrown, thrown);
        }
        throw new AssertionError(message + " did not fail");
    }
}
