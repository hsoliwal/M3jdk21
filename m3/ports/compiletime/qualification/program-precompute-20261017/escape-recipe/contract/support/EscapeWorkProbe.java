// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.IntStream;
import org.objectweb.asm.tree.MethodInsnNode;

/** Bounded work/CPU/allocation measurement on the same emitted Java owners and test corpus. */
public final class EscapeWorkProbe {
    private static volatile int checksum;
    private static volatile int[] retained;
    private EscapeWorkProbe() { }

    private static long[] measure(ThreadMXBean vm, int repetitions, Runnable operation) {
        long thread = Thread.currentThread().threadId();
        long allocated = vm.getThreadAllocatedBytes(thread);
        long cpu = vm.getCurrentThreadCpuTime();
        long wall = System.nanoTime();
        for (int i = 0; i < repetitions; i++) operation.run();
        wall = System.nanoTime() - wall;
        cpu = vm.getCurrentThreadCpuTime() - cpu;
        allocated = vm.getThreadAllocatedBytes(thread) - allocated;
        return new long[] {wall / repetitions, cpu / repetitions, allocated / repetitions};
    }

    private static long[] median(ThreadMXBean vm, int repetitions, Runnable operation) {
        long[][] samples = new long[7][];
        for (int i = 0; i < samples.length; i++) samples[i] = measure(vm, repetitions, operation);
        long[] result = new long[3];
        for (int field = 0; field < result.length; field++) {
            long[] values = new long[samples.length];
            for (int i = 0; i < values.length; i++) values[i] = samples[i][field];
            Arrays.sort(values);
            result[field] = values[values.length / 2];
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        ThreadMXBean vm = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!vm.isCurrentThreadCpuTimeSupported() || !vm.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("actual VM CPU/allocation measurement unavailable");
        }
        vm.setThreadCpuTimeEnabled(true);
        vm.setThreadAllocatedMemoryEnabled(true);
        System.out.println("{\"scope\":\"fixed ASM frames; medians of seven rounds; one query batch visits each obtaining site\","
                + "\"retained_measurement\":\"Instrumentation.getObjectSize of the sole returned primitive array\","
                + "\"java\":\"" + System.getProperty("java.runtime.version") + "\",\"rows\":[");
        boolean comma = false;
        for (int count : new int[] {1, 8, 32}) {
            var method = M3EscapePreparedTest.loopProgram(count, 7);
            var frames = M3EscapePreparedTest.frames(method);
            var scan = new M3ItemReferenceScan();
            int[] sites = IntStream.range(0, method.instructions.size())
                    .filter(i -> method.instructions.get(i) instanceof MethodInsnNode call && call.name.equals("getItem"))
                    .toArray();
            int[] answers = M3EscapeFlow.prepare(scan, method, frames);
            if (answers == null) throw new AssertionError("probe shape refused");
            int[] legacy = new int[sites.length];
            for (int i = 0; i < sites.length; i++) {
                legacy[i] = M3EscapeFlow.escape(scan, method, frames, method.instructions.get(sites[i]));
                if (legacy[i] != answers[sites[i]]) throw new AssertionError("probe parity");
            }
            Runnable old = () -> {
                int sum = 0;
                for (int site : sites) sum += M3EscapeFlow.escape(scan, method, frames, method.instructions.get(site));
                checksum = sum;
            };
            Runnable cold = () -> {
                int[] prepared = M3EscapeFlow.prepare(scan, method, frames);
                int sum = 0;
                for (int site : sites) sum += prepared[site];
                retained = prepared;
                checksum = sum;
            };
            Runnable warm = () -> {
                int sum = 0;
                for (int site : sites) sum += answers[site];
                checksum = sum;
            };
            for (int i = 0; i < 100; i++) { old.run(); cold.run(); warm.run(); }
            long[] oldCost = median(vm, 30, old);
            long[] coldCost = median(vm, 30, cold);
            long[] warmCost = median(vm, 3_000, warm);
            long retainedBytes = HeapAgent.retainedPrimitiveArrayBytes(answers);
            if (comma) System.out.println(",");
            comma = true;
            System.out.printf(Locale.ROOT,
                    "{\"sites\":%d,\"instructions\":%d,\"legacy_ns\":%d,\"legacy_cpu_ns\":%d,\"legacy_alloc_bytes\":%d,"
                    + "\"cold_ns\":%d,\"cold_cpu_ns\":%d,\"cold_alloc_bytes\":%d,\"warm_ns\":%d,\"warm_cpu_ns\":%d,"
                    + "\"warm_alloc_bytes\":%d,\"retained_vm_bytes\":%d,\"primitive_payload_bytes\":%d,\"checksum\":%d}",
                    sites.length, method.instructions.size(), oldCost[0], oldCost[1], oldCost[2],
                    coldCost[0], coldCost[1], coldCost[2], warmCost[0], warmCost[1], warmCost[2],
                    retainedBytes, Integer.BYTES * answers.length, checksum);
        }
        retained = null;
        System.out.println("\n]}");
    }
}
