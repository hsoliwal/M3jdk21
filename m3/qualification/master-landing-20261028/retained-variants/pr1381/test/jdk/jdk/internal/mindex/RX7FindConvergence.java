/* Copyright (c) 2026, Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* @test
 * @summary Leftmost-longest lexical find retains competitive earlier origins and validates full input
 * @modules java.base/jdk.internal.mindex
 * @run main RX7FindConvergence
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import jdk.internal.mindex.M3RX7;

public final class RX7FindConvergence {
    private static long checks;
    private static final String ROOT = "0".repeat(64);
    private RX7FindConvergence() { }
    private record Edge(int from, int to, int kind) { }
    private static final class Graph {
        final int states;
        final int start;
        final byte[] accepting;
        final List<Edge> edges = new ArrayList<>();
        Graph(int states, int start) { this.states = states; this.start = start; accepting = new byte[states]; }
        Graph edge(int from, int to, int kind) { edges.add(new Edge(from, to, kind)); return this; }
        M3RX7.LexicalPlan plan() {
            int[] offsets = new int[states + 1], target = new int[edges.size()], kind = new int[edges.size()];
            long[] ids = new long[edges.size()]; Arrays.fill(ids, -1);
            int[] flags = new int[edges.size()];
            int index = 0;
            for (int state = 0; state < states; state++) {
                offsets[state] = index;
                for (Edge edge : edges) if (edge.from == state) {
                    target[index] = edge.to; kind[index++] = edge.kind;
                }
            }
            offsets[states] = index;
            return M3RX7.fromLexicalNfa(ROOT, start, offsets, target, kind, ids, flags, flags, accepting);
        }
        boolean matches(int[] input, int from, int to) {
            boolean[] active = new boolean[states]; active[start] = true;
            for (int pos = from; ; pos++) {
                boolean changed;
                do {
                    changed = false;
                    for (Edge e : edges) if (e.kind == -2 && active[e.from] && !active[e.to]) {
                        active[e.to] = true; changed = true;
                    }
                } while (changed);
                if (pos == to) {
                    for (int s = 0; s < states; s++) if (active[s] && accepting[s] != 0) return true;
                    return false;
                }
                boolean[] next = new boolean[states];
                for (Edge e : edges) if (active[e.from] && e.kind != -2
                        && (e.kind == -1 || e.kind == input[pos])) next[e.to] = true;
                active = next;
            }
        }
        long find(int[] input, int from, int to) {
            for (int first = from; first <= to; first++) {
                for (int last = to; last >= first; last--) {
                    if (matches(input, first, last)) return range(first, last);
                }
            }
            return -1;
        }
    }
    private static long range(int a, int b) { return ((long) a << 32) | b; }
    private static void equal(long expected, long actual) {
        checks++; if (expected != actual) throw new AssertionError("find expected=" + expected + " got=" + actual);
    }
    private static void refusal(Runnable op) {
        try { op.run(); } catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("invalid suffix accepted");
    }
    private static void generated() {
        Random r = new Random(0x52583746494e44L);
        for (int trial = 0; trial < 1400; trial++) {
            int states = 1 + r.nextInt(7);
            Graph g = new Graph(states, r.nextInt(states));
            for (int s = 0; s < states; s++) {
                g.accepting[s] = (byte) (r.nextInt(3) == 0 ? 1 : 0);
                for (int n = r.nextInt(5); n > 0; n--) g.edge(s, r.nextInt(states), r.nextInt(5) - 2);
            }
            var plan = g.plan();
            for (int sample = 0; sample < 24; sample++) {
                int[] input = new int[r.nextInt(12)];
                for (int i = 0; i < input.length; i++) input[i] = r.nextInt(3);
                long[] ids = new long[input.length]; int[] flags = new int[input.length];
                for (int from = 0; from <= input.length; from++) {
                    int to = from + r.nextInt(input.length - from + 1);
                    equal(g.find(input, from, to), plan.find(input, ids, flags, from, to));
                }
                equal(g.find(input, 0, input.length), plan.find(input, ids, flags));
            }
        }
    }
    private static void adversarial() {
        Graph g = new Graph(5, 0).edge(0, 1, 0).edge(1, 1, 0).edge(1, 2, 1)
                .edge(0, 3, 1).edge(3, 4, 2);
        g.accepting[2] = 1; g.accepting[3] = 1; g.accepting[4] = 1;
        for (int[] input : List.of(new int[]{0, 0, 1, 2, 2}, new int[]{1, 2, 2},
                new int[]{2, 0, 0, 1}, new int[]{2, 2, 2})) {
            equal(g.find(input, 0, input.length), g.plan().find(input, new long[input.length], new int[input.length]));
        }
        Graph earlier = new Graph(4, 0).edge(0, 1, 0).edge(1, 1, 0).edge(1, 2, 1).edge(0, 3, 0);
        earlier.accepting[2] = 1; earlier.accepting[3] = 1;
        int[] input = {0, 0, 0, 1, 2};
        equal(range(0, 4), earlier.plan().find(input, new long[5], new int[5]));
        Graph empty = new Graph(1, 0); empty.accepting[0] = 1;
        equal(range(0, 0), empty.plan().find(new int[]{0, 1}, new long[2], new int[2]));
        Graph loop = new Graph(1, 0).edge(0, 0, -1); loop.accepting[0] = 1;
        equal(range(0, 100), loop.plan().find(new int[100], new long[100], new int[100]));
        Graph literal = new Graph(2, 0).edge(0, 1, 1); literal.accepting[1] = 1;
        var p = literal.plan();
        refusal(() -> p.find(new int[]{1, 2, -1}, new long[3], new int[3]));
        refusal(() -> p.find(new int[]{1, 2, 2}, new long[]{0, 0, -1}, new int[3]));
        for (int size : new int[]{0, 1, 2, 63, 64, 65, 9999}) {
            int[] seq = new int[size]; if (size > 0) seq[0] = 1;
            equal(size == 0 ? -1 : range(0, 1), p.find(seq, new long[size], new int[size]));
        }
    }
    public static void main(String[] args) {
        generated(); adversarial();
        System.out.println("RX7_FIND_CONVERGENCE_PASS checks=" + checks);
    }
}
