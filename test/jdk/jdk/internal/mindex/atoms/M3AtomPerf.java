/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.LongSupplier;

/**
 * Per-atom JIT/perf harness (ATOM-PERF HARNESS v1). Each case is one batch of {@code ops} calls of
 * an atom through a constant {@link java.lang.invoke.MethodHandle} ({@code invokeExact}); the batch
 * returns a checksum that must equal the oracle checksum computed before timing, so every timed batch
 * is also a correctness check and its result cannot be dead code. All cases warm up first, then the
 * timed rounds interleave the cases (round r times case 1..n) so host load drifts hit every atom alike.
 * The system and process CPU load are sampled every round; PERF rows go to stdout and, when the system
 * property {@code m3.perf.out} names a file, to that TSV.
 */
final class M3AtomPerf {
    /** Warm-up batches per case (enough for C2 at the default thresholds) and timed interleaved rounds. */
    static final int WARMUP_BATCHES = 400;
    static final int ROUNDS = 25;
    /** Nanoseconds per millisecond, for the report. */
    private static final double NANOS_PER_MILLI = 1_000_000.0;
    /** Percent scale of the load samples. */
    private static final double PERCENT = 100.0;
    /** Quantile of the reported upper bound. */
    private static final double P90 = 0.9;

    private M3AtomPerf() {
    }

    /** One timed unit: {@code ops} atom calls returning a checksum, plus the expected checksum. */
    record Batch(int ops, long expected, LongSupplier body) {
    }

    /** Builds a batch; the first run is checked immediately so a wrong atom fails before timing. */
    static Batch checkedBatch(int ops, long expected, LongSupplier body) {
        Batch batch = new Batch(ops, expected, body);
        check(batch, "first run");
        return batch;
    }

    @FunctionalInterface
    interface Factory {
        Batch make() throws Throwable;
    }

    record Case(String atom, Factory factory) {
    }

    static Case named(String atom, Factory factory) {
        return new Case(atom, factory);
    }

    private static void check(Batch batch, String phase) {
        long sum = batch.body().getAsLong();
        if (sum != batch.expected()) {
            throw new AssertionError(phase + ": checksum " + sum + " != oracle " + batch.expected());
        }
    }

    private static double systemLoad() {
        var os = ManagementFactory.getOperatingSystemMXBean();
        return os instanceof com.sun.management.OperatingSystemMXBean bean ? bean.getCpuLoad() : Double.NaN;
    }

    private static double processLoad() {
        var os = ManagementFactory.getOperatingSystemMXBean();
        return os instanceof com.sun.management.OperatingSystemMXBean bean ? bean.getProcessCpuLoad() : Double.NaN;
    }

    /** Warms up every case, runs the interleaved rounds and reports one PERF row per case. */
    static void run(List<Case> cases, String[] args) throws Throwable {
        List<Batch> batches = new ArrayList<>();
        for (Case c : cases) {
            batches.add(c.factory().make());
        }
        for (int i = 0; i < WARMUP_BATCHES; i++) {
            for (Batch b : batches) {
                check(b, "warm-up");
            }
        }
        double[][] nsPerOp = new double[cases.size()][ROUNDS];
        double systemSum = 0;
        double processSum = 0;
        int samples = 0;
        long totalNanos = 0;
        for (int r = 0; r < ROUNDS; r++) {
            for (int k = 0; k < batches.size(); k++) {
                Batch b = batches.get(k);
                long start = System.nanoTime();
                long sum = b.body().getAsLong();
                long elapsed = System.nanoTime() - start;
                totalNanos += elapsed;
                if (sum != b.expected()) {
                    throw new AssertionError(cases.get(k).atom() + " round " + r + ": checksum " + sum);
                }
                nsPerOp[k][r] = (double) elapsed / b.ops();
            }
            double system = systemLoad();
            double process = processLoad();
            if (system >= 0 && process >= 0) {
                systemSum += system;
                processSum += process;
                samples++;
            }
        }
        double systemLoad = samples == 0 ? Double.NaN : PERCENT * systemSum / samples;
        double processLoad = samples == 0 ? Double.NaN : PERCENT * processSum / samples;
        List<String> rows = new ArrayList<>();
        rows.add("atom\tns_per_op_median\tns_per_op_min\tns_per_op_p90\tops_per_batch\trounds\twarmup_batches"
                + "\tsystem_cpu_pct\tprocess_cpu_pct\tcpus\tchecksum");
        for (int k = 0; k < cases.size(); k++) {
            double[] sorted = nsPerOp[k].clone();
            Arrays.sort(sorted);
            rows.add(String.format(Locale.ROOT, "%s\t%.3f\t%.3f\t%.3f\t%d\t%d\t%d\t%.1f\t%.1f\t%d\t%d",
                    cases.get(k).atom(), sorted[ROUNDS / 2], sorted[0], sorted[(int) (P90 * (ROUNDS - 1))],
                    batches.get(k).ops(), ROUNDS, WARMUP_BATCHES, systemLoad, processLoad,
                    Runtime.getRuntime().availableProcessors(), batches.get(k).expected()));
        }
        PrintStream out = System.out;
        rows.forEach(out::println);
        String file = System.getProperty("m3.perf.out");
        if (file != null) {
            try {
                Files.write(Path.of(file), rows, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new AssertionError("cannot write " + file, e);
            }
        }
        out.printf(Locale.ROOT, "M3_ATOM_PERF PASS cases=%d rounds=%d timed_ms=%.1f%n", cases.size(), ROUNDS,
                totalNanos / NANOS_PER_MILLI);
    }
}
