/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** Differential and physical-lane proofs for the dense table over fixed row and column keys. */
final class ArrayTableContractTest {
    /** A key whose hash keeps only a few bits, so probe runs collide and wrap. */
    private record Crowded(int id, int bits) {
        @Override public int hashCode() { return id & bits; }
    }

    /** Independent oracle: a grid of values and two key lists searched linearly. */
    private static final class GridModel {
        final List<Object> rows;
        final List<Object> columns;
        final Object[][] grid;

        GridModel(List<Object> rows, List<Object> columns) {
            this.rows = rows;
            this.columns = columns;
            this.grid = new Object[rows.size()][columns.size()];
        }

        Object get(Object row, Object column) {
            int r = rows.indexOf(row);
            int c = columns.indexOf(column);
            return r < 0 || c < 0 ? null : grid[r][c];
        }

        Object set(int r, int c, Object value) {
            Object old = grid[r][c];
            grid[r][c] = value;
            return old;
        }
    }

    @Test
    void randomOperationsMatchTheModelAndTheLanesStayConsistent() throws Exception {
        runAgainstModel(keys(1, 0, -1), keys(1, 100, -1), 0x51ED270BL, 20_000);
        runAgainstModel(keys(7, 0, -1), keys(5, 100, -1), 0x9E3779B9L, 60_000);
        runAgainstModel(keys(64, 0, 3), keys(65, 100, 7), 0x2545F491L, 120_000);
        runAgainstModel(keys(200, 0, 15), keys(3, 100, 0), 0x7F4A7C15L, 60_000);
    }

