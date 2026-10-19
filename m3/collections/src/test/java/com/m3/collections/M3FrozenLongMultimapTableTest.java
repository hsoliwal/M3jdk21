/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * LS3 self-differentials for {@link M3FrozenLongMultimap} (vs a {@code TreeMap<Long, List<Long>>} reduction) and
 * {@link M3FrozenLongTable} (vs a {@code TreeMap<Long, TreeMap<Long, Long>>} reduction), >= 40,000 operations each;
 * the provider claims (Guava ImmutableListMultimap, ImmutableTable) are qualified separately in the interop test.
 * Field-shape and steady-state zero-allocation proofs for both CSR owners.
 */
final class M3FrozenLongMultimapTableTest {
    private static final long MISSING = 0x5EED_0000_0000_0003L;

    @Test
    void multimapDifferentialAgainstListReduction() {
        SplittableRandom random = new SplittableRandom(0x3_1001L);
        long operations = 0;
        for (int round = 0; round < 1200; round++) {
            int n = round % 15 == 0 ? 0 : random.nextInt(1, 50);
            long[] keys = new long[n], values = new long[n];
            TreeMap<Long, List<Long>> oracle = new TreeMap<>();
            for (int i = 0; i < n; i++) {
                keys[i] = random.nextInt(4) == 0 ? random.nextLong() : random.nextLong(-8, 9);
                values[i] = random.nextLong(-5, 6);
                oracle.computeIfAbsent(keys[i], k -> new ArrayList<>()).add(values[i]);
            }
            M3FrozenLongMultimap map = M3FrozenLongMultimap.copyOf(keys, values);
            assertEquals(n, map.size());
            assertEquals(oracle.size(), map.keyCount());
            assertEquals(oracle.toString(), map.toString());
            int hash = 0;
            for (Map.Entry<Long, List<Long>> e : oracle.entrySet()) hash += e.getKey().hashCode() ^ e.getValue().hashCode();
            assertEquals(hash, map.hashCode());
            operations += 4;
            List<Long> orderedKeys = new ArrayList<>(oracle.keySet());
            for (int step = 0; step < 30; step++) {
                long probe = random.nextInt(3) == 0 && n > 0 ? keys[random.nextInt(n)] : random.nextLong(-10, 11);
                long value = random.nextLong(-6, 7);
                List<Long> run = oracle.getOrDefault(probe, List.of());
                assertEquals(oracle.containsKey(probe), map.containsKey(probe));
                assertEquals(run.contains(value), map.containsEntry(probe, value));
                assertEquals(run.size(), map.valueCount(probe));
                assertArrayEquals(run.stream().mapToLong(Long::longValue).toArray(), map.get(probe));
                operations += 4;
            }
            int offset = 0;
            for (int k = 0; k < orderedKeys.size(); k++) {
                assertEquals((long) orderedKeys.get(k), map.keyAt(k));
                assertEquals(offset, map.valueOffsetAt(k));
                for (long v : oracle.get(orderedKeys.get(k))) assertEquals(v, map.valueAt(offset++));
                operations += 2;
            }
            assertEquals(n, map.valueOffsetAt(orderedKeys.size()));
            assertThrows(IndexOutOfBoundsException.class, () -> map.keyAt(orderedKeys.size()));
            assertThrows(IndexOutOfBoundsException.class, () -> map.valueAt(n));
            assertThrows(UnsupportedOperationException.class, () -> map.put(1, 1));
            assertThrows(UnsupportedOperationException.class, () -> map.removeAll(1));
            assertEquals(map, M3FrozenLongMultimap.copyOf(keys.clone(), values.clone()));
            operations += 5;
        }
        assertTrue(operations >= 40_000, "operations " + operations);
    }

