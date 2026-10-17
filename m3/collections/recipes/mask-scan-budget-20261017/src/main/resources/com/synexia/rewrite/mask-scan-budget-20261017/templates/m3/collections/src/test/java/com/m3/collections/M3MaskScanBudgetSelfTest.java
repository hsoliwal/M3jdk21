/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** Actual Java owners and optional actual JNI; the oracle is an independent linear scan. */
public final class M3MaskScanBudgetSelfTest {
    private static long checks;
    private static final int GUARD = 0x735A1137;
    private static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("budget check " + checks); }
    private static void fails(Class<? extends Throwable> kind, Runnable action) {
        try { action.run(); } catch (Throwable e) { check(kind.isInstance(e)); return; }
        throw new AssertionError("expected " + kind.getName());
    }
    private static long expected(int[] rows, byte[] flags, int first, int budget, int req,
            int exc, int[] out, int offset, int limit) {
        int count = 0, i = first, end = first + Math.min(budget, rows.length - first);
        while (i < end && count < limit) {
            int state = Byte.toUnsignedInt(flags[i]);
            if ((state & req) == req && (state & exc) == 0) out[offset + count++] = rows[i];
            i++;
        }
        return ((long)i << 32) | count;
    }
    private static long scan(int[] rows, byte[] states, int first, int budget, int req, int exc,
            int[] out, int offset, int limit, M3IndexMaskCompactor.Backend backend, BooleanSupplier cancel) {
        return M3IndexMaskWindow.scanBounded(rows, states, first, budget, req, exc,
                out, offset, limit, backend, cancel);
    }
    private static void caseOf(int[] rows, byte[] states, int first, int budget, int req, int exc,
            int limit, M3IndexMaskCompactor.Backend backend) {
        int[] output = new int[limit + 4]; Arrays.fill(output, GUARD);
        int[] oracle = output.clone(), rowCopy = rows.clone(); byte[] stateCopy = states.clone();
        long wanted = expected(rows, states, first, budget, req, exc, oracle, 2, limit);
        int[] callbacks = {0};
        long actual = scan(rows, states, first, budget, req, exc, output, 2, limit, backend,
                () -> { callbacks[0]++; return false; });
        check(actual == wanted); check(Arrays.equals(output, oracle)); check(callbacks[0] == 1);
        check(Arrays.equals(rows, rowCopy)); check(Arrays.equals(states, stateCopy));
        int next = M3IndexMaskWindow.nextIndex(actual), written = M3IndexMaskWindow.written(actual);
        check(next >= first && next - first <= budget && written <= limit);
    }
    private static void resume(Random random, M3IndexMaskCompactor.Backend backend) {
        for (int sample = 0; sample < 96; sample++) {
            int[] rows = new int[1 + random.nextInt(3000)]; byte[] states = new byte[rows.length];
            random.nextBytes(states); Arrays.setAll(rows, i -> i % 17 - 8);
            int req = sample % 4 == 0 ? 0 : random.nextInt(256), exc = sample % 4 == 0 ? 0 : random.nextInt(256);
            int[] expected = new int[rows.length];
            long full = expected(rows, states, 0, rows.length, req, exc, expected, 0, rows.length);
            int[] gathered = new int[rows.length], page = new int[15]; int size = 0, cursor = 0, steps = 0;
            while (cursor < rows.length) {
                int budget = 1 + random.nextInt(257), limit = 1 + random.nextInt(page.length);
                long result = scan(rows, states, cursor, budget, req, exc, page, 0, limit, backend, null);
                int next = M3IndexMaskWindow.nextIndex(result), wrote = M3IndexMaskWindow.written(result);
                check(next > cursor && next <= Math.min(rows.length, cursor + budget));
                System.arraycopy(page, 0, gathered, size, wrote); size += wrote; cursor = next; steps++;
                check(steps <= rows.length);
            }
            check(size == M3IndexMaskWindow.written(full));
            check(Arrays.equals(Arrays.copyOf(gathered, size), Arrays.copyOf(expected, size)));
        }
    }
    private static void validation(M3IndexMaskCompactor.Backend backend) {
        int[] rows = {9,9,-1,4}, output = new int[5]; byte[] states = {0,1,(byte)128,(byte)255};
        Arrays.fill(output, GUARD); int[] original = output.clone();
        int[] calls = {0}; BooleanSupplier probe = () -> {calls[0]++;return false;};
        fails(NullPointerException.class, () -> scan(null,states,0,1,1,0,output,0,1,backend,probe));
        fails(NullPointerException.class, () -> scan(rows,null,0,1,1,0,output,0,1,backend,probe));
        fails(NullPointerException.class, () -> scan(rows,states,0,1,1,0,null,0,1,backend,probe));
        fails(NullPointerException.class, () -> scan(rows,states,0,1,1,0,output,0,1,null,probe));
        fails(IllegalArgumentException.class, () -> scan(rows,new byte[3],0,1,1,0,output,0,1,backend,probe));
        fails(IllegalArgumentException.class, () -> scan(rows,states,0,1,1,0,rows,0,1,backend,probe));
        for (int invalid : new int[]{-1,4097,Integer.MAX_VALUE})
            fails(IllegalArgumentException.class, () -> scan(rows,states,0,invalid,1,0,output,0,1,backend,probe));
        for (int invalid : new int[]{-1,256,Integer.MAX_VALUE}) {
            fails(IllegalArgumentException.class, () -> scan(rows,states,0,1,invalid,0,output,0,1,backend,probe));
            fails(IllegalArgumentException.class, () -> scan(rows,states,0,1,1,invalid,output,0,1,backend,probe));
        }
        fails(IndexOutOfBoundsException.class, () -> scan(rows,states,5,1,1,0,output,0,1,backend,probe));
        fails(IndexOutOfBoundsException.class, () -> scan(rows,states,-1,1,1,0,output,0,1,backend,probe));
        fails(IndexOutOfBoundsException.class, () -> scan(rows,states,0,1,1,0,output,Integer.MAX_VALUE,1,backend,probe));
        fails(IllegalArgumentException.class, () -> scan(rows,states,0,1,1,0,output,0,4097,backend,probe));
        check(calls[0] == 0); check(Arrays.equals(original,output));
        fails(CancellationException.class, () -> scan(rows,states,0,4,0,0,output,0,4,backend,()->true));
        check(Arrays.equals(original,output));
        RuntimeException sentinel = new IllegalStateException("probe");
        try { scan(rows,states,0,4,0,0,output,0,4,backend,()->{throw sentinel;});throw new AssertionError(); }
        catch (RuntimeException e) {check(e==sentinel);}
        check(Arrays.equals(original,output));
        Thread.currentThread().interrupt();
        try { fails(CancellationException.class, () -> scan(rows,states,0,4,0,0,output,0,4,backend,probe)); }
        finally { check(Thread.interrupted()); }
        fails(CancellationException.class, () -> scan(rows,states,0,4,0,0,output,0,4,backend,()->{Thread.currentThread().interrupt();return false;}));
        check(Thread.interrupted()); check(Arrays.equals(original,output));
        check(scan(rows,states,0,0,0,0,output,5,0,backend,null)==0L);
    }
    private static void directNative() throws Exception {
        Method method = M3IndexMaskWindow.class.getDeclaredMethod("scanBoundedNative",int[].class,byte[].class,
                int.class,int.class,int.class,int.class,int[].class,int.class,int.class);
        method.setAccessible(true);int[] out=new int[]{GUARD,GUARD};
        Object[][] bad = {{null,new byte[2],0,1,0,0,out,0,1},
                {new int[2],null,0,1,0,0,out,0,1},{new int[2],new byte[1],0,1,0,0,out,0,1},
                {new int[2],new byte[2],-1,1,0,0,out,0,1},{new int[2],new byte[2],3,1,0,0,out,0,1},
                {new int[2],new byte[2],0,-1,0,0,out,0,1},{new int[2],new byte[2],0,4097,0,0,out,0,1},
                {new int[2],new byte[2],0,1,256,0,out,0,1},{new int[2],new byte[2],0,1,0,-1,out,0,1},
                {new int[2],new byte[2],0,1,0,0,out,Integer.MAX_VALUE,1},
                {new int[2],new byte[2],0,1,0,0,out,0,3},{out,new byte[2],0,1,0,0,out,0,1}};
        for (Object[] args : bad) {
            try {method.invoke(null,args);throw new AssertionError("private JNI admitted invalid args");}
            catch (InvocationTargetException e) {check(e.getCause() instanceof IllegalArgumentException);}
            check(out[0]==GUARD && out[1]==GUARD);
        }
    }
    public static void main(String[] args) throws Exception {
        String mode=args.length==0?"java":args[0];
        var backend=mode.equals("native")?M3IndexMaskCompactor.Backend.NATIVE_REQUIRED:M3IndexMaskCompactor.Backend.JAVA;
        if(mode.equals("missing") || mode.equals("legacy")) {
            int[] out={GUARD};
            fails(UnsatisfiedLinkError.class,()->scan(new int[2],new byte[2],0,1,1,0,out,0,1,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED,null));
            check(out[0]==GUARD);
            if(mode.equals("missing")) fails(UnsatisfiedLinkError.class,()->scan(new int[0],new byte[0],0,0,0,0,out,0,0,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED,null));
            System.out.println("Budget refusal mode="+mode+" checks="+checks);return;
        }
        Random random=new Random(11371110L);
        int[] boundaries={0,1,2,127,128,129,255,256,257,4095,4096,4097,9000};
        for(int n:boundaries) {
            int[] rows=new int[n];byte[] states=new byte[n];Arrays.setAll(rows,i->i%11-5);random.nextBytes(states);
            for(int budget:new int[]{0,1,127,128,129,4096})
                for(int limit:new int[]{0,1,127,129,4096})
                    for(int first:new int[]{0,n/2,n}) caseOf(rows,states,first,budget,128,2,limit,backend);
        }
        // All required/excluded unsigned-byte pairs, including contradictory masks.
        int[] small={7,-1,Integer.MIN_VALUE,7,0,Integer.MAX_VALUE,3};
        byte[] flags={0,1,2,3,(byte)128,(byte)255,(byte)129};
        for(int req=0;req<256;req++) for(int exc=0;exc<256;exc++)
            caseOf(small,flags,req%8,exc%8,req,exc,req%4,backend);
        for(int i=0;i<1024;i++) {
            int n=random.nextInt(9001);int[] rows=new int[n];byte[] states=new byte[n];random.nextBytes(states);
            for(int j=0;j<n;j++) rows[j]=random.nextInt();
            caseOf(rows,states,random.nextInt(n+1),random.nextInt(4097),random.nextInt(256),random.nextInt(256),random.nextInt(130),backend);
        }
        resume(random,backend);validation(backend);
        if(mode.equals("native"))directNative();
        System.out.println("M3MaskScanBudgetSelfTest mode="+mode+" checks="+checks+" maskPairs=65536 randomCases=1024");
    }
}
