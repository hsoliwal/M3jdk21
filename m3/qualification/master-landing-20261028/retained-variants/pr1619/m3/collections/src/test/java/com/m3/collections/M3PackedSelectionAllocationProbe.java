/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

/** JVM-specific allocation evidence, deliberately separate from portable behavior tests. */
public final class M3PackedSelectionAllocationProbe {
    private static volatile Object sink;
    private M3PackedSelectionAllocationProbe() { }

    public static void main(String[] args) {
        int length = 1_000_000;
        Integer[] input = new Integer[length];
        Arrays.fill(input, 7);
        M3IndexedListSnapshot<Integer> owner = M3IndexedListSnapshot.copyOf(input);
        com.sun.management.ThreadMXBean bean =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new UnsupportedOperationException("allocation counter");
        bean.setThreadAllocatedMemoryEnabled(true);
        for (int i = 0; i < 20; i++) sink = owner.filtered(value -> false);
        long tid = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(tid);
        for (int i = 0; i < 10; i++) sink = owner.filtered(value -> false);
        long bytes = (bean.getThreadAllocatedBytes(tid) - before) / 10;
        System.out.println("zero_survivors=" + length + " average_allocated_bytes=" + bytes);
        if (bytes >= length / 2) {
            throw new AssertionError("scalar selection scratch exceeds packed-bit budget: " + bytes);
        }
    }
}
