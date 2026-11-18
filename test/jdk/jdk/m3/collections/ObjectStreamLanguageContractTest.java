// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia contributors
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Differential and boundary contracts for cross-language replayable stream operators. */
final class ObjectStreamLanguageContractTest {
    @Test void indexedAndAdjacentPreserveOrderAndResetOnReplay() {
        ObjectStream<String> source = ObjectStream.snapshot(Arrays.asList(null, "a", "bbb"));
        List<String> expected = List.of("0:null", "1:a", "2:bbb");
        ObjectStream<String> indexed =
                source.mapIndexedOrdinalFirst((i, x) -> i + ":" + x);
        assertEquals(expected, indexed.toBoundedList(3));
        assertEquals(expected, indexed.toBoundedList(3));
        assertEquals(List.of("null:0", "a:1", "bbb:2"),
                source.mapIndexed((value, ordinal) -> value + ":" + ordinal)
                        .toBoundedList(3));
        assertEquals(List.of(1, 2), source.zipWithNext((a, b) ->
                b.length() - (a == null ? 0 : a.length())).toBoundedList(2));
        assertEquals(List.of(), ObjectStream.snapshot(List.of(1)).zipWithNext((a, b) -> b - a)
                .toBoundedList(0));
    }

    @Test void runningReduceHandlesEmptySingletonAndRepeatedEvaluation() {
        ObjectStream<Integer> scan = ObjectStream.snapshot(List.of(2, 3, 5))
                .runningReduce(Integer::sum);
        assertEquals(List.of(2, 5, 10), scan.toBoundedList(3));
        assertEquals(List.of(2, 5, 10), scan.toBoundedList(3));
        assertEquals(List.of(), ObjectStream.snapshot(List.<Integer>of())
                .runningReduce(Integer::sum).toBoundedList(0));
        assertEquals(List.of(7), ObjectStream.snapshot(List.of(7))
                .runningReduce(Integer::sum).toBoundedList(1));
    }

    @Test void contiguousGroupsAreNotGlobalGroupsAndAreBudgeted() {
        ObjectStream<String> source = ObjectStream.snapshot(
                List.of("aa", "ab", "ba", "bb", "ac"));
        List<List<String>> expected = List.of(List.of("aa", "ab"), List.of("ba", "bb"), List.of("ac"));
        ObjectStream<List<String>> chunks = source.chunkBy(v -> v.charAt(0), 2);
        assertEquals(expected, chunks.toBoundedList(3));
        assertEquals(expected, chunks.toBoundedList(3));
        assertThrows(UnsupportedOperationException.class, () -> chunks.toBoundedList(3).get(0).add("x"));
        assertThrows(IllegalStateException.class, () -> ObjectStream.snapshot(List.of(1, 1, 1))
                .chunkBy(v -> v, 2).toBoundedList(2));
        assertThrows(IllegalArgumentException.class, () -> source.chunkBy(v -> v, 0));
        assertEquals(Arrays.asList(Arrays.asList(null, null), List.of("x")),
                ObjectStream.snapshot(Arrays.asList(null, null, "x"))
                        .chunkBy(v -> v, 2).toBoundedList(2));
    }

    @Test void adjacentDistinctAndInterspersePreserveNulls() {
        ObjectStream<String> source = ObjectStream.snapshot(
                Arrays.asList(null, null, "a", "a", null, null));
        ObjectStream<String> runs = source.distinctUntilChanged(v -> v);
        assertEquals(Arrays.asList(null, "a", null), runs.toBoundedList(3));
        assertEquals(Arrays.asList(null, "a", null), runs.toBoundedList(3));
        assertEquals(Arrays.asList("x", null, "y"),
                ObjectStream.snapshot(List.of("x", "y")).intersperse(null).toBoundedList(3));
        assertEquals(List.of(), ObjectStream.snapshot(List.<String>of())
                .intersperse("|").toBoundedList(0));
        assertEquals(List.of("only"), ObjectStream.snapshot(List.of("only"))
                .intersperse("|").toBoundedList(1));
    }

    @Test void zipTruncatesAtShortestAndClosesBothSources() {
        AtomicInteger firstClosed = new AtomicInteger();
        AtomicInteger secondClosed = new AtomicInteger();
        ObjectStream<Integer> first = ObjectStream.once(Stream.of(2, 4, 8)
                .onClose(firstClosed::incrementAndGet));
        ObjectStream<Integer> second = ObjectStream.once(Stream.of(3, 5)
                .onClose(secondClosed::incrementAndGet));
        ObjectStream<Integer> pairs = first.zip(second, Integer::sum);
        assertFalse(pairs.replayable());
        assertEquals(List.of(5, 9), pairs.toBoundedList(2));
        assertEquals(1, firstClosed.get());
        assertEquals(1, secondClosed.get());
        assertThrows(IllegalStateException.class, () -> pairs.toBoundedList(2));
        assertTrue(ObjectStream.snapshot(List.of(1)).zip(
                ObjectStream.snapshot(List.of(2)), Integer::sum).replayable());
        assertEquals(List.of(), ObjectStream.snapshot(List.<Integer>of()).zip(
                ObjectStream.snapshot(List.of(9)), Integer::sum).toBoundedList(0));
    }

    @Test void shortCircuitClosesOriginAndRandomPairwiseMatchesLoopOracle() {
        AtomicInteger closed = new AtomicInteger();
        ObjectStream<Integer> once = ObjectStream.once(Stream.of(1, 2, 3)
                .onClose(closed::incrementAndGet));
        assertEquals(1, once.zipWithNext((a, b) -> b - a).findFirst().orElseThrow());
        assertEquals(1, closed.get());
        Random random = new Random(20261118L);
        for (int run = 0; run < 100; run++) {
            int length = random.nextInt(80);
            List<Integer> source = new ArrayList<>();
            for (int i = 0; i < length; i++) source.add(random.nextInt(21) - 10);
            List<Integer> expected = new ArrayList<>();
            for (int i = 1; i < length; i++) expected.add(source.get(i) - source.get(i - 1));
            assertEquals(expected, ObjectStream.snapshot(source).zipWithNext((a, b) -> b - a)
                    .toBoundedList(length));
        }
    }
}
