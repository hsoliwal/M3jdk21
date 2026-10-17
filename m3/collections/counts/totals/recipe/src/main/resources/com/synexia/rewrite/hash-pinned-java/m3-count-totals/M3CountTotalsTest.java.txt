/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class M3CountTotalsTest {
    @BeforeAll static void loadRealNativeLibrary() {
        String library = System.getProperty("m3.totals.native");
        assertNotNull(library, "native verification is required, never skipped");
        System.load(library);
    }
    @Test void bagTotalsMatchMaterializedAlgebraAndIndependentFrequencyOracle() {
        for (int seed = 0; seed < 32; seed++) {
            Random r = new Random(seed);
            M3LongBag a = new M3LongBag(), b = new M3LongBag();
            Map<Long, Long> x = new HashMap<>(), y = new HashMap<>();
            for (int i = 0; i < 300; i++) {
                long key = i % 31 == 0 ? Long.MIN_VALUE : i % 37 == 0 ? Long.MAX_VALUE : r.nextInt(64);
                long n = r.nextInt(1000);
                M3LongBag bag = (i & 1) == 0 ? a : b;
                Map<Long, Long> ref = (i & 1) == 0 ? x : y;
                bag.setCount(key, n);
                if (n == 0) ref.remove(key); else ref.put(key, n);
                if (i % 10 == 0) checkBags(a, b, x, y);
            }
            checkBags(a, b, x, y);
        }
    }
    private static void checkBags(M3LongBag a, M3LongBag b, Map<Long, Long> x, Map<Long, Long> y) {
        long overlap = 0;
        for (var e : x.entrySet()) overlap += Math.min(e.getValue(), y.getOrDefault(e.getKey(), 0L));
        long ax = x.values().stream().mapToLong(Long::longValue).sum();
        long by = y.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(overlap, M3CountTotals.intersection(a, b));
        assertEquals(ax - overlap + by, M3CountTotals.union(a, b));
        assertEquals(ax + by, M3CountTotals.sum(a, b));
        assertEquals(ax - overlap, M3CountTotals.difference(a, b));
        assertEquals(a.intersection(b).size(), M3CountTotals.intersection(a, b));
        assertEquals(a.union(b).size(), M3CountTotals.union(a, b));
        assertEquals(a.sum(b).size(), M3CountTotals.sum(a, b));
        assertEquals(a.difference(b).size(), M3CountTotals.difference(a, b));
        assertEquals(ax, a.size()); assertEquals(by, b.size());
        x.forEach((k, n) -> assertEquals(n.longValue(), a.count(k)));
        y.forEach((k, n) -> assertEquals(n.longValue(), b.count(k)));
    }
    @Test void bagSelfEmptyAndSparseCapacityCases() {
        M3LongBag sparse = new M3LongBag(100_000), small = new M3LongBag();
        sparse.add(7, 10); small.add(7, 4); small.add(9, 2);
        assertEquals(4, M3CountTotals.intersection(sparse, small));
        assertEquals(4, M3CountTotals.intersection(small, sparse));
        assertEquals(12, M3CountTotals.union(sparse, small));
        assertEquals(6, M3CountTotals.difference(sparse, small));
        assertEquals(2, M3CountTotals.difference(small, sparse));
        assertEquals(10, M3CountTotals.intersection(sparse, sparse));
        assertEquals(0, M3CountTotals.difference(sparse, sparse));
        small.clear(); assertEquals(10, M3CountTotals.union(sparse, small));
        assertEquals(0, M3CountTotals.intersection(sparse, small));
        sparse.trimToSize(); assertEquals(10, M3CountTotals.sum(sparse, small));
    }
    @Test void overlapIsSubtractedBeforeUnionOverflowCheck() {
        M3LongBag a = new M3LongBag(), b = new M3LongBag();
        a.add(Long.MIN_VALUE, Long.MAX_VALUE); b.add(Long.MIN_VALUE, Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, M3CountTotals.union(a, b));
        assertEquals(Long.MAX_VALUE, M3CountTotals.intersection(a, b));
        assertEquals(0, M3CountTotals.difference(a, b));
        assertThrows(ArithmeticException.class, () -> M3CountTotals.sum(a, b));
        b.clear(); b.add(Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> M3CountTotals.union(a, b));
        assertEquals(0, M3CountTotals.intersection(a, b));
        assertEquals(Long.MAX_VALUE, M3CountTotals.difference(a, b));
        assertEquals(Long.MAX_VALUE, a.size()); assertEquals(1, b.size());
    }
    @Test void bagNullsRejectedEvenWhenOtherInputIsEmpty() {
        M3LongBag empty = new M3LongBag();
        assertThrows(NullPointerException.class, () -> M3CountTotals.sum(null, empty));
        assertThrows(NullPointerException.class, () -> M3CountTotals.sum(empty, null));
        assertThrows(NullPointerException.class, () -> M3CountTotals.intersection(null, empty));
        assertThrows(NullPointerException.class, () -> M3CountTotals.intersection(empty, null));
        assertThrows(NullPointerException.class, () -> M3CountTotals.union(null, empty));
        assertThrows(NullPointerException.class, () -> M3CountTotals.union(empty, null));
        assertThrows(NullPointerException.class, () -> M3CountTotals.difference(null, empty));
        assertThrows(NullPointerException.class, () -> M3CountTotals.difference(empty, null));
    }
    @Test void alignedLanesMatchBigIntegerAndExistingJavaAndJni() {
        Random r = new Random(0x4d33544f54414cL);
        for (int length : new int[]{0, 1, 2, 31, 1023, 1024, 1025, 2049, 4097}) {
            for (int seed = 0; seed < 12; seed++) {
                long[] a = new long[length + 7], b = new long[length + 9];
                for (int i = 0; i < a.length; i++) a[i] = r.nextInt(100_000);
                for (int i = 0; i < b.length; i++) b[i] = r.nextInt(100_000);
                for (int op = 0; op <= 3; op++) checkLanes(a, 3, b, 4, length, op);
            }
        }
    }
    private static void checkLanes(long[] a, int ao, long[] b, int bo, int length, int op) {
        long[] beforeA = a.clone(), beforeB = b.clone();
        BigInteger total = BigInteger.ZERO;
        for (int i = 0; i < length; i++) {
            BigInteger x = BigInteger.valueOf(a[ao + i]), y = BigInteger.valueOf(b[bo + i]);
            total = total.add(switch (op) {
                case 0 -> x.add(y); case 1 -> x.max(y); case 2 -> x.min(y);
                default -> x.subtract(y).max(BigInteger.ZERO);
            });
        }
        long expected = total.longValueExact();
        assertEquals(expected, M3CountTotals.combine(a, ao, b, bo, length, op));
        assertEquals(expected, M3CountTotals.combineNative(a, ao, b, bo, length, op));
        assertEquals(expected, Arrays.stream(M3CountLanes.combine(a, ao, b, bo, length, op)).sum());
        assertEquals(expected, Arrays.stream(M3CountLanes.combineNative(a, ao, b, bo, length, op)).sum());
        assertArrayEquals(beforeA, a); assertArrayEquals(beforeB, b);
    }
    @Test void aliasesEmptySlicesAndValuesOutsideSlices() {
        long[] a = {-1, 1, 3, 2, 6, -1};
        for (int op = 0; op < 4; op++) {
            checkLanes(a, 1, a, 2, 3, op);
            checkLanes(a, a.length, a, a.length, 0, op);
            checkLanes(a, 0, a, 0, 0, op);
        }
    }
    @Test void laneBoundsNullsAndOperationsHaveExistingExceptionParity() {
        long[] a = {1, 2};
        fails(NullPointerException.class, null, -1, a, 0, 99, -1);
        fails(NullPointerException.class, a, -1, null, 0, 99, -1);
        for (int[] bounds : new int[][]{{-1,0,1},{0,-1,1},{0,0,-1},{1,0,2},{0,1,2},
                {Integer.MAX_VALUE,0,1},{0,Integer.MAX_VALUE,1},{0,0,Integer.MAX_VALUE},
                {Integer.MIN_VALUE,0,1},{3,0,0}}) {
            fails(IndexOutOfBoundsException.class, a, bounds[0], a, bounds[1], bounds[2], -1);
        }
        fails(IllegalArgumentException.class, a, 0, a, 0, 0, -1);
        fails(IllegalArgumentException.class, a, 0, a, 0, 0, 4);
    }
    @Test void laneIndividualAndAggregateOverflow() {
        fails(ArithmeticException.class, new long[]{Long.MAX_VALUE},0,new long[]{1},0,1,0);
        fails(ArithmeticException.class, new long[]{Long.MAX_VALUE,1},0,new long[]{0,0},0,2,0);
        for (int op=1; op<4; op++) {
            long[] right = op==2 ? new long[]{Long.MAX_VALUE,1} : new long[]{0,0};
            fails(ArithmeticException.class,new long[]{Long.MAX_VALUE,1},0,right,0,2,op);
        }
        for (int op=0; op<4; op++) checkLanes(new long[]{Long.MAX_VALUE},0,new long[]{0},0,1,op);
    }
    @Test void lateChunkFailuresNeverModifyInputs() {
        for (int op=0; op<4; op++) {
            long[] a = new long[2051], b = new long[2051];
            a[2048] = -1;
            fails(IllegalArgumentException.class,a,0,b,0,a.length,op);
            a[2048] = Long.MAX_VALUE; a[2049] = 1;
            if (op==2) { b[2048]=Long.MAX_VALUE; b[2049]=1; }
            fails(ArithmeticException.class,a,0,b,0,a.length,op);
        }
    }
    @Test void perLaneValidationOrderIsNotReorderedByReduction() {
        fails(ArithmeticException.class,new long[]{Long.MAX_VALUE,-1},0,new long[]{1,0},0,2,0);
        fails(IllegalArgumentException.class,new long[]{-1,Long.MAX_VALUE},0,new long[]{0,1},0,2,0);
        fails(IllegalArgumentException.class,new long[]{Long.MAX_VALUE},0,new long[]{-1},0,1,0);
    }
    private static void fails(Class<? extends Throwable> type,long[] a,int ao,long[] b,int bo,int len,int op) {
        long[] ac=a==null?null:a.clone(), bc=b==null?null:b.clone();
        assertThrows(type, () -> M3CountLanes.combine(a,ao,b,bo,len,op));
        assertThrows(type, () -> M3CountLanes.combineNative(a,ao,b,bo,len,op));
        assertThrows(type, () -> M3CountTotals.combine(a,ao,b,bo,len,op));
        assertThrows(type, () -> M3CountTotals.combineNative(a,ao,b,bo,len,op));
        assertArrayEquals(ac,a); assertArrayEquals(bc,b);
    }
}
