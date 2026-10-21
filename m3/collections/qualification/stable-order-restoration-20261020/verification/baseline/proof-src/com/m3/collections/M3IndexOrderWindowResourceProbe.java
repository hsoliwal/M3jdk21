/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.io.IOException;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/** Observes actual owners; timings and process memory are measurements, not correctness proofs. */
public final class M3IndexOrderWindowResourceProbe {
    private static volatile Object objectSink;
    private static volatile long resultSink;
    private static final com.sun.management.ThreadMXBean THREADS =
            ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean ? bean : null;
    private static final com.sun.management.OperatingSystemMXBean PROCESS =
            ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean bean
                    ? bean : null;

    private M3IndexOrderWindowResourceProbe() { }
    @FunctionalInterface private interface Workload { long run(); }

    private static long allocation() {
        return THREADS != null && THREADS.isThreadAllocatedMemorySupported()
                ? THREADS.getThreadAllocatedBytes(Thread.currentThread().threadId()) : -1;
    }

    private static long threadCpu() {
        var bean = ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime() : -1;
    }

    private static long processCpu() { return PROCESS == null ? -1 : PROCESS.getProcessCpuTime(); }
    private static long delta(long before, long after) { return before < 0 || after < 0 ? -1 : after - before; }

    private static long heapAfterRequestedGc() {
        System.gc();
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    private static long[] processMemory() {
        long[] values = {-1, -1};
        try {
            for (String line : Files.readAllLines(Path.of("/proc/self/status"))) {
                if (line.startsWith("VmRSS:") || line.startsWith("VmHWM:")) {
                    long value = Long.parseLong(line.substring(line.indexOf(':') + 1).trim().split("\\s+")[0]);
                    values[line.startsWith("VmRSS:") ? 0 : 1] = value;
                }
            }
        } catch (IOException | NumberFormatException ignored) {
            // Non-Linux or unavailable process status is explicitly reported as -1.
        }
        return values;
    }

    private static long bufferBytes(String name) {
        for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class))
            if (pool.getName().equals(name)) return pool.getMemoryUsed();
        return -1;
    }

    private static void measure(String name, int units, int repetitions, Workload workload) {
        for (int i = 0; i < 50; i++) resultSink ^= workload.run();
        for (int sample = 0; sample < 3; sample++) {
            long retainedBefore = heapAfterRequestedGc();
            long[] memoryBefore = processMemory();
            long allocatedBefore = allocation(), threadBefore = threadCpu(), processBefore = processCpu();
            long start = System.nanoTime();
            for (int i = 0; i < repetitions; i++) resultSink ^= workload.run();
            long elapsed = System.nanoTime() - start;
            long processAfter = processCpu(), threadAfter = threadCpu(), allocatedAfter = allocation();
            long retainedAfter = heapAfterRequestedGc();
            long[] memoryAfter = processMemory();
            System.out.println("INDEX_RESOURCE case=" + name + " sample=" + sample + " units=" + units
                    + " operations=" + repetitions + " wall_ns=" + elapsed
                    + " thread_cpu_ns=" + delta(threadBefore, threadAfter)
                    + " process_cpu_ns=" + delta(processBefore, processAfter)
                    + " allocated_bytes=" + delta(allocatedBefore, allocatedAfter)
                    + " heap_after_requested_gc_before=" + retainedBefore
                    + " heap_after_requested_gc_after=" + retainedAfter
                    + " heap_after_requested_gc_delta=" + (retainedAfter - retainedBefore)
                    + " rss_kib=" + memoryAfter[0] + " rss_delta_kib=" + delta(memoryBefore[0], memoryAfter[0])
                    + " vmhwm_kib=" + memoryAfter[1]
                    + " direct_buffer_bytes=" + bufferBytes("direct")
                    + " mapped_buffer_bytes=" + bufferBytes("mapped") + " sink=" + resultSink);
        }
    }

    private static void sort(int size, int repetitions) {
        long start = System.nanoTime();
        long[] input = new long[size];
        for (int i = 0; i < size; i++) input[i] = i / 8;
        M3LongIndexProjection view = M3LongIndexProjection.snapshot(input).sortedAscending().slice(5, size - 18);
        System.out.println("INDEX_RESOURCE_PREP case=sort wall_ns=" + (System.nanoTime() - start));
        measure("known-direction-reversal", view.size(), repetitions, () -> {
            M3LongIndexProjection result = view.sortedDescending();
            objectSink = result;
            return ((long) result.physicalIndex(0) << 32) ^ result.physicalIndex(result.size() - 1);
        });
    }

    private static void mask(String name, int size, int repetitions, int pattern, int required,
            int excluded, int budget, int limit, M3IndexMaskCompactor.Backend backend) {
        long start = System.nanoTime();
        int[] rows = new int[size]; byte[] states = new byte[size]; int[] output = new int[limit];
        for (int i = 0; i < size; i++) {
            rows[i] = i % 17 - 8;
            states[i] = (byte) (pattern == 1 || (pattern == 2 && i % 128 == 127) ? 128 : 0);
        }
        System.out.println("INDEX_RESOURCE_PREP case=" + name + " wall_ns=" + (System.nanoTime() - start));
        measure(name, size, repetitions, () -> {
            int cursor = 0; long checksum = 0;
            while (cursor < size) {
                long progress = budget == Integer.MAX_VALUE
                        ? M3IndexMaskWindow.scan(rows, states, cursor, required, excluded, output, 0, limit, backend)
                        : M3IndexMaskWindow.scanBounded(rows, states, cursor, budget, required, excluded,
                                output, 0, limit, backend);
                int next = M3IndexMaskWindow.nextIndex(progress), written = M3IndexMaskWindow.written(progress);
                if (next <= cursor) throw new AssertionError("positive work/result budget must progress");
                checksum ^= progress;
                if (written != 0) checksum ^= (long) output[0] + output[written - 1];
                cursor = next;
            }
            return checksum;
        });
    }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 3)
            throw new IllegalArgumentException("sort|mask-java|mask-jni [rows>=32] [repetitions>0]");
        int size = args.length > 1 ? Integer.parseInt(args[1]) : 32768;
        int repetitions = args.length > 2 ? Integer.parseInt(args[2]) : 100;
        if (size < 32 || repetitions < 1) throw new IllegalArgumentException("positive bounded workload required");
        if (THREADS != null && THREADS.isThreadAllocatedMemorySupported()) THREADS.setThreadAllocatedMemoryEnabled(true);
        var standardThreads = ManagementFactory.getThreadMXBean();
        if (standardThreads.isThreadCpuTimeSupported()) standardThreads.setThreadCpuTimeEnabled(true);
        System.out.println("INDEX_RESOURCE_ENV java=" + Runtime.version() + " vm=" + System.getProperty("java.vm.name")
                + " os=" + System.getProperty("os.name") + " arch=" + System.getProperty("os.arch"));
        System.out.println("INDEX_RESOURCE_SCOPE RSS/VmHWM include Java heap and the whole VM; requested GC is observational; -1 means unavailable; native stack/transfer counts require the C work probe.");
        if (args[0].equals("sort")) { sort(size, repetitions); return; }
        if (!args[0].equals("mask-java") && !args[0].equals("mask-jni"))
            throw new IllegalArgumentException("sort|mask-java|mask-jni [rows>=32] [repetitions>0]");
        M3IndexMaskCompactor.Backend backend = args[0].equals("mask-jni")
                ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA;
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED && !M3IndexMaskCompactor.nativeAvailable())
            throw new IllegalStateException("actual native mask library required");
        mask("rejected", size, repetitions, 0, 128, 0, Integer.MAX_VALUE, 4096, backend);
        mask("dense", size, repetitions, 1, 128, 0, Integer.MAX_VALUE, 4096, backend);
        mask("one-tail-hit-per-chunk", size, repetitions, 2, 128, 0, Integer.MAX_VALUE, 4096, backend);
        mask("tail-early-limit", size, repetitions, 2, 128, 0, Integer.MAX_VALUE, 1, backend);
        mask("tail-bounded", size, repetitions, 2, 128, 0, 129, 7, backend);
        mask("all-pass", size, repetitions, 0, 0, 0, Integer.MAX_VALUE, 4096, backend);
        mask("contradictory", size, repetitions, 1, 128, 128, Integer.MAX_VALUE, 4096, backend);
    }
}
