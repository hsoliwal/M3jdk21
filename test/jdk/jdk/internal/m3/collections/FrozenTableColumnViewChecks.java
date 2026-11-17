/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import jdk.internal.m3.collections.FrozenTableColumnSources.Val;

/** Lockstep comparisons of patched column views and lanes against the master reference. */
final class FrozenTableColumnViewChecks {
    private static final List<Function<Map<?, ?>, Collection<?>>> VIEWS =
            List.of(map -> map.entrySet(), map -> map.keySet(), map -> map.values());

    private FrozenTableColumnViewChecks() { }

    /** Returned value or thrown runtime failure of one call. */
    record Outcome(Object value, RuntimeException failure) {
        String shape() {
            return failure == null ? "returned"
                    : failure.getClass().getName() + ": " + failure.getMessage();
        }
    }

    static Outcome outcome(Supplier<?> call) {
        try {
            return new Outcome(call.get(), null);
        } catch (RuntimeException failure) {
            return new Outcome(null, failure);
        }
    }

    /** Same failure class and message on both sides; true when both returned. */
    static boolean sameShape(Outcome actual, Outcome expected) {
        assertEquals(expected.shape(), actual.shape());
        return expected.failure() == null;
    }

    static void sameResult(Outcome actual, Outcome expected) {
        if (sameShape(actual, expected)) { assertSame(expected.value(), actual.value()); }
    }

    static void equalResult(Outcome actual, Outcome expected) {
        if (sameShape(actual, expected)) { assertEquals(expected.value(), actual.value()); }
    }

    /** Entries match by key and value identity; other elements by identity. */
    static void sameElement(Object actual, Object expected) {
        if (expected instanceof Map.Entry<?, ?> entry) {
            Map.Entry<?, ?> other = (Map.Entry<?, ?>) actual;
            assertSame(entry.getKey(), other.getKey());
            assertSame(entry.getValue(), other.getValue());
        } else {
            assertSame(expected, actual);
        }
    }

    static void sameElements(Collection<?> actual, Collection<?> expected) {
        List<?> left = new ArrayList<>(actual);
        List<?> right = new ArrayList<>(expected);
        assertEquals(right.size(), left.size());
        IntStream.range(0, right.size()).forEach(at -> sameElement(left.get(at), right.get(at)));
    }

    /** Full lockstep comparison of one column view pair. */
    static void sameView(Map<?, ?> actual, Map<?, ?> expected, List<Object> rowProbes) {
        assertEquals(expected.size(), actual.size());
        assertEquals(expected.isEmpty(), actual.isEmpty());
        VIEWS.forEach(view -> sameElements(view.apply(actual), view.apply(expected)));
        sameEquality(actual, expected);
        sameLookups(actual, expected, rowProbes);
        sameIteration(actual, expected);
        sameMutationRefusals(actual, expected);
        samePartialRemainder(actual, expected);
        VIEWS.forEach(view -> sameSpliterator(view.apply(actual), view.apply(expected)));
    }

    private static void sameEquality(Map<?, ?> actual, Map<?, ?> expected) {
        assertEquals(expected.toString(), actual.toString());
        assertEquals(expected.hashCode(), actual.hashCode());
        assertTrue(actual.equals(expected));
        assertTrue(expected.equals(actual));
        Map<Object, Object> copy = new LinkedHashMap<>(expected);
        assertEquals(expected.equals(copy), actual.equals(copy));
        assertEquals(copy.equals(expected), copy.equals(actual));
        assertEquals(expected.entrySet().equals(copy.entrySet()),
                actual.entrySet().equals(copy.entrySet()));
        assertEquals(expected.keySet().equals(copy.keySet()),
                actual.keySet().equals(copy.keySet()));
        assertTrue(actual.entrySet().equals(expected.entrySet()));
    }

    private static void sameLookups(Map<?, ?> actual, Map<?, ?> expected, List<Object> probes) {
        probes.forEach(probe -> {
            sameResult(outcome(() -> actual.get(probe)), outcome(() -> expected.get(probe)));
            equalResult(outcome(() -> actual.containsKey(probe)),
                    outcome(() -> expected.containsKey(probe)));
        });
        List<Object> values = new ArrayList<>(expected.values());
        values.addAll(expected.values().stream()
                .map(value -> value instanceof Val val ? new Val(val.id()) : value).toList());
        values.add(new Val(-1));
        values.add(null);
        values.forEach(value -> equalResult(outcome(() -> actual.containsValue(value)),
                outcome(() -> expected.containsValue(value))));
    }

