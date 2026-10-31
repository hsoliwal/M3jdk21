// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import java.util.Arrays;
import java.util.Random;
/** Self-contained original-owner Java/JNI differential; only JNI kernel is injected. */
public final class M3MaskWindowNativeStagingContract {
    private M3MaskWindowNativeStagingContract() { }
    private static int assertions;
    private static void check(boolean result, String why) {
        assertions++;
        if (!result) throw new AssertionError(why);
    }
    private static void expectedError(Runnable call, int[] output, String cause) {
        int[] before = output.clone();
        boolean threw = false;
        try { call.run(); } catch (IllegalStateException exception) { threw = true; }
        check(threw, "must refuse "+cause);
        check(Arrays.equals(before, output), "caller output touched during "+cause);
    }
    public static void main(String[] args) {
        check(M3IndexMaskCompactor.nativeAvailable(), "fault injection native backend loaded");
        var javaBackend = M3IndexMaskCompactor.Backend.JAVA;
        var nativeBackend = M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
        Random random = new Random(0x3ACCE11L);
        for (int pass = 0; pass < 2200; pass++) {
            int len = random.nextInt(250);
            int[] rows = new int[len]; byte[] states = new byte[len];
            for (int i=0;i<len;i++) {rows[i]=random.nextInt(8)-4;states[i]=(byte)random.nextInt(256);}
            int first=random.nextInt(len+1), budget=random.nextInt(len+3), limit=random.nextInt(35);
            int required=random.nextInt(16),excluded=random.nextInt(16);
            int[] javaOut=new int[limit+4],nativeOut=new int[limit+4];
            Arrays.fill(javaOut,777);Arrays.fill(nativeOut,777);
            long a=M3IndexMaskWindow.scanBounded(rows,states,first,budget,required,excluded,javaOut,2,limit,javaBackend);
            long b=M3IndexMaskWindow.scanBounded(rows,states,first,budget,required,excluded,nativeOut,2,limit,nativeBackend);
            check(a==b,"bounded cursor parity at "+pass);
            check(Arrays.equals(javaOut,nativeOut),"bounded values parity at "+pass);
            Arrays.fill(javaOut,777);Arrays.fill(nativeOut,777);
            a=M3IndexMaskWindow.scan(rows,states,first,required,excluded,javaOut,2,limit,javaBackend);
            b=M3IndexMaskWindow.scan(rows,states,first,required,excluded,nativeOut,2,limit,nativeBackend);
            check(a==b,"legacy cursor parity at "+pass);
            check(Arrays.equals(javaOut,nativeOut),"legacy values parity at "+pass);
        }
        int[] rows={1,2,3,4};byte[] states={1,2,3,4};int[] output={77,77,77,77,77};
        expectedError(()->M3IndexMaskWindow.scan(rows,states,0,254,0,output,1,1,nativeBackend),output,"invalid native cursor");
        expectedError(()->M3IndexMaskWindow.scanBounded(rows,states,0,4,254,0,output,1,1,nativeBackend),output,"invalid bounded native cursor");
        expectedError(()->M3IndexMaskWindow.scan(rows,states,0,253,0,output,1,1,nativeBackend),output,"JNI exception");
        expectedError(()->M3IndexMaskWindow.scanBounded(rows,states,0,4,253,0,output,1,1,nativeBackend),output,"bounded JNI exception");
        check(output[0]==77 && output[4]==77,"output surrounds unchanged");
        // Fail-closed geometry preflight: all inputs are validated before JNI.
        boolean bad = false;
        try { M3IndexMaskWindow.scan(rows, states, 0, 256, 0, output, 1, 1, nativeBackend); }
        catch (IllegalArgumentException expected) { bad = true; }
        check(bad && Arrays.equals(output,new int[]{77,77,77,77,77}),"invalid mask preflight");
        bad = false;
        try { M3IndexMaskWindow.scan(rows, states, 0, 1, 0, rows, 0, 1, nativeBackend); }
        catch (IllegalArgumentException expected) { bad = true; }
        check(bad && rows[0]==1,"output/source alias preflight");
        int[] wideRows = new int[4100];
        byte[] wideStates = new byte[4100];
        for (int i = 0; i < wideRows.length; i++) wideRows[i] = i - 2000;
        int[] nativeWide = new int[4096];
        int[] javaWide = new int[4096];
        long wideA = M3IndexMaskWindow.scan(wideRows, wideStates, 0, 0, 0, javaWide, 0, 4096, javaBackend);
        long wideB = M3IndexMaskWindow.scan(wideRows, wideStates, 0, 0, 0, nativeWide, 0, 4096, nativeBackend);
        check(wideA == wideB && Arrays.equals(javaWide, nativeWide),"maximum bounded mask window");
        System.out.println("PASS M3 native staging Java/JNI checks="+assertions);
    }
}
