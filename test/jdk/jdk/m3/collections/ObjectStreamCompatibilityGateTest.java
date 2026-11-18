// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia contributors
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Binary/source compatibility oracle for operators already shipped on master.
 * Neither the existing indexed mapper convention nor Optional mapWhile may change.
 */
final class ObjectStreamCompatibilityGateTest {
    @Test void preservesBothIndexedArgumentOrders() {
        ObjectStream<String> plan = ObjectStream.snapshot(List.of("a", "bbb"));
        List<String> expected = List.of("0:a", "1:bbb");
        assertEquals(expected, plan.mapIndexed((value, index) -> index + ":" + value)
                .toBoundedList(2));
        assertEquals(expected, plan.mapIndexedOrdinalFirst((index, value) -> index + ":" + value)
                .toBoundedList(2));
        assertEquals(expected, plan.mapIndexed((value, index) -> index + ":" + value)
                .toBoundedList(2));
    }

    @Test void optionalAndNullableMapWhileRemainDisjoint() {
        ObjectStream<Integer> plan = ObjectStream.snapshot(List.of(1, 2, 3, 4));
        assertEquals(List.of(10, 20), plan.mapWhile(
                number -> number < 3 ? Optional.of(number * 10) : Optional.empty())
                .toBoundedList(2));
        assertEquals(List.of(10, 20), plan.mapWhileNullable(
                number -> number < 3 ? number * 10 : null).toBoundedList(2));
        assertEquals(List.of(10, 20), plan.mapWhile(
                number -> number < 3 ? Optional.of(number * 10) : Optional.empty())
                .toBoundedList(2));
    }

    @Test void independentAdjacentKernelsRemainPresent() {
        ObjectStream<Integer> plan = ObjectStream.snapshot(Arrays.asList(1, 2, 4));
        assertEquals(List.of(1, 2), plan.zipWithNext((a, b) -> b - a).toBoundedList(2));
        assertEquals(List.of(1, 2), plan.pairwise((a, b) -> b - a).toBoundedList(2));
        assertEquals(List.of(1, 2, 4), plan.distinctAdjacent().toBoundedList(3));
    }

    @Test void existingFoldByKeyDoesNotTurnIntoGroupMapReduce() {
        ObjectStream<Integer> plan = ObjectStream.snapshot(List.of(1, 2, 3, 4));
        assertEquals(plan.foldByKey(i -> i % 2, 2, () -> 0, Integer::sum),
                plan.groupMapReduce(i -> i % 2, 2, i -> i, Integer::sum));
    }

    @Test void optionalMappingAndEarlyCloseStayDistinct() {
        AtomicInteger optionalClosed = new AtomicInteger();
        ObjectStream<Integer> optional = ObjectStream.once(Stream.of(1, 2, 3)
                .onClose(optionalClosed::incrementAndGet));
        assertFalse(optional.replayable());
        assertEquals(List.of(1), optional.mapWhile(
                x -> x < 2 ? Optional.of(x) : Optional.empty()).toBoundedList(1));
        assertEquals(1, optionalClosed.get());
        AtomicInteger nullableClosed = new AtomicInteger();
        ObjectStream<Integer> nullable = ObjectStream.once(Stream.of(1, 2, 3)
                .onClose(nullableClosed::incrementAndGet));
        assertTrue(nullable.mapWhileNullable(x -> x < 2 ? x : null)
                .toBoundedList(1).equals(List.of(1)));
        assertEquals(1, nullableClosed.get());
    }
}
