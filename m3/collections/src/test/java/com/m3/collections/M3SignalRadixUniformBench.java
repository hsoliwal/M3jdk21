/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Random;

/** Process-local diagnostic; medians are workload observations, not a universal speed claim. */
public final class M3SignalRadixUniformBench {
    private static volatile long sink;
    private M3SignalRadixUniformBench() { }
    public static void main(String[] arguments) {
        boolean nativeBackend = arguments.length > 0 && arguments[0].equals("native");
        M3SignalOrder.Backend backend = nativeBackend ? M3SignalOrder.Backend.NATIVE_REQUIRED : M3SignalOrder.Backend.JAVA;
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        bean.setThreadCpuTimeEnabled(true);
        long thread = Thread.currentThread().threadId();
        for (int size : new int[] {0, 1, 256, 65536}) {
            for (String distribution : new String[] {"repeated", "narrow", "varying"}) {
                double[] values = new double[size];
                byte[] states = new byte[size];
                Random random = new Random(31337);
                for (int i = 0; i < size; i++) values[i] = switch (distribution) {
                    case "repeated" -> 42.0;
                    case "narrow" -> random.nextInt(256);
                    default -> random.nextDouble() * 1e9 - 5e8;
                };
                long coldStart = System.nanoTime();
                M3SignalOrder first = M3SignalOrder.filtered(values, states, 0, 0, true, backend);
                long cold = System.nanoTime() - coldStart;
                int repetitions = Math.max(16, Math.min(2048, 2_000_000 / Math.max(1, size)));
                for (int i = 0; i < repetitions * 2; i++) consume(M3SignalOrder.filtered(values, states, 0, 0, true, backend));
                long[] elapsed = new long[9], cpu = new long[9], allocated = new long[9];
                for (int sample = 0; sample < elapsed.length; sample++) {
                    long a = bean.getThreadAllocatedBytes(thread), c = bean.getCurrentThreadCpuTime(), t = System.nanoTime();
                    for (int i = 0; i < repetitions; i++) consume(M3SignalOrder.filtered(values, states, 0, 0, true, backend));
                    elapsed[sample] = (System.nanoTime() - t) / repetitions;
                    cpu[sample] = (bean.getCurrentThreadCpuTime() - c) / repetitions;
                    allocated[sample] = (bean.getThreadAllocatedBytes(thread) - a) / repetitions;
                }
                Arrays.sort(elapsed); Arrays.sort(cpu); Arrays.sort(allocated);
                System.out.printf("%s,%d,%s,%d,%d,%d,%d,%d,%d%n", nativeBackend ? "JNI" : "Java", size,
                        distribution, cold, elapsed[4], cpu[4], allocated[4], first.retainedPrimitiveBytes(),
                        nativeBackend && size >= 2 ? 24L * size : 0L);
            }
        }
        if (sink == Long.MIN_VALUE) throw new AssertionError();
    }
    private static void consume(M3SignalOrder order) {
        sink += order.size() + (order.isEmpty() ? 0 : order.rowAt(order.size() / 2));
    }
}
