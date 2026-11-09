// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;

/**
 * Census pass-2 C4 before/after differential of the pre-existing M3LongRankIndex / M3LongRankBatchNative surface
 * (leaves P2-073, P2-074 are purely additive): seeded snapshots (duplicates, signed extremes) and every pre-existing
 * query (bounds, select, counts, ranks, range counts, dense rank, batched lower/upper bounds, top-k frequency,
 * fillLowerUpperBounds, toArray, longStream). Run on the master and on the edited classes: digests must be equal.
 * The added LC 315 / LC 719 members are qualified by the relocated contracts (quadratic and two-pointer references).
 * usage: java CensusRankDifferential OPS
 */
public final class CensusRankDifferential {
    private static final long SEED = 0x43345032303733L; // "C4P2073"

    private CensusRankDifferential() { }

    private static long value(Random random) {
        return switch (random.nextInt(8)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            case 3, 4 -> random.nextInt(16) - 8;
            default -> random.nextLong();
        };
    }

    public static void main(String[] args) throws Exception {
        int ops = Integer.parseInt(args[0]);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        Random random = new Random(SEED);
        for (int op = 0; op < ops; op++) {
            long[] source = new long[random.nextInt(4) == 0 ? random.nextInt(600) : random.nextInt(40)];
            for (int i = 0; i < source.length; i++) source[i] = value(random);
            M3LongRankIndex index = M3LongRankIndex.copyOf(source);
            long[] queries = new long[random.nextInt(80)];
            for (int i = 0; i < queries.length; i++) queries[i] = value(random);
            StringBuilder line = new StringBuilder();
            long q = value(random), r = value(random);
            line.append(index.size()).append(index.isEmpty()).append(index.isDistinct())
                    .append(index.lowerBound(q)).append(',').append(index.upperBound(q)).append(',')
                    .append(index.contains(q)).append(index.count(q)).append(',').append(index.rankLessThan(q))
                    .append(',').append(index.rankLessOrEqual(q)).append(',')
                    .append(index.rangeCount(Math.min(q, r), Math.max(q, r))).append(',')
                    .append(index.rangeCountInclusive(Math.min(q, r), Math.max(q, r))).append(',');
            if (index.size() > 0) {
                line.append(index.select(random.nextInt(index.size()))).append(',')
                        .append(index.isDistinct() ? index.denseRankDescending(index.select(random.nextInt(index.size())))
                                : random.nextInt(1)).append(',')
                        .append(Arrays.toString(index.topKFrequent(1 + random.nextInt(Math.min(8, index.size())))));
            }
            line.append(Arrays.toString(index.lowerBounds(queries)))
                    .append(Arrays.toString(index.lowerUpperBounds(queries)));
            int[] pairs = new int[queries.length * 2];
            index.fillLowerUpperBounds(queries, pairs);
            line.append(Arrays.toString(pairs)).append(Arrays.toString(index.toArray()))
                    .append(index.longStream().sum());
            digest.update(line.toString().getBytes());
        }
        System.out.println("RANK_DIFFERENTIAL ops=" + ops + " existing_sha256=" + hex(digest.digest()));
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        for (byte b : bytes) out.append(String.format("%02x", b));
        return out.toString();
    }
}
