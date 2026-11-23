/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Java21 host-toolchain regression for M3 pairwise and Optional-based mapWhile.
 * Uses an independent JDK List loop oracle, not java.util.stream.Streams.
 */
public final class ObjectStreamHostJdk21SelfTest {
    private static long checks;
    private ObjectStreamHostJdk21SelfTest() { }

    public static void main(String[] args) {
        fixedAndReplayContracts();
        oneShotClosingAndErrors();
        originalNoSplitCharacteristics();
        randomizedReference();
        System.out.println("PASS ObjectStreamHostJdk21SelfTest checks=" + checks);
    }

    private static void fixedAndReplayContracts() {
        ObjectStream<Integer> source = ObjectStream.snapshot(List.of(1, 2, 3, 4));
        ObjectStream<Integer> pairs = source.pairwise(Integer::sum);
        same(List.of(3, 5, 7), pairs.toBoundedList(3), "overlapping pairs");
        same(List.of(3, 5, 7), pairs.toBoundedList(3), "replayed overlapping pairs");
        check(pairs.replayable(), "snapshot pairs remain replayable");
        same(List.of(3, 5, 7), source.zipWithNext(Integer::sum)
                .toBoundedList(3), "existing zipWithNext parity");
        same(List.of(), ObjectStream.snapshot(List.<Integer>of())
                .pairwise(Integer::sum).toBoundedList(0), "empty pairwise");
        same(List.of(), ObjectStream.snapshot(List.of(9))
                .pairwise(Integer::sum).toBoundedList(0), "singleton pairwise");
        same(Arrays.asList("null/x", "x/null"),
                ObjectStream.snapshot(Arrays.asList(null, "x", null))
                        .pairwise((a, b) -> a + "/" + b)
                        .toBoundedList(2), "pairwise null-element preservation");
        same(Arrays.asList((Integer) null),
                ObjectStream.snapshot(List.of(1, 2))
                        .pairwise((a, b) -> (Integer) null).toBoundedList(1),
                "pairwise null-result preservation");

        ObjectStream<Integer> mapped = source.mapWhile(
                v -> v <= 2 ? Optional.of(v * 10) : Optional.empty());
        same(List.of(10, 20), mapped.toBoundedList(2), "mapWhile empty stops");
        same(List.of(10, 20), mapped.toBoundedList(2), "mapWhile replay");
        same(List.of(), ObjectStream.snapshot(List.<Integer>of())
                .mapWhile(Optional::of).toBoundedList(0), "empty mapWhile");
        same(List.of(), ObjectStream.snapshot(List.of(1, 2))
                .<Integer>mapWhile(v -> Optional.empty()).toBoundedList(0),
                "first-empty immediate stop");
        same(List.of(3), ObjectStream.snapshot(List.of(3))
                .mapWhile(Optional::of).toBoundedList(1), "mapWhile singleton");
        check(mapped.replayable(), "mapped snapshot remains replayable");
    }

    private static void oneShotClosingAndErrors() {
        AtomicInteger pairClosed = new AtomicInteger();
        ObjectStream<Integer> once = ObjectStream.once(
                Stream.of(1, 2, 3).onClose(pairClosed::incrementAndGet));
        check(!once.pairwise(Integer::sum).replayable(), "one-shot flag retained");
        same(3, once.pairwise(Integer::sum).findFirst().orElseThrow(),
                "one-shot first adjacent pair");
        same(1, pairClosed.get(), "early pairwise close");
        expect(IllegalStateException.class, () ->
                once.pairwise(Integer::sum).toBoundedList(2));

        AtomicInteger visited = new AtomicInteger(), closed = new AtomicInteger();
        ObjectStream<Integer> mapping = ObjectStream.once(
                Stream.of(1, 2, 3, 4).onClose(closed::incrementAndGet));
        same(List.of(2, 4), mapping.mapWhile(v -> {
            visited.incrementAndGet();
            return v == 3 ? Optional.empty() : Optional.of(v * 2);
        }).toBoundedList(2), "stop at empty");
        same(3, visited.get(), "terminate without consuming later elements");
        same(1, closed.get(), "close truncated one-shot source");

        expect(NullPointerException.class, () ->
                ObjectStream.snapshot(List.of(1)).pairwise(null));
        expect(NullPointerException.class, () ->
                ObjectStream.snapshot(List.of(1)).mapWhile(null));
        expect(NullPointerException.class, () ->
                ObjectStream.snapshot(List.of(1))
                        .<Integer>mapWhile(v -> null).toBoundedList(1));
        RuntimeException signal = new IllegalStateException("mapper identity");
        try {
            ObjectStream.snapshot(List.of(1, 2)).<Integer>mapWhile(v -> {
                throw signal;
            }).toBoundedList(1);
            throw new AssertionError("mapper failure swallowed");
        } catch (RuntimeException actual) {
            check(actual == signal, "original mapper exception identity");
        }
        AtomicInteger failureClosed = new AtomicInteger();
        ObjectStream<Integer> failing = ObjectStream.once(
                Stream.of(1, 2).onClose(failureClosed::incrementAndGet));
        expect(IllegalArgumentException.class, () ->
                failing.<Integer>mapWhile(v -> {
                    throw new IllegalArgumentException("failure");
                }).toBoundedList(1));
        same(1, failureClosed.get(), "failed mapWhile source closed");
    }

