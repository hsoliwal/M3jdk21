/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import jdk.m3.collections.ObjectIntHashMap;

/**
 * Immutable object table with CSR cells, ordinal indexes and projected views.
 *
 * <p>Copying flattens nonempty source rows in encounter order. Null rows, row
 * maps, columns and values are rejected. Equal row keys share a row; duplicate
 * equal coordinates are rejected. No cell, entry or per-row map is retained.
 * Empty source rows contribute no row or column key. All exposed maps and sets
 * are immutable, including absent and empty views.
 *
 * <p>The Guava 33.4.8 ImmutableTable ordering contract is preserved for both
 * densities: sparse row views retain cell encounter order, while dense row
 * views follow global column encounter order. The same existing radix owner
 * constructs either permutation. Column projections preserve the provider's
 * representative keys and order without retaining a second table of maps.
 */
public final class FrozenTable<R, C, V> {
    static final long STATE_FROZEN = 1L;
    private final Object[] rowLane;
    private final int[] rowOffsetLane;
    private final Object[] columnLane;
    private final Object[] valueLane;
    private final Object[] distinctColumnLane;
    private final Object[] cellRowLane;
    private final int[] columnOrdinalLane;
    private final int[] columnOffsetLane;
    private final int[] columnCellLane;
    private final long[] stateMaskWords;
    private final ObjectIntHashMap<Object> rowOrdinals;
    private final ObjectIntHashMap<Object> columnOrdinals;
    private final boolean denseOrder;
    private final int size;

    /** Creates a valid immutable empty table. */
    public FrozenTable() {
        this(new Object[0], new int[] {0}, new Object[0], new Object[0],
                new Object[0], new Object[0], new int[0], new int[0],
                new ObjectIntHashMap<>(0), new ObjectIntHashMap<>(0), false);
    }

    private FrozenTable(Object[] rows, int[] offsets, Object[] columns,
            Object[] values, Object[] distinctColumns, Object[] cellRows,
            int[] columnIds, int[] sourceOrder, ObjectIntHashMap<Object> rowIndex,
            ObjectIntHashMap<Object> columnIndex, boolean dense) {
        rowLane = rows;
        rowOffsetLane = offsets;
        columnLane = columns;
        valueLane = values;
        distinctColumnLane = distinctColumns;
        cellRowLane = cellRows;
        columnOrdinalLane = columnIds;
        columnOffsetLane = columnOffsets(distinctColumns.length, columnIds);
        columnCellLane = columnCells(columnOffsetLane, columnIds, sourceOrder, dense);
        rowOrdinals = rowIndex;
        columnOrdinals = columnIndex;
        denseOrder = dense;
        size = values.length;
        stateMaskWords = frozenMask(size);
    }

    /** Live-cell mask: one set bit per frozen cell; the tail word keeps count % 64 low bits. */
    private static long[] frozenMask(int count) {
        long[] words = new long[(int) (((long) count + 63) >>> 6)];
        Arrays.fill(words, -1L);
        if ((count & 63) != 0) {
            words[words.length - 1] = (1L << (count & 63)) - 1;
        }
        return words;
    }

    /**
     * Counting pass of the column offset lane: entry c ends as the exclusive end of
     * column c, and the last entry holds the cell count.
     */
    private static int[] columnOffsets(int columns, int[] ids) {
        int[] offsets = new int[columns + 1];
        for (int id : ids) { offsets[id]++; }
        for (int c = 1; c <= columns; c++) { offsets[c] += offsets[c - 1]; }
        return offsets;
    }

    /**
     * Stable reverse scatter of packed cell slots in column-view order (packed order when
     * dense, source order when sparse); turns every end offset into its column start.
     */
    private static int[] columnCells(int[] offsets, int[] ids, int[] sourceOrder, boolean dense) {
        int[] cells = new int[ids.length];
        for (int index = ids.length - 1; index >= 0; index--) {
            int slot = dense ? index : sourceOrder[index];
            cells[--offsets[ids[slot]]] = slot;
        }
        return cells;
    }

    /** Retained compatibility descriptor; it does not describe admission status. */
    public static String scaffoldDescriptor() {
        return "M3FrozenTable|FROZEN_TABLE|OBJECT|FROZEN|Object[] rowLane;int[] rowOffsetLane;Object[] columnLane;Object[] valueLane";
    }

