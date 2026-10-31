/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Supplier;

/** Narrow per-thread allocated-byte observation; run both exact source images with -Xint. */
public final class M3MaskedSortAllocationProbe {
    private static volatile Object sink;
    private static final int SAMPLES = 80;

    private M3MaskedSortAllocationProbe() { }

    public static void main(String[] args) {
        M3IndexPathMasks.Backend backend = args.length == 0 ? M3IndexPathMasks.Backend.JAVA
                : args.length == 1 && args[0].equals("--native")
                ? M3IndexPathMasks.Backend.NATIVE_REQUIRED : null;
        if (backend == null) throw new IllegalArgumentException("use no arguments or --native");
        if (backend == M3IndexPathMasks.Backend.NATIVE_REQUIRED && !M3IndexPathMasks.nativeAvailable()) {
            throw new AssertionError("actual pathmask native library is required");
        }
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new AssertionError("allocated-byte counter unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        Integer[] input = new Integer[2048];
        for (int i = 0; i < input.length; i++) input[i] = (input.length - i) % 31;
        var source = M3IndexedListSnapshot.copyOf(input);
        Comparator<Integer> order = Comparator.naturalOrder();
        for (int selected : new int[] {0, 1, 64, 1024}) {
            int[] flags = new int[input.length];
            Arrays.fill(flags, 0, selected, Integer.MIN_VALUE);
            Supplier<M3IndexedListSnapshot<Integer>> operation = () -> source.sortedMaskedByPhysicalState(
                    flags, Integer.MIN_VALUE, 0, order, backend);
            var reference = source.maskedByPhysicalState(flags, Integer.MIN_VALUE, 0, backend).sorted(order);
            if (!Arrays.equals(reference.physicalIndices(), operation.get().physicalIndices())) {
                throw new AssertionError("measured operation disagrees with incumbent pipeline");
            }
            for (int i = 0; i < 32; i++) sink = operation.get();
            long before = bean.getThreadAllocatedBytes(thread);
            long start = System.nanoTime();
            for (int i = 0; i < SAMPLES; i++) sink = operation.get();
            long elapsed = System.nanoTime() - start;
            long allocated = bean.getThreadAllocatedBytes(thread) - before;
            System.out.println("{\"backend\":\"" + backend + "\",\"rows\":2048,\"selected\":" + selected
                    + ",\"samples\":" + SAMPLES + ",\"allocated_bytes\":" + allocated
                    + ",\"elapsed_ns\":" + elapsed + ",\"mode\":\"allocation-observation-only\"}");
        }
        if (sink == null) throw new AssertionError("result must escape the measured operation");
    }
}
