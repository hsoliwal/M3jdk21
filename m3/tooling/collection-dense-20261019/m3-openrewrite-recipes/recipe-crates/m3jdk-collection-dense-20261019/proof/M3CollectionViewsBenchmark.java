/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package java.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.function.Predicate;

/**
 * Manual complete-call comparison of filteredSortedSnapshot and capture into
 * ArrayList, removeIf, List.sort, then an unmodifiable wrapper. Each measured
 * call includes input capture, filtering, sorting and all returned-container
 * allocation. Caller-owned input records and callbacks are prepared outside
 * measurement; every returned list escapes through a volatile sink.
 *
 * All six workload results are checked for reference identity, stable ties,
 * predicate membership, source preservation and unmodifiable output before
 * any report is printed. This is a bounded qualification probe, not JMH, a
 * timing assertion, a product-image result, or authority to promote JNI.
 * Run separate JVMs for baseline/integrated to limit cross-backend JIT effects.
 * Native request, if supplied, uses the existing internal startup property;
 * this benchmark never enables it based on an observed duration.
 *
 * Compile/run with java.base reads to java.management and jdk.management.
 * An optional test.m3.masks.native.library path is loaded by this bootstrap
 * test class only for stock-runtime native qualification.
 */
public class M3CollectionViewsBenchmark {
    private M3CollectionViewsBenchmark() { }
    private record Item(int rank, int occurrence) { }
    private record Workload(String density, List<Item> source,
                            Predicate<Item> selection, Predicate<Item> rejection, List<Item> expected) { }
    private static final Comparator<Item> ORDER = Comparator.comparingInt(Item::rank);
    private static final Predicate<Item> SPARSE = item -> (item.occurrence() & 15) == 0;
    private static final Predicate<Item> DENSE = item -> (item.occurrence() & 3) != 3;
    private static final ThreadMXBean CPU = ManagementFactory.getThreadMXBean();
    private static final com.sun.management.ThreadMXBean ALLOCATION =
            CPU instanceof com.sun.management.ThreadMXBean bean ? bean : null;
    private static final long THREAD = Thread.currentThread().threadId();
    private static volatile List<Item> sink;
    private static volatile long checksum;
    private static boolean cpuSupported;
    private static boolean allocationSupported;

    public static void main(String[] args) {
        String requested = args.length == 0 ? "both" : args[0];
        if (args.length > 1 || !Set.of("baseline", "integrated", "both").contains(requested)) {
            throw new IllegalArgumentException("backend must be baseline, integrated, or both");
        }
        String library = System.getProperty("test.m3.masks.native.library");
        if (library != null) System.load(library);
        List<Workload> workloads = prepare();
        String focusSize = System.getProperty("m3.bench.size");
        String focusDensity = System.getProperty("m3.bench.density");
        if (focusSize != null) {
            int size = Integer.parseInt(focusSize);
            workloads.removeIf(workload -> workload.source().size() != size);
        }
        if (focusDensity != null) workloads.removeIf(workload -> !workload.density().equals(focusDensity));
        if (workloads.isEmpty()) throw new IllegalArgumentException("empty focused workload");
        qualify(workloads);
        cpuSupported = CPU.isCurrentThreadCpuTimeSupported();
        if (cpuSupported && !CPU.isThreadCpuTimeEnabled()) CPU.setThreadCpuTimeEnabled(true);
        allocationSupported = ALLOCATION != null && ALLOCATION.isThreadAllocatedMemorySupported();
        if (allocationSupported && !ALLOCATION.isThreadAllocatedMemoryEnabled())
            ALLOCATION.setThreadAllocatedMemoryEnabled(true);
        System.out.println("{\"schema\":1,\"probe\":\"M3CollectionViews.filteredSortedSnapshot.complete-call\","
                + "\"java\":\"" + Runtime.version() + "\",\"arch\":\"" + System.getProperty("os.arch") + "\","
                + "\"semantic_workloads\":" + workloads.size() + ",\"semantic_preflight\":\"PASSED\","
                + "\"sparse_fraction\":\"1/16\",\"dense_fraction\":\"3/4\",\"rank_groups\":32,"
                + "\"warmup_target_millis\":300,\"warmup_max_batches\":64,"
                + "\"cpu_supported\":" + cpuSupported + ",\"allocation_supported\":" + allocationSupported + ","
                + "\"native_requested\":" + Boolean.parseBoolean(jdk.internal.misc.VM.getSavedProperty(
                        "jdk.m3.collectionMasks.native")) + "}");
        for (Workload workload : workloads) {
            int iterations = Math.max(16, Math.min(10_000, 1_500_000 / workload.source().size()));
                        for (boolean integrated : new boolean[] {false, true})
                if (selected(requested, integrated)) warmup(integrated, workload, iterations);
            for (int round = 0; round < 5; round++) {
                for (int position = 0; position < 2; position++) {
                    boolean integrated = ((position + round) & 1) != 0;
                    if (selected(requested, integrated)) measure(integrated, workload, iterations, round);
                }
            }
        }
        if (sink == null) throw new AssertionError("no benchmark call escaped");
        System.out.println("{\"checksum\":" + checksum + ",\"default_dispatch\":\"JAVA_UNCHANGED\"}");
    }

