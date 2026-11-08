// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Random;

/** Optional diagnostic; an allocation probe is not a JMH throughput benchmark. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/test/java/com/synexia/algorithms/compression/M3CompressionAllocationProbe.java blob f48afc99f1d3; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class M3CompressionAllocationProbe {
    private static volatile long consumed;

    private M3CompressionAllocationProbe() {}

    public static void main(String[] args) {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) {
            throw new UnsupportedOperationException("thread allocation counters unavailable");
        }
        bean.setThreadAllocatedMemoryEnabled(true);
        byte[] random = new byte[256];
        byte[] repeated = new byte[256];
        new Random(42).nextBytes(random);
        Arrays.fill(repeated, (byte) 31);
        for (byte[] source : new byte[][] {random, repeated}) {
            String shape = source == random ? "random" : "repeated";
            M3SnappyRawEncoder snappy = new M3SnappyRawEncoder(12);
            M3Lz4RawBlockEncoder lz4 = new M3Lz4RawBlockEncoder(12);
            sample(bean, shape, "snappy", () -> snappy.encode(source, 1024));
            sample(bean, shape, "lz4", () -> lz4.encode(source, 1024));
        }
    }

    private interface EncoderCall {
        byte[] encode();
    }

    private static void sample(ThreadMXBean bean, String shape, String codec, EncoderCall call) {
        for (int i = 0; i < 10_000; i++) consumed += call.encode().length;
        long thread = Thread.currentThread().threadId();
        long allocatedBefore = bean.getThreadAllocatedBytes(thread);
        long started = System.nanoTime();
        for (int i = 0; i < 30_000; i++) consumed += call.encode().length;
        long elapsed = System.nanoTime() - started;
        long allocated = bean.getThreadAllocatedBytes(thread) - allocatedBefore;
        System.out.printf("%s %s bytes/op=%.1f ns/op=%.1f%n", codec, shape,
                allocated / 30_000.0, elapsed / 30_000.0);
    }
}
