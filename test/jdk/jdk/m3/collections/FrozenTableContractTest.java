/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.internal.m3.collections.FrozenLongTable;
import jdk.internal.m3.collections.FrozenTable;
import org.junit.jupiter.api.Test;

/** Portable contract and independent JDK map models; no provider runtime dependency. */
final class FrozenTableContractTest {
    private record Key(int value) {
        @Override public int hashCode() { return 7; }
    }

    private static <K, V> Map<K, V> entries(List<Map.Entry<K, V>> items) {
        return new AbstractMap<>() {
            @Override public Set<Entry<K, V>> entrySet() {
                return new AbstractSet<>() {
                    @Override public int size() { return items.size(); }
                    @Override public Iterator<Entry<K, V>> iterator() { return items.iterator(); }
                };
            }
        };
    }

    private static Map<String, Integer> row(String first, int a, String second, int b) {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put(first, a);
        result.put(second, b);
        return result;
    }

    @Test void emptyOwnersHaveValidQueriesOffsetsAndRefusals() {
        FrozenTable<String, String, Integer> object = new FrozenTable<>();
        assertEquals(0, object.size());
        assertTrue(object.rowKeySet().isEmpty());
        assertTrue(object.columnKeySet().isEmpty());
        assertTrue(object.rowMap().isEmpty());
        assertTrue(object.row("missing").isEmpty());
        assertTrue(object.column("missing").isEmpty());
        assertNull(object.get(null, null));
        assertFalse(object.contains("x", "y"));
        assertThrows(NullPointerException.class, () -> object.row(null));
        assertThrows(NullPointerException.class, () -> object.column(null));
        assertThrows(UnsupportedOperationException.class, () -> object.put(null, null, null));
        assertThrows(UnsupportedOperationException.class, () -> object.remove(null, null));
        FrozenLongTable primitive = new FrozenLongTable();
        assertEquals(0, primitive.size());
        assertEquals(0, primitive.rowCount());
        assertEquals(0, primitive.rowOffsetAt(0));
        assertFalse(primitive.contains(0, 0));
        assertEquals(Long.MIN_VALUE, primitive.getOrDefault(0, 0, Long.MIN_VALUE));
        assertThrows(IndexOutOfBoundsException.class, () -> primitive.rowAt(0));
        assertThrows(IndexOutOfBoundsException.class, () -> primitive.rowOffsetAt(1));
        assertThrows(IndexOutOfBoundsException.class, () -> primitive.columnAt(0));
        assertThrows(IndexOutOfBoundsException.class, () -> primitive.valueAt(0));
        assertThrows(UnsupportedOperationException.class, () -> primitive.put(0, 0, 0));
        assertThrows(UnsupportedOperationException.class, () -> primitive.remove(0, 0));
    }

    @Test void denseRowsUseGlobalColumnEncounterOrder() {
        Map<String, Map<String, Integer>> source = new LinkedHashMap<>();
        source.put("r0", row("a", 1, "z", 2));
        Map<String, Integer> second = row("z", 3, "a", 4);
        second.put("x", 5);
        source.put("r1", second);
        FrozenTable<String, String, Integer> actual = FrozenTable.copyOf(source);
        assertEquals(List.of("r0", "r1"), new ArrayList<>(actual.rowKeySet()));
        assertEquals(List.of("a", "z", "x"), new ArrayList<>(actual.columnKeySet()));
        assertEquals(List.of("a", "z", "x"), new ArrayList<>(actual.row("r1").keySet()));
        assertEquals(List.of(4, 3, 5), new ArrayList<>(actual.row("r1").values()));
        assertEquals(List.of("r0", "r1"), new ArrayList<>(actual.column("a").keySet()));
        assertEquals(source, actual.rowMap());
    }

    @Test void sparseRowsRetainTheirOwnEncounterOrder() {
        Map<String, Map<String, Integer>> source = new LinkedHashMap<>();
        source.put("r0", row("a", 1, "z", 2));
        source.put("r1", row("z", 3, "a", 4));
        source.put("r2", Map.of("x", 5));
        source.put("r3", Map.of("y", 6));
        source.put("empty", Map.of());
        FrozenTable<String, String, Integer> actual = FrozenTable.copyOf(source);
        assertEquals(6, actual.size());
        assertEquals(List.of("z", "a"), new ArrayList<>(actual.row("r1").keySet()));
        assertEquals(List.of("a", "z", "x", "y"), new ArrayList<>(actual.columnKeySet()));
        assertFalse(actual.rowKeySet().contains("empty"));
        assertNull(actual.rowMap().get("empty"));
        assertEquals(Map.of("r0", 1, "r1", 4), actual.column("a"));
    }

