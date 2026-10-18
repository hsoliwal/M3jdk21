/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.Comparator;
import java.util.function.Supplier;

/** Setup-excluded allocation observations; not a universal latency ranking. */
public final class M3SnapshotSupersetAllocation {
    private static volatile Object sink;
    private M3SnapshotSupersetAllocation() { }
    private static void measure(String label,Supplier<?> call,com.sun.management.ThreadMXBean bean) {
        for(int i=0;i<100;i++)sink=call.get();
        long id=Thread.currentThread().threadId();
        long before=bean.getThreadAllocatedBytes(id),start=System.nanoTime();
        for(int i=0;i<100;i++)sink=call.get();
        long ns=System.nanoTime()-start, bytes=bean.getThreadAllocatedBytes(id)-before;
        System.out.println(label+"\t"+(bytes/100)+"\t"+ns);
    }
    public static void main(String[] args) {
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        if(!bean.isThreadAllocatedMemorySupported())throw new IllegalStateException("allocation counter unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        Integer[] values=new Integer[32768];for(int i=0;i<values.length;i++)values[i]=(i*1009)%65521;
        var source=M3IndexedListSnapshot.copyOf(values);Comparator<Integer> order=Integer::compare;
        System.out.println("workload\tbytes_per_call\tns_per_100_calls");
        measure("separate_half",()->source.filtered(v->(v&1)==0).sorted(order),bean);
        measure("fused_half",()->source.filteredSorted(v->(v&1)==0,order),bean);
        measure("separate_all",()->source.filtered(v->true).sorted(order),bean);
        measure("fused_all",()->source.filteredSorted(v->true,order),bean);
    }
}
