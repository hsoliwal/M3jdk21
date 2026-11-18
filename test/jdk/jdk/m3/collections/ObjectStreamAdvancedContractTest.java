// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia contributors
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Boundary, replay and randomized oracle tests for the second language wave. */
final class ObjectStreamAdvancedContractTest {
    @Test void mapNotNullAndFilterMapAreLazyAndOnePass() {
        AtomicInteger invoked = new AtomicInteger();
        ObjectStream<String> source = ObjectStream.snapshot(Arrays.asList(null, "12", "bad", "7"));
        ObjectStream<Integer> nonNull = source.mapNotNull(value -> {
            invoked.incrementAndGet();
            if (value == null || value.equals("bad")) return null;
            return Integer.valueOf(value);
        });
        assertEquals(0, invoked.get());
        assertEquals(List.of(12, 7), nonNull.toBoundedList(2));
        assertEquals(4, invoked.get());
        assertEquals(List.of(12, 7), nonNull.toBoundedList(2));
        assertEquals(8, invoked.get());
        assertEquals(List.of(12, 7), source.filterMap(
                value -> value != null && value.matches("[0-9]+")
                        ? Optional.of(Integer.valueOf(value)) : Optional.empty()).toBoundedList(2));
        assertThrows(NullPointerException.class, () -> source.filterMap(v -> null).count());
    }

    @Test void zipLongestPreservesNullVersusMissingAndClosesSources() {
        AtomicInteger leftClosed = new AtomicInteger();
        AtomicInteger rightClosed = new AtomicInteger();
        ObjectStream<String> left = ObjectStream.once(Stream.of("A", (String) null)
                .onClose(leftClosed::incrementAndGet));
        ObjectStream<Integer> right = ObjectStream.once(Stream.of(5)
                .onClose(rightClosed::incrementAndGet));
        ObjectStream<String> zipped = left.zipLongest(right, (hasA, a, hasB, b) ->
                (hasA ? "present:" + a : "missing") + "/" + (hasB ? "present:" + b : "missing"));
        assertFalse(zipped.replayable());
        assertEquals(List.of("present:A/present:5", "present:null/missing"), zipped.toBoundedList(2));
        assertEquals(1, leftClosed.get());
        assertEquals(1, rightClosed.get());
        assertThrows(IllegalStateException.class, () -> zipped.toBoundedList(2));
        assertEquals(List.of(), ObjectStream.snapshot(List.<String>of()).zipLongest(
                ObjectStream.snapshot(List.<Integer>of()), (a, b, c, d) -> "x").toBoundedList(0));
    }

    @Test void interleaveDrainsLongerSideAndResetsPerReplay() {
        ObjectStream<Integer> merged = ObjectStream.snapshot(List.of(1, 3, 5, 7))
                .interleave(ObjectStream.snapshot(List.of(2, 4)));
        assertTrue(merged.replayable());
        assertEquals(List.of(1, 2, 3, 4, 5, 7), merged.toBoundedList(6));
        assertEquals(List.of(1, 2, 3, 4, 5, 7), merged.toBoundedList(6));
        assertEquals(List.of(2, 4), ObjectStream.snapshot(List.<Integer>of())
                .interleave(ObjectStream.snapshot(List.of(2, 4))).toBoundedList(2));
    }

    @Test void partialSlidingWindowsDifferFromFullWindowOnShortInputs() {
        assertEquals(List.of(List.of(1, 2)), ObjectStream.snapshot(List.of(1, 2))
                .windowSlidingPartial(3).toBoundedList(1));
        assertEquals(List.of(List.of(1, 2), List.of(2, 3)), ObjectStream.snapshot(List.of(1, 2, 3))
                .windowSlidingPartial(2).toBoundedList(2));
        assertEquals(List.of(), ObjectStream.snapshot(List.<Integer>of())
                .windowSlidingPartial(5).toBoundedList(0));
        assertEquals(List.of(List.of(1), List.of(2)), ObjectStream.snapshot(List.of(1, 2))
                .windowSlidingPartial(1).toBoundedList(2));
        assertThrows(UnsupportedOperationException.class, () -> ObjectStream.snapshot(List.of(9))
                .windowSlidingPartial(4).toBoundedList(1).get(0).add(10));
        assertThrows(IllegalArgumentException.class, () -> ObjectStream.snapshot(List.of(1))
                .windowSlidingPartial(0));
    }

