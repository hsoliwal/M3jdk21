// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import java.util.Arrays;
import java.util.Random;

/** Deterministic primitive offset/mask differential, including real checked JNI. */
public final class M3IndexMaskWindowLongContract {
    private static long checks;
    private static boolean useNative;
    private static final int CANARY = 0x6c7da01f;
    private static final long LCANARY = 0x71ae2c3bd46f8590L;
    private M3IndexMaskWindowLongContract() { }

    private static void ok(boolean value) {
        checks++;
        if (!value) throw new AssertionError("failed check " + checks);
    }
    private static void eq(long expected, long actual) {
        checks++;
        if (expected != actual) throw new AssertionError("expected=" + expected + " actual=" + actual);
    }
    private static void same(int[] x, int[] y) { ok(Arrays.equals(x,y)); }
    private static void same(long[] x, long[] y) { ok(Arrays.equals(x,y)); }
    private static long pack(int next, int written) { return ((long) next << 32) | (written & 0xffff_ffffL); }
    private static long reference(byte[] states, int from, int required, int excluded, int limit, int[] indexes) {
        int consumed = from, n = 0;
        while (consumed < states.length && n < limit) {
            int bits = states[consumed] & 255;
            if ((bits & required) == required && (bits & excluded) == 0) indexes[n++] = consumed;
            consumed++;
        }
        return pack(consumed,n);
    }
    private static void runCase(int[] rows, long[] longs, byte[] states,
            int first, int required, int excluded, int limit) {
        int[] originals = rows.clone();
        long[] originalLongs = longs.clone();
        byte[] originalStates = states.clone();
        int[] keptIndexes = new int[limit];
        long expected = reference(states,first,required,excluded,limit,keptIndexes);
        for (M3IndexMaskCompactor.Backend backend : selectedBackends()) {
            int offset = 3;
            int[] result = new int[offset + limit + 4]; Arrays.fill(result,CANARY);
            long[] longResult = new long[offset + limit + 4]; Arrays.fill(longResult,LCANARY);
            long integerCursor = M3IndexMaskWindow.scan(rows,states,first,required,excluded,
                    result,offset,limit,backend);
            long longCursor = M3IndexMaskWindow.scanLong(longs,states,first,required,excluded,
                    longResult,offset,limit,backend);
            eq(expected,integerCursor); eq(expected,longCursor);
            int n = M3IndexMaskWindow.written(expected);
            for (int i=0;i<n;i++) {
                eq(rows[keptIndexes[i]],result[offset+i]);
                eq(longs[keptIndexes[i]],longResult[offset+i]);
            }
            for (int i=0;i<offset;i++) {eq(CANARY,result[i]);eq(LCANARY,longResult[i]);}
            for (int i=offset+n;i<result.length;i++) {eq(CANARY,result[i]);eq(LCANARY,longResult[i]);}
            same(originals,rows); same(originalLongs,longs); ok(Arrays.equals(originalStates,states));
        }
    }
    private static void expectFailure(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {action.run();}
        catch (Throwable err) { if (type.isInstance(err)) return; throw new AssertionError(err); }
        throw new AssertionError("expected "+type.getName());
    }
    private static M3IndexMaskCompactor.Backend[] selectedBackends() {
        return useNative ? M3IndexMaskCompactor.Backend.values() : new M3IndexMaskCompactor.Backend[]{M3IndexMaskCompactor.Backend.JAVA};
    }
    private static void invalidInputs() {
        for (var backend : M3IndexMaskCompactor.Backend.values()) {
            long[] rows = {Long.MIN_VALUE, 0, Long.MAX_VALUE};
            int[] ints = {-1,0,1};
            byte[] flags = {(byte) 255,0,3};
            long[] output = {LCANARY,LCANARY,LCANARY,LCANARY};
            int[] intOutput = {CANARY,CANARY,CANARY,CANARY};
            expectFailure(IllegalArgumentException.class,()->M3IndexMaskWindow.scanLong(rows,flags,0,1,0,rows,0,1,backend));
            expectFailure(IllegalArgumentException.class,()->M3IndexMaskWindow.scan(ints,flags,0,1,0,ints,0,1,backend));
            expectFailure(IllegalArgumentException.class,()->M3IndexMaskWindow.scanLong(rows,new byte[1],0,1,0,output,0,1,backend));
            expectFailure(IllegalArgumentException.class,()->M3IndexMaskWindow.scanLong(rows,flags,0,-1,0,output,0,1,backend));
            expectFailure(IllegalArgumentException.class,()->M3IndexMaskWindow.scanLong(rows,flags,0,0,256,output,0,1,backend));
            expectFailure(IllegalArgumentException.class,()->M3IndexMaskWindow.scanLong(rows,flags,0,0,0,output,0,4097,backend));
            expectFailure(IndexOutOfBoundsException.class,()->M3IndexMaskWindow.scanLong(rows,flags,4,0,0,output,0,1,backend));
            expectFailure(IndexOutOfBoundsException.class,()->M3IndexMaskWindow.scanLong(rows,flags,0,0,0,output,4,1,backend));
            expectFailure(NullPointerException.class,()->M3IndexMaskWindow.scanLong(null,flags,0,0,0,output,0,1,backend));
            expectFailure(NullPointerException.class,()->M3IndexMaskWindow.scanLong(rows,null,0,0,0,output,0,1,backend));
            eq(LCANARY,output[0]);eq(CANARY,intOutput[0]);
        }
    }
    public static void main(String[] args) {
        if (args.length > 1 || args.length == 1 && !"--java".equals(args[0]))
            throw new IllegalArgumentException("use no args for JNI or --java for Java only");
        useNative = args.length == 0;
        if (useNative) ok(M3IndexMaskCompactor.nativeAvailable());
        Random random = new Random(0x4d330c0ffee123L);
        for (int trial=0;trial<15000;trial++) {
            int length=trial%17==0?0:random.nextInt(512);
            int[] rows = new int[length]; long[] longs = new long[length]; byte[] states = new byte[length];
            for(int i=0;i<length;i++) {
                rows[i] = random.nextInt();
                longs[i] = (i%8 == 0 ? Long.MIN_VALUE : i%9==0 ? Long.MAX_VALUE : random.nextLong());
                states[i] = (byte)random.nextInt(256);
            }
            int first=length==0?0:random.nextInt(length+1);
            int required=trial%7==0?0:trial%11==0?255:random.nextInt(256);
            int excluded=trial%7==0?0:trial%11==0?0:random.nextInt(256);
            int limit=trial%13==0?0:trial%17==0?4096:random.nextInt(32);
            runCase(rows,longs,states,first,required,excluded,limit);
        }
        // Dense/large windows crossing native chunk boundaries and using the full limit.
        int[] rows = new int[5000];long[] longs=new long[5000];byte[] states = new byte[5000];
        for(int i=0;i<5000;i++){rows[i]=i;longs[i]=-(long)i;states[i]=(byte)0xFF;}
        for(int mask:new int[]{0,1,0x80,0xFF})
            runCase(rows,longs,states,14,mask,0,4096);
        invalidInputs();
        System.out.println("PASS M3IndexMaskWindowLongContract checks="+checks+" realJNI="+useNative);
    }
}
