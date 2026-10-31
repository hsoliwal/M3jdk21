// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.ast;

import java.lang.management.ManagementFactory;

/** Focused current-thread allocation/CPU diagnostic; not a whole-SDK benchmark. */
public final class M3AstPreOrderAllocationProbe {
    private static volatile int[] sink;

    public static void main(String[] args) {
        if (!(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean)
                || !bean.isThreadAllocatedMemorySupported()) {
            throw new AssertionError("thread allocation measurement unavailable");
        }
        bean.setThreadAllocatedMemoryEnabled(true);
        if (!bean.isCurrentThreadCpuTimeSupported()) {
            throw new AssertionError("thread CPU measurement unavailable");
        }
        bean.setThreadCpuTimeEnabled(true);
        M3AtomStore.Builder builder = M3AtomStore.builder();
        int row = builder.intern(M3AtomDomain.AST, 1, 1L, 0L);
        for (int i = 1; i <= 12; i++) {
            row = builder.intern(M3AtomDomain.AST, i + 1, i + 1L, 0L, row, row);
        }
        M3Ast ast = new M3Ast(builder.freeze(), row);
        for (int i = 0; i < 500; i++) sink = ast.preOrder(0);
        long thread = Thread.currentThread().threadId();
        int repetitions = 2500;
        long allocated = bean.getThreadAllocatedBytes(thread);
        long cpu = bean.getCurrentThreadCpuTime();
        for (int i = 0; i < repetitions; i++) sink = ast.preOrder(0);
        cpu = bean.getCurrentThreadCpuTime() - cpu;
        allocated = bean.getThreadAllocatedBytes(thread) - allocated;
        if (sink.length != 8191) throw new AssertionError("expanded occurrence result");
        System.out.println("M3_AST_PREORDER_ALLOCATION_PROBE repetitions=" + repetitions
                + " occurrences=" + sink.length + " allocated_bytes=" + allocated
                + " allocated_bytes_per_call=" + allocated / repetitions
                + " cpu_ns=" + cpu);
    }
}
