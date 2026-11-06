// SPDX-License-Identifier: Apache-2.0
import com.m3.tools.cpu.M3DataflowLiveness;
import com.m3.tools.cpu.M3CriticalPath;
import java.util.Arrays;
import java.util.Random;

/** Reference dataflow full sweeps and DAG path enumeration, including CSR refusal tests. */
public final class M3DataflowScheduleContract {
    private static int checks;
    private M3DataflowScheduleContract() { }
    private static void ok(boolean yes, String reason) {
        if (!yes) throw new AssertionError(reason + " at=" + checks);
        checks++;
    }
    private static void refuses(Runnable fn) {
        try { fn.run(); throw new AssertionError("expected refusal"); }
        catch (IllegalArgumentException | ArithmeticException expected) { checks++; }
    }
    private static void liveness(long[] use, long[] def, int[] offsets, int[] edges, int variables) {
        var result = M3DataflowLiveness.solve(variables, use, def, offsets, edges);
        long[] in = new long[use.length], out = new long[use.length];
        boolean changed;
        int pass = 0;
        do {
            if (++pass > 1000) throw new AssertionError("independent reference failed to converge");
            changed = false;
            for (int i = use.length - 1; i >= 0; i--) {
                long after = 0;
                for (int e = offsets[i]; e < offsets[i + 1]; e++) after |= in[edges[e]];
                long before = use[i] | (after & ~def[i]);
                if (before != in[i] || after != out[i]) changed = true;
                in[i] = before; out[i] = after;
            }
        } while (changed);
        ok(Arrays.equals(in,result.liveIn()), "live in oracle");
        ok(Arrays.equals(out,result.liveOut()), "live out oracle");
        if (in.length > 0) {
            result.liveIn()[0] = ~result.liveIn()[0];
            ok(Arrays.equals(in,result.liveIn()), "detached liveness state");
        }
    }
    private static void critical(long[] duration, int[] offsets, int[] target) {
        var result = M3CriticalPath.analyze(duration, offsets, target);
        int n = duration.length;
        long[] earliest = new long[n], latest = new long[n];
        for (int u = 0; u < n; u++)
            for (int e = offsets[u]; e < offsets[u + 1]; e++)
                earliest[target[e]] = Math.max(earliest[target[e]],earliest[u] + duration[u]);
        long finish = 0;
        for (int u = 0; u < n; u++) finish = Math.max(finish,earliest[u] + duration[u]);
        for (int u = n-1; u >= 0; u--) {
            long end = finish;
            for (int e = offsets[u]; e < offsets[u + 1]; e++) end = Math.min(end,latest[target[e]]);
            latest[u] = end-duration[u];
        }
        ok(result.makespan() == finish,"makespan oracle");
        ok(Arrays.equals(earliest,result.earliestStart()),"earliest oracle");
        ok(Arrays.equals(latest,result.latestStart()),"latest oracle");
        long[] slack = new long[n];
        for (int i=0;i<n;i++) slack[i]=latest[i]-earliest[i];
        ok(Arrays.equals(slack,result.slack()),"slack oracle");
        boolean[] seen = new boolean[n];
        for (int v: result.order()) {
            ok(!seen[v],"topological duplicate");
            for (int e = offsets[v]; e < offsets[v + 1]; e++)
                ok(!seen[target[e]],"topological edge order");
            seen[v] = true;
        }
        if (n>0) {
            result.slack()[0]++;
            ok(Arrays.equals(slack,result.slack()),"detached schedule");
        }
    }
    public static void main(String[] args) {
        liveness(new long[]{1,0,4},new long[]{0,1,0},new int[]{0,1,2,3},new int[]{1,2,0},4);
        critical(new long[]{2,3,4}, new int[]{0,2,2,2},new int[]{1,2});
        Random random = new Random(0x4D334C495645L);
        for (int trial=0;trial<12_000;trial++) {
            int n=random.nextInt(15), variables=1+random.nextInt(20);
            long mask=(1L<<variables)-1L;
            long[] use=new long[n], def=new long[n];
            int[] off=new int[n+1], edges=new int[n*9];
            int cursor=0;
            for(int i=0;i<n;i++) {
                use[i]=random.nextLong()&mask;
                def[i]=random.nextLong()&mask;
                int count=n==0?0:random.nextInt(10);
                for(int j=0;j<count;j++) edges[cursor++]=random.nextInt(n);
                off[i+1]=cursor;
            }
            liveness(use,def,off,Arrays.copyOf(edges,cursor),variables);
        }
        for (int trial=0;trial<12_000;trial++) {
            int n=random.nextInt(16);
            long[] d=new long[n];
            int[] off=new int[n+1], edges=new int[n*12];
            int cursor=0;
            for(int i=0;i<n;i++) {
                d[i]=random.nextInt(30);
                int count=n-i-1==0?0:random.nextInt(13);
                for(int j=0;j<count;j++) edges[cursor++]=i+1+random.nextInt(n-i-1);
                off[i+1]=cursor;
            }
            critical(d,off,Arrays.copyOf(edges,cursor));
        }
        int n=256;
        long[] use=new long[n],def=new long[n];int[] off=new int[n+1],edges=new int[n];
        for(int i=0;i<n;i++) {use[i]=1L <<(i&63);edges[i]=(i+1)%n;off[i+1]=i+1;}
        liveness(use,def,off,edges,64);
        refuses(() -> M3DataflowLiveness.solve(1,new long[]{2},new long[]{0},new int[]{0,0},new int[]{}));
        refuses(() -> M3DataflowLiveness.solve(2,new long[]{0},new long[]{0},new int[]{0,1},new int[]{1}));
        refuses(() -> M3CriticalPath.analyze(new long[]{1,1},new int[]{0,1,2},new int[]{1,0}));
        refuses(() -> M3CriticalPath.analyze(new long[]{Long.MAX_VALUE,2},new int[]{0,1,1},new int[]{1}));
        refuses(() -> M3CriticalPath.analyze(new long[]{-1},new int[]{0,0},new int[]{}));
        System.out.println("M3_WAVE5_DATAFLOW_SCHEDULER_PASS checks=" + checks);
    }
}
