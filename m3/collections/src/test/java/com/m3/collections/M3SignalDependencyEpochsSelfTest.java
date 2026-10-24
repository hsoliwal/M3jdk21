/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Independent, deterministic source-owner contract executable without a JUnit dependency. */
public final class M3SignalDependencyEpochsSelfTest {
    private static long checks;

    private M3SignalDependencyEpochsSelfTest() { }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void invalid(Runnable action, String label) {
        boolean threw = false;
        try { action.run(); } catch (IndexOutOfBoundsException | IllegalArgumentException expected) {
            threw = true;
        }
        check(threw, label);
    }

    public static void main(String[] args) throws Exception {
        exerciseSmall();
        exerciseRandom();
        exerciseDeepChain();
        exerciseConcurrentMutations();
        System.out.println("M3_SIGNAL_DEPENDENCY_EPOCHS_PASS checks=" + checks);
    }

    private static void exerciseSmall() {
        M3SignalRelations graph = M3SignalRelations.of(7,
                new int[]{0,2,3,3,4,4,5}, new int[]{1,1,2,2,4,1,1},
                new byte[]{1,1,1,1,1,2,2}, new int[]{1,1,1,4,1,1,1});
        M3SignalDependencyEpochs direct = M3SignalDependencyEpochs.from(graph, false);
        M3SignalDependencyEpochs candidates = M3SignalDependencyEpochs.from(graph, true);
        check(direct.rowCount() == 7 && direct.dependencyEdgeCount() == 5, "direct capacity");
        check(candidates.dependencyEdgeCount() == 7, "candidate capacity");
        int[] selected = {0,1,2,3,4,5,6,1};
        long[] old = direct.capture(selected);
        check(direct.unchanged(selected, old), "initial capture");
        check(direct.invalidate() == 0, "empty mutation no epoch");
        check(direct.invalidate(1,1) == 1, "unique invalidation generation");
        check(Arrays.equals(direct.changedRowsSince(0), new int[]{0,1,2,3}), "reverse transitive");
        check(direct.stamp(0) == 1 && direct.stamp(6) == 0, "independent rows");
        check(!direct.unchanged(selected, old), "stale capture");
        check(direct.unchanged(6, 0), "unrelated stamp remains valid");
        check(!direct.unchanged(6, 9) && !direct.unchanged(6, -1), "impossible stamps");
        invalid(() -> direct.invalidate(6, -1), "atomic validation");
        check(direct.epoch() == 1, "no partial mutation");
        invalid(() -> direct.capture(new int[]{1,7}), "atomic capture validation");
        check(direct.invalidate(4) == 2, "self cycle");
        check(Arrays.equals(direct.changedRowsSince(1), new int[]{4}), "self cycle isolated");
        check(candidates.invalidate(1) == 1, "candidate mutation");
        check(Arrays.equals(candidates.changedRowsSince(0), new int[]{0,1,2,3,4,5}), "candidate propagation");
        check(candidates.invalidateAll() == 2, "global fallback");
        check(candidates.changedRowsSince(1).length == 7, "all rows invalidated");
        long[] newStamps = candidates.capture(selected);
        check(candidates.unchanged(selected, newStamps), "fresh read");
        invalid(() -> candidates.unchanged(new int[]{0}, new long[]{}), "lane shape");
        invalid(() -> candidates.changedRowsSince(3), "future history");
        M3SignalDependencyEpochs empty = M3SignalDependencyEpochs.from(
                M3SignalRelations.of(0,new int[]{},new int[]{},new byte[]{},new int[]{}),true);
        check(empty.invalidateAll() == 1 && empty.changedRowsSince(0).length == 0,
                "empty image fallback");
    }

