/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * @test
 * @summary Byte-state count preserves cardinality, bounded cursor and validation across JNI transfer boundaries
 * @modules java.base/jdk.internal.m3
 * @run main/othervm -Xcheck:jni M3NativeCountBatchingTest
 */
import java.util.Arrays;
import java.util.Random;
import jdk.internal.m3.M3IndexMaskWindow;

public final class M3NativeCountBatchingTest {
    private static long checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static long expected(byte[] states, int first, int budget, int required, int excluded) {
        int end = first + Math.min(budget, states.length - first), count = 0;
        for (int i = first; i < end; i++) {
            boolean keep = true;
            for (int bit = 0; bit < 8; bit++) {
                boolean set = ((states[i] >>> bit) & 1) != 0;
                if (((required >>> bit) & 1) != 0 && !set) keep = false;
                if (((excluded >>> bit) & 1) != 0 && set) keep = false;
            }
            if (keep) count++;
        }
        return ((long) end << 32) | count;
    }
    private static void verify(byte[] states, int first, int budget, int required, int excluded) {
        long want = expected(states, first, budget, required, excluded);
        for (var backend : M3IndexMaskWindow.Backend.values()) {
            long got = M3IndexMaskWindow.countBounded(states, first, budget, required, excluded, backend);
            check(got == want, "count/cursor " + backend + " first=" + first + " budget=" + budget);
        }
    }
    private static void rejects(Class<? extends Throwable> kind, Runnable operation) {
        try { operation.run(); throw new AssertionError("missing " + kind); }
        catch (Throwable error) { check(kind.isInstance(error), "wrong exception " + error); }
    }
    public static void main(String[] args) {
        check(M3IndexMaskWindow.nativeCountAvailable(), "actual JDK libjava count required");
        byte[] states = new byte[9001]; new Random(0x3c12).nextBytes(states);
        byte[] saved = states.clone();
        int[] starts = {0, 1, 7, 127, 128, 129, 511, 1023, 1024, 1025, 8192, 9001};
        int[] budgets = {0, 1, 127, 128, 129, 255, 256, 1023, 1024, 1025, 2047, 2048, 4095, 4096, 4097, Integer.MAX_VALUE};
        int[][] masks = {{0,0},{128,0},{0,128},{3,128},{255,0},{0,255},{3,1},{128,127}};
        for (int first : starts) for (int budget : budgets) for (int[] mask : masks) verify(states, first, budget, mask[0], mask[1]);
        check(Arrays.equals(states, saved), "count must not mutate input");
        byte[] cycle = new byte[256];
        for (int i=0;i<cycle.length;i++) cycle[i]=(byte)(73*i+19);
        // All256 byte values once: each unconstrained bit doubles admissible assignments.
        for (int required=0;required<256;required++) for (int excluded=0;excluded<256;excluded++) {
            int want = (required & excluded) != 0 ? 0 : 1 << (8-Integer.bitCount(required|excluded));
            for (var backend : M3IndexMaskWindow.Backend.values()) {
                long got=M3IndexMaskWindow.countBounded(cycle,0,Integer.MAX_VALUE,required,excluded,backend);
                check(M3IndexMaskWindow.nextIndex(got)==256 && M3IndexMaskWindow.written(got)==want,"unsigned-byte truth table");
            }
        }
        // Each multi-bit row contributes one, not a popcount. Tail mutation is seen next call.
        Arrays.fill(states,(byte)0x83);verify(states,7,4097,3,0);
        for(int i=1023;i<states.length;i+=1024) states[i]=0;
        verify(states,7,Integer.MAX_VALUE,3,0);
        for(var backend:M3IndexMaskWindow.Backend.values()) {
            int total=0,next=0;
            while(next<states.length) {long got=M3IndexMaskWindow.countBounded(states,next,1023,3,0,backend);check(M3IndexMaskWindow.nextIndex(got)>next,"progress");next=M3IndexMaskWindow.nextIndex(got);total+=M3IndexMaskWindow.written(got);}
            check(total==8993,"resumption count");
            rejects(NullPointerException.class,()->M3IndexMaskWindow.countBounded(null,0,0,0,0,backend));
            rejects(IllegalArgumentException.class,()->M3IndexMaskWindow.countBounded(states,0,1,256,0,backend));
            rejects(IllegalArgumentException.class,()->M3IndexMaskWindow.countBounded(states,0,1,0,-1,backend));
            rejects(IndexOutOfBoundsException.class,()->M3IndexMaskWindow.countBounded(states,-1,1,0,0,backend));
            rejects(IndexOutOfBoundsException.class,()->M3IndexMaskWindow.countBounded(states,9002,1,0,0,backend));
            rejects(IllegalArgumentException.class,()->M3IndexMaskWindow.countBounded(states,0,-1,0,0,backend));
            verify(new byte[0],0,Integer.MAX_VALUE,128,0);
        }
        rejects(NullPointerException.class,()->M3IndexMaskWindow.countBounded(states,0,1,0,0,null));
        System.out.println("COUNT_BATCHING_PASS checks="+checks+" backend=NATIVE_REQUIRED");
    }
}