    /** Keys {@code from .. from + count - 1}; with {@code bits >= 0} they are crowded into a few hash values. */
    private static List<Object> keys(int count, int from, int bits) {
        List<Object> keys = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            keys.add(bits < 0 ? (Object) ("k" + (from + i)) : new Crowded(from + i, bits));
        }
        return keys;
    }

    private static void runAgainstModel(List<Object> rows, List<Object> columns, long seed, int steps) throws Exception {
        ArrayTable<Object, Object, Object> table = new ArrayTable<>(rows, columns);
        GridModel model = new GridModel(rows, columns);
        SplittableRandom random = new SplittableRandom(seed);
        for (int step = 0; step < steps; step++) {
            int r = random.nextInt(rows.size());
            int c = random.nextInt(columns.size());
            Object value = random.nextInt(4) == 0 ? null : Integer.valueOf(random.nextInt(1000));
            switch (random.nextInt(8)) {
                case 0, 1 -> assertSame(model.set(r, c, value), table.put(rows.get(r), columns.get(c), value));
                case 2 -> assertSame(model.set(r, c, value), table.set(r, c, value));
                case 3 -> assertSame(model.set(r, c, null), table.erase(rows.get(r), columns.get(c)));
                case 4 -> assertSame(model.grid[r][c], table.at(r, c));
                case 5 -> assertNull(table.get(unknown(random), columns.get(c)));
                default -> assertSame(model.get(rows.get(r), columns.get(c)), table.get(rows.get(r), columns.get(c)));
            }
            if (step % 211 == 0) {
                assertSameState(table, model);
            }
        }
        assertSameState(table, model);
    }

    private static Object unknown(SplittableRandom random) {
        return random.nextBoolean() ? (Object) ("absent" + random.nextInt(50)) : new Crowded(5000 + random.nextInt(50), 3);
    }

    private static void assertSameState(ArrayTable<Object, Object, Object> table, GridModel model) throws Exception {
        assertEquals(model.rows, table.rowKeyList());
        assertEquals(model.columns, table.columnKeyList());
        assertEquals(model.rows.size() * model.columns.size(), table.size());
        Object[] values = (Object[]) lane(table, "valueLane");
        long[] states = (long[]) lane(table, "stateMaskWords");
        int live = 0;
        for (int r = 0; r < model.rows.size(); r++) {
            for (int c = 0; c < model.columns.size(); c++) {
                int cell = r * model.columns.size() + c;
                assertSame(model.grid[r][c], values[cell], "the value lane is the grid, row after row");
                assertSame(model.grid[r][c], table.get(model.rows.get(r), model.columns.get(c)));
                boolean present = (states[cell >>> 6] >>> cell & 1L) != 0;
                assertEquals(model.grid[r][c] != null, present, "the state bit follows the value");
                live += present ? 1 : 0;
            }
        }
        assertEquals(live, (int) lane(table, "size"), "the live count is the number of values");
        assertEquals(values.length, table.size());
    }

    private static Object lane(ArrayTable<?, ?, ?> table, String name) throws Exception {
        Field field = ArrayTable.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(table);
    }

    @Test
    void everyPairOfKeysIsACellFromTheStart() {
        ArrayTable<String, Integer, String> table = new ArrayTable<>(List.of("a", "b"), List.of(1, 2, 3));
        assertEquals(6, table.size());
        assertTrue(table.contains("a", 3));
        assertNull(table.get("a", 3), "a cell without a value reads as null");
        assertNull(table.put("a", 3, "x"));
        assertEquals("x", table.put("a", 3, null), "null is a value that can be written");
        assertTrue(table.contains("a", 3), "the cell stays");
        assertEquals(6, table.size());
        assertNull(table.at(0, 2));
    }

    @Test
    void keysOutsideTheTableAreNeverFoundAndCannotBeWritten() {
        ArrayTable<String, Integer, String> table = new ArrayTable<>(List.of("a", "b"), List.of(1, 2, 3));
        table.put("b", 2, "v");
        for (Object row : new Object[] {null, "c", 1}) {
            assertNull(table.get(row, 2));
            assertNull(table.erase(row, 2));
            assertFalse(table.contains(row, 2));
        }
        for (Object column : new Object[] {null, 4, "a"}) {
            assertNull(table.get("b", column));
            assertNull(table.erase("b", column));
            assertFalse(table.contains("b", column));
        }
        assertEquals("v", table.get("b", 2));
        IllegalArgumentException row = assertThrows(IllegalArgumentException.class, () -> table.put("c", 2, "x"));
        assertEquals("Row c not in [a, b]", row.getMessage());
        IllegalArgumentException column = assertThrows(IllegalArgumentException.class, () -> table.put("a", 9, "x"));
        assertEquals("Column 9 not in [1, 2, 3]", column.getMessage());
        assertThrows(NullPointerException.class, () -> table.put(null, 2, "x"));
        assertThrows(NullPointerException.class, () -> table.put("a", null, "x"));
        assertEquals("v", table.erase("b", 2));
        assertNull(table.erase("b", 2));
    }

    @Test
    void indexesOutsideTheTableAreRefused() {
        ArrayTable<String, Integer, String> table = new ArrayTable<>(List.of("a", "b"), List.of(1, 2, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> table.at(2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> table.at(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> table.at(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> table.set(0, -1, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> table.set(2, 2, "x"));
        assertNull(table.at(1, 2));
    }

    @Test
    void theKeyListsAreFixed() {
        List<String> rows = new ArrayList<>(List.of("a", "b"));
        ArrayTable<String, Integer, String> table = new ArrayTable<>(rows, List.of(1, 2));
        rows.add("c");
        assertEquals(List.of("a", "b"), table.rowKeyList(), "the keys are copied at construction");
        assertEquals(List.of(1, 2), table.columnKeyList());
        assertThrows(UnsupportedOperationException.class, () -> table.rowKeyList().add("z"));
        assertThrows(UnsupportedOperationException.class, () -> table.columnKeyList().remove(0));
        assertSame(table.rowKeyList(), table.rowKeyList(), "no list is made per call");
    }

    @Test
    void constructionRefusesBadKeyLists() {
        assertThrows(IllegalArgumentException.class, () -> new ArrayTable<>(List.of("a", "a"), List.of(1)));
        assertThrows(IllegalArgumentException.class, () -> new ArrayTable<>(List.of("a"), List.of(1, 2, 1)));
        assertThrows(IllegalArgumentException.class, () -> new ArrayTable<>(List.of("a"), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ArrayTable<>(List.of(), List.of(1)));
        assertThrows(NullPointerException.class, () -> new ArrayTable<>(Arrays.asList("a", null), List.of(1)));
        assertThrows(NullPointerException.class, () -> new ArrayTable<>(null, List.of(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new ArrayTable<>(List.of(new Crowded(1, 0), new Crowded(1, 0)), List.of(1)));
    }

    @Test
    void aTableWithoutKeysHasNoCell() {
        for (ArrayTable<String, Integer, String> table : List.of(new ArrayTable<String, Integer, String>(),
                new ArrayTable<String, Integer, String>(List.of(), List.of()))) {
            assertEquals(0, table.size());
            assertEquals(List.of(), table.rowKeyList());
            assertEquals(List.of(), table.columnKeyList());
            assertNull(table.get("a", 1));
            assertNull(table.erase("a", 1));
            assertFalse(table.contains("a", 1));
            assertThrows(IllegalArgumentException.class, () -> table.put("a", 1, "x"));
            assertThrows(IndexOutOfBoundsException.class, () -> table.at(0, 0));
        }
    }

    @Test
    void theDescriptorNamesTheLanes() {
        assertEquals("ArrayTable|ARRAY_TABLE|OBJECT|PRESENT|Object[] rowKeyLane;Object[] columnKeyLane;Object[] valueLane",
                ArrayTable.scaffoldDescriptor());
    }
}
