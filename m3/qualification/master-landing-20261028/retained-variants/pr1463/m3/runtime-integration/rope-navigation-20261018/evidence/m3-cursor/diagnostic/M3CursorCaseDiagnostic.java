/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** A separate per-case compiler diagnostic using the unchanged actual-owner fixture. */
public final class M3CursorCaseDiagnostic {
    private static final int WARMUPS = 20;
    private static final int ROUNDS = 15;
    private static final int PASSES = 128;
    private static final long FORWARD_SUM = 1_261_394L;
    private static final long RANDOM_SUM = 1_259_362L;
    private static volatile long sink;

    private M3CursorCaseDiagnostic() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 3 || !(args[0].equals("atom-16384")
                || args[0].equals("balanced-64-16384"))
                || !(args[1].equals("forward") || args[1].equals("random"))) {
            throw new IllegalArgumentException("kind forward|random output-json");
        }
        int mode = args[1].equals("forward") ? 0 : 2;
        long expected = (mode == 0 ? FORWARD_SUM : RANDOM_SUM) * PASSES;
        ThreadMXBean threads = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!threads.isThreadAllocatedMemorySupported()
                || !threads.isCurrentThreadCpuTimeSupported()) {
            throw new IllegalStateException("allocation and CPU counters required");
        }
        threads.setThreadAllocatedMemoryEnabled(true);
        threads.setThreadCpuTimeEnabled(true);
        long thread = Thread.currentThread().threadId();
        try (M3StringPathCursorTest.BenchmarkFixture fixture =
                M3StringPathCursorTest.benchmarkFixture(args[0])) {
            if (fixture.length() != 16_384 || fixture.expected(mode, PASSES) != expected) {
                throw new AssertionError("frozen workload/checksum mismatch");
            }
            fixture.verify();
            System.gc();
            for (int warmup = 0; warmup < WARMUPS; warmup++) {
                check(work(fixture, mode), expected);
            }
            System.out.println("DIAGNOSTIC_WARMUP_COMPLETE kind=" + args[0]
                    + " mode=" + args[1]);
            long[] wall = new long[ROUNDS];
            long[] cpu = new long[ROUNDS];
            long[] bytes = new long[ROUNDS];
            for (int round = 0; round < ROUNDS; round++) {
                long allocated = threads.getThreadAllocatedBytes(thread);
                long threadCpu = threads.getCurrentThreadCpuTime();
                long start = System.nanoTime();
                long actual = work(fixture, mode);
                wall[round] = System.nanoTime() - start;
                cpu[round] = threads.getCurrentThreadCpuTime() - threadCpu;
                bytes[round] = threads.getThreadAllocatedBytes(thread) - allocated;
                check(actual, expected);
            }
            long units = 16_384L * PASSES;
            String result = "{\n  \"status\": \"PASS\",\n  \"kind\": \"" + args[0]
                    + "\",\n  \"mode\": \"" + args[1]
                    + "\",\n  \"reuse_cursor\": true,\n  \"warmups\": " + WARMUPS
                    + ",\n  \"rounds\": " + ROUNDS + ",\n  \"passes\": " + PASSES
                    + ",\n  \"units\": " + units + ",\n  \"checksum\": " + expected
                    + ",\n  \"median_wall_ns\": " + median(wall)
                    + ",\n  \"median_cpu_ns\": " + median(cpu)
                    + ",\n  \"median_allocated_bytes\": " + median(bytes)
                    + ",\n  \"raw_wall_ns\": " + Arrays.toString(wall)
                    + ",\n  \"raw_cpu_ns\": " + Arrays.toString(cpu)
                    + ",\n  \"raw_allocated_bytes\": " + Arrays.toString(bytes) + "\n}\n";
            Files.writeString(Path.of(args[2]), result);
            System.out.println("M3_CURSOR_CASE_DIAGNOSTIC_PASS kind=" + args[0]
                    + " mode=" + args[1] + " ns_per_unit=" + (double) median(wall) / units
                    + " cpu_ns_per_unit=" + (double) median(cpu) / units
                    + " allocated_bytes=" + median(bytes) + " checksum=" + expected);
        }
    }

    private static long work(M3StringPathCursorTest.BenchmarkFixture fixture, int mode) {
        long value = fixture.traverse(mode, PASSES, true);
        sink = value;
        return value;
    }

    private static void check(long actual, long expected) {
        if (actual != expected) {
            throw new AssertionError("checksum expected=" + expected + " actual=" + actual);
        }
    }

    private static long median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length >>> 1];
    }
}