    /** Copies each input cell once; failed traversal never publishes a partial table. */
    public static <R, C, V> FrozenTable<R, C, V> copyOf(
            Map<? extends R, ? extends Map<? extends C, ? extends V>> source) {
        Objects.requireNonNull(source, "source");
        ArrayList<Object> rows = new ArrayList<>();
        ArrayList<Object> columns = new ArrayList<>();
        ArrayList<Object> cellRows = new ArrayList<>();
        ArrayList<Object> cellColumns = new ArrayList<>();
        ArrayList<Object> values = new ArrayList<>();
        ObjectIntHashMap<Object> rowIndex = new ObjectIntHashMap<>(0);
        ObjectIntHashMap<Object> columnIndex = new ObjectIntHashMap<>(0);
        PackedLongRowIndex coordinates = new PackedLongRowIndex(0);
        long[] rowIds = new long[8];
        long[] columnIds = new long[8];
        for (Map.Entry<? extends R, ? extends Map<? extends C, ? extends V>> row
                : source.entrySet()) {
            R rowKey = Objects.requireNonNull(row.getKey(), "row");
            Map<? extends C, ? extends V> cells = Objects.requireNonNull(row.getValue(), "row map");
            for (Map.Entry<? extends C, ? extends V> cell : cells.entrySet()) {
                C columnKey = Objects.requireNonNull(cell.getKey(), "column");
                V value = Objects.requireNonNull(cell.getValue(), "value");
                int rowId = rowIndex.getOrDefault(rowKey, -1);
                if (rowId < 0) {
                    rowId = rows.size();
                    rowIndex.put(rowKey, rowId);
                    rows.add(rowKey);
                }
                int columnId = columnIndex.getOrDefault(columnKey, -1);
                if (columnId < 0) {
                    columnId = columns.size();
                    columnIndex.put(columnKey, columnId);
                    columns.add(columnKey);
                }
                if (coordinates.find(rowId, columnId, rowIds, columnIds) >= 0) {
                    throw new IllegalArgumentException("duplicate table coordinate");
                }
                int count = values.size();
                if (count == rowIds.length) {
                    int length = PackedLongRowIndex.grownLength(count, count + 1);
                    rowIds = Arrays.copyOf(rowIds, length);
                    columnIds = Arrays.copyOf(columnIds, length);
                }
                coordinates.ensure(count + 1, rowIds, columnIds);
                rowIds[count] = rowId;
                columnIds[count] = columnId;
                coordinates.insert(count, rowIds, columnIds);
                cellRows.add(rowKey);
                cellColumns.add(columnKey);
                values.add(value);
            }
        }
        if (rows.size() == Integer.MAX_VALUE) {
            throw new OutOfMemoryError("FrozenTable offset capacity exhausted");
        }
        int count = values.size();
        rowIds = Arrays.copyOf(rowIds, count);
        columnIds = Arrays.copyOf(columnIds, count);
        boolean dense = count > ((long) rows.size() * columns.size()) / 2;
        int[] order = dense ? LongKeyOrder.lexicographic(rowIds, columnIds)
                : LongKeyOrder.ascending(rowIds, false);
        int[] offsets = new int[rows.size() + 1];
        int[] packedColumns = new int[count];
        int[] sourceOrder = new int[count];
        Object[] columnLane = new Object[count];
        Object[] valueLane = new Object[count];
        Object[] rowLane = new Object[count];
        int previousRow = -1;
        for (int packed = 0; packed < count; packed++) {
            int at = order[packed];
            int rowId = (int) rowIds[at];
            int columnId = (int) columnIds[at];
            if (rowId != previousRow) {
                offsets[rowId] = packed;
                previousRow = rowId;
            }
            columnLane[packed] = dense ? columns.get(columnId) : cellColumns.get(at);
            rowLane[packed] = dense ? rows.get(rowId) : cellRows.get(at);
            valueLane[packed] = values.get(at);
            packedColumns[packed] = columnId;
            sourceOrder[at] = packed;
        }
        offsets[rows.size()] = count;
        return new FrozenTable<>(rows.toArray(), offsets, columnLane, valueLane,
                columns.toArray(), rowLane, packedColumns, sourceOrder, rowIndex,
                columnIndex, dense);
    }

    public int size() { return size; }

    public boolean contains(Object row, Object column) { return get(row, column) != null; }

    public V get(Object row, Object column) {
        if (denseOrder && size > 1) {
            // The selected dense provider evaluates both indexes, even for an absent row.
            int rowId = rowOrdinal(row);
            int columnId = columnOrdinal(column);
            int at = cell(rowId, columnId);
            return at < 0 ? null : valueAt(at);
        }
        try {
            int rowId = rowOrdinal(row);
            return rowId < 0 ? null : valueInRow(rowId, column);
        } catch (ClassCastException | NullPointerException absent) {
            // AbstractTable's sparse and singleton safe lookups treat these as absence.
            return null;
        }
    }

