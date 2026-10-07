/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * Bounded diagnostics, not a statistically controlled benchmark or a performance gate.
 * Cold allocation includes owner construction and insertion. Generic payloads are prepared
 * outside measurement; boxed-long admission is deliberately measured as a separate case.
 * The same keys, operation counts, VM, and deterministic order serve each paired comparison.
 */
public final class M3CollectionCostProbe {
    private static final int SAMPLES = 7;
    private static volatile long blackhole;
    private static final com.sun.management.ThreadMXBean BEAN = bean();

    private M3CollectionCostProbe() { }

    private record Key(int id) implements Comparable<Key> {
        @Override public int compareTo(Key other) { return Integer.compare(id, other.id); }
    }

    private interface Fixture {
        long read();
        long operations();
    }

    public static void main(String[] args) {
        System.out.println("# diagnostic_only=true; time_is_not_a_correctness_gate=true");
        System.out.println("# java=" + System.getProperty("java.runtime.version")
                + "; vm=" + System.getProperty("java.vm.name")
                + "; os=" + System.getProperty("os.name")
                + "; arch=" + System.getProperty("os.arch")
                + "; processors=" + Runtime.getRuntime().availableProcessors());
        System.out.println("# allocation=thread_allocated_bytes; excludes_generic_key_value_payloads=true"
                + "; retained_heap_and_native_rss_not_measured=true");
        System.out.println("# samples=" + SAMPLES + "; warmups=3; reported_statistic=median"
                + "; cold_owner_insertion_included=true; gc_not_forced=true");
        System.out.println("family,implementation,size,cold_ns,cold_allocated_bytes,warm_ns,warm_allocated_bytes,warm_operations,checksum");
        for (int size : new int[] {1024, 16384}) {
            Key[] keys = new Key[size + size / 4];
            for (int i = 0; i < keys.length; i++) keys[i] = new Key(i);
            measure("ordered-map", "JDK_LinkedHashMap", size,
                    () -> map(new LinkedHashMap<>(), keys, size));
            measure("ordered-map", "M3LinkedHashMap", size,
                    () -> map(new M3LinkedHashMap<>(), keys, size));
            measure("sorted-map", "JDK_TreeMap", size,
                    () -> map(new TreeMap<>(), keys, size));
            measure("sorted-map", "M3TreeMap", size,
                    () -> map(new M3TreeMap<>(), keys, size));
            measure("linked-list-traversal", "JDK_LinkedList", size,
                    () -> sequence(new LinkedList<>(), keys, size));
            measure("linked-list-traversal", "M3LinkedList", size,
                    () -> sequence(new M3LinkedList<>(), keys, size));
            measure("long-list-admission", "JDK_ArrayList_boxed_long", size,
                    () -> boxedLongs(size));
            measure("long-list-admission", "M3LongArrayList", size,
                    () -> primitiveLongs(size));
        }
        System.out.println("# blackhole=" + blackhole);
    }

    private static Fixture map(Map<Key, Key> map, Key[] keys, int size) {
        for (int i = 0; i < size; i++) {
            int index = (i * 8191) & (size - 1);
            map.put(keys[index], keys[index]);
        }
        return new Fixture() {
            @Override public long read() {
                long sum = 0;
                for (int round = 0; round < 4; round++) {
                    for (Key key : keys) {
                        Key found = map.get(key);
                        sum += found == null ? -1 : found.id();
                    }
                }
                return sum;
            }
            @Override public long operations() { return keys.length * 4L; }
        };
    }

    private static Fixture sequence(List<Key> sequence, Key[] keys, int size) {
        for (int i = 0; i < size; i++) sequence.add(keys[i]);
        return new Fixture() {
            @Override public long read() {
                long sum = 0;
                for (int round = 0; round < 4; round++) {
                    for (Key key : sequence) sum += key.id();
                }
                return sum;
            }
            @Override public long operations() { return size * 4L; }
        };
    }

    private static Fixture boxedLongs(int size) {
        List<Long> list = new ArrayList<>();
        for (int i = 0; i < size; i++) list.add(1000L + i);
        return new Fixture() {
            @Override public long read() {
                long sum = 0;
                for (int round = 0; round < 4; round++) {
                    for (int i = 0; i < size; i++) sum += list.get((i * 8191) & (size - 1));
                }
                return sum;
            }
            @Override public long operations() { return size * 4L; }
        };
    }

    private static Fixture primitiveLongs(int size) {
        M3LongArrayList list = new M3LongArrayList();
        for (int i = 0; i < size; i++) list.add(1000L + i);
        return new Fixture() {
            @Override public long read() {
                long sum = 0;
                for (int round = 0; round < 4; round++) {
                    for (int i = 0; i < size; i++) sum += list.get((i * 8191) & (size - 1));
                }
                return sum;
            }
            @Override public long operations() { return size * 4L; }
        };
    }

    private static void measure(String family, String implementation, int size,
            Supplier<Fixture> prepare) {
        for (int i = 0; i < 3; i++) {
            Fixture warmup = prepare.get();
            blackhole = warmup.read();
            blackhole = warmup.read();
        }
        long[] coldTime = new long[SAMPLES], coldBytes = new long[SAMPLES];
        long[] warmTime = new long[SAMPLES], warmBytes = new long[SAMPLES];
        long checksum = 0, operations = 0;
        for (int i = 0; i < SAMPLES; i++) {
            long allocated = allocated(), start = System.nanoTime();
            Fixture fixture = prepare.get();
            coldTime[i] = System.nanoTime() - start;
            coldBytes[i] = allocated() - allocated;
            long expected = fixture.read();
            allocated = allocated();
            start = System.nanoTime();
            long observed = fixture.read();
            warmTime[i] = System.nanoTime() - start;
            warmBytes[i] = allocated() - allocated;
            if (observed != expected) throw new AssertionError("unstable operation result");
            checksum = observed;
            operations = fixture.operations();
            blackhole = observed;
        }
        System.out.println(family + "," + implementation + "," + size + ","
                + median(coldTime) + "," + median(coldBytes) + "," + median(warmTime) + ","
                + median(warmBytes) + "," + operations + "," + checksum);
    }

    private static long median(long[] values) {
        java.util.Arrays.sort(values);
        return values[values.length / 2];
    }

    private static com.sun.management.ThreadMXBean bean() {
        java.lang.management.ThreadMXBean standard = ManagementFactory.getThreadMXBean();
        if (!(standard instanceof com.sun.management.ThreadMXBean bean)
                || !bean.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("thread allocation measurement unavailable");
        }
        if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        return bean;
    }

    private static long allocated() {
        long value = BEAN.getCurrentThreadAllocatedBytes();
        if (value < 0) throw new IllegalStateException("thread allocation measurement disabled");
        return value;
    }
}
