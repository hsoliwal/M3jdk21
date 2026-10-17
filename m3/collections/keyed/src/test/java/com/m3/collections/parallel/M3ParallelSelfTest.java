// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.parallel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.Function;
import java.util.stream.IntStream;

/** Dependency-free contract runner; Maven/CI run the exact recipe-emitted source, not substitutes. */
public final class M3ParallelSelfTest {
    private static int cases;
    private static volatile int blackhole;
    private M3ParallelSelfTest() { }
    private record Collision(int value) { @Override public int hashCode() { return 1; } }
    public static void main(String[] args) throws Exception {
        if (args.length != 0 && args[0].equals("--missing-native")) {
            expect(UnsatisfiedLinkError.class, () -> M3Parallel.by(List.of(1), x -> x, M3Parallel.Backend.NATIVE));
            equal(List.of(1), M3Parallel.by(List.of(1), x -> x).mapOrdered(x -> x));
            System.out.println("PASS native missing fails explicitly; Java remains usable"); return;
        }
        if (args.length != 0 && args[0].equals("--benchmark")) { benchmark(); return; }
        test("nulls and stable outputs", M3ParallelSelfTest::nulls);
        test("empty operations and argument validation", M3ParallelSelfTest::empty);
        test("collision equality and first-key order", M3ParallelSelfTest::collisions);
        test("snapshot and exactly-once key extraction", M3ParallelSelfTest::snapshot);
        test("dense keys and range refusal", M3ParallelSelfTest::dense);
        test("same-key serialization and encounter order", M3ParallelSelfTest::sameKey);
        test("different keys overlap", M3ParallelSelfTest::overlap);
        test("nonassociative left fold", M3ParallelSelfTest::fold);
        test("partition views and immutability", M3ParallelSelfTest::views);
        test("200 randomized differential traces", M3ParallelSelfTest::randomized);
        test("pre-cancel and mid-key cancellation", M3ParallelSelfTest::cancellation);
        test("failure waits for active callbacks", M3ParallelSelfTest::failureQuiescence);
        test("supplier/predicate/errors propagate", M3ParallelSelfTest::callbackErrors);
        test("borrowed pool and rejection", M3ParallelSelfTest::poolOwnership);
        test("nested same-pool execution", M3ParallelSelfTest::nested);
        test("caller interrupt is preserved", M3ParallelSelfTest::interrupt);
        test("independent concurrent plan reuse", M3ParallelSelfTest::reuse);
        test("Java layout validation", M3ParallelSelfTest::layoutValidation);
        if (Arrays.asList(args).contains("--native")) {
            test("1000 Java/JNI stable-layout parity cases", M3ParallelSelfTest::nativeParity);
            test("JNI null/range/count refusal", M3ParallelSelfTest::nativeValidation);
            test("native-backed terminals", M3ParallelSelfTest::nativeTerminals);
        }
        System.out.println("PASS " + cases + " contract cases");
    }
    private static void nulls() {
        var plan = M3Parallel.by(Arrays.asList("a", null, "bb", "c"), x -> x == null ? null : x.length());
        equal(Arrays.asList(1, null, 2), plan.keys());
        equal(Arrays.asList("a!", null, "bb!", "c!"), plan.mapOrdered(x -> x == null ? null : x + "!"));
        equal(Arrays.asList("a", null, "c"), plan.filter(x -> x == null || x.length() == 1));
        check(plan.foldByKey(() -> null, (a, v) -> a).containsKey(null));
    }
    private static void empty() {
        var plan = M3Parallel.by(List.<Integer>of(), x -> x);
        equal(List.of(), plan.mapOrdered(x -> x)); equal(List.of(), plan.filter(x -> true));
        equal(Map.of(), plan.foldByKey(() -> { throw new AssertionError(); }, (a, x) -> a));
        plan.forEach(x -> { throw new AssertionError(); });
        equal(new M3Parallel.Statistics(0, 0, 0, 4, M3Parallel.Backend.JAVA), plan.explain());
        expect(NullPointerException.class, () -> plan.forEach(null));
        expect(NullPointerException.class, () -> plan.mapOrdered(null));
        expect(NullPointerException.class, () -> plan.filter(null));
        expect(NullPointerException.class, () -> plan.forEachPartition(null));
        expect(NullPointerException.class, () -> M3Parallel.by(null, Function.identity()));
    }
    private static void collisions() {
        var plan = M3Parallel.by(List.of(3, 1, 3, 2, 1), Collision::new);
        equal(List.of(new Collision(3), new Collision(1), new Collision(2)), plan.keys());
        equal(Map.of(new Collision(3), 6, new Collision(1), 2, new Collision(2), 2),
                plan.foldByKey(() -> 0, Integer::sum));
        equal(3, plan.explain().keys()); equal(2, plan.explain().largestPartition());
    }
    private static void snapshot() {
        List<Integer> source = new ArrayList<>(List.of(1, 2, 3)); AtomicInteger count = new AtomicInteger();
        var plan = M3Parallel.by(source, x -> { count.incrementAndGet(); return x % 2; });
        source.clear(); equal(List.of(1, 2, 3), plan.mapOrdered(x -> x));
        equal(List.of(1, 2, 3), plan.mapOrdered(x -> x)); equal(3, count.get());
        equal(24L, plan.explain().indexBytes());
    }
    private static void dense() {
        var plan = M3Parallel.byInt(List.of(9, 1, 9, 4), x -> x, 10);
        equal(List.of(9, 1, 4), plan.keys()); equal(List.of(9, 1, 9, 4), plan.mapOrdered(x -> x));
        expect(IllegalArgumentException.class, () -> M3Parallel.byInt(List.of(10), x -> x, 10));
        expect(IllegalArgumentException.class, () -> M3Parallel.byInt(List.of(-1), x -> x, 10));
        expect(IllegalArgumentException.class, () -> M3Parallel.byInt(List.of(), x -> 0, -1));
        equal(0, M3Parallel.byInt(List.of(), x -> 0, 0).explain().keys());
    }
    private static void sameKey() {
        int groups = 23, n = 23000; AtomicIntegerArray active = new AtomicIntegerArray(groups);
        int[] next = new int[groups];
        try (ForkJoinPool pool = new ForkJoinPool(4)) {
            M3Parallel.byInt(IntStream.range(0, n).boxed().toList(), x -> x % groups, groups).using(pool).forEach(x -> {
                int g = x % groups; check(active.incrementAndGet(g) == 1);
                try { equal(x / groups, next[g]++); if ((x & 63) == 0) Thread.yield(); }
                finally { active.decrementAndGet(g); }
            });
        }
        for (int value : next) equal(1000, value);
    }
    private static void overlap() {
        CountDownLatch both = new CountDownLatch(2);
        try (ForkJoinPool pool = new ForkJoinPool(2)) {
            M3Parallel.by(List.of(0, 1), x -> x).using(pool).forEach(x -> { both.countDown(); await(both); });
        }
    }
    private static void fold() {
        var plan = M3Parallel.by(List.of(1, 2, 3, 4), x -> x % 2);
        equal(Map.of(1, 96, 0, 94), plan.foldByKey(() -> 100, (a, x) -> a - x));
        var result = plan.collectByKey(StringBuilder::new, StringBuilder::append);
        equal("13", result.get(1).toString()); equal("24", result.get(0).toString());
        expect(UnsupportedOperationException.class, () -> result.clear());
    }
    private static void views() {
        var plan = M3Parallel.by(List.of(1, 2, 3), x -> x % 2);
        var result = new java.util.concurrent.ConcurrentHashMap<Integer, List<Integer>>();
        plan.forEachPartition(result::put);
        equal(List.of(1, 3), result.get(1)); equal(List.of(2), result.get(0));
        expect(UnsupportedOperationException.class, () -> result.get(1).set(0, 99));
        expect(IndexOutOfBoundsException.class, () -> result.get(1).get(-1));
        expect(UnsupportedOperationException.class, () -> plan.keys().clear());
        expect(UnsupportedOperationException.class, () -> plan.mapOrdered(x -> x).set(0, 99));
        expect(UnsupportedOperationException.class, () -> plan.filter(x -> true).clear());
    }
    private static void randomized() {
        Random random = new Random(31015);
        try (ForkJoinPool pool = new ForkJoinPool(3)) {
            for (int round = 0; round < 200; round++) {
                List<Integer> source = new ArrayList<>();
                for (int n = random.nextInt(100); n > 0; n--) source.add(random.nextInt(31) - 15);
                Map<Collision, List<Integer>> expected = new LinkedHashMap<>();
                for (int value : source) expected.computeIfAbsent(new Collision(Math.floorMod(value, 7)), k -> new ArrayList<>()).add(value);
                var plan = M3Parallel.by(source, x -> new Collision(Math.floorMod(x, 7))).using(pool);
                equal(new ArrayList<>(expected.keySet()), plan.keys());
                equal(expected, plan.collectByKey(ArrayList<Integer>::new, List::add));
                equal(source.stream().map(x -> x * 2).toList(), plan.mapOrdered(x -> x * 2));
                equal(source.stream().filter(x -> x % 3 == 0).toList(), plan.filter(x -> x % 3 == 0));
            }
        }
    }
    private static void cancellation() {
        AtomicInteger calls = new AtomicInteger();
        var plan = M3Parallel.by(List.of(1, 2, 3, 4), x -> 0);
        expect(CancellationException.class, () -> plan.cancelWhen(() -> true).forEach(x -> calls.incrementAndGet()));
        equal(0, calls.get()); AtomicBoolean cancelled = new AtomicBoolean();
        expect(CancellationException.class, () -> plan.cancelWhen(cancelled::get).forEach(x -> {
            calls.incrementAndGet(); if (x == 2) cancelled.set(true);
        }));
        equal(2, calls.get());
    }
    private static void failureQuiescence() throws Exception {
        CountDownLatch started = new CountDownLatch(2), release = new CountDownLatch(1), failed = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger(); RuntimeException marker = new RuntimeException("expected");
        try (ForkJoinPool pool = new ForkJoinPool(2)) {
            FutureTask<Throwable> future = new FutureTask<>(() -> {
                try { M3Parallel.by(List.of(0, 1), x -> x).using(pool).forEach(x -> {
                    active.incrementAndGet(); started.countDown();
                    try { await(started); if (x == 0) { failed.countDown(); throw marker; } await(release); }
                    finally { active.decrementAndGet(); }
                }); return null; } catch (Throwable error) { return error; }
            });
            Thread thread = new Thread(future); thread.start();
            try {
                await(failed);
                try { future.get(100, TimeUnit.MILLISECONDS); throw new AssertionError("returned before quiescence"); }
                catch (TimeoutException expected) { check(active.get() >= 1); }
            } finally { release.countDown(); }
            check(future.get(10, TimeUnit.SECONDS) == marker); thread.join(); equal(0, active.get());
        }
    }
    private static void callbackErrors() {
        var plan = M3Parallel.by(List.of(1), x -> x);
        expect(IllegalStateException.class, () -> plan.foldByKey(() -> { throw new IllegalStateException(); }, (a, x) -> a));
        expect(AssertionError.class, () -> plan.mapOrdered(x -> { throw new AssertionError(); }));
        expect(IllegalArgumentException.class, () -> plan.cancelWhen(() -> { throw new IllegalArgumentException(); }).forEach(x -> { }));
        AtomicInteger calls = new AtomicInteger();
        expect(IllegalStateException.class, () -> M3Parallel.by(List.of(1, 2), x -> { calls.incrementAndGet(); throw new IllegalStateException(); }));
        equal(1, calls.get());
    }
    private static void poolOwnership() {
        ForkJoinPool pool = new ForkJoinPool(1); var plan = M3Parallel.by(List.of(1), x -> x).using(pool);
        plan.forEach(x -> { }); check(!pool.isShutdown()); pool.shutdown();
        expect(RejectedExecutionException.class, () -> plan.forEach(x -> { throw new AssertionError(); }));
    }
    private static void nested() throws Exception {
        try (ForkJoinPool pool = new ForkJoinPool(2)) {
            var plan = M3Parallel.by(List.of(1, 2, 3), x -> x % 2).using(pool);
            equal(List.of(2, 3, 4), pool.submit(() -> plan.mapOrdered(x -> x + 1)).get(10, TimeUnit.SECONDS));
        }
    }
    private static void interrupt() {
        Thread.currentThread().interrupt();
        try { M3Parallel.by(List.of(1, 2), x -> x).forEach(x -> { }); check(Thread.currentThread().isInterrupted()); }
        finally { Thread.interrupted(); }
    }
    private static void reuse() throws Exception {
        var plan = M3Parallel.by(List.of(1, 2, 3), x -> x % 2);
        FutureTask<List<Integer>> a = new FutureTask<>(() -> plan.mapOrdered(x -> x + 1));
        FutureTask<List<Integer>> b = new FutureTask<>(() -> plan.mapOrdered(x -> x + 2));
        Thread first = new Thread(a), second = new Thread(b); first.start(); second.start();
        equal(List.of(2, 3, 4), a.get(10, TimeUnit.SECONDS)); equal(List.of(3, 4, 5), b.get(10, TimeUnit.SECONDS));
        first.join(); second.join();
    }
    private static void layoutValidation() {
        equal("[0, 3, 5, 0, 2, 4, 1, 3]", Arrays.toString(M3PartitionLayout.scatter(new int[] {0, 1, 0, 1, 0}, 2)));
        expect(NullPointerException.class, () -> M3PartitionLayout.scatter(null, 0));
        expect(IllegalArgumentException.class, () -> M3PartitionLayout.scatter(new int[] {-1}, 1));
        expect(IllegalArgumentException.class, () -> M3PartitionLayout.scatter(new int[] {1}, 1));
        expect(IllegalArgumentException.class, () -> M3PartitionLayout.scatter(new int[0], 1));
        expect(IllegalArgumentException.class, () -> M3PartitionLayout.scatter(new int[0], -1));
    }
    private static void nativeParity() {
        Random random = new Random(20261015);
        for (int round = 0; round < 1000; round++) {
            int n = random.nextInt(1000), groups = n == 0 ? 0 : 1 + random.nextInt(Math.min(50, n));
            int[] ids = new int[n]; for (int i = 0; i < n; i++) ids[i] = random.nextInt(groups);
            int[] copy = ids.clone();
            check(Arrays.equals(M3PartitionLayout.scatter(ids, groups), M3PartitionLayout.Native.scatter(ids, groups)));
            check(Arrays.equals(copy, ids));
        }
    }
    private static void nativeValidation() {
        expect(NullPointerException.class, () -> M3PartitionLayout.Native.scatter(null, 0));
        for (int g : new int[] {-1, 1, Integer.MAX_VALUE})
            expect(IllegalArgumentException.class, () -> M3PartitionLayout.Native.scatter(new int[0], g));
        for (int id : new int[] {-1, 1, Integer.MIN_VALUE, Integer.MAX_VALUE})
            expect(IllegalArgumentException.class, () -> M3PartitionLayout.Native.scatter(new int[] {id}, 1));
    }
    private static void nativeTerminals() {
        var plan = M3Parallel.by(List.of(1, 2, 3, 4), x -> x % 2, M3Parallel.Backend.NATIVE);
        equal(List.of(1, 2, 3, 4), plan.mapOrdered(x -> x)); equal(Map.of(1, 4, 0, 6), plan.foldByKey(() -> 0, Integer::sum));
        equal(M3Parallel.Backend.NATIVE, plan.explain().backend());
        equal(List.of(2, 4), M3Parallel.byInt(List.of(1, 2, 3, 4), x -> x % 2, 2, M3Parallel.Backend.NATIVE).filter(x -> x % 2 == 0));
    }
    private static void benchmark() {
        int n = 1_000_000, groups = 1024, rounds = 20; int[] ids = new int[n];
        for (int i = 0; i < n; i++) ids[i] = i % groups;
        for (int i = 0; i < 5; i++) { blackhole ^= Arrays.hashCode(M3PartitionLayout.scatter(ids, groups)); blackhole ^= Arrays.hashCode(M3PartitionLayout.Native.scatter(ids, groups)); }
        System.out.println("diagnostic only, not JMH: elements=" + n + " keys=" + groups + " rounds=" + rounds);
        for (boolean nativePath : new boolean[] {false, true}) {
            long start = System.nanoTime();
            for (int i = 0; i < rounds; i++) blackhole ^= Arrays.hashCode(nativePath
                    ? M3PartitionLayout.Native.scatter(ids, groups) : M3PartitionLayout.scatter(ids, groups));
            System.out.printf("%s layout+checksum average_ms=%.3f%n", nativePath ? "JNI" : "JAVA", (System.nanoTime() - start) / (rounds * 1_000_000.0));
        }
    }
    private static void await(CountDownLatch latch) {
        try { check(latch.await(10, TimeUnit.SECONDS)); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void test(String name, Checked test) throws Exception { test.run(); cases++; System.out.println("PASS " + name); }
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    private static void equal(Object expected, Object actual) { if (!Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual); }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable error) { if (type.isInstance(error)) return; throw new AssertionError("expected " + type, error); }
        throw new AssertionError("missing " + type);
    }
}