    @Test void takeUntilInclusiveIncludesBoundaryAndShortCircuits() {
        AtomicInteger seen = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        ObjectStream<Integer> source = ObjectStream.once(Stream.of(1, 2, 3, 4, 5)
                .peek(v -> seen.incrementAndGet()).onClose(closed::incrementAndGet));
        assertEquals(List.of(1, 2, 3), source.takeUntilInclusive(x -> x == 3).toBoundedList(3));
        assertEquals(3, seen.get());
        assertEquals(1, closed.get());
        assertEquals(List.of(1, 2), ObjectStream.snapshot(List.of(1, 2))
                .takeUntilInclusive(x -> x == 9).toBoundedList(2));
    }

    @Test void topKStableTieAndBounds() {
        List<String> source = List.of("aa", "bb", "ccc", "dd", "dddd", "ee");
        ObjectStream<String> top = ObjectStream.snapshot(source).topKBy(3,
                Comparator.comparingInt(String::length));
        assertEquals(List.of("dddd", "ccc", "aa"), top.toBoundedList(3));
        assertEquals(List.of("dddd", "ccc", "aa"), top.toBoundedList(3));
        assertEquals(List.of(), ObjectStream.snapshot(source).topKBy(0, Comparator.naturalOrder())
                .toBoundedList(0));
        assertThrows(IllegalArgumentException.class, () -> ObjectStream.snapshot(source)
                .topKBy(65537, Comparator.naturalOrder()));
        assertEquals(List.of("aa", "bb", "ccc", "dd", "dddd", "ee").size(), source.size());
    }

    @Test void randomizedTopKAgreesWithStableSortOracle() {
        Random random = new Random(20261118L);
        for (int round = 0; round < 500; round++) {
            int length = random.nextInt(120);
            int k = random.nextInt(18);
            List<Integer> input = new ArrayList<>();
            for (int i = 0; i < length; i++) input.add(random.nextInt(20));
            List<Integer> expected = new ArrayList<>(input);
            expected.sort(Comparator.reverseOrder());
            expected = expected.subList(0, Math.min(k, expected.size()));
            assertEquals(expected, ObjectStream.snapshot(input).topKBy(k, Comparator.naturalOrder())
                    .toBoundedList(k));
        }
    }

    @Test void groupMapReducePreservesFirstKeyOrderAndNullState() {
        AtomicInteger maps = new AtomicInteger();
        ObjectStream<String> source = ObjectStream.snapshot(Arrays.asList("aa", "a", null, "bb", "c"));
        Map<String, Integer> grouped = source.groupMapReduce(
                value -> value == null ? null : value.substring(0, 1), 4,
                value -> { maps.incrementAndGet(); return value == null ? null : value.length(); },
                (a, b) -> (a == null ? 0 : a) + (b == null ? 0 : b));
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("a", 3);
        expected.put(null, null);
        expected.put("b", 2);
        expected.put("c", 1);
        assertEquals(expected, grouped);
        assertEquals(5, maps.get());
        assertEquals(Arrays.asList("a", null, "b", "c"), new ArrayList<>(grouped.keySet()));
        assertThrows(UnsupportedOperationException.class, () -> grouped.put("x", 99));
        assertThrows(IllegalStateException.class, () -> source.groupMapReduce(v -> v, 3,
                v -> v, (a, b) -> a));
        assertThrows(IllegalArgumentException.class, () -> source.groupMapReduce(v -> v, -1,
                v -> v, (a, b) -> a));
    }
}
