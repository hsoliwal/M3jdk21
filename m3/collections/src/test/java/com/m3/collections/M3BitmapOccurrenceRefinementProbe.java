/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
/*
 * Authored qualification template; baseline and candidate must use identical bytes.
 */
package com.m3.collections;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.function.LongPredicate;

/** Exact occurrence/callback contracts and separate, threshold-free resource observations. */
public final class M3BitmapOccurrenceRefinementProbe {
    private static final int[] SIZES = {0, 1, 7, 31, 32, 63, 64, 65, 127, 255, 256, 257, 4096, 8193};
    private static final String[] SHAPES = {"encounter", "ascending", "descending", "offset"};
    private static final String[] MASKS = {"all", "none", "alternate", "sparse"};
    private static volatile Object sourceEscape;
    private static volatile Object resultEscape;
    private static long checks;

    private record Fixture(M3LongIndexProjection projection, long[] values, int[] physical) { }
    private record Sample(M3LongBitmapFilteredSnapshot result, long bytes, long cpu, long wall) { }

    private M3BitmapOccurrenceRefinementProbe() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static int[] sorted(int[] physical, long[] values, boolean descending) {
        Integer[] boxed = Arrays.stream(physical).boxed().toArray(Integer[]::new);
        Comparator<Integer> comparator = Comparator.comparingLong(i -> values[i]);
        Arrays.sort(boxed, descending ? comparator.reversed() : comparator);
        return Arrays.stream(boxed).mapToInt(Integer::intValue).toArray();
    }

    private static Fixture fixture(int size, String shape) {
        int length = size + (shape.equals("offset") ? 6 : 0);
        long[] values = new long[length];
        int[] physical = new int[length];
        for (int i = 0; i < length; i++) {
            values[i] = (i * 37L) % 29 - 14;
            physical[i] = i;
        }
        if (length > 2) { values[1] = Long.MIN_VALUE; values[length - 2] = Long.MAX_VALUE; }
        M3LongIndexProjection projection = M3LongIndexProjection.snapshot(values);
        if (shape.equals("ascending") || shape.equals("offset")) {
            projection = projection.sortedAscending();
            physical = sorted(physical, values, false);
        } else if (shape.equals("descending")) {
            projection = projection.sortedDescending();
            physical = sorted(physical, values, true);
        }
        if (shape.equals("offset")) {
            projection = projection.slice(1, size + 4).slice(2, size).sortedDescending();
            physical = sorted(Arrays.copyOfRange(physical, 3, size + 3), values, true);
        }
        check(Arrays.equals(projection.physicalIndices(), physical), "prepared physical order");
        for (int i = 0; i < size; i++) check(projection.get(i) == values[physical[i]], "prepared value");
        return new Fixture(projection, values, physical);
    }

    private static boolean accepted(int ordinal, String mask) {
        return switch (mask) {
            case "all" -> true;
            case "none" -> false;
            case "alternate" -> (ordinal & 1) == 0;
            case "sparse" -> ordinal % 67 == 0;
            default -> throw new IllegalArgumentException(mask);
        };
    }

    private static int[] expected(int[] physical, String mask) {
        int[] admitted = new int[physical.length];
        int count = 0;
        for (int i = 0; i < physical.length; i++) if (accepted(i, mask)) admitted[count++] = physical[i];
        return Arrays.copyOf(admitted, count);
    }

    private static void verify(M3LongBitmapFilteredSnapshot actual, long[] values, int[] physical) {
        check(actual.size() == physical.length, "selected size");
        long[] expected = new long[physical.length];
        BitSet mask = new BitSet();
        for (int i = 0; i < physical.length; i++) {
            expected[i] = values[physical[i]];
            mask.set(physical[i]);
            check(actual.physicalIndex(i) == physical[i], "selected physical occurrence");
            check(actual.get(i) == expected[i], "selected value");
        }
        check(Arrays.equals(actual.toArray(), expected), "detached values");
        check(actual.physicalMaskCopy().equals(mask), "physical membership bitmap");
        for (int i = -1; i <= values.length; i++) {
            check(actual.includesPhysicalIndex(i) == (i >= 0 && mask.get(i)), "physical membership");
        }
        check(!actual.contains(123456789L), "absent value");
        if (physical.length != 0) {
            check(actual.contains(expected[0]), "present value");
            long[] detached = actual.toArray();
            detached[0] ^= 1;
            BitSet detachedMask = actual.physicalMaskCopy();
            detachedMask.clear();
            check(actual.get(0) == expected[0] && actual.includesPhysicalIndex(physical[0]), "detachment");
        }
    }

