// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Matched-process microprobe. This is not a JMH or installed Eclipse measurement. */
public final class M3EclipseSelectionProbe {
    private static volatile List<Object> sink;

    private M3EclipseSelectionProbe() { }

    public static void main(String[] args) throws Exception {
        com.sun.management.ThreadMXBean bean =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        bean.setThreadCpuTimeEnabled(true);
        long thread = Thread.currentThread().threadId();
        System.out.println("size,workload,iterations,resultCount,allocatedBytes,cpuNanos,rssKiB");
        int[] sizes = {0, 1, 64, 256, 8192, 65536};
        String[] names = {"all", "half", "sparse", "excluded", "overlap"};
        int[][] masks = {{0,0},{1,0},{255,0},{0,254},{1,1}};
        for (int size : sizes) {
            M3EclipseIndexedSnapshot<Object> snapshot = new M3EclipseIndexedSnapshot<>();
            for (int i = 0; i < size; i++) snapshot.add(new Object(), i & 255);
            for (int workload = 0; workload < names.length; workload++) {
                int required = masks[workload][0], excluded = masks[workload][1];
                int repetitions = size < 1024 ? 10000 : size < 65536 ? 160 : 32;
                for (int i = 0; i < repetitions; i++) sink = snapshot.selected(required, excluded);
                long allocated = bean.getThreadAllocatedBytes(thread);
                long cpu = bean.getCurrentThreadCpuTime();
                for (int i = 0; i < repetitions; i++) sink = snapshot.selected(required, excluded);
                cpu = bean.getCurrentThreadCpuTime() - cpu;
                allocated = bean.getThreadAllocatedBytes(thread) - allocated;
                String rss = Files.readAllLines(Path.of("/proc/self/status")).stream()
                        .filter(line -> line.startsWith("VmRSS:"))
                        .map(line -> line.replaceAll("[^0-9]", "")).findFirst().orElse("0");
                System.out.println(size + "," + names[workload] + "," + repetitions + ","
                        + sink.size() + "," + allocated + "," + cpu + "," + rss);
            }
        }
    }
}
