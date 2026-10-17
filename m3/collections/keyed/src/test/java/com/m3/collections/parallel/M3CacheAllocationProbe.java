// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.parallel;

import java.lang.management.ManagementFactory;

/** Optional warmed allocation diagnostic, not an admission test or throughput benchmark. */
public final class M3CacheAllocationProbe {
    private static volatile M3PlanCache<Object, Object> retained;
    private static volatile Object sink;
    private M3CacheAllocationProbe() { }
    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        var platform = ManagementFactory.getThreadMXBean();
        if (!(platform instanceof com.sun.management.ThreadMXBean bean) || !bean.isThreadAllocatedMemorySupported()) {
            System.out.println("SKIP per-thread allocation counter unavailable"); return;
        }
        if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        retained = new M3PlanCache<>(2048, 2048); Object value = new Object(), key = null;
        for (int i = 0; i < 2048; i++) { key = new Object(); retained.put(key, value, 1); }
        run(key, value, 200_000);
        long id = Thread.currentThread().threadId(); int iterations = 500_000;
        for (int trial = 0; trial < 3; trial++) {
            long before = bean.getThreadAllocatedBytes(id), start = System.nanoTime();
            run(key, value, iterations);
            long elapsed = System.nanoTime() - start, bytes = bean.getThreadAllocatedBytes(id) - before;
            if (bytes < 0) throw new IllegalStateException("allocation counter unavailable during measurement");
            System.out.println("{\"trial\":" + trial + ",\"put_get_pairs\":" + iterations +
                    ",\"allocated_bytes\":" + bytes + ",\"elapsed_ns\":" + elapsed + "}");
        }
    }
    private static void run(Object key, Object value, int count) {
        for (int i = 0; i < count; i++) { sink = retained.put(key, value, 1); sink = retained.get(key); }
    }
}