    private static void contractCase(Fixture fixture, String shape, String mask,
                                     M3IndexMaskCompactor.Backend backend) {
        int size = fixture.physical.length;
        long[] tape = new long[size];
        int[] calls = {0};
        M3LongBitmapFilteredSnapshot actual = M3LongBitmapFilteredSnapshot.fromProjection(
                fixture.projection, value -> {
                    int ordinal = calls[0]++;
                    check(ordinal < size, "extra predicate call");
                    check(value == fixture.values[fixture.physical[ordinal]], "predicate encounter order");
                    tape[ordinal] = value;
                    return accepted(ordinal, mask);
                }, backend);
        check(calls[0] == size, "predicate exactly once per occurrence");
        int[] physical = expected(fixture.physical, mask);
        verify(actual, fixture.values, physical);
        verify(actual.sortedAscending(), fixture.values, sorted(physical, fixture.values, false));
        verify(actual.sortedDescending(), fixture.values, sorted(physical, fixture.values, true));
        int start = physical.length / 3;
        verify(actual.slice(start, physical.length - start), fixture.values,
                Arrays.copyOfRange(physical, start, physical.length));
        check(calls[0] == size, "sorting/slicing/membership must not repeat predicate");
        int[] furtherCalls = {0};
        M3LongBitmapFilteredSnapshot further = actual.filtered(value -> {
            int ordinal = furtherCalls[0]++;
            check(ordinal < physical.length && value == fixture.values[physical[ordinal]], "refinement order");
            return accepted(ordinal, "alternate");
        }, backend);
        check(furtherCalls[0] == physical.length, "refinement callback count");
        verify(further, fixture.values, expected(physical, "alternate"));
        // Full arrays are checked above; deterministic hashes keep the comparison transcript bounded.
        System.out.println("contract," + backend + ',' + size + ',' + shape + ',' + mask
                + ",calls=" + calls[0] + ",tape=" + Arrays.hashCode(tape)
                + ",physical=" + Arrays.hashCode(physical) + ",selected=" + physical.length);
    }

    private static void throwingCase(Fixture fixture, M3IndexMaskCompactor.Backend backend) {
        if (fixture.physical.length == 0) return;
        int stop = Math.min(64, fixture.physical.length - 1);
        int[] calls = {0};
        RuntimeException sentinel = new IllegalStateException("predicate-sentinel");
        try {
            M3LongBitmapFilteredSnapshot.fromProjection(fixture.projection, value -> {
                int ordinal = calls[0]++;
                check(value == fixture.values[fixture.physical[ordinal]], "throwing predicate order");
                if (ordinal == stop) throw sentinel;
                return (ordinal & 1) == 0;
            }, backend);
            throw new AssertionError("missing predicate failure");
        } catch (RuntimeException actual) {
            check(actual == sentinel, "predicate exception identity");
            check(calls[0] == stop + 1, "no callbacks after failure");
        }
    }

    private static void contracts(M3IndexMaskCompactor.Backend backend) {
        for (int size : SIZES) for (String shape : SHAPES) {
            Fixture fixture = fixture(size, shape);
            for (String mask : MASKS) contractCase(fixture, shape, mask, backend);
            throwingCase(fixture, backend);
        }
        System.out.println("PASS M3BitmapOccurrenceRefinementProbe backend=" + backend + " checks=" + checks);
    }

    private static LongPredicate resourcePredicate(String mask) {
        return switch (mask) {
            case "all" -> value -> true;
            case "none" -> value -> false;
            case "alternate" -> value -> (value & 1) == 0;
            case "sparse" -> value -> value == 3;
            default -> throw new IllegalArgumentException(mask);
        };
    }

    private static long allocated(ThreadMXBean bean, long thread) {
        return bean.isThreadAllocatedMemorySupported() && bean.isThreadAllocatedMemoryEnabled()
                ? bean.getThreadAllocatedBytes(thread) : -1;
    }

