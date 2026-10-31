// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import com.m3.tools.cpu.M3CnfSat;
import com.m3.tools.cpu.M3ArrayGraph;
import com.m3.tools.cpu.M3StreamMath;
import com.m3.tools.cpu.M3EvidenceMath;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Independent seeded exhaustive/brute-force differential witnesses; no JUnit runtime required. */
public final class M3StructuredAtomsContract {
    private static long checks;
    private static final Random RANDOM = new Random(0x4D335354525543L);
    private M3StructuredAtomsContract() { }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message + " case=" + checks);
        checks++;
    }

    private static void expectRefusal(Runnable op) {
        boolean refused = false;
        try { op.run(); }
        catch (IllegalArgumentException | IllegalStateException | ArithmeticException | IndexOutOfBoundsException expected) { refused = true; }
        check(refused, "missing refusal");
    }

    private static boolean bruteSat(int variables, int[] offsets, int[] literals) {
        for (int mask = 0; mask < (1 << variables); mask++) {
            boolean all = true;
            for (int clause = 0; clause < offsets.length - 1; clause++) {
                boolean any = false;
                for (int j = offsets[clause]; j < offsets[clause + 1]; j++) {
                    int literal = literals[j];
                    boolean assigned = ((mask >>> (Math.abs(literal) - 1)) & 1) == 1;
                    any |= literal > 0 ? assigned : !assigned;
                }
                all &= any;
            }
            if (all) return true;
        }
        return false;
    }

    private static void sat() {
        var tautology = M3CnfSat.compile(2, new int[]{0, 2}, new int[]{1, -1});
        check(tautology.solve(200).satisfiable(), "tautology");
        var unsat = M3CnfSat.compile(1, new int[]{0, 1, 2}, new int[]{1, -1});
        check(!unsat.solve(100).satisfiable(), "unit contradiction");
        check(!M3CnfSat.compile(0, new int[]{0, 0}, new int[]{}).solve(100).satisfiable(), "empty clause");
        check(M3CnfSat.compile(0, new int[]{0}, new int[]{}).verifies(new byte[]{0}), "empty solution");
        expectRefusal(() -> M3CnfSat.compile(1, new int[]{0, 1}, new int[]{2}));
        expectRefusal(() -> M3CnfSat.compile(1, new int[]{0, 3}, new int[]{1}));
        expectRefusal(() -> tautology.solve(0));
        for (int iteration = 0; iteration < 20_000; iteration++) {
            int variables = RANDOM.nextInt(8) + 1;
            int clauses = RANDOM.nextInt(20);
            int[] offsets = new int[clauses + 1];
            int count = 0;
            for (int c = 0; c < clauses; c++) {
                count += RANDOM.nextInt(5);
                offsets[c + 1] = count;
            }
            int[] literals = new int[count];
            for (int i = 0; i < count; i++) {
                int variable = 1 + RANDOM.nextInt(variables);
                literals[i] = RANDOM.nextBoolean() ? variable : -variable;
            }
            int[] pre = literals.clone(), preOffsets = offsets.clone();
            M3CnfSat solver = M3CnfSat.compile(variables, offsets, literals);
            boolean expected = bruteSat(variables, offsets, literals);
            var result = solver.solve(10_000);
            check(result.satisfiable() == expected, "SAT correctness");
            if (expected) {
                check(solver.verifies(result.assignment()), "SAT witness");
                byte[] external = result.assignment();
                external[1] = 0;
                check(solver.verifies(result.assignment()), "SAT witness detached");
            }
            Arrays.fill(offsets, -99);
            Arrays.fill(literals, Integer.MAX_VALUE);
            check(solver.solve(10_000).satisfiable() == expected, "SAT source snapshot");
            check(Arrays.equals(preOffsets, preOffsets.clone()) && Arrays.equals(pre, pre.clone()), "input integrity");
        }
        // Exactly one tree node is insufficient for a genuine two-branch conflict search.
        var hard = M3CnfSat.compile(2, new int[]{0,2,4,6,8}, new int[]{1,2,1,-2,-1,2,-1,-2});
        expectRefusal(() -> hard.solve(1));
    }

    private static boolean[][] reachability(int n, int[] offsets, int[] targets) {
        boolean[][] reach = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            reach[i][i] = true;
            for (int e = offsets[i]; e < offsets[i + 1]; e++) reach[i][targets[e]] = true;
        }
        for (int k = 0; k < n; k++)
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++) reach[i][j] |= reach[i][k] && reach[k][j];
        return reach;
    }

    private static void graph() {
        check(M3ArrayGraph.stronglyConnectedComponents(new int[]{0}, new int[]{}).length == 0,
                "empty CSR");
        expectRefusal(() -> M3ArrayGraph.stronglyConnectedComponents(new int[]{0, 2}, new int[]{0}));
        expectRefusal(() -> M3ArrayGraph.reachableMask(new int[]{0, 0}, new int[]{}, 1));
        for (int iteration = 0; iteration < 12_000; iteration++) {
            int n = RANDOM.nextInt(12) + 1;
            int[] offsets = new int[n + 1];
            int count = 0;
            for (int node = 0; node < n; node++) {
                count += RANDOM.nextInt(n + 1);
                offsets[node + 1] = count;
            }
            int[] targets = new int[count];
            for (int i = 0; i < count; i++) targets[i] = RANDOM.nextInt(n);
            boolean[][] oracle = reachability(n, offsets, targets);
            int[] labels = M3ArrayGraph.stronglyConnectedComponents(offsets, targets);
            int[] minimum = new int[n]; Arrays.fill(minimum, Integer.MAX_VALUE);
            for (int i = 0; i < n; i++) minimum[labels[i]] = Math.min(minimum[labels[i]], i);
            for (int i = 0; i < n; i++) {
                long[] mask = M3ArrayGraph.reachableMask(offsets, targets, i);
                for (int j = 0; j < n; j++) {
                    check(((mask[j >>> 6] >>> (j & 63)) & 1L) != 0 == oracle[i][j], "reachability");
                    check((labels[i] == labels[j]) == (oracle[i][j] && oracle[j][i]), "SCC equivalence");
                }
            }
            for (int c = 0; c < n; c++) if (minimum[c] != Integer.MAX_VALUE)
                check(c == 0 || minimum[c] > minimum[c - 1], "SCC canonical labels");
        }
    }

    private static long bruteMinCut(int n, int[] from, int[] to, long[] capacity,
                                    int source, int sink) {
        long best = Long.MAX_VALUE;
        for (int mask = 0; mask < (1 << n); mask++) {
            if (((mask >>> source) & 1) == 0 || ((mask >>> sink) & 1) != 0) continue;
            long value = 0;
            for (int i = 0; i < from.length; i++)
                if (((mask >>> from[i]) & 1) == 1 && ((mask >>> to[i]) & 1) == 0)
                    value += capacity[i];
            best = Math.min(best, value);
        }
        return best;
    }

    private static void flow() {
        check(M3ArrayGraph.maximumFlow(4,new int[]{0,0,1,2,1},new int[]{1,2,3,3,2},
                new long[]{3,2,2,4,1},0,3)==5,"flow example");
        expectRefusal(() -> M3ArrayGraph.maximumFlow(2,new int[]{0},new int[]{1},new long[]{-1},0,1));
        expectRefusal(() -> M3ArrayGraph.maximumFlow(2,new int[]{0},new int[]{1},new long[]{1},0,0));
        for (int trial=0;trial<7000;trial++) {
            int n=2+RANDOM.nextInt(6), m=RANDOM.nextInt(24);
            int[] from=new int[m], to=new int[m]; long[] capacity=new long[m];
            for (int i=0;i<m;i++) { from[i]=RANDOM.nextInt(n);to[i]=RANDOM.nextInt(n);
                capacity[i]=RANDOM.nextInt(10); }
            long actual=M3ArrayGraph.maximumFlow(n,from,to,capacity,0,n-1);
            check(actual==bruteMinCut(n,from,to,capacity,0,n-1),"max-flow/min-cut oracle");
        }
    }

    private static void close(double expected,double actual,double epsilon,String name) {
        check(Double.isFinite(actual) && Math.abs(expected-actual)<=epsilon,name+" expected="+expected+" got="+actual);
    }

    private static void stats() {
        check(Arrays.equals(M3StreamMath.histogram(new int[]{2,0,2,1},3),new long[]{1,1,2}),"histogram");
        close(1,M3StreamMath.entropyBits(new long[]{1,1}),1e-12,"entropy fair");
        close(.5,M3StreamMath.gini(new long[]{1,1}),1e-12,"gini fair");
        close(1,M3StreamMath.jsDivergence(new long[]{1,0},new long[]{0,1}),1e-12,"JS disjoint");
        close(0,M3StreamMath.jsDivergence(new long[]{1,1},new long[]{2,2}),1e-12,"JS same");
        expectRefusal(() -> M3StreamMath.histogram(new int[]{2},2));
        expectRefusal(() -> M3StreamMath.ewma(new double[]{1},Double.NaN));
        expectRefusal(() -> M3StreamMath.jsDivergence(new long[]{1},new long[]{0}));
        expectRefusal(() -> M3StreamMath.entropyBits(new long[]{-1}));
        for (int trial=0;trial<20_000;trial++) {
            int n=1+RANDOM.nextInt(30), w=1+RANDOM.nextInt(n);
            double[] inputs=new double[n];
            for (int i=0;i<n;i++) inputs[i]=RANDOM.nextDouble()*40-20;
            double[] moving=M3StreamMath.movingMean(inputs,w);
            for (int i=0;i<=n-w;i++) {
                double sum=0;
                for (int j=0;j<w;j++) sum+=inputs[i+j];
                close(sum/w,moving[i],1e-9,"moving mean");
            }
            double alpha=RANDOM.nextDouble();
            double[] smoothed=M3StreamMath.ewma(inputs,alpha);
            double mean=0;
            for (int i=0;i<n;i++) {
                mean=i==0?inputs[0]:mean+alpha*(inputs[i]-mean);
                close(mean,smoothed[i],1e-10,"EWMA");
            }
            int bins=1+RANDOM.nextInt(20);
            int[] labels=new int[n]; long[] oracle=new long[bins];
            for (int i=0;i<n;i++) { labels[i]=RANDOM.nextInt(bins);oracle[labels[i]]++; }
            check(Arrays.equals(oracle,M3StreamMath.histogram(labels,bins)),"histogram random");
        }
    }

    public static void main(String[] args) throws Exception {
        sat(); graph(); flow(); stats();
        // Immutable compiled SAT plans and independent graph operands must be safe across readers.
        M3CnfSat compiled=M3CnfSat.compile(3,new int[]{0,2,4},new int[]{1,2,-2,3});
        try (ExecutorService workers=Executors.newVirtualThreadPerTaskExecutor()) {
            @SuppressWarnings("unchecked") Future<Boolean>[] jobs=(Future<Boolean>[])new Future<?>[16];
            for(int i=0;i<jobs.length;i++) jobs[i]=workers.submit(()->{
                for(int k=0;k<200;k++) if(!compiled.verifies(compiled.solve(100).assignment())) return false;
                return true;
            });
            for(Future<Boolean> job:jobs) check(job.get(),"concurrent SAT compiled plan");
        }
        System.out.println("M3_STRUCTURED_ATOMS_PASS checks="+checks);
    }
}