    @Test void copiesOwnStructureAndQueriesDistinguishAbsence() {
        Map<String, Integer> cells = row("a", 10, "b", 20);
        Map<String, Map<String, Integer>> source = new LinkedHashMap<>();
        source.put("r", cells);
        FrozenTable<String, String, Integer> actual = FrozenTable.copyOf(source);
        cells.clear();
        source.clear();
        assertEquals(2, actual.size());
        assertEquals(10, actual.get("r", "a"));
        assertTrue(actual.contains("r", "b"));
        assertNull(actual.get(null, "a"));
        assertNull(actual.get("r", null));
        assertNull(actual.get(123, new Object()));
        assertFalse(actual.contains("r", "absent"));
        assertNull(actual.rowMap().get(null));
        assertFalse(actual.rowKeySet().contains(null));
        assertFalse(actual.columnKeySet().contains(null));
        assertEquals(actual.rowMap().hashCode(), Map.of("r", Map.of("a", 10, "b", 20)).hashCode());
    }

    @Test void allMapAndSetViewsRejectNoOpAndNestedMutations() {
        for (FrozenTable<String, String, Integer> actual : List.of(
                new FrozenTable<String, String, Integer>(),
                FrozenTable.copyOf(Map.of("r", Map.of("c", 1))))) {
            for (Map<String, Integer> view : List.of(actual.row("r"), actual.row("absent"),
                    actual.column("c"), actual.column("absent"))) {
                AtomicInteger callbacks = new AtomicInteger();
                assertThrows(UnsupportedOperationException.class, () -> view.remove("absent"));
                assertThrows(UnsupportedOperationException.class, () -> view.putAll(Map.of()));
                assertThrows(UnsupportedOperationException.class, view::clear);
                assertThrows(UnsupportedOperationException.class, () -> view.computeIfAbsent("absent", key -> {
                    callbacks.incrementAndGet(); return 2;
                }));
                assertThrows(UnsupportedOperationException.class, () -> view.replaceAll((key, value) -> {
                    callbacks.incrementAndGet(); return value;
                }));
                assertEquals(0, callbacks.get());
                assertThrows(UnsupportedOperationException.class, () -> view.entrySet().removeIf(entry -> false));
                assertThrows(UnsupportedOperationException.class, () -> view.keySet().removeAll(Set.of()));
                assertThrows(UnsupportedOperationException.class, () -> view.values().remove(99));
                Iterator<Map.Entry<String, Integer>> iterator = view.entrySet().iterator();
                assertThrows(UnsupportedOperationException.class, iterator::remove);
                while (iterator.hasNext()) {
                    Map.Entry<String, Integer> entry = iterator.next();
                    assertThrows(UnsupportedOperationException.class, () -> entry.setValue(entry.getValue()));
                }
                assertThrows(NoSuchElementException.class, iterator::next);
            }
            for (Set<String> set : List.of(actual.rowKeySet(), actual.columnKeySet())) {
                assertThrows(UnsupportedOperationException.class, () -> set.addAll(List.of()));
                assertThrows(UnsupportedOperationException.class, () -> set.remove("absent"));
                assertThrows(UnsupportedOperationException.class, set::clear);
            }
            assertThrows(UnsupportedOperationException.class, () -> actual.rowMap().putAll(Map.of()));
            assertThrows(UnsupportedOperationException.class, () -> actual.rowMap().remove("absent"));
            for (Map.Entry<String, Map<String, Integer>> entry : actual.rowMap().entrySet()) {
                assertThrows(UnsupportedOperationException.class, () -> entry.setValue(Map.of()));
                assertThrows(UnsupportedOperationException.class, () -> entry.getValue().clear());
            }
        }
    }

    @Test void nullIngressRejectsBeforePublishingAnyOwner() {
        assertThrows(NullPointerException.class, () -> FrozenTable.copyOf(null));
        Map<String, Map<String, Integer>> nullRow = new LinkedHashMap<>();
        nullRow.put(null, Map.of());
        assertThrows(NullPointerException.class, () -> FrozenTable.copyOf(nullRow));
        Map<String, Map<String, Integer>> nullRun = new LinkedHashMap<>();
        nullRun.put("r", null);
        assertThrows(NullPointerException.class, () -> FrozenTable.copyOf(nullRun));
        Map<String, Integer> nullColumn = new LinkedHashMap<>();
        nullColumn.put(null, 1);
        assertThrows(NullPointerException.class, () -> FrozenTable.copyOf(Map.of("r", nullColumn)));
        Map<String, Integer> nullValue = new LinkedHashMap<>();
        nullValue.put("c", null);
        assertThrows(NullPointerException.class, () -> FrozenTable.copyOf(Map.of("r", nullValue)));
    }

