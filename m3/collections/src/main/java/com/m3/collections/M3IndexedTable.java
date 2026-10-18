/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Objects;

/**
 * M3-native, sparse two-coordinate table. A value resides in exactly one Object[]
 * slot; row/column/global iteration and pair lookup manipulate primitive indices.
 * No retained Cell, Entry, Node, vendor adapter or per-occurrence object exists.
 *
 * <p>Existing {@link M3LinkedHashMap} directories intern distinct row/column keys;
 * {@link M3LongLongHashMap} indexes collision-free packed row/column IDs to
 * physical cell slots. Each axis has an independent contiguous primitive-link
 * directory. Slots recycle after deletion and clear their Java payload reference;
 * coordinate IDs recycle only after the last matching cell is removed; the
 * pair lookup is deleted before reuse, and stale views fail on revision.
 * This class is deliberately single-threaded; callers must externally synchronize access.
 *
 * <p>Snapshot views contain only physical int[] indices, offset windows and an
 * owner revision. They reject all access after any owner mutation, including
 * value/state changes. They neither copy source payloads nor keep stale slots
 * alive. Optional JNI only selects detached int[] indices and byte[] flags
 * through the incumbent {@link M3IndexMaskCompactor}; Java object references,
 * predicates and comparators never cross JNI. The public API is M3-owned,
 * not a substitute for Guava Table or any other vendor interface.
 */
public final class M3IndexedTable<R, C, V> {
    /** Reserved internal presence bit. All public state writes preserve LIVE. */
    public static final int LIVE = 1;
    public static final int VISIBLE = 2;
    public static final int DIRTY = 4;
    public static final int PIN_REQUEST = 8; // metadata, not JVM/GC pinning
    private static final int MAX_SLOTS = Integer.MAX_VALUE - 8;

    private final M3LinkedHashMap<R, Integer> rowDirectory = new M3LinkedHashMap<>();
    private final M3LinkedHashMap<C, Integer> columnDirectory = new M3LinkedHashMap<>();
    private final M3LongLongHashMap pairToSlot = new M3LongLongHashMap();

    private Object[] rowKeys = M3SlotArrays.EMPTY_OBJECTS;
    private Object[] columnKeys = M3SlotArrays.EMPTY_OBJECTS;
    private int[] rowHead = M3SlotArrays.EMPTY_INTS, rowTail = M3SlotArrays.EMPTY_INTS;
    private int[] columnHead = M3SlotArrays.EMPTY_INTS, columnTail = M3SlotArrays.EMPTY_INTS;
    private int[] rowCounts = M3SlotArrays.EMPTY_INTS, columnCounts = M3SlotArrays.EMPTY_INTS;
    private int[] freeRows = M3SlotArrays.EMPTY_INTS, freeColumns = M3SlotArrays.EMPTY_INTS;
    private int rowsAllocated, columnsAllocated, freeRow = -1, freeColumn = -1;

    private Object[] values = M3SlotArrays.EMPTY_OBJECTS;
    private byte[] flags = M3SlotArrays.EMPTY_BYTES;
    private int[] cellRow = M3SlotArrays.EMPTY_INTS, cellColumn = M3SlotArrays.EMPTY_INTS;
    private int[] rowBefore = M3SlotArrays.EMPTY_INTS, rowAfter = M3SlotArrays.EMPTY_INTS;
    private int[] columnBefore = M3SlotArrays.EMPTY_INTS, columnAfter = M3SlotArrays.EMPTY_INTS;
    private int[] before = M3SlotArrays.EMPTY_INTS, after = M3SlotArrays.EMPTY_INTS;
    private int cellsAllocated, free = -1, first = -1, last = -1, size;
    private long revision;

    public M3IndexedTable() { }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public long revision() { return revision; }
    public int rowCount() { return rowDirectory.size(); }
    public int columnCount() { return columnDirectory.size(); }
    public int physicalCapacity() { return values.length; }
    public int rowIdCapacity() { return rowKeys.length; }
    public int columnIdCapacity() { return columnKeys.length; }
    public boolean containsRow(Object row) { return rowDirectory.containsKey(row); }
    public boolean containsColumn(Object column) { return columnDirectory.containsKey(column); }
    public int rowSize(Object row) {
        Integer id = rowDirectory.get(row);
        return id == null ? 0 : rowCounts[id];
    }
    public int columnSize(Object column) {
        Integer id = columnDirectory.get(column);
        return id == null ? 0 : columnCounts[id];
    }

