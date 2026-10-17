// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target-only diagnostic namespace.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
/** Current-thread Java allocation diagnostic; no claim of JMH, GC pauses or native costs. */
public final class AllocationProbeV2 {
    private record Row(String key,long value) {
    }
    private static volatile Object sink;
    private AllocationProbeV2() {
    }
    public static void main(String[] args) {
        int n=100_000,g=64;
        String[] keys=new String[g];
        for(int i=0;i<g;i++)keys[i]="key-"+i;
        List<Row> rows=new ArrayList<>(n);
        for(int i=0;i<n;i++)rows.add(new Row(keys[i%g],i));
        Map<String,Supplier<Map<String,Long>>> cases=new LinkedHashMap<>();
        cases.put("jdk_lists_then_restream",()-> {
            Map<String,List<Row>> grouped=rows.stream().collect(Collectors.groupingBy(Row::key,LinkedHashMap::new,Collectors.toList()));
            Map<String,Long> result=grouped.entrySet().stream().collect(Collectors.toMap( entry->entry.getKey(),entry->entry.getValue().stream().mapToLong(Row::value).sum(),(left,right)->left,LinkedHashMap::new));
            return Collections.unmodifiableMap(result);
        }
        );
        cases.put("jdk_downstream_sum",()->Collections.unmodifiableMap(rows.stream().collect( Collectors.groupingBy(Row::key,LinkedHashMap::new,Collectors.summingLong(Row::value)))));
        cases.put("m3_fluent_sum",()->M3Flow.indexed(rows::size,rows::get).groupBy(Row::key).sumLong(Row::value).toMap());
        Map<String,Long> expected=cases.values().iterator().next().get();
        for(var c:cases.values())if(!expected.equals(c.get()))throw new AssertionError("result mismatch");
        var bean=ManagementFactory.getThreadMXBean();
        if(!(bean instanceof com.sun.management.ThreadMXBean memory)||!memory.isThreadAllocatedMemorySupported()) throw new UnsupportedOperationException("allocation counter unavailable");
        memory.setThreadAllocatedMemoryEnabled(true);
        for(int warm=0;warm<20;warm++)for(var c:cases.values())sink=c.get();
        long thread=Thread.currentThread().threadId();
        int iterations=15;
        System.out.println("case,rows,keys,iterations,java_bytes_per_op,nanos_per_op");
        for(var c:cases.entrySet()) {
            long before=memory.getThreadAllocatedBytes(thread),start=System.nanoTime();
            for(int i=0;i<iterations;i++)sink=c.getValue().get();
            long elapsed=System.nanoTime()-start,bytes=memory.getThreadAllocatedBytes(thread)-before;
            System.out.println(c.getKey()+","+n+","+g+","+iterations+","+bytes/iterations+","+elapsed/iterations);
        }
    }
}
