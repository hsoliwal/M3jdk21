/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package java.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Manual complete-call qualification probe, not a timing assertion or a
 * substitute for JMH/product qualification. It measures validation, both mask
 * passes, result allocation, JNI transitions and native region copies.
 * Caller-owned input construction occurs before the timed repeated calls.
 * Each returned array escapes through a volatile sink. Native dispatch is
 * deliberately not enabled by the result of this probe.
 *
 * Compile/run with java.base reads to java.management and jdk.management;
 * the accompanying qualification script records the full commands.
 */
public class M3CollectionMasksBenchmark {
    private static volatile int[] sink;
    private static volatile long checksum;
    private static final ThreadMXBean CPU = ManagementFactory.getThreadMXBean();
    private static final com.sun.management.ThreadMXBean ALLOCATION =
            CPU instanceof com.sun.management.ThreadMXBean bean ? bean : null;
    private static final long THREAD = Thread.currentThread().threadId();
    private static boolean cpuSupported;
    private static boolean allocationSupported;

    public static void main(String[] args) {
        String library = System.getProperty("test.m3.masks.native.library");
        if (library != null) {
            System.load(library);
        }
        cpuSupported = CPU.isCurrentThreadCpuTimeSupported();
        if (cpuSupported && !CPU.isThreadCpuTimeEnabled()) {
            CPU.setThreadCpuTimeEnabled(true);
        }
        allocationSupported = ALLOCATION != null && ALLOCATION.isThreadAllocatedMemorySupported();
        if (allocationSupported && !ALLOCATION.isThreadAllocatedMemoryEnabled()) {
            ALLOCATION.setThreadAllocatedMemoryEnabled(true);
        }
        String requested = args.length == 0 ? "both" : args[0];
        if (!Set.of("java", "native", "both").contains(requested)) {
            throw new IllegalArgumentException("backend must be java, native, or both");
        }
        System.out.println("{\"schema\":1,\"probe\":\"M3CollectionMasks.complete-call\","
                + "\"java\":\"" + Runtime.version() + "\","
                + "\"arch\":\"" + System.getProperty("os.arch") + "\","
                + "\"cpu_supported\":" + cpuSupported + ","
                + "\"allocation_supported\":" + allocationSupported + "}");
        for (int bits : new int[] {64, 4096, 262144}) {
            for (int stride : new int[] {1, 2, 64}) {
                long[] words = new long[(bits + 63) >>> 6];
                for (int bit = 0; bit < bits; bit += stride) {
                    words[bit >>> 6] |= 1L << (bit & 63);
                }
                int from = bits == 64 ? 1 : 63;
                int to = bits - 1;
                int[] expected = BitSet.valueOf(words).stream()
                        .filter(index -> index >= from && index < to).toArray();
                int iterations = Math.max(80, Math.min(30000, 20000000 / bits));
                int warmup = Math.max(80, iterations / 2);
                for (boolean nativeBackend : new boolean[] {false, true}) {
                    if (selected(requested, nativeBackend)) {
                        if (!Arrays.equals(expected, invoke(nativeBackend, words, from, to))) {
                            throw new AssertionError("benchmark backend mismatch");
                        }
                        repeat(nativeBackend, words, from, to, warmup);
                    }
                }
                for (int round = 0; round < 5; round++) {
                    // Alternate backend order across rounds when running together.
                    for (int position = 0; position < 2; position++) {
                        boolean nativeBackend = ((position + round) & 1) != 0;
                        if (selected(requested, nativeBackend)) {
                            measure(nativeBackend, words, from, to, bits, stride,
                                    expected.length, iterations, round);
                        }
                    }
                }
            }
        }
        if (sink == null) {
            throw new AssertionError("no benchmark call escaped");
        }
        System.out.println("{\"checksum\":" + checksum + ",\"default_dispatch\":\"JAVA_UNCHANGED\"}");
    }

    private static boolean selected(String requested, boolean nativeBackend) {
        return requested.equals("both") || requested.equals(nativeBackend ? "native" : "java");
    }

    private static int[] invoke(boolean nativeBackend, long[] words, int from, int to) {
        return nativeBackend ? M3CollectionMasks.selectedNative(words, from, to)
                : M3CollectionMasks.selectedJava(words, from, to);
    }

    private static void repeat(boolean nativeBackend, long[] words, int from, int to,
                               int iterations) {
        long sum = 0L;
        for (int iteration = 0; iteration < iterations; iteration++) {
            int[] result = invoke(nativeBackend, words, from, to);
            sink = result;
            sum += result.length;
            if (result.length != 0) {
                sum += result[0] + result[result.length - 1];
            }
        }
        checksum += sum;
    }

    private static void measure(boolean nativeBackend, long[] words, int from, int to,
                                int bits, int stride, int selected, int iterations, int round) {
        long allocatedBefore = allocationSupported ? ALLOCATION.getThreadAllocatedBytes(THREAD) : 0L;
        long cpuBefore = cpuSupported ? CPU.getCurrentThreadCpuTime() : 0L;
        long wallBefore = System.nanoTime();
        repeat(nativeBackend, words, from, to, iterations);
        long wall = System.nanoTime() - wallBefore;
        long cpu = cpuSupported ? CPU.getCurrentThreadCpuTime() - cpuBefore : 0L;
        long allocated = allocationSupported
                ? ALLOCATION.getThreadAllocatedBytes(THREAD) - allocatedBefore : 0L;
        System.out.printf(Locale.ROOT,
                "{\"backend\":\"%s\",\"bits\":%d,\"stride\":%d,"
                + "\"from\":%d,\"to\":%d,\"selected\":%d,\"iterations\":%d,\"round\":%d,"
                + "\"wall_ns_per_call\":%.3f,\"cpu_ns_per_call\":%s,\"allocated_bytes_per_call\":%s}%n",
                nativeBackend ? "native" : "java", bits, stride, from, to, selected,
                iterations, round, (double)wall / iterations,
                cpuSupported ? String.format(Locale.ROOT, "%.3f", (double)cpu / iterations) : "null",
                allocationSupported ? String.format(Locale.ROOT, "%.3f", (double)allocated / iterations) : "null");
    }
}
