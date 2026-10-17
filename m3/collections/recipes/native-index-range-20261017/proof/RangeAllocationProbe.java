/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;
import java.lang.management.ManagementFactory;
import static com.m3.collections.M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
/** Narrow single-thread allocated-byte observation, not a throughput/JMH claim. */
public final class RangeAllocationProbe {
    private static volatile Object sink;
    public static void main(String[] args) {
        var bean = ManagementFactory.getThreadMXBean();
        if (!(bean instanceof com.sun.management.ThreadMXBean allocation)
                || !allocation.isThreadAllocatedMemorySupported())
            throw new IllegalStateException("allocation measurement unsupported");
        allocation.setThreadAllocatedMemoryEnabled(true);
        long[] values=new long[65536]; for(int i=0;i<values.length;i++) values[i]=i;
        var view=M3LongIndexProjection.snapshot(values).sortedAscending().slice(33,65500);
        byte[] states=new byte[view.size()];
        long thread=Thread.currentThread().threadId();
        for(int i=0;i<100;i++) { sink=view.filtered(states,1,NATIVE_REQUIRED); sink=view.sortedAscending(); }
        int iterations=100;
        for(int sample=0;sample<3;sample++) {
            long before=allocation.getThreadAllocatedBytes(thread);
            for(int i=0;i<iterations;i++) sink=view.filtered(states,1,NATIVE_REQUIRED);
            long filter=allocation.getThreadAllocatedBytes(thread)-before;
            before=allocation.getThreadAllocatedBytes(thread);
            for(int i=0;i<iterations;i++) sink=view.sortedAscending();
            long sorted=allocation.getThreadAllocatedBytes(thread)-before;
            System.out.println("Allocation sample="+sample+" rows="+view.size()+" operations="+iterations
                +" nativeFilterBytes="+filter+" repeatedSortBytes="+sorted);
        }
    }
}
