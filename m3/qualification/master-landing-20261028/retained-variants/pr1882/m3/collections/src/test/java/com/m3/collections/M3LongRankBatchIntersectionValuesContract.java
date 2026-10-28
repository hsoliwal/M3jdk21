// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import java.util.Arrays;
import java.util.Random;
public final class M3LongRankBatchIntersectionValuesContract {
    private static int checks;
    private static void eq(long[] expected, long[] actual) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("expected="+Arrays.toString(expected)+" actual="+Arrays.toString(actual));
        }
    }
    private static long[] oracle(long[] a, long[] b, boolean distinct) {
        long[] temp = new long[Math.min(a.length, b.length)];
        int at = 0;
        for (int i = 0; i < a.length;) {
            long target = a[i]; int ia = i;
            while (i < a.length && a[i] == target) i++;
            int lo = 0, hi = b.length;
            while (lo < hi) { int mid=lo+((hi-lo)>>>1); if(b[mid]<target)lo=mid+1; else hi=mid; }
            int begin=lo; hi=b.length;
            while(lo<hi) {int mid=lo+((hi-lo)>>>1);if(b[mid]<=target)lo=mid+1; else hi=mid;}
            int n = Math.min(i-ia,lo-begin);
            if (distinct && n>0) n=1;
            for(int k=0;k<n;k++)temp[at++]=target;
        }
        return Arrays.copyOf(temp,at);
    }
    public static void main(String[] args) {
        Random random = new Random(0x3cbeb141L);
        boolean nativeAbi = false;
        if (M3LongRankBatchNative.nativeAvailable()) {
            try {
                eq(new long[0], M3LongRankBatchNative.intersectionValues0(
                        new long[0], new long[0], false));
                nativeAbi = true;
            } catch (UnsatisfiedLinkError olderNativeLibrary) {
                // The additive ABI is optional; automatic dispatch must recover.
            }
        }
        long[][] fixed = {{},{Long.MIN_VALUE},{Long.MAX_VALUE},{-1,0,0,1},
                          {Long.MIN_VALUE,Long.MIN_VALUE,Long.MAX_VALUE},
                          {2,2,2,3,3,4,5}, {2,2,3,3,3,3}};
        for (long[] a:fixed) for (long[] b:fixed) for(boolean distinct:new boolean[]{false,true}) {
            eq(oracle(a,b,distinct),M3LongRankBatchNative.intersectionValues(a,b,distinct));
            if (nativeAbi) {
                eq(oracle(a,b,distinct),M3LongRankBatchNative.intersectionValues(a,b,distinct,true));
            }
        }
        for(int round=0;round<1500;round++) {
            int m = random.nextInt(550),n=random.nextInt(550);
            long[] a=new long[m],b=new long[n];
            for(int i=0;i<m;i++)a[i]=random.nextInt(18)==0 ? Long.MIN_VALUE : random.nextInt(18)==1 ? Long.MAX_VALUE : random.nextInt(45)-22;
            for(int i=0;i<n;i++)b[i]=random.nextInt(18)==0 ? Long.MIN_VALUE : random.nextInt(18)==1 ? Long.MAX_VALUE : random.nextInt(45)-22;
            Arrays.sort(a);Arrays.sort(b);
            for(boolean distinct:new boolean[]{false,true}) {
                long[] expected=oracle(a,b,distinct);
                eq(expected,M3LongRankBatchNative.intersectionValues(a,b,distinct));
                if(nativeAbi)eq(expected,M3LongRankBatchNative.intersectionValues(a,b,distinct,true));
            }
        }
        // Cross all three 256-long JNI output-page boundaries deterministically.
        long[] repeatedLeft = new long[1025];
        long[] repeatedRight = new long[769];
        Arrays.fill(repeatedLeft, Long.MIN_VALUE);
        Arrays.fill(repeatedRight, Long.MIN_VALUE);
        long[] repeatedExpected = new long[769];
        Arrays.fill(repeatedExpected, Long.MIN_VALUE);
        eq(repeatedExpected, M3LongRankBatchNative.intersectionValues(
                repeatedLeft, repeatedRight, false));
        eq(new long[]{Long.MIN_VALUE}, M3LongRankBatchNative.intersectionValues(
                repeatedLeft, repeatedRight, true));
        if (nativeAbi) {
            eq(repeatedExpected, M3LongRankBatchNative.intersectionValues(
                    repeatedLeft, repeatedRight, false, true));
            eq(new long[]{Long.MIN_VALUE}, M3LongRankBatchNative.intersectionValues(
                    repeatedLeft, repeatedRight, true, true));
        }
        if(!M3LongRankBatchNative.nativeAvailable()) {
            try {M3LongRankBatchNative.intersectionValues(new long[0],new long[0],false,true);
                throw new AssertionError("native-required should fail");
            } catch (UnsatisfiedLinkError expected) { checks++; }
        }
        // The returned result is detached, not a view over either original input.
        long[] first = {1, 2, 2, 3};
        long[] second = {2, 2, 4};
        long[] result = M3LongRankBatchNative.intersectionValues(first, second, false);
        eq(new long[]{2, 2}, result);
        result[0] = -999;
        eq(new long[]{1, 2, 2, 3}, first);
        eq(new long[]{2, 2, 4}, second);
        // Any null is rejected before allocating a result or calling JNI.
        try { M3LongRankBatchNative.intersectionValues(null, second, false); throw new AssertionError("missing NPE"); }
        catch (NullPointerException expected) { checks++; }
        try { M3LongRankBatchNative.intersectionValues(first, null, true); throw new AssertionError("missing NPE"); }
        catch (NullPointerException expected) { checks++; }
        if (nativeAbi) {
            try { M3LongRankBatchNative.intersectionValues0(null, first, false); throw new AssertionError("native missing NPE"); }
            catch (NullPointerException expected) { checks++; }
        }
        if (M3LongRankBatchNative.nativeAvailable() && !nativeAbi) {
            try { M3LongRankBatchNative.intersectionValues(first, second, false, true);
                throw new AssertionError("legacy JNI required should fail");
            } catch (UnsatisfiedLinkError expected) { checks++; }
        }
        System.out.println("PASS M3_NATIVE_INTERSECTION_VALUES checks="+checks+" native="+M3LongRankBatchNative.nativeAvailable());
    }
}
