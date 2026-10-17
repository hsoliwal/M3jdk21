// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Random;

/** Standalone Java21/JNI parity and allocated-heap-byte oracle. */
public final class M3IntFilterDenseContract {
    private static int checks;
    private M3IntFilterDenseContract() { }
    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label);
    }
    private static long heapBytes(int[] values, int threshold, int repetitions) {
        com.sun.management.ThreadMXBean bean =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new AssertionError("thread allocation unsupported");
        if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        int sink = 0;
        for (int i = 0; i < 800; i++) sink += M3IntFilterKernel.indicesGreaterOrEqualNative(values, threshold).length;
        long start = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < repetitions; i++) sink += M3IntFilterKernel.indicesGreaterOrEqualNative(values, threshold).length;
        long used = bean.getThreadAllocatedBytes(thread) - start;
        if (sink <= 0 && threshold == Integer.MIN_VALUE) throw new AssertionError("unused output");
        return used / repetitions;
    }
    public static void main(String[] args) {
        check(M3IntFilterKernel.nativeAvailable(), "native required");
        int n = 16384;
        int[] values = new int[n];
        Arrays.fill(values, 42);
        for (int threshold : new int[]{Integer.MIN_VALUE, 0, 42, 43, Integer.MAX_VALUE}) {
            check(Arrays.equals(M3IntFilterKernel.indicesGreaterOrEqualNative(values, threshold),
                    M3IntFilterKernel.indicesGreaterOrEqualJava(values, threshold)), "dense/or empty parity");
        }
        Random random = new Random(20261017L);
        for(int i=0;i<400;i++) {
            int[] candidate = new int[(i*37)%32768];
            for(int j=0;j<candidate.length;j++) candidate[j]=random.nextInt();
            int threshold=random.nextInt();
            check(Arrays.equals(M3IntFilterKernel.indicesGreaterOrEqualNative(candidate,threshold),
                    M3IntFilterKernel.indicesGreaterOrEqualJava(candidate,threshold)), "random row order "+i);
        }
        long dense=heapBytes(values,Integer.MIN_VALUE,1000);
        long empty=heapBytes(values,Integer.MAX_VALUE,1000);
        System.out.printf("DENSE_BYTES_PER_CALL=%d EMPTY_BYTES_PER_CALL=%d%n",dense,empty);
        check(dense-empty < n * 2L, "dense native selection must not allocate an extra full-length result copy");
        System.out.println("MAT_INT_FILTER_DENSE_PASS checks="+checks);
    }
}