    @Test void equalRowsMergeDisjointCellsButDuplicateCoordinatesReject() {
        String first = new String("r");
        String later = new String("r");
        Map<String, Map<String, Integer>> source = entries(List.of(
                Map.entry(first, Map.of("a", 1)), Map.entry(later, Map.of("b", 2))));
        FrozenTable<String, String, Integer> actual = FrozenTable.copyOf(source);
        assertEquals(2, actual.size());
        assertSame(first, actual.rowKeySet().iterator().next());
        assertEquals(Map.of("a", 1, "b", 2), actual.row("r"));
        Map<String, Map<String, Integer>> duplicates = entries(List.of(
                Map.entry(first, Map.of("a", 1)), Map.entry(later, Map.of(new String("a"), 1))));
        assertThrows(IllegalArgumentException.class, () -> FrozenTable.copyOf(duplicates));
        Map<String, Integer> duplicateColumns = entries(List.of(Map.entry("c", 1), Map.entry(new String("c"), 2)));
        assertThrows(IllegalArgumentException.class, () -> FrozenTable.copyOf(Map.of("r", duplicateColumns)));
    }

    @Test void collisionHeavyObjectCellsMatchIndependentJdkMaps() {
        Random random = new Random(0x5441424c454f424aL);
        for (int trial = 0; trial < 120; trial++) {
            Map<Key, Map<Key, Integer>> source = new LinkedHashMap<>();
            for (int cell = 0; cell < 180; cell++) {
                source.computeIfAbsent(new Key(random.nextInt(25)), key -> new LinkedHashMap<>())
                        .put(new Key(random.nextInt(31)), random.nextInt());
            }
            FrozenTable<Key, Key, Integer> actual = FrozenTable.copyOf(source);
            assertEquals(source, actual.rowMap());
            assertEquals(source.values().stream().mapToInt(Map::size).sum(), actual.size());
            for (int row = -1; row <= 25; row++) {
                for (int column = -1; column <= 31; column++) {
                    Integer expected = source.getOrDefault(new Key(row), Map.of()).get(new Key(column));
                    assertEquals(expected, actual.get(new Key(row), new Key(column)));
                    assertEquals(expected != null, actual.contains(new Key(row), new Key(column)));
                }
            }
        }
    }

