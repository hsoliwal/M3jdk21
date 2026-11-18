// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia contributors
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Contract and differential oracles for cross-language custom-loop convergence. */
final class ObjectStreamLoopContractTest {
    @Test void mapWhileStopsAtFirstMissingProjectionAndClosesSource() {
        AtomicInteger mapped = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        ObjectStream<Integer> source = ObjectStream.once(Stream.of(2, 3, -1, 9)
                .onClose(closed::incrementAndGet));
        ObjectStream<Integer> mappedStream = source.mapWhileNullable(v -> {
            mapped.incrementAndGet();
            return v < 0 ? null : 2 * v;
        });
        assertFalse(mappedStream.replayable());
        assertEquals(List.of(4, 6), mappedStream.toBoundedList(2));
        assertEquals(3, mapped.get());
        assertEquals(1, closed.get());
        assertEquals(List.of(), ObjectStream.snapshot(List.<Integer>of())
                .mapWhileNullable(v -> 2 * v).toBoundedList(0));
        assertThrows(IllegalStateException.class, () -> mappedStream.toBoundedList(3));
    }

    @Test void stepByRetainsIndexZeroAndProtectsStride() {
        ObjectStream<Integer> source = ObjectStream.snapshot(Arrays.asList(null, 1, 2, 3, 4, 5));
        assertEquals(Arrays.asList(null, 2, 4), source.stepBy(2).toBoundedList(3));
        assertEquals(Arrays.asList(null, 1, 2, 3, 4, 5), source.stepBy(1).toBoundedList(6));
        assertEquals(Arrays.asList((Integer) null), source.stepBy(100).toBoundedList(1));
        assertThrows(IllegalArgumentException.class, () -> source.stepBy(0));
        Random random = new Random(20261118L);
        for (int n = 0; n < 250; n++) {
            int size = random.nextInt(100);
            int stride = 1 + random.nextInt(25);
            List<Integer> input = new ArrayList<>();
            for (int i = 0; i < size; i++) input.add(random.nextInt(40));
            List<Integer> expected = new ArrayList<>();
            for (int i = 0; i < size; i += stride) expected.add(input.get(i));
            ObjectStream<Integer> plan = ObjectStream.snapshot(input).stepBy(stride);
            assertEquals(expected, plan.toBoundedList(expected.size()));
            assertEquals(expected, plan.toBoundedList(expected.size()));
        }
    }

    private record Keyed(int score, String name) { }

    @Test void mergeSortedPreservesDuplicatesAndLeftSideTies() {
        Comparator<Keyed> byScore = Comparator.comparingInt(Keyed::score);
        List<Keyed> left = List.of(new Keyed(1, "L1"), new Keyed(2, "L2"), new Keyed(2, "L3"));
        List<Keyed> right = List.of(new Keyed(2, "R2"), new Keyed(3, "R3"));
        ObjectStream<Keyed> merged = ObjectStream.snapshot(left).mergeSorted(
                ObjectStream.snapshot(right), byScore);
        assertTrue(merged.replayable());
        assertEquals(List.of(left.get(0), left.get(1), left.get(2), right.get(0), right.get(1)),
                merged.toBoundedList(5));
        assertEquals(merged.toBoundedList(5), merged.toBoundedList(5));
        AtomicInteger closeL = new AtomicInteger();
        AtomicInteger closeR = new AtomicInteger();
        ObjectStream<Integer> l = ObjectStream.once(Stream.of(1, 2)
                .onClose(closeL::incrementAndGet));
        ObjectStream<Integer> r = ObjectStream.once(Stream.of(0, 3)
                .onClose(closeR::incrementAndGet));
        assertFalse(l.mergeSorted(r, Integer::compareTo).replayable());
        // Fresh sources because a one-shot plan must never be reopened.
        assertEquals(List.of(3), l.mergeSorted(r, Integer::compareTo)
                .takeLast(1).toBoundedList(1));
        assertEquals(1, closeL.get());
        assertEquals(1, closeR.get());
    }

    @Test void mergeSortedRandomizedVsStableFullSort() {
        Random random = new Random(122347L);
        for (int round = 0; round < 350; round++) {
            List<Integer> a = new ArrayList<>(), b = new ArrayList<>();
            int al = random.nextInt(75), bl = random.nextInt(75);
            for (int i = 0; i < al; i++) a.add(random.nextInt(30));
            for (int i = 0; i < bl; i++) b.add(random.nextInt(30));
            Collections.sort(a);
            Collections.sort(b);
            List<Integer> expected = new ArrayList<>(a);
            expected.addAll(b);
            Collections.sort(expected);
            List<Integer> observed = ObjectStream.snapshot(a)
                    .mergeSorted(ObjectStream.snapshot(b), Comparator.naturalOrder())
                    .toBoundedList(al + bl);
            assertEquals(expected, observed);
        }
    }

