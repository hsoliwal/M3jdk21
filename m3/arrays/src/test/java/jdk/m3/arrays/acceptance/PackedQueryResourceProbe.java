// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.acceptance;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.IntSupplier;
import jdk.m3.arrays.M3ArrayNative;
import jdk.m3.arrays.packed.Int40Array;

/** Local allocation/work observation, not a portable speed or runtime-image admission gate. */
public final class PackedQueryResourceProbe {
    private static volatile int observed;
    private PackedQueryResourceProbe() { }

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("explicit JNI library path required");
        M3ArrayNative.load(Path.of(args[0]));
        final int length = 262_144;
        Int40Array packed = new Int40Array(length);
        for (int i = 0; i < length; i++) packed.set(i, i % 2048 - 1024);
        byte[] bytes = packed.toByteArray();
        int expected = 23 * (length / 2048);
        IntSupplier expanded = () -> {
            int count = 0;
            for (long value : packed.toLongArray()) if (value >= -11 && value <= 11) count++;
            return count;
        };
        IntSupplier direct = () -> packed.countInRange(0, length, -11, 11);
        IntSupplier nativeScan = () -> M3ArrayNative.countPackedInRangeRequired(bytes, 40, 0, length, -11, 11);
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation counters unavailable");
        if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        for (int i = 0; i < 80; i++) {
            observed = expanded.getAsInt(); observed = direct.getAsInt(); observed = nativeScan.getAsInt();
        }
        measure("expand_then_count", expanded, expected, bean);
        measure("direct_java_count", direct, expected, bean);
        measure("bounded_jni_count", nativeScan, expected, bean);
        System.out.printf(Locale.ROOT,
                "PACKED_QUERY_SCOPE elements=%d packed_payload_bytes=%d decoded_payload_bytes=%d native_scratch_bytes=8192 input_prebuilt=true timing_is_smoke_only=true result=%d%n",
                length, packed.payloadBytes(), length * 8L, observed);
    }

    private static void measure(String name, IntSupplier operation, int expected, ThreadMXBean bean) {
        long[] time = new long[25], allocated = new long[25];
        long thread = Thread.currentThread().threadId();
        for (int i = 0; i < time.length; i++) {
            long allocationStart = bean.getThreadAllocatedBytes(thread);
            long start = System.nanoTime();
            int result = operation.getAsInt();
            time[i] = System.nanoTime() - start;
            allocated[i] = bean.getThreadAllocatedBytes(thread) - allocationStart;
            if (result != expected) throw new AssertionError(name + " exact count: " + result);
            observed = result;
        }
        Arrays.sort(time); Arrays.sort(allocated);
        System.out.printf(Locale.ROOT, "PACKED_QUERY_RESOURCE operation=%s median_ns=%d median_thread_allocated_bytes=%d%n",
                name, time[time.length / 2], allocated[allocated.length / 2]);
    }
}
