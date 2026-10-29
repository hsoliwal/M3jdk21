// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import com.m3.tools.cpu.M3FiniteModelChecker;
import com.m3.tools.cpu.M3ConstraintCsp;
import com.m3.tools.cpu.M3StreamJoin;
import com.m3.tools.cpu.M3BloomIndex;
import com.m3.tools.cpu.M3QueueSimulation;
import com.m3.tools.cpu.M3PairwiseCases;
import com.m3.tools.cpu.M3AclMatrix;
import com.m3.tools.cpu.M3RateGovernor;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;

/** Independent nested-loop, bitset, queueing and categorical-coverage oracles. */
public final class M3IndexedSystemsContract {
    private static long checks;
    private M3IndexedSystemsContract() { }
    private static void check(boolean value) {
        if (!value) throw new AssertionError("indexed systems assertion " + checks);
        checks++;
    }
    private static void refuses(Runnable action) {
        try { action.run(); throw new AssertionError("missing refusal"); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    private static void arrays(long[] a, long[] b) { check(Arrays.equals(a,b)); }
    private static void arrays(int[] a, int[] b) { check(Arrays.equals(a,b)); }

    private static void join(Random r) {
        for (int trial = 0; trial < 7000; trial++) {
            int ln = r.nextInt(24), rn = r.nextInt(25);
            long[] left = new long[ln], right = new long[rn];
            for (int i = 0; i < ln; i++) left[i] = r.nextInt(31) - 15;
            for (int i = 0; i < rn; i++) right[i] = r.nextInt(31) - 15;
            Arrays.sort(left); Arrays.sort(right);
            long before = r.nextInt(8), after = r.nextInt(8);
            List<Integer> a = new ArrayList<>(), b = new ArrayList<>();
            for (int i = 0; i < ln; i++) for (int j = 0; j < rn; j++)
                if (right[j] >= left[i] - before && right[j] <= left[i] + after) {
                    a.add(i); b.add(j);
                }
            M3StreamJoin.Pairs pairs = M3StreamJoin.within(left, right, before, after, 1000);
            check(pairs.size() == a.size());
            for (int i = 0; i < a.size(); i++) {
                check(pairs.left()[i] == a.get(i));
                check(pairs.right()[i] == b.get(i));
            }
            int[] detached = pairs.left();
            if (detached.length != 0) { detached[0] = -12; check(pairs.left()[0] >= 0); }
        }
        refuses(() -> M3StreamJoin.within(new long[]{2,1}, new long[0], 0,0,2));
        try {
            M3StreamJoin.within(new long[]{Long.MAX_VALUE},new long[]{1},0,1,20);
            throw new AssertionError("join overflow must refuse");
        } catch (ArithmeticException expected) { checks++; }
        refuses(() -> M3StreamJoin.within(new long[]{3},new long[]{3},0,0,0));
    }

    private static void bloom(Random r) throws Exception {
        for (int t = 0; t < 2600; t++) {
            int count = r.nextInt(80), hashes = 1 + r.nextInt(8);
            long[] inserted = new long[count];
            for (int i = 0; i < count; i++) inserted[i] = r.nextLong();
            M3BloomIndex filter = M3BloomIndex.build(inserted, 256 << r.nextInt(6), hashes);
            arrays(filter.snapshotBits(), filter.snapshotBits());
            for (long id : inserted) check(filter.mightContain(id));
            long[] detached = filter.snapshotBits();
            if (detached.length > 0) { Arrays.fill(detached, 0); for (long id : inserted) check(filter.mightContain(id)); }
            check(filter.hashes() == hashes && filter.insertedCount() == count);
        }
        // Immutable index can be shared by readers; no retained mutable input array.
        long[] source = {0, 1, Long.MIN_VALUE, Long.MAX_VALUE};
        M3BloomIndex shared = M3BloomIndex.build(source, 1024, 5);
        source[0] = 200;
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int i = 0; i < 5; i++) tasks.add(pool.submit(() -> {
                for (int j = 0; j < 3000; j++) if (!shared.mightContain(0)) return false;
                return true;
            }));
            for (var future : tasks) check(future.get());
        }
        refuses(() -> M3BloomIndex.build(new long[0], 96, 3));
        refuses(() -> M3BloomIndex.build(new long[0], 64, 0));
    }

    private static void queue(Random r) {
        for (int t = 0; t < 6500; t++) {
            int n = r.nextInt(90), workers = 1 + r.nextInt(9);
            long[] arrivals = new long[n], durations = new long[n];
            for (int i = 0; i < n; i++) {
                arrivals[i] = r.nextInt(150);
                durations[i] = 1 + r.nextInt(25);
            }
            Arrays.sort(arrivals);
            M3QueueSimulation.Result actual = M3QueueSimulation.fifo(arrivals, durations, workers);
            long[] ready = new long[workers], expectedStart = new long[n], expectedEnd = new long[n];
            int[] expectedWorker = new int[n];
            long wait = 0, makespan = 0;
            for (int job = 0; job < n; job++) {
                int worker = 0;
                for (int k = 1; k < workers; k++) if (ready[k] < ready[worker]) worker = k;
                expectedWorker[job] = worker;
                expectedStart[job] = Math.max(arrivals[job], ready[worker]);
                expectedEnd[job] = expectedStart[job] + durations[job];
                wait += expectedStart[job] - arrivals[job];
                makespan = Math.max(makespan, expectedEnd[job]);
                ready[worker] = expectedEnd[job];
            }
            arrays(expectedStart, actual.started());
            arrays(expectedEnd, actual.finished());
            arrays(expectedWorker, actual.worker());
            check(actual.totalWait() == wait && actual.makespan() == makespan);
        }
        refuses(() -> M3QueueSimulation.fifo(new long[]{3,2},new long[]{1,1},2));
        refuses(() -> M3QueueSimulation.fifo(new long[]{0},new long[]{0},2));
        try {
            M3QueueSimulation.fifo(new long[]{Long.MAX_VALUE}, new long[]{1}, 1);
            throw new AssertionError("long overflow must refuse");
        } catch (ArithmeticException expected) { checks++; }
    }

