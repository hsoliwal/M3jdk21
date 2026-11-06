/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Independent row-model and tiny-memory arithmetic regressions for the original lane owner. */
public final class M3ColumnarGrowthContract {
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    private static final int[] DEFAULTS = {-7, 0};
    private static final int COLUMNS = 3;
    private static long checks;

    private M3ColumnarGrowthContract() { }

    public static void main(String[] args) {
        String mode = args.length == 0 ? "all" : args[0];
        long passed = switch (mode) {
            case "ordinary" -> runOrdinary();
            case "boundary" -> runBoundary();
            case "all" -> run();
            default -> throw new IllegalArgumentException("mode must be all, ordinary or boundary");
        };
        System.out.println("M3_COLUMNAR_GROWTH_PASS mode=" + mode + " checks=" + passed);
    }

    public static long run() {
        long ordinary = runOrdinary();
        return ordinary + runBoundary();
    }

    /** Baseline and candidate must both satisfy the same ordinary row-model oracle. */
    public static long runOrdinary() {
        checks = 0;
        for (long seed : new long[] {0x6a2026L, 0x10_29_2026L, 0x5eedL}) randomizedRows(seed);
        noOpAndInvalidArguments();
        return checks;
    }

    /** Fails on the exact old implementation without allocating a giant backing array. */
    public static long runBoundary() {
        checks = 0;
        for (int index : new int[] {0, 1, MAX_ARRAY_SIZE / 2, MAX_ARRAY_SIZE}) {
            for (int count : new int[] {1, 8, 9, Integer.MAX_VALUE}) {
                M3ColumnarLanes virtual = new M3ColumnarLanes(2, DEFAULTS, COLUMNS);
                virtual.setLength(MAX_ARRAY_SIZE);
                equal(MAX_ARRAY_SIZE, virtual.length(), "virtual large initial length");
                expect(OutOfMemoryError.class, () -> virtual.insertRows(index, count));
                equal(MAX_ARRAY_SIZE, virtual.length(), "failed insertion preserves large length");
                equal(COLUMNS, virtual.columns(), "failed insertion preserves columns");
                same(null, virtual.get(0, 0, MAX_ARRAY_SIZE - 1), "virtual object default remains");
                equal(-7, virtual.getInt(0, 0, MAX_ARRAY_SIZE - 1), "virtual int default remains");
                check(!virtual.hasState(MAX_ARRAY_SIZE - 1), "virtual lanes remain default-only");
                virtual.removeRows(0, MAX_ARRAY_SIZE);
                equal(0, virtual.length(), "rejected insertion permits valid retry");
                virtual.setLength(2);
                virtual.insertRows(1, 1);
                equal(3, virtual.length(), "retry on default-only lane");
                same(null, virtual.get(0, 0, 1), "retry preserves lazy storage");
            }
        }
        // This second witness is independent of insertion: index + count wraps negative,
        // so the old check silently subtracts rows from a default-only ten-row owner.
        int[][] invalidRanges = {
                {Integer.MAX_VALUE, 1}, {Integer.MAX_VALUE, Integer.MAX_VALUE},
                {10, Integer.MAX_VALUE}, {9, Integer.MAX_VALUE}, {11, 0}, {9, 2}
        };
        for (int[] range : invalidRanges) {
            M3ColumnarLanes virtual = new M3ColumnarLanes(0, new int[0], 1);
            virtual.setLength(10);
            expect(IndexOutOfBoundsException.class, () -> virtual.removeRows(range[0], range[1]));
            equal(10, virtual.length(), "invalid removal preserves length");
            equal(1, virtual.columns(), "invalid removal preserves columns");
            virtual.removeRows(9, 1);
            equal(9, virtual.length(), "invalid removal permits valid retry");
        }
        // End-index arithmetic can be rejected before touching already materialized lanes too.
        M3ColumnarLanes populated = new M3ColumnarLanes(1, new int[] {-7}, 1);
        populated.setLength(3);
        populated.set(0, 0, 1, "preserved");
        populated.setInt(0, 0, 2, 123);
        expect(OutOfMemoryError.class, () -> populated.insertRows(0, Integer.MAX_VALUE));
        equal(3, populated.length(), "populated insertion length");
        same("preserved", populated.get(0, 0, 1), "populated object unchanged");
        equal(123, populated.getInt(0, 0, 2), "populated int unchanged");
        expect(IndexOutOfBoundsException.class, () -> populated.removeRows(Integer.MAX_VALUE, 1));
        same("preserved", populated.get(0, 0, 1), "populated invalid removal unchanged");
        equal(3, populated.length(), "populated invalid removal length");
        return checks;
    }