    private static void exerciseRandom() {
        Random random = new Random(0x5a17c0deL);
        for (int trial = 0; trial < 2400; trial++) {
            int n = 1 + random.nextInt(65);
            int edges = random.nextInt(n * 3 + 1);
            int[] from = new int[edges];
            int[] to = new int[edges];
            byte[] kind = new byte[edges];
            int[] counts = new int[edges];
            for (int i = 0; i < edges; i++) {
                from[i] = random.nextInt(n);
                to[i] = random.nextInt(n);
                kind[i] = random.nextBoolean() ? M3SignalRelations.DIRECT
                        : M3SignalRelations.DISPATCH_CANDIDATE;
                counts[i] = 1 + random.nextInt(20);
            }
            M3SignalRelations graph = M3SignalRelations.of(n, from,to,kind,counts);
            for (boolean include : new boolean[]{false,true}) {
                M3SignalDependencyEpochs index = M3SignalDependencyEpochs.from(graph,include);
                int seeds = 1 + random.nextInt(5);
                int[] changed = new int[seeds];
                boolean[] expected = new boolean[n];
                for (int i=0;i<seeds;i++) {
                    changed[i] = random.nextInt(n);
                    expected[changed[i]] = true;
                }
                boolean progress;
                do {
                    progress = false;
                    for (int edge=0;edge<edges;edge++) {
                        if ((include || kind[edge] == M3SignalRelations.DIRECT)
                                && expected[to[edge]] && !expected[from[edge]]) {
                            expected[from[edge]] = true;
                            progress = true;
                        }
                    }
                } while (progress);
                long[] baseline = index.capture(identity(n));
                check(index.invalidate(changed)==1, "random generation");
                int observed = 0;
                for (int row=0;row<n;row++) {
                    check((index.stamp(row)==1)==expected[row], "seeded oracle " + trial);
                    check(index.unchanged(row,baseline[row]) != expected[row], "oracle freshness");
                    if(expected[row]) observed++;
                }
                int[] actual = index.changedRowsSince(0);
                check(actual.length == observed, "change cardinality");
                for (int row : actual) check(expected[row], "change membership");
            }
        }
    }

    private static int[] identity(int n) {
        int[] result = new int[n];
        for(int i=0;i<n;i++) result[i] = i;
        return result;
    }

    private static void exerciseDeepChain() {
        final int n = 50_000;
        int[] from = new int[n-1], to = new int[n-1], counts = new int[n-1];
        byte[] kinds = new byte[n-1];
        for(int i=0;i<n-1;i++) {
            from[i]=i; to[i]=i+1; kinds[i]=M3SignalRelations.DIRECT; counts[i]=1;
        }
        M3SignalDependencyEpochs index = M3SignalDependencyEpochs.from(
                M3SignalRelations.of(n,from,to,kinds,counts),false);
        check(index.invalidate(n-1)==1, "deep chain generation");
        check(index.changedRowsSince(0).length==n, "iterative full transitive closure");
        check(index.stamp(0)==1, "deep caller invalidated");
    }

    private static void exerciseConcurrentMutations() throws Exception {
        final int tasks = 4;
        final int iterations = 200;
        M3SignalDependencyEpochs index = M3SignalDependencyEpochs.from(
                M3SignalRelations.of(4, new int[]{},new int[]{},new byte[]{},new int[]{}),false);
        CountDownLatch ready = new CountDownLatch(tasks), go = new CountDownLatch(1);
        AtomicReference<Throwable> problem = new AtomicReference<>();
        Thread[] threads = new Thread[tasks];
        for (int t=0;t<tasks;t++) {
            final int row = t;
            threads[t] = new Thread(() -> {
                ready.countDown();
                try {
                    go.await();
                    for(int x=0;x<iterations;x++) {
                        index.invalidate(row);
                        long now = index.stamp(row);
                        if(now<=0 || now>index.epoch()) throw new AssertionError("future stamp");
                    }
                } catch(Throwable e) { problem.compareAndSet(null,e); }
            });
            threads[t].start();
        }
        ready.await(); go.countDown();
        for(Thread t:threads) t.join();
        if(problem.get()!=null) throw new AssertionError("thread failure",problem.get());
        check(index.epoch()==(long)tasks*iterations, "serialized mutation epochs");
        for(int row=0;row<tasks;row++) check(index.stamp(row)>0, "thread row touched");
    }
}