    private static void pairwise(Random r) {
        for (int t = 0; t < 4200; t++) {
            int n = 2 + r.nextInt(7);
            int[] levels = new int[n];
            for (int i = 0; i < n; i++) levels[i] = 1 + r.nextInt(6);
            int expected = 0;
            for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++)
                expected += levels[i] * levels[j];
            M3PairwiseCases.Cases cases = M3PairwiseCases.cover(levels, 65_536);
            check(cases.rows() == expected && cases.parameters() == n);
            for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) {
                boolean[] seen = new boolean[levels[i] * levels[j]];
                for (int row = 0; row < cases.rows(); row++) {
                    int a = cases.at(row, i), b = cases.at(row, j);
                    check(a >= 0 && a < levels[i] && b >= 0 && b < levels[j]);
                    seen[a * levels[j] + b] = true;
                }
                for (boolean value : seen) check(value);
            }
            int[] copy = cases.values(); copy[0] = -1;
            check(cases.at(0,0) >= 0);
        }
        refuses(() -> M3PairwiseCases.cover(new int[]{4}, 20));
        refuses(() -> M3PairwiseCases.cover(new int[]{5,5}, 24));
        refuses(() -> M3PairwiseCases.cover(new int[]{0,4}, 50));
        M3PairwiseCases.Cases shape = M3PairwiseCases.cover(new int[]{2,3}, 16);
        for (int[] coordinate : new int[][]{{-1,0},{0,-1},{shape.rows(),0},{0,shape.parameters()},{1,-1}}) {
            try {
                shape.at(coordinate[0], coordinate[1]);
                throw new AssertionError("pairwise coordinate must refuse");
            } catch (IndexOutOfBoundsException expected) { checks++; }
        }
    }

    private static void acl(Random r) {
        for (int t = 0; t < 8000; t++) {
            int resources = r.nextInt(90), actions = 1 + r.nextInt(16);
            long[] grants = new long[resources * actions], denials = new long[grants.length];
            for (int i = 0; i < grants.length; i++) {
                grants[i] = r.nextLong(); denials[i] = r.nextLong();
            }
            long roles = r.nextLong();
            long[] allowed = M3AclMatrix.allowed(resources, actions, grants, denials, roles);
            check(allowed.length == (grants.length + 63) / 64);
            for (int res = 0; res < resources; res++) for (int action = 0; action < actions; action++) {
                int index = res * actions + action;
                boolean expected = (grants[index] & roles) != 0 && (denials[index] & roles) == 0;
                check(M3AclMatrix.at(allowed, resources, res, action, actions) == expected);
            }
            for (long value : M3AclMatrix.allowed(resources, actions, grants, denials, 0)) check(value == 0L);
        }
        refuses(() -> M3AclMatrix.allowed(2,2,new long[4],new long[3],1));
        refuses(() -> M3AclMatrix.at(new long[1],1,1,0,1));
    }

    private static void rate(Random r) {
        for (int trial = 0; trial < 14000; trial++) {
            int n = r.nextInt(85);
            long cap = 1 + r.nextInt(100), rate = r.nextInt(9);
            long[] tick = new long[n], cost = new long[n];
            for (int i = 0; i < n; i++) {
                tick[i] = (i == 0 ? 0 : tick[i - 1]) + r.nextInt(5);
                cost[i] = r.nextInt(120);
            }
            M3RateGovernor.Decisions result = M3RateGovernor.tokenBucket(tick,cost,cap,rate);
            long balance = cap, last = 0;
            int accepted = 0;
            for (int i = 0; i < n; i++) {
                for (long time = last; time < tick[i]; time++) balance = Math.min(cap, balance + rate);
                boolean admit = balance >= cost[i];
                if (admit) { balance -= cost[i]; accepted++; }
                check(((result.accepted()[i >>> 6] >>> (i & 63)) & 1L) == (admit ? 1L : 0L));
                check(result.balanceAfter()[i] == balance);
                last = tick[i];
            }
            check(result.acceptedCount() == accepted);
        }
        M3RateGovernor.Decisions extreme = M3RateGovernor.tokenBucket(
                new long[]{0, Long.MAX_VALUE}, new long[]{Long.MAX_VALUE, 1},
                Long.MAX_VALUE, Long.MAX_VALUE);
        check(extreme.acceptedCount() == 2 && extreme.balanceAfter()[1] == Long.MAX_VALUE - 1);
        refuses(() -> M3RateGovernor.tokenBucket(new long[]{2,1},new long[]{1,1},2,1));
        refuses(() -> M3RateGovernor.tokenBucket(new long[]{1},new long[]{-1},2,1));
    }

    public static void main(String[] ignored) throws Exception {
        Random random = new Random(0x53A11A4C2026L);
        join(random); bloom(random); queue(random); pairwise(random); acl(random); rate(random);
        System.out.println("M3_WAVE3_INDEXED_SYSTEMS_PASS checks=" + checks);
    }
}
