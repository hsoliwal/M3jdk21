// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Bounded Java snapshot resource observations, reusing M3LinkedListWorkloadProbe's
 * ThreadMXBean counters. Run identical bytecode against frozen baseline/candidate
 * owners in separate JVMs. This is a local sampler, not JMH or a retained-size test.
 * Construction, the stable JDK oracle and full validation are outside measurement.
 * Every measured operation publishes its result and reads three physical positions;
 * no output-array copy, predicate allocation or membership-cache build is measured.
 */
public final class M3PackedSnapshotResourceProbe {
    private static final Comparator<Row> ORDER = (left, right) -> Integer.compare(left.key(), right.key());
    private static final Decision NONE = new Decision(2, 2);
    private static final Decision HALF = new Decision(1, 0);
    private static final Decision ALL = new Decision(2, 0);
    private static volatile Object retained;
    private static volatile long checksum;

    private M3PackedSnapshotResourceProbe() { }
    private record Row(int key, int id, int state) { }
    private record Decision(int mask, int required) implements Predicate<Row> {
        @Override public boolean test(Row value) { return (value.state() & mask) == required; }
    }

    public static void main(String[] args) {
        if (args.length != 1 || !args[0].equals("suite")) {
            throw new IllegalArgumentException("usage: suite (fixed sizes, selectivities and round bounds)");
        }
        Counters counters = new Counters();
        System.out.println("operation\tsize\tselection\twarmupRounds\tmeasuredRounds\tselected"
                + "\telapsedNanos\tcpuNanos\tallocatedBytes\tchecksum");
        for (int size : new int[]{64, 1025, 65536}) {
            Input input = new Input(size);
            for (boolean sorted : new boolean[]{false, true}) {
                for (String selection : new String[]{"none", "half", "all"}) {
                    Workload workload = new Workload(input, sorted, selection);
                    workload.validate(workload.apply());
                    int warmup = Math.max(sorted ? 4 : 8, (sorted ? 500_000 : 1_000_000) / size);
                    int rounds = Math.max(sorted ? 8 : 16, (sorted ? 1_000_000 : 2_000_000) / size);
                    checksum = workload.run(warmup);
                    if (checksum != workload.expectedObservation * warmup) throw new AssertionError("warmup checksum");
                    long allocatedBefore = counters.allocated();
                    long cpuBefore = counters.cpu();
                    long wallBefore = System.nanoTime();
                    long result = workload.run(rounds);
                    long wall = System.nanoTime() - wallBefore;
                    long cpu = counters.cpu() - cpuBefore;
                    long allocated = counters.allocated() - allocatedBefore;
                    checksum = result;
                    if (result != workload.expectedObservation * rounds) throw new AssertionError("measured checksum");
                    if (cpu < 0 || allocated < 0) throw new AssertionError("counter discontinuity");
                    workload.validate(workload.last);
                    System.out.println((sorted ? "filteredSorted" : "filtered") + "\t" + size + "\t"
                            + selection + "\t" + warmup + "\t" + rounds + "\t" + workload.expected.size()
                            + "\t" + wall + "\t" + cpu + "\t" + allocated + "\t" + result);
                }
            }
        }
        System.out.println("# javaRuntimeVersion=" + System.getProperty("java.runtime.version"));
        System.out.println("# includes=operation,volatile-result-publication,three-position-observation,counter-overhead");
        System.out.println("# excludes=input-construction,oracle,full-validation,retained-graph-accounting,native-backend");
    }

    private static final class Input {
        final Row[] rows;
        final M3IndexedListSnapshot<Row> source;

        Input(int size) {
            rows = new Row[size + 14];
            for (int i = 0; i < rows.length; i++) {
                rows[i] = new Row(((i * 0x9e3779b9) >>> 16) & 1023, i, (i - 7) & 1);
            }
            source = M3IndexedListSnapshot.copyOf(rows).slice(7, size);
        }
    }