    private static List<Workload> prepare() {
        List<Workload> workloads = new ArrayList<>();
        for (int length : new int[] {65, 1024, 65536}) {
            ArrayList<Item> source = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                int mixed = index * 0x9e3779b9;
                mixed ^= mixed >>> 16;
                // Both predicates select positions zero and sixteen. Their
                // equal ranks guarantee that every workload exercises ties.
                int rank = index == 0 || index == 16 ? 7 : mixed & 31;
                source.add(new Item(rank, index));
            }
            for (String density : new String[] {"sparse", "dense", "all"}) {
                Predicate<Item> selection = density.equals("sparse") ? SPARSE
                        : density.equals("dense") ? DENSE : item -> true;
                ArrayList<Item> expected = new ArrayList<>();
                for (Item item : source) if (selection.test(item)) expected.add(item);
                expected.sort(ORDER);
                workloads.add(new Workload(density, source, selection, selection.negate(), expected));
            }
        }
        return workloads;
    }

    private static void qualify(List<Workload> workloads) {
        for (Workload workload : workloads) {
            Object[] before = workload.source().toArray();
            for (boolean integrated : new boolean[] {false, true}) {
                List<Item> actual = invoke(integrated, workload);
                if (actual.size() != workload.expected().size()) throw new AssertionError("selection size");
                int ties = 0;
                for (int index = 0; index < actual.size(); index++) {
                    Item item = actual.get(index);
                    if (item != workload.expected().get(index) || !workload.selection().test(item))
                        throw new AssertionError("selected reference or ordering mismatch");
                    if (index != 0 && ORDER.compare(actual.get(index - 1), item) == 0) {
                        ties++;
                        if (actual.get(index - 1).occurrence() >= item.occurrence())
                            throw new AssertionError("unstable equal-rank ordering");
                    }
                }
                if (ties == 0) throw new AssertionError("workload failed to exercise stable ties");
                if (workload.source().size() != before.length) throw new AssertionError("source size changed");
                for (int index = 0; index < before.length; index++)
                    if (workload.source().get(index) != before[index]) throw new AssertionError("source changed");
                try {
                    actual.add(new Item(-1, -1));
                    throw new AssertionError("output accepts add");
                } catch (UnsupportedOperationException expected) { }
                try {
                    actual.set(0, actual.get(0));
                    throw new AssertionError("output accepts set");
                } catch (UnsupportedOperationException expected) { }
            }
        }
    }

    private static boolean selected(String requested, boolean integrated) {
        return requested.equals("both") || requested.equals(integrated ? "integrated" : "baseline");
    }

    private static List<Item> invoke(boolean integrated, Workload workload) {
        if (integrated)
            return Collections.filteredSortedSnapshot(workload.source(), workload.selection(), ORDER);
        ArrayList<Item> result = new ArrayList<>(workload.source());
        result.removeIf(workload.rejection());
        result.sort(ORDER);
        return Collections.unmodifiableList(result);
    }

    private static void warmup(boolean integrated, Workload workload, int iterations) {
        long started = System.nanoTime();
        int batches = 0;
        // Bounded time warmup permits compilation after a new workload shape;
        // a small call count alone left visible compilation drift in a pilot.
        do {
            repeat(integrated, workload, iterations);
        } while (++batches < 64 && System.nanoTime() - started < 300_000_000L);
    }

    private static void repeat(boolean integrated, Workload workload, int iterations) {
        long sum = 0;
        for (int iteration = 0; iteration < iterations; iteration++) {
            List<Item> result = invoke(integrated, workload);
            sink = result;
            sum += result.size();
            if (!result.isEmpty()) sum += result.getFirst().occurrence() + result.getLast().occurrence();
        }
        checksum += sum;
    }

    private static void measure(boolean integrated, Workload workload, int iterations, int round) {
        long allocatedBefore = allocationSupported ? ALLOCATION.getThreadAllocatedBytes(THREAD) : 0L;
        long cpuBefore = cpuSupported ? CPU.getCurrentThreadCpuTime() : 0L;
        long wallBefore = System.nanoTime();
        repeat(integrated, workload, iterations);
        long wall = System.nanoTime() - wallBefore;
        long cpu = cpuSupported ? CPU.getCurrentThreadCpuTime() - cpuBefore : 0L;
        long allocated = allocationSupported ? ALLOCATION.getThreadAllocatedBytes(THREAD) - allocatedBefore : 0L;
        System.out.printf(Locale.ROOT,
                "{\"backend\":\"%s\",\"n\":%d,\"density\":\"%s\",\"selected\":%d,"
                + "\"iterations\":%d,\"round\":%d,\"wall_ns_per_call\":%.3f,"
                + "\"cpu_ns_per_call\":%s,\"allocated_bytes_per_call\":%s}%n",
                integrated ? "integrated" : "baseline", workload.source().size(), workload.density(),
                workload.expected().size(), iterations, round, (double)wall / iterations,
                cpuSupported ? String.format(Locale.ROOT, "%.3f", (double)cpu / iterations) : "null",
                allocationSupported ? String.format(Locale.ROOT, "%.3f", (double)allocated / iterations) : "null");
    }
}