    @Test
    void tableDifferentialAgainstNestedReduction() {
        SplittableRandom random = new SplittableRandom(0x3_7AB1EL);
        long operations = 0;
        for (int round = 0; round < 1200; round++) {
            int n = round % 15 == 0 ? 0 : random.nextInt(1, 40);
            long[] rows = new long[n], columns = new long[n], values = new long[n];
            TreeMap<Long, TreeMap<Long, Long>> oracle = new TreeMap<>();
            boolean duplicate = false;
            for (int i = 0; i < n; i++) {
                rows[i] = random.nextLong(-6, 7);
                columns[i] = random.nextInt(5) == 0 ? random.nextLong() : random.nextLong(-300, 301);
                values[i] = random.nextLong(-50, 51);
                duplicate |= oracle.computeIfAbsent(rows[i], r -> new TreeMap<>()).put(columns[i], values[i]) != null;
            }
            if (duplicate) {
                assertThrows(IllegalArgumentException.class, () -> M3FrozenLongTable.copyOf(rows, columns, values));
                operations++;
                continue;
            }
            M3FrozenLongTable table = M3FrozenLongTable.copyOf(rows, columns, values);
            assertEquals(n, table.size());
            assertEquals(oracle.size(), table.rowCount());
            assertEquals(oracle.toString(), table.toString());
            int hash = 0;
            for (Map.Entry<Long, TreeMap<Long, Long>> row : oracle.entrySet()) {
                for (Map.Entry<Long, Long> cell : row.getValue().entrySet()) {
                    hash += java.util.Arrays.hashCode(new Object[] {row.getKey(), cell.getKey(), cell.getValue()});
                }
            }
            assertEquals(hash, table.hashCode());
            operations += 4;
            for (int step = 0; step < 30; step++) {
                long row = random.nextLong(-7, 8);
                long column = random.nextInt(3) == 0 && n > 0 ? columns[random.nextInt(n)] : random.nextLong(-301, 302);
                TreeMap<Long, Long> cells = oracle.get(row);
                Long expected = cells == null ? null : cells.get(column);
                assertEquals(expected != null, table.contains(row, column));
                assertEquals(expected == null ? MISSING : expected, table.getOrDefault(row, column, MISSING));
                operations += 2;
            }
            int cell = 0, r = 0;
            for (Map.Entry<Long, TreeMap<Long, Long>> row : oracle.entrySet()) {
                assertEquals((long) row.getKey(), table.rowAt(r));
                assertEquals(cell, table.rowOffsetAt(r++));
                for (Map.Entry<Long, Long> c : row.getValue().entrySet()) {
                    assertEquals((long) c.getKey(), table.columnAt(cell));
                    assertEquals((long) c.getValue(), table.valueAt(cell++));
                    operations += 2;
                }
            }
            assertEquals(n, table.rowOffsetAt(r));
            assertThrows(IndexOutOfBoundsException.class, () -> table.columnAt(n));
            assertThrows(UnsupportedOperationException.class, () -> table.put(1, 1, 1));
            assertThrows(UnsupportedOperationException.class, () -> table.remove(1, 1));
            assertEquals(table, M3FrozenLongTable.copyOf(rows.clone(), columns.clone(), values.clone()));
            operations += 4;
        }
        assertTrue(operations >= 40_000, "operations " + operations);
    }

    @Test
    void argumentsEmptyAndShapes() {
        assertThrows(IllegalArgumentException.class, () -> M3FrozenLongMultimap.copyOf(new long[1], new long[0]));
        assertThrows(IllegalArgumentException.class, () -> M3FrozenLongTable.copyOf(new long[1], new long[1], new long[0]));
        assertThrows(NullPointerException.class, () -> M3FrozenLongMultimap.copyOf(null, new long[0]));
        assertThrows(NullPointerException.class, () -> M3FrozenLongTable.copyOf(new long[0], null, new long[0]));
        assertSame(M3FrozenLongMultimap.copyOf(new long[0], new long[0]), M3FrozenLongMultimap.copyOf(new long[0], new long[0]));
        assertEquals(new M3FrozenLongTable(), M3FrozenLongTable.copyOf(new long[0], new long[0], new long[0]));
        assertEquals("{}", new M3FrozenLongMultimap().toString());
        assertEquals((int) M3FrozenLongTable.STATE_FROZEN, new M3FrozenLongTable().stateWord());
        assertEquals((int) M3FrozenLongMultimap.STATE_FROZEN, new M3FrozenLongMultimap().stateWord());
        assertEquals(Map.of("keyLane", long[].class, "valueOffsetLane", int[].class, "valueLane", long[].class,
                "stateMaskWords", long[].class, "size", int.class), fields(M3FrozenLongMultimap.class));
        assertEquals(Map.of("rowLane", long[].class, "rowOffsetLane", int[].class, "columnLane", long[].class,
                "valueLane", long[].class, "stateMaskWords", long[].class, "size", int.class), fields(M3FrozenLongTable.class));
    }

    @Test
    void steadyStateQueriesAllocateNothing() {
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) return;
        bean.setThreadAllocatedMemoryEnabled(true);
        long[] a = new long[1024], b = new long[1024], c = new long[1024];
        for (int i = 0; i < a.length; i++) { a[i] = i % 64; b[i] = i; c[i] = i * 3L; }
        M3FrozenLongMultimap map = M3FrozenLongMultimap.copyOf(a, b);
        M3FrozenLongTable table = M3FrozenLongTable.copyOf(a, b, c);
        long[] sink = new long[1];
        M3LongLongConsumer visit = (k, v) -> sink[0] += k ^ v;
        Runnable work = () -> {
            for (int i = 0; i < 512; i++) {
                sink[0] += map.valueCount(i % 70) + (map.containsEntry(i % 64, i) ? 1 : 0) + map.valueAt(i);
                sink[0] += table.getOrDefault(i % 64, i, -1) + (table.contains(i % 64, i + 1) ? 1 : 0) + table.columnAt(i);
            }
            map.forEach(visit);
        };
        for (int i = 0; i < 2_000; i++) work.run();
        long id = Thread.currentThread().threadId();
        long best = Long.MAX_VALUE;
        for (int trial = 0; trial < 3; trial++) {
            long before = bean.getThreadAllocatedBytes(id);
            for (int i = 0; i < 2_000; i++) work.run();
            best = Math.min(best, bean.getThreadAllocatedBytes(id) - before);
        }
        assertEquals(0L, best, "steady-state bytes allocated by 2000 x (3072 queries + forEach over 1024 pairs)");
        assertTrue(sink[0] != Long.MIN_VALUE);
    }

    private static Map<String, Class<?>> fields(Class<?> type) {
        Map<String, Class<?>> fields = new TreeMap<>();
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) fields.put(field.getName(), field.getType());
        }
        assertEquals(0, type.getDeclaredClasses().length);
        return fields;
    }
}
