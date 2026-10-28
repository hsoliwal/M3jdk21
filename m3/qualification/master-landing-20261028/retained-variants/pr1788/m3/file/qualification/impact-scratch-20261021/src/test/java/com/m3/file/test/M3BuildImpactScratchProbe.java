// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file.test;

import com.m3.file.M3BuildImpactPlan;
import com.m3.file.M3FileDeltaLedger;
import com.sun.management.ThreadMXBean;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Observes actual compute allocation/CPU independently of file-scan setup; no timing assertions. */
public final class M3BuildImpactScratchProbe {
    private static volatile M3BuildImpactPlan sink;

    private M3BuildImpactScratchProbe() { }

    public static void main(String[] arguments) throws IOException {
        int sourceCount = arguments.length > 0 ? Integer.parseInt(arguments[0]) : 512;
        int repeats = arguments.length > 1 ? Integer.parseInt(arguments[1]) : 4096;
        if (arguments.length > 2 || sourceCount < 16 || sourceCount > 2048
                || repeats < 1 || repeats > 10000) {
            throw new IllegalArgumentException("expected sourceCount [16, 2048], repeats [1, 10000]");
        }
        ThreadMXBean measurements = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!measurements.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("thread allocation measurement unsupported");
        }
        measurements.setThreadAllocatedMemoryEnabled(true);
        boolean cpu = measurements.isCurrentThreadCpuTimeSupported();
        if (cpu) measurements.setThreadCpuTimeEnabled(true);
        Path root = Files.createTempDirectory("m3-impact-scratch-probe-");
        try {
            run(root, sourceCount, repeats, measurements, cpu);
        } finally {
            try (var files = Files.walk(root)) {
                files.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); }
                    catch (IOException failure) { throw new UncheckedIOException(failure); }
                });
            }
        }
    }

    private static void run(Path root, int sourceCount, int repeats,
            ThreadMXBean measurements, boolean cpu) throws IOException {
        Files.createDirectory(root.resolve("src"));
        List<Path> tracked = new ArrayList<>();
        for (int index = 0; index < sourceCount; index++) {
            Path relative = Path.of("src", "Source" + index + ".java");
            Files.writeString(root.resolve(relative), "base");
            tracked.add(relative);
        }
        Path build = Path.of("build.properties");
        Files.writeString(root.resolve(build), "base");
        tracked.add(build);
        M3FileDeltaLedger.Snapshot seed = M3FileDeltaLedger.scan(root, null, tracked);
        M3FileDeltaLedger.Snapshot steady = M3FileDeltaLedger.scan(root, seed, tracked);
        Files.writeString(root.resolve(tracked.get(0)), "changed");
        M3FileDeltaLedger.Snapshot changed = M3FileDeltaLedger.scan(root, steady, tracked);
        Files.writeString(root.resolve(build), "changed");
        M3FileDeltaLedger.Snapshot model = M3FileDeltaLedger.scan(root, changed, tracked);
        int count = steady.slots();
        int[] sourceSlots = new int[sourceCount];
        for (int index = 0; index < sourceCount; index++) sourceSlots[index] = steady.physicalSlot(tracked.get(index));
        int[] offsets = new int[count + 1];
        for (int index = 0; index + 1 < sourceCount; index++) offsets[sourceSlots[index] + 1]++;
        for (int slot = 0; slot < count; slot++) offsets[slot + 1] += offsets[slot];
        int[] targets = new int[sourceCount - 1];
        int[] cursor = offsets.clone();
        for (int index = 0; index + 1 < sourceCount; index++) targets[cursor[sourceSlots[index]]++] = sourceSlots[index + 1];
        byte[] safe = new byte[count];
        observe("no-change-verified", steady, true, offsets, targets, safe, repeats, measurements, cpu);
        observe("no-change-unverified", steady, false, null, null, null, repeats, measurements, cpu);
        observe("conservative-unverified", changed, false, null, null, null, repeats, measurements, cpu);
        observe("conservative-build-input", model, true, offsets, targets, safe, repeats, measurements, cpu);
        observe("verified-propagation", changed, true, offsets, targets, safe, repeats, measurements, cpu);
        System.out.println("M3_IMPACT_SCRATCH_PROBE_SHAPE slots=" + count + " sourceFiles=" + sourceCount
                + " dependencyEdges=" + targets.length + " avoidablePrimitiveScratchBytes=" + (5L * count));
    }

    private static void observe(String mode, M3FileDeltaLedger.Snapshot snapshot, boolean verified,
            int[] offsets, int[] targets, byte[] safe, int repeats,
            ThreadMXBean measurements, boolean cpu) {
        int warmup = 2048;
        for (int index = 0; index < warmup; index++) {
            sink = M3BuildImpactPlan.compute(snapshot, verified, offsets, targets, safe);
        }
        long thread = Thread.currentThread().threadId();
        long allocatedBefore = measurements.getThreadAllocatedBytes(thread);
        long cpuBefore = cpu ? measurements.getCurrentThreadCpuTime() : 0;
        long started = System.nanoTime();
        long checksum = 0;
        for (int index = 0; index < repeats; index++) {
            M3BuildImpactPlan plan = M3BuildImpactPlan.compute(snapshot, verified, offsets, targets, safe);
            checksum += plan.size() * 31L + plan.reasonAt(0);
            sink = plan;
        }
        long elapsed = System.nanoTime() - started;
        long cpuElapsed = cpu ? measurements.getCurrentThreadCpuTime() - cpuBefore : -1;
        long allocated = measurements.getThreadAllocatedBytes(thread) - allocatedBefore;
        System.out.println("M3_IMPACT_SCRATCH_PROBE mode=" + mode + " warmup=" + warmup
                + " repeats=" + repeats + " totalAllocatedBytes=" + allocated
                + " bytesPerOperation=" + (allocated / repeats) + " totalNanos=" + elapsed
                + " nanosPerOperation=" + (elapsed / repeats) + " totalCpuNanos=" + cpuElapsed
                + " cpuNanosPerOperation=" + (cpu ? cpuElapsed / repeats : -1)
                + " checksum=" + checksum);
    }
}
