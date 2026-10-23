/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Independent finite boundary oracle; native execution is required unless explicitly disabled. */
public final class M3IndexPathNativeBatchContract {
    private static long checks;
    private static Method nativeSelector;

    private M3IndexPathNativeBatchContract() { }

    private record Input(int[] rows, int[] offsets, int[][] planes,
                         int[] needs, int[] denies, int first, int count, int stride) { }

    public static void main(String[] args) throws Throwable {
        if (args.length > 1 || (args.length == 1 && !args[0].equals("--java-only")
                && !args[0].equals("--native"))) {
            throw new IllegalArgumentException("use --native or --java-only");
        }
        boolean nativeRun = args.length == 0 || !args[0].equals("--java-only");
        if (nativeRun && !M3IndexPathMasks.nativeAvailable()) {
            throw new AssertionError("required path-mask JNI library did not load");
        }
        int[] counts = {0, 1, 7, 8, 9, 63, 64, 65, 127, 128, 129,
                255, 256, 257, 511, 512, 513, 1025};
        for (int count : counts) {
            for (int stages : new int[] {1, 2, 8, 9}) {
                for (int stride : new int[] {1, -1, 2, -3}) {
                    for (int mode = 0; mode < 5; mode++) {
                        selection(input(count, stages, stride, mode), nativeRun);
                    }
                }
            }
        }
        for (int stages : new int[] {1, 8, 9}) {
            for (int mode : new int[] {0, 4}) {
                Input input = input(257, stages, -2, mode);
                for (int limit : new int[] {1, 128, 129}) {
                    for (int budget : new int[] {7, Integer.MAX_VALUE}) {
                        windows(input, limit, budget, M3IndexPathMasks.Backend.JAVA);
                        if (nativeRun) {
                            windows(input, limit, budget,
                                    M3IndexPathMasks.Backend.NATIVE_REQUIRED);
                        }
                    }
                }
            }
        }
        boundaries(M3IndexPathMasks.Backend.JAVA);
        if (nativeRun) {
            boundaries(M3IndexPathMasks.Backend.NATIVE_REQUIRED);
            nativeSelector = M3IndexPathMasks.class.getDeclaredMethod("selectNative",
                    int[].class, int[].class, int[][].class, int[].class, int[].class,
                    int.class, int.class, int.class);
            nativeSelector.setAccessible(true);
            nativeBoundaries();
        }
        System.out.println("M3_PATH_NATIVE_BATCH_PASS checks=" + checks + " JNI=" + nativeRun);
    }

    private static Input input(int count, int stages, int stride, int mode) {
        int size = count == 0 ? 4 : (count - 1) * Math.abs(stride) + 5;
        int first = count == 0 ? size : stride > 0 ? 2 : size - 3;
        int capacity = Math.max(4, count + 7);
        int[] rows = new int[size];
        int[] offsets = new int[size];
        for (int i = 0; i < size; i++) {
            rows[i] = switch (i % 6) {
                case 0 -> Integer.MIN_VALUE;
                case 1 -> Integer.MAX_VALUE;
                case 2 -> -7;
                case 3 -> 0;
                default -> i % 11 - 5;
            };
            offsets[i] = (i * 17 + 3) % capacity;
        }
        int[][] planes = new int[stages][];
        int[] needs = new int[stages];
        int[] denies = new int[stages];
        for (int stage = 0; stage < stages; stage++) {
            planes[stage] = new int[capacity + stage % 3];
            for (int slot = 0; slot < planes[stage].length; slot++) {
                planes[stage][slot] = Integer.rotateLeft(slot * 0x45d9f3b, stage);
            }
        }
        if (mode == 1) {
            needs[0] = denies[0] = Integer.MIN_VALUE;
        } else if (mode == 2) {
            needs[stages - 1] = Integer.MIN_VALUE;
            denies[stages - 1] = 0x40;
            for (int slot = 0; slot < planes[stages - 1].length; slot++) {
                planes[stages - 1][slot] = (slot & 1) == 0 ? Integer.MIN_VALUE : -1;
            }
        } else if (mode == 3) {
            for (int stage = 0; stage < stages; stage++) {
                needs[stage] = 1 << (stage % 31);
                denies[stage] = Integer.MIN_VALUE;
                for (int slot = 0; slot < planes[stage].length; slot++) {
                    planes[stage][slot] = slot % (stage + 2) == 0
                            ? needs[stage] : Integer.MIN_VALUE;
                }
            }
        } else if (mode == 4) {
            needs[stages - 1] = -1;
            for (int slot = 0; slot < planes[stages - 1].length; slot++) {
                planes[stages - 1][slot] = slot % 127 == 0 || slot % 128 == 1 ? -1 : 0;
            }
        }
        return new Input(rows, offsets, planes, needs, denies, first, count, stride);
    }

