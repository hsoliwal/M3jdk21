// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Row-major fixed-width table backed by one integer lane.
 *
 * <p>A table can optionally bind one canonical {@link MIndexShapeIndex} shape.
 * The shape is metadata shared across all rows; values remain primitive IDs.
 */
public final class MIndexTable<E> {
    private final MIndexSpace<E> space;
    private final int columns;
    private final MIndexShapeIndex shapeIndex;
    private final int shapeId;
    private int[] cells;
    private int rows;

    public MIndexTable(MIndexSpace<E> space, int columns) {
        this(space, columns, 16);
    }

    public MIndexTable(
            MIndexSpace<E> space,
            int columns,
            int expectedRows) {
        this(space, columns, expectedRows, null, -1);
    }

    public MIndexTable(
            MIndexSpace<E> space,
            MIndexShapeIndex shapeIndex,
            int shapeId,
            int expectedRows) {
        this(
                space,
                Objects.requireNonNull(
                        shapeIndex, "shapeIndex")
                        .memberCount(shapeId),
                expectedRows,
                shapeIndex,
                shapeId);
    }

    private MIndexTable(
            MIndexSpace<E> space,
            int columns,
            int expectedRows,
            MIndexShapeIndex shapeIndex,
            int shapeId) {
        this.space = Objects.requireNonNull(space, "space");
        if (columns <= 0 || expectedRows < 0) {
            throw new IllegalArgumentException(
                    "invalid columns/expectedRows");
        }
        this.columns = columns;
        this.shapeIndex = shapeIndex;
        this.shapeId = shapeId;
        cells = new int[Math.multiplyExact(
                columns, expectedRows)];
    }

    public MIndexSpace<E> space() {
        return space;
    }

    public int rows() {
        return rows;
    }

    public int columns() {
        return columns;
    }

    public int size() {
        return Math.multiplyExact(rows, columns);
    }

    public boolean isShaped() {
        return shapeIndex != null;
    }

    public int shapeId() {
        return shapeId;
    }

    public int columnOrdinal(int memberNameId) {
        if (shapeIndex == null) {
            throw new IllegalStateException(
                    "table has no bound shape");
        }
        return shapeIndex.ordinalOfNameId(
                shapeId, memberNameId);
    }

    public void addRowIds(int[] ids) {
        Objects.requireNonNull(ids, "ids");
        if (ids.length != columns) {
            throw new IllegalArgumentException(
                    "row width "
                            + ids.length
                            + " != "
                            + columns);
        }
        for (int id : ids) {
            space.requireId(id);
        }
        int offset = Math.multiplyExact(rows, columns);
        ensureCellCapacity(offset + columns);
        System.arraycopy(
                ids, 0, cells, offset, columns);
        rows++;
    }

    /** Append complete row-major rows. The slice must contain a multiple of columns IDs. */
    public void addRowsIds(int[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        if (length % columns != 0) throw new IllegalArgumentException("incomplete row");
        IdSupport.requireIds(space, source, offset, length);
        int used = size();
        int needed = Math.addExact(used, length);
        ensureCellCapacity(needed);
        System.arraycopy(source, offset, cells, used, length);
        rows += length / columns;
    }

    /** Remove a half-open row range; shape metadata and survivor order remain unchanged. */
    public void removeRows(int fromRow, int toRow) {
        Objects.checkFromToIndex(fromRow, toRow, rows);
        int start = fromRow * columns;
        int end = toRow * columns;
        int used = size();
        System.arraycopy(cells, end, cells, start, used - end);
        Arrays.fill(cells, used - (end - start), used, 0);
        rows -= toRow - fromRow;
    }

    public void clear() {
        Arrays.fill(cells, 0, size(), 0);
        rows = 0;
    }

    public void compact() {
        int used = size();
        if (cells.length != used) cells = Arrays.copyOf(cells, used);
    }

    public E get(int row, int column) {
        return space.value(idAt(row, column));
    }

    public E getByNameId(int row, int memberNameId) {
        int column = requireColumn(memberNameId);
        return get(row, column);
    }

    public int idAt(int row, int column) {
        return cells[cellIndex(row, column)];
    }

    public int idAtByNameId(
            int row,
            int memberNameId) {
        return idAt(row, requireColumn(memberNameId));
    }

    public E set(int row, int column, E value) {
        return space.value(
                setId(row, column, space.id(value)));
    }

    public E setByNameId(
            int row,
            int memberNameId,
            E value) {
        return set(
                row,
                requireColumn(memberNameId),
                value);
    }

    public int setId(int row, int column, int id) {
        space.requireId(id);
        int index = cellIndex(row, column);
        int previous = cells[index];
        cells[index] = id;
        return previous;
    }

    public int setIdByNameId(
            int row,
            int memberNameId,
            int id) {
        return setId(
                row,
                requireColumn(memberNameId),
                id);
    }

    public void forEachRowId(
            int row,
            IntConsumer action) {
        Objects.requireNonNull(action, "action");
        if (row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException(row);
        }
        int offset = row * columns;
        for (int column = 0;
                column < columns;
                column++) {
            action.accept(cells[offset + column]);
        }
    }

    public int[] snapshotIds() {
        return Arrays.copyOf(cells, size());
    }

    public int canonicalId(MIndexCompositeIndex index) {
        Objects.requireNonNull(index, "index");
        return Objects.requireNonNull(index, "index")
                .intern(
                        MIndexCompositeIndex.KIND_TABLE,
                        space,
                        null,
                        identityLane());
    }

    public MIndexFrozenTable<E> freeze(
            MIndexCompositeIndex index) {
        return MIndexFrozenTable.copyOf(
                this,
                Objects.requireNonNull(index, "index"));
    }

    private int requireColumn(int memberNameId) {
        int column = columnOrdinal(memberNameId);
        if (column < 0) {
            throw new IllegalArgumentException(
                    "member name ID not in shape: "
                            + memberNameId);
        }
        return column;
    }

    int[] identityLane() {
        int[] body = snapshotIds();
        int[] shape = shapeIndex == null
                ? new int[0]
                : shapeIndex.copyLane(shapeId);
        int[] identity = new int[
                Math.addExact(Math.addExact(body.length, shape.length), 3)];
        identity[0] = columns;
        identity[1] = shapeIndex == null ? 0 : 1;
        identity[2] = shape.length;
        System.arraycopy(
                shape, 0, identity, 3, shape.length);
        System.arraycopy(
                body,
                0,
                identity,
                3 + shape.length,
                body.length);
        return identity;
    }

    private int cellIndex(int row, int column) {
        if (row < 0
                || row >= rows
                || column < 0
                || column >= columns) {
            throw new IndexOutOfBoundsException(
                    "(" + row + "," + column + ")");
        }
        return Math.addExact(
                Math.multiplyExact(row, columns),
                column);
    }

    private void ensureCellCapacity(int needed) {
        if (needed <= cells.length) {
            return;
        }
        cells = Arrays.copyOf(
                cells,
                IdSupport.grown(cells.length, needed));
    }
}
