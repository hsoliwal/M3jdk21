/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections.vavr.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Prefix selection preserves values while allocating only the retained bitmap prefix. */
final class BitSetPrefixAllocationTest {
    private static volatile Object retained;

    @Test
    void prefixesPreserveWordBoundariesAndTheOriginalSet() {
        Integer[] values = {0, 1, 63, 64, 65, 127, 128, 255, 4096};
        BitSet<Integer> source = BitSet.of(values);
        for (int count = -1; count <= values.length + 1; count++) {
            int size = Math.max(0, Math.min(count, values.length));
            assertEquals(Arrays.asList(Arrays.copyOf(values, size)), source.take(count).toJavaList());
        }
        assertSame(source, source.take(source.length()));
        assertEquals(Arrays.asList(values), source.toJavaList());
    }

    @Test
    void tinyPrefixDoesNotCopyTheSparseSuffix() {
        var platform = ManagementFactory.getThreadMXBean();
        assumeTrue(platform instanceof com.sun.management.ThreadMXBean, "thread allocation counter");
        var allocations = (com.sun.management.ThreadMXBean) platform;
        assumeTrue(allocations.isThreadAllocatedMemorySupported()
                && allocations.isThreadAllocatedMemoryEnabled(), "enabled allocation counter");
        BitSet<Integer> source = BitSet.of(1, 64, 65, 1 << 26);
        for (int warmup = 0; warmup < 16; warmup++) retained = source.take(1);
        long thread = Thread.currentThread().threadId();
        long bytes = allocations.getThreadAllocatedBytes(thread);
        long cpu = allocations.getCurrentThreadCpuTime();
        for (int repeat = 0; repeat < 16; repeat++) retained = source.take(1);
        long allocated = allocations.getThreadAllocatedBytes(thread) - bytes;
        long cpuNanos = allocations.getCurrentThreadCpuTime() - cpu;
        System.out.println("BITSET_PREFIX_ALLOCATION bytes=" + allocated + " operations=16 cpuNanos=" + cpuNanos);
        assertEquals(BitSet.of(1), retained);
        assertEquals(4, source.length());
        assertTrue(source.contains(1 << 26), "source suffix must survive");
        assertTrue(allocated >= 0 && allocated < 64 * 1024,
                "tiny prefixes must not copy the 8 MiB suffix: allocated=" + allocated);
    }
}