    private static long pair(int row, int column) {
        return ((long) row << 32) | (column & 0xffff_ffffL);
    }

    private int find(Object row, Object column) {
        Integer r = rowDirectory.get(row), c = columnDirectory.get(column);
        if (r == null || c == null) return -1;
        return (int) (pairToSlot.getOrDefault(pair(r, c), 0L) - 1L);
    }

    public boolean contains(Object row, Object column) { return find(row, column) >= 0; }
    public V get(Object row, Object column) {
        int slot = find(row, column);
        return slot < 0 ? null : M3SlotArrays.get(values, slot);
    }
    public V getOrDefault(Object row, Object column, V fallback) {
        int slot = find(row, column);
        return slot < 0 ? fallback : M3SlotArrays.get(values, slot);
    }
    public int state(Object row, Object column) {
        int slot = find(row, column);
        return slot < 0 ? 0 : flags[slot] & 0xff;
    }

    /**
     * Insert or replace a single coordinate, including null row/column/value.
     * Identity of the physical slot stays stable on replacement. A first insert
     * preallocates all typed lanes before publishing the coordinate index.
     */
    public V put(R row, C column, V value) { return put(row, column, value, 0); }

    public V put(R row, C column, V value, int stateBits) {
        requireUserBits(stateBits);
        int old = find(row, column);
        if (old >= 0) {
            V prior = M3SlotArrays.get(values, old);
            values[old] = value;
            flags[old] = (byte) (stateBits | LIVE);
            revision++;
            return prior;
        }
        if (size == MAX_SLOTS) throw new OutOfMemoryError("M3 table capacity exhausted");
        Integer knownRow = rowDirectory.get(row), knownColumn = columnDirectory.get(column);
        boolean newRow = knownRow == null, newColumn = knownColumn == null;
        int rowId = newRow ? (freeRow >= 0 ? freeRow : rowsAllocated) : knownRow;
        int columnId = newColumn ? (freeColumn >= 0 ? freeColumn : columnsAllocated) : knownColumn;
        if (rowId >= MAX_SLOTS || columnId >= MAX_SLOTS) {
            throw new OutOfMemoryError("M3 coordinate ID capacity exhausted");
        }
        if (newRow) ensureRow(rowId + 1);
        if (newColumn) ensureColumn(columnId + 1);
        if (free < 0) ensureCell(cellsAllocated + 1);
        // Roll back newly-created dictionary keys on allocation/index failures;
        // no live cell or slot links are published until pairToSlot succeeds.
        boolean installedRow = false, installedColumn = false;
        try {
            if (newRow) { rowDirectory.put(row, rowId); installedRow = true; }
            if (newColumn) { columnDirectory.put(column, columnId); installedColumn = true; }
            int slot = free >= 0 ? free : cellsAllocated;
            if (!pairToSlot.put(pair(rowId, columnId), (long) slot + 1L)) {
                throw new IllegalStateException("duplicate coordinate index");
            }
            if (free >= 0) free = after[slot]; else cellsAllocated++;
            if (newRow) {
                if (freeRow >= 0) freeRow = freeRows[rowId]; else rowsAllocated++;
                rowKeys[rowId] = row; rowHead[rowId] = rowTail[rowId] = -1;
            }
            if (newColumn) {
                if (freeColumn >= 0) freeColumn = freeColumns[columnId]; else columnsAllocated++;
                columnKeys[columnId] = column; columnHead[columnId] = columnTail[columnId] = -1;
            }
            values[slot] = value;
            flags[slot] = (byte) (stateBits | LIVE);
            cellRow[slot] = rowId;
            cellColumn[slot] = columnId;
            rowBefore[slot] = rowTail[rowId]; rowAfter[slot] = -1;
            if (rowTail[rowId] < 0) rowHead[rowId] = slot;
            else rowAfter[rowTail[rowId]] = slot;
            rowTail[rowId] = slot;
            columnBefore[slot] = columnTail[columnId]; columnAfter[slot] = -1;
            if (columnTail[columnId] < 0) columnHead[columnId] = slot;
            else columnAfter[columnTail[columnId]] = slot;
            columnTail[columnId] = slot;
            before[slot] = last; after[slot] = -1;
            if (last < 0) first = slot; else after[last] = slot;
            last = slot;
            rowCounts[rowId]++; columnCounts[columnId]++; size++; revision++;
            return null;
        } catch (RuntimeException | Error failure) {
            if (installedColumn && newColumn && columnCounts[columnId] == 0) {
                columnDirectory.remove(column);
            }
            if (installedRow && newRow && rowCounts[rowId] == 0) {
                rowDirectory.remove(row);
            }
            throw failure;
        }
    }

