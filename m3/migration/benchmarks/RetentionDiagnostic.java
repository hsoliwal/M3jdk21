/* Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0 */
package com.m3.text;

import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.Locale;

/** Bounded diagnostic, not JMH or a claim about complete retained heap/native/mapped memory. */
public final class RetentionDiagnostic {
    private static volatile Object sink;
    private RetentionDiagnostic() { }
    private static Object field(Object object, String name) throws Exception {
        Field f = object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object);
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !(args[0].equals("flat") || args[0].equals("view"))) throw new IllegalArgumentException("flat or view");
        boolean indexed = args[0].equals("view");
        String left = "L" + "\u0100".repeat(2047), right = "R" + "\u0200".repeat(2047);
        M3Text a = M3Text.fromString(left), b = M3Text.fromString(right), liveJoin = a.concat(b);
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported() || !bean.isCurrentThreadCpuTimeSupported()) throw new IllegalStateException("unsupported metrics");
        bean.setThreadAllocatedMemoryEnabled(true); bean.setThreadCpuTimeEnabled(true);
        for (int i = 0; i < 2000; i++) sink = indexed ? a.concat(b) : left.concat(right);
        Object[] retained = new Object[2000];
        long thread = Thread.currentThread().threadId();
        long allocated = bean.getThreadAllocatedBytes(thread), cpu = bean.getCurrentThreadCpuTime(), start = System.nanoTime();
        for (int i = 0; i < retained.length; i++) retained[i] = indexed ? a.concat(b) : left.concat(right);
        long elapsed = System.nanoTime() - start;
        cpu = bean.getCurrentThreadCpuTime() - cpu;
        allocated = bean.getThreadAllocatedBytes(thread) - allocated;
        String expected = left.concat(right);
        for (Object value : retained) if (!expected.contentEquals((CharSequence) value)) throw new AssertionError("text mismatch");
        if (indexed) for (Object value : retained) if (value != liveJoin) throw new AssertionError("live facade not reused");
        LocalM3StringPiece large = new LocalM3Arena().copyUtf16(new char[1 << 20]);
        LocalM3StringPiece tiny = large.subSequence(17, 18);
        if (field(large, "backing") != field(tiny, "backing")) throw new AssertionError("slice payload changed");
        System.out.printf(Locale.ROOT,
                "{\"mode\":\"%s\",\"joins\":2000,\"utf16UnitsPerJoin\":4096,\"warmupJoins\":2000,\"allocatedBytes\":%d,\"cpuNanos\":%d,\"elapsedNanos\":%d,\"tinySliceUnits\":1,\"tinySliceRetainedPayloadBytes\":%d}%n",
                args[0], allocated, cpu, elapsed, tiny.retainedBackingBytes());
        sink = retained; Reference.reachabilityFence(liveJoin); Reference.reachabilityFence(tiny);
    }
}
