/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.Random;

/** Deterministic array/offset/snapshot/state-mask oracle; JNI checked in product workflow. */
public final class M3LongRangeMapSelfTest {
    private static long checks;
    private M3LongRangeMapSelfTest() { }
    private static void ok(boolean truth) {
        checks++;
        if (!truth) throw new AssertionError("range-map check=" + checks);
    }
    private static void eq(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual + " check=" + checks);
        }
    }
    private static void refuse(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable error) {
            if (kind.isInstance(error)) return;
            throw new AssertionError("wrong exception", error);
        }
        throw new AssertionError("missing exception " + kind);
    }
    private static void compare(M3LongRangeMap<Integer> map, boolean[] present, Integer[] expected) {
        map.verifyInvariants();
        eq(map.rangeCount(), map.startsToArray().length);
        eq(map.rangeCount(), map.endsToArray().length);
        eq(map.rangeCount(), map.valuesToArray().length);
        for (int i = 0; i < expected.length; i++) {
            long coordinate = i - 128L;
            ok(map.containsPoint(coordinate) == present[i]);
            eq(present[i] ? expected[i] : null, map.get(coordinate));
        }
    }
    private static void randomEdits() {
        Random random = new Random(0x4d3352414e47454dL);
        for (int trial = 0; trial < 4; trial++) {
            M3LongRangeMap<Integer> actual = new M3LongRangeMap<>();
            boolean[] present = new boolean[256];
            Integer[] expected = new Integer[256];
            for (int operation = 0; operation < 12_288; operation++) {
                int a = random.nextInt(257), b = random.nextInt(257);
                int lo = Math.min(a,b), hi = Math.max(a,b);
                int mode = random.nextInt(8);
                if (mode <= 4) {
                    Integer value = mode == 0 ? null : random.nextInt(13) - 6;
                    actual.put(lo-128L,hi-128L,value);
                    Arrays.fill(present,lo,hi,true);
                    Arrays.fill(expected,lo,hi,value);
                } else if (mode <= 6) {
                    actual.remove(lo-128L,hi-128L);
                    Arrays.fill(present,lo,hi,false);
                    Arrays.fill(expected,lo,hi,null);
                } else {
                    boolean any = false;
                    for (int i = lo; i < hi; i++) any |= present[i];
                    ok(any == actual.intersects(lo-128L,hi-128L));
                }
                if ((operation & 15) == 0) compare(actual,present,expected);
                if ((operation & 255) == 0) {
                    M3LongRangeMap<Integer> snapshot = actual.snapshot();
                    compare(snapshot,present,expected);
                    long[] escaped = snapshot.startsToArray();
                    if (escaped.length != 0) {
                        escaped[0] = Long.MAX_VALUE;
                        snapshot.verifyInvariants();
                    }
                }
            }
            compare(actual,present,expected);
        }
    }
    private static void extremesAndSelection() {
        var map = new M3LongRangeMap<String>();
        map.put(Long.MIN_VALUE, Long.MAX_VALUE, "all");
        ok(map.containsPoint(Long.MIN_VALUE));
        eq("all",map.get(Long.MAX_VALUE-1));
        ok(!map.containsPoint(Long.MAX_VALUE));
        map.remove(-4,5);
        eq(2,map.rangeCount());
        eq("all",map.get(-5));ok(!map.containsPoint(0));eq("all",map.get(5));
        map.put(-2,2,null);
        ok(map.containsPoint(0));eq(null,map.get(0));
        map.verifyInvariants();
        var frozen=map.snapshot();
        map.clear();ok(map.isEmpty());ok(frozen.containsPoint(Long.MIN_VALUE));
        refuse(IllegalArgumentException.class,()->map.put(9,8,"x"));
        refuse(IllegalArgumentException.class,()->map.remove(9,8));
        map.put(0,10,"a");map.put(20,30,"b");map.put(40,50,"c");
        var one=map.selectedByState(new int[]{0x8000_0001,0x0000_0001,0x8000_0003},
                0x8000_0001,0x0000_0002,M3IndexPathMasks.Backend.JAVA);
        eq(1,one.rangeCount());eq("a",one.get(5));ok(!one.containsPoint(45));
        var pair=map.selectedByState(new int[]{1,0,1},1,0,M3IndexPathMasks.Backend.JAVA);
        eq(2,pair.rangeCount());eq("a",pair.get(0));eq("c",pair.get(45));
        refuse(IllegalArgumentException.class,()->map.selectedByState(new int[0],1,0,M3IndexPathMasks.Backend.JAVA));
        refuse(NullPointerException.class,()->map.selectedByState(null,1,0,M3IndexPathMasks.Backend.JAVA));
        long[] starts=map.startsToArray();Object[] vals=map.valuesToArray();
        starts[0]=99;vals[0]="mutated";
        eq("a",map.get(0));map.verifyInvariants();
        int[] count={0};
        refuse(ConcurrentModificationException.class,()->map.forEachRange((lo,hi,v)->{
            count[0]++;map.remove(lo,hi);
        }));
        eq(1,count[0]);
        if (M3IndexPathMasks.nativeAvailable()) {
            var nativeMap=pair.selectedByState(new int[]{0x8000_0001,1},1,0,
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED);
            eq(pair.rangeCount(),nativeMap.rangeCount());eq("a",nativeMap.get(5));eq("c",nativeMap.get(45));
        } else {
            refuse(UnsatisfiedLinkError.class,()->pair.selectedByState(new int[]{1,1},1,0,
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        }
    }
    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("no CLI arguments");
        checks=0;randomEdits();extremesAndSelection();
        System.out.println("PASS M3_LONG_RANGE_MAP checks="+checks+" seeded_operations=49152");
    }
}
