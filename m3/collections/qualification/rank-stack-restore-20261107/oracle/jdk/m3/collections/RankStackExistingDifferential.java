// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Before/after differential of the pre-existing API of the six owners touched by qualification/rank-stack-restore-20261107:
 * M3LongRankIndex (every pre-existing query, as census2-c4's CensusRankDifferential), M3LongRangeSet (seeded add/remove
 * streams, lanes, contains, intersects, cardinality), M3BagRankIndex (every occurrenceAt rank, bounds, slices, reversed;
 * leaf 26 rewrote occurrenceAt's bisection) and M3IndexPathRank (rank, every selectedSourceOrdinal, row ids, windows;
 * leaves 26 and 60 rewrote selectedSourceOrdinal). Compiles against master and against the restored owners; run both
 * with the same seed and op count: every per-owner digest must be equal. M3BagRankIndex needs the m3_arrays JNI library
 * (M3CountedBag storage); OWNERS (comma list, default all four) skips an owner without consuming its random draws.
 * usage: java RankStackExistingDifferential OPS [OWNERS]
 */
public final class RankStackExistingDifferential {
    private static final long SEED = 0x52414E4B53544B4CL; // "RANKSTKL"

    private RankStackExistingDifferential() { }

    private static long value(Random random) {
        return switch (random.nextInt(8)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            case 3, 4 -> random.nextInt(16) - 8;
            default -> random.nextLong();
        };
    }

    private static String rankIndex(Random random) {
        long[] source = new long[random.nextInt(4) == 0 ? random.nextInt(600) : random.nextInt(40)];
        for (int i = 0; i < source.length; i++) source[i] = value(random);
        M3LongRankIndex index = M3LongRankIndex.copyOf(source);
        long[] queries = new long[random.nextInt(80)];
        for (int i = 0; i < queries.length; i++) queries[i] = value(random);
        long q = value(random);
        long r = value(random);
        StringBuilder line = new StringBuilder();
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
        int[] pairs = new int[queries.length * 2];
        index.fillLowerUpperBounds(queries, pairs);
        line.append(Arrays.toString(index.lowerBounds(queries))).append(Arrays.toString(index.lowerUpperBounds(queries)))
                .append(Arrays.toString(pairs)).append(Arrays.toString(index.toArray())).append(index.longStream().sum());
        return line.toString();
    }

    private static String rangeSet(Random random) {
        M3LongRangeSet set = new M3LongRangeSet();
        StringBuilder line = new StringBuilder();
        int ops = random.nextInt(60);
        for (int op = 0; op < ops; op++) {
            long a = random.nextInt(4) == 0 ? value(random) : random.nextInt(200) - 100;
            long b = random.nextInt(4) == 0 ? value(random) : a + random.nextInt(30);
            long from = Math.min(a, b);
            long to = Math.max(a, b);
            if (random.nextInt(3) == 0) set.remove(from, to);
            else set.add(from, to);
            long p = random.nextInt(220) - 110;
            line.append(set.rangeCount()).append(set.contains(p)).append(set.intersects(Math.min(p, from), Math.max(p, from)))
                    .append(',');
        }
        set.verifyInvariants();
        return line.append(Arrays.toString(set.startsToArray())).append(Arrays.toString(set.endsToArray()))
                .append(set.cardinality()).append(set.snapshot().rangeCount()).toString();
    }

