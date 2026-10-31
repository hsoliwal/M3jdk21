// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import com.m3.tools.cpu.*;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** Independent monotone-closure, explicit-state BFS, brute-force assignment references. */
public final class M3LogicFlowContract {
    private M3LogicFlowContract() { }
    private static int checks;
    private static final Random RANDOM = new Random(0x4d3352554c45534cL);
    private static void ok(boolean condition) {
        if (!condition) throw new AssertionError("M3LogicFlowContract case=" + checks);
        checks++;
    }
    private static void rejects(Runnable action) {
        try { action.run(); throw new AssertionError("expected refusal"); }
        catch (IllegalArgumentException | IllegalStateException expected) { checks++; }
    }

    public static void main(String[] args) {
        horn(); petri(); assignment();
        System.out.println("M3_LOGIC_FLOW_PASS checks=" + checks);
    }

    private static void horn() {
        for (int t = 0; t < 2800; t++) {
            int facts = RANDOM.nextInt(12), ruleCount = facts == 0 ? 0 : RANDOM.nextInt(30);
            int[] offsets = new int[ruleCount + 1], heads = new int[ruleCount];
            int[] tmp = new int[ruleCount * 4]; int n = 0;
            for (int r = 0; r < ruleCount; r++) {
                offsets[r] = n;
                heads[r] = RANDOM.nextInt(facts);
                int width = RANDOM.nextInt(5);
                for (int j = 0; j < width; j++) tmp[n++] = RANDOM.nextInt(facts);
            }
            offsets[ruleCount] = n;
            int[] pre = Arrays.copyOf(tmp, n);
            long[] initial = new long[(facts + 63) >>> 6];
            for (int fact = 0; fact < facts; fact++) if (RANDOM.nextBoolean()) initial[fact >>> 6] |= 1L << (fact & 63);
            M3HornRules.Closure actual = M3HornRules.close(facts, initial, offsets, pre, heads);
            boolean[] reference = new boolean[facts];
            for (int i = 0; i < facts; i++) reference[i] = (initial[i >>> 6] & (1L << (i & 63))) != 0;
            for (int pass = 0; pass <= facts; pass++) {
                boolean changed = false;
                for (int r = 0; r < ruleCount; r++) {
                    boolean ready = true;
                    for (int i = offsets[r]; i < offsets[r + 1]; i++) ready &= reference[pre[i]];
                    if (ready && !reference[heads[r]]) { reference[heads[r]] = true; changed = true; }
                }
                if (!changed) break;
            }
            for (int fact = 0; fact < facts; fact++) {
                ok(actual.has(fact) == reference[fact]);
                if (actual.firstRule()[fact] >= 0) ok(heads[actual.firstRule()[fact]] == fact);
            }
            ok(actual.firedRules() <= ruleCount);
            if (facts > 0) {
                int[] rules = actual.firstRule(); rules[0] = 9999;
                ok(actual.firstRule()[0] != 9999);
            }
        }
        rejects(() -> M3HornRules.close(1,new long[]{2},new int[]{0},new int[0],new int[0]));
        rejects(() -> M3HornRules.close(1,new long[1],new int[]{0,1},new int[]{2},new int[]{0}));
    }

    private static void petri() {
        for (int t = 0; t < 1800; t++) {
            int places = RANDOM.nextInt(7), transitions = RANDOM.nextInt(9);
            long domain = (1L << places) - 1;
            long initial = RANDOM.nextLong() & domain;
            long[] pre = new long[transitions], post = new long[transitions];
            for (int i = 0; i < transitions; i++) {
                pre[i] = RANDOM.nextLong() & domain;
                post[i] = RANDOM.nextLong() & domain;
            }
            M3PetriState.Graph graph = M3PetriState.explore(places, initial, pre, post, 1 << places);
            Set<Long> expected = new HashSet<>();
            ArrayDeque<Long> queue = new ArrayDeque<>();
            queue.add(initial); expected.add(initial);
            while (!queue.isEmpty()) {
                long marking = queue.removeFirst();
                for (int op = 0; op < transitions; op++) {
                    if ((marking & pre[op]) != pre[op]) continue;
                    long leftover = marking & ~pre[op];
                    if ((leftover & post[op]) != 0) continue;
                    long next = leftover | post[op];
                    if (expected.add(next)) queue.addLast(next);
                }
            }
            long[] actual = graph.markings();
            ok(actual.length == expected.size());
            ok(actual[0] == initial);
            for (int i = 0; i < actual.length; i++) {
                ok(expected.contains(actual[i]));
                if (i > 0) {
                    int parent = graph.parent()[i], via = graph.transition()[i];
                    ok(parent >= 0 && parent < i && via >= 0 && via < transitions);
                    if (parent >= 0 && parent < i && via >= 0 && via < transitions)
                        ok(((actual[parent] & ~pre[via]) | post[via]) == actual[i]);
                }
            }
            if (actual.length > 0) {
                actual[0] = -1;
                ok(graph.markings()[0] == initial);
            }
        }
        rejects(() -> M3PetriState.explore(1,0,new long[]{0},new long[]{1},1));
        rejects(() -> M3PetriState.explore(2,0,new long[]{4},new long[]{1},3));
    }

    private static void assignment() {
        for (int t = 0; t < 2800; t++) {
            int workers = RANDOM.nextInt(8);
            long[] costs = new long[workers * workers];
            for (int i = 0; i < costs.length; i++) costs[i] = RANDOM.nextInt(5) == 0
                    ? M3AssignmentPlan.FORBIDDEN : RANDOM.nextInt(15);
            M3AssignmentPlan.Plan plan = M3AssignmentPlan.minCost(workers, costs);
            Reference expected = new Reference();
            brute(costs, workers, 0, 0, 0, new int[workers], expected);
            ok(plan.feasible() == (expected.assignment != null));
            if (expected.assignment != null) {
                ok(plan.cost() == expected.cost);
                ok(Arrays.equals(plan.taskByWorker(), expected.assignment));
                int[] assignment = plan.taskByWorker();
                if (assignment.length > 0) {
                    assignment[0] = -5;
                    ok(plan.taskByWorker()[0] >= 0);
                }
            }
        }
        rejects(() -> M3AssignmentPlan.minCost(19,new long[361]));
        rejects(() -> M3AssignmentPlan.minCost(1,new long[]{-1}));
        ok(M3AssignmentPlan.minCost(0, new long[0]).feasible());
    }

    private static final class Reference {
        long cost = Long.MAX_VALUE;
        int[] assignment;
    }
    private static void brute(long[] costs, int n, int worker, int used, long total,
                              int[] chosen, Reference best) {
        if (worker == n) {
            if (best.assignment == null || total < best.cost || (total == best.cost
                    && lexicographic(chosen, best.assignment) < 0)) {
                best.cost = total; best.assignment = chosen.clone();
            }
            return;
        }
        for (int task = 0; task < n; task++) if ((used & (1 << task)) == 0) {
            long cost = costs[worker * n + task];
            if (cost == Long.MAX_VALUE || cost > Long.MAX_VALUE - total) continue;
            chosen[worker] = task;
            brute(costs, n, worker + 1, used | (1 << task), total + cost, chosen, best);
        }
    }
    private static int lexicographic(int[] a, int[] b) {
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return Integer.compare(a[i], b[i]);
        return 0;
    }
}
