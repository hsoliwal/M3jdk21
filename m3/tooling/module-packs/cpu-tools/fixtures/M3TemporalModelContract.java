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

/** Independent iterative Boolean CTL oracle plus exact exhaustive graph-colouring CSP. */
public final class M3TemporalModelContract {
    private static long checks;
    private M3TemporalModelContract() { }
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("temporal/CSP oracle assertion " + checks);
        checks++;
    }
    private static void rejects(Runnable fn) {
        try { fn.run(); throw new AssertionError("expected refusal"); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    private static long[] mask(boolean[] states) {
        long[] result = new long[(states.length + 63) / 64];
        for (int i = 0; i < states.length; i++) if (states[i]) result[i >>> 6] |= 1L << (i & 63);
        return result;
    }
    private static boolean[] oracle(boolean[][] successors, boolean[] p, boolean[] q, int op) {
        int n = p.length;
        boolean[] now = new boolean[n];
        for (int i = 0; i < n; i++) {
            now[i] = switch (op) {
                case 0 -> false; // EX
                case 1, 5 -> q[i]; // EU / AU
                default -> p[i]; // EG / AG
            };
        }
        if (op == 0) {
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++) if (successors[i][j] && p[j]) now[i] = true;
            return now;
        }
        if (op == 4) { // AF: least fixed point goal or AX already-good
            now = q.clone();
        }
        for (int round = 0; round <= n; round++) {
            boolean[] next = now.clone();
            for (int i = 0; i < n; i++) {
                boolean any = false, all = true;
                for (int j = 0; j < n; j++) if (successors[i][j]) {
                    any |= now[j]; all &= now[j];
                }
                switch (op) {
                    case 1 -> next[i] |= p[i] && any; // EU
                    case 2 -> next[i] &= any; // EG
                    case 3 -> next[i] &= all; // AG
                    case 4 -> next[i] |= all; // AF, now initially q
                    case 5 -> next[i] |= p[i] && all; // AU
                    default -> throw new AssertionError(op);
                }
            }
            if (Arrays.equals(now, next)) return now;
            now = next;
        }
        throw new AssertionError("oracle fixed point did not converge");
    }
    private static boolean[] complement(boolean[] a) {
        boolean[] b = new boolean[a.length];
        for (int i = 0; i < a.length; i++) b[i] = !a[i];
        return b;
    }
    private static boolean[] expected(boolean[][] g, boolean[] p, boolean[] q, int op) {
        if (op == 3) return oracle(g, p, q, 3);
        if (op == 4) return oracle(g, p, q, 4);
        return oracle(g, p, q, op);
    }
    private static boolean bruteCsp(int n, int colors, int[] mask, int[] from, int[] to) {
        int[] value = new int[n]; Arrays.fill(value, -1);
        return assign(0, colors, mask, from, to, value);
    }
    private static boolean assign(int k, int colors, int[] mask, int[] from, int[] to, int[] values) {
        if (k == values.length) return true;
        for (int c = 0; c < colors; c++) if ((mask[k] & (1 << c)) != 0) {
            boolean valid = true;
            for (int e = 0; e < from.length; e++) {
                int a = from[e], b = to[e];
                if (a == b) { valid = false; break; }
                if ((a == k && values[b] == c) || (b == k && values[a] == c)) { valid = false; break; }
            }
            if (valid) { values[k] = c; if (assign(k + 1, colors, mask, from, to, values)) return true; }
            values[k] = -1;
        }
        return false;
    }

    public static void main(String[] ignored) {
        Random random = new Random(0xCAFE1E57202L);
        for (int trial = 0; trial < 5800; trial++) {
            int n = random.nextInt(10);
            List<Integer> edges = new ArrayList<>();
            boolean[][] g = new boolean[n][n];
            int[] offsets = new int[n + 1];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) if (random.nextInt(5) == 0) {
                    edges.add(j); g[i][j] = true;
                }
                offsets[i + 1] = edges.size();
                if (offsets[i + 1] == offsets[i]) g[i][i] = true; // implicit totalization
            }
            int[] targets = edges.stream().mapToInt(Integer::intValue).toArray();
            M3FiniteModelChecker.Graph graph = new M3FiniteModelChecker.Graph(offsets, targets);
            boolean[] p = new boolean[n], q = new boolean[n];
            for (int i = 0; i < n; i++) { p[i] = random.nextBoolean(); q[i] = random.nextBoolean(); }
            long[] pm = mask(p), qm = mask(q);
            long[][] result = {
                M3FiniteModelChecker.ex(graph, pm),
                M3FiniteModelChecker.eu(graph, pm, qm),
                M3FiniteModelChecker.eg(graph, pm),
                M3FiniteModelChecker.ag(graph, pm),
                M3FiniteModelChecker.af(graph, qm),
                M3FiniteModelChecker.au(graph, pm, qm)
            };
            for (int op = 0; op < result.length; op++) {
                boolean[] ref = expected(g, p, q, op);
                for (int i = 0; i < n; i++)
                    check(((result[op][i >>> 6] >>> (i & 63)) & 1L) == (ref[i] ? 1L : 0L));
                check(Arrays.equals(result[op], mask(ref)));
            }
            if (n > 0) {
                int[] original = graph.offsets(); original[0] = -2;
                check(graph.offsets()[0] == 0);
            }
        }
        for (int size : new int[]{0, 1, 63, 64, 65, 129, 4096}) {
            M3FiniteModelChecker.Graph dead = new M3FiniteModelChecker.Graph(new int[size + 1], new int[0]);
            boolean[] some = new boolean[size];
            for (int i = 0; i < size; i++) some[i] = (i & 1) == 0;
            long[] signal = mask(some), empty = new long[signal.length];
            check(Arrays.equals(signal, M3FiniteModelChecker.ex(dead, signal)));
            check(Arrays.equals(signal, M3FiniteModelChecker.eg(dead, signal)));
            check(Arrays.equals(signal, M3FiniteModelChecker.ag(dead, signal)));
            check(Arrays.equals(signal, M3FiniteModelChecker.af(dead, signal)));
            check(Arrays.equals(signal, M3FiniteModelChecker.eu(dead, empty, signal)));
            check(Arrays.equals(signal, M3FiniteModelChecker.au(dead, empty, signal)));
        }
        int[] manyAllowed = new int[64]; Arrays.fill(manyAllowed, 1);
        int[] sixtyFour = M3ConstraintCsp.color(64, 1, new int[0], new int[0], manyAllowed, 256);
        check(sixtyFour != null && sixtyFour.length == 64);
        rejects(() -> new M3FiniteModelChecker.Graph(new int[]{0, 2}, new int[]{0}));
        rejects(() -> M3FiniteModelChecker.ex(new M3FiniteModelChecker.Graph(new int[]{0, 0}, new int[0]), new long[]{2}));
        for (int trial = 0; trial < 4600; trial++) {
            int n = random.nextInt(8), colors = 1 + random.nextInt(4);
            int[] domains = new int[n];
            for (int i = 0; i < n; i++) domains[i] = random.nextInt(1 << colors);
            int ec = random.nextInt(16);
            if (n == 0) ec = 0;
            int[] from = new int[ec], to = new int[ec];
            for (int e = 0; e < ec; e++) { from[e] = random.nextInt(n); to[e] = random.nextInt(n); }
            boolean sat = bruteCsp(n, colors, domains, from, to);
            int[] value = M3ConstraintCsp.color(n, colors, from, to, domains, 1_000_000);
            check((value != null) == sat);
            if (value != null) check(M3ConstraintCsp.valid(value, colors, from, to, domains));
        }
        rejects(() -> M3ConstraintCsp.color(2, 17, new int[0], new int[0], new int[2], 100));
        rejects(() -> M3ConstraintCsp.color(2, 2, new int[]{0}, new int[]{2}, new int[]{3, 3}, 100));
        try {
            M3ConstraintCsp.color(8, 2, new int[0], new int[0], new int[]{3, 3, 3, 3, 3, 3, 3, 3}, 1);
            throw new AssertionError("CSP budget must refuse");
        } catch (IllegalStateException expectedBudget) { checks++; }
        System.out.println("M3_WAVE3_TEMPORAL_CSP_PASS checks=" + checks);
    }
}
