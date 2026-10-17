/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/** Deterministic Java21/JNI property oracle over M3-native row/column slot directories. */
public final class M3IndexedTableSelfTest {
    private static long checks;
    private M3IndexedTableSelfTest() { }
    private record Cell(String row, String column) { }
    private record Expected(Integer value, int flags) { }

    private static void ok(boolean success, String label) {
        checks++;
        if (!success) throw new AssertionError(label);
    }
    private static void eq(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }
    private static void fails(Class<? extends Throwable> type, Runnable task, String label) {
        checks++;
        try { task.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError(label + " unexpected: " + error, error);
        }
        throw new AssertionError(label + " should refuse");
    }

    private static List<Cell> coordinates(M3IndexedTable<String, String, Integer>.IndexView view) {
        List<Cell> result = new ArrayList<>();
        for (int i = 0; i < view.size(); i++) {
            result.add(new Cell(view.rowAt(i), view.columnAt(i)));
        }
        return result;
    }

    private static void basicAndSlices(boolean nativeRun) {
        var table = new M3IndexedTable<String, String, Integer>();
        table.put(null, null, null);
        table.put("R", "C", 7, M3IndexedTable.VISIBLE);
        table.put("R", "D", 7, M3IndexedTable.DIRTY);
        table.put("S", "C", 9, M3IndexedTable.VISIBLE | M3IndexedTable.DIRTY);
        table.put("R", "E", -1);
        eq(3, table.rowCount(), "row dictionary owner");
        eq(4, table.columnCount(), "column dictionary owner");
        var row = table.row("R");
        eq(List.of(new Cell("R", "C"), new Cell("R", "D"), new Cell("R", "E")),
                coordinates(row), "row value slots");
        eq(List.of(new Cell("R", "E"), new Cell("R", "D"), new Cell("R", "C")),
                coordinates(row.reversed()), "reverse index only");
        eq(List.of(new Cell("R", "D")), coordinates(row.slice(1, 2)), "offset-only slice");
        eq(List.of(new Cell("R", "E"), new Cell("R", "C"), new Cell("R", "D")),
                coordinates(row.sortedByValue(Comparator.naturalOrder())), "stable ties");
        eq(List.of(new Cell("R", "C")),
                coordinates(row.select(M3IndexedTable.VISIBLE, M3IndexedTable.DIRTY,
                        M3IndexMaskCompactor.Backend.JAVA)), "required and forbidden bits");
        eq(List.of(new Cell("R", "C"), new Cell("R", "D"), new Cell("R", "E")),
                coordinates(row.select(0, 0, M3IndexMaskCompactor.Backend.JAVA)),
                "mask-zero all physical slots");
        if (nativeRun) {
            var nativeSelected = row.select(M3IndexedTable.VISIBLE, 0,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            eq(coordinates(row.select(M3IndexedTable.VISIBLE, 0,
                    M3IndexMaskCompactor.Backend.JAVA)), coordinates(nativeSelected),
                    "real JNI primitive index parity");
        } else if (!M3IndexMaskCompactor.nativeAvailable()) {
            fails(UnsatisfiedLinkError.class, () -> row.select(0, 0,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED), "no native fallback");
        }
        fails(IllegalArgumentException.class, () -> row.select(256, 0,
                M3IndexMaskCompactor.Backend.JAVA), "mask width");
        fails(IllegalArgumentException.class, () -> row.select(2, 2,
                M3IndexMaskCompactor.Backend.JAVA), "disjoint state masks");
        fails(IllegalArgumentException.class, () -> table.put("R", "C", 4, 1),
                "cannot write internal live mask");
        fails(IllegalArgumentException.class, () -> table.updateState("R", "C", 2, 2),
                "cannot set+clear same bit");
        fails(IndexOutOfBoundsException.class, () -> row.slice(0, 4), "index window bounds");
        eq(3, table.updateState("R", "C", M3IndexedTable.DIRTY, 0), "old state on update");
        fails(ConcurrentModificationException.class, () -> row.valueAt(0),
                "state mutation invalidates snapshots");
        var updated = table.row("R");
        eq(7, table.put("R", "C", 70), "replace value without relocating coordinates");
        fails(ConcurrentModificationException.class, updated::physicalIndices,
                "value mutation invalidates snapshots");
        var beforeRemove = table.all();
        eq(true, table.removeCell("R", "D"), "remove existing occurrence");
        eq(false, table.removeCell("R", "D"), "repeat deletion returns false");
        eq(List.of(new Cell("R", "C"), new Cell("R", "E")),
                coordinates(table.row("R")), "row deletes only one coordinate");
        fails(ConcurrentModificationException.class, beforeRemove::physicalIndices,
                "physical slot may recycle after deletion");
        int physicalCapacity = table.physicalCapacity();
        table.put("S", "D", 17); // reuses old physical slot
        eq(physicalCapacity, table.physicalCapacity(), "slot recycling");
        eq(List.of(new Cell("S", "C"), new Cell("S", "D")),
                coordinates(table.row("S")), "row-linked order on recycled slot");
        eq(null, table.remove(null, null), "remove null-valued cell");
        eq(false, table.containsRow(null), "retire empty row directory");
        eq(false, table.containsColumn(null), "retire empty column directory");
        eq(false, table.contains(null, null), "null pair removed");
    }

    private static void randomized(boolean nativeRun) {
        var table = new M3IndexedTable<String, String, Integer>();
        LinkedHashMap<Cell, Expected> expected = new LinkedHashMap<>();
        Random random = new Random(0x4d335441424c4531L);
        for (int operation = 0; operation < 6_000; operation++) {
            String row = random.nextInt(11) == 0 ? null : "R" + random.nextInt(23);
            String column = random.nextInt(13) == 0 ? null : "C" + random.nextInt(21);
            Cell key = new Cell(row, column);
            int kind = random.nextInt(8);
            if (kind <= 3) {
                Integer value = random.nextInt(13) == 0 ? null : random.nextInt(61) - 30;
                int state = ((random.nextInt(8)) << 1);
                Expected prior = expected.put(key, new Expected(value, state | 1));
                eq(prior == null ? null : prior.value(), table.put(row, column, value, state),
                        "reference put returns previous value");
            } else if (kind == 4) {
                boolean existed = expected.remove(key) != null;
                eq(existed, table.removeCell(row, column), "oracle remove identity");
            } else if (kind == 5) {
                Expected prior = expected.get(key);
                if (prior != null) {
                    int flag = random.nextInt(4) + 1;
                    int bit = 1 << flag;
                    int newBits = prior.flags() ^ bit;
                    eq(prior.flags(), table.updateState(row, column,
                            (newBits & bit), (prior.flags() & bit)), "state transition old bits");
                    expected.put(key, new Expected(prior.value(), newBits));
                }
            } else {
                Expected prior = expected.get(key);
                eq(prior != null, table.contains(row, column), "oracle contains");
                eq(prior == null ? null : prior.value(), table.get(row, column), "oracle get");
                eq(prior == null ? 0 : prior.flags(), table.state(row, column),
                        "oracle state");
            }
            eq(expected.size(), table.size(), "whole table cardinality");
            if (operation % 37 == 0) {
                List<Cell> wanted = new ArrayList<>(expected.keySet());
                eq(wanted, coordinates(table.all()), "global insertion order");
                Map<String, List<Cell>> rowGroups = new HashMap<>();
                Map<String, List<Cell>> colGroups = new HashMap<>();
                for (Cell cell : wanted) {
                    rowGroups.computeIfAbsent(cell.row(), unused -> new ArrayList<>()).add(cell);
                    colGroups.computeIfAbsent(cell.column(), unused -> new ArrayList<>()).add(cell);
                }
                eq(rowGroups.size(), table.rowCount(), "distinct active rows");
                eq(colGroups.size(), table.columnCount(), "distinct active columns");
                for (var entry : rowGroups.entrySet()) {
                    eq(entry.getValue(), coordinates(table.row(entry.getKey())), "row index");
                }
                for (var entry : colGroups.entrySet()) {
                    eq(entry.getValue(), coordinates(table.column(entry.getKey())), "column index");
                }
                var snapshot = table.all();
                int required = random.nextInt(8) << 1;
                int forbidden = 0x1e & ~required;
                List<Cell> admitted = new ArrayList<>();
                for (var entry : expected.entrySet()) {
                    int state = entry.getValue().flags();
                    if ((state & required) == required && (state & forbidden) == 0) {
                        admitted.add(entry.getKey());
                    }
                }
                eq(admitted, coordinates(snapshot.select(required, forbidden,
                        M3IndexMaskCompactor.Backend.JAVA)), "state selection differential");
                if (nativeRun) {
                    eq(admitted, coordinates(snapshot.select(required, forbidden,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED)),
                            "actual JNI state projection differential");
                }
            }
        }
    }

    /** Axis dictionaries recycle integer IDs; churn must not retain every old key. */
    private static void recyclingPressure() {
        var table = new M3IndexedTable<String, String, Integer>();
        for (int cycle = 0; cycle < 8_000; cycle++) {
            String row = "retired-row-" + cycle;
            String column = "retired-column-" + cycle;
            table.put(row, column, cycle);
            eq(1, table.rowCount(), "one live row per recycling cycle");
            eq(1, table.columnCount(), "one live column per recycling cycle");
            ok(table.removeCell(row, column), "retire old pair");
            eq(0, table.rowCount(), "row directory evacuated");
            eq(0, table.columnCount(), "column directory evacuated");
        }
        ok(table.rowIdCapacity() <= 8, "released row ID retained in bounded array");
        ok(table.columnIdCapacity() <= 8, "released column ID retained in bounded array");
        ok(table.physicalCapacity() <= 8, "released physical slot reused");
    }

    /** Only primitive/source-owner metadata arrays, never one Java node per cell. */
    private static void structural() {
        for (Field field : M3IndexedTable.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            Class<?> type = field.getType();
            ok(type == int[].class || type == byte[].class || type == Object[].class
                    || type == M3LinkedHashMap.class || type == M3LongLongHashMap.class
                    || type == int.class || type == long.class,
                    "no per-cell object/node/entry field: " + field);
        }
    }

    public static void main(String[] args) {
        boolean nativeRun = args.length == 1 && "--native".equals(args[0]);
        if (args.length > 1 || args.length == 1 && !nativeRun) {
            throw new IllegalArgumentException("usage: [--native]");
        }
        if (nativeRun && !M3IndexMaskCompactor.nativeAvailable()) {
            throw new AssertionError("JNI required but unavailable");
        }
        basicAndSlices(nativeRun);
        randomized(nativeRun);
        recyclingPressure();
        structural();
        System.out.println("PASS M3IndexedTableSelfTest checks=" + checks
                + " randomized=6000 native=" + nativeRun);
    }
}
