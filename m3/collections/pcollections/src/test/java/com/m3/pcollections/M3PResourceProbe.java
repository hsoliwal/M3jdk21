/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.pcollections;

import com.m3.collections.M3IndexMaskCompactor;
import com.m3.collections.M3IndexMaskCompactor.Backend;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

/**
 * Reproducible resource observations, not a statistically controlled benchmark.
 * Reports wall/CPU time and current-thread allocation when the JVM supports it;
 * JNI/native allocation and process RSS are outside the thread allocation metric.
 */
public final class M3PResourceProbe {
    private static volatile Object retained;
    private static final int SIZE = 8192;
    private static final int VERSIONS = 256;
    private static final java.lang.management.ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();
    private static final com.sun.management.ThreadMXBean ALLOCATION = THREADS instanceof com.sun.management.ThreadMXBean bean ? bean : null;
    private M3PResourceProbe() { }

    public static void main(String[] arguments) {
        Backend backend = arguments.length > 0 && arguments[0].equals("--native")
                ? Backend.NATIVE_REQUIRED : Backend.JAVA;
        if (backend == Backend.NATIVE_REQUIRED && !M3IndexMaskCompactor.nativeAvailable()) {
            throw new AssertionError("Native resource probe requires the actual JNI binary");
        }
        if (THREADS.isCurrentThreadCpuTimeSupported() && !THREADS.isThreadCpuTimeEnabled()) {
            THREADS.setThreadCpuTimeEnabled(true);
        }
        if (ALLOCATION != null && ALLOCATION.isThreadAllocatedMemorySupported()
                && !ALLOCATION.isThreadAllocatedMemoryEnabled()) ALLOCATION.setThreadAllocatedMemoryEnabled(true);
        boolean named = M3PVector.class.getModule().isNamed();
        if (named) {
            require("com.m3.pcollections".equals(M3PVector.class.getModule().getName()), "M3 module identity");
            require("org.pcollections".equals(TreePVector.class.getModule().getName()), "donor module identity");
            require("com.m3.collections".equals(M3IndexMaskCompactor.class.getModule().getName()), "JNI owner module identity");
        }
        System.out.println("{\"environment\":true,\"javaVersion\":\"" + clean(System.getProperty("java.version"))
                + "\",\"javaVendor\":\"" + clean(System.getProperty("java.vendor"))
                + "\",\"os\":\"" + clean(System.getProperty("os.name"))
                + "\",\"arch\":\"" + clean(System.getProperty("os.arch"))
                + "\",\"backend\":\"" + backend + "\",\"m3Module\":\"" + M3PVector.class.getModule().getName()
                + "\",\"donorModule\":\"" + TreePVector.class.getModule().getName()
                + "\",\"jniOwnerModule\":\"" + M3IndexMaskCompactor.class.getModule().getName()
                + "\",\"size\":" + SIZE + ",\"retainedVersions\":" + VERSIONS + "}");

        ArrayList<Integer> input = new ArrayList<>(SIZE);
        for (int index = 0; index < SIZE; index++) input.add(index);
        long expected = copyVersions(input); // Separate JDK oracle; not inside candidate timing.
        require(m3Versions(input) == expected, "M3 warm-up digest");
        require(donorVersions(input) == expected, "donor warm-up digest");
        measure("m3-vector-branching", () -> m3Versions(input), expected);
        measure("donor-vector-branching", () -> donorVersions(input), expected);
        measure("jdk-copy-branching", () -> copyVersions(input), expected);

        M3PVector<Integer> base = M3PVector.from(input);
        M3PVector<Integer> changed = base.with(SIZE / 2, -1);
        base.verifyInvariants(); changed.verifyInvariants();
        M3PVector<Integer> slice = base.subList(SIZE / 2, SIZE / 2 + 3);
        M3PVector<Integer> materialized = M3PVector.<Integer>empty().plusAll(slice);
        require(base.sharedPageCount(changed) > 0, "Untouched pages share identity");
        require(slice.retainedSize() == SIZE && materialized.retainedSize() == 3, "Documented slice retention/materialization");
        System.out.println("{\"sharing\":true,\"basePages\":" + base.pageCount()
                + ",\"treeLevels\":" + base.treeHeight() + ",\"sharedUpdatePages\":" + base.sharedPageCount(changed)
                + ",\"visibleSliceSize\":" + slice.size() + ",\"sliceRetainedSize\":" + slice.retainedSize()
                + ",\"materializedRetainedSize\":" + materialized.retainedSize() + "}");

        M3PListView<Integer> view = M3PListView.of(base).reversed();
        byte[] states = new byte[SIZE];
        long selectionDigest = 0;
        for (int index = 0; index < SIZE; index++) {
            states[index] = (byte) (index * 73);
            if (((states[index] & 255) & 0x81) == 0x81) selectionDigest = 31 * selectionDigest + input.get(SIZE - 1 - index);
        }
        final long expectedSelection = selectionDigest;
        require(selectDigest(view, states, backend) == expectedSelection, "Selection warm-up digest");
        measure("index-mask-selection-" + backend, () -> selectDigest(view, states, backend), expectedSelection);
        System.out.println("{\"heapUsedObservationBytes\":" + ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()
                + ",\"threadAllocationExcludesNativeMemory\":true,\"statisticalPerformanceClaim\":false}");
    }

