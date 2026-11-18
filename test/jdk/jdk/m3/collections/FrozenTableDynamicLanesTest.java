/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary FrozenTable arrayification preserves CSR, radix dense/sparse ordering and callbacks
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm FrozenTableDynamicLanesTest
 */
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import jdk.internal.m3.collections.FrozenTable;

public final class FrozenTableDynamicLanesTest {
    private static long checks;
    private static long trace;

    public static void main(String[] args) {
        deterministicShapes();
        randomizedTables();
        invalidAndViewContracts();
        callbackNeutralSources();
        System.out.printf("FrozenTableDynamicLanesTest PASS %d assertions sourceTrace=%016x%n",
                checks, trace);
    }

    private static void deterministicShapes() {
        int[] rowCounts = {0, 1, 2, 3, 7, 8, 9, 16, 17, 63, 64, 65, 127,
                128, 129, 1024, 4097, 65536};
        for (int count : rowCounts) {
            LinkedHashMap<Integer, Map<Integer, Integer>> linked = new LinkedHashMap<>();
            for (int row = 0; row < count; row++) {
                LinkedHashMap<Integer, Integer> cells = new LinkedHashMap<>();
                if (row % 17 != 0) {
                    cells.put(row % 17, row * 7 + 1);
                    if (row % 3 != 0) cells.put(100 + row % 19, row * 7 + 2);
                }
                linked.put(row * 13 + 1, cells);
            }
            verify(FrozenTable.copyOf(linked), linked, count <= 4097);
            if (count <= 4097) {
                HashMap<Integer, Map<Integer, Integer>> hashed = new HashMap<>(linked);
                verify(FrozenTable.copyOf(hashed), hashed, true);
                TreeMap<Integer, Map<Integer, Integer>> sorted = new TreeMap<>(linked);
                verify(FrozenTable.copyOf(sorted), sorted, true);
                verify(FrozenTable.copyOf(Collections.unmodifiableMap(linked)), linked, true);
            }
        }
        for (int rows : new int[]{1, 2, 3, 8, 32}) {
            for (int cols : new int[]{1, 2, 5, 8, 64}) {
                LinkedHashMap<Integer, Map<Integer, Integer>> full = new LinkedHashMap<>();
                for (int row = 0; row < rows; row++) {
                    LinkedHashMap<Integer, Integer> cells = new LinkedHashMap<>();
                    for (int col = cols - 1; col >= 0; col--) {
                        cells.put(col * 17 + 3, row * 4096 + col);
                    }
                    full.put(row + 20, cells);
                }
                verify(FrozenTable.copyOf(full), full, true);
            }
        }
        verify(new FrozenTable<>(), Map.of(), true);
    }

    private static void randomizedTables() {
        for (int seed = 0; seed < 96; seed++) {
            Random random = new Random(seed * 241L + 0xAA55C7L);
            LinkedHashMap<Long, Map<Long, Long>> source = new LinkedHashMap<>();
            int rows = random.nextInt(1200);
            for (int row = 0; row < rows; row++) {
                LinkedHashMap<Long, Long> cells = new LinkedHashMap<>();
                int count = random.nextInt(11);
                for (int i = 0; i < count; i++) {
                    long col = random.nextInt(64);
                    cells.put(col, ((long) row << 32) | i);
                }
                source.put((long) row * 65537 + seed, cells);
            }
            verify(FrozenTable.copyOf(source), source, true);
            if ((seed & 7) == 0) {
                SizeThrowsMap<Long, Map<Long, Long>> subclass = new SizeThrowsMap<>();
                subclass.putAll(source);
                check(FrozenTable.copyOf(subclass).size() == FrozenTable.copyOf(source).size(),
                        "source subclass size remains opaque");
            }
        }
    }

