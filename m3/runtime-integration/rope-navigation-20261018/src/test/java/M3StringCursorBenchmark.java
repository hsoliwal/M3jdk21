/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Locale;

/**
 * Measures exact M3 owners and cursor classes loaded through java.base patching.
 * The VM and public String stay stock; these are isolated owner/cursor results.
 * Native fixture creation and oracle validation are outside traversal samples.
 * Fresh-cursor work includes cursor allocation/first resolution with warmed code.
 */
public final class M3StringCursorBenchmark {
    private static final int WARMUPS = Integer.getInteger("m3.cursor.warmups", 4);
    private static final int ROUNDS = Integer.getInteger("m3.cursor.rounds", 9);
    private static final int PASSES = Integer.getInteger("m3.cursor.passes", 32);
    private static final ThreadMXBean THREADS =
            (ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final long THREAD = Thread.currentThread().threadId();
    private static volatile long blackhole;

    private M3StringCursorBenchmark() { }

    public static void main(String[] arguments) {
        if (!THREADS.isThreadAllocatedMemorySupported()
                || !THREADS.isCurrentThreadCpuTimeSupported()) {
            throw new IllegalStateException("allocation and CPU counters required");
        }
        THREADS.setThreadAllocatedMemoryEnabled(true);
        THREADS.setThreadCpuTimeEnabled(true);
        System.out.println("M3_CURSOR_BENCH_ENV java=" + System.getProperty("java.version")
                + " vm=" + System.getProperty("java.vm.name")
                + " warmups=" + WARMUPS + " rounds=" + ROUNDS + " passes=" + PASSES
                + " actual_owners=true simulated_owners=false stock_hotspot=true"
                + " string_admission_tested=false");
        String[] kinds = {"atom-16384", "balanced-unit-16384", "balanced-8-16384",
                "balanced-64-16384", "shared-16384", "shared-slice-16382"};
        for (String kind : kinds) {
            System.gc();
            long heapBefore = usedHeap();
            long beforeAllocated = THREADS.getThreadAllocatedBytes(THREAD);
            long beforeCpu = THREADS.getCurrentThreadCpuTime();
            long start = System.nanoTime();
            try (M3StringPathCursorTest.BenchmarkFixture fixture =
                    M3StringPathCursorTest.benchmarkFixture(kind)) {
                long constructionNanos = System.nanoTime() - start;
                long constructionCpu = THREADS.getCurrentThreadCpuTime() - beforeCpu;
                long constructionBytes = THREADS.getThreadAllocatedBytes(THREAD) - beforeAllocated;
                System.gc();
                long heapWithFixture = usedHeap();
                fixture.verify();
                System.out.printf(Locale.ROOT,
                        "M3_CURSOR_FIXTURE kind=%s length=%d construction_ns=%d"
                        + " construction_cpu_ns=%d construction_allocated_bytes=%d"
                        + " native_fixture_bytes=%d heap_before_gc_sample=%d"
                        + " heap_with_fixture_gc_sample=%d%n",
                        kind, fixture.length(), constructionNanos, constructionCpu,
                        constructionBytes, fixture.nativeBytes(), heapBefore, heapWithFixture);
                for (int mode = 0; mode < 3; mode++) {
                    measure(kind, fixture, mode, false);
                    measure(kind, fixture, mode, true);
                }
                measure(kind, fixture, 3, false);
            }
        }
        System.out.println("M3_CURSOR_BENCH_PASS blackhole=" + blackhole);
    }

    private static long usedHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static long work(M3StringPathCursorTest.BenchmarkFixture fixture,
                             int mode, boolean reuseCursor) {
        long value = mode == 3 ? fixture.firstRead(PASSES * 32)
                : fixture.traverse(mode, PASSES, reuseCursor);
        blackhole = value;
        return value;
    }

    private static void measure(String kind, M3StringPathCursorTest.BenchmarkFixture fixture,
                                int mode, boolean reuseCursor) {
        String label = mode == 0 ? "forward" : mode == 1 ? "reverse"
                : mode == 2 ? "random" : "fresh-first-read";
        long expected = mode == 3 ? fixture.firstReadExpected(PASSES * 32)
                : fixture.expected(mode, PASSES);
        for (int warmup = 0; warmup < WARMUPS; warmup++) {
            check(work(fixture, mode, reuseCursor), expected, kind, label);
        }
        long[] nanos = new long[ROUNDS];
        long[] cpu = new long[ROUNDS];
        long[] allocated = new long[ROUNDS];
        for (int round = 0; round < ROUNDS; round++) {
            long bytes = THREADS.getThreadAllocatedBytes(THREAD);
            long threadCpu = THREADS.getCurrentThreadCpuTime();
            long start = System.nanoTime();
            long value = work(fixture, mode, reuseCursor);
            nanos[round] = System.nanoTime() - start;
            cpu[round] = THREADS.getCurrentThreadCpuTime() - threadCpu;
            allocated[round] = THREADS.getThreadAllocatedBytes(THREAD) - bytes;
            check(value, expected, kind, label);
        }
        long units = mode == 3 ? PASSES * 32L : PASSES * (long) fixture.length();
        System.out.printf(Locale.ROOT,
                "M3_CURSOR_RESULT kind=%s mode=%s reuse_cursor=%s units=%d"
                + " median_ns=%d median_cpu_ns=%d median_allocated_bytes=%d"
                + " median_ns_per_unit=%.6f checksum=%d%n",
                kind, label, reuseCursor, units, median(nanos), median(cpu), median(allocated),
                (double) median(nanos) / units, expected);
        System.out.println("M3_CURSOR_RAW kind=" + kind + " mode=" + label
                + " reuse_cursor=" + reuseCursor + " wall_ns=" + Arrays.toString(nanos)
                + " cpu_ns=" + Arrays.toString(cpu) + " allocated_bytes=" + Arrays.toString(allocated));
    }

    private static void check(long actual, long expected, String kind, String mode) {
        if (actual != expected) throw new AssertionError(kind + " " + mode
                + " expected=" + expected + " actual=" + actual);
    }

    private static long median(long[] values) {
        long[] copy = values.clone();
        Arrays.sort(copy);
        return copy[copy.length >>> 1];
    }
}