    private static String clean(String text) { return text.replace("\\", "\\\\").replace("\"", "\\\""); }

    private static long m3Versions(List<Integer> input) {
        M3PVector<Integer> base = M3PVector.from(input);
        ArrayList<M3PVector<Integer>> versions = new ArrayList<>();
        long digest = 0;
        for (int step = 0; step < VERSIONS; step++) {
            int index = (step * 4051) & (SIZE - 1);
            M3PVector<Integer> version = base.with(index, -step - 1);
            versions.add(version);
            digest = 31 * digest + version.get(index);
        }
        for (M3PVector<Integer> version : versions) digest = 31 * digest + version.hashCode();
        retained = versions;
        return digest;
    }

    private static long donorVersions(List<Integer> input) {
        PVector<Integer> base = TreePVector.from(input);
        ArrayList<PVector<Integer>> versions = new ArrayList<>();
        long digest = 0;
        for (int step = 0; step < VERSIONS; step++) {
            int index = (step * 4051) & (SIZE - 1);
            PVector<Integer> version = base.with(index, -step - 1);
            versions.add(version);
            digest = 31 * digest + version.get(index);
        }
        for (PVector<Integer> version : versions) digest = 31 * digest + version.hashCode();
        retained = versions;
        return digest;
    }

    private static long copyVersions(List<Integer> input) {
        ArrayList<Integer> base = new ArrayList<>(input);
        ArrayList<List<Integer>> versions = new ArrayList<>();
        long digest = 0;
        for (int step = 0; step < VERSIONS; step++) {
            int index = (step * 4051) & (SIZE - 1);
            ArrayList<Integer> version = new ArrayList<>(base);
            version.set(index, -step - 1);
            versions.add(version);
            digest = 31 * digest + version.get(index);
        }
        for (List<Integer> version : versions) digest = 31 * digest + version.hashCode();
        retained = versions;
        return digest;
    }

    private static long selectDigest(M3PListView<Integer> view, byte[] states, Backend backend) {
        M3PListView<Integer> result = view.select(states, 0x81, backend);
        long digest = 0;
        for (int value : result) digest = 31 * digest + value;
        retained = result;
        return digest;
    }

    private static void measure(String scenario, LongSupplier operation, long expected) {
        long allocation = allocated();
        long cpu = cpu();
        long start = System.nanoTime();
        long digest = operation.getAsLong();
        long wall = System.nanoTime() - start;
        long endCpu = cpu();
        long endAllocation = allocated();
        require(digest == expected, "Independent oracle digest: " + scenario);
        System.out.println("{\"scenario\":\"" + scenario + "\",\"wallNanos\":" + wall
                + ",\"cpuNanos\":" + (cpu < 0 || endCpu < 0 ? -1 : endCpu - cpu)
                + ",\"threadAllocatedBytes\":" + (allocation < 0 || endAllocation < 0 ? -1 : endAllocation - allocation)
                + ",\"digest\":" + digest + "}");
    }

    private static long allocated() {
        return ALLOCATION == null || !ALLOCATION.isThreadAllocatedMemorySupported()
                ? -1 : ALLOCATION.getThreadAllocatedBytes(Thread.currentThread().threadId());
    }
    private static long cpu() { return THREADS.isCurrentThreadCpuTimeSupported() ? THREADS.getCurrentThreadCpuTime() : -1; }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