    private static void selection(Input input, boolean nativeRun) {
        int[] rows = input.rows.clone();
        int[] offsets = input.offsets.clone();
        int[] needs = input.needs.clone();
        int[] denies = input.denies.clone();
        int[][] planes = Arrays.stream(input.planes).map(int[]::clone).toArray(int[][]::new);
        int[] expected = oracle(input);
        int[] javaRows = select(input, M3IndexPathMasks.Backend.JAVA);
        equal(expected, javaRows);
        if (nativeRun) {
            int[] nativeRows = select(input, M3IndexPathMasks.Backend.NATIVE_REQUIRED);
            equal(expected, nativeRows);
            require(nativeRows != input.rows && nativeRows != input.offsets,
                    "native result must be detached");
            if (nativeRows.length > 0) nativeRows[0] ^= 0x13579bdf;
        }
        if (javaRows.length > 0) javaRows[0] ^= 0x2468ace0;
        equal(rows, input.rows);
        equal(offsets, input.offsets);
        equal(needs, input.needs);
        equal(denies, input.denies);
        require(Arrays.deepEquals(planes, input.planes), "state arrays changed");
    }

    private static int[] select(Input input, M3IndexPathMasks.Backend backend) {
        return M3IndexPathMasks.select(input.rows, input.offsets, input.planes,
                input.needs, input.denies, input.first, input.count, input.stride, backend);
    }

