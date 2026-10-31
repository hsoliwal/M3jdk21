/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;
import java.lang.management.ManagementFactory;
import java.util.function.Predicate;
/** Bounded single-host observation, not JMH or an application performance guarantee. */
public final class M3PackedFilterBenchmark {
    private static volatile Object sink;
    private M3PackedFilterBenchmark() { }
    public static void main(String[] args) {
        boolean nativeMode = args.length != 0 && args[0].equals("native");
        int n = 1 << 20;
        Integer[] pool = new Integer[256]; for(int i=0;i<256;i++)pool[i]=i;
        Integer[] refs = new Integer[n]; int[] rows = new int[n]; byte[] states = new byte[n];
        for(int i=0;i<n;i++){refs[i]=pool[i&255];rows[i]=i;}
        var snapshot=M3IndexedListSnapshot.copyOf(refs);
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        if(!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);
        if(!bean.isThreadCpuTimeEnabled())bean.setThreadCpuTimeEnabled(true);
        long tid=Thread.currentThread().threadId();
        for(int mode=0;mode<4;mode++) {
            final int pattern=mode;
            Predicate<Integer> keep=v -> switch(pattern){case 0->false;case 1->v==255;case 2->(v&1)==0;default->true;};
            for(int i=0;i<n;i++)states[i]=(byte)(keep.test(refs[i])?1:0);
            for(int i=0;i<25;i++){
                sink=nativeMode?M3IndexMaskCompactor.selectWindow(rows,0,states,0,n,1,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED):snapshot.filtered(keep);
            }
            for(int sample=0;sample<5;sample++) {
                long allocated=bean.getThreadAllocatedBytes(tid),cpu=bean.getCurrentThreadCpuTime(),wall=System.nanoTime();
                for(int i=0;i<20;i++)sink=nativeMode?M3IndexMaskCompactor.selectWindow(rows,0,states,0,n,1,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED):snapshot.filtered(keep);
                System.out.println("BENCH mode="+pattern+" sample="+sample+" count=20 n="+n+" allocated="+(bean.getThreadAllocatedBytes(tid)-allocated)+" cpu_ns="+(bean.getCurrentThreadCpuTime()-cpu)+" wall_ns="+(System.nanoTime()-wall));
            }
        }
        if(sink==null)throw new AssertionError();
    }
}