    private static String bagRank(Random random) {
        M3CountedBag<Integer> bag = new M3CountedBag<>();
        int distinct = random.nextInt(50);
        for (int i = 0; i < distinct; i++) bag.addOccurrences(random.nextInt(80) - 40, 1 + random.nextInt(5));
        M3BagRankIndex<Integer> index = M3BagRankIndex.snapshot(bag, Comparator.naturalOrder());
        StringBuilder line = new StringBuilder();
        List<M3BagRankIndex<Integer>> views =
                List.of(index, index.reversed(), index.slice(index.distinctSize() / 3, index.distinctSize()));
        for (M3BagRankIndex<Integer> view : views) {
            line.append(view.distinctSize()).append(':').append(view.exactSize()).append(':');
            for (long rank = 0; rank < view.exactSize(); rank++) line.append(view.occurrenceAt(rank)).append(',');
            int key = random.nextInt(90) - 45;
            line.append(view.lowerBound(key)).append(',').append(view.upperBound(key)).append(',')
                    .append(view.occurrencesBefore(key)).append(',').append(view.occurrencesThrough(key)).append(';');
        }
        return line.toString();
    }

    private static String pathRank(Random random) {
        int n = random.nextInt(4) == 0 ? random.nextInt(700) : random.nextInt(130);
        int[] rows = new int[n];
        int[] offsets = new int[n];
        int[] states = new int[n];
        int density = random.nextInt(4);
        for (int i = 0; i < n; i++) {
            rows[i] = random.nextInt(1000) - 500;
            offsets[i] = i;
            states[i] = density == 0 ? 1 : density == 1 ? random.nextInt(2) : random.nextInt(16) == 0 ? 1 : 0;
        }
        M3IndexPathRank rank = M3IndexPathRank.snapshot(rows, offsets, new int[][] {states}, new int[] {1}, new int[] {0},
                0, n, 1);
        StringBuilder line = new StringBuilder().append(rank.size()).append(':');
        for (int s = 0; s < rank.size(); s++) {
            line.append(rank.selectedSourceOrdinal(s)).append('/').append(rank.selectedRowId(s)).append(',');
        }
        for (int e = 0; e <= n; e += 1 + random.nextInt(9)) line.append(rank.rank(e)).append(',');
        int from = n == 0 ? 0 : random.nextInt(n + 1);
        int to = from + (n == from ? 0 : random.nextInt(n - from + 1));
        M3IndexPathRank.Window window = rank.window(from, to);
        line.append(Arrays.toString(rank.toArray())).append(Arrays.toString(window.toArray()));
        for (int s = 0; s < window.size(); s++) line.append(window.selectedSourceOrdinal(s)).append(',');
        return line.toString();
    }

    public static void main(String[] args) throws Exception {
        int ops = Integer.parseInt(args[0]);
        String[] owners = {"M3LongRankIndex", "M3LongRangeSet", "M3BagRankIndex", "M3IndexPathRank"};
        List<String> enabled = List.of(args.length > 1 ? args[1].split(",") : owners);
        MessageDigest[] digests = new MessageDigest[owners.length];
        MessageDigest all = MessageDigest.getInstance("SHA-256");
        for (int i = 0; i < owners.length; i++) digests[i] = MessageDigest.getInstance("SHA-256");
        Random[] randoms = new Random[owners.length];
        for (int i = 0; i < owners.length; i++) randoms[i] = new Random(SEED + i);
        for (int op = 0; op < ops; op++) {
            String[] lines = {
                enabled.contains(owners[0]) ? rankIndex(randoms[0]) : "",
                enabled.contains(owners[1]) ? rangeSet(randoms[1]) : "",
                enabled.contains(owners[2]) ? bagRank(randoms[2]) : "",
                enabled.contains(owners[3]) ? pathRank(randoms[3]) : ""};
            for (int i = 0; i < owners.length; i++) {
                digests[i].update(lines[i].getBytes(java.nio.charset.StandardCharsets.UTF_8));
                all.update(lines[i].getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        StringBuilder out = new StringBuilder("RANK_STACK_EXISTING ops=" + ops);
        for (int i = 0; i < owners.length; i++) {
            out.append(' ').append(owners[i]).append('=')
                    .append(enabled.contains(owners[i]) ? hex(digests[i].digest()) : "skipped");
        }
        System.out.println(out.append(" all=").append(hex(all.digest())));
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        for (byte b : bytes) out.append(String.format("%02x", b));
        return out.substring(0, 16);
    }
}