    private static int[] oracle(Input input) {
        List<Integer> expected = new ArrayList<>();
        long at = input.first;
        for (int ordinal = 0; ordinal < input.count; ordinal++, at += input.stride) {
            int slot = input.offsets[(int) at];
            boolean selected = true;
            for (int stage = 0; stage < input.planes.length; stage++) {
                int state = input.planes[stage][slot];
                if ((state & input.needs[stage]) != input.needs[stage]
                        || (state & input.denies[stage]) != 0) {
                    selected = false;
                    break;
                }
            }
            if (selected) expected.add(input.rows[(int) at]);
        }
        return expected.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void windows(Input input, int limit, int budget,
                                M3IndexPathMasks.Backend backend) {
        List<Integer> found = new ArrayList<>();
        int[] output = new int[limit + 4];
        int next = 0;
        while (next < input.count) {
            Arrays.fill(output, 0x3b17ad20);
            long result = M3IndexPathMasks.scanWindowBounded(input.rows, input.offsets,
                    input.planes, input.needs, input.denies, input.first, input.count,
                    input.stride, next, budget, output, 2, limit, backend);
            int end = M3IndexPathMasks.nextOrdinal(result);
            int written = M3IndexPathMasks.written(result);
            require(end > next && end <= input.count
                    && end - next <= budget, "bounded cursor progress");
            require(written >= 0 && written <= limit, "bounded output count");
            require(output[0] == 0x3b17ad20 && output[1] == 0x3b17ad20,
                    "output prefix changed");
            for (int i = 0; i < written; i++) found.add(output[2 + i]);
            for (int i = written + 2; i < output.length; i++) {
                require(output[i] == 0x3b17ad20, "output tail changed");
            }
            next = end;
        }
        equal(oracle(input), found.stream().mapToInt(Integer::intValue).toArray());
    }

    private static void boundaries(M3IndexPathMasks.Backend backend) throws Throwable {
        Input input = input(129, 8, 1, 0);
        failure(NullPointerException.class, "backend", () -> M3IndexPathMasks.select(
                null, null, null, null, null, 0, 0, 0, null));
        failure(IllegalArgumentException.class, "unaligned row IDs and physical offsets",
                () -> M3IndexPathMasks.select(input.rows, new int[0], input.planes,
                        input.needs, input.denies, input.first, input.count, 1, backend));
        failure(IndexOutOfBoundsException.class, "invalid path offset/count/stride",
                () -> M3IndexPathMasks.select(input.rows, input.offsets, input.planes,
                        input.needs, input.denies, input.first, input.count, 0, backend));
        failure(IndexOutOfBoundsException.class, "path leaves logical row domain",
                () -> M3IndexPathMasks.select(input.rows, input.offsets, input.planes,
                        input.needs, input.denies, 1, Integer.MAX_VALUE,
                        Integer.MAX_VALUE, backend));
        int[] bad = input.offsets.clone();
        bad[input.first + 128] = -1;
        failure(IndexOutOfBoundsException.class, "physical offset outside a state plane",
                () -> M3IndexPathMasks.select(input.rows, bad, input.planes,
                        input.needs, input.denies, input.first, input.count, 1, backend));

        int[] output = new int[12];
        Arrays.fill(output, 73);
        Input small = input(12, 8, 1, 0);
        int[] lateInvalid = small.offsets.clone();
        lateInvalid[small.first + 9] = Integer.MAX_VALUE;
        long first = M3IndexPathMasks.scanWindowBounded(small.rows, lateInvalid,
                small.planes, small.needs, small.denies, small.first, small.count, 1,
                0, 8, output, 2, 8, backend);
        require(M3IndexPathMasks.nextOrdinal(first) == 8
                && M3IndexPathMasks.written(first) == 8, "first bounded segment");
        int[] before = output.clone();
        failure(IndexOutOfBoundsException.class, "physical offset outside a state plane",
                () -> M3IndexPathMasks.scanWindowBounded(small.rows, lateInvalid,
                        small.planes, small.needs, small.denies, small.first, small.count,
                        1, 8, 4, output, 2, 8, backend));
        equal(before, output);
        long zero = M3IndexPathMasks.scanWindowBounded(small.rows, lateInvalid,
                small.planes, small.needs, small.denies, small.first, small.count,
                1, 8, 0, output, 2, 8, backend);
        require(M3IndexPathMasks.nextOrdinal(zero) == 8
                && M3IndexPathMasks.written(zero) == 0, "zero budget");
        equal(before, output);
        failure(IllegalArgumentException.class, "source/output metadata alias",
                () -> M3IndexPathMasks.scanWindow(small.rows, small.offsets,
                        small.planes, small.needs, small.denies, small.first, small.count,
                        1, 0, small.needs, 0, 0, backend));
    }

    private static void nativeBoundaries() throws Throwable {
        Input input = input(129, 8, -1, 0);
        equal(oracle(input), raw(input.rows, input.offsets, input.planes,
                input.needs, input.denies, input.first, input.count, input.stride));
        failure(NullPointerException.class, "null native mask path argument",
                () -> raw(null, input.offsets, input.planes, input.needs, input.denies,
                        input.first, input.count, input.stride));
        failure(IllegalArgumentException.class, "unaligned native mask planes",
                () -> raw(input.rows, new int[0], input.planes, input.needs, input.denies,
                        input.first, input.count, input.stride));
        failure(IndexOutOfBoundsException.class, "invalid native mask path",
                () -> raw(input.rows, input.offsets, input.planes, input.needs, input.denies,
                        input.first, input.count, 0));
        failure(IndexOutOfBoundsException.class, "native path out of bounds",
                () -> raw(input.rows, input.offsets, input.planes, input.needs, input.denies,
                        1, Integer.MAX_VALUE, Integer.MAX_VALUE));
        int[][] nullPlane = input.planes.clone();
        nullPlane[7] = null;
        failure(NullPointerException.class, "null state plane",
                () -> raw(input.rows, input.offsets, nullPlane, input.needs, input.denies,
                        input.first, input.count, input.stride));
        int[] invalid = input.offsets.clone();
        invalid[input.first] = -1;
        failure(IndexOutOfBoundsException.class, "physical offset outside state plane",
                () -> raw(input.rows, invalid, input.planes, input.needs, input.denies,
                        input.first, input.count, input.stride));
    }

    private static int[] raw(int[] rows, int[] offsets, int[][] planes, int[] needs,
                             int[] denies, int first, int count, int stride) throws Throwable {
        try {
            return (int[]) nativeSelector.invoke(null, rows, offsets, planes,
                    needs, denies, first, count, stride);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
        }
    }

    @FunctionalInterface
    private interface Operation { void run() throws Throwable; }

    private static void failure(Class<? extends Throwable> expected, String message,
                                Operation operation) throws Throwable {
        try {
            operation.run();
        } catch (Throwable actual) {
            require(actual.getClass() == expected && message.equals(actual.getMessage()),
                    "expected " + expected.getName() + ": " + message + "; got " + actual);
            return;
        }
        throw new AssertionError("expected " + expected.getName() + ": " + message);
    }

    private static void equal(int[] expected, int[] actual) {
        require(Arrays.equals(expected, actual), "ordered row mismatch");
    }

    private static void require(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail);
    }
}