    private static final class Workload {
        final Input input;
        final boolean sorted;
        final Decision predicate;
        final List<Integer> expected = new ArrayList<>();
        final long expectedObservation;
        M3IndexedListSnapshot<Row> last;

        Workload(Input input, boolean sorted, String selection) {
            this.input = input;
            this.sorted = sorted;
            predicate = switch (selection) {
                case "none" -> NONE;
                case "half" -> HALF;
                case "all" -> ALL;
                default -> throw new AssertionError("fixed selection");
            };
            for (int i = 0; i < input.source.size(); i++) {
                if (selection.equals("all") || selection.equals("half") && (i & 1) == 0) expected.add(i + 7);
            }
            if (sorted) expected.sort((left, right) -> ORDER.compare(input.rows[left], input.rows[right]));
            long value = 0x9e3779b97f4a7c15L ^ expected.size();
            if (!expected.isEmpty()) {
                int first = expected.getFirst(), middle = expected.get(expected.size() / 2), end = expected.getLast();
                value = mix(value, first, input.rows[first]);
                value = mix(value, middle, input.rows[middle]);
                value = mix(value, end, input.rows[end]);
            }
            expectedObservation = value;
        }

        M3IndexedListSnapshot<Row> apply() {
            return sorted ? input.source.filteredSorted(predicate, ORDER, M3IndexMaskCompactor.Backend.JAVA)
                    : input.source.filtered(predicate, M3IndexMaskCompactor.Backend.JAVA);
        }

        long run(int rounds) {
            long result = 0;
            M3IndexedListSnapshot<Row> current = null;
            for (int i = 0; i < rounds; i++) {
                current = apply();
                retained = current; // Every result escapes; output arrays remain observable.
                long value = 0x9e3779b97f4a7c15L ^ current.size();
                if (!current.isEmpty()) {
                    int middle = current.size() / 2, end = current.size() - 1;
                    value = mix(value, current.physicalIndex(0), current.get(0));
                    value = mix(value, current.physicalIndex(middle), current.get(middle));
                    value = mix(value, current.physicalIndex(end), current.get(end));
                }
                result += value;
            }
            last = current;
            return result;
        }

        void validate(M3IndexedListSnapshot<Row> actual) {
            if (actual == input.source || actual.size() != expected.size()) throw new AssertionError("fresh projection/size");
            for (int i = 0; i < expected.size(); i++) {
                int position = expected.get(i);
                if (actual.physicalIndex(i) != position || actual.get(i) != input.rows[position]) {
                    throw new AssertionError("stable physical occurrence/reference identity");
                }
            }
            for (int i = 0; i < input.source.size(); i++) {
                if (input.source.physicalIndex(i) != i + 7 || input.source.get(i) != input.rows[i + 7]) {
                    throw new AssertionError("source projection was changed");
                }
            }
        }
    }

    private static long mix(long value, int position, Row row) {
        return (value ^ position) * 31 + ((long) row.key() << 32) + row.id();
    }

    private static final class Counters {
        final com.sun.management.ThreadMXBean bean;
        final long thread = Thread.currentThread().threadId();

        Counters() {
            java.lang.management.ThreadMXBean original = ManagementFactory.getThreadMXBean();
            if (!(original instanceof com.sun.management.ThreadMXBean extended)
                    || !extended.isCurrentThreadCpuTimeSupported() || !extended.isThreadAllocatedMemorySupported()) {
                throw new IllegalStateException("actual current-thread CPU and allocation counters are required");
            }
            bean = extended;
            if (!bean.isThreadCpuTimeEnabled()) bean.setThreadCpuTimeEnabled(true);
            if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
            for (int i = 0; i < 20; i++) {
                if (cpu() < 0 || allocated() < 0) throw new IllegalStateException("VM counters unavailable");
                System.nanoTime();
            }
        }

        long cpu() { return bean.getCurrentThreadCpuTime(); }
        long allocated() { return bean.getThreadAllocatedBytes(thread); }
    }
}
