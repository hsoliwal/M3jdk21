/* Copyright (c) 2026, Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.M3FilteredViews;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Spliterator;
import java.util.TreeSet;
import jdk.internal.mindex.M3RX7;

/** Informational same-host diagnostic. Not JMH or a whole-runtime performance gate. */
public final class ConvergenceCostProbe {
    private static volatile Object objectSink;
    private static volatile long valueSink;
    private ConvergenceCostProbe() { }
    private static long split(Spliterator<Integer> value) {
        objectSink = value;
        Spliterator<Integer> prefix = value.trySplit();
        return prefix == null ? 1 : 1 + split(prefix) + split(value);
    }
    public static void main(String[] args) {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        System.out.println("case,size,round,ns_per_call,java_bytes_per_call");
        for (int size : new int[]{64, 1024, 4096}) {
            TreeSet<Integer> source = new TreeSet<>(Comparator.reverseOrder());
            for (int i = 0; i < size; i++) source.add(i);
            var view = M3FilteredViews.filter(source, x -> true);
            for (int warm = 0; warm < 50; warm++) valueSink = split(view.spliterator());
            for (int round = 0; round < 5; round++) {
                long bytes = bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
                long start = System.nanoTime();
                for (int n = 0; n < 25; n++) valueSink = split(view.spliterator());
                long elapsed = System.nanoTime() - start;
                bytes = bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) - bytes;
                System.out.println("sorted-split," + size + "," + round + "," + elapsed / 25 + "," + bytes / 25);
            }
            objectSink = null;
        }
        for (int size : new int[]{1000, 100000}) {
            for (String shape : new String[]{"early", "absent", "longest"}) {
                boolean longest = shape.equals("longest");
                var p = M3RX7.fromLexicalNfa("0".repeat(64), 0,
                        longest ? new int[]{0, 1} : new int[]{0, 1, 1},
                        new int[]{longest ? 0 : 1}, new int[]{longest ? -1 : 1}, new long[]{-1},
                        new int[1], new int[1], longest ? new byte[]{1} : new byte[]{0, 1});
                int[] kinds = new int[size]; long[] ids = new long[size]; int[] flags = new int[size];
                if (shape.equals("early")) kinds[0] = 1;
                Arrays.fill(ids, 4);
                for (int warm = 0; warm < 30; warm++) valueSink = p.find(kinds, ids, flags);
                for (int round = 0; round < 5; round++) {
                    long bytes = bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
                    long start = System.nanoTime();
                    for (int n = 0; n < 30; n++) valueSink = p.find(kinds, ids, flags);
                    long elapsed = System.nanoTime() - start;
                    bytes = bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) - bytes;
                    System.out.println("find-" + shape + "," + size + "," + round + "," + elapsed / 30 + "," + bytes / 30);
                }
            }
        }
    }
}
