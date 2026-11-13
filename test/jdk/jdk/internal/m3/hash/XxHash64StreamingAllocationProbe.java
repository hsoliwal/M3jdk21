/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * Not a jtreg test: the allocation diagnostic of recipe make/m3/qualification/hash-xxhash64-streaming-20261106 (run by its
 * differential.sh on the donor class and on M3XxHash64Streaming; the figures are recorded in GATES.tsv, never asserted).
 * java --add-exports java.base/jdk.internal.m3.hash=ALL-UNNAMED M3XxHash64StreamingAllocationProbe
 */

import jdk.internal.m3.hash.XxHash64Streaming;

import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;

/**
 * Optional single-thread allocation diagnostic: run against old and new class files.
 * Neither elapsed time nor allocation counts are pass/fail performance thresholds.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-fileindex/src/test/java/com/synexia/fileindex/content/XXHash64AllocationProbe.java blob 2b3d933711d9; recipe make/m3/qualification/hash-xxhash64-streaming-20261106/recipe/relocate.py v4
public final class XxHash64StreamingAllocationProbe {
    private XxHash64StreamingAllocationProbe() {}

    public static void main(String[] args) {
        ByteBuffer data = ByteBuffer.allocateDirect(256 * 1024);
        for (int i = 0; i < data.capacity(); i++) {
            data.put((byte) i);
        }
        data.flip();
        long checksum = 0;
        for (int i = 0; i < 80; i++) {
            data.position(0);
            XxHash64Streaming hasher = new XxHash64Streaming(0L);
            hasher.update(data);
            checksum ^= hasher.getValue();
        }
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean)
            ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) {
            System.out.println("SKIP: thread allocation counters unavailable");
            return;
        }
        if (!bean.isThreadAllocatedMemoryEnabled()) {
            bean.setThreadAllocatedMemoryEnabled(true);
        }
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        long start = System.nanoTime();
        for (int i = 0; i < 120; i++) {
            data.position(0);
            XxHash64Streaming hasher = new XxHash64Streaming(0L);
            hasher.update(data);
            checksum ^= hasher.getValue();
        }
        long nanos = System.nanoTime() - start;
        long allocated = bean.getThreadAllocatedBytes(thread) - before;
        System.out.println("hashedMiB=30 allocatedBytes=" + allocated + " nanos=" + nanos
            + " checksum=" + checksum);
    }
}
