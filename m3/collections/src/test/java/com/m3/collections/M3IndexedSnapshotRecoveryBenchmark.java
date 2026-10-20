/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.Comparator;

/**
 * Fresh bounded allocation observation, 2026-10-19. The previous benchmark's
 * raw source/configuration was lost; this program does not reconstruct its
 * checksums or throughput claim. Run identical bytes in paired fresh JVMs.
 */
public final class M3IndexedSnapshotRecoveryBenchmark {
    private static volatile long sink;

    private M3IndexedSnapshotRecoveryBenchmark() { }

    public static void main(String[] args) {
        if (args.length != 3) throw new IllegalArgumentException("length density repetitions");
        int length = Integer.parseInt(args[0]);
        int density = Integer.parseInt(args[1]);
        int repetitions = Integer.parseInt(args[2]);
        Integer[] values = new Integer[length];
        long state = 0x4d335245434f5645L;
        for (int i = 0; i < length; i++) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            values[i] = (int) (state >>> 32);
        }
        M3IndexedListSnapshot<Integer> source = M3IndexedListSnapshot.copyOf(values);
        Comparator<Integer> order = Comparator.naturalOrder();
        com.sun.management.ThreadMXBean allocations =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!allocations.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation counter unavailable");
        allocations.setThreadAllocatedMemoryEnabled(true);
        for (int warmup = 0; warmup < 64; warmup++) sink = operation(source, order, length, density);
        long thread = Thread.currentThread().threadId();
        long before = allocations.getThreadAllocatedBytes(thread);
        long start = System.nanoTime();
        long checksum = 0;
        for (int repetition = 0; repetition < repetitions; repetition++) {
            checksum += operation(source, order, length, density);
        }
        long elapsed = System.nanoTime() - start;
        long bytes = allocations.getThreadAllocatedBytes(thread) - before;
        sink = checksum;
        System.out.println("{\"length\":" + length + ",\"density\":" + density
                + ",\"repetitions\":" + repetitions + ",\"warmup\":64,\"allocated_bytes\":" + bytes
                + ",\"elapsed_ns\":" + elapsed + ",\"checksum\":" + checksum + "}");
    }

    private static long operation(M3IndexedListSnapshot<Integer> source,
                                  Comparator<Integer> order, int length, int density) {
        M3IndexedListSnapshot<Integer> output = source.filtered(value -> density == 1
                || (length != 8 && (value & 3) == 0)).sorted(order);
        long checksum = output.size();
        for (int ordinal = 0; ordinal < output.size(); ordinal++) {
            checksum = checksum * 31 + output.get(ordinal);
            checksum = checksum * 31 + output.physicalIndex(ordinal);
        }
        return checksum;
    }
}