    private static void originalNoSplitCharacteristics() {
        ObjectStream<Integer> plan = ObjectStream.snapshot(
                java.util.stream.IntStream.range(0, 80).boxed().toList());
        try (Stream<Integer> s = plan.pairwise(Integer::sum).stream()) {
            Spliterator<Integer> split = s.spliterator();
            check(split.hasCharacteristics(Spliterator.ORDERED), "ordered pairs");
            check(!split.hasCharacteristics(Spliterator.NONNULL),
                    "pairwise nullable mapped values");
            check(split.trySplit() == null, "pairwise original no-split contract");
        }
        try (Stream<Integer> s = plan.mapWhile(Optional::of).stream()) {
            Spliterator<Integer> split = s.spliterator();
            check(split.hasCharacteristics(Spliterator.ORDERED), "ordered mapWhile");
            check(split.hasCharacteristics(Spliterator.NONNULL),
                    "Optional mapWhile never emits null");
            check(split.trySplit() == null, "mapWhile original no-split contract");
        }
        // The older M3 zipWithNext kernel must not silently inherit the new
        // unsplittable pairwise contract or change its published behavior.
        try (Stream<Integer> s = plan.zipWithNext(Integer::sum).stream()) {
            Spliterator<Integer> split = s.spliterator();
            check(split.trySplit() != null, "zipWithNext historical split contract");
        }

        java.util.Iterator<Integer> items = List.of(1, 2, 3).iterator();
        Spliterator<Integer> unsequenced = Spliterators.spliteratorUnknownSize(
                items, 0);
        try (Stream<Integer> s = ObjectStream.once(
                StreamSupport.stream(unsequenced, false))
                .pairwise(Integer::sum).stream()) {
            Spliterator<Integer> split = s.spliterator();
            check(!split.hasCharacteristics(Spliterator.ORDERED),
                    "unordered pairwise must not claim ORDERED");
            check(split.trySplit() == null, "unordered pairwise no-split");
        }
        java.util.Iterator<Integer> mappedItems = List.of(1, 2, 3).iterator();
        try (Stream<Integer> s = ObjectStream.once(StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(mappedItems, 0), false))
                .mapWhile(Optional::of).stream()) {
            Spliterator<Integer> split = s.spliterator();
            check(!split.hasCharacteristics(Spliterator.ORDERED),
                    "unordered mapWhile must not claim ORDERED");
            check(split.hasCharacteristics(Spliterator.NONNULL),
                    "unordered mapWhile still NONNULL");
            check(split.trySplit() == null, "unordered mapWhile no-split");
        }
    }

    private static void randomizedReference() {
        Random random = new Random(0x4d3353545245414dL);
        for (int trial = 0; trial < 1_250; trial++) {
            int n = random.nextInt(130);
            List<Integer> source = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                source.add(random.nextInt(71) - 35);
            }
            List<Integer> wantPairs = new ArrayList<>();
            for (int i = 1; i < n; i++) {
                wantPairs.add(source.get(i) - source.get(i - 1));
            }
            ObjectStream<Integer> plan = ObjectStream.snapshot(source);
            List<Integer> gotPairs = plan.pairwise((a, b) -> b - a)
                    .toBoundedList(wantPairs.size());
            same(wantPairs, gotPairs, "random pairwise oracle");
            same(wantPairs, plan.zipWithNext((a, b) -> b - a)
                    .toBoundedList(wantPairs.size()), "random zipWithNext parity");

            int boundary = random.nextInt(n + 1);
            AtomicInteger visited = new AtomicInteger();
            List<Integer> expectedMapped = new ArrayList<>();
            for (int i = 0; i < boundary; i++) expectedMapped.add(source.get(i) * 3);
            List<Integer> mapped = plan.mapWhile(value -> {
                int read = visited.getAndIncrement();
                return read == boundary ? Optional.empty() : Optional.of(value * 3);
            }).toBoundedList(expectedMapped.size());
            same(expectedMapped, mapped, "random mapWhile source order");
            same(Math.min(n, boundary + 1), visited.get(), "termination consumption");
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable actual) {
            if (type.isInstance(actual)) return;
            throw new AssertionError("unexpected failure " + actual, actual);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void same(Object expected, Object actual, String description) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(description + " expected=" + expected
                    + " actual=" + actual + " check=" + checks);
        }
    }

    private static void check(boolean pass, String description) {
        checks++;
        if (!pass) throw new AssertionError(description + " check=" + checks);
    }
}
