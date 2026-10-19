/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import com.m3.collections.M3FrozenLongMultimap;
import com.m3.collections.M3FrozenLongTable;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/**
 * LS3 provider differentials, each against its own Guava oracle: {@link M3FrozenLongMultimap} vs
 * {@code ImmutableListMultimap} (pairs, per-key value order, containsEntry, key count, hash; key order NOT claimed)
 * and {@link M3FrozenLongTable} vs {@code ImmutableTable} built with natural row/column ordering (cells, get,
 * contains, cell order, hash, rowMap rendering, duplicate refusal). At least 40,000 operations per provider owner.
 */
final class M3FrozenLongMultimapTableProviderTest {
    private static final Long MISSING = 0x5EED_0000_0000_0004L;

    @Test
    void multimapAgainstGuavaImmutableListMultimap() {
        SplittableRandom random = new SplittableRandom(0x6A7A_3001L);
        long operations = 0;
        for (int round = 0; round < 1000; round++) {
            int n = round % 12 == 0 ? 0 : random.nextInt(1, 45);
            long[] keys = new long[n], values = new long[n];
            ImmutableListMultimap.Builder<Long, Long> builder = ImmutableListMultimap.builder();
            for (int i = 0; i < n; i++) {
                keys[i] = random.nextLong(-7, 8);
                values[i] = random.nextInt(4) == 0 ? random.nextLong() : random.nextLong(-4, 5);
                builder.put(keys[i], values[i]);
            }
            ImmutableListMultimap<Long, Long> guava = builder.build();
            M3FrozenLongMultimap map = M3FrozenLongMultimap.copyOf(keys, values);
            assertEquals(guava.size(), map.size());
            assertEquals(guava.keySet().size(), map.keyCount());
            assertEquals(guava.hashCode(), map.hashCode());
            operations += 3;
            for (int step = 0; step < 32; step++) {
                long key = random.nextInt(3) == 0 && n > 0 ? keys[random.nextInt(n)] : random.nextLong(-9, 10);
                long value = random.nextInt(3) == 0 && n > 0 ? values[random.nextInt(n)] : random.nextLong(-5, 6);
                assertEquals(guava.containsKey(key), map.containsKey(key));
                assertEquals(guava.containsEntry(key, value), map.containsEntry(key, value));
                assertEquals(guava.get(key).size(), map.valueCount(key));
                assertArrayEquals(guava.get(key).stream().mapToLong(Long::longValue).toArray(), map.get(key));
                operations += 4;
            }
            Multimap<Long, Long> asMultimap = guava;
            assertThrows(UnsupportedOperationException.class, () -> asMultimap.put(1L, 1L));
            assertThrows(UnsupportedOperationException.class, () -> map.put(1, 1));
            assertThrows(UnsupportedOperationException.class, () -> asMultimap.removeAll(1L));
            assertThrows(UnsupportedOperationException.class, () -> map.removeAll(1));
            operations += 4;
        }
        assertTrue(operations >= 40_000, "operations " + operations);
    }

    @Test
    void tableAgainstGuavaImmutableTableNaturalOrder() {
        SplittableRandom random = new SplittableRandom(0x6A7A_7AB1EL);
        long operations = 0;
        for (int round = 0; round < 1000; round++) {
            int n = round % 12 == 0 ? 0 : random.nextInt(1, 40);
            long[] rows = new long[n], columns = new long[n], values = new long[n];
            ImmutableTable.Builder<Long, Long, Long> builder = ImmutableTable.<Long, Long, Long>builder()
                    .orderRowsBy(Ordering.natural()).orderColumnsBy(Ordering.natural());
            for (int i = 0; i < n; i++) {
                rows[i] = random.nextLong(-5, 6);
                columns[i] = random.nextInt(6) == 0 ? random.nextLong() : random.nextLong(-300, 301);
                values[i] = random.nextLong(-99, 100);
                builder.put(rows[i], columns[i], values[i]);
            }
            ImmutableTable<Long, Long, Long> guava;
            try {
                guava = builder.buildOrThrow();
            } catch (IllegalArgumentException duplicate) {
                assertThrows(IllegalArgumentException.class, () -> M3FrozenLongTable.copyOf(rows, columns, values));
                operations += 2;
                continue;
            }
            M3FrozenLongTable table = M3FrozenLongTable.copyOf(rows, columns, values);
            assertEquals(guava.size(), table.size());
            assertEquals(guava.rowKeySet().size(), table.rowCount());
            assertEquals(guava.hashCode(), table.hashCode());
            assertEquals(guava.rowMap().toString(), table.toString());
            int cell = 0;
            for (Table.Cell<Long, Long, Long> expected : guava.cellSet()) {
                assertEquals((long) expected.getColumnKey(), table.columnAt(cell));
                assertEquals((long) expected.getValue(), table.valueAt(cell++));
                operations += 2;
            }
            operations += 4;
            for (int step = 0; step < 30; step++) {
                long row = random.nextLong(-6, 7);
                long column = random.nextInt(3) == 0 && n > 0 ? columns[random.nextInt(n)] : random.nextLong(-301, 302);
                assertEquals(guava.contains(row, column), table.contains(row, column));
                Long value = guava.get(row, column);
                assertEquals(value == null ? MISSING : value, table.getOrDefault(row, column, MISSING));
                operations += 2;
            }
            Table<Long, Long, Long> asTable = guava;
            assertThrows(UnsupportedOperationException.class, () -> asTable.put(1L, 1L, 1L));
            assertThrows(UnsupportedOperationException.class, () -> table.put(1, 1, 1));
            operations += 2;
        }
        assertTrue(operations >= 40_000, "operations " + operations);
    }
}