    private static void randomizedRows(long seed) {
        M3ColumnarLanes actual = new M3ColumnarLanes(2, DEFAULTS, COLUMNS);
        List<Row> model = new ArrayList<>();
        Random random = new Random(seed);
        for (int step = 0; step < 4_000; step++) {
            int action = random.nextInt(9);
            switch (action) {
                case 0 -> {
                    int at = random.nextInt(model.size() + 1);
                    int count = model.size() > 80 ? 0 : random.nextInt(7);
                    actual.insertRows(at, count);
                    for (int i = 0; i < count; i++) model.add(at, new Row());
                }
                case 1 -> {
                    int at = random.nextInt(model.size() + 1);
                    int count = random.nextInt(model.size() - at + 1);
                    actual.removeRows(at, count);
                    model.subList(at, at + count).clear();
                }
                case 2 -> {
                    int length = random.nextInt(81);
                    actual.setLength(length);
                    if (length < model.size()) model.subList(length, model.size()).clear();
                    while (model.size() < length) model.add(new Row());
                }
                case 3 -> {
                    if (!model.isEmpty()) {
                        int row = random.nextInt(model.size()), field = random.nextInt(2);
                        int column = random.nextInt(COLUMNS);
                        String value = random.nextInt(4) == 0 ? null : "s" + seed + ":" + step;
                        actual.set(field, column, row, value);
                        model.get(row).objects[field][column] = value;
                    }
                }
                case 4 -> {
                    if (!model.isEmpty()) {
                        int row = random.nextInt(model.size()), field = random.nextInt(2);
                        int column = random.nextInt(COLUMNS);
                        int value = random.nextInt(3) == 0 ? DEFAULTS[field] : random.nextInt();
                        actual.setInt(field, column, row, value);
                        model.get(row).ints[field][column] = value;
                    }
                }
                case 5 -> {
                    if (!model.isEmpty()) {
                        int row = random.nextInt(model.size());
                        actual.clearRow(row);
                        model.set(row, new Row());
                    }
                }
                case 6 -> {
                    int field = random.nextInt(2), column = random.nextInt(COLUMNS);
                    actual.dropLane(field, column);
                    for (Row row : model) row.objects[field][column] = null;
                }
                case 7 -> {
                    int field = random.nextInt(2), column = random.nextInt(COLUMNS);
                    actual.dropIntLane(field, column);
                    for (Row row : model) row.ints[field][column] = DEFAULTS[field];
                }
                default -> {
                    int column = random.nextInt(COLUMNS);
                    actual.clearColumn(column);
                    for (Row row : model) {
                        for (int field = 0; field < 2; field++) {
                            row.objects[field][column] = null;
                            row.ints[field][column] = DEFAULTS[field];
                        }
                    }
                }
            }
            equal(model.size(), actual.length(), "row-model length");
            if ((step & 15) == 0) compareRows(actual, model);
        }
        compareRows(actual, model);
    }

    private static void noOpAndInvalidArguments() {
        M3ColumnarLanes lanes = new M3ColumnarLanes(1, new int[] {-7}, 1);
        lanes.setLength(3);
        lanes.set(0, 0, 1, "kept");
        lanes.setInt(0, 0, 1, 91);
        lanes.insertRows(3, 0);
        lanes.removeRows(3, 0);
        equal(3, lanes.length(), "end zero-count operations");
        same("kept", lanes.get(0, 0, 1), "zero count object unchanged");
        equal(91, lanes.getInt(0, 0, 1), "zero count int unchanged");
        expect(IndexOutOfBoundsException.class, () -> lanes.insertRows(-1, 0));
        expect(IndexOutOfBoundsException.class, () -> lanes.insertRows(4, 0));
        expect(IndexOutOfBoundsException.class, () -> lanes.insertRows(0, -1));
        expect(IndexOutOfBoundsException.class, () -> lanes.removeRows(-1, 0));
        expect(IndexOutOfBoundsException.class, () -> lanes.removeRows(4, 0));
        expect(IndexOutOfBoundsException.class, () -> lanes.removeRows(0, -1));
        expect(IndexOutOfBoundsException.class, () -> lanes.removeRows(2, 2));
        expect(IllegalArgumentException.class, () -> lanes.setLength(-1));
        equal(3, lanes.length(), "invalid argument length unchanged");
        same("kept", lanes.get(0, 0, 1), "invalid argument object unchanged");
        equal(91, lanes.getInt(0, 0, 1), "invalid argument int unchanged");
    }

    private static void compareRows(M3ColumnarLanes actual, List<Row> model) {
        equal(COLUMNS, actual.columns(), "row-model columns");
        for (int row = 0; row < model.size(); row++) {
            Row expected = model.get(row);
            boolean state = false;
            for (int field = 0; field < 2; field++) {
                for (int column = 0; column < COLUMNS; column++) {
                    same(expected.objects[field][column], actual.get(field, column, row), "row-model object identity");
                    equal(expected.ints[field][column], actual.getInt(field, column, row), "row-model int value");
                    state |= expected.objects[field][column] != null || expected.ints[field][column] != DEFAULTS[field];
                }
            }
            check(state == actual.hasState(row), "row-model state flag");
        }
        for (int field = 0; field < 2; field++) {
            for (int column = 0; column < COLUMNS; column++) {
                int maximum = DEFAULTS[field];
                for (Row row : model) maximum = Math.max(maximum, row.ints[field][column]);
                equal(maximum, actual.maxInt(field, column), "row-model maximum");
            }
        }
        check(!actual.hasState(-1) && !actual.hasState(model.size()), "out-of-range state false");
    }

    private static final class Row {
        private final String[][] objects = new String[2][COLUMNS];
        private final int[][] ints = new int[2][COLUMNS];
        private Row() {
            for (int field = 0; field < 2; field++) Arrays.fill(ints[field], DEFAULTS[field]);
        }
    }

    private static void same(Object expected, Object actual, String label) { check(expected == actual, label); }
    private static void equal(long expected, long actual, String label) { check(expected == actual, label + ": " + expected + " != " + actual); }
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName() + ", operation returned normally");
    }
}