    @Test void tailOperatorsPreserveNullAndCloseTheirInput() {
        ObjectStream<Integer> src = ObjectStream.snapshot(Arrays.asList(null, 1, 2, null));
        assertEquals(Arrays.asList(2, null), src.takeLast(2).toBoundedList(2));
        assertEquals(Arrays.asList(null, 1), src.dropLast(2).toBoundedList(2));
        assertEquals(Arrays.asList(null, 1, 2, null), src.dropLast(0).toBoundedList(4));
        assertEquals(List.of(), src.takeLast(0).toBoundedList(0));
        assertEquals(List.of(), src.dropLast(99).toBoundedList(0));
        assertThrows(IllegalArgumentException.class, () -> src.takeLast(-1));
        assertThrows(IllegalArgumentException.class, () -> src.dropLast(65537));
        AtomicInteger closed = new AtomicInteger();
        ObjectStream<Integer> once = ObjectStream.once(Stream.of(1, 2, 3)
                .onClose(closed::incrementAndGet));
        assertEquals(List.of(2, 3), once.takeLast(2).toBoundedList(2));
        assertEquals(1, closed.get());
    }

    @Test void tailOperatorsRandomizedVsSliceOracle() {
        Random random = new Random(455719L);
        for (int round = 0; round < 350; round++) {
            int size = random.nextInt(70), k = random.nextInt(85);
            List<Integer> input = new ArrayList<>();
            for (int i = 0; i < size; i++) input.add(random.nextInt(15));
            int split = Math.max(0, size - k);
            List<Integer> expectedTail = input.subList(split, size);
            List<Integer> expectedDrop = input.subList(0, split);
            ObjectStream<Integer> plan = ObjectStream.snapshot(input);
            assertEquals(expectedTail, plan.takeLast(k).toBoundedList(k));
            assertEquals(expectedDrop, plan.dropLast(k).toBoundedList(expectedDrop.size()));
        }
    }

    @Test void indexedRunningFoldEmitsSeedAndStartsFromIndexZero() {
        ObjectStream<String> source = ObjectStream.snapshot(List.of("a", "b"));
        ObjectStream<String> folded = source.runningFoldIndexed(
                () -> "s", (idx, state, value) -> state + value + idx);
        assertEquals(List.of("s", "sa0", "sa0b1"), folded.toBoundedList(3));
        assertEquals(List.of("s", "sa0", "sa0b1"), folded.toBoundedList(3));
        assertEquals(List.of("seed"), ObjectStream.snapshot(List.<String>of())
                .runningFoldIndexed(() -> "seed", (idx, acc, v) -> acc + v).toBoundedList(1));
        assertEquals(Arrays.asList((String) null, "x"), ObjectStream.snapshot(List.of("x"))
                .runningFoldIndexed(() -> (String) null, (idx, acc, v) -> v).toBoundedList(2));
    }

    @Test void coalesceAdjacentUsesAccumulatedStateAndWorksWithNull() {
        ObjectStream<String> plan = ObjectStream.snapshot(
                List.of("aa", "ab", "bb", "bc", "aa"))
                .coalesceAdjacent((a, b) -> a.charAt(0) == b.charAt(0),
                        (a, b) -> a + "+" + b);
        assertEquals(List.of("aa+ab", "bb+bc", "aa"), plan.toBoundedList(3));
        assertEquals(List.of("aa+ab", "bb+bc", "aa"), plan.toBoundedList(3));
        assertEquals(Arrays.asList(null, "x", null),
                ObjectStream.snapshot(Arrays.asList(null, null, "x", null))
                        .coalesceAdjacent(Objects::equals, (a, b) -> a).toBoundedList(3));
        assertEquals(List.of(), ObjectStream.snapshot(List.<String>of())
                .coalesceAdjacent(Objects::equals, (a, b) -> a).toBoundedList(0));
    }

    @Test void partitionMapProducesTwoImmutableTypedBranchesWithoutEitherNodes() {
        AtomicInteger predCalls = new AtomicInteger();
        AtomicInteger leftCalls = new AtomicInteger();
        AtomicInteger rightCalls = new AtomicInteger();
        ObjectStream<Integer> source = ObjectStream.snapshot(Arrays.asList(1, -2, null, 3));
        ObjectStream.Partition<String, Integer> split = source.partitionMap(v -> {
            predCalls.incrementAndGet(); return v != null && v > 0;
        }, v -> { leftCalls.incrementAndGet(); return "pos" + v; },
                v -> { rightCalls.incrementAndGet(); return v == null ? null : -v; }, 4);
        assertEquals(List.of("pos1", "pos3"), split.left());
        assertEquals(Arrays.asList(2, null), split.right());
        assertEquals(4, predCalls.get());
        assertEquals(2, leftCalls.get());
        assertEquals(2, rightCalls.get());
        assertThrows(UnsupportedOperationException.class, () -> split.left().add("bad"));
        assertThrows(IllegalStateException.class, () -> source.partitionMap(
                v -> v != null, v -> v, v -> v, 3));
        assertThrows(IllegalArgumentException.class, () -> source.partitionMap(
                v -> true, v -> v, v -> v, -1));
    }
}
