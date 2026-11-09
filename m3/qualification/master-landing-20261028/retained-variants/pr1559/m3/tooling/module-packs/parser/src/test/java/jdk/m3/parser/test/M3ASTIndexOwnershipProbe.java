// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.URI;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3File;

/** Same-input baseline/postimage resource probe; timing is finite evidence only. */
public final class M3ASTIndexOwnershipProbe {
    private static final M3File SOURCE = M3File.of(URI.create("memory:/Probe.java"), "class Probe {}");
    private static final String[] KINDS = new String[16];
    private static volatile M3ASTIndex indexSink;
    private static volatile M3ASTIndex.Builder builderSink;
    static { for (int i = 0; i < KINDS.length; i++) KINDS[i] = "K" + i; }
    private M3ASTIndexOwnershipProbe() { }
    private static void publish() {
        M3ASTIndex.Builder builder = M3ASTIndex.builder(SOURCE, M3ASTIndex.Domain.ECLIPSE_JDT, 32);
        for (int row = 0; row < 32; row++) builder.add(row == 0 ? -1 : 0, KINDS[row % 16], -1, -1, 0);
        indexSink = builder.seal();
        builderSink = builder; // A real caller retaining both objects defeats escape elimination.
    }
    public static void main(String[] args) throws Exception {
        ThreadMXBean thread = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!thread.isThreadAllocatedMemorySupported() || !thread.isCurrentThreadCpuTimeSupported()) {
            throw new IllegalStateException("allocation and CPU counters required");
        }
        thread.setThreadAllocatedMemoryEnabled(true);
        thread.setThreadCpuTimeEnabled(true);
        for (int i = 0; i < 5_000; i++) publish();
        int runs = 20_000;
        long allocated = thread.getCurrentThreadAllocatedBytes();
        long cpu = thread.getCurrentThreadCpuTime();
        long wall = System.nanoTime();
        for (int i = 0; i < runs; i++) publish();
        wall = System.nanoTime() - wall;
        cpu = thread.getCurrentThreadCpuTime() - cpu;
        allocated = thread.getCurrentThreadAllocatedBytes() - allocated;
        long retainedIntBytes = 0;
        int retainedReferences = 0;
        for (Field field : builderSink.getClass().getDeclaredFields()) {
            if (field.getType().isPrimitive()) continue;
            field.setAccessible(true);
            Object value = field.get(builderSink);
            if (value != null) retainedReferences++;
            if (value instanceof int[] lane) retainedIntBytes += (long) lane.length * Integer.BYTES;
        }
        System.out.println("{\"runs\":" + runs + ",\"nodesPerIndex\":32,\"kindCount\":16"
                + ",\"allocatedBytesPerOperation\":" + ((double) allocated / runs)
                + ",\"cpuNanosPerOperation\":" + ((double) cpu / runs)
                + ",\"wallNanosPerOperation\":" + ((double) wall / runs)
                + ",\"publishedPrimitiveBytes\":" + indexSink.primitiveBytes()
                + ",\"retainedBuilderIntPayloadBytes\":" + retainedIntBytes
                + ",\"retainedBuilderReferences\":" + retainedReferences + "}");
    }
}
