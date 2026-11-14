/* Copyright 2026 Hitesh Soliwal and contributors. GPLv2 with the Classpath exception. */
package jdk.internal.mindex;
import java.util.Arrays;
/** Only compiled with source-instrumented scratch copies, never shipped as a runtime owner. */
public final class M3FactWorkProbe {
    static long relationReads;
    static long maskWords;
    private M3FactWorkProbe() { }
    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("relation or mask");
        if (args[0].equals("relation")) {
            long[] left = new long[64], right = new long[64], out = new long[64];
            Arrays.fill(left, Long.MIN_VALUE);
            for (int i = 0; i < 64; i++) right[i] = 1L << i;
            M3StateRelation.compose(left, 0, right, 0, out, 0, 64);
            for (long result : out) if (result != Long.MIN_VALUE) throw new AssertionError("wrong relation");
            System.out.println("relation right-row reads=" + relationReads);
            if (relationReads != 1) throw new AssertionError("repeated sparse row must reuse exact answer");
        } else if (args[0].equals("mask")) {
            int length = 1 << 20;
            long[] mask = new long[length >>> 6];
            Arrays.fill(mask, 0x5555555555555555L);
            int end = M3CollectionMaskCompactor.compact(new Object[length], 0, length, mask);
            if (end != length / 2) throw new AssertionError("wrong compaction");
            System.out.println("fragmentation prepass words=" + maskWords);
            if (maskWords > 257) throw new AssertionError("dispatch proof must stop after threshold exceeded");
        } else throw new IllegalArgumentException(args[0]);
    }
}
