// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3PrimitiveSortedTableLiveViewTest {
    @Test
    void rowViewIsEmptyLiveAndMutatesOwner() {
        M3PrimitiveSortedTable table =
                new M3PrimitiveSortedTable(
                        M3PrimitiveKind.INT, M3PrimitiveKind.INT, M3PrimitiveKind.LONG);
        M3PrimitiveSortedTable.RowView row = table.rowViewBits(7);
        assertTrue(row.isEmpty());
        assertFalse(table.containsRowBits(7));
        assertEquals(-1L, row.putBits(2, 20L, -1L));
        assertEquals(-1L, row.putBits(1, 10L, -1L));
        assertEquals(2, row.size());
        assertEquals(10L, row.getOrDefaultBits(1, -1L));
        table.putBits(7, 3, 30L, -1L);
        assertEquals(3, row.size());
        List<Long> keys = new ArrayList<>();
        row.forEach((key, value) -> keys.add(key));
        assertEquals(List.of(1L, 2L, 3L), keys);
        assertEquals(20L, row.removeOrDefaultBits(2, -1L));
        assertFalse(table.containsCellBits(7, 2));
        row.clear();
        assertFalse(table.containsRowBits(7));
        assertTrue(row.isEmpty());
    }

    @Test
    void columnViewIsEmptyLiveAndMutatesOwner() {
        M3PrimitiveSortedTable table =
                new M3PrimitiveSortedTable(
                        M3PrimitiveKind.INT, M3PrimitiveKind.INT, M3PrimitiveKind.INT);
        M3PrimitiveSortedTable.ColumnView column = table.columnViewBits(5);
        assertTrue(column.isEmpty());
        column.putBits(2, 25, -1);
        column.putBits(1, 15, -1);
        assertEquals(2, column.size());
        assertEquals(15L, table.getOrDefaultBits(1, 5, -1));
        table.putBits(3, 5, 35, -1);
        assertEquals(3, column.size());
        assertEquals(25L, column.removeOrDefaultBits(2, -1));
        assertFalse(table.containsCellBits(2, 5));
        column.clear();
        assertFalse(table.containsColumnBits(5));
        assertTrue(column.isEmpty());
    }

    @Test
    void transposeIsCachedLiveAndRoundTripsOwner() {
        M3PrimitiveSortedTable table =
                new M3PrimitiveSortedTable(
                        M3PrimitiveKind.INT, M3PrimitiveKind.LONG, M3PrimitiveKind.INT);
        table.putBits(1, 100L, 7, -1);
        M3PrimitiveSortedTable.TransposedView transposed = table.transpose();
        assertSame(transposed, table.transpose());
        assertSame(table, transposed.transpose());
        assertEquals(M3PrimitiveKind.LONG, transposed.rowKind());
        assertEquals(M3PrimitiveKind.INT, transposed.columnKind());
        assertEquals(7L, transposed.getOrDefaultBits(100L, 1, -1));
        assertEquals(-1L, transposed.putBits(200L, 2, 8, -1));
        assertEquals(8L, table.getOrDefaultBits(2, 200L, -1));
        M3PrimitiveSortedTable.ColumnView transposedRow = transposed.rowViewBits(200L);
        assertEquals(8L, transposedRow.getOrDefaultBits(2, -1));
        assertEquals(8L, transposed.removeOrDefaultBits(200L, 2, -1));
        assertFalse(table.containsCellBits(2, 200L));
    }

    @Test
    void transposeForEachSwapsRowAndColumn() {
        M3PrimitiveSortedTable table =
                new M3PrimitiveSortedTable(
                        M3PrimitiveKind.INT, M3PrimitiveKind.INT, M3PrimitiveKind.INT);
        table.putBits(1, 10, 100, -1);
        table.putBits(2, 20, 200, -1);
        List<String> cells = new ArrayList<>();
        table.transpose().forEach(
                (row, column, value) ->
                        cells.add(row + ":" + column + "=" + value));
        assertEquals(List.of("10:1=100", "20:2=200"), cells);
    }
}
