// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;

/**
 * Focused same-source sampler for clear/refill after a retained capacity peak.
 * Defaults cover unchanged sparse controls and the adaptive clear's dense fallback.
 */
public final class M3LinkedListSparseClearProbe {
    private static final int PEAK = 65536;
    private static final Object PAYLOAD = new Object();
    private static volatile Object retained;
    private static volatile long checksum;

    private M3LinkedListSparseClearProbe() { }

    public static void main(String[] arguments) throws ReflectiveOperationException {
        if ((arguments.length == 1 || arguments.length == 2) && arguments[0].equals("suite")) {
            String specification = arguments.length == 2 ? arguments[1]
                    : "1,64,512,2048,4096,16384";
            String[] tokens = specification.split(",", -1);
            int[] refills = new int[tokens.length];
            for (int index = 0; index < tokens.length; index++) {
                refills[index] = Integer.parseInt(tokens[index]);
                if (refills[index] <= 0 || refills[index] > PEAK) {
                    throw new IllegalArgumentException("invalid refill list");
                }
            }
            for (int refill : refills) {
                main(new String[] {Integer.toString(refill), "0", "1"});
                main(new String[] {Integer.toString(refill), "3000", "6000"});
            }
            return;
        }
        if (arguments.length != 3) {
            throw new IllegalArgumentException(
                    "usage: suite [refill,refill,...] OR refill warmupRounds measuredRounds");
        }
        int refill = Integer.parseInt(arguments[0]);
        int warmup = Integer.parseInt(arguments[1]);
        int measured = Integer.parseInt(arguments[2]);
        if (refill <= 0 || refill > PEAK || warmup < 0 || measured <= 0) {
            throw new IllegalArgumentException("invalid workload bounds");
        }
        Counters counters = new Counters();
        M3LinkedList<Object> list = new M3LinkedList<>();
        for (int index = 0; index < PEAK; index++) list.addLast(PAYLOAD);
        list.clear();
        for (int index = 0; index < refill; index++) list.addLast(PAYLOAD);
        retained = list;
        checksum = run(list, refill, warmup);
        long allocatedBefore = counters.allocated();
        long cpuBefore = counters.cpu();
        long wallBefore = System.nanoTime();
        long result = run(list, refill, measured);
        long elapsed = System.nanoTime() - wallBefore;
        long cpuAfter = counters.cpu();
        long allocatedAfter = counters.allocated();
        checksum = result;
        list.verifyInvariants();
        Field storageField = M3LinkedList.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        Object storage = storageField.get(list);
        Field valuesField = storage.getClass().getDeclaredField("values");
        valuesField.setAccessible(true);
        int retainedSlots = ((Object[]) valuesField.get(storage)).length;
        System.out.println("peak\trefill\twarmupRounds\tmeasuredRounds\telapsedNanos"
                + "\tcpuNanos\tallocatedBytes\tchecksum\tretainedSlots");
        System.out.println(PEAK + "\t" + refill + "\t" + warmup + "\t" + measured + "\t"
                + elapsed + "\t" + difference(cpuBefore, cpuAfter) + "\t"
                + difference(allocatedBefore, allocatedAfter) + "\t" + result + "\t" + retainedSlots);
        System.out.println("# javaRuntimeVersion=" + System.getProperty("java.runtime.version"));
        System.out.println("# vmArguments=" + ManagementFactory.getRuntimeMXBean().getInputArguments());
        System.out.println("# initial sample is not a pristine-JVM claim; unavailable counters=-1");
    }

    private static long run(M3LinkedList<Object> list, int refill, int rounds) {
        long result = 0;
        for (int round = 0; round < rounds; round++) {
            list.clear();
            for (int index = 0; index < refill; index++) list.addLast(PAYLOAD);
            result += list.size();
            if (list.getFirst() == PAYLOAD) result++;
        }
        return result;
    }

    private static long difference(long before, long after) {
        return before < 0 || after < 0 ? -1 : after - before;
    }

    private static final class Counters {
        private final java.lang.management.ThreadMXBean cpu =
                ManagementFactory.getThreadMXBean();
        private final com.sun.management.ThreadMXBean allocation;
        private final long threadId = Thread.currentThread().threadId();

        private Counters() {
            if (cpu.isCurrentThreadCpuTimeSupported() && !cpu.isThreadCpuTimeEnabled()) {
                cpu.setThreadCpuTimeEnabled(true);
            }
            if (cpu instanceof com.sun.management.ThreadMXBean extended
                    && extended.isThreadAllocatedMemorySupported()) {
                allocation = extended;
                if (!allocation.isThreadAllocatedMemoryEnabled()) {
                    allocation.setThreadAllocatedMemoryEnabled(true);
                }
            } else {
                allocation = null;
            }
        }

        private long cpu() {
            return cpu.isCurrentThreadCpuTimeSupported() && cpu.isThreadCpuTimeEnabled()
                    ? cpu.getCurrentThreadCpuTime() : -1;
        }

        private long allocated() {
            return allocation == null || !allocation.isThreadAllocatedMemoryEnabled()
                    ? -1 : allocation.getThreadAllocatedBytes(threadId);
        }
    }
}
