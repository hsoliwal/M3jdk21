/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Dense long table indexed by canonical long-key lanes with geometric stride growth
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm --add-opens=jdk.m3.collections/jdk.internal.m3.collections=ALL-UNNAMED LongArrayTableDenseGridTest
 */
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import jdk.internal.m3.collections.LongArrayTable;

public final class LongArrayTableDenseGridTest {
    private record Cell(long row, long column) { }

    private static long assertions;

    public static void main(String[] args) throws Exception {
        fixedUniverseContract();
        dynamicUniverseContract();
        geometricBoundaryAndDenseMask();
        randomizedDifferential();
        System.out.println("LongArrayTableDenseGridTest PASS " + assertions + " assertions");
    }

    private static void fixedUniverseContract() throws Exception {
        long[] rows = {Long.MIN_VALUE, 0, -1, Long.MAX_VALUE};
        long[] columns = {Long.MAX_VALUE, -1, 0};
        LongArrayTable actual = new LongArrayTable(rows, columns);
        Oracle expected = new Oracle(rows, columns, true);
        check(actual.size() == 12, "fixed grid cardinality includes unset cells");
        check(actual.contains(Long.MIN_VALUE, 0), "fixed unset pair is registered");
        check(actual.at(0, 2) == 0L, "fixed unset direct-index reads zero");
        check(actual.getOrDefault(Long.MIN_VALUE, 0, 1234L) == 0L,
                "fixed unset key-pair reads zero instead of missing fallback");
        check(actual.getOrDefault(13L, 0, 1234L) == 1234L, "unknown row fallback");
        check(actual.getOrDefault(0, 13L, 1234L) == 1234L, "unknown column fallback");
        check(actual.set(0, 2, Long.MIN_VALUE) == 0, "fixed indexed set old zero");
        expected.set(0, 2, Long.MIN_VALUE);
        check(actual.put(Long.MIN_VALUE, 0, Long.MAX_VALUE) == Long.MIN_VALUE,
                "fixed key update returns old long extreme");
        expected.put(Long.MIN_VALUE, 0, Long.MAX_VALUE);
        check(actual.put(0, -1, 0) == 0, "fixed explicit zero value");
        expected.put(0, -1, 0);
        expect(IllegalArgumentException.class, () -> actual.put(13, 0, 1),
                "fixed table rejects new row");
        expect(IllegalArgumentException.class, () -> actual.put(0, 13, 1),
                "fixed table rejects new column");
        expect(IndexOutOfBoundsException.class, () -> actual.at(-1, 0),
                "fixed table rejects negative row index");
        expect(IndexOutOfBoundsException.class, () -> actual.at(0, columns.length),
                "fixed table rejects invalid column index");
        expect(IndexOutOfBoundsException.class, () -> actual.set(rows.length, 0, 1),
                "fixed set rejects invalid row index");
        rows[0] = 77;
        columns[0] = 88;
        check(actual.contains(Long.MIN_VALUE, Long.MAX_VALUE), "input key arrays snapshotted");
        long[] rowSnapshot = actual.rowKeysToArray();
        long[] columnSnapshot = actual.columnKeysToArray();
        rowSnapshot[0] = 123;
        columnSnapshot[0] = 123;
        check(actual.contains(Long.MIN_VALUE, Long.MAX_VALUE), "returned key arrays are copies");
        expect(IllegalArgumentException.class, () -> new LongArrayTable(new long[] {1, 1},
                new long[] {0}), "duplicate row rejected");
        expect(IllegalArgumentException.class, () -> new LongArrayTable(new long[] {1},
                new long[] {0, 0}), "duplicate column rejected");
        expect(NullPointerException.class, () -> new LongArrayTable(null,
                new long[] {0}), "null row array rejected");
        expect(NullPointerException.class, () -> new LongArrayTable(new long[] {0},
                null), "null column array rejected");
        LongArrayTable noRows = new LongArrayTable(new long[0], new long[] {1, 2});
        check(noRows.size() == 0, "no-row grid has zero cells");
        check(Arrays.equals(noRows.columnKeysToArray(), new long[] {1, 2}),
                "zero-row grid retains column universe");
        expect(IllegalArgumentException.class, () -> noRows.put(1, 1, 1),
                "fixed zero-row table cannot register new rows");
        verifyStorage(actual, expected);
        check(LongArrayTable.scaffoldDescriptor().equals(
                "M3LongArrayTable|LONG_ARRAY_TABLE|LONG|PRESENT|long[] rowKeyLane;long[] columnKeyLane;long[] valueLane"),
                "historical descriptor unchanged");
    }

