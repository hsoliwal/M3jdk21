/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/** Actual-owner cold/warm and retained-result observations; no universal speed claims. */
public final class M3LongIndexMonotoneResourceProbe {
    private static final com.sun.management.ThreadMXBean THREADS =
            ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean ? bean : null;
    private static volatile Object resultSink;
    private static volatile Object retainedSink;
    private static volatile long checksum;

    private M3LongIndexMonotoneResourceProbe() { }

    private record Workload(M3LongIndexProjection source, boolean descending) {
        M3LongIndexProjection sort() {
            return descending ? source.sortedDescending() : source.sortedAscending();
        }
    }

    private static long allocated() {
        return THREADS != null && THREADS.isThreadAllocatedMemorySupported()
                ? THREADS.getThreadAllocatedBytes(Thread.currentThread().threadId()) : -1;
    }

    private static long cpu() {
        return THREADS != null && THREADS.isCurrentThreadCpuTimeSupported()
                ? THREADS.getCurrentThreadCpuTime() : -1;
    }

    private static long delta(long before, long after) {
        return before < 0 || after < 0 ? -1 : after - before;
    }

    private static long requestedGcHeap() {
        System.gc();
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    private static long rssKiB() {
        try {
            for (String line : Files.readAllLines(Path.of("/proc/self/status"))) {
                if (line.startsWith("VmRSS:"))
                    return Long.parseLong(line.substring(6).trim().split("\\s+")[0]);
            }
        } catch (IOException | NumberFormatException unavailable) {
            return -1;
        }
        return -1;
    }

    private static void consume(M3LongIndexProjection value) {
        resultSink = value;
        checksum ^= value.size();
        if (!value.isEmpty())
            checksum ^= value.get(0) ^ value.get(value.size() - 1) ^ value.physicalIndex(value.size() / 2);
    }

    private static Workload prepare(String name, int size) {
        long[] values = new long[size];
        Random random = new Random(0x4d334d4f4e4fL);
        for (int i = 0; i < size; i++) {
            values[i] = switch (name) {
                case "descending" -> -(i / 8);
                case "equal" -> 17;
                case "random" -> random.nextLong();
                case "early-inversion" -> i == 0 ? Long.MAX_VALUE : i;
                case "late-inversion" -> i == size - 1 ? Long.MIN_VALUE : i;
                case "ascending", "partial", "tiny-slice", "masked", "known-order" -> i / 8;
                default -> throw new IllegalArgumentException("unknown workload: " + name);
            };
        }
        M3LongIndexProjection view = M3LongIndexProjection.snapshot(values);
        if (name.equals("partial")) view = view.slice(size / 4, size / 2);
        if (name.equals("tiny-slice")) view = view.slice(size / 2, 16);
        if (name.equals("masked")) {
            byte[] states = new byte[size];
            for (int i = 0; i < size; i++) states[i] = (byte) (i % 3 == 0 ? 0x80 : 0);
            view = view.filtered(states, 0x80, M3IndexMaskCompactor.Backend.JAVA);
        }
        if (name.equals("known-order")) view = view.sortedAscending();
        return new Workload(view, name.equals("descending"));
    }

    private static void measure(String phase, String name, int repetitions, Workload work) {
        long allocatedBefore = allocated();
        long cpuBefore = cpu();
        long start = System.nanoTime();
        for (int i = 0; i < repetitions; i++) consume(work.sort());
        long elapsed = System.nanoTime() - start;
        long cpuAfter = cpu();
        long allocatedAfter = allocated();
        System.out.println("MONOTONE_RESOURCE phase=" + phase + " case=" + name
                + " rows=" + work.source.size() + " operations=" + repetitions
                + " wall_ns=" + elapsed + " thread_cpu_ns=" + delta(cpuBefore, cpuAfter)
                + " allocated_bytes=" + delta(allocatedBefore, allocatedAfter) + " checksum=" + checksum);
    }

    private static void verify(M3LongIndexProjection value, boolean descending) {
        for (int i = 1; i < value.size(); i++) {
            long previous = value.get(i - 1), current = value.get(i);
            if (descending ? previous < current : previous > current)
                throw new AssertionError("benchmark output is not ordered");
            if (previous == current && value.physicalIndex(i - 1) > value.physicalIndex(i))
                throw new AssertionError("benchmark output loses stable occurrence order");
        }
    }

    private static void storageObservation(String name, Workload work, M3LongIndexProjection output) {
        // This is an allocation/retention observation, not an exposed String/collection contract.
        try {
            Field indexField = M3LongIndexProjection.class.getDeclaredField("indices");
            if (!indexField.trySetAccessible()) {
                System.out.println("MONOTONE_STORAGE case=" + name + " status=INACCESSIBLE");
                return;
            }
            int[] inputIndex = (int[]) indexField.get(work.source);
            int[] resultIndex = (int[]) indexField.get(output);
            System.out.println("MONOTONE_STORAGE case=" + name + " source_index_length=" + inputIndex.length
                    + " result_index_length=" + resultIndex.length + " shared_index=" + (inputIndex == resultIndex));
        } catch (ReflectiveOperationException unavailable) {
            System.out.println("MONOTONE_STORAGE case=" + name + " status=UNAVAILABLE");
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) throw new IllegalArgumentException("case rows>=64 repetitions>0");
        String name = args[0];
        int size = Integer.parseInt(args[1]), repetitions = Integer.parseInt(args[2]);
        if (size < 64 || repetitions < 1 || repetitions > 10000)
            throw new IllegalArgumentException("bounded positive workload required");
        if (THREADS != null && THREADS.isThreadAllocatedMemorySupported())
            THREADS.setThreadAllocatedMemoryEnabled(true);
        if (THREADS != null && THREADS.isThreadCpuTimeSupported()) THREADS.setThreadCpuTimeEnabled(true);
        System.out.println("MONOTONE_ENV java=" + Runtime.version() + " vm=" + System.getProperty("java.vm.name")
                + " os=" + System.getProperty("os.name") + " arch=" + System.getProperty("os.arch"));
        System.out.println("MONOTONE_SCOPE cold=first-sort-in-process; warm=prepared-ENCOUNTER-view;"
                + " requested-GC-and-RSS=observations; -1=unavailable; no-native-memory-delta-claim");
        long preparation = System.nanoTime();
        Workload work = prepare(name, size);
        int[] sourceBefore = work.source.physicalIndices();
        System.out.println("MONOTONE_PREP case=" + name + " source_rows=" + size + " wall_ns="
                + (System.nanoTime() - preparation));
        measure("cold", name, 1, work);
        int warmupOperations = Math.max(80, repetitions);
        for (int i = 0; i < warmupOperations; i++) consume(work.sort());
        System.out.println("MONOTONE_WARMUP case=" + name + " operations=" + warmupOperations);
        for (int sample = 0; sample < 3; sample++) measure("warm" + sample, name, repetitions, work);
        M3LongIndexProjection result = work.sort();
        verify(result, work.descending);
        storageObservation(name, work, result);
        resultSink = null;
        retainedSink = null;
        long beforeHeap = requestedGcHeap(), beforeRss = rssKiB();
        M3LongIndexProjection[] retained = new M3LongIndexProjection[Math.min(repetitions, 64)];
        for (int i = 0; i < retained.length; i++) retained[i] = work.sort();
        retainedSink = retained;
        long afterHeap = requestedGcHeap(), afterRss = rssKiB();
        System.out.println("MONOTONE_RETAIN case=" + name + " outputs=" + retained.length
                + " heap_before=" + beforeHeap + " heap_after=" + afterHeap
                + " heap_delta=" + (afterHeap - beforeHeap) + " rss_before_kib=" + beforeRss
                + " rss_after_kib=" + afterRss + " rss_delta_kib=" + delta(beforeRss, afterRss));
        if (!Arrays.equals(sourceBefore, work.source.physicalIndices()))
            throw new AssertionError("source changed during benchmark");
    }
}
