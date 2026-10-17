// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.parallel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/** Standalone differential, ownership, boundary and JNI qualification of exact recipe inputs. */
public final class M3ExtensionsSelfTest {
    private static int cases;
    private static boolean nativeEnabled;
    private M3ExtensionsSelfTest() { }
    record Item(int key, int score, int id) { }
    record Resources(int id, List<Integer> keys) { }
    record Collision(int id) { @Override public int hashCode() { return 1; } }
    private static final class Marker extends RuntimeException { private static final long serialVersionUID = 1L; }
    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--missing-native")) {
            expect(UnsatisfiedLinkError.class, () -> M3LongWindows.max(new long[]{1}, 1, M3Parallel.Backend.NATIVE));
            same(new long[]{3, 3}, M3LongWindows.max(new long[]{1, 3, 2}, 2));
            System.out.println("PASS window native missing fails explicitly; Java remains usable"); return;
        }
        nativeEnabled = Arrays.asList(args).contains("--native");
        test("stable first and top-k including ties/zero/null", M3ExtensionsSelfTest::topEdges);
        test("500 selection/window/reduction differential traces", M3ExtensionsSelfTest::differential);
        test("noncommutative associative reduction and chunk boundaries", M3ExtensionsSelfTest::reduceOrder);
        test("hot-key chunks actually overlap", M3ExtensionsSelfTest::reduceOverlap);
        test("reduction failure drains active callbacks", M3ExtensionsSelfTest::reduceFailure);
        test("reduction validation/cancellation/null identity", M3ExtensionsSelfTest::reduceEdges);
        test("conflict transitivity/empty sets/collisions/null", M3ExtensionsSelfTest::conflictEdges);
        test("200 conflict-graph differential traces", M3ExtensionsSelfTest::conflictDifferential);
        test("exclusive resources do not overlap", M3ExtensionsSelfTest::conflictExecution);
        test("half-open range boundaries/reverse/null comparators", M3ExtensionsSelfTest::ranges);
        test("stale versions rejected across configuration views", M3ExtensionsSelfTest::versions);
        test("version changes during capture and execution detected", M3ExtensionsSelfTest::versionChanges);
        test("batch bounded prefetch/same-key order/single-use", M3ExtensionsSelfTest::batches);
        test("batch failure stops before pulling next batch", M3ExtensionsSelfTest::batchFailure);
        test("batch cancellation/empty/validation/pool ownership", M3ExtensionsSelfTest::batchEdges);
        test("batch nested same-pool execution", M3ExtensionsSelfTest::batchNested);
        test("window clipped boundaries/read-only/null/large radii", M3ExtensionsSelfTest::windowEdges);
        test("sorted join duplicate/comparator/null semantics", M3ExtensionsSelfTest::joinEdges);
        test("500 merge-join differential traces", M3ExtensionsSelfTest::joinDifferential);
        test("join budget uses long cardinality before effects", M3ExtensionsSelfTest::joinBudget);
        test("join unsorted input/failure/key capture", M3ExtensionsSelfTest::joinFailure);
        test("2000 primitive-window reference traces", M3ExtensionsSelfTest::longWindows);
        test("bounded claims never wrap at maximum integer", M3ExtensionsSelfTest::claims);
        test("new terminals preserve plan immutability and reuse", M3ExtensionsSelfTest::reuse);
        if (nativeEnabled) {
            test("native window argument validation", M3ExtensionsSelfTest::nativeEdges);
            test("native conflict plan and new terminals", M3ExtensionsSelfTest::nativePlan);
        }
        System.out.println("PASS " + cases + " extension contract cases");
    }
    private static void topEdges() {
        var p = M3Parallel.by(List.of(new Item(0, 2, 0), new Item(1, 3, 1), new Item(0, 2, 2), new Item(0, 1, 3)), Item::key);
        eq(List.of(0, 1), p.firstByKey().stream().map(Item::id).toList());
        var top = p.topByKey(2, Comparator.comparingInt(Item::score).reversed());
        eq(List.of(0, 2), top.get(0).stream().map(Item::id).toList());
        eq(List.of(0, 2, 3), p.topByKey(Integer.MAX_VALUE, Comparator.comparingInt(Item::score).reversed()).get(0).stream().map(Item::id).toList());
        eq(List.of(), p.topByKey(0, (a, b) -> { throw new AssertionError("k=0 compared"); }).get(0));
        expect(IllegalArgumentException.class, () -> p.topByKey(-1, Comparator.comparingInt(Item::score)));
        expect(NullPointerException.class, () -> p.topByKey(1, null));
        expect(UnsupportedOperationException.class, () -> top.get(0).clear());
        expect(UnsupportedOperationException.class, () -> top.clear());
        var nullable = M3Parallel.by(Arrays.asList(null, "b", "a", null), x -> 0);
        eq(Arrays.asList(null, null, "a"), nullable.topByKey(3, Comparator.nullsFirst(Comparator.<String>naturalOrder())).get(0));
        eq(Arrays.asList(null, "b", "a"), M3Parallel.by(Arrays.asList(null, "b", "a", null), Function.identity()).firstByKey());
    }
    private static void differential() {
        Random random = new Random(61947);
        try (ForkJoinPool pool = new ForkJoinPool(4)) {
            for (int trial = 0; trial < 500; trial++) {
                List<Item> source = new ArrayList<>(); Map<Integer, List<Item>> groups = new LinkedHashMap<>();
                for (int i = 0, n = random.nextInt(130); i < n; i++) {
                    Item item = new Item(random.nextInt(9), random.nextInt(11) - 5, i);
                    source.add(item); groups.computeIfAbsent(item.key(), k -> new ArrayList<>()).add(item);
                }
                var p = M3Parallel.by(source, Item::key).using(pool);
                int k = random.nextInt(20), before = random.nextInt(6), after = random.nextInt(6);
                Comparator<Item> order = Comparator.comparingInt(Item::score).reversed();
                Map<Integer, List<Item>> expected = new LinkedHashMap<>();
                groups.forEach((key, rows) -> expected.put(key, rows.stream().sorted(order).limit(k).toList()));
                eq(expected, p.topByKey(k, order));
                eq(groups.values().stream().map(List::getFirst).toList(), p.firstByKey());
                List<List<Integer>> windows = p.mapWindows(before, after, (center, rows) -> rows.stream().map(Item::id).toList());
                for (List<Item> rows : groups.values()) for (int at = 0; at < rows.size(); at++)
                    eq(rows.subList(Math.max(0, at - before), Math.min(rows.size(), at + after + 1)).stream().map(Item::id).toList(), windows.get(rows.get(at).id()));
                Map<Integer, Long> sum = new LinkedHashMap<>();
                groups.forEach((key, rows) -> sum.put(key, rows.stream().mapToLong(Item::score).sum()));
                eq(sum, p.reduceByKey(1 + random.nextInt(33), () -> 0L, x -> (long) x.score(), Long::sum));
            }
        }
    }
    private static void reduceOrder() {
        List<Integer> source = java.util.stream.IntStream.range(0, 513).boxed().toList();
        var p = M3Parallel.by(source, x -> x % 3);
        var sequential = p.foldByKey(() -> "", (s, x) -> s + "[" + x + "]");
        for (int chunk : new int[]{1, 2, 3, 31, 171, 172, 512, Integer.MAX_VALUE})
            eq(sequential, p.reduceByKey(chunk, () -> "", x -> "[" + x + "]", String::concat));
        eq(sequential, p.reduceByKey(() -> "", x -> "[" + x + "]", String::concat));
    }
    private static void reduceOverlap() {
        AtomicInteger active = new AtomicInteger(), max = new AtomicInteger(), lifts = new AtomicInteger();
        CountDownLatch together = new CountDownLatch(2);
        try (ForkJoinPool pool = new ForkJoinPool(2)) {
            var result = M3Parallel.by(List.of(1, 2, 3, 4), x -> 0).using(pool).reduceByKey(1, () -> 0, x -> {
                int a = active.incrementAndGet(); max.accumulateAndGet(a, Math::max); lifts.incrementAndGet();
                try { together.countDown(); await(together); return x; } finally { active.decrementAndGet(); }
            }, Integer::sum);
            eq(10, result.get(0)); eq(4, lifts.get()); check(max.get() >= 2, "hot key was not split"); eq(0, active.get());
        }
    }
    private static void reduceFailure() {
        AtomicInteger active = new AtomicInteger(); CountDownLatch other = new CountDownLatch(1);
        try (ForkJoinPool pool = new ForkJoinPool(2)) {
            var p = M3Parallel.by(List.of(0, 1), x -> 0).using(pool);
            expect(Marker.class, () -> p.reduceByKey(1, () -> 0, x -> {
                if (x == 0) { await(other); throw new Marker(); }
                active.incrementAndGet(); other.countDown();
                try { pause(); return x; } finally { active.decrementAndGet(); }
            }, Integer::sum));
            eq(0, active.get()); eq(List.of(0, 1), p.mapOrdered(Function.identity()));
        }
    }
    private static void reduceEdges() {
        var empty = M3Parallel.<Integer, Integer>by(List.of(), Function.identity());
        eq(Map.of(), empty.reduceByKey(1, () -> { throw new AssertionError(); }, x -> x, Integer::sum));
        var p = M3Parallel.by(List.of(1, 2, 3), x -> 0);
        expect(IllegalArgumentException.class, () -> p.reduceByKey(0, () -> 0, x -> x, Integer::sum));
        expect(NullPointerException.class, () -> p.reduceByKey(1, null, x -> x, Integer::sum));
        expect(CancellationException.class, () -> p.cancelWhen(() -> true).reduceByKey(1, () -> 0, x -> x, Integer::sum));
        eq("123", p.reduceByKey(1, () -> (String) null, Object::toString, (a, b) -> a == null ? b : b == null ? a : a + b).get(0));
    }
    private static void conflictEdges() {
        List<Resources> rows = List.of(new Resources(0, List.of(1)), new Resources(1, List.of(2)),
                new Resources(2, List.of(1, 2)), new Resources(3, List.of()), new Resources(4, List.of()));
        var p = M3Parallel.byKeys(rows, Resources::keys);
        eq(List.of(0, 3, 4), p.keys());
        eq(Map.of(0, List.of(0, 1, 2), 3, List.of(3), 4, List.of(4)), p.foldByKey(ArrayList<Integer>::new, (a, r) -> { a.add(r.id()); return a; }));
        eq(2, M3Parallel.byKeys(List.of(0, 1), x -> List.of(new Collision(x))).explain().keys());
        eq(1, M3Parallel.byKeys(List.of(0, 1), x -> Arrays.asList(null, null)).explain().keys());
        expect(NullPointerException.class, () -> M3Parallel.byKeys(List.of(1), x -> null));
        eq(0, M3Parallel.byKeys(List.of(), x -> List.of()).explain().keys());
    }
    private static void conflictDifferential() {
        Random random = new Random(82017);
        for (int trial = 0; trial < 200; trial++) {
            List<Resources> rows = new ArrayList<>(); int n = random.nextInt(40);
            for (int i = 0; i < n; i++) {
                List<Integer> keys = new ArrayList<>();
                for (int k = 0, m = random.nextInt(4); k < m; k++) keys.add(random.nextInt(20));
                rows.add(new Resources(i, keys));
            }
            int[] component = new int[n]; Arrays.fill(component, -1);
            for (int i = 0; i < n; i++) if (component[i] < 0) {
                component[i] = i; boolean changed;
                do {
                    changed = false;
                    for (int a = 0; a < n; a++) if (component[a] == i)
                        for (int b = 0; b < n; b++) if (component[b] < 0 && !Collections.disjoint(rows.get(a).keys(), rows.get(b).keys())) {
                            component[b] = i; changed = true;
                        }
                } while (changed);
            }
            Map<Integer, List<Integer>> expected = new LinkedHashMap<>();
            for (int i = 0; i < n; i++) expected.computeIfAbsent(component[i], k -> new ArrayList<>()).add(i);
            AtomicInteger calls = new AtomicInteger();
            var p = M3Parallel.byKeys(rows, r -> { calls.incrementAndGet(); return r.keys(); });
            eq(n, calls.get()); eq(new ArrayList<>(expected.keySet()), p.keys());
            eq(expected, p.foldByKey(ArrayList<Integer>::new, (a, r) -> { a.add(r.id()); return a; }));
        }
    }
    private static void conflictExecution() {
        List<Resources> rows = new ArrayList<>();
        for (int i = 0; i < 10000; i++) rows.add(new Resources(i, List.of((i % 8) * 2, (i % 8) * 2 + 1)));
        AtomicIntegerArray active = new AtomicIntegerArray(16); int[] last = new int[8]; Arrays.fill(last, -1);
        try (ForkJoinPool pool = new ForkJoinPool(4)) {
            M3Parallel.byKeys(rows, Resources::keys).using(pool).forEach(r -> {
                int g = r.id() % 8;
                for (int key : r.keys()) eq(1, active.incrementAndGet(key));
                try { check(last[g] < r.id(), "conflict encounter order"); last[g] = r.id(); Thread.yield(); }
                finally { for (int key : r.keys()) active.decrementAndGet(key); }
            });
        }
        for (int i = 0; i < 16; i++) eq(0, active.get(i));
    }
    private static void ranges() {
        var p = M3Parallel.byRange(List.of(20, -1, 0, 9, 10), Function.identity(), List.of(0, 10), Comparator.naturalOrder());
        eq(List.of(2, 0, 1), p.keys());
        eq(Map.of(2, List.of(20, 10), 0, List.of(-1), 1, List.of(0, 9)), p.collectByKey(ArrayList<Integer>::new, List::add));
        eq(Map.of(0, List.of(20), 1, List.of(10, 1), 2, List.of(0, -1)), M3Parallel.byRange(List.of(20, 10, 1, 0, -1), Function.identity(), List.of(10, 0), Comparator.reverseOrder()).collectByKey(ArrayList<Integer>::new, List::add));
        eq(List.of(1, 2), M3Parallel.byRange(Arrays.asList(null, 0), Function.identity(), Arrays.asList(null, 0), Comparator.nullsFirst(Comparator.<Integer>naturalOrder())).keys());
        expect(IllegalArgumentException.class, () -> M3Parallel.byRange(List.of(1), Function.identity(), List.of(2, 1), Comparator.naturalOrder()));
        expect(IllegalArgumentException.class, () -> M3Parallel.byRange(List.of(1), Function.identity(), List.of(1, 1), Comparator.naturalOrder()));
        eq(List.of(0), M3Parallel.byRange(List.of(1), Function.identity(), List.<Integer>of(), Comparator.naturalOrder()).keys());
    }
    private static void versions() {
        AtomicLong version = new AtomicLong(1); AtomicInteger calls = new AtomicInteger();
        var p = M3Parallel.byVersioned(List.of(1, 2, 3), x -> x % 2, version::get);
        eq(List.of(1, 2, 3), p.mapOrdered(Function.identity()));
        try (ForkJoinPool pool = new ForkJoinPool(2)) {
            var view = p.using(pool).cancelWhen(() -> false); version.incrementAndGet();
            expect(ConcurrentModificationException.class, () -> view.forEach(x -> calls.incrementAndGet()));
            expect(ConcurrentModificationException.class, p::keys); expect(ConcurrentModificationException.class, p::explain);
            expect(ConcurrentModificationException.class, p::firstByKey); eq(0, calls.get());
        }
    }
    private static void versionChanges() {
        AtomicLong version = new AtomicLong();
        expect(ConcurrentModificationException.class, () -> M3Parallel.byVersioned(List.of(1), x -> { version.incrementAndGet(); return x; }, version::get));
        var p = M3Parallel.byVersioned(List.of(1), x -> 0, version::get);
        expect(ConcurrentModificationException.class, () -> p.forEach(x -> version.incrementAndGet()));
    }
    private static void batches() {
        AtomicInteger pulled = new AtomicInteger(), completed = new AtomicInteger(), maxAhead = new AtomicInteger();
        Iterator<Integer> source = new Iterator<>() {
            @Override public boolean hasNext() { return pulled.get() < 1003; }
            @Override public Integer next() {
                int value = pulled.getAndIncrement();
                maxAhead.accumulateAndGet(pulled.get() - completed.get(), Math::max); return value;
            }
        };
        int[] last = new int[7]; Arrays.fill(last, -1);
        var batches = M3Parallel.batches(source, 17, x -> x % 7);
        try (ForkJoinPool pool = new ForkJoinPool(3)) {
            long count = batches.using(pool).forEach(x -> { int k = x % 7; check(x > last[k], "batch order"); last[k] = x; completed.incrementAndGet(); });
            eq(1003L, count); eq(1003, completed.get()); check(maxAhead.get() <= 17, "unbounded prefetch");
            expect(IllegalStateException.class, () -> batches.forEach(x -> { })); check(!pool.isShutdown(), "borrowed pool closed");
        }
    }
    private static void batchFailure() {
        AtomicInteger pulled = new AtomicInteger();
        Iterator<Integer> source = new Iterator<>() {
            @Override public boolean hasNext() { return pulled.get() < 100; }
            @Override public Integer next() { return pulled.getAndIncrement(); }
        };
        try (ForkJoinPool pool = new ForkJoinPool(1)) {
            expect(Marker.class, () -> M3Parallel.batches(source, 7, x -> 0).using(pool).forEach(x -> { throw new Marker(); }));
            eq(7, pulled.get());
        }
    }
    private static void batchEdges() {
        eq(0L, M3Parallel.batches(List.<Integer>of().iterator(), 1, Function.identity()).forEach(x -> { throw new AssertionError(); }));
        expect(IllegalArgumentException.class, () -> M3Parallel.batches(List.of(1).iterator(), 0, Function.identity()));
        AtomicInteger calls = new AtomicInteger();
        expect(CancellationException.class, () -> M3Parallel.batches(List.of(1).iterator(), 1, Function.identity()).cancelWhen(() -> true).forEach(x -> calls.incrementAndGet()));
        eq(0, calls.get());
        AtomicBoolean cancel = new AtomicBoolean();
        try (ForkJoinPool pool = new ForkJoinPool(1)) {
            expect(CancellationException.class, () -> M3Parallel.batches(List.of(1, 2, 3).iterator(), 1, x -> 0).using(pool).cancelWhen(cancel::get).forEach(x -> { calls.incrementAndGet(); cancel.set(true); }));
            eq(1, calls.get());
        }
    }
    private static void batchNested() {
        try (ForkJoinPool pool = new ForkJoinPool(1)) {
            long count = pool.submit(() -> M3Parallel.batches(List.of(1, 2, 3).iterator(), 2, x -> x % 2).using(pool).forEach(x -> { })).join();
            eq(3L, count);
        }
    }
    private static void windowEdges() {
        var p = M3Parallel.by(List.of(0, 1, 2, 3, 4), x -> x % 2);
        eq(List.of(3, 2, 3, 2, 3), p.mapWindows(Integer.MAX_VALUE, Integer.MAX_VALUE, (center, w) -> w.size()));
        eq(List.of(0, 1, 2, 3, 4), p.mapWindows(0, 0, (center, w) -> w.getFirst()));
        p.mapWindows(1, 1, (center, w) -> { expect(UnsupportedOperationException.class, () -> w.clear()); return null; });
        eq(Arrays.asList(null, "a"), M3Parallel.by(Arrays.asList(null, "a"), x -> 0).mapWindows(0, 0, (center, w) -> w.getFirst()));
        expect(IllegalArgumentException.class, () -> p.mapWindows(-1, 0, (a, b) -> a));
        expect(NullPointerException.class, () -> p.mapWindows(1, 1, null));
        expect(Marker.class, () -> p.mapWindows(1, 1, (a, b) -> { throw new Marker(); }));
    }
    private static void joinEdges() {
        var join = M3SortedJoin.by(List.of("a", "A", "b"), Function.identity(), List.of("A", "a", "c"), Function.identity(), String.CASE_INSENSITIVE_ORDER);
        eq(4L, join.count()); List<String> pairs = new ArrayList<>(); join.forEach(4, (a, b) -> pairs.add(a + ":" + b));
        eq(List.of("a:A", "a:a", "A:A", "A:a"), pairs);
        var nullJoin = M3SortedJoin.by(Arrays.asList(null, 1), Function.identity(), Arrays.asList(null, null, 2), Function.identity(), Comparator.nullsFirst(Comparator.<Integer>naturalOrder()));
        eq(2L, nullJoin.count()); nullJoin.forEach(2, (a, b) -> { eq(null, a); eq(null, b); });
    }
    private static void joinDifferential() {
        Random random = new Random(37152);
        for (int trial = 0; trial < 500; trial++) {
            List<Item> left = new ArrayList<>(), right = new ArrayList<>();
            for (int i = 0, n = random.nextInt(60); i < n; i++) left.add(new Item(random.nextInt(12), 0, i));
            for (int i = 0, n = random.nextInt(60); i < n; i++) right.add(new Item(random.nextInt(12), 0, i));
            left.sort(Comparator.comparingInt(Item::key)); right.sort(Comparator.comparingInt(Item::key));
            List<String> expected = new ArrayList<>(), actual = new ArrayList<>();
            for (Item a : left) for (Item b : right) if (a.key() == b.key()) expected.add(a.id() + ":" + b.id());
            var join = M3SortedJoin.by(left, Item::key, right, Item::key, Comparator.naturalOrder());
            eq((long) expected.size(), join.count()); join.forEach(expected.size(), (a, b) -> actual.add(a.id() + ":" + b.id())); eq(expected, actual);
        }
    }
    private static void joinBudget() {
        var join = M3SortedJoin.by(Collections.nCopies(50000, 1), Function.identity(), Collections.nCopies(50000, 1), Function.identity(), Comparator.naturalOrder());
        eq(2500000000L, join.count()); AtomicInteger effects = new AtomicInteger();
        expect(IllegalArgumentException.class, () -> join.forEach(Integer.MAX_VALUE, (a, b) -> effects.incrementAndGet()));
        expect(IllegalArgumentException.class, () -> join.forEach(-1, (a, b) -> effects.incrementAndGet())); eq(0, effects.get());
    }
    private static void joinFailure() {
        expect(IllegalArgumentException.class, () -> M3SortedJoin.by(List.of(2, 1), Function.identity(), List.of(0), Function.identity(), Comparator.naturalOrder()));
        expect(IllegalArgumentException.class, () -> M3SortedJoin.by(List.<Integer>of(), Function.identity(), List.of(2, 1), Function.identity(), Comparator.naturalOrder()));
        AtomicInteger keys = new AtomicInteger();
        var join = M3SortedJoin.by(List.of(1, 1), x -> { keys.incrementAndGet(); return x; }, List.of(1), x -> { keys.incrementAndGet(); return x; }, Comparator.naturalOrder());
        eq(3, keys.get()); expect(Marker.class, () -> join.forEach(2, (a, b) -> { throw new Marker(); }));
        eq(3, keys.get());
    }
    private static void longWindows() {
        Random random = new Random(713917);
        for (int trial = 0; trial < 2000; trial++) {
            int n = random.nextInt(250), width = 1 + random.nextInt(n + 5); long[] values = new long[n];
            for (int i = 0; i < n; i++) values[i] = switch (random.nextInt(6)) { case 0 -> Long.MIN_VALUE; case 1 -> Long.MAX_VALUE; case 2 -> 0L; default -> random.nextLong(); };
            long[] copy = values.clone(), expected = new long[width > n ? 0 : n - width + 1];
            for (int i = 0; i < expected.length; i++) { long max = Long.MIN_VALUE; for (int j = i; j < i + width; j++) max = Math.max(max, values[j]); expected[i] = max; }
            same(expected, M3LongWindows.max(values, width)); same(copy, values);
            if (nativeEnabled) { same(expected, M3LongWindows.max(values, width, M3Parallel.Backend.NATIVE)); same(copy, values); }
        }
        same(new long[0], M3LongWindows.max(new long[]{1}, Integer.MAX_VALUE));
        expect(NullPointerException.class, () -> M3LongWindows.max(null, 1));
        expect(IllegalArgumentException.class, () -> M3LongWindows.max(new long[0], 0));
    }
    private static void claims() {
        AtomicInteger next = new AtomicInteger(Integer.MAX_VALUE - 1);
        eq(Integer.MAX_VALUE - 1, M3Parallel.claim(next, Integer.MAX_VALUE));
        eq(Integer.MAX_VALUE, M3Parallel.claim(next, Integer.MAX_VALUE));
        eq(Integer.MAX_VALUE, M3Parallel.claim(next, Integer.MAX_VALUE));
        eq(Integer.MAX_VALUE, next.get());
        next.set(0); eq(0, M3Parallel.claim(next, 0)); eq(0, next.get());
        AtomicInteger counter = new AtomicInteger(); boolean[] seen = new boolean[10000];
        try (ForkJoinPool pool = new ForkJoinPool(4)) {
            pool.submit(() -> java.util.stream.IntStream.range(0, 4).parallel().forEach(worker -> {
                for (;;) { int task = M3Parallel.claim(counter, seen.length); if (task == seen.length) break;
                    check(!seen[task], "duplicate claim"); seen[task] = true; }
            })).join();
        }
        for (boolean hit : seen) check(hit, "missing claim"); eq(seen.length, counter.get());
    }
    private static void reuse() {
        var p = M3Parallel.by(List.of(1, 2, 3), x -> 0);
        eq(List.of(1, 2, 3), p.mapOrdered(Function.identity())); p.topByKey(2, Comparator.reverseOrder());
        p.reduceByKey(1, () -> 0, x -> x, Integer::sum); p.mapWindows(1, 1, (a, b) -> b.size());
        eq(List.of(1, 2, 3), p.mapOrdered(Function.identity()));
        expect(UnsupportedOperationException.class, () -> p.firstByKey().clear());
        expect(UnsupportedOperationException.class, () -> p.reduceByKey(1, () -> 0, x -> x, Integer::sum).clear());
    }
    private static void nativeEdges() {
        expect(NullPointerException.class, () -> M3PartitionLayout.Native.windowMax(null, 1));
        expect(IllegalArgumentException.class, () -> M3PartitionLayout.Native.windowMax(new long[0], 0));
        expect(IllegalArgumentException.class, () -> M3PartitionLayout.Native.windowMax(new long[]{1}, -1));
        same(new long[0], M3PartitionLayout.Native.windowMax(new long[0], Integer.MAX_VALUE));
        same(new long[]{Long.MAX_VALUE, Long.MAX_VALUE}, M3PartitionLayout.Native.windowMax(new long[]{Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE}, 2));
    }
    private static void nativePlan() {
        var p = M3Parallel.byKeys(List.of(0, 1, 2, 3), x -> List.of(x % 2), M3Parallel.Backend.NATIVE);
        eq(Map.of(0, 2, 1, 4), p.reduceByKey(1, () -> 0, x -> x, Integer::sum));
        eq(Map.of(0, List.of(2), 1, List.of(3)), p.topByKey(1, Comparator.reverseOrder()));
        eq(List.of(2, 2, 2, 2), p.mapWindows(1, 1, (a, b) -> b.size()));
    }
    private static void await(CountDownLatch latch) {
        try { check(latch.await(5, TimeUnit.SECONDS), "latch timeout"); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
    }
    private static void pause() {
        try { Thread.sleep(30); } catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
    }
    private static void test(String name, Runnable body) { body.run(); cases++; System.out.println("PASS " + name); }
    private static void eq(Object expected, Object actual) { if (!Objects.equals(expected, actual)) throw new AssertionError("expected " + expected + " but got " + actual); }
    private static void same(long[] expected, long[] actual) { if (!Arrays.equals(expected, actual)) throw new AssertionError("long array mismatch"); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable error) { if (type.isInstance(error)) return; throw new AssertionError("expected " + type, error); }
        throw new AssertionError("expected " + type);
    }
}
