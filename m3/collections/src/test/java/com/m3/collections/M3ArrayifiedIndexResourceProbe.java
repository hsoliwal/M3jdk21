/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.function.IntBinaryOperator;
import java.util.function.Supplier;

/** Explicit finite-workload CPU/allocation observations, not a universal speed claim. */
public final class M3ArrayifiedIndexResourceProbe {
    private static volatile int[] escaped;
    private M3ArrayifiedIndexResourceProbe() { }

    private static void measure(com.sun.management.ThreadMXBean meter, String operation,
            int size, Supplier<int[]> call, int[] expected) {
        if (!Arrays.equals(call.get(), expected)) throw new AssertionError(operation + " oracle");
        for (int i = 0; i < 2000; i++) escaped = call.get();
        long thread = Thread.currentThread().threadId();
        for (int sample = 0; sample < 5; sample++) {
            int iterations = 1000;
            long bytes = meter.getThreadAllocatedBytes(thread);
            long cpu = meter.getCurrentThreadCpuTime();
            for (int i = 0; i < iterations; i++) escaped = call.get();
            cpu = meter.getCurrentThreadCpuTime() - cpu;
            bytes = meter.getThreadAllocatedBytes(thread) - bytes;
            if (!Arrays.equals(escaped, expected)) throw new AssertionError(operation + " result");
            System.out.println(operation + "\t" + size + "\t" + sample + "\t" + iterations
                    + "\t" + (bytes / (double) iterations) + "\t" + (cpu / (double) iterations));
        }
    }

    public static void main(String[] args) {
        if (!(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean meter)
                || !meter.isThreadAllocatedMemorySupported() || !meter.isCurrentThreadCpuTimeSupported()) {
            throw new IllegalStateException("allocation/CPU counters unavailable");
        }
        if (!M3IndexMaskCompactor.nativeAvailable()) throw new AssertionError("real native binary required");
        meter.setThreadAllocatedMemoryEnabled(true);
        meter.setThreadCpuTimeEnabled(true);
        System.out.println("operation\tsize\tsample\titerations\tallocated_bytes_per_call\tcpu_ns_per_call");
        for (int size : new int[]{32, 4096}) {
            int[] rows = new int[size];
            for (int i = 0; i < size; i++) rows[i] = i;
            int distance = 17;
            int[] rotated = new int[size];
            for (int i = 0; i < size; i++) rotated[(i + distance) % size] = rows[i];
            measure(meter, "rotation", size, () -> M3ArrayifiedIndexOps.rotated(rows, 0, size, distance), rotated);
            for (int step : new int[]{1, 2, 64}) {
                byte[] states = new byte[size];
                for (int i = 0; i < size; i += step) states[i] = 1;
                int[] expected = new int[(size + step - 1) / step];
                for (int i = 0; i < expected.length; i++) expected[i] = i * step;
                measure(meter, "native_selection_step_" + step, size,
                        () -> M3ArrayifiedIndexOps.selected(rows, 0, size, states, 0, 1, 0,
                                M3IndexMaskCompactor.Backend.NATIVE_REQUIRED), expected);
            }
            int[] empty = new int[0];
            IntBinaryOperator order = Integer::compare;
            measure(meter, "merge_single_tail", size,
                    () -> M3ArrayifiedIndexOps.mergeSorted(rows, 0, size, empty, 0, 0, order), rows);
        }
        escaped = null;
    }
}