    private static <R, C, V> void verify(FrozenTable<R, C, V> owner,
            Map<R, ? extends Map<C, V>> source, boolean checkColumns) {
        LinkedHashMap<R, LinkedHashMap<C, V>> rows = new LinkedHashMap<>();
        LinkedHashMap<C, LinkedHashMap<R, V>> columns = new LinkedHashMap<>();
        int cells = 0;
        for (Map.Entry<R, ? extends Map<C, V>> row : source.entrySet()) {
            if (row.getValue().isEmpty()) continue;
            LinkedHashMap<C, V> expected = new LinkedHashMap<>();
            for (Map.Entry<C, V> cell : row.getValue().entrySet()) {
                expected.put(cell.getKey(), cell.getValue());
                columns.computeIfAbsent(cell.getKey(), unused -> new LinkedHashMap<>())
                        .put(row.getKey(), cell.getValue());
                cells++;
            }
            rows.put(row.getKey(), expected);
        }
        check(owner.size() == cells, "cell count");
        check(owner.rowKeySet().equals(rows.keySet()), "nonempty row keys");
        check(owner.columnKeySet().equals(columns.keySet()), "nonempty column keys");
        check(new ArrayList<>(owner.rowKeySet()).equals(new ArrayList<>(rows.keySet())),
                "row key encounter order");
        check(new ArrayList<>(owner.columnKeySet()).equals(new ArrayList<>(columns.keySet())),
                "column key encounter order");
        check(owner.rowMap().size() == rows.size(), "rowMap size");
        check(owner.rowMap().keySet().equals(rows.keySet()), "rowMap keys");
        check(owner.rowMap().equals(rows), "rowMap logical equality");
        check(FrozenTable.scaffoldDescriptor().contains("rowOffsetLane"),
                "original descriptor unchanged");
        check(!owner.contains(new Object(), new Object()), "absent coordinates");
        check(owner.get(new Object(), new Object()) == null, "absent value");
        if (rows.isEmpty()) {
            check(owner.rowKeySet().isEmpty(), "empty row key set");
            check(owner.columnKeySet().isEmpty(), "empty column key set");
        }
        boolean dense = cells > ((long) rows.size() * columns.size()) / 2;
        for (Map.Entry<R, LinkedHashMap<C, V>> row : rows.entrySet()) {
            R rowKey = row.getKey();
            Map<C, V> expected = row.getValue();
            check(owner.row(rowKey).equals(expected), "row equality");
            check(owner.rowMap().get(rowKey).equals(expected), "rowMap projection");
            Collection<C> intendedOrder = dense ? columns.keySet() : expected.keySet();
            ArrayList<C> ordered = new ArrayList<>();
            for (C col : intendedOrder) if (expected.containsKey(col)) ordered.add(col);
            check(new ArrayList<>(owner.row(rowKey).keySet()).equals(ordered),
                    "dense global column or sparse source order");
            for (Map.Entry<C, V> cell : expected.entrySet()) {
                check(owner.contains(rowKey, cell.getKey()), "cell membership");
                check(cell.getValue().equals(owner.get(rowKey, cell.getKey())), "cell get");
                check(cell.getValue().equals(owner.row(rowKey).get(cell.getKey())), "row get");
            }
        }
        if (checkColumns) {
            for (Map.Entry<C, LinkedHashMap<R, V>> column : columns.entrySet()) {
                C columnKey = column.getKey();
                check(owner.column(columnKey).equals(column.getValue()), "column equality");
                check(new ArrayList<>(owner.column(columnKey).keySet())
                                .equals(new ArrayList<>(column.getValue().keySet())),
                        "column row encounter order");
            }
        }
        check(owner.rowMap().entrySet().stream().allMatch(e ->
                e.getValue().equals(rows.get(e.getKey()))), "lazy rowMap entries");
        expect(UnsupportedOperationException.class, () -> owner.put(null, null, null));
        expect(UnsupportedOperationException.class, () -> owner.remove(null, null));
        expect(UnsupportedOperationException.class, () -> owner.rowMap().clear());
        expect(UnsupportedOperationException.class, () -> owner.rowKeySet().clear());
        expect(UnsupportedOperationException.class, () -> owner.columnKeySet().clear());
        if (!rows.isEmpty()) {
            R firstRow = rows.keySet().iterator().next();
            expect(UnsupportedOperationException.class, () -> owner.row(firstRow).clear());
            Map.Entry<R, Map<C, V>> first = owner.rowMap().entrySet().iterator().next();
            expect(UnsupportedOperationException.class, () -> first.setValue(first.getValue()));
        }
        if (!columns.isEmpty()) {
            C firstColumn = columns.keySet().iterator().next();
            expect(UnsupportedOperationException.class, () -> owner.column(firstColumn).clear());
        }
        trace ^= Long.rotateLeft(cells * 0x9E3779B97F4A7C15L, 11)
                ^ Long.rotateLeft(rows.size() * 0xD1B54A32D192ED03L, 19)
                ^ Long.rotateLeft(columns.size() * 0x94D049BB133111EBL, 27);
    }

