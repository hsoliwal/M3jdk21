/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import com.google.common.collect.RowSortedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3TreeMap;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Guava RowSortedTable contract over existing M3 sorted row index and M3 row maps.
 * Authoritative payloads are retained in M3 slot arrays and sorted row indexes;
 * Guava's public Table adapter supplies cell and column semantics. Sorting never
 * moves cell value objects, only row-order indexes. No retained M3 Cell/Entry node.
 * Concurrent use requires external synchronization, and no concrete TreeBasedTable
 * identity, serialization shape or universal speedup is claimed.
 */
public final class M3GuavaRowSortedTable<R,C,V> implements RowSortedTable<R,C,V> {
    private final NavigableMap<R,Map<C,V>> rows;
    private final Table<R,C,V> table;
    private SortedMap<R,Map<C,V>> sortedRows;

    public M3GuavaRowSortedTable(Comparator<? super R> comparator) {
        rows = new M3TreeMap<>(Objects.requireNonNull(comparator, "comparator"));
        table = Tables.newCustomTable(rows, M3LinkedHashMap::new);
    }

    /** Optional independent column ordering, matching Guava TreeBasedTable behavior. */
    public M3GuavaRowSortedTable(Comparator<? super R> rowOrder,
            Comparator<? super C> columnOrder) {
        rows = new M3TreeMap<>(Objects.requireNonNull(rowOrder, "rowOrder"));
        Objects.requireNonNull(columnOrder, "columnOrder");
        table = Tables.newCustomTable(rows, () -> new M3TreeMap<>(columnOrder));
    }

    @Override public boolean contains(Object row,Object column) { return table.contains(row,column); }
    @Override public boolean containsRow(Object row) { return table.containsRow(row); }
    @Override public boolean containsColumn(Object col) { return table.containsColumn(col); }
    @Override public boolean containsValue(Object value) { return table.containsValue(value); }
    @Override public V get(Object row,Object col) { return table.get(row,col); }
    @Override public boolean isEmpty() { return table.isEmpty(); }
    @Override public int size() { return table.size(); }
    @Override public void clear() { table.clear(); }
    @Override public V put(R row,C col,V value) { return table.put(row,col,value); }
    @Override public void putAll(Table<? extends R,? extends C,? extends V> other) { table.putAll(other); }
    @Override public V remove(Object row,Object col) { return table.remove(row,col); }
    @Override public Map<C,V> row(R row) { return table.row(row); }
    @Override public Map<R,V> column(C col) { return table.column(col); }
    @Override public Set<Table.Cell<R,C,V>> cellSet() { return table.cellSet(); }
    @Override public SortedSet<R> rowKeySet() { return rows.navigableKeySet(); }
    @Override public Set<C> columnKeySet() { return table.columnKeySet(); }
    @Override public Collection<V> values() { return table.values(); }
    @Override public Map<C,Map<R,V>> columnMap() { return table.columnMap(); }
    @Override public SortedMap<R,Map<C,V>> rowMap() {
        if (sortedRows == null) sortedRows = new SortedRowMap(rows);
        return sortedRows;
    }
    @Override public boolean equals(Object other) { return table.equals(other); }
    @Override public int hashCode() { return table.hashCode(); }
    @Override public String toString() { return table.toString(); }

    /** Bounded, sorted, live row view. Puts are disabled like Guava's rowMap. */
    private final class SortedRowMap extends AbstractMap<R,Map<C,V>> implements SortedMap<R,Map<C,V>> {
        private final NavigableMap<R,Map<C,V>> range;
        private SortedRowMap(NavigableMap<R,Map<C,V>> range) { this.range=range; }
        @Override public Comparator<? super R> comparator() { return range.comparator(); }
        @Override public R firstKey() { return range.firstKey(); }
        @Override public R lastKey() { return range.lastKey(); }
        @Override public SortedMap<R,Map<C,V>> subMap(R from,R to) { return new SortedRowMap(range.subMap(from,true,to,false)); }
        @Override public SortedMap<R,Map<C,V>> headMap(R to) { return new SortedRowMap(range.headMap(to,false)); }
        @Override public SortedMap<R,Map<C,V>> tailMap(R from) { return new SortedRowMap(range.tailMap(from,true)); }
        @Override public int size() { return range.size(); }
        @Override public boolean containsKey(Object key) { return range.containsKey(key); }
        @Override public Map<C,V> get(Object key) {
            return range.containsKey(key) ? table.rowMap().get(key) : null;
        }
        @Override public Map<C,V> remove(Object key) {
            return range.containsKey(key) ? table.rowMap().remove(key) : null;
        }
        @Override public Map<C,V> put(R key,Map<C,V> value) {
            throw new UnsupportedOperationException("rowMap is a live removal-only view");
        }
        @Override public void clear() { range.clear(); }
        @Override public Set<Map.Entry<R,Map<C,V>>> entrySet() {
            return new AbstractSet<>() {
                @Override public int size() { return range.size(); }
                @Override public void clear() { SortedRowMap.this.clear(); }
                @Override public Iterator<Map.Entry<R,Map<C,V>>> iterator() {
                    var cursor=range.navigableKeySet().iterator();
                    return new Iterator<>() {
                        @Override public boolean hasNext() { return cursor.hasNext(); }
                        @Override public Map.Entry<R,Map<C,V>> next() {
                            R key=cursor.next();
                            return new SimpleImmutableEntry<>(key,table.rowMap().get(key));
                        }
                        @Override public void remove() { cursor.remove(); }
                    };
                }
            };
        }
    }
}