    private static int lookup(Object[] keys, ObjectIntHashMap<Object> index, Object key) {
        if (keys.length == 0) { return -1; }
        if (keys.length == 1) { return keys[0].equals(key) ? 0 : -1; }
        return index.getOrDefault(key, -1);
    }

    private int rowOrdinal(Object key) { return lookup(rowLane, rowOrdinals, key); }
    private int columnOrdinal(Object key) { return lookup(distinctColumnLane, columnOrdinals, key); }

    private V valueInRow(int row, Object column) {
        int start = rowOffsetLane[row];
        if ((!denseOrder || size == 1) && rowOffsetLane[row + 1] - start == 1) {
            return columnLane[start].equals(column) ? valueAt(start) : null;
        }
        int at = cell(row, columnOrdinal(column));
        return at < 0 ? null : valueAt(at);
    }

    private int cell(int row, int column) {
        if (row < 0 || column < 0) { return -1; }
        int end = rowOffsetLane[row + 1];
        if (denseOrder) {
            int at = LaneSearch.lowerBound(columnOrdinalLane, rowOffsetLane[row], end, column);
            return at < end && columnOrdinalLane[at] == column ? at : -1;
        }
        for (int at = rowOffsetLane[row]; at < end; at++) {
            if (columnOrdinalLane[at] == column) { return at; }
        }
        return -1;
    }

    public Map<C, V> row(R rowKey) {
        Objects.requireNonNull(rowKey, "row");
        int ordinal = rowOrdinal(rowKey);
        return ordinal < 0 ? emptyMap() : rowView(ordinal);
    }

    private Map<C, V> rowView(int ordinal) {
        int start = rowOffsetLane[ordinal];
        int end = rowOffsetLane[ordinal + 1];
        return Collections.unmodifiableMap(new ProjectionMap<>(!denseOrder || size <= 1) {
            @Override public int size() { return end - start; }
            @Override public V get(Object key) {
                return valueInRow(ordinal, key);
            }
            @Override public Set<Entry<C, V>> entrySet() {
                return new OrderedSet<>() {
                    @Override public int size() { return end - start; }
                    @Override public Iterator<Entry<C, V>> iterator() {
                        return new Iterator<>() {
                            private int at = start;
                            @Override public boolean hasNext() { return at < end; }
                            @Override public Entry<C, V> next() {
                                if (!hasNext()) { throw new NoSuchElementException(); }
                                int slot = at++;
                                return new SimpleImmutableEntry<>(columnAt(slot), valueAt(slot));
                            }
                        };
                    }
                };
            }
        });
    }

    public Map<R, V> column(C columnKey) {
        Objects.requireNonNull(columnKey, "column");
        int ordinal = columnOrdinal(columnKey);
        return ordinal < 0 ? emptyMap() : columnView(ordinal);
    }

    /** Column view over one offset-lane slice; every known column owns at least one cell. */
    private Map<R, V> columnView(int ordinal) {
        int start = columnOffsetLane[ordinal];
        int end = columnOffsetLane[ordinal + 1];
        return Collections.unmodifiableMap(new ProjectionMap<>(!denseOrder || size <= 1) {
            @Override public int size() { return end - start; }
            @Override public V get(Object key) { return columnValue(ordinal, start, end, key); }
            @Override public Set<Entry<R, V>> entrySet() {
                return new OrderedSet<>() {
                    @Override public int size() { return end - start; }
                    @Override public Iterator<Entry<R, V>> iterator() {
                        return columnCursor(start, end);
                    }
                };
            }
        });
    }

    /** Column lookup; a single-cell sparse or singleton column compares its one stored row key. */
    private V columnValue(int ordinal, int start, int end, Object key) {
        if ((!denseOrder || size == 1) && end - start == 1) {
            int only = columnCellLane[start];
            return cellRowLane[only].equals(key) ? valueAt(only) : null;
        }
        int at = cell(rowOrdinal(key), ordinal);
        return at < 0 ? null : valueAt(at);
    }

    public Set<R> rowKeySet() { return keySet(rowLane, rowOrdinals); }
    public Set<C> columnKeySet() { return keySet(distinctColumnLane, columnOrdinals); }

    private static <K> Set<K> keySet(Object[] lane, ObjectIntHashMap<Object> index) {
        return Collections.unmodifiableSet(new OrderedSet<>() {
            @Override public int size() { return lane.length; }
            @Override public boolean contains(Object key) { return lookup(lane, index, key) >= 0; }
            @Override public Iterator<K> iterator() {
                return new Iterator<>() {
                    private int at;
                    @Override public boolean hasNext() { return at < lane.length; }
                    @Override @SuppressWarnings("unchecked") public K next() {
                        if (!hasNext()) { throw new NoSuchElementException(); }
                        return (K) lane[at++];
                    }
                };
            }
        });
    }

