// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Immutable canonical row-major table backed by one shared primitive lane.
 *
 * <p>The canonical lane is
 * {@code [columns, shapedFlag, shapeLaneLength, shape..., cells...]}.
 * Shape pairs contain global MIndexString name/type IDs. Cell IDs belong to
 * the owning value space, which participates in canonical identity.
 */
public final class MIndexFrozenTable<E> implements MIndexCanonicalCollection {
    private static final int HEADER_WORDS = 3;

    private final MIndexSpace<E> space;
    private final MIndexCompositeIndex index;
    private final int canonicalId;
    private final int columns;
    private final int shapeLength;
    private final int cellOffset;
    private final int cellCount;

    private MIndexFrozenTable(
            MIndexSpace<E> space,
            MIndexCompositeIndex index,
            int canonicalId) {
        this.space = Objects.requireNonNull(space, "space");
        this.index = Objects.requireNonNull(index, "index");
        this.canonicalId = canonicalId;
        if (index.kind(canonicalId) != kind()
                || !index.hasDomains(canonicalId, space, null)) {
            throw new IllegalArgumentException(
                    "canonical ID is not a table in this MIndexSpace");
        }

        int laneLength = index.length(canonicalId);
        if (laneLength < HEADER_WORDS) {
            throw new IllegalArgumentException("canonical table lane is truncated");
        }
        int width = index.valueAt(canonicalId, 0);
        int shapedFlag = index.valueAt(canonicalId, 1);
        int shapeWords = index.valueAt(canonicalId, 2);
        if (width <= 0
                || (shapedFlag != 0 && shapedFlag != 1)
                || shapeWords < 0
                || (shapeWords & 1) != 0
                || HEADER_WORDS + (long) shapeWords > laneLength
                || (shapedFlag == 0 && shapeWords != 0)
                || (shapedFlag == 1 && shapeWords != 2L * width)) {
            throw new IllegalArgumentException("invalid canonical table header");
        }

        columns = width;
        shapeLength = shapeWords;
        cellOffset = HEADER_WORDS + shapeWords;
        cellCount = laneLength - cellOffset;
        if (cellCount % columns != 0) {
            throw new IllegalArgumentException(
                    "cell count is not divisible by columns");
        }
        validateShape(copyShapeLane());
        for (int position = 0; position < cellCount; position++) {
            space.requireId(index.valueAt(canonicalId, cellOffset + position));
        }
    }

    public static <E> MIndexFrozenTable<E> copyOf(
            MIndexTable<E> source,
            MIndexCompositeIndex index) {
        MIndexTable<E> actual = Objects.requireNonNull(source, "source");
        MIndexCompositeIndex registry = Objects.requireNonNull(index, "index");
        int id = registry.intern(
                MIndexCompositeIndex.KIND_TABLE,
                actual.space(),
                null,
                actual.identityLane());
        return new MIndexFrozenTable<>(actual.space(), registry, id);
    }

    public static <E> MIndexFrozenTable<E> ofIds(
            MIndexSpace<E> space,
            MIndexCompositeIndex index,
            int columns,
            int[] cells) {
        return ofIds(space, index, columns, new int[0], cells);
    }

    public static <E> MIndexFrozenTable<E> ofIds(
            MIndexSpace<E> space,
            MIndexCompositeIndex index,
            int columns,
            int[] shapeLane,
            int[] cells) {
        MIndexSpace<E> domain = Objects.requireNonNull(space, "space");
        MIndexCompositeIndex registry = Objects.requireNonNull(index, "index");
        int[] shape = Objects.requireNonNull(shapeLane, "shapeLane").clone();
        int[] values = Objects.requireNonNull(cells, "cells").clone();
        if (columns <= 0
                || values.length % columns != 0
                || (shape.length != 0 && shape.length != 2L * columns)) {
            throw new IllegalArgumentException("invalid table width or shape");
        }
        validateShape(shape);
        for (int valueId : values) {
            domain.requireId(valueId);
        }

        int length = Math.addExact(
                Math.addExact(HEADER_WORDS, shape.length),
                values.length);
        int[] lane = new int[length];
        lane[0] = columns;
        lane[1] = shape.length == 0 ? 0 : 1;
        lane[2] = shape.length;
        System.arraycopy(shape, 0, lane, HEADER_WORDS, shape.length);
        System.arraycopy(
                values, 0, lane, HEADER_WORDS + shape.length, values.length);
        int id = registry.intern(
                MIndexCompositeIndex.KIND_TABLE,
                domain,
                null,
                lane);
        return new MIndexFrozenTable<>(domain, registry, id);
    }

    public MIndexSpace<E> space() {
        return space;
    }

    @Override
    public MIndexCompositeIndex compositeIndex() {
        return index;
    }

    @Override
    public int canonicalId() {
        return canonicalId;
    }

    @Override
    public int kind() {
        return MIndexCompositeIndex.KIND_TABLE;
    }

    @Override
    public int size() {
        return cellCount;
    }

    public boolean isEmpty() {
        return cellCount == 0;
    }

    public int rows() {
        return cellCount / columns;
    }

    public int columns() {
        return columns;
    }

    public boolean isShaped() {
        return shapeLength != 0;
    }

    public int shapeMemberCount() {
        return shapeLength >>> 1;
    }

