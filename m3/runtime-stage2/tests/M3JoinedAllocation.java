/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.lang.management.ManagementFactory;

public class M3JoinedAllocation {
    private static volatile Object sink;

    public static void main(String[] args) {
        int iterations = Integer.getInteger("m3.iterations", 20_000);
        String left = "L".repeat(4096);
        String middle = "M".repeat(4096);
        String right = "R".repeat(4096);

        for (int i = 0; i < 5_000; i++) {
            sink = String.join("|", left, middle, right);
        }

        com.sun.management.ThreadMXBean bean =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        long started = System.nanoTime();
        long logicalChars = 0;
        for (int i = 0; i < iterations; i++) {
            String value = String.join("|", left, middle, right);
            logicalChars += value.length();
            sink = value;
        }
        long elapsed = System.nanoTime() - started;
        long allocated = bean.getThreadAllocatedBytes(thread) - before;
        System.out.println("M3_STRING_ALLOCATION iterations=" + iterations
                + " logicalChars=" + logicalChars
                + " allocatedBytes=" + allocated
                + " elapsedNanos=" + elapsed
                + " enabled=" + Boolean.getBoolean("m3.enabled"));
    }
}
