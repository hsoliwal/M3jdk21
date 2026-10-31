/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Random;
import java.util.SortAlgorithm;
import java.util.Sorts;
import java.util.function.Consumer;

/**
 * Reproducible exploratory timings, not JMH or a universal performance claim.
 * The measured boundary includes input cloning, key/index setup and payload commit.
 * Run in a separate JVM from correctness tests; compare rows only at identical n/shape.
 */
public final class SortsBenchmark {
    private static volatile int sink;
    private static final com.sun.management.ThreadMXBean BEAN = bean();

    private SortsBenchmark() { }

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 512;
        int repeats = args.length > 1 ? Integer.parseInt(args[1]) : 9;
        if (n < 1 || n > 16384 || repeats < 3 || repeats > 100) {
            throw new IllegalArgumentException("n must be 1..16384; repetitions 3..100");
        }
        System.out.println("n,shape,algorithm,first_call_ns,warm_median_ns,median_cpu_ns,median_allocated_bytes,repetitions");
        for (String shape : new String[]{"random-256-keys", "ascending", "descending", "equal"}) {
            int[] input = new Random(0x33C05EEDL).ints(n, -128, 128).toArray();
            if (shape.equals("ascending")) Arrays.sort(input);
            if (shape.equals("descending")) {
                Arrays.sort(input);
                for (int i = 0; i < n / 2; i++) {
                    int save = input[i];
                    input[i] = input[n - 1 - i];
                    input[n - 1 - i] = save;
                }
            }
            if (shape.equals("equal")) Arrays.fill(input, 7);
            measure(input, shape, "JDK_ARRAYS_SORT", repeats, Arrays::sort);
            for (SortAlgorithm algorithm : SortAlgorithm.values()) {
                measure(input, shape, algorithm.name(), repeats,
                        values -> Sorts.sort(values, 0, values.length, algorithm, null));
            }
        }
        if (sink == Integer.MIN_VALUE) System.out.println("sink=" + sink);
    }

    private static com.sun.management.ThreadMXBean bean() {
        java.lang.management.ThreadMXBean base = ManagementFactory.getThreadMXBean();
        if (!(base instanceof com.sun.management.ThreadMXBean measured)) return null;
        if (measured.isThreadAllocatedMemorySupported() && !measured.isThreadAllocatedMemoryEnabled()) {
            measured.setThreadAllocatedMemoryEnabled(true);
        }
        if (measured.isCurrentThreadCpuTimeSupported() && !measured.isThreadCpuTimeEnabled()) {
            measured.setThreadCpuTimeEnabled(true);
        }
        return measured;
    }

    private static long allocated() {
        return BEAN != null && BEAN.isThreadAllocatedMemorySupported()
                ? BEAN.getThreadAllocatedBytes(Thread.currentThread().threadId()) : -1;
    }

    private static long cpu() {
        return BEAN != null && BEAN.isCurrentThreadCpuTimeSupported() ? BEAN.getCurrentThreadCpuTime() : -1;
    }

    private static void measure(int[] input, String shape, String name, int repeats,
                                 Consumer<int[]> sort) {
        long started = System.nanoTime();
        int[] first = input.clone();
        sort.accept(first);
        long firstCall = System.nanoTime() - started;
        verify(first);
        for (int warmup = 0; warmup < 12; warmup++) {
            int[] values = input.clone();
            sort.accept(values);
            sink = values[0];
        }
        long[] nanos = new long[repeats];
        long[] cpus = new long[repeats];
        long[] bytes = new long[repeats];
        for (int repetition = 0; repetition < repeats; repetition++) {
            long beforeBytes = allocated();
            long beforeCpu = cpu();
            long before = System.nanoTime();
            int[] values = input.clone();
            sort.accept(values);
            nanos[repetition] = System.nanoTime() - before;
            long afterCpu = cpu();
            long afterBytes = allocated();
            cpus[repetition] = beforeCpu < 0 ? -1 : afterCpu - beforeCpu;
            bytes[repetition] = beforeBytes < 0 ? -1 : afterBytes - beforeBytes;
            verify(values);
            sink = values[0];
        }
        Arrays.sort(nanos);
        Arrays.sort(cpus);
        Arrays.sort(bytes);
        System.out.println(input.length + "," + shape + "," + name + "," + firstCall + ","
                + nanos[repeats / 2] + "," + cpus[repeats / 2] + ","
                + bytes[repeats / 2] + "," + repeats);
    }

    private static void verify(int[] values) {
        for (int i = 1; i < values.length; i++) {
            if (values[i - 1] > values[i]) throw new AssertionError("Benchmark result is unsorted");
        }
    }
}
