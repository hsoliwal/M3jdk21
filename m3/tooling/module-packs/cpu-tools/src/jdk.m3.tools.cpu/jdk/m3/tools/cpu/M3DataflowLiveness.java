// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/** Backwards monotone liveness fixed point over graph CSR and 64-bit variable masks. */
public final class M3DataflowLiveness {
    private M3DataflowLiveness() { }

    public record Result(long[] liveIn, long[] liveOut, long processed) {
        public Result {
            liveIn = liveIn.clone(); liveOut = liveOut.clone();
            if (liveIn.length != liveOut.length || processed < 0) throw new IllegalArgumentException();
        }
        @Override public long[] liveIn() { return liveIn.clone(); }
        @Override public long[] liveOut() { return liveOut.clone(); }
    }

    /** 
     * use[i] denotes variables read before local definition; def[i] denotes all killed variables.
     * liveOut[i]=OR liveIn[successors], liveIn[i]=use[i] | (liveOut[i] & ~def[i]).
     * All output masks exclude variables beyond variableCount.
     */
    public static Result solve(int variableCount, long[] use, long[] def,
                               int[] offsets, int[] successors) {
        Objects.requireNonNull(use); Objects.requireNonNull(def);
        Objects.requireNonNull(offsets); Objects.requireNonNull(successors);
        int n = use.length;
        if (variableCount < 1 || variableCount > 64 || n > 4096 || def.length != n
                || offsets.length != n + 1 || successors.length > 262144
                || offsets[0] != 0 || offsets[n] != successors.length)
            throw new IllegalArgumentException("liveness geometry");
        long allowed = variableCount == 64 ? -1L : (1L << variableCount) - 1L;
        for (int i = 0; i < n; i++) {
            if (offsets[i] > offsets[i + 1] || (use[i] & ~allowed) != 0
                    || (def[i] & ~allowed) != 0)
                throw new IllegalArgumentException("invalid bit-mask or edge order");
        }
        int[] incoming = new int[n];
        for (int v : successors) {
            if (v < 0 || v >= n) throw new IllegalArgumentException("invalid successor");
            incoming[v] = Math.addExact(incoming[v], 1);
        }
        int[] reverseOffsets = new int[n + 1];
        for (int v = 0; v < n; v++) reverseOffsets[v + 1] = reverseOffsets[v] + incoming[v];
        int[] write = reverseOffsets.clone(), parents = new int[successors.length];
        for (int v = 0; v < n; v++)
            for (int e = offsets[v]; e < offsets[v + 1]; e++) parents[write[successors[e]]++] = v;
        long[] in = new long[n], out = new long[n];
        int[] queue = new int[Math.addExact(n, 1)];
        boolean[] pending = new boolean[n];
        int head = 0, tail = 0;
        for (int v = 0; v < n; v++) { queue[tail++] = v; pending[v] = true; }
        long steps = 0;
        while (head != tail) {
            int v = queue[head]; head = (head + 1) % queue.length;
            pending[v] = false;
            if (++steps > 20_000_000) throw new IllegalStateException("liveness work budget");
            long newOut = 0;
            for (int e = offsets[v]; e < offsets[v + 1]; e++) newOut |= in[successors[e]];
            long newIn = use[v] | (newOut & ~def[v]);
            if (newIn != in[v] || newOut != out[v]) {
                if ((newIn & in[v]) != in[v]) throw new IllegalStateException("nonmonotone liveness");
                in[v] = newIn; out[v] = newOut;
                for (int e = reverseOffsets[v]; e < reverseOffsets[v + 1]; e++) {
                    int p = parents[e];
                    if (!pending[p]) {
                        queue[tail] = p; tail = (tail + 1) % queue.length; pending[p] = true;
                    }
                }
            }
        }
        return new Result(in, out, steps);
    }
}
