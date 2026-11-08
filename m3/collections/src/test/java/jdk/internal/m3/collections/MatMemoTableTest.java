/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.mat.MatCodec;
import jdk.internal.m3.collections.mat.MatLimits;
import jdk.internal.m3.collections.mat.MatMemoTable;
import jdk.internal.m3.collections.mat.MatPool;
import jdk.internal.m3.collections.mat.MatRef;
import jdk.internal.m3.collections.mat.MatSink;
import jdk.internal.m3.collections.mat.MatSource;

/** Frozen observable contracts and retained-storage checks for the existing MAT memo owner. */
final class MatMemoTableTest {
    private static final MatCodec<Integer> INTS = new MatCodec<>() {
        @Override public String typeId() { return "memo-contract-int/1"; }
        @Override public Class<Integer> javaType() { return Integer.class; }
        @Override public void encode(Integer value, MatSink sink) { sink.i32(value); }
        @Override public Integer decode(MatSource source) { return source.i32(); }
    };

    private static MatPool<Integer> pool(int size) {
        MatPool.Builder<Integer> builder = MatPool.builder(INTS);
        for (int value = 0; value < size; value++) builder.add(value);
        return builder.build();
    }

    private static MatLimits limits(int entries, int arity) {
        return new MatLimits(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MAX_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE, entries, arity,
                Integer.MAX_VALUE);
    }

    @Test void exactPoolIdentityTupleOrderAndFreshHandles() {
        MatPool<Integer> first = pool(40);
        MatPool<Integer> second = pool(40);
        MatMemoTable<Integer> table = new MatMemoTable<>("  operation/v7  ", first,
                MatLimits.DEFAULT);
        assertEquals("operation/v7", table.operationId());
        assertSame(first, table.resultPool());
        assertEquals(0, table.getOrCompute(() -> 0, first.ref(1), first.ref(2)));
        assertEquals(OptionalInt.of(0), table.find(first.ref(1), first.ref(2)));
        assertEquals(OptionalInt.empty(), table.find(second.ref(1), first.ref(2)));
        assertEquals(OptionalInt.empty(), table.find(first.ref(2), first.ref(1)));
        assertEquals(OptionalInt.empty(), table.find(first.ref(1)));
        assertEquals(39, table.getOrCompute(() -> 39, second.ref(1), first.ref(2)));
        assertEquals(new MatMemoTable.Stats(2, 1, 2, 0), table.stats());
    }

    @Test void fullHashCollisionsStillCompareEveryArgument() {
        MatPool<Integer> pool = pool(100);
        MatMemoTable<Integer> table = new MatMemoTable<>("collision/v1", pool,
                MatLimits.DEFAULT);
        MatRef<?>[] left = {pool.ref(0), pool.ref(31)};
        MatRef<?>[] right = {pool.ref(1), pool.ref(0)};
        assertEquals(Arrays.hashCode(left), Arrays.hashCode(right));
        assertFalse(Arrays.equals(left, right));
        assertEquals(7, table.getOrCompute(() -> 7, left));
        assertEquals(19, table.getOrCompute(() -> 19, right));
        assertEquals(OptionalInt.of(7), table.find(left));
        assertEquals(OptionalInt.of(19), table.find(right));
        assertEquals(new MatMemoTable.Stats(2, 2, 2, 0), table.stats());
    }

    @Test void snapshotsCallerArrayBeforeRunningSupplier() {
        MatPool<Integer> pool = pool(8);
        MatMemoTable<Integer> table = new MatMemoTable<>("snapshot/v1", pool,
                MatLimits.DEFAULT);
        MatRef<?>[] arguments = {pool.ref(1), pool.ref(2)};
        assertEquals(3, table.getOrCompute(() -> {
            arguments[0] = pool.ref(6);
            arguments[1] = null;
            return 3;
        }, arguments));
        assertEquals(OptionalInt.of(3), table.find(pool.ref(1), pool.ref(2)));
        assertEquals(OptionalInt.empty(), table.find(pool.ref(6), pool.ref(2)));
        assertThrows(NullPointerException.class, () -> table.find(arguments));
    }

