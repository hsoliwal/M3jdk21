// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** Isolated LC #15 oracle with extreme signed longs and required-JNI parity. */
public final class M3LongRankTripletSumContract {
    private static int checks;
    private record Triple(long a, long b, long c) { }
    private M3LongRankTripletSumContract() { }
    private static void expect(long value, long actual, String reason) {
        checks++;
        if (actual != value) throw new AssertionError(reason + " expected=" + value + " got=" + actual);
    }
    private static long brute(long[] sorted, long target) {
        Set<Triple> unique = new HashSet<>();
        BigInteger goal = BigInteger.valueOf(target);
        for (int a = 0; a < sorted.length; a++) {
            for (int b = a + 1; b < sorted.length; b++) {
                for (int c = b + 1; c < sorted.length; c++) {
                    BigInteger sum = BigInteger.valueOf(sorted[a])
                            .add(BigInteger.valueOf(sorted[b])).add(BigInteger.valueOf(sorted[c]));
                    if (sum.equals(goal)) unique.add(new Triple(sorted[a], sorted[b], sorted[c]));
                }
            }
        }
        return unique.size();
    }
    private static void check(long[] unsorted, long target, boolean bruteForce) {
        long[] input = unsorted.clone(), before = unsorted.clone();
        Arrays.sort(input);
        long java = M3LongRankBatchNative.distinctTripletSumCountJava(input, target);
        expect(java, M3LongRankBatchNative.distinctTripletSumCount(input, target), "fallback/dispatch");
        if (bruteForce) expect(brute(input, target), java, "independent BigInteger brute");
        if (M3LongRankBatchNative.nativeAvailable()
                && !Boolean.getBoolean("m3.triplet.testOlderAbi"))
            expect(java, M3LongRankBatchNative.distinctTripletSumCountNativeRequired(input, target), "explicit JNI");
        expect(0, Arrays.compare(before, unsorted), "caller payload intact");
    }
    public static void main(String[] ignored) {
        checks = 0;
        check(new long[0], 0, true);
        check(new long[]{1}, 1, true);
        check(new long[]{1,2}, 3, true);
        check(new long[]{-1,0,1,2,-1,-4}, 0, true); // unique: (-1,-1,2), (-1,0,1)
        check(new long[]{0,0,0,0,0,0,0}, 0, true);
        check(new long[]{Long.MIN_VALUE,Long.MAX_VALUE,0,1,-1}, 0, true);
        check(new long[]{Long.MIN_VALUE,Long.MIN_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,1,0}, Long.MIN_VALUE, true);
        Random rng = new Random(0x15000021L);
        for (int iteration=0;iteration<1800;iteration++) {
            int n=rng.nextInt(21);
            long[] a=new long[n];
            for (int i=0;i<n;i++) a[i]=switch (rng.nextInt(12)) {
                case 0 -> Long.MIN_VALUE;
                case 1 -> Long.MAX_VALUE;
                case 2 -> rng.nextLong();
                default -> rng.nextInt(13)-6;
            };
            long target=switch (iteration%7) {
                case 0 -> Long.MIN_VALUE;
                case 1 -> Long.MAX_VALUE;
                case 2 -> rng.nextLong();
                default -> rng.nextInt(41)-20;
            };
            check(a,target,true);
        }
        for(int size:new int[]{191,192,193,256,512}){
            for(int round=0;round<8;round++){
                long[] a=new long[size];
                for(int i=0;i<size;i++) a[i]=rng.nextInt(601)-300;
                check(a,round-4,false);
            }
        }
        if (Boolean.getBoolean("m3.triplet.testOlderAbi")) {
            if (!M3LongRankBatchNative.nativeAvailable())
                throw new AssertionError("simulated old m3rankbatch library was not loaded");
            try {
                M3LongRankBatchNative.distinctTripletSumCountNativeRequired(
                        new long[]{-1L, 0L, 1L}, 0L);
                throw new AssertionError("required new ABI must fail closed on old native library");
            } catch (UnsatisfiedLinkError expected) { checks++; }
        }
        if(!M3LongRankBatchNative.nativeAvailable()) {
            try {
                M3LongRankBatchNative.distinctTripletSumCountNativeRequired(new long[0],0);
                throw new AssertionError("required JNI must not fall back");
            } catch (UnsatisfiedLinkError expected) { checks++; }
        }
        System.out.println("PASS M3_TRIPLET_JNI checks="+checks+" native="+M3LongRankBatchNative.nativeAvailable());
    }
}
