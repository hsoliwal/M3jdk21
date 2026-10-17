// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.parallel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Executable contract probes, not a substitute for Maven/OpenRewrite application. */
public final class M3SchedulingSelfTest {
    private static int cases;
    private M3SchedulingSelfTest() { }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void test(String name, Checked work) throws Exception {
        work.run(); cases++; System.out.println("PASS " + name);
    }
    private static void check(boolean valid, String why) { if (!valid) throw new AssertionError(why); }
    private static <E extends Throwable> E expect(Class<E> type, Checked work) {
        try { work.run(); } catch (Throwable error) {
            if (type.isInstance(error)) return type.cast(error);
            throw new AssertionError("expected " + type + " got " + error, error);
        }
        throw new AssertionError("expected " + type);
    }
    private static void await(CountDownLatch latch) {
        try { check(latch.await(10, TimeUnit.SECONDS), "latch timeout"); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
    }
    private static void get(Future<?> result) throws Exception { result.get(20, TimeUnit.SECONDS); }
    private static M3ReadWrite<Integer> three(ForkJoinPool pool) {
        return M3Parallel.byReadWrite(List.of(0, 1, 2, 3),
                row -> row == 2 ? List.of() : List.of("x"),
                row -> row == 2 ? List.of("x") : List.of()).using(pool);
    }
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--missing-native")) {
            test("minimum Java path survives unavailable native library", () -> {
                expect(LinkageError.class, () -> M3LongWindows.min(new long[]{2, 1}, 2, M3Parallel.Backend.NATIVE));
                check(Arrays.equals(M3LongWindows.min(new long[]{2, 1}, 2), new long[]{1}), "Java fallback independent");
            });
            System.out.println("CASES " + cases); return;
        }
        boolean nativeRun = args.length > 0 && args[0].equals("--native");
        test("readers overlap; writers and later reads preserve conflicts", () -> {
            try (ForkJoinPool pool = new ForkJoinPool(4); ExecutorService coordinator = Executors.newSingleThreadExecutor()) {
                CountDownLatch readers = new CountDownLatch(2), release = new CountDownLatch(1);
                AtomicInteger active = new AtomicInteger(), wrote = new AtomicInteger();
                Future<?> result = coordinator.submit(() -> three(pool).forEach(row -> {
                    if (row < 2) { active.incrementAndGet(); readers.countDown(); await(release); active.decrementAndGet(); }
                    if (row == 2) { check(active.get() == 0, "writer overlaps readers"); wrote.set(1); }
                    if (row == 3) check(wrote.get() == 1, "read passed writer");
                }));
                try { await(readers); check(active.get() == 2, "no reader overlap"); }
                finally { release.countDown(); }
                get(result); check(!pool.isShutdown(), "borrowed pool shut down");
            }
        });
        test("dynamic ready release does not wait for unrelated callback", () -> {
            try (ForkJoinPool pool = new ForkJoinPool(3)) {
                CountDownLatch started = new CountDownLatch(1), successor = new CountDownLatch(1);
                M3Parallel.byReadWrite(List.of(0, 1, 2), row -> List.of(),
                        row -> List.of(row == 0 ? "unrelated" : "chain")).using(pool).forEach(row -> {
                    if (row == 0) { started.countDown(); await(successor); }
                    else if (row == 1) await(started);
                    else successor.countDown();
                });
            }
        });
        test("null equal collision and duplicate resource identities", () -> {
            record Key(int id) { @Override public int hashCode() { return 0; } }
            List<Key> resources = Arrays.asList(null, new Key(1), new Key(1), new Key(2));
            var plan = M3Parallel.byReadWrite(List.of(0, 1, 2), row -> resources, row -> resources);
            check(plan.explain().resources() == 3 && plan.explain().dependencies() == 2, "deduplication");
            check(plan.explain().touches() == 24, "duplicates must count toward admission");
            AtomicInteger serial = new AtomicInteger(); plan.forEach(row -> check(serial.getAndIncrement() == row, "source conflict order"));
        });
        test("read-only graph has no reader-reader edges", () -> {
            List<Integer> rows = new ArrayList<>(); for (int i = 0; i < 1000; i++) rows.add(i);
            var plan = M3Parallel.byReadWrite(rows, row -> List.of("r"), row -> List.of());
            check(plan.explain().dependencies() == 0 && plan.explain().graphBytes() == 8000, "read/read overhead");
            AtomicInteger seen = new AtomicInteger(); plan.forEach(row -> seen.incrementAndGet()); check(seen.get() == 1000, "lost ready row");
        });
        test("row and resource budgets refuse before execution", () -> {
            AtomicInteger selectors = new AtomicInteger();
            expect(IllegalArgumentException.class, () -> M3Parallel.byReadWrite(List.of(1, 2), row -> {
                selectors.incrementAndGet(); return List.of("r");
            }, row -> List.of(), new M3ReadWrite.Budget(1, 10, 10)));
            check(selectors.get() == 0, "row cap too late");
            expect(IllegalArgumentException.class, () -> M3Parallel.byReadWrite(List.of(1), row -> List.of("r", "r"),
                    row -> List.of(), new M3ReadWrite.Budget(1, 1, 0)));
            expect(IllegalArgumentException.class, () -> M3Parallel.byReadWrite(List.of(1, 2), row -> List.of(),
                    row -> List.of("r"), new M3ReadWrite.Budget(2, 2, 0)));
            expect(IllegalArgumentException.class, () -> new M3ReadWrite.Budget(-1, 0, 0));
        });
        test("read/write argument validation and empty cancellation", () -> {
            expect(NullPointerException.class, () -> M3Parallel.byReadWrite(List.of(1), row -> null, row -> List.of()));
            var plan = M3Parallel.byReadWrite(List.<Integer>of(), row -> List.of(), row -> List.of());
            check(plan.mapOrdered(row -> row).isEmpty(), "empty result");
            expect(CancellationException.class, () -> plan.cancelWhen(() -> true).forEach(row -> { throw new AssertionError(); }));
        });
        test("ordered mapping and reusable independent executions", () -> {
            var plan = M3Parallel.byReadWrite(Arrays.asList(3, 1, 2, null), row -> Collections.singletonList(row), row -> List.of());
            check(plan.mapOrdered(row -> row).equals(Arrays.asList(3, 1, 2, null)), "output order/null");
            expect(UnsupportedOperationException.class, () -> plan.mapOrdered(row -> row).add(4));
            check(plan.mapOrdered(row -> row == null ? 9 : row).equals(List.of(3, 1, 2, 9)), "reuse");
        });
        test("single-worker nested read/write execution", () -> {
            try (ForkJoinPool pool = new ForkJoinPool(1)) {
                get(pool.submit(() -> three(pool).forEach(row -> {
                    M3Parallel.byReadWrite(List.of(row), x -> List.of(), x -> List.of("nested"))
                            .using(pool).forEach(x -> check(x.equals(row), "nested value"));
                })));
            }
        });
        test("callback failure drains running readers before terminal return", () -> {
            try (ForkJoinPool pool = new ForkJoinPool(3); ExecutorService coordinator = Executors.newSingleThreadExecutor()) {
                CountDownLatch running = new CountDownLatch(2), release = new CountDownLatch(1), throwing = new CountDownLatch(1);
                RuntimeException marker = new RuntimeException("callback-marker"); AtomicInteger after = new AtomicInteger();
                Future<?> result = coordinator.submit(() -> {
                    Throwable failure = expect(RuntimeException.class, () -> three(pool).forEach(row -> {
                        if (row < 2) { running.countDown(); await(running); }
                        if (row == 0) { throwing.countDown(); throw marker; }
                        if (row == 1) { await(release); after.incrementAndGet(); }
                        if (row >= 2) throw new AssertionError("dependent ran after failure");
                    }));
                    check(failure == marker && after.get() == 1, "failure identity or early return");
                });
                try { await(throwing); check(!result.isDone(), "did not drain active reader"); }
                finally { release.countDown(); }
                get(result);
            }
        });
        test("300 read/write traces match sequential reference including visibility", () -> {
            Random random = new Random(731); try (ForkJoinPool pool = new ForkJoinPool(4)) {
                for (int trial = 0; trial < 300; trial++) {
                    int n = 1 + random.nextInt(60); List<Integer> rows = new ArrayList<>();
                    int[] reads = new int[n], writes = new int[n];
                    for (int row = 0; row < n; row++) { rows.add(row); reads[row] = random.nextInt(256); writes[row] = random.nextInt(256); }
                    long[] expectedState = new long[8], actualState = new long[8], expected = new long[n], actual = new long[n];
                    for (int row : rows) expected[row] = apply(row, reads, writes, expectedState);
                    M3Parallel.byReadWrite(rows, row -> keys(reads[row]), row -> keys(writes[row]))
                            .using(pool).forEach(row -> actual[row] = apply(row, reads, writes, actualState));
                    check(Arrays.equals(expectedState, actualState) && Arrays.equals(expected, actual), "RW trace " + trial);
                }
            }
        });
        test("async cancelled before queued start; no callback", () -> {
            AtomicReference<Runnable> queued = new AtomicReference<>(); AtomicInteger called = new AtomicInteger();
            var execution = M3Parallel.by(List.of(1), x -> x).forEachAsync(queued::set, x -> called.incrementAndGet());
            check(execution.requestCancellation() && !execution.requestCancellation(), "cancel idempotence");
            check(!execution.completion().toCompletableFuture().isDone(), "premature completion"); queued.get().run();
            CompletionException failure = expect(CompletionException.class, () -> execution.completion().toCompletableFuture().join());
            check(failure.getCause() instanceof CancellationException && called.get() == 0, "prestart cancellation");
            check(!execution.requestCancellation(), "cancel accepted after finished");
        });
        test("async cancellation completion waits for callback quiescence", () -> {
            try (ForkJoinPool pool = new ForkJoinPool(2); ExecutorService coordinator = Executors.newSingleThreadExecutor()) {
                CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1); AtomicInteger stopped = new AtomicInteger();
                var execution = M3Parallel.by(List.of(1, 2), x -> 0).using(pool).forEachAsync(coordinator, x -> {
                    started.countDown(); await(release); stopped.incrementAndGet();
                });
                try {
                    await(started); check(execution.requestCancellation(), "cancel not accepted");
                    check(!execution.completion().toCompletableFuture().isDone(), "cancel completed before drain");
                } finally { release.countDown(); }
                CompletionException failure = expect(CompletionException.class, () -> execution.completion().toCompletableFuture().join());
                check(failure.getCause() instanceof CancellationException && stopped.get() == 1, "drain/cancel boundary");
            }
        });
        test("async ordered success and read-only completion stage", () -> {
            AtomicReference<Runnable> queued = new AtomicReference<>();
            var execution = M3Parallel.by(List.of(3, 1, 2), x -> x).mapOrderedAsync(queued::set, x -> x * 2);
            execution.completion().toCompletableFuture().complete(List.of(999));
            check(!execution.completion().toCompletableFuture().isDone(), "external future completion escaped");
            queued.get().run(); check(execution.completion().toCompletableFuture().join().equals(List.of(6, 2, 4)), "async order");
            check(!execution.requestCancellation(), "finished cancellation");
        });
        test("async rejection and failure retain original causes", () -> {
            var plan = M3Parallel.by(List.of(1), x -> x);
            expect(RejectedExecutionException.class, () -> plan.forEachAsync(work -> { throw new RejectedExecutionException(); }, x -> { }));
            RuntimeException marker = new RuntimeException("async-marker");
            var execution = plan.forEachAsync(Runnable::run, x -> { throw marker; });
            check(expect(CompletionException.class, () -> execution.completion().toCompletableFuture().join()).getCause() == marker, "async failure identity");
        });
        test("read/write async shares conflict and completion contract", () -> {
            try (ForkJoinPool pool = new ForkJoinPool(2); ExecutorService coordinator = Executors.newSingleThreadExecutor()) {
                AtomicInteger serial = new AtomicInteger();
                var execution = M3Parallel.byReadWrite(List.of(0, 1, 2), x -> List.of(), x -> List.of("w"))
                        .using(pool).forEachAsync(coordinator, x -> check(serial.getAndIncrement() == x, "async dependency"));
                execution.completion().toCompletableFuture().get(20, TimeUnit.SECONDS);
                check(serial.get() == 3, "async count");
            }
        });
        test("cache count bound and access-order eviction", () -> {
            M3PlanCache<String, String> cache = new M3PlanCache<>(2, 20);
            cache.put("a", "A", 3); cache.put("b", "B", 4); check(cache.get("a").equals("A"), "cache hit");
            cache.put("c", "C", 5); check(cache.get("b") == null && cache.explain().weight() == 8, "LRU eviction");
            check(cache.put("a", "AA", 6).equals("A") && cache.explain().weight() == 11, "replacement weight");
            check(cache.invalidate("c").equals("C") && cache.explain().entries() == 1, "invalidation");
        });
        test("cache weighted trim null keys and immutable admission failure", () -> {
            M3PlanCache<String, Object> cache = new M3PlanCache<>(4, 9);
            Object value = new Object(); cache.put(null, value, 4); cache.put("a", "A", 4); cache.get(null);
            expect(IllegalArgumentException.class, () -> cache.put(null, new Object(), 10));
            check(cache.get(null) == value && cache.explain().weight() == 8, "failed replacement changed cache");
            check(cache.trimTo(4) == 1 && cache.get(null) == value, "trim recency");
            cache.clear(); check(cache.explain().entries() == 0 && cache.explain().weight() == 0, "clear");
        });
        test("cache zero weights and long overflow boundaries", () -> {
            M3PlanCache<Integer, Integer> cache = new M3PlanCache<>(3, Long.MAX_VALUE);
            cache.put(1, 1, Long.MAX_VALUE); cache.put(2, 2, Long.MAX_VALUE);
            check(cache.get(1) == null && cache.explain().weight() == Long.MAX_VALUE, "weight overflow");
            cache.put(3, 3, 0); cache.put(4, 4, 0); cache.put(5, 5, 0);
            check(cache.explain().entries() == 3 && cache.explain().weight() == 0, "count bound for free entries");
            check(cache.trimTo(0) == 0, "zero trim"); cache.clear();
            expect(IllegalArgumentException.class, () -> new M3PlanCache<>(0, 0).put(1, 1, 0));
            expect(IllegalArgumentException.class, () -> cache.put(1, 1, -1));
            expect(NullPointerException.class, () -> cache.put(1, null, 0));
        });
        test("20000 cache operations match independent access-order reference", () -> {
            M3PlanCache<Integer, Integer> cache = new M3PlanCache<>(8, 30);
            LinkedHashMap<Integer, Weighted> reference = new LinkedHashMap<>(16, 0.75f, true); Random random = new Random(146);
            for (int step = 0; step < 20000; step++) {
                int key = random.nextInt(16), kind = random.nextInt(5);
                if (kind < 2) {
                    int value = random.nextInt(), weight = random.nextInt(12);
                    Weighted old = reference.remove(key); Integer actual = cache.put(key, value, weight);
                    check(java.util.Objects.equals(actual, old == null ? null : old.value()), "cache replace trace");
                    while (reference.size() >= 8 || weight(reference) > 30 - weight) reference.remove(reference.keySet().iterator().next());
                    reference.put(key, new Weighted(value, weight));
                } else if (kind == 2) {
                    Weighted old = reference.get(key); check(java.util.Objects.equals(cache.get(key), old == null ? null : old.value()), "cache lookup trace");
                } else if (kind == 3) {
                    Weighted old = reference.remove(key); check(java.util.Objects.equals(cache.invalidate(key), old == null ? null : old.value()), "cache remove trace");
                } else {
                    long goal = random.nextInt(31); int removed = 0;
                    while (weight(reference) > goal) { reference.remove(reference.keySet().iterator().next()); removed++; }
                    check(cache.trimTo(goal) == removed, "cache trim trace");
                }
                check(cache.explain().entries() == reference.size() && cache.explain().weight() == weight(reference), "cache state trace " + step);
            }
        });
        test("cache concurrent operations retain admission invariants", () -> {
            M3PlanCache<Integer, Integer> cache = new M3PlanCache<>(32, 100);
            try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
                List<Future<?>> runs = new ArrayList<>();
                for (int worker = 0; worker < 4; worker++) { final int seed = worker;
                    runs.add(executor.submit(() -> {
                        Random random = new Random(seed);
                        for (int i = 0; i < 2000; i++) {
                            int key = random.nextInt(64); cache.put(key, key, random.nextInt(8)); cache.get(key);
                            if (i % 7 == 0) cache.invalidate(random.nextInt(64));
                            var stats = cache.explain(); check(stats.weight() <= 100 && stats.entries() <= 32, "cache concurrency bound");
                        }
                    }));
                }
                for (Future<?> run : runs) get(run);
            }
        });
        test("minimum width and extreme-value contracts", () -> {
            long[] input = {Long.MIN_VALUE, Long.MAX_VALUE, -1, 0, Long.MIN_VALUE};
            check(Arrays.equals(M3LongWindows.min(input, 2), new long[]{Long.MIN_VALUE, -1, -1, Long.MIN_VALUE}), "signed extrema");
            check(M3LongWindows.min(input, 6).length == 0 && M3LongWindows.min(new long[0], 1).length == 0, "empty window");
            expect(IllegalArgumentException.class, () -> M3LongWindows.min(input, 0));
            expect(NullPointerException.class, () -> M3LongWindows.min(null, 1));
        });
        test("2000 minimum reference trials with unchanged inputs", () -> minimumTrials(false));
        if (nativeRun) {
            test("2000 JNI minimum reference trials", () -> minimumTrials(true));
            test("JNI direct validation and minimum/maximum ABI coexist", () -> {
                expect(NullPointerException.class, () -> M3PartitionLayout.Native.windowMin(null, 1));
                expect(IllegalArgumentException.class, () -> M3PartitionLayout.Native.windowMin(new long[1], 0));
                check(M3PartitionLayout.Native.windowMin(new long[]{1}, Integer.MAX_VALUE).length == 0, "native oversize width");
                check(Arrays.equals(M3PartitionLayout.Native.windowMax(new long[]{2, 3}, 2), new long[]{3}), "old symbol");
            });
        }
        System.out.println("CASES " + cases);
    }
    private record Weighted(Integer value, long weight) { }
    private static long weight(Map<Integer, Weighted> values) { long total = 0; for (Weighted value : values.values()) total += value.weight(); return total; }
    private static List<Integer> keys(int bits) { List<Integer> keys = new ArrayList<>(); for (int bit = 0; bit < 8; bit++) if ((bits & (1 << bit)) != 0) keys.add(bit); return keys; }
    private static long apply(int row, int[] reads, int[] writes, long[] state) {
        long value = row + 1L;
        for (int bit = 0; bit < 8; bit++) if ((reads[row] & (1 << bit)) != 0) value = value * 31L + state[bit];
        for (int bit = 0; bit < 8; bit++) if ((writes[row] & (1 << bit)) != 0) state[bit] = value + bit;
        return value;
    }
    private static void minimumTrials(boolean nativeRun) {
        Random random = new Random(239);
        for (int trial = 0; trial < 2000; trial++) {
            long[] input = new long[random.nextInt(100)];
            for (int i = 0; i < input.length; i++) input[i] = switch (random.nextInt(10)) {
                case 0 -> Long.MIN_VALUE; case 1 -> Long.MAX_VALUE; default -> random.nextInt(30) - 15;
            };
            long[] before = input.clone(); int width = 1 + random.nextInt(120);
            long[] expected = new long[Math.max(0, input.length - width + 1)];
            for (int start = 0; start < expected.length; start++) {
                expected[start] = Long.MAX_VALUE;
                for (int offset = 0; offset < width; offset++) expected[start] = Math.min(expected[start], input[start + offset]);
            }
            long[] actual = M3LongWindows.min(input, width, nativeRun ? M3Parallel.Backend.NATIVE : M3Parallel.Backend.JAVA);
            check(Arrays.equals(actual, expected) && Arrays.equals(input, before) && input != actual, "minimum trial " + trial);
        }
    }
}