    private static void invalidAndViewContracts() {
        expect(NullPointerException.class, () -> FrozenTable.copyOf(null));
        Map<String, Map<String, String>> nullRow = new LinkedHashMap<>();
        nullRow.put(null, Map.of("x", "v"));
        expect(NullPointerException.class, () -> FrozenTable.copyOf(nullRow));
        Map<String, Map<String, String>> nullCells = new LinkedHashMap<>();
        nullCells.put("r", null);
        expect(NullPointerException.class, () -> FrozenTable.copyOf(nullCells));
        Map<String, Map<String, String>> nullColumn = new LinkedHashMap<>();
        LinkedHashMap<String, String> withNullColumn = new LinkedHashMap<>();
        withNullColumn.put(null, "v");
        nullColumn.put("r", withNullColumn);
        expect(NullPointerException.class, () -> FrozenTable.copyOf(nullColumn));
        Map<String, Map<String, String>> nullValue = new LinkedHashMap<>();
        LinkedHashMap<String, String> withNullValue = new LinkedHashMap<>();
        withNullValue.put("c", null);
        nullValue.put("r", withNullValue);
        expect(NullPointerException.class, () -> FrozenTable.copyOf(nullValue));

        IdentityHashMap<String, Map<String, String>> duplicateRows = new IdentityHashMap<>();
        duplicateRows.put(new String("equal"), Map.of("c", "one"));
        duplicateRows.put(new String("equal"), Map.of("c", "two"));
        expect(IllegalArgumentException.class, () -> FrozenTable.copyOf(duplicateRows));

        LinkedHashMap<String, Map<String, String>> mutable = new LinkedHashMap<>();
        LinkedHashMap<String, String> row = new LinkedHashMap<>();
        row.put("col", "original");
        mutable.put("row", row);
        FrozenTable<String, String, String> frozen = FrozenTable.copyOf(mutable);
        mutable.clear();
        row.clear();
        check("original".equals(frozen.get("row", "col")), "detached source map");
        check(frozen.row("row").get("col").equals("original"), "detached row view");
        check(frozen.column("col").get("row").equals("original"), "detached col view");
        check(frozen.row("missing").isEmpty(), "missing row immutable empty");
        check(frozen.column("missing").isEmpty(), "missing column immutable empty");
        expect(UnsupportedOperationException.class, () -> frozen.row("missing").clear());
        expect(UnsupportedOperationException.class, () -> frozen.column("missing").clear());
        expect(NoSuchElementException.class, () -> new FrozenTable<>().rowKeySet().iterator().next());
    }

    private static void callbackNeutralSources() {
        TraceMap outer = new TraceMap();
        FrozenTable<String, String, String> table = FrozenTable.copyOf(outer);
        check(table.size() == 3, "custom map flattened");
        check(outer.sizeCalls == 0, "custom source size never invoked");
        check(outer.entrySetCalls == 1 && outer.iteratorCalls == 1,
                "outer map traversed once");
        check(outer.nextCalls == 3 && outer.hasNextCalls == 4,
                "outer callbacks unchanged");
        check(outer.keyCalls == 3 && outer.valueCalls == 3, "row entry accessed once");
        check(table.get("row1", "c1").equals("v1"), "custom row1");
        check(table.get("row3", "c2").equals("v3"), "custom row3");
        trace ^= outer.signature;
        SizeThrowsMap<String, Map<String, String>> subclass = new SizeThrowsMap<>();
        SizeThrowsMap<String, String> inner = new SizeThrowsMap<>();
        inner.put("c", "v");
        subclass.put("row", inner);
        check(FrozenTable.copyOf(subclass).get("row", "c").equals("v"),
                "both JDK subclasses avoid size callbacks");
    }

    private static final class SizeThrowsMap<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;
        @Override public int size() { throw new AssertionError("Map.size callback"); }
    }

    private static final class TraceMap extends AbstractMap<String, Map<String, String>> {
        private final List<String> keys = List.of("row1", "row2", "row3");
        private final List<Map<String, String>> values = List.of(
                new LinkedHashMap<>(Map.of("c1", "v1", "c2", "v2")),
                Collections.emptyMap(), Map.of("c2", "v3"));
        private int sizeCalls;
        private int entrySetCalls;
        private int iteratorCalls;
        private int nextCalls;
        private int hasNextCalls;
        private int keyCalls;
        private int valueCalls;
        private long signature = 0xDA7A7A7AL;

        @Override public int size() {
            sizeCalls++;
            throw new AssertionError("source.size invoked");
        }
        @Override public Set<Map.Entry<String, Map<String, String>>> entrySet() {
            entrySetCalls++;
            return new AbstractSet<>() {
                @Override public int size() { throw new AssertionError("entrySet.size invoked"); }
                @Override public Iterator<Map.Entry<String, Map<String, String>>> iterator() {
                    iteratorCalls++;
                    return new Iterator<>() {
                        private int cursor;
                        @Override public boolean hasNext() {
                            hasNextCalls++;
                            return cursor < keys.size();
                        }
                        @Override public Map.Entry<String, Map<String, String>> next() {
                            if (cursor >= keys.size()) throw new NoSuchElementException();
                            int position = cursor++;
                            nextCalls++;
                            signature = Long.rotateLeft(signature ^ position, 7);
                            return new AbstractMap.SimpleImmutableEntry<>(
                                    keys.get(position), values.get(position)) {
                                @Override public String getKey() {
                                    keyCalls++;
                                    signature = Long.rotateLeft(signature ^ position, 3);
                                    return super.getKey();
                                }
                                @Override public Map<String, String> getValue() {
                                    valueCalls++;
                                    signature = Long.rotateLeft(signature ^ position, 13);
                                    return super.getValue();
                                }
                            };
                        }
                    };
                }
            };
        }
    }

    private static void check(boolean okay, String why) {
        checks++;
        if (!okay) throw new AssertionError(why);
    }

    private static void expect(Class<? extends Throwable> expected, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("Expected " + expected.getName() + " got " + failure, failure);
        }
        throw new AssertionError("Expected " + expected.getName());
    }
}
