// SPDX-License-Identifier: Apache-2.0
package jdk.m3.structured;

import jdk.m3.text.Limits;
import jdk.m3.text.TextFrame;
import java.lang.management.ManagementFactory;
public final class DelimitedAllocationProbe {
    private static volatile long sink;
    private DelimitedAllocationProbe() { }
    public static void main(String[] args) {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation counter unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        var input = TextFrame.fromString("\"a\"\"b\",x,y,z\n".repeat(2048));
        var limits = new Limits(10, 0, 5000, 1L << 20);
        var index = StructuredTextTools.prepareDelimited(input, ',', limits, 1L << 20);
        long thread = Thread.currentThread().threadId();
        for (int i=0;i<1000;i++) sink += index.countEquals(0,"a\"b");
        long first = bean.getThreadAllocatedBytes(thread);
        for (int i=0;i<1000;i++) sink += index.countEquals(0,"a\"b");
        long reused = bean.getThreadAllocatedBytes(thread)-first;
        for (int i=0;i<100;i++) sink += StructuredTextTools.prepareDelimited(input,',',limits,1L<<20).records();
        first = bean.getThreadAllocatedBytes(thread);
        for (int i=0;i<100;i++) sink += StructuredTextTools.prepareDelimited(input,',',limits,1L<<20).records();
        long cold = bean.getThreadAllocatedBytes(thread)-first;
        first = bean.getThreadAllocatedBytes(thread);
        for (int i=0;i<100;i++) {
            long count=0;
            for (var row:StructuredTextTools.delimited(input,',',limits)) if (row.getFirst().equals("a\"b")) count++;
            sink += count;
        }
        long reparsed = bean.getThreadAllocatedBytes(thread)-first;
        System.out.printf(java.util.Locale.ROOT,"DELIMITED_ALLOCATION rows=2048 retainedPrimitiveBytes=%d warmCountBytes=%.1f coldPrepareBytes=%.1f reparsedMaterializedCountBytes=%.1f sink=%d%n",index.primitivePayloadBytes(),reused/1000.0,cold/100.0,reparsed/100.0,sink);
    }
}