    private static long cpu(ThreadMXBean bean) {
        return bean.isCurrentThreadCpuTimeSupported() && bean.isThreadCpuTimeEnabled()
                ? bean.getCurrentThreadCpuTime() : -1;
    }

    private static Sample sample(Fixture fixture, LongPredicate predicate,
                                 M3IndexMaskCompactor.Backend backend, ThreadMXBean bean, int repetitions) {
        long thread = Thread.currentThread().threadId();
        long bytesBefore = allocated(bean, thread), cpuBefore = cpu(bean), wallBefore = System.nanoTime();
        M3LongBitmapFilteredSnapshot result = null;
        for (int i = 0; i < repetitions; i++) {
            result = M3LongBitmapFilteredSnapshot.fromProjection(fixture.projection, predicate, backend);
            resultEscape = result;
        }
        long wall = System.nanoTime() - wallBefore, cpuAfter = cpu(bean), bytesAfter = allocated(bean, thread);
        return new Sample(result, bytesBefore < 0 ? -1 : bytesAfter - bytesBefore,
                cpuBefore < 0 ? -1 : cpuAfter - cpuBefore, wall);
    }

    private static void printSample(String mask, String phase, int size, int operations,
                                    M3IndexMaskCompactor.Backend backend, Sample sample) {
        System.out.println(backend + "," + size + ',' + mask + ',' + phase + ',' + operations
                + ',' + sample.bytes + ',' + sample.cpu + ',' + sample.wall);
    }

    private static void resources(M3IndexMaskCompactor.Backend backend, int repetitions) {
        if (!(ManagementFactory.getThreadMXBean() instanceof ThreadMXBean bean)) {
            throw new UnsupportedOperationException("ThreadMXBean allocation counters unavailable");
        }
        if (bean.isThreadAllocatedMemorySupported()) bean.setThreadAllocatedMemoryEnabled(true);
        if (bean.isCurrentThreadCpuTimeSupported()) bean.setThreadCpuTimeEnabled(true);
        System.out.println("# cold_first_case is the first filter call for this fixture; earlier cases may warm the VM.");
        System.out.println("# projection creation, expected values, contains and physical-mask checks are outside measurement.");
        System.out.println("# counter -1 means unavailable; raw samples have no acceptance threshold.");
        System.out.println("backend,size,mask,phase,operations,allocated_bytes,thread_cpu_ns,elapsed_ns");
        for (int size : SIZES) for (String mask : MASKS) {
            Fixture fixture = fixture(size, "offset");
            sourceEscape = fixture.projection;
            LongPredicate predicate = resourcePredicate(mask);
            int[] admitted = Arrays.stream(fixture.physical).filter(i -> predicate.test(fixture.values[i])).toArray();
            Sample first = sample(fixture, predicate, backend, bean, 1);
            verify(first.result, fixture.values, admitted);
            printSample(mask, "cold_first_case", size, 1, backend, first);
            for (int i = 0; i < 64; i++) {
                resultEscape = M3LongBitmapFilteredSnapshot.fromProjection(fixture.projection, predicate, backend);
            }
            for (int round = 0; round < 3; round++) {
                Sample measured = sample(fixture, predicate, backend, bean, repetitions);
                verify(measured.result, fixture.values, admitted);
                printSample(mask, "warm_" + round, size, repetitions, backend, measured);
            }
            check(Arrays.equals(fixture.projection.physicalIndices(), fixture.physical), "source remains immutable");
        }
    }

    public static void main(String[] args) {
        String mode = args.length == 0 ? "contract" : args[0];
        String requested = args.length < 2 ? "java" : args[1];
        M3IndexMaskCompactor.Backend backend = switch (requested) {
            case "java" -> M3IndexMaskCompactor.Backend.JAVA;
            case "native" -> M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
            default -> throw new IllegalArgumentException("backend must be java or native");
        };
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED && !M3IndexMaskCompactor.nativeAvailable()) {
            throw new AssertionError("explicit native lane requires the real compactor JNI library");
        }
        if (mode.equals("contract")) contracts(backend);
        else if (mode.equals("resource")) {
            int repetitions = args.length < 3 ? 32 : Integer.parseInt(args[2]);
            if (repetitions < 1 || repetitions > 10000) throw new IllegalArgumentException("repetitions 1..10000");
            resources(backend, repetitions);
        } else throw new IllegalArgumentException("mode must be contract or resource");
    }
}
