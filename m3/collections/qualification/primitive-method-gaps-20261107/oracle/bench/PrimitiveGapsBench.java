/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * G1 design measurement for primitive-method-gaps-20261107 (bench only, never in src/): lazy page-prefix lane
 * (landed M3LongTreeSet) against the eager candidate (make_eager_candidate.py) and against the base owner, plus
 * get(int) against iterating to the index. Median of {@value #ROUNDS} timed rounds after {@value #WARMUP} warm-up
 * rounds, one workload per JVM; prints one TSV row: workload, impl, n, ops, median ns/op, checksum.
 *
 * <p>usage: java -cp ... jdk.m3.collections.PrimitiveGapsBench WORKLOAD IMPL N
 * (WORKLOAD = mutate | get | iterate | interleave | bulk | getalloc; IMPL = m3 | eager | tree)</p>
 */
public final class PrimitiveGapsBench {
    private static final int WARMUP = 3;
    private static final int ROUNDS = 5;
    private static final long SEED = 0x5052494D47415053L;
    private static final int ITERATE_QUERIES = 2_000;
    private static final int GET_QUERIES = 2_000_000;
    private static final int INTERLEAVE_OPS = 20_000;

    private PrimitiveGapsBench() {}

    /** The two set shapes under test, behind one tiny facade so one loop times both. */
    private interface Set {
        boolean add(long value);
        boolean remove(long value);
        long get(int index);
        int size();
        M3LongIterator iterator();
        int addAllSorted(long[] sorted, int from, int to);
    }

    private static Set open(String impl) {
        if (impl.equals("eager")) {
            M3LongTreeSetEagerPrefixCandidate s = new M3LongTreeSetEagerPrefixCandidate();
            return new Set() {
                public boolean add(long v) { return s.add(v); }
                public boolean remove(long v) { return s.remove(v); }
                public long get(int i) { return s.get(i); }
                public int size() { return s.size(); }
                public M3LongIterator iterator() { return s.iterator(); }
                public int addAllSorted(long[] a, int f, int t) { return s.addAllSorted(a, f, t); }
            };
        }
        M3LongTreeSet s = new M3LongTreeSet();
        return new Set() {
            public boolean add(long v) { return s.add(v); }
            public boolean remove(long v) { return s.remove(v); }
            public long get(int i) { return s.get(i); }
            public int size() { return s.size(); }
            public M3LongIterator iterator() { return s.iterator(); }
            public int addAllSorted(long[] a, int f, int t) { return s.addAllSorted(a, f, t); }
        };
    }

    private static long[] keys(int n) {
        SplittableRandom random = new SplittableRandom(SEED);
        long[] out = new long[n];
        for (int i = 0; i < n; i++) {
            out[i] = random.nextLong();
        }
        return out;
    }

    private static Set build(String impl, long[] keys) {
        Set set = open(impl);
        for (long key : keys) {
            set.add(key);
        }
        return set;
    }

    /** One timed round; returns {elapsed ns, checksum}. */
    private static long[] round(String workload, String impl, long[] keys) {
        SplittableRandom random = new SplittableRandom(SEED ^ keys.length);
        long sum = 0;
        long start;
        switch (workload) {
            case "mutate" -> {
                start = System.nanoTime();
                Set set = build(impl, keys);
                for (int i = 0; i < keys.length; i += 2) {
                    sum += set.remove(keys[i]) ? 1 : 0;
                }
                for (int i = 0; i < keys.length; i += 2) {
                    sum += set.add(keys[i] ^ SEED) ? 1 : 0;
                }
            }
            case "get" -> {
                Set set = build(impl, keys);
                sum += set.get(0);
                start = System.nanoTime();
                for (int q = 0; q < GET_QUERIES; q++) {
                    sum += set.get(random.nextInt(set.size()));
                }
            }
            case "iterate" -> {
                Set set = build(impl, keys);
                start = System.nanoTime();
                for (int q = 0; q < ITERATE_QUERIES; q++) {
                    int target = random.nextInt(set.size());
                    M3LongIterator cursor = set.iterator();
                    long value = 0;
                    for (int i = 0; i <= target; i++) {
                        value = cursor.nextLong();
                    }
                    sum += value;
                }
            }
            case "interleave" -> {
                Set set = build(impl, keys);
                start = System.nanoTime();
                for (int q = 0; q < INTERLEAVE_OPS; q++) {
                    set.add(random.nextLong());
                    sum += set.get(random.nextInt(set.size()));
                    set.remove(set.get(random.nextInt(set.size())));
                }
            }
            case "bulk" -> {
                long[] sorted = keys.clone();
                Arrays.sort(sorted);
                start = System.nanoTime();
                Set set = open(impl);
                if (impl.equals("m3loop")) {
                    for (long key : sorted) {
                        set.add(key);
                    }
                } else {
                    sum += set.addAllSorted(sorted, 0, sorted.length);
                }
                sum += set.size();
            }
            default -> throw new IllegalArgumentException(workload);
        }
        return new long[] {System.nanoTime() - start, sum};
    }

    private static long ops(String workload, int n) {
        return switch (workload) {
            case "mutate" -> n + (long) n;
            case "get" -> GET_QUERIES;
            case "iterate" -> ITERATE_QUERIES;
            case "interleave" -> INTERLEAVE_OPS * 3L;
            default -> n;
        };
    }

    private static void allocation(String impl, long[] keys) {
        Set set = build(impl, keys);
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        SplittableRandom random = new SplittableRandom(SEED);
        long sum = set.get(0);
        for (int warm = 0; warm < GET_QUERIES; warm++) {
            sum += set.get(random.nextInt(set.size()));
        }
        long before = bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
        for (int q = 0; q < GET_QUERIES; q++) {
            sum += set.get(random.nextInt(set.size()));
        }
        long allocated = bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) - before;
        System.out.println("getalloc\t" + impl + "\t" + keys.length + "\t" + GET_QUERIES + "\t" + allocated
                + "\t" + sum);
    }

    public static void main(String[] args) {
        String workload = args[0];
        String impl = args[1].equals("m3loop") ? "m3" : args[1];
        int n = Integer.parseInt(args[2]);
        long[] keys = keys(n);
        if (workload.equals("getalloc")) {
            allocation(impl, keys);
            return;
        }
        String mode = workload.equals("bulk") ? args[1] : impl;
        long[] times = new long[ROUNDS];
        long checksum = 0;
        for (int r = 0; r < WARMUP + ROUNDS; r++) {
            long[] result = round(workload, mode.equals("m3loop") ? "m3loop" : impl, keys);
            if (r >= WARMUP) {
                times[r - WARMUP] = result[0];
            }
            checksum = result[1];
        }
        Arrays.sort(times);
        double nsPerOp = times[ROUNDS / 2] / (double) ops(workload, n);
        System.out.printf("%s\t%s\t%d\t%d\t%.1f\t%d%n", workload, args[1], n, ops(workload, n), nsPerOp, checksum);
    }
}
