// SPDX-License-Identifier: Apache-2.0
package jdk.m3.regexcursor;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

/** Descriptive JVM-local allocation and time microbench, never a speedup guarantee. */
public final class M3TokenCursorMultiLaneBench {
    private M3TokenCursorMultiLaneBench() { }
    private static volatile long sink;

    private static int[][] parts(int lanes) {
        int[][] output = new int[lanes][];
        for (int i = 0; i < lanes; i++) {
            output[i] = i % 5 == 0 ? new int[0] : new int[] {i + 3, i + 17, i + 23};
        }
        return output;
    }

    private static void measure(String name, int[][] input) {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("JVM Thread allocated byte counter unsupported");
        }
        bean.setThreadAllocatedMemoryEnabled(true);
        long id = Thread.currentThread().threadId();
        long[] allocations = new long[7], nanos = new long[7];
        long sum = 0;
        for (int i = 0; i < 60_000; i++) sum += M3TokenCursor.lanes(input).totalLength();
        int iterations = 180_000;
        for (int sample = 0; sample < 7; sample++) {
            long before = bean.getThreadAllocatedBytes(id);
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                M3TokenCursor cursor = M3TokenCursor.lanes(input);
                sum += cursor.totalLength() + (cursor.unitPresenceMask() & 1);
            }
            nanos[sample] = System.nanoTime() - start;
            allocations[sample] = bean.getThreadAllocatedBytes(id) - before;
        }
        sink = sum;
        Arrays.sort(allocations);
        Arrays.sort(nanos);
        System.out.printf("M3_MULTILANE_BENCH %s lanes=%d allocation_bytes_per_call=%.2f ns_per_call=%.2f checksum=%d%n",
                name, input.length, allocations[3] / (double) iterations,
                nanos[3] / (double) iterations, sink);
    }

    public static void main(String[] args) {
        measure("empty", parts(0));
        measure("two", parts(2));
        measure("eight", parts(8));
        measure("thirtytwo", parts(32));
    }
}
