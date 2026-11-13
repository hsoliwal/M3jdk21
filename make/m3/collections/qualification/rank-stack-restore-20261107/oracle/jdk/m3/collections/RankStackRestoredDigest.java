// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Digest of every restored rank-stack entry point (qualification/rank-stack-restore-20261107) over seeded sorted
 * signed-long snapshots with duplicates and Long.MIN_VALUE/MAX_VALUE: M3LongRankIndex equalRanges(Into), upperBounds(Into),
 * counts/countInto, rangeCounts/rangeCountInto, intersection/union/difference/symmetric values and counts, selectMerged
 * (Batch/Into), run statistics, distinct triplet and K-diff counts, closest windows, pair sums; M3LongRankBatchNative
 * lowerBoundsInto, floorIndices, intersectionWindowCount. An exception is
 * digested by its class name. Run with and without -Dm3.collections.rank.native=true (library on java.library.path),
 * default and -Xint: native and Java digests must be byte-identical (sizes cross every NATIVE_MIN_* threshold).
 * -Drankstack.trace=true prints each digested exception (class and message) to stderr.
 * usage: java RankStackRestoredDigest OPS SEED
 */
public final class RankStackRestoredDigest {
    private static final boolean TRACE = Boolean.getBoolean("rankstack.trace");
    private static MessageDigest digest;
    private static long calls;

    private RankStackRestoredDigest() { }

    private static void rec(Supplier<Object> call) {
        Object value;
        try {
            value = call.get();
        } catch (RuntimeException | UnsatisfiedLinkError error) {
            value = "!" + error.getClass().getSimpleName();
            if (TRACE) System.err.println("TRACE " + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
        String text = value instanceof long[] l ? Arrays.toString(l) : value instanceof int[] i ? Arrays.toString(i)
                : String.valueOf(value);
        digest.update(text.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) ';');
        calls++;
    }

    private static long value(Random random) {
        return switch (random.nextInt(9)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            case 3, 4, 5 -> random.nextInt(24) - 12;
            default -> random.nextLong() >> random.nextInt(60);
        };
    }

    private static long[] lane(Random random, boolean sorted) {
        int n = switch (random.nextInt(4)) {
            case 0 -> random.nextInt(8);
            case 1 -> random.nextInt(80);
            case 2 -> 60 + random.nextInt(400);
            default -> 250 + random.nextInt(900);
        };
        long[] out = new long[n];
        for (int i = 0; i < n; i++) out[i] = value(random);
        if (sorted) Arrays.sort(out);
        return out;
    }

    private static void index(Random random) {
        long[] a = lane(random, true);
        long[] b = lane(random, true);
        long[] q = lane(random, random.nextBoolean());
        M3LongRankIndex left = M3LongRankIndex.copyOf(a);
        M3LongRankIndex right = M3LongRankIndex.copyOf(b);
        int from = q.length == 0 ? 0 : random.nextInt(q.length);
        int len = q.length - from == 0 ? 0 : random.nextInt(q.length - from + 1);
        rec(() -> left.equalRanges(q));
        rec(() -> { int[] o = new int[2 * len + 3]; left.equalRangesInto(q, from, len, o, 1); return o; });
        rec(() -> left.upperBounds(q));
        rec(() -> { int[] o = new int[len + 2]; left.upperBoundsInto(q, from, len, o, 2); return o; });
        rec(() -> left.counts(q));
        rec(() -> { int[] o = new int[len + 1]; left.countInto(q, from, len, o, 1); return o; });
        long[] hi = new long[q.length];
        for (int i = 0; i < q.length; i++) hi[i] = random.nextInt(5) == 0 ? value(random) : q[i] + random.nextInt(9);
        rec(() -> left.rangeCounts(q, hi));
        rec(() -> { int[] o = new int[len]; left.rangeCountInto(q, hi, from, len, o, 0); return o; });
        boolean distinct = random.nextBoolean();
        rec(() -> left.intersectionCount(right));
        rec(() -> left.intersectionValues(right, distinct));
        rec(() -> left.unionValues(right, distinct));
        rec(() -> left.unionCount(right));
        rec(() -> left.differenceValues(right, distinct));
        rec(() -> left.symmetricDifferenceValues(right, distinct));
        long total = (long) a.length + b.length;
        long[] ranks = new long[q.length];
        for (int i = 0; i < ranks.length; i++) ranks[i] = total == 0 ? i : Math.floorMod(q[i], total + 1);
        rec(() -> left.selectMerged(right, total == 0 ? 0 : random.nextLong(total)));
        rec(() -> left.selectMergedBatch(right, ranks));
        rec(() -> { long[] o = new long[len + 1]; left.selectMergedInto(right, ranks, from, len, o, 1); return o; });
        rec(left::distinctValueCount);
        rec(left::longestConsecutiveRun);
        rec(() -> M3LongRankBatchNative.runStatistics(a));
        long target = value(random);
        rec(() -> a.length > 700 ? "skip" : left.distinctTripletSumCount(target));
        rec(() -> left.distinctDifferencePairCount(Math.abs(target % 64)));
        rec(() -> left.distinctDifferencePairCount(target));
        int k = a.length == 0 ? 0 : random.nextInt(a.length + 1);
        rec(() -> left.closestWindowStarts(q, k));
        rec(() -> { int[] o = new int[len + 2]; left.closestWindowStartsInto(q, from, len, k, o, 2); return o; });
        rec(() -> left.pairSumIndices(q));
        rec(() -> { int[] o = new int[2 * len]; left.pairSumIndicesInto(q, from, len, o, 0); return o; });
    }

    private static void statics(Random random) {
        long[] a = lane(random, true);
        long[] b = lane(random, true);
        long[] q = lane(random, random.nextBoolean());
        int from = q.length == 0 ? 0 : random.nextInt(q.length);
        int len = q.length - from == 0 ? 0 : random.nextInt(q.length - from + 1);
        rec(() -> { int[] o = new int[len + 1]; M3LongRankBatchNative.lowerBoundsInto(a, q, from, len, o, 1); return o; });
        rec(() -> M3LongRankBatchNative.floorIndices(a, q));
        int lf = a.length == 0 ? 0 : random.nextInt(a.length);
        int rf = b.length == 0 ? 0 : random.nextInt(b.length);
        int ll = a.length - lf == 0 ? 0 : random.nextInt(a.length - lf + 1);
        int rl = b.length - rf == 0 ? 0 : random.nextInt(b.length - rf + 1);
        rec(() -> M3LongRankBatchNative.intersectionWindowCount(a, lf, ll, b, rf, rl, random.nextBoolean()));
    }

    public static void main(String[] args) throws Exception {
        int ops = Integer.parseInt(args[0]);
        Random random = new Random(Long.parseLong(args[1]));
        digest = MessageDigest.getInstance("SHA-256");
        for (int op = 0; op < ops; op++) {
            index(random);
            statics(random);
        }
        StringBuilder hex = new StringBuilder();
        for (byte x : digest.digest()) hex.append(String.format("%02x", x));
        System.out.println("RANK_STACK_RESTORED ops=" + ops + " seed=" + args[1] + " calls=" + calls
                + " native=" + M3LongRankBatchNative.nativeAvailable() + " sha256=" + hex.substring(0, 16));
    }
}
