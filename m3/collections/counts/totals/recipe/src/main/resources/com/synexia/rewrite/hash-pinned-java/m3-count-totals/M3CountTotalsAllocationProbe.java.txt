/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

/** Paired Java-heap allocation diagnostic, not a throughput benchmark or universal claim. */
public final class M3CountTotalsAllocationProbe {
    private static volatile Object escapedResult;
    private static volatile long escapedTotal;
    private M3CountTotalsAllocationProbe() { }
    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("native library path required");
        System.load(args[0]);
        var bean = ManagementFactory.getThreadMXBean();
        if (!(bean instanceof com.sun.management.ThreadMXBean allocation)
                || !allocation.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("real thread-allocation support is required");
        }
        allocation.setThreadAllocatedMemoryEnabled(true);
        long[] a = new long[16384], b = new long[16384];
        Arrays.fill(a,3); Arrays.fill(b,2);
        M3LongBag x=new M3LongBag(2048), y=new M3LongBag(2048);
        for (int i=0;i<2048;i++) {x.add(i,3);y.add(i+512,2);}
        long expected = 81920;
        System.out.println("case\titerations\tallocated_bytes\tbytes_per_operation");
        long javaArray = measure(allocation,"java_array",()->{
            long[] result=M3CountLanes.combine(a,0,b,0,a.length,0); escapedResult=result;
            long sum=0; for(long value:result) sum+=value; escapedTotal=sum;},300);
        long javaTotal = measure(allocation,"java_total",()->escapedTotal=M3CountTotals.combine(a,0,b,0,a.length,0),300);
        if (escapedTotal!=expected) throw new AssertionError("Java total");
        long nativeArray=measure(allocation,"jni_array",()->{
            long[] result=M3CountLanes.combineNative(a,0,b,0,a.length,0); escapedResult=result;
            long sum=0; for(long value:result) sum+=value; escapedTotal=sum;},300);
        long nativeTotal=measure(allocation,"jni_total",()->escapedTotal=M3CountTotals.combineNative(a,0,b,0,a.length,0),300);
        if (escapedTotal!=expected) throw new AssertionError("JNI total");
        long bag=measure(allocation,"intersection_bag",()->{M3LongBag result=x.intersection(y);escapedResult=result;escapedTotal=result.size();},300);
        long total=measure(allocation,"intersection_total",()->escapedTotal=M3CountTotals.intersection(x,y),300);
        if (escapedTotal!=3072) throw new AssertionError("bag total");
        if (!(javaTotal<javaArray && nativeTotal<nativeArray && total<bag)) {
            throw new AssertionError("paired allocation reduction was not demonstrated");
        }
        System.out.println("PAIRED_ALLOCATION_REDUCTION_PASS");
    }
    private static long measure(com.sun.management.ThreadMXBean bean,String name,Runnable action,int iterations) {
        for(int i=0;i<200;i++) action.run();
        long thread=Thread.currentThread().threadId();
        long before=bean.getThreadAllocatedBytes(thread);
        for(int i=0;i<iterations;i++) action.run();
        long allocated=bean.getThreadAllocatedBytes(thread)-before;
        if(allocated<0) throw new AssertionError("invalid allocation counter");
        System.out.printf(java.util.Locale.ROOT,"%s\t%d\t%d\t%.2f%n",name,iterations,allocated,(double)allocated/iterations);
        return allocated;
    }
}