    private static void dynamicUniverseContract() throws Exception {
        LongArrayTable actual = new LongArrayTable();
        Oracle expected = new Oracle(new long[0], new long[0], false);
        check(actual.size() == 0, "default dynamic empty");
        check(!actual.contains(0, 0), "no implicit starting keys");
        check(actual.getOrDefault(0, 0, 71) == 71, "default before first registration");
        expect(IndexOutOfBoundsException.class, () -> actual.at(0, 0),
                "empty dynamic index is invalid");
        long[] keys = {Long.MIN_VALUE, 0L, Long.MAX_VALUE, -1L, 1L, 17L};
        for (long row : keys) {
            for (long column : keys) {
                long value = row ^ Long.rotateLeft(column, 13);
                check(actual.put(row, column, value) == expected.put(row, column, value),
                        "dynamic old value");
                check(actual.size() == expected.size(), "dynamic Cartesian size");
            }
        }
        verifyStorage(actual, expected);
        check(actual.put(0, 0, 0) == expected.put(0, 0, 0), "explicit zero overwrite");
        check(actual.put(0, 0, Long.MAX_VALUE) == expected.put(0, 0, Long.MAX_VALUE),
                "overwrite zero key pair");
        check(actual.size() == 36, "replacements do not change universe");
        verifyStorage(actual, expected);
    }

    private static void geometricBoundaryAndDenseMask() throws Exception {
        LongArrayTable actual = new LongArrayTable();
        Oracle expected = new Oracle(new long[0], new long[0], false);
        for (int row = 0; row < 65; row++) {
            for (int column = 0; column < 65; column++) {
                long value = (long) row * 10_000 + column;
                check(actual.put(row, column, value) == expected.put(row, column, value),
                        "stress stride growth old value");
            }
            if ((row & 7) == 0) verifyStorage(actual, expected);
        }
        verifyStorage(actual, expected);
        for (int row = 0; row < 65; row++) {
            for (int column = 0; column < 65; column++) {
                check(actual.at(row, column) == (long) row * 10_000 + column,
                        "direct row-major contents after geometric growth");
            }
        }
    }

    private static void randomizedDifferential() throws Exception {
        for (int seed = 0; seed < 24; seed++) {
            Random random = new Random(0x4D3341525241594CL ^
                    (seed * 0x9E3779B97F4A7C15L));
            LongArrayTable actual = new LongArrayTable();
            Oracle expected = new Oracle(new long[0], new long[0], false);
            for (int step = 0; step < 12_000; step++) {
                long row = key(random);
                long column = key(random);
                long value = random.nextLong();
                int op = random.nextInt(100);
                if (op < 52) {
                    check(actual.put(row, column, value) == expected.put(row, column, value),
                            "random put old value");
                } else if (op < 75) {
                    check(actual.getOrDefault(row, column, value) ==
                            expected.getOrDefault(row, column, value), "random getOrDefault");
                } else if (op < 88) {
                    check(actual.contains(row, column) == expected.contains(row, column),
                            "random contains registered pair");
                } else if (expected.size() > 0) {
                    int r = random.nextInt(expected.rows.size());
                    int c = random.nextInt(expected.columns.size());
                    if (op < 94) {
                        check(actual.set(r, c, value) == expected.set(r, c, value),
                                "random indexed set");
                    } else {
                        check(actual.at(r, c) == expected.at(r, c), "random indexed get");
                    }
                }
                check(actual.size() == expected.size(), "random Cartesian cardinality");
                if ((step & 511) == 0) verifyStorage(actual, expected);
            }
            verifyStorage(actual, expected);
        }
    }

    private static long key(Random random) {
        return switch (random.nextInt(9)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            case 3 -> -1L;
            case 4 -> 1L;
            default -> random.nextInt(66) - 33;
        };
    }

