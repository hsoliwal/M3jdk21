/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Compact primitive row scratch preserves frozen signed-column table contracts
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm FrozenLongTableCompactRowsTest
 */

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import jdk.internal.m3.collections.FrozenLongTable;

/** Independent linked-row/ordered-column oracle for the immutable long table. */
public final class FrozenLongTableCompactRowsTest {
    private static long checks;
    private static void check(boolean accepted, String why) {
        checks++;
        if (!accepted) throw new AssertionError(why);
    }
    private static void rejects(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("wrong exception", failure);
        }
        throw new AssertionError("missing " + kind.getName());
    }

    private static long row(int i, int kind, Random random) {
        return switch (kind) {
            case 0 -> 0L;
            case 1 -> i % 8;
            case 2 -> i % 32;
            case 3 -> i % 33;
            case 4 -> i;
            case 5 -> i % 3 == 0 ? Long.MIN_VALUE :
                    i % 3 == 1 ? Long.MAX_VALUE : 0L;
            case 6 -> random.nextLong();
            default -> i % 64 - 32L;
        };
    }

    private static void compare(int length, int kind, Random random) {
        long[] rows = new long[length];
        long[] columns = new long[length];
        long[] values = new long[length];
        LinkedHashMap<Long, TreeMap<Long, Long>> expected = new LinkedHashMap<>();
        for (int i = 0; i < length; i++) {
            long key = row(i, kind, random);
            long column = (i & 1) == 0 ? -1L - i : 1L + i;
            long value = i % 17 == 0 ? Long.MIN_VALUE :
                    i % 19 == 0 ? Long.MAX_VALUE : random.nextLong();
            rows[i] = key;
            columns[i] = column;
            values[i] = value;
            expected.computeIfAbsent(key, unused -> new TreeMap<>()).put(column, value);
        }
        long[] keptRows = rows.clone(), keptColumns = columns.clone(),
                keptValues = values.clone();
        FrozenLongTable owner = FrozenLongTable.copyOf(rows, columns, values);
        check(Arrays.equals(rows, keptRows), "caller rows changed");
        check(Arrays.equals(columns, keptColumns), "caller columns changed");
        check(Arrays.equals(values, keptValues), "caller values changed");
        check(owner.size() == length, "cell count");
        check(owner.rowCount() == expected.size(), "distinct row count");
        int rowOrdinal = 0;
        int cellOrdinal = 0;
        for (Map.Entry<Long, TreeMap<Long, Long>> row : expected.entrySet()) {
            long key = row.getKey();
            check(owner.rowAt(rowOrdinal) == key, "row encounter order");
            check(owner.rowOffsetAt(rowOrdinal) == cellOrdinal, "row prefix");
            for (Map.Entry<Long, Long> cell : row.getValue().entrySet()) {
                long column = cell.getKey(), value = cell.getValue();
                check(owner.columnAt(cellOrdinal) == column, "signed column order");
                check(owner.valueAt(cellOrdinal) == value, "value projection");
                check(owner.contains(key, column), "coordinate membership");
                check(owner.getOrDefault(key, column, 5L) == value, "lookup");
                cellOrdinal++;
            }
            rowOrdinal++;
        }
        check(cellOrdinal == owner.size(), "packed cell count");
        check(owner.rowOffsetAt(owner.rowCount()) == owner.size(), "terminal offset");
        for (long key : new long[] {Long.MIN_VALUE, Long.MAX_VALUE, -1L, 0L, 1L}) {
            long absentColumn = Long.MIN_VALUE + 13;
            check(!owner.contains(key, absentColumn), "absent coordinate");
            check(owner.getOrDefault(key, absentColumn, 7L) == 7L, "fallback");
        }
        if (length != 0) {
            rows[0] ^= 0x55112233L;
            columns[0] ^= 0x55112233L;
            values[0] ^= 0x55112233L;
            check(owner.rowAt(0) == expected.keySet().iterator().next(), "detached input");
        }
        rejects(IndexOutOfBoundsException.class, () -> owner.rowAt(-1));
        rejects(IndexOutOfBoundsException.class, () -> owner.rowOffsetAt(owner.rowCount() + 1));
        rejects(IndexOutOfBoundsException.class, () -> owner.columnAt(owner.size()));
        rejects(IndexOutOfBoundsException.class, () -> owner.valueAt(owner.size()));
        rejects(UnsupportedOperationException.class, () -> owner.put(1, 2, 3));
        rejects(UnsupportedOperationException.class, () -> owner.remove(1, 2));
    }

    public static void main(String[] arguments) {
        rejects(NullPointerException.class, () -> FrozenLongTable.copyOf(null, new long[0], new long[0]));
        rejects(NullPointerException.class, () -> FrozenLongTable.copyOf(new long[0], null, new long[0]));
        rejects(NullPointerException.class, () -> FrozenLongTable.copyOf(new long[0], new long[0], null));
        rejects(IllegalArgumentException.class,
                () -> FrozenLongTable.copyOf(new long[2], new long[1], new long[2]));
        rejects(IllegalArgumentException.class,
                () -> FrozenLongTable.copyOf(new long[] {1, 1}, new long[] {2, 2}, new long[] {3, 4}));
        check(new FrozenLongTable().rowOffsetAt(0) == 0, "empty terminal");
        for (int length : new int[] {0, 1, 2, 31, 32, 33, 511, 512, 513, 4095, 4096, 65536}) {
            Random random = new Random(0xD20F20261116L ^ length);
            for (int kind = 0; kind < 8; kind++) compare(length, kind, random);
        }
        Random random = new Random(0x125020261116L);
        for (int trial = 0; trial < 270; trial++) {
            compare(random.nextInt(1200), trial % 8, random);
        }
        System.out.println("PASS FrozenLongTableCompactRowsTest checks=" + checks);
    }
}
