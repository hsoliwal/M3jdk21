// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/** Fixed-input cost evidence, not a universal benchmark or retained-heap proof. */
public final class M3MaskWindowCostProbe {
    private static volatile long sink;
    private M3MaskWindowCostProbe() { }
    public static void main(String[] args) throws Exception {
        String shape = args[1];
        var backend = M3IndexMaskCompactor.Backend.valueOf(args[0]);
        int n = 262_144;
        int[] rows = new int[n], output = new int[4096];
        byte[] states = new byte[n];
        for (int i = 0; i < n; i++) {
            rows[i] = i ^ 0x734d;
            states[i] = (byte) (shape.equals("dense") || (shape.equals("sparse") && i % 1021 == 0) ? 128 : 0);
        }
        int required = shape.equals("all") ? 0 : 128;
        int excluded = shape.equals("contradictory") ? 128 : 0;
        long cold = System.nanoTime();
        sink = M3IndexMaskWindow.scan(rows, states, 0, required, excluded, output, 0, 4096, backend);
        cold = System.nanoTime() - cold;
        int warmup = args.length > 2 ? Integer.parseInt(args[2]) : 1000;
        for (int i = 0; i < warmup; i++) sink ^= M3IndexMaskWindow.scan(rows, states, 0, required, excluded, output, 0, 4096, backend) ^ output[0];
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long thread = Thread.currentThread().threadId();
        long allocated = bean.getThreadAllocatedBytes(thread);
        long cpu = bean.getCurrentThreadCpuTime();
        long wall = System.nanoTime();
        int iterations = 500;
        for (int i = 0; i < iterations; i++) sink ^= M3IndexMaskWindow.scan(rows, states, 0, required, excluded, output, 0, 4096, backend) ^ output[0];
        wall = System.nanoTime() - wall;
        cpu = bean.getCurrentThreadCpuTime() - cpu;
        allocated = bean.getThreadAllocatedBytes(thread) - allocated;
        String rss = Files.readAllLines(Path.of("/proc/self/status")).stream().filter(s -> s.startsWith("VmRSS:")).findFirst().orElse("unavailable");
        System.out.println("{\"backend\":\"" + backend + "\",\"shape\":\"" + shape + "\",\"rows\":" + n
                + ",\"warmup\":" + warmup + ",\"iterations\":" + iterations + ",\"first_call_ns\":" + cold + ",\"wall_ns\":" + wall
                + ",\"cpu_ns\":" + cpu + ",\"allocated_bytes\":" + allocated + ",\"rss\":\"" + rss.trim().replace('\t', ' ')
                + "\",\"sink\":" + sink + "}");
    }
}