    private static void verifyStorage(LongArrayTable actual, Oracle expected)
            throws Exception {
        long[] actualRows = actual.rowKeysToArray();
        long[] actualColumns = actual.columnKeysToArray();
        check(actualRows.length == expected.rows.size(), "row universe length");
        check(actualColumns.length == expected.columns.size(), "column universe length");
        check(Arrays.equals(actualRows, expected.rowKeys()), "row order preserved");
        check(Arrays.equals(actualColumns, expected.columnKeys()), "column order preserved");
        long[] rowLane = (long[]) field(actual, "rowKeyLane");
        long[] columnLane = (long[]) field(actual, "columnKeyLane");
        long[] valueLane = (long[]) field(actual, "valueLane");
        long[] maskWords = (long[]) field(actual, "stateMaskWords");
        int rowCapacity = (int) field(actual, "rowCapacity");
        int columnCapacity = (int) field(actual, "columnCapacity");
        check(rowLane.length == rowCapacity && columnLane.length == columnCapacity,
                "single owner of row/column key lanes");
        check(valueLane.length == rowCapacity * columnCapacity,
                "one flat dense physical value lane");
        check(maskWords.length == (valueLane.length + 63L) / 64L,
                "one bit per physical cell");
        check(actual.size() == expected.size(), "whole grid cardinality");
        for (int row = 0; row < actualRows.length; row++) {
            for (int column = 0; column < actualColumns.length; column++) {
                long r = actualRows[row];
                long c = actualColumns[column];
                int offset = row * columnCapacity + column;
                long oracleValue = expected.at(row, column);
                check(valueLane[offset] == oracleValue, "row-major value lane");
                check(actual.at(row, column) == oracleValue, "direct indexed view");
                check(actual.getOrDefault(r, c, -17) == oracleValue, "indexed key view");
                check(actual.contains(r, c), "all in-universe pairs logically registered");
                check(((maskWords[offset >>> 6] >>> (offset & 63)) & 1L) ==
                        (expected.isWritten(r, c) ? 1L : 0L), "bitmask follows writes");
            }
        }
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static void expect(Class<? extends Throwable> type, CheckedAction action,
            String label) throws Exception {
        try {
            action.run();
        } catch (Throwable thrown) {
            check(type.isInstance(thrown), label + " type " + thrown.getClass());
            return;
        }
        check(false, label + " no exception");
    }

    private interface CheckedAction { void run() throws Exception; }

    private static void check(boolean condition, String label) {
        assertions++;
        if (!condition) throw new AssertionError(label + " at assertion " + assertions);
    }

    private static final class Oracle {
        private final LinkedHashMap<Long, Integer> rows = new LinkedHashMap<>();
        private final LinkedHashMap<Long, Integer> columns = new LinkedHashMap<>();
        private final Map<Cell, Long> values = new HashMap<>();
        private final boolean fixed;

        Oracle(long[] initialRows, long[] initialColumns, boolean fixed) {
            this.fixed = fixed;
            for (long row : initialRows) rows.put(row, rows.size());
            for (long column : initialColumns) columns.put(column, columns.size());
        }

        long put(long row, long column, long value) {
            if (fixed && !contains(row, column))
                throw new IllegalArgumentException("unknown configured key");
            rows.computeIfAbsent(row, unused -> rows.size());
            columns.computeIfAbsent(column, unused -> columns.size());
            Long previous = values.put(new Cell(row, column), value);
            return previous == null ? 0 : previous;
        }

        long set(int rowIndex, int columnIndex, long value) {
            return put(rowKeys()[rowIndex], columnKeys()[columnIndex], value);
        }

        long at(int rowIndex, int columnIndex) {
            return values.getOrDefault(new Cell(rowKeys()[rowIndex],
                    columnKeys()[columnIndex]), 0L);
        }

        long getOrDefault(long row, long column, long fallback) {
            return contains(row, column) ? values.getOrDefault(new Cell(row, column), 0L)
                    : fallback;
        }

        boolean contains(long row, long column) {
            return rows.containsKey(row) && columns.containsKey(column);
        }

        boolean isWritten(long row, long column) {
            return values.containsKey(new Cell(row, column));
        }

        int size() { return rows.size() * columns.size(); }

        long[] rowKeys() { return asArray(rows.keySet()); }

        long[] columnKeys() { return asArray(columns.keySet()); }

        private static long[] asArray(Set<Long> keys) {
            List<Long> list = new ArrayList<>(keys);
            long[] result = new long[list.size()];
            for (int i = 0; i < result.length; i++) result[i] = list.get(i);
            return result;
        }
    }
}