    public int memberNameId(int column) {
        requireShapedColumn(column);
        return index.valueAt(canonicalId, HEADER_WORDS + (column << 1));
    }

    public int memberTypeId(int column) {
        requireShapedColumn(column);
        return index.valueAt(canonicalId, HEADER_WORDS + (column << 1) + 1);
    }

    public int columnOrdinal(int memberNameId) {
        if (!isShaped()) {
            throw new IllegalStateException("table has no bound shape");
        }
        for (int column = 0; column < columns; column++) {
            if (memberNameId(column) == memberNameId) {
                return column;
            }
        }
        return -1;
    }

    public E get(int row, int column) {
        return space.value(idAt(row, column));
    }

    public E getByNameId(int row, int memberNameId) {
        return get(row, requireColumn(memberNameId));
    }

    public int idAt(int row, int column) {
        return index.valueAt(canonicalId, cellOffset + cellIndex(row, column));
    }

    public int idAtByNameId(int row, int memberNameId) {
        return idAt(row, requireColumn(memberNameId));
    }

    public MIndexFrozenTable<E> withSet(int row, int column, E value) {
        return withSetId(row, column, space.id(value));
    }

    public MIndexFrozenTable<E> withSetId(int row, int column, int id) {
        space.requireId(id);
        int cell = cellIndex(row, column);
        if (index.valueAt(canonicalId, cellOffset + cell) == id) {
            return this;
        }
        int[] values = copyIds();
        values[cell] = id;
        return ofIds(space, index, columns, copyShapeLane(), values);
    }

    public void forEachRowId(int row, IntConsumer action) {
        Objects.requireNonNull(action, "action");
        if (row < 0 || row >= rows()) {
            throw new IndexOutOfBoundsException(row);
        }
        int offset = cellOffset + row * columns;
        for (int column = 0; column < columns; column++) {
            action.accept(index.valueAt(canonicalId, offset + column));
        }
    }

    public int[] copyIds() {
        int[] values = new int[cellCount];
        for (int position = 0; position < cellCount; position++) {
            values[position] = index.valueAt(canonicalId, cellOffset + position);
        }
        return values;
    }

    public int[] copyShapeLane() {
        int[] shape = new int[shapeLength];
        for (int position = 0; position < shapeLength; position++) {
            shape[position] = index.valueAt(canonicalId, HEADER_WORDS + position);
        }
        return shape;
    }

    public MIndexTable<E> mutableCopy() {
        MIndexTable<E> table;
        if (isShaped()) {
            int[] shape = copyShapeLane();
            int[] names = new int[columns];
            int[] types = new int[columns];
            for (int column = 0; column < columns; column++) {
                names[column] = shape[column << 1];
                types[column] = shape[(column << 1) + 1];
            }
            MIndexShapeIndex shapes = new MIndexShapeIndex(index);
            table = new MIndexTable<>(
                    space, shapes, shapes.intern(names, types), rows());
        } else {
            table = new MIndexTable<>(space, columns, rows());
        }
        int[] values = copyIds();
        for (int row = 0; row < rows(); row++) {
            int[] rowIds = new int[columns];
            System.arraycopy(values, row * columns, rowIds, 0, columns);
            table.addRowIds(rowIds);
        }
        return table;
    }

    @Override
    public long structuralHash64() {
        return index.structuralHash64(canonicalId);
    }

    @Override
    public long signal64() {
        return index.signal64(canonicalId);
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof MIndexFrozenTable<?> that
                && index == that.index
                && canonicalId == that.canonicalId;
    }

    @Override
    public int hashCode() {
        long hash = structuralHash64();
        return (int) (hash ^ (hash >>> 32));
    }

    @Override
    public String toString() {
        return "MIndexFrozenTable[id="
                + canonicalId + ",rows=" + rows()
                + ",columns=" + columns + ']';
    }

    private int requireColumn(int memberNameId) {
        int column = columnOrdinal(memberNameId);
        if (column < 0) {
            throw new IllegalArgumentException(
                    "member name ID not in shape: " + memberNameId);
        }
        return column;
    }

    private void requireShapedColumn(int column) {
        if (!isShaped()) {
            throw new IllegalStateException("table has no bound shape");
        }
        Objects.checkIndex(column, columns);
    }

    private int cellIndex(int row, int column) {
        if (row < 0 || row >= rows() || column < 0 || column >= columns) {
            throw new IndexOutOfBoundsException("(" + row + "," + column + ")");
        }
        return Math.addExact(Math.multiplyExact(row, columns), column);
    }

    private static void validateShape(int[] shape) {
        MIndexSpace<?> strings = MIndexSpaces.strings();
        for (int offset = 0; offset < shape.length; offset += 2) {
            int nameId = shape[offset];
            int typeId = shape[offset + 1];
            strings.requireId(nameId);
            if (typeId < MIndexShapeIndex.UNSPECIFIED_TYPE_ID) {
                throw new IllegalArgumentException("invalid shape member type ID");
            }
            if (typeId >= 0) {
                strings.requireId(typeId);
            }
            for (int previous = 0; previous < offset; previous += 2) {
                if (shape[previous] == nameId) {
                    throw new IllegalArgumentException(
                            "duplicate member name ID: " + nameId);
                }
            }
        }
    }
}