    /** Remove only the specified present coordinate, preserving all other cells. */
    public boolean removeCell(Object row, Object column) {
        int slot = find(row, column);
        if (slot < 0) return false;
        int r = cellRow[slot], c = cellColumn[slot];
        if (!pairToSlot.remove(pair(r, c))) throw new IllegalStateException("missing cell index");
        int a = rowBefore[slot], b = rowAfter[slot];
        if (a < 0) rowHead[r] = b; else rowAfter[a] = b;
        if (b < 0) rowTail[r] = a; else rowBefore[b] = a;
        a = columnBefore[slot]; b = columnAfter[slot];
        if (a < 0) columnHead[c] = b; else columnAfter[a] = b;
        if (b < 0) columnTail[c] = a; else columnBefore[b] = a;
        a = before[slot]; b = after[slot];
        if (a < 0) first = b; else after[a] = b;
        if (b < 0) last = a; else before[b] = a;
        values[slot] = null; flags[slot] = 0;
        rowBefore[slot] = rowAfter[slot] = -1;
        columnBefore[slot] = columnAfter[slot] = -1;
        before[slot] = -1;
        after[slot] = free; free = slot;
        if (--rowCounts[r] == 0) {
            rowDirectory.remove(rowKeys[r]); rowKeys[r] = null;
            rowHead[r] = rowTail[r] = -1;
            freeRows[r] = freeRow; freeRow = r;
        }
        if (--columnCounts[c] == 0) {
            columnDirectory.remove(columnKeys[c]); columnKeys[c] = null;
            columnHead[c] = columnTail[c] = -1;
            freeColumns[c] = freeColumn; freeColumn = c;
        }
        size--; revision++;
        return true;
    }

    public V remove(Object row, Object column) {
        int slot = find(row, column);
        if (slot < 0) return null;
        V previous = M3SlotArrays.get(values, slot);
        removeCell(row, column);
        return previous;
    }

    public int updateState(Object row, Object column, int setBits, int clearBits) {
        requireUserBits(setBits); requireUserBits(clearBits);
        if ((setBits & clearBits) != 0) throw new IllegalArgumentException("overlapping masks");
        int slot = find(row, column);
        if (slot < 0) throw new IllegalArgumentException("absent table coordinate");
        int old = flags[slot] & 0xff;
        int changed = (old | setBits) & ~clearBits;
        if (changed != old) { flags[slot] = (byte) changed; revision++; }
        return old;
    }

    private static void requireUserBits(int bits) {
        if ((bits & ~0xfe) != 0) throw new IllegalArgumentException("state flags must fit bits 1..7");
    }
    private static void requireSelection(int required, int forbidden) {
        if (((required | forbidden) & ~0xff) != 0 || (required & forbidden) != 0) {
            throw new IllegalArgumentException("state masks must fit 8 bits and be disjoint");
        }
    }