    @Test void emptyTupleResultZeroAndClearKeepStatistics() {
        MatPool<Integer> pool = pool(3);
        MatMemoTable<Integer> table = new MatMemoTable<>("empty/v1", pool, limits(1, 0));
        assertEquals(OptionalInt.empty(), table.find());
        assertEquals(0, table.getOrCompute(() -> 0));
        assertEquals(0, table.getOrCompute(() -> { throw new AssertionError("cache hit"); }));
        assertEquals(OptionalInt.of(0), table.find());
        table.clear();
        assertEquals(new MatMemoTable.Stats(0, 2, 1, 0), table.stats());
        assertEquals(OptionalInt.empty(), table.find());
        assertEquals(2, table.getOrCompute(() -> 2));
        assertEquals(new MatMemoTable.Stats(1, 2, 2, 0), table.stats());
    }

    @Test void validationOrderAndRejectAccountingStayUnchanged() {
        MatPool<Integer> pool = pool(4);
        MatMemoTable<Integer> table = new MatMemoTable<>("validation/v1", pool, limits(1, 2));
        assertEquals("computation", assertThrows(NullPointerException.class,
                () -> table.getOrCompute(null, (MatRef<?>[]) null)).getMessage());
        assertEquals("arguments", assertThrows(NullPointerException.class,
                () -> table.find((MatRef<?>[]) null)).getMessage());
        assertEquals("argument", assertThrows(NullPointerException.class,
                () -> table.find((MatRef<?>) null)).getMessage());
        assertEquals("memo arity budget exceeded: 3 > 2", assertThrows(IllegalArgumentException.class,
                () -> table.find(new MatRef<?>[3])).getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> table.getOrCompute(() -> 1, new MatRef<?>[3]));
        assertEquals(new MatMemoTable.Stats(0, 0, 0, 2), table.stats());
        assertEquals(1, table.getOrCompute(() -> 1, pool.ref(0)));
        AtomicInteger invoked = new AtomicInteger();
        assertEquals("memo entry budget exceeded: 2 > 1", assertThrows(IllegalArgumentException.class,
                () -> table.getOrCompute(invoked::incrementAndGet, pool.ref(1))).getMessage());
        assertEquals(0, invoked.get());
        assertEquals(1, table.getOrCompute(() -> 2, pool.ref(0)));
        assertEquals(new MatMemoTable.Stats(1, 1, 1, 2), table.stats());
        assertThrows(NullPointerException.class, () -> new MatMemoTable<>(null, pool, limits(1, 1)));
        assertThrows(IllegalArgumentException.class, () -> new MatMemoTable<>(" \t", pool, limits(1, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> new MatMemoTable<>("a".repeat(257), pool, limits(1, 1)));
        assertThrows(IllegalArgumentException.class, () -> new MatMemoTable.Stats(-1, 0, 0, 0));
    }

    @Test void failedSupplierAndInvalidResultDoNotCacheOrCountAMiss() {
        MatPool<Integer> pool = pool(4);
        MatMemoTable<Integer> table = new MatMemoTable<>("failure/v1", pool, limits(1, 1));
        RuntimeException failure = new RuntimeException("supplier failure");
        assertSame(failure, assertThrows(RuntimeException.class,
                () -> table.getOrCompute(() -> { throw failure; }, pool.ref(0))));
        assertEquals("pool id: -1", assertThrows(IndexOutOfBoundsException.class,
                () -> table.getOrCompute(() -> -1, pool.ref(0))).getMessage());
        assertEquals("pool id: 4", assertThrows(IndexOutOfBoundsException.class,
                () -> table.getOrCompute(() -> 4, pool.ref(0))).getMessage());
        assertEquals(new MatMemoTable.Stats(0, 0, 0, 0), table.stats());
        assertEquals(3, table.getOrCompute(() -> 3, pool.ref(0)));
    }

    @Test void reentrantInsertOverwriteResizeAndClearMatchMapPut() {
        MatPool<Integer> pool = pool(1101);
        MatMemoTable<Integer> table = new MatMemoTable<>("reentrant/v1", pool,
                MatLimits.DEFAULT);
        assertEquals(9, table.getOrCompute(() -> {
            for (int id = 0; id < 1000; id++) table.getOrCompute(() -> 6, pool.ref(id));
            return 9;
        }, pool.ref(0)));
        assertEquals(new MatMemoTable.Stats(1000, 0, 1001, 0), table.stats());
        assertEquals(OptionalInt.of(9), table.find(pool.ref(0)));
        assertEquals(7, table.getOrCompute(() -> {
            table.clear();
            table.getOrCompute(() -> 8, pool.ref(1));
            return 7;
        }, pool.ref(1100)));
        assertEquals(new MatMemoTable.Stats(2, 1, 1003, 0), table.stats());
        assertEquals(OptionalInt.empty(), table.find(pool.ref(0)));
        assertEquals(OptionalInt.of(8), table.find(pool.ref(1)));
        assertEquals(OptionalInt.of(7), table.find(pool.ref(1100)));
        assertThrows(IndexOutOfBoundsException.class, () -> table.getOrCompute(() -> {
            table.clear();
            table.getOrCompute(() -> 3, pool.ref(2));
            return -1;
        }, pool.ref(1000)));
        assertEquals(new MatMemoTable.Stats(1, 3, 1004, 0), table.stats());
        assertEquals(OptionalInt.of(3), table.find(pool.ref(2)));
    }

    @Test void entryBudgetRetainsItsExistingPreSupplierTiming() {
        MatPool<Integer> pool = pool(4);
        MatMemoTable<Integer> table = new MatMemoTable<>("reentrant-budget/v1", pool,
                limits(1, 1));
        assertEquals(3, table.getOrCompute(() -> {
            assertEquals(2, table.getOrCompute(() -> 2, pool.ref(1)));
            return 3;
        }, pool.ref(0)));
        assertEquals(new MatMemoTable.Stats(2, 0, 2, 0), table.stats());
        assertThrows(IllegalArgumentException.class,
                () -> table.getOrCompute(() -> 0, pool.ref(2)));
        assertEquals(OptionalInt.of(3), table.find(pool.ref(0)));
        assertEquals(OptionalInt.of(2), table.find(pool.ref(1)));
    }

    @Test void entryAndArgumentPageBoundariesSurviveGrowthAndReuse() {
        MatPool<Integer> pool = pool(6501);
        MatMemoTable<Integer> table = new MatMemoTable<>("growth/v1", pool,
                MatLimits.DEFAULT);
        for (int id = 0; id < 6500; id++) {
            int result = id % 300;
            assertEquals(result, table.getOrCompute(() -> result, tuple(pool, id)));
        }
        for (int id = 6499; id >= 0; id--) {
            assertEquals(OptionalInt.of(id % 300), table.find(tuple(pool, id)));
        }
        assertEquals(new MatMemoTable.Stats(6500, 6500, 6500, 0), table.stats());
        table.clear();
        for (int id = 0; id < 6500; id += 31) {
            assertEquals(OptionalInt.empty(), table.find(tuple(pool, id)));
            assertEquals(0, table.getOrCompute(() -> 0, tuple(pool, id)));
        }
    }

    private static MatRef<?>[] tuple(MatPool<Integer> pool, int id) {
        int length = id % 23 == 0 ? 256 : 1 + id % 19;
        MatRef<?>[] tuple = new MatRef<?>[length];
        tuple[0] = pool.ref(id);
        for (int index = 1; index < length; index++) tuple[index] = pool.ref((id + index) % pool.size());
        return tuple;
    }

    @Test void deterministicStateMachineMatchesIndependentListKeyOracle() {
        MatPool<Integer> first = pool(300);
        MatPool<Integer> second = pool(300);
        MatMemoTable<Integer> table = new MatMemoTable<>("state-machine/v1", first,
                MatLimits.DEFAULT);
        Map<List<MatRef<?>>, Integer> oracle = new HashMap<>();
        Random random = new Random(0x4d334d454d4fL);
        long hits = 0, misses = 0;
        for (int step = 0; step < 20000; step++) {
            if (step > 0 && step % 4093 == 0) {
                table.clear();
                oracle.clear();
            }
            MatRef<?>[] tuple = new MatRef<?>[random.nextInt(5)];
            for (int index = 0; index < tuple.length; index++) {
                tuple[index] = (random.nextBoolean() ? first : second).ref(random.nextInt(20));
            }
            List<MatRef<?>> key = List.copyOf(Arrays.asList(tuple));
            Integer existing = oracle.get(key);
            if (random.nextInt(3) == 0) {
                assertEquals(existing == null ? OptionalInt.empty() : OptionalInt.of(existing),
                        table.find(tuple));
                if (existing != null) hits++;
            } else {
                int proposed = random.nextInt(first.size());
                AtomicInteger invocations = new AtomicInteger();
                int actual = table.getOrCompute(() -> {
                    invocations.incrementAndGet();
                    return proposed;
                }, tuple);
                if (existing == null) {
                    oracle.put(key, proposed);
                    assertEquals(proposed, actual);
                    assertEquals(1, invocations.get());
                    misses++;
                } else {
                    assertEquals(existing.intValue(), actual);
                    assertEquals(0, invocations.get());
                    hits++;
                }
            }
            assertEquals(new MatMemoTable.Stats(oracle.size(), hits, misses, 0), table.stats());
        }
    }

    @Test void synchronizedConcurrentMissComputesExactlyOnce() throws Exception {
        MatPool<Integer> pool = pool(40);
        MatMemoTable<Integer> table = new MatMemoTable<>("threads/v1", pool,
                MatLimits.DEFAULT);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger invocations = new AtomicInteger();
        var threads = Executors.newFixedThreadPool(8);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int worker = 0; worker < 8; worker++) results.add(threads.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("start timeout");
                return table.getOrCompute(() -> { invocations.incrementAndGet(); return 31; }, pool.ref(17));
            }));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            for (Future<Integer> result : results) assertEquals(31, result.get(5, TimeUnit.SECONDS).intValue());
            assertEquals(1, invocations.get());
            assertEquals(new MatMemoTable.Stats(1, 7, 1, 0), table.stats());
        } finally {
            start.countDown();
            threads.shutdownNow();
            assertTrue(threads.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test void retainedStateContainsNoEntryWrappersAndClearReleasesPoolLanes() throws Exception {
        MatPool<Integer> pool = pool(1025);
        MatMemoTable<Integer> table = new MatMemoTable<>("storage/v1", pool,
                MatLimits.DEFAULT);
        for (int id = 0; id < 1024; id++) table.getOrCompute(() -> 0, pool.ref(id));
        for (Field field : MatMemoTable.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            assertFalse(Map.class.isAssignableFrom(field.getType()), "retained per-entry map nodes");
            field.setAccessible(true);
            Object value = field.get(table);
            if (field.getType().isArray()) assertPoolOrPrimitiveLeaves(value);
        }
        for (Class<?> nested : MatMemoTable.class.getDeclaredClasses()) {
            assertNotEquals("Key", nested.getSimpleName(), "retained tuple wrapper class");
        }
        table.clear();
        for (Field field : MatMemoTable.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || !field.getType().isArray()) continue;
            field.setAccessible(true);
            assertEquals(0, java.lang.reflect.Array.getLength(field.get(table)), field.getName());
        }
    }

    private static void assertPoolOrPrimitiveLeaves(Object value) {
        if (value == null || value.getClass().getComponentType().isPrimitive()) return;
        for (Object element : (Object[]) value) {
            if (element == null) continue;
            if (element.getClass().isArray()) assertPoolOrPrimitiveLeaves(element);
            else assertInstanceOf(MatPool.class, element, "only pool identity is retained in reference lanes");
        }
    }
}
