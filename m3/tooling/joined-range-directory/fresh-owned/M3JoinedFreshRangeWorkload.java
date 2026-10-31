/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.LocalM3Arena;
import com.m3.text.M3StringPiece;
import java.lang.management.ManagementFactory;
import java.util.Locale;

/** Diagnostic decomposition only; the unchanged original combined harness remains authoritative. */
public final class M3JoinedFreshRangeWorkload {
    private static volatile M3StringPiece sink;
    private M3JoinedFreshRangeWorkload() { }
    private static long endpoints(M3StringPiece result, int iterations) {
        long checksum = 0;
        for (int i = 0; i < iterations; i++) {
            checksum += result.length();
            if (result.length() != 0) checksum += result.charAt(0) + result.charAt(result.length() - 1);
            sink = result;
        }
        return checksum;
    }
    private static void construction(M3StringPiece source, int iterations) {
        int length = source.length();
        for (int i = 0; i < iterations; i++) sink = source.subSequence(0, length);
    }
    public static void main(String[] args) {
        if (args.length != 2 || !(args[0].equals("construction") || args[0].equals("endpoints")))
            throw new IllegalArgumentException("construction|endpoints <1024|8192>");
        int count = Integer.parseInt(args[1]);
        if (count != 1024 && count != 8192) throw new IllegalArgumentException("bounded full-range sizes");
        LocalM3Arena arena = new LocalM3Arena();
        M3StringPiece[] input = new M3StringPiece[count];
        for (int i = 0; i < count; i++)
            input[i] = arena.copyUtf16(new char[]{(char) (i % 7 == 0 ? 0xd800 : i % 7 == 1 ? 0xdc00 : i % 256)});
        M3StringPiece source = M3StringPiece.join(input);
        String oracle = source.flatten();
        M3StringPiece result = source.subSequence(0, count);
        if (!result.flatten().equals(oracle)) throw new AssertionError("full range oracle");
        var raw = ManagementFactory.getThreadMXBean();
        if (!(raw instanceof com.sun.management.ThreadMXBean bean) || !bean.isThreadAllocatedMemorySupported())
            throw new IllegalStateException("HOLD_THREAD_ALLOCATION_ACCOUNTING_UNAVAILABLE");
        if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        boolean constructing = args[0].equals("construction");
        if (constructing) construction(source, 128); else endpoints(result, 128);
        long thread = Thread.currentThread().threadId();
        long bytesBefore = bean.getThreadAllocatedBytes(thread);
        long started = System.nanoTime();
        long checksum;
        if (constructing) { construction(source, 1024); checksum = 0; }
        else checksum = endpoints(result, 1024);
        long elapsed = System.nanoTime() - started;
        long allocated = bean.getThreadAllocatedBytes(thread) - bytesBefore;
        M3StringPiece last = sink;
        long expected = ((long) count + oracle.charAt(0) + oracle.charAt(count - 1)) * 1024L;
        if (last.length() != count || !last.flatten().equals(oracle) || (!constructing && checksum != expected)
                || elapsed <= 0 || allocated < 0) throw new AssertionError("diagnostic workload outcome");
        System.out.printf(Locale.ROOT, "{\"schema\":\"M3_JOINED_FRESH_RANGE_DIAGNOSTIC_V1\","
                + "\"mode\":\"%s\",\"leaves\":%d,\"iterations\":1024,\"warmup\":128,"
                + "\"elapsedNanos\":%d,\"allocatedBytes\":%d,\"checksum\":%d}%n",
                args[0], count, elapsed, allocated, checksum);
    }
}
