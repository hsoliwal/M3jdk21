// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Independent #349/#350 hash-frequencies oracle over signed-long sorted windows. */
public final class M3LongRankWindowIntersectionContract {
    private static long checks;
    private M3LongRankWindowIntersectionContract() { }

    private static void equal(long expected, long actual, String label) {
        checks++;
        if (actual != expected) throw new AssertionError(
                label + " expected=" + expected + " actual=" + actual);
    }
    private static void same(long[] expected, long[] actual, String label) {
        checks++;
        if (!Arrays.equals(expected, actual)) throw new AssertionError(label + " changed input");
    }
    private static void rejects(Class<? extends Throwable> type, Runnable op) {
        checks++;
        try { op.run(); }
        catch (Throwable ex) {
            if (type.isInstance(ex)) return;
            throw new AssertionError("wrong exception: " + ex, ex);
        }
        throw new AssertionError("missing " + type.getName());
    }
    private static long oracle(long[] left, int lf, int ln,
                               long[] right, int rf, int rn, boolean distinct) {
        if (distinct) {
            Set<Long> all = new HashSet<>();
            for (int i = lf; i < lf + ln; i++) all.add(left[i]);
            Set<Long> matches = new HashSet<>();
            for (int j = rf; j < rf + rn; j++) {
                if (all.contains(right[j])) matches.add(right[j]);
            }
            return matches.size();
        }
        Map<Long, Integer> counts = new HashMap<>();
        for (int i = lf; i < lf + ln; i++) counts.merge(left[i], 1, Integer::sum);
        long matches = 0;
        for (int j = rf; j < rf + rn; j++) {
            int available = counts.getOrDefault(right[j], 0);
            if (available > 0) {
                matches++;
                counts.put(right[j], available - 1);
            }
        }
        return matches;
    }
    private static void pair(long[] left, int lf, int ln, long[] right, int rf, int rn,
                             boolean requireNative) {
        long[] beforeLeft = left.clone(), beforeRight = right.clone();
        for (boolean distinct : new boolean[] {false, true}) {
            long expected = oracle(left, lf, ln, right, rf, rn, distinct);
            equal(expected, M3LongRankBatchNative.intersectionWindowCount(
                    left, lf, ln, right, rf, rn, distinct), "auto distinct=" + distinct);
            equal(expected, M3LongRankBatchNative.intersectionWindowCount(
                    right, rf, rn, left, lf, ln, distinct), "symmetry distinct=" + distinct);
            if (requireNative) {
                equal(expected, M3LongRankBatchNative.intersectionWindowCount(
                        left, lf, ln, right, rf, rn, distinct, true),
                        "JNI distinct=" + distinct);
                equal(expected, M3LongRankBatchNative.intersectionWindowCount0(
                        left, lf, ln, right, rf, rn, distinct),
                        "direct ABI distinct=" + distinct);
            }
        }
        same(beforeLeft, left, "left");
        same(beforeRight, right, "right");
    }
    private static long value(Random random) {
        return switch (random.nextInt(10)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> random.nextLong();
            case 3 -> -1L;
            case 4 -> 0L;
            default -> random.nextInt(19) - 9L;
        };
    }
    private static void invalid(boolean nativeRequired) {
        long[] l={-3,-1,0,0,4};
        rejects(NullPointerException.class, () ->
                M3LongRankBatchNative.intersectionWindowCount(null,0,0,l,0,0,false));
        rejects(NullPointerException.class, () ->
                M3LongRankBatchNative.intersectionWindowCount(l,0,0,null,0,0,false));
        rejects(IndexOutOfBoundsException.class, () ->
                M3LongRankBatchNative.intersectionWindowCount(l,-1,1,l,0,1,false));
        rejects(IndexOutOfBoundsException.class, () ->
                M3LongRankBatchNative.intersectionWindowCount(l,5,1,l,0,1,true));
        rejects(IndexOutOfBoundsException.class, () ->
                M3LongRankBatchNative.intersectionWindowCount(l,0,Integer.MAX_VALUE,l,0,1,false));
        rejects(IndexOutOfBoundsException.class, () ->
                M3LongRankBatchNative.intersectionWindowCount(l,0,1,l,6,0,false));
        if (nativeRequired) {
            rejects(NullPointerException.class, () ->
                    M3LongRankBatchNative.intersectionWindowCount0(null,0,1,l,0,1,false));
            rejects(IndexOutOfBoundsException.class, () ->
                    M3LongRankBatchNative.intersectionWindowCount0(l,4,2,l,0,2,true));
            rejects(IndexOutOfBoundsException.class, () ->
                    M3LongRankBatchNative.intersectionWindowCount0(l,0,2,l,4,2,false));
        } else {
            rejects(UnsatisfiedLinkError.class, () ->
                    M3LongRankBatchNative.intersectionWindowCount(l,0,1,l,0,1,false,true));
        }
    }
    public static long contracts(boolean requireNative) {
        if (M3LongRankBatchNative.nativeAvailable() != requireNative) {
            throw new AssertionError("availability differs from required --java/--native mode");
        }
        checks=0;
        pair(new long[0],0,0,new long[0],0,0,requireNative);
        pair(new long[] {Long.MIN_VALUE,-1,-1,0,0,Long.MAX_VALUE},0,6,
             new long[]{Long.MIN_VALUE,-1,0,0,0,Long.MAX_VALUE},0,6,requireNative);
        pair(new long[]{0,0,0,0},0,4,new long[]{0},0,1,requireNative);
        pair(new long[]{0,0,0,0},4,0,new long[]{0},1,0,requireNative);
        pair(new long[]{Long.MIN_VALUE,Long.MAX_VALUE},0,2,
             new long[]{Long.MAX_VALUE,Long.MAX_VALUE},0,2,requireNative);
        int[] sizes={0,1,2,3,7,31,63,64,127,128,129,255,256,257,511,512,1024};
        Random random=new Random(0x34935026L);
        for (int trial=0; trial<1000; trial++) {
            int a=sizes[random.nextInt(sizes.length)];
            int b=sizes[random.nextInt(sizes.length)];
            long[] left=new long[a+5],right=new long[b+9];
            for(int k=0;k<left.length;k++) left[k]=value(random);
            for(int k=0;k<right.length;k++) right[k]=value(random);
            Arrays.sort(left,2,2+a); Arrays.sort(right,4,4+b);
            int lf=2,rf=4;
            pair(left,lf,a,right,rf,b,requireNative);
        }
        invalid(requireNative);
        System.out.println("PASS M3_LEETCODE349_350_JNI_WINDOW checks="+checks+
                " native="+M3LongRankBatchNative.nativeAvailable());
        return checks;
    }
    public static void main(String[] args) {
        if (args.length!=1||(!"--java".equals(args[0])&&!"--native".equals(args[0]))) {
            throw new IllegalArgumentException("expected --java or --native");
        }
        contracts("--native".equals(args[0]));
    }
}