    public IndexView all() { return linkView(first, size, after); }
    public IndexView row(Object row) {
        Integer id = rowDirectory.get(row);
        return id == null ? new IndexView(new int[0], 0, 0, revision)
                : linkView(rowHead[id], rowCounts[id], rowAfter);
    }
    public IndexView column(Object column) {
        Integer id = columnDirectory.get(column);
        return id == null ? new IndexView(new int[0], 0, 0, revision)
                : linkView(columnHead[id], columnCounts[id], columnAfter);
    }
    private IndexView linkView(int head, int count, int[] links) {
        int[] positions = new int[count];
        int cursor = head;
        for (int i = 0; i < count; i++) {
            if (cursor < 0) throw new IllegalStateException("damaged table index links");
            positions[i] = cursor; cursor = links[cursor];
        }
        if (cursor >= 0) throw new IllegalStateException("extra table index links");
        return new IndexView(positions, 0, count, revision);
    }

    /** Immutable primitive index descriptor bound to one table revision. */
    public final class IndexView {
        private final int[] indices;
        private final int from, to;
        private final long expectedRevision;
        private IndexView(int[] indices, int from, int to, long expectedRevision) {
            this.indices = indices; this.from = from; this.to = to;
            this.expectedRevision = expectedRevision;
        }
        private void valid() {
            if (revision != expectedRevision) {
                throw new ConcurrentModificationException("M3 table index view is stale");
            }
        }
        public int size() { valid(); return to - from; }
        public int physicalAt(int index) {
            valid(); return indices[from + Objects.checkIndex(index, to - from)];
        }
        public R rowAt(int index) {
            int slot = physicalAt(index);
            return M3SlotArrays.get(rowKeys, cellRow[slot]);
        }
        public C columnAt(int index) {
            int slot = physicalAt(index);
            return M3SlotArrays.get(columnKeys, cellColumn[slot]);
        }
        public V valueAt(int index) { return M3SlotArrays.get(values, physicalAt(index)); }
        public int stateAt(int index) { return flags[physicalAt(index)] & 0xff; }
        public int[] physicalIndices() {
            valid(); return Arrays.copyOfRange(indices, from, to);
        }
        /** Shares the original int[] physical permutation; O(1) metadata slice. */
        public IndexView slice(int begin, int end) {
            valid(); Objects.checkFromToIndex(begin, end, to - from);
            return new IndexView(indices, from + begin, from + end, expectedRevision);
        }
        public IndexView reversed() {
            valid(); int[] changed = physicalIndices();
            for (int left = 0, right = changed.length - 1; left < right; left++, right--) {
                int saved = changed[left]; changed[left] = changed[right]; changed[right] = saved;
            }
            return new IndexView(changed, 0, changed.length, expectedRevision);
        }
        /**
         * Stable bottom-up primitive mergesort of slot IDs only. Values never move.
         * Comparator callbacks remain in Java and ties retain original encounter order.
         */
        public IndexView sortedByValue(Comparator<? super V> comparator) {
            valid(); Objects.requireNonNull(comparator, "comparator");
            int[] sorted = physicalIndices();
            int[] scratch = new int[sorted.length];
            for (int width = 1; width < sorted.length;) {
                for (int start = 0; start < sorted.length;) {
                    int mid = (int) Math.min(sorted.length, (long) start + width);
                    int end = (int) Math.min(sorted.length, (long) mid + width);
                    int a = start, b = mid, write = start;
                    while (a < mid && b < end) {
                        V firstValue = M3SlotArrays.get(values, sorted[a]);
                        V secondValue = M3SlotArrays.get(values, sorted[b]);
                        // Comparator callbacks are arbitrary Java: refuse a reentrant
                        // write before publishing an index that may reference a recycled slot.
                        int comparison = comparator.compare(firstValue, secondValue);
                        valid();
                        scratch[write++] = comparison <= 0 ? sorted[a++] : sorted[b++];
                    }
                    while (a < mid) scratch[write++] = sorted[a++];
                    while (b < end) scratch[write++] = sorted[b++];
                    start = end;
                }
                int[] swap = sorted; sorted = scratch; scratch = swap;
                if (width > sorted.length / 2) break;
                width <<= 1;
            }
            // Also refuse post-loop drift rather than returning a stale descriptor.
            valid();
            return new IndexView(sorted, 0, sorted.length, expectedRevision);
        }
        /**
         * Pure primitive state projection. The normal Java path allocates only
         * the exact selected index array. NATIVE_REQUIRED uses the already-owned
         * JNI mask compactor; it neither receives values nor calls Java predicates.
         */
        public IndexView select(int required, int forbidden, M3IndexMaskCompactor.Backend backend) {
            valid(); requireSelection(required, forbidden);
            Objects.requireNonNull(backend, "backend");
            if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) {
                byte[] accepted = new byte[to - from];
                int[] ordinal = physicalIndices();
                for (int i = 0; i < ordinal.length; i++) {
                    int bits = flags[ordinal[i]] & 0xff;
                    accepted[i] = (byte) ((bits & required) == required
                            && (bits & forbidden) == 0 ? 1 : 0);
                }
                int[] selected = M3IndexMaskCompactor.select(ordinal, accepted, 1, backend);
                return new IndexView(selected, 0, selected.length, expectedRevision);
            }
            if (required == 0 && forbidden == 0) {
                return new IndexView(indices, from, to, expectedRevision);
            }
            int matches = 0;
            for (int i = from; i < to; i++) {
                int bits = flags[indices[i]] & 0xff;
                if ((bits & required) == required && (bits & forbidden) == 0) matches++;
            }
            int[] selected = new int[matches];
            int cursor = 0;
            for (int i = from; i < to; i++) {
                int bits = flags[indices[i]] & 0xff;
                if ((bits & required) == required && (bits & forbidden) == 0) {
                    selected[cursor++] = indices[i];
                }
            }
            return new IndexView(selected, 0, selected.length, expectedRevision);
        }
    }

    /** Grow dimensions using the canonical M3 checked capacity helper. */
    private void ensureRow(int required) {
        if (required <= rowKeys.length) return;
        int cap = rowKeys.length;
        while (cap < required) cap = M3SlotArrays.grow(cap);
        Object[] keys = Arrays.copyOf(rowKeys, cap);
        int[] heads = Arrays.copyOf(rowHead, cap), tails = Arrays.copyOf(rowTail, cap);
        int[] counts = Arrays.copyOf(rowCounts, cap);
        int[] released = Arrays.copyOf(freeRows, cap);
        Arrays.fill(heads, rowKeys.length, cap, -1);
        Arrays.fill(tails, rowKeys.length, cap, -1);
        rowKeys = keys; rowHead = heads; rowTail = tails; rowCounts = counts;
        freeRows = released;
    }
    private void ensureColumn(int required) {
        if (required <= columnKeys.length) return;
        int cap = columnKeys.length;
        while (cap < required) cap = M3SlotArrays.grow(cap);
        Object[] keys = Arrays.copyOf(columnKeys, cap);
        int[] heads = Arrays.copyOf(columnHead, cap), tails = Arrays.copyOf(columnTail, cap);
        int[] counts = Arrays.copyOf(columnCounts, cap);
        int[] released = Arrays.copyOf(freeColumns, cap);
        Arrays.fill(heads, columnKeys.length, cap, -1);
        Arrays.fill(tails, columnKeys.length, cap, -1);
        columnKeys = keys; columnHead = heads; columnTail = tails; columnCounts = counts;
        freeColumns = released;
    }
    private void ensureCell(int required) {
        if (required <= values.length) return;
        int cap = values.length;
        while (cap < required) cap = M3SlotArrays.grow(cap);
        Object[] val = Arrays.copyOf(values, cap);
        byte[] state = Arrays.copyOf(flags, cap);
        int[] rows = Arrays.copyOf(cellRow, cap), cols = Arrays.copyOf(cellColumn, cap);
        int[] rb = Arrays.copyOf(rowBefore, cap), ra = Arrays.copyOf(rowAfter, cap);
        int[] cb = Arrays.copyOf(columnBefore, cap), ca = Arrays.copyOf(columnAfter, cap);
        int[] gb = Arrays.copyOf(before, cap), ga = Arrays.copyOf(after, cap);
        values = val; flags = state; cellRow = rows; cellColumn = cols;
        rowBefore = rb; rowAfter = ra; columnBefore = cb; columnAfter = ca;
        before = gb; after = ga;
    }
}