    private static void verifyPrimitive(long[] rows, long[] columns, long[] values) {
        Map<Long, Map<Long, Long>> expected = new LinkedHashMap<>();
        for (int i = 0; i < rows.length; i++) {
            assertNull(expected.computeIfAbsent(rows[i], key -> new TreeMap<>()).put(columns[i], values[i]));
        }
        FrozenLongTable actual = FrozenLongTable.copyOf(rows, columns, values);
        assertEquals(rows.length, actual.size());
        assertEquals(expected.size(), actual.rowCount());
        int rowOrdinal = 0;
        int cellOrdinal = 0;
        for (Map.Entry<Long, Map<Long, Long>> row : expected.entrySet()) {
            assertEquals(row.getKey().longValue(), actual.rowAt(rowOrdinal));
            assertEquals(cellOrdinal, actual.rowOffsetAt(rowOrdinal++));
            for (Map.Entry<Long, Long> cell : row.getValue().entrySet()) {
                assertEquals(cell.getKey().longValue(), actual.columnAt(cellOrdinal));
                assertEquals(cell.getValue().longValue(), actual.valueAt(cellOrdinal++));
                assertTrue(actual.contains(row.getKey(), cell.getKey()));
                assertEquals(cell.getValue().longValue(), actual.getOrDefault(row.getKey(), cell.getKey(), 11));
            }
        }
        assertEquals(actual.size(), actual.rowOffsetAt(actual.rowCount()));
        for (int invalid : new int[] {Integer.MIN_VALUE, -1, Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> actual.rowAt(invalid));
            assertThrows(IndexOutOfBoundsException.class, () -> actual.rowOffsetAt(invalid));
            assertThrows(IndexOutOfBoundsException.class, () -> actual.columnAt(invalid));
            assertThrows(IndexOutOfBoundsException.class, () -> actual.valueAt(invalid));
        }
        assertThrows(IndexOutOfBoundsException.class, () -> actual.rowAt(actual.rowCount()));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.rowOffsetAt(actual.rowCount() + 1));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.columnAt(actual.size()));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.valueAt(actual.size()));
        Arrays.fill(rows, 123);
        Arrays.fill(columns, 456);
        Arrays.fill(values, 789);
        assertEquals(cellOrdinal, actual.size());
        for (Map.Entry<Long, Map<Long, Long>> row : expected.entrySet()) {
            for (Map.Entry<Long, Long> cell : row.getValue().entrySet()) {
                assertEquals(cell.getValue().longValue(), actual.getOrDefault(row.getKey(), cell.getKey(), 0));
            }
        }
    }

    @Test void fullWidthPrimitiveCellsUseStableRowsAndSignedColumnOrder() {
        verifyPrimitive(new long[] {9, Long.MIN_VALUE, 9, 0, Long.MAX_VALUE, 9, 0},
                new long[] {Long.MAX_VALUE, 0, Long.MIN_VALUE, 4, -1, 0, Long.MIN_VALUE},
                new long[] {0, Long.MAX_VALUE, Long.MIN_VALUE, 17, -1, 22, 33});
        long[] shared = {Long.MAX_VALUE, 0, Long.MIN_VALUE};
        FrozenLongTable aliases = FrozenLongTable.copyOf(shared, shared, shared);
        Arrays.fill(shared, 4);
        assertEquals(Long.MIN_VALUE, aliases.getOrDefault(Long.MIN_VALUE, Long.MIN_VALUE, 99));
        assertEquals(99, aliases.getOrDefault(4, 4, 99));
    }

    @Test void primitiveInputShapeNullsAndDuplicateCellsReject() {
        assertThrows(NullPointerException.class, () -> FrozenLongTable.copyOf(null, new long[0], new long[0]));
        assertThrows(NullPointerException.class, () -> FrozenLongTable.copyOf(new long[0], null, new long[0]));
        assertThrows(NullPointerException.class, () -> FrozenLongTable.copyOf(new long[0], new long[0], null));
        assertThrows(IllegalArgumentException.class, () -> FrozenLongTable.copyOf(new long[1], new long[0], new long[1]));
        assertThrows(IllegalArgumentException.class, () -> FrozenLongTable.copyOf(new long[1], new long[1], new long[0]));
        for (long value : new long[] {1, 2}) {
            assertThrows(IllegalArgumentException.class, () -> FrozenLongTable.copyOf(
                    new long[] {8, 7, 8}, new long[] {2, 9, 2}, new long[] {1, 4, value}));
        }
    }

    @Test void randomizedPrimitiveCsrMatchesOrderedMapModel() {
        Random random = new Random(0x5441424c454c4f4eL);
        for (int trial = 0; trial < 160; trial++) {
            Map<Long, Map<Long, Long>> source = new LinkedHashMap<>();
            long[] pool = {Long.MIN_VALUE, Long.MAX_VALUE, 0, -1, 85078, 177926, random.nextLong()};
            for (int cell = 0; cell < trial * 3; cell++) {
                long row = pool[random.nextInt(pool.length)];
                source.computeIfAbsent(row, key -> new TreeMap<>()).put(random.nextLong(), random.nextLong());
            }
            List<long[]> cells = new ArrayList<>();
            source.forEach((row, columns) -> columns.forEach((column, value) -> cells.add(new long[] {row, column, value})));
            Collections.shuffle(cells, random);
            long[] rows = new long[cells.size()];
            long[] columns = new long[cells.size()];
            long[] values = new long[cells.size()];
            for (int i = 0; i < cells.size(); i++) {
                rows[i] = cells.get(i)[0]; columns[i] = cells.get(i)[1]; values[i] = cells.get(i)[2];
            }
            verifyPrimitive(rows, columns, values);
        }
    }

    @Test void throwingSourceTraversalAndKeyCallbacksRemainObservable() {
        RuntimeException failure = new IllegalStateException("retained table callback failure");
        Map<String, Map<String, Integer>> broken = new AbstractMap<>() {
            @Override public Set<Entry<String, Map<String, Integer>>> entrySet() { throw failure; }
        };
        assertSame(failure, assertThrows(IllegalStateException.class, () -> FrozenTable.copyOf(broken)));
        Object key = new Object() {
            @Override public int hashCode() { throw failure; }
        };
        Map<Object, Map<String, Integer>> source = entries(List.of(Map.entry(key, Map.of("c", 1))));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> FrozenTable.copyOf(source)));
    }
}
