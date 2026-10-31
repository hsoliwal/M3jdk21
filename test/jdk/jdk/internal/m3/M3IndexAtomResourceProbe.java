/*
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * Diagnostic resource probe, separate from behavioral acceptance.
 * Requires java.management and jdk.management in addition to patched java.base.
 */

import jdk.internal.m3.M3IndexedArray;
import jdk.internal.m3.M3IntAlgorithms;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.CollectionViews;
import java.util.Comparator;
import java.util.List;

/** Emits raw cold/warm host observations; does not enforce a performance threshold. */
public final class M3IndexAtomResourceProbe {
    private static volatile Object escaped;
    private static volatile long checksum;

    private record Item(int key, int occurrence) { }

    private M3IndexAtomResourceProbe() { }

    public static void main(String[] args) {
        int size = args.length > 0 ? Integer.parseInt(args[0]) : 4096;
        int iterations = args.length > 1 ? Integer.parseInt(args[1]) : 200;
        if (size < 2 || size > 32768 || iterations < 1 || iterations > 1_000_000) {
            throw new IllegalArgumentException("size 2..32768; iterations 1..1000000");
        }
        if (!(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean)
                || !bean.isThreadAllocatedMemorySupported()) {
            throw new UnsupportedOperationException("per-thread allocation measurement unavailable");
        }
        if (!bean.isCurrentThreadCpuTimeSupported()) {
            throw new UnsupportedOperationException("current-thread CPU measurement unavailable");
        }
        if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        if (!bean.isThreadCpuTimeEnabled()) bean.setThreadCpuTimeEnabled(true);
        System.out.println("# diagnostic only; compare alternating fresh baseline/candidate JVMs");
        System.out.println("# cold=first workload call; warm=after fixed in-process warmup");
        System.out.println("workload,phase,size,operations,allocated_bytes,elapsed_ns,thread_cpu_ns");

        int[] dense = new int[size];
        int[] sparse = new int[size];
        for (int i = 0; i < size; i++) {
            dense[i] = i * 3;
            sparse[i] = (int) ((long) Integer.MIN_VALUE + i * 65537L);
        }
        run(bean, "lonely-dense-all", size, iterations, () -> consume(M3IntAlgorithms.lonelyNumbers(dense)));
        run(bean, "lonely-sparse-all", size, iterations, () -> consume(M3IntAlgorithms.lonelyNumbers(sparse)));

        M3IndexedArray<Item> owner = new M3IndexedArray<>();
        Item first = new Item(17 % 11, 0);
        long firstHandle = owner.append(first);
        for (int i = 1; i < size; i++) owner.append(new Item((i * 17) % 11, i));
        List<Item> sorted = owner.sorted(Comparator.comparingInt(Item::key));
        run(bean, "indexed-sorted-refresh", size, iterations, () -> {
            if (!owner.replace(firstHandle, first)) throw new AssertionError("unleased benchmark owner");
            int count = sorted.size();
            checksum += count + sorted.get(0).occurrence() + sorted.get(count - 1).occurrence();
            escaped = sorted;
        });

        ArrayList<Integer> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) values.add(i);
        Collection<Integer> firstHit = CollectionViews.filtered(values, value -> value == 0);
        Collection<Integer> noHit = CollectionViews.filtered(values, value -> value < 0);
        run(bean, "filtered-empty-first-hit", size, iterations, () -> {
            checksum += firstHit.isEmpty() ? 1 : 2;
            escaped = firstHit;
        });
        run(bean, "filtered-empty-no-hit", size, iterations, () -> {
            checksum += noHit.isEmpty() ? 1 : 2;
            escaped = noHit;
        });
        System.out.println("# checksum=" + checksum + "; escaped=" + escaped.getClass().getName());
    }

    private static void consume(int[] values) {
        escaped = values;
        checksum += values.length;
        if (values.length != 0) checksum += values[0] + (long) values[values.length - 1];
    }

    private static void run(com.sun.management.ThreadMXBean bean, String name,
            int size, int iterations, Runnable operation) {
        measure(bean, name, "cold", size, 1, operation);
        int warmup = Math.max(100, Math.min(1000, iterations));
        for (int i = 0; i < warmup; i++) operation.run();
        measure(bean, name, "warm", size, iterations, operation);
    }

    private static void measure(com.sun.management.ThreadMXBean bean, String name,
            String phase, int size, int operations, Runnable operation) {
        long thread = Thread.currentThread().threadId();
        long cpuBefore = bean.getCurrentThreadCpuTime();
        long allocatedBefore = bean.getThreadAllocatedBytes(thread);
        long started = System.nanoTime();
        for (int i = 0; i < operations; i++) operation.run();
        long elapsed = System.nanoTime() - started;
        long allocated = bean.getThreadAllocatedBytes(thread) - allocatedBefore;
        long threadCpu = bean.getCurrentThreadCpuTime() - cpuBefore;
        System.out.println(name + "," + phase + "," + size + "," + operations + ","
                + allocated + "," + elapsed + "," + threadCpu);
    }
}
