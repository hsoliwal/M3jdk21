/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Standalone deterministic contract harness; no hidden reliance on JVM assertion flags. */
public final class M3ObjectStreamContract {
    private static int assertions;
    private M3ObjectStreamContract() { }
    public static void main(String[] args) {
        sourcesAndReplay(); statelessAndStateful(); windowsAndSnapshots(); aggregates(); keyedParallelism();
        System.out.println("M3ObjectStreamContract PASS assertions=" + assertions);
    }
    private static void sourcesAndReplay() {
        List<Integer> source = new ArrayList<>(Arrays.asList(1, 2, null, 3));
        var snapshot = M3ObjectStream.snapshot(source);
        source.add(4);
        eq(Arrays.asList(1, 2, null, 3), snapshot.toBoundedList(4));
        eq(4L, snapshot.count());
        eq(Arrays.asList(1, 2, null, 3, 4), M3ObjectStream.live(source).toBoundedList(5));
        var once = M3ObjectStream.once(Stream.of(1, 2, 3));
        eq(Arrays.asList(1, 2, 3), once.mark(3).reprocess().toBoundedList(3));
        throwsType(IllegalStateException.class, once::count);
        throwsType(IllegalStateException.class, () -> M3ObjectStream.once(Stream.of(1,2)).toBoundedList(1));
    }
    private static void statelessAndStateful() {
        eq(List.of(4, 6, 8), M3ObjectStream.snapshot(List.of(1,2,3,4))
                .filter(x -> x > 1).map(x -> x * 2).toBoundedList(3));
        eq(List.of(1, 10, 2, 20), M3ObjectStream.snapshot(List.of(1,2))
                .flatMapIterable(x -> List.of(x, x * 10)).toBoundedList(4));
        eq(List.of(1, 2, 3), M3ObjectStream.snapshot(Arrays.asList(1,1,2,3,3)).distinct(3).toBoundedList(3));
        throwsType(IllegalStateException.class, () -> M3ObjectStream.snapshot(List.of(1,2,3)).distinct(2).count());
        eq(List.of(2, 3), M3ObjectStream.snapshot(List.of(1,2,3,4)).dropWhile(x -> x < 2).takeWhile(x -> x < 4).toBoundedList(2));
        AtomicInteger seen = new AtomicInteger();
        var plan = M3ObjectStream.snapshot(List.of(1,2,3)).peek(x -> seen.incrementAndGet());
        eq(3L, plan.count()); eq(3, seen.get()); eq(3L, plan.count()); eq(6, seen.get());
        throwsType(IllegalArgumentException.class, () -> plan.limit(-1));
    }
    private static void windowsAndSnapshots() {
        eq(Arrays.asList(Arrays.asList(1, null), Arrays.asList(null, 3)),
                M3ObjectStream.snapshot(Arrays.asList(1, null, 3)).window(2).toBoundedList(2));
        eq(Arrays.asList(Arrays.asList(1, null), Arrays.asList(3)),
                M3ObjectStream.snapshot(Arrays.asList(1, null, 3)).batch(2).toBoundedList(2));
        eq(List.of(List.of(1,2)), M3ObjectStream.snapshot(List.of(1,2,3)).batch(2, M3ObjectStream.TailPolicy.DROP).toBoundedList(2));
        eq(List.of(1,3,6), M3ObjectStream.snapshot(List.of(1,2,3)).scan(() -> 0, Integer::sum).toBoundedList(3));
        eq(List.of(), M3ObjectStream.<Integer>snapshot(List.of()).scan(() -> 99, Integer::sum).toBoundedList(0));
        var sorted = M3ObjectStream.snapshot(List.of("b1", "a1", "a2", "b2"))
                .sorted(Comparator.comparing(s -> s.substring(0,1)), 4).toBoundedList(4);
        eq(List.of("a1", "a2", "b1", "b2"), sorted);
        var indexed = M3ObjectStream.snapshot(Arrays.asList(1, null, 3, 4)).indexedFiltered(x -> x == null || x % 2 == 0, 4);
        eq(Arrays.asList(null, 4), new ArrayList<>(indexed));
        eq(Arrays.asList(1, null, 3, 4), M3ObjectStream.snapshot(Arrays.asList(1, null, 3, 4)).mark(4).reprocess().toBoundedList(4));
        throwsType(IllegalStateException.class, () -> M3ObjectStream.snapshot(List.of(1,2)).toBoundedList(1));
        throwsType(IllegalArgumentException.class, () -> M3ObjectStream.snapshot(List.of(1)).window(0));
    }
    private static void aggregates() {
        var source = M3ObjectStream.snapshot(Arrays.asList("a", null, "b", "a", null));
        eq(5L, source.count());
        eq("anullbanull", source.reduce("", (a,b) -> a + b));
        eq(5, source.collect(() -> new int[1], (a,v) -> a[0]++, a -> a[0]));
        var counts = source.countByKey(x -> x, 3);
        eq(Arrays.asList("a", null, "b"), new ArrayList<>(counts.keySet()));
        eq(2L, counts.get("a")); eq(2L, counts.get(null));
        throwsType(IllegalStateException.class, () -> source.countByKey(x -> x, 2));
        int[] traversals = {0};
        var instrumented = M3ObjectStream.snapshot(List.of(1,2,3)).peek(x -> traversals[0]++);
        eq("6/3", instrumented.tee(Collectors.summingInt(Integer::intValue), Collectors.counting(), (sum,count) -> sum + "/" + count));
        eq(3, traversals[0]);
        throwsType(UnsupportedOperationException.class, () -> source.virtualCollection().clear());
    }
    private static void keyedParallelism() {
        List<Integer> items = new ArrayList<>(); for (int i=0;i<500;i++) items.add(i);
        List<String> values = M3ObjectStream.snapshot(items).parallelByKey(i -> i % 4)
                .mapOrdered(i -> { try { Thread.sleep((i % 7) * 1L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); } return i + ":" + (i*i); }, 8, 12, 500);
        eq(500, values.size());
        for (int i=0;i<500;i++) eq(i + ":" + (i*i), values.get(i));
        throwsType(IllegalArgumentException.class, () -> M3ObjectStream.snapshot(items).parallelByKey(i -> i).mapOrdered(i -> i, 0, 10, 500));
        throwsType(IllegalStateException.class, () -> M3ObjectStream.snapshot(items).parallelByKey(i -> i).mapOrdered(i -> i, 4, 5, 10));
        throwsType(IllegalStateException.class, () -> M3ObjectStream.snapshot(items).parallelByKey(i -> i).mapOrdered(i -> { if (i==23) throw new IllegalStateException("expected"); return i; }, 4, 8, 500));
    }
    private static void eq(Object expected, Object actual) {
        assertions++;
        if (!Objects.equals(expected, actual)) throw new AssertionError("expected=" + expected + ", actual=" + actual);
    }
    private static void throwsType(Class<? extends Throwable> expected, Runnable runnable) {
        assertions++;
        try { runnable.run(); } catch (Throwable t) { if (expected.isInstance(t)) return; throw new AssertionError("expected " + expected + " got " + t, t); }
        throw new AssertionError("expected exception " + expected.getName());
    }
}