    /** Two interleaved iterators on one view, repeated hasNext and the exhausted next. */
    private static void sameIteration(Map<?, ?> actual, Map<?, ?> expected) {
        Iterator<?> first = actual.entrySet().iterator();
        Iterator<?> second = actual.entrySet().iterator();
        Iterator<?> model = expected.entrySet().iterator();
        while (model.hasNext()) {
            assertTrue(first.hasNext());
            assertTrue(first.hasNext());
            Object entry = model.next();
            sameElement(first.next(), entry);
            sameElement(second.next(), entry);
        }
        assertFalse(first.hasNext());
        assertFalse(second.hasNext());
        assertThrows(NoSuchElementException.class, first::next);
        sameShape(outcome(second::next), outcome(model::next));
    }

    private static void sameMutationRefusals(Map<?, ?> actual, Map<?, ?> expected) {
        Iterator<? extends Map.Entry<?, ?>> left = actual.entrySet().iterator();
        Iterator<? extends Map.Entry<?, ?>> right = expected.entrySet().iterator();
        sameShape(outcome(() -> remove(left)), outcome(() -> remove(right)));
        if (right.hasNext()) {
            Map.Entry<?, ?> mine = left.next();
            Map.Entry<?, ?> theirs = right.next();
            sameShape(outcome(() -> mine.setValue(null)), outcome(() -> theirs.setValue(null)));
        }
    }

    private static Object remove(Iterator<?> iterator) {
        iterator.remove();
        return iterator;
    }

    private static void samePartialRemainder(Map<?, ?> actual, Map<?, ?> expected) {
        Iterator<?> left = actual.entrySet().iterator();
        Iterator<?> right = expected.entrySet().iterator();
        List<Object> mine = new ArrayList<>();
        List<Object> theirs = new ArrayList<>();
        IntStream.range(0, (expected.size() + 1) / 2).forEach(at -> {
            mine.add(left.next());
            theirs.add(right.next());
        });
        left.forEachRemaining(mine::add);
        right.forEachRemaining(theirs::add);
        sameElements(mine, theirs);
    }

    private static void sameSpliterator(Collection<?> actual, Collection<?> expected) {
        Spliterator<?> left = actual.spliterator();
        Spliterator<?> right = expected.spliterator();
        assertEquals(right.characteristics(), left.characteristics());
        assertEquals(right.estimateSize(), left.estimateSize());
        assertEquals(right.getExactSizeIfKnown(), left.getExactSizeIfKnown());
        sameElements(actual.stream().toList(), expected.stream().toList());
        List<Object> mine = new ArrayList<>();
        List<Object> theirs = new ArrayList<>();
        actual.forEach(mine::add);
        expected.forEach(theirs::add);
        sameElements(mine, theirs);
    }

    /** Patched CSR lanes: offsets, slot permutation, old scan order and the frozen mask. */
    static void sameLanes(FrozenTable<?, ?, ?> actual,
            FrozenTableColumnScanReference<?, ?, ?> expected) {
        int[] offsets = (int[]) field(actual, "columnOffsetLane");
        int[] cells = (int[]) field(actual, "columnCellLane");
        int[] ids = (int[]) field(actual, "columnOrdinalLane");
        int[] rank = scanRanks(expected);
        assertEquals(expected.columnKeySet().size() + 1, offsets.length);
        assertEquals(0, offsets[0]);
        assertEquals(actual.size(), offsets[offsets.length - 1]);
        assertArrayEquals(IntStream.range(0, actual.size()).toArray(),
                IntStream.of(cells).sorted().toArray());
        IntStream.range(0, offsets.length - 1)
                .forEach(column -> sameSlice(offsets, cells, ids, rank, column));
        assertArrayEquals(mask(expected), mask(actual));
    }

    private static void sameSlice(int[] offsets, int[] cells, int[] ids, int[] rank, int column) {
        int start = offsets[column];
        int end = offsets[column + 1];
        assertTrue(start < end, "empty column slice");
        IntStream.range(start, end).forEach(at -> assertEquals(column, ids[cells[at]]));
        IntStream.range(start + 1, end)
                .forEach(at -> assertTrue(rank[cells[at - 1]] < rank[cells[at]], "slice order"));
    }

    /** Old scan position of every packed slot: identity when dense, source rank when sparse. */
    private static int[] scanRanks(FrozenTableColumnScanReference<?, ?, ?> expected) {
        int[] order = (int[]) field(expected, "sourceOrderLane");
        boolean dense = (boolean) field(expected, "denseOrder");
        int[] rank = new int[order.length];
        IntStream.range(0, order.length)
                .forEach(index -> rank[dense ? index : order[index]] = index);
        return rank;
    }

    static long[] mask(Object table) { return (long[]) field(table, "stateMaskWords"); }

    static Object field(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(name, failure);
        }
    }
}