    public Map<R, Map<C, V>> rowMap() {
        return Collections.unmodifiableMap(new ProjectionMap<>(!denseOrder || size <= 1) {
            @Override public int size() { return rowLane.length; }
            @Override public Map<C, V> get(Object key) {
                int ordinal = rowOrdinal(key);
                return ordinal < 0 ? null : rowView(ordinal);
            }
            @Override public Set<R> keySet() { return rowKeySet(); }
            @Override public Set<Entry<R, Map<C, V>>> entrySet() {
                return new OrderedSet<>() {
                    @Override public int size() { return rowLane.length; }
                    @Override public Iterator<Entry<R, Map<C, V>>> iterator() {
                        return new Iterator<>() {
                            private int next;
                            @Override public boolean hasNext() { return next < rowLane.length; }
                            @Override public Entry<R, Map<C, V>> next() {
                                if (!hasNext()) { throw new NoSuchElementException(); }
                                int ordinal = next++;
                                return new SimpleImmutableEntry<>(rowAt(ordinal), rowView(ordinal));
                            }
                        };
                    }
                };
            }
        });
    }

    private static <K, T> Map<K, T> emptyMap() {
        return Collections.unmodifiableMap(new ProjectionMap<>(true) {
            @Override public int size() { return 0; }
            @Override public Set<Entry<K, T>> entrySet() {
                return new OrderedSet<>() {
                    @Override public int size() { return 0; }
                    @Override public Iterator<Entry<K, T>> iterator() { return Collections.emptyIterator(); }
                };
            }
        });
    }

    /** Walks one column slice of packed cell slots in frozen column order. */
    private Iterator<Map.Entry<R, V>> columnCursor(int start, int end) {
        return new Iterator<>() {
            private int at = start;
            @Override public boolean hasNext() { return at < end; }
            @Override public Map.Entry<R, V> next() {
                if (!hasNext()) { throw new NoSuchElementException(); }
                int slot = columnCellLane[at++];
                return new AbstractMap.SimpleImmutableEntry<>(cellRowAt(slot), valueAt(slot));
            }
        };
    }

    private abstract static class OrderedSet<E> extends AbstractSet<E> {
        @Override public Spliterator<E> spliterator() {
            return Spliterators.spliterator(iterator(), size(), Spliterator.ORDERED
                    | Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL);
        }
    }

    private abstract static class ProjectionMap<K, T> extends AbstractMap<K, T> {
        private final boolean nonNullValues;
        ProjectionMap(boolean nonNullValues) { this.nonNullValues = nonNullValues; }
        @Override public boolean containsKey(Object key) { return get(key) != null; }
        @Override public Set<K> keySet() {
            return new OrderedSet<>() {
                @Override public int size() { return ProjectionMap.this.size(); }
                @Override public boolean contains(Object key) { return ProjectionMap.this.containsKey(key); }
                @Override public Iterator<K> iterator() {
                    Iterator<Entry<K, T>> entries = entrySet().iterator();
                    return new Iterator<>() {
                        @Override public boolean hasNext() { return entries.hasNext(); }
                        @Override public K next() { return entries.next().getKey(); }
                    };
                }
            };
        }
        @Override public Collection<T> values() {
            return new AbstractCollection<>() {
                @Override public int size() { return ProjectionMap.this.size(); }
                @Override public Iterator<T> iterator() {
                    Iterator<Entry<K, T>> entries = entrySet().iterator();
                    return new Iterator<>() {
                        @Override public boolean hasNext() { return entries.hasNext(); }
                        @Override public T next() { return entries.next().getValue(); }
                    };
                }
                @Override public Spliterator<T> spliterator() {
                    return Spliterators.spliterator(iterator(), size(), Spliterator.ORDERED
                            | Spliterator.IMMUTABLE | (nonNullValues ? Spliterator.NONNULL : 0));
                }
            };
        }
    }

    @SuppressWarnings("unchecked") private R rowAt(int at) { return (R) rowLane[at]; }
    @SuppressWarnings("unchecked") private R cellRowAt(int at) { return (R) cellRowLane[at]; }
    @SuppressWarnings("unchecked") private C columnAt(int at) { return (C) columnLane[at]; }
    @SuppressWarnings("unchecked") private V valueAt(int at) { return (V) valueLane[at]; }

    public V put(R row, C column, V value) {
        throw new UnsupportedOperationException("immutable table");
    }

    public V remove(Object row, Object column) {
        throw new UnsupportedOperationException("immutable table");
    }
}
