/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.indexdb;

import java.util.Arrays;
import java.util.Random;

/** Independent scalar oracle, geometry/refusal, chunk-DAG and overflow tests. */
public final class M3IndexDbChunkSimHashTest {
    private static final String DOMAIN = "m3:indexdb:token-shingle64:v1";
    private static long assertions;
    private M3IndexDbChunkSimHashTest() { }

    private static void check(boolean condition, String reason) {
        assertions++;
        if (!condition) throw new AssertionError(reason);
    }
    private static void equal(long expected, long actual, String reason) {
        check(expected == actual, reason + ": expected=" + expected + " actual=" + actual);
    }
    private static void rejects(Class<? extends Throwable> expected, Runnable action, String label) {
        assertions++;
        try { action.run(); } catch (Throwable e) {
            if (expected.isInstance(e)) return;
            throw new AssertionError(label, e);
        }
        throw new AssertionError(label + " accepted");
    }

    private static long[] independentlyCount(long[] features, int[] weights, int from, int len) {
        long[] vector = new long[64];
        for (int bit = 0; bit < 64; bit++) {
            long score = 0;
            for (int i = from; i < from + len; i++) {
                score += ((features[i] & (1L << bit)) != 0) ? weights[i] : -(long) weights[i];
            }
            vector[bit] = score;
        }
        return vector;
    }
    private static long independentlyFinish(long[] votes, int count) {
        if (count == 0) return 0;
        long fingerprint = 0;
        for (int bit = 0; bit < 64; bit++) if (votes[bit] >= 0) fingerprint |= 1L << bit;
        return fingerprint;
    }

    public static void main(String[] args) {
        var empty = M3IndexDbChunkSimHash.empty(DOMAIN);
        equal(0L, empty.simHash64(), "empty signature explicitly zero");
        equal(0L, empty.totalWeight(), "empty weight");
        equal(0L, empty.featureCount(), "empty count");
        rejects(IllegalArgumentException.class, () -> M3IndexDbChunkSimHash.empty(" "), "blank domain");
        rejects(IllegalArgumentException.class, () -> empty.merge(M3IndexDbChunkSimHash.empty("different")), "domain composition");
        rejects(IllegalArgumentException.class, () -> empty.hammingDistance(M3IndexDbChunkSimHash.empty("different")), "domain distance");
        rejects(IllegalArgumentException.class, () -> empty.repeat(-1), "negative repeat");
        rejects(NullPointerException.class, () -> empty.merge(null), "null child");
        rejects(IndexOutOfBoundsException.class, () -> M3IndexDbChunkSimHash.fromFeatures(DOMAIN, new long[0], -1, new int[0], 0, 0), "negative hash offset");
        rejects(IndexOutOfBoundsException.class, () -> M3IndexDbChunkSimHash.fromFeatures(DOMAIN, new long[1], 0, new int[0], 0, 1), "weight geometry");
        rejects(IllegalArgumentException.class, () -> M3IndexDbChunkSimHash.fromFeatures(DOMAIN, new long[]{5}, 0, new int[]{0}, 0, 1), "zero weight");
        rejects(IllegalArgumentException.class, () -> M3IndexDbChunkSimHash.fromFeatures(DOMAIN, new long[]{5}, 0, new int[]{-3}, 0, 1), "negative weight");

        Random random = new Random(0x4d334348554e4bL);
        for (int trial = 0; trial < 600; trial++) {
            int n = random.nextInt(91);
            long[] features = new long[n+5]; int[] weights = new int[n+7];
            for (int i = 0; i < features.length; i++) features[i] = random.nextLong();
            for (int i = 0; i < weights.length; i++) weights[i] = 1 + random.nextInt(15);
            long[] actualFeatures = Arrays.copyOfRange(features, 2, n+2);
            int[] actualWeights = Arrays.copyOfRange(weights, 3, n+3);
            var full = M3IndexDbChunkSimHash.fromFeatures(DOMAIN, features,2,weights,3,n);
            long[] independent = independentlyCount(actualFeatures, actualWeights,0,n);
            check(Arrays.equals(independent,full.voteCopy()), "independent 64-bit vote vector");
            equal(independentlyFinish(independent,n),full.simHash64(),"independent fingerprint");
            equal(n,full.featureCount(),"feature count");
            int cut1 = n==0 ? 0 : random.nextInt(n+1);
            int cut2 = cut1 + (n==cut1 ? 0 : random.nextInt(n-cut1+1));
            var a = M3IndexDbChunkSimHash.fromFeatures(DOMAIN,actualFeatures,0,actualWeights,0,cut1);
            var b = M3IndexDbChunkSimHash.fromFeatures(DOMAIN,actualFeatures,cut1,actualWeights,cut1,cut2-cut1);
            var c = M3IndexDbChunkSimHash.fromFeatures(DOMAIN,actualFeatures,cut2,actualWeights,cut2,n-cut2);
            check(Arrays.equals(full.voteCopy(),a.merge(b).merge(c).voteCopy()),"associative (a+b)+c");
            check(Arrays.equals(full.voteCopy(),a.merge(b.merge(c)).voteCopy()),"associative a+(b+c)");
            check(Arrays.equals(full.voteCopy(),c.merge(a).merge(b).voteCopy()),"feature multiset order does not matter");
            equal(full.simHash64(),a.merge(b).merge(c).simHash64(),"derived composition signature");
            equal(0,full.hammingDistance(a.merge(b).merge(c)),"identical multisets distance 0");
            long[] copied=full.voteCopy(); copied[0] ^= 1234567;
            check(!Arrays.equals(copied,full.voteCopy()) || n==0,"defensive copy");
            long repeat=1 + random.nextInt(32);
            var multiplied=full.repeat(repeat);
            for(int bit=0;bit<64;bit++) equal(Math.multiplyExact(independent[bit],repeat),multiplied.voteCopy()[bit],"repeated vote");
            equal(full.featureCount()*repeat,multiplied.featureCount(),"repeated feature count");
            equal(full.totalWeight()*repeat,multiplied.totalWeight(),"repeated weights");
        }
        // Final signatures are NOT sufficient compositional state: both (1,0) and (100,0)
        // have identical signs, but adding an opposing -2 contribution yields different signs.
        var weak = M3IndexDbChunkSimHash.fromFeatures(DOMAIN,new long[]{-1L,0L},0,new int[]{1,1},0,2);
        var strong = M3IndexDbChunkSimHash.fromFeatures(DOMAIN,new long[]{-1L,-1L,-1L,-1L},0,new int[]{1,1,1,1},0,4);
        // All-bits-one/zero features deliberately create different margins; use bit0.
        check((weak.simHash64() & 1L) == (strong.simHash64() & 1L),"equal final bit sign");
        var oppose = M3IndexDbChunkSimHash.fromFeatures(DOMAIN,new long[]{0L,0L,0L},0,new int[]{1,1,1},0,3);
        check((weak.merge(oppose).simHash64() & 1L) != (strong.merge(oppose).simHash64() & 1L),
                "same final bit needs different next-composition result");
        var huge = M3IndexDbChunkSimHash.fromFeatures(DOMAIN,new long[]{Long.MAX_VALUE},0,new int[]{Integer.MAX_VALUE},0,1);
        rejects(ArithmeticException.class, () -> huge.repeat(Long.MAX_VALUE),"checked overflow");
        System.out.println("PASS chunk SimHash: 600 randomized dynamic-array windows, associative/repetition oracle, assertions=" + assertions);
    }
}
