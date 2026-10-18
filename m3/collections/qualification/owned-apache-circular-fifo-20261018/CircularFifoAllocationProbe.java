/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import java.lang.management.ManagementFactory;
import java.util.Queue;
public final class CircularFifoAllocationProbe {
    private CircularFifoAllocationProbe() { }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Queue<Integer> queue=(Queue<Integer>)Class.forName(args[0]).getConstructor(int.class).newInstance(17);
        var generic=ManagementFactory.getThreadMXBean();
        if(!(generic instanceof com.sun.management.ThreadMXBean bean)||!bean.isThreadAllocatedMemorySupported())
            throw new IllegalStateException("allocation accounting unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        Integer value=Integer.valueOf(7);
        for(int i=0;i<500000;i++)queue.add(value);
        long thread=Thread.currentThread().threadId();
        long before=bean.getThreadAllocatedBytes(thread);
        for(int i=0;i<1000000;i++)queue.add(value);
        long after=bean.getThreadAllocatedBytes(thread);
        long allocated=after-before;
        if(queue.size()!=17||queue.peek()!=value)throw new AssertionError("eviction workload incorrect");
        System.out.println("{\"operations\":1000000,\"allocatedBytes\":"+allocated
            +",\"scope\":\"warmed cached-reference add/eviction loop only; no throughput or all-operations claim\"}");
    }
}
