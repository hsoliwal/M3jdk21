/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/** Independent alias, geometry and resumable full-32-bit selection oracle. */
public final class M3PathMaskAliasContract {
    private static long checks;

    private M3PathMaskAliasContract() { }

    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }

    private static void equal(int expected, int actual, String label) {
        check(expected == actual, label + " expected=" + expected + " actual=" + actual);
    }

    private static void refuses(Class<? extends Throwable> type, Runnable action, String label) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (failure.getClass() == type) return;
            throw new AssertionError(label + " wrong failure " + failure, failure);
        }
        throw new AssertionError(label + " did not refuse");
    }

    /** Permanently retained original failure: output overwrites borrowed mask parameters. */
    public static void witness() {
        int[] rows = {42, 43};
        int[] offsets = {0, 1};
        int[][] states = {{1, 1}};
        int[] required = {1};
        int[] forbidden = {0};
        refuses(IllegalArgumentException.class, () -> M3IndexPathMasks.scanWindow(
                rows, offsets, states, required, forbidden, 0, 2, 1, 0,
                required, 0, 1, M3IndexPathMasks.Backend.JAVA), "required mask alias");
        equal(1, required[0], "required mask unchanged");
    }

    private static void aliases(M3IndexPathMasks.Backend backend) {
        for (boolean aliasRequired : new boolean[] {false, true}) {
            for (int shape = 0; shape < 4; shape++) {
                int[] rows = {42, 43};
                int[] offsets = {0, 1};
                int[][] states = {{1, 1}, {-1, -1}};
                int[] required = {1, Integer.MIN_VALUE};
                int[] forbidden = {0, 0};
                int[] output = aliasRequired ? required : forbidden;
                int[] before = output.clone();
                int count = shape == 2 ? 0 : rows.length;
                int next = shape == 3 ? count : 0;
                int limit = shape == 1 ? 0 : 2;
                refuses(IllegalArgumentException.class, () -> M3IndexPathMasks.scanWindow(
                        rows, offsets, states, required, forbidden, 0, count, 1, next,
                        output, 0, limit, backend), "mask alias shape " + shape);
                check(Arrays.equals(before, output), "alias refusal precedes writes");
                check(Arrays.equals(rows, new int[] {42, 43}), "rows unchanged");
                check(Arrays.equals(offsets, new int[] {0, 1}), "offsets unchanged");
                check(Arrays.deepEquals(states, new int[][] {{1, 1}, {-1, -1}}), "states unchanged");
            }
        }
        int[] rows = {2, 3};
        int[] offsets = {0, 1};
        int[][] states = {{1, 1}};
        int[] required = {1};
        int[] forbidden = {0};
        for (int[] output : new int[][] {rows, offsets, states[0]}) {
            int[] before = output.clone();
            refuses(IllegalArgumentException.class, () -> M3IndexPathMasks.scanWindow(
                    rows, offsets, states, required, forbidden, 0, 2, 1, 0,
                    output, 0, 1, backend), "existing metadata alias");
            check(Arrays.equals(before, output), "existing alias remains untouched");
        }
        // Validation order: malformed shape and output geometry precede alias admission.
        refuses(IllegalArgumentException.class, () -> M3IndexPathMasks.scanWindow(
                rows, new int[0], states, required, forbidden, 0, 2, 1, 0,
                required, 0, 1, backend), "shape before alias");
        refuses(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.scanWindow(
                rows, offsets, states, required, forbidden, 0, 2, 1, 0,
                required, 0, 2, backend), "output bounds before alias");
    }

    /** Test each bit separately; this intentionally differs from the production word test. */
    private static boolean accepted(int[][] planes, int[] required, int[] forbidden, int slot) {
        for (int stage = 0; stage < planes.length; stage++) {
            for (int bit = 0; bit < Integer.SIZE; bit++) {
                boolean present = ((planes[stage][slot] >>> bit) & 1) != 0;
                if (((required[stage] >>> bit) & 1) != 0 && !present) return false;
                if (((forbidden[stage] >>> bit) & 1) != 0 && present) return false;
            }
        }
        return true;
    }

    private static void spectrum(M3IndexPathMasks.Backend backend) {
        int[] words = {0, 1, 2, 3, -1, Integer.MIN_VALUE, Integer.MAX_VALUE,
                0x5555_5555, 0xaaaa_aaaa, 0x8000_0001};
        for (int length : new int[] {0, 1, 2, 7, 17, 65}) {
            for (int stride : new int[] {-3, -1, 1, 3}) {
                for (int stages : new int[] {1, 2, 4}) {
                    for (int selection = 0; selection < words.length; selection++) {
                        int[] rows = new int[length];
                        int[] offsets = new int[length];
                        int[][] planes = new int[stages][length + 2];
                        int[] required = new int[stages];
                        int[] forbidden = new int[stages];
                        for (int i = 0; i < length; i++) {
                            rows[i] = switch (i % 5) {
                                case 0 -> Integer.MIN_VALUE;
                                case 1 -> Integer.MAX_VALUE;
                                case 2 -> 0;
                                default -> i % 3;
                            };
                            offsets[i] = length - i;
                        }
                        for (int stage = 0; stage < stages; stage++) {
                            required[stage] = words[(selection + stage) % words.length];
                            forbidden[stage] = words[(selection * 3 + stage * 2) % words.length];
                            for (int i = 0; i < planes[stage].length; i++) {
                                planes[stage][i] = words[(i + stage * 3) % words.length];
                            }
                        }
                        int first = stride < 0 && length != 0 ? length - 1 : 0;
                        int count = length == 0 ? 0 : (length - 1) / Math.abs(stride) + 1;
                        int[] expected = new int[count];
                        int expectedSize = 0;
                        for (int ordinal = 0; ordinal < count; ordinal++) {
                            int at = first + ordinal * stride;
                            if (accepted(planes, required, forbidden, offsets[at])) {
                                expected[expectedSize++] = rows[at];
                            }
                        }
                        expected = Arrays.copyOf(expected, expectedSize);
                        check(Arrays.equals(expected, M3IndexPathMasks.select(rows, offsets,
                                planes, required, forbidden, first, count, stride, backend)), "detached selection");
                        int[] rowsBefore = rows.clone();
                        int[] offsetsBefore = offsets.clone();
                        int[] requiredBefore = required.clone();
                        int[] forbiddenBefore = forbidden.clone();
                        for (int limit : new int[] {1, 3, 8}) {
                            int next = 0;
                            int emitted = 0;
                            do {
                                int[] output = new int[limit + 4];
                                Arrays.fill(output, 0x1234_5678);
                                long result = M3IndexPathMasks.scanWindow(rows, offsets, planes,
                                        required, forbidden, first, count, stride, next,
                                        output, 2, limit, backend);
                                int stop = M3IndexPathMasks.nextOrdinal(result);
                                int written = M3IndexPathMasks.written(result);
                                check(stop >= next && stop <= count, "next ordinal bounds");
                                check(count == 0 || stop > next, "positive-limit progress");
                                check(written >= 0 && written <= limit, "written bounds");
                                for (int j = 0; j < written; j++) {
                                    check(emitted < expected.length, "no excess row");
                                    equal(expected[emitted++], output[2 + j], "stable path occurrence");
                                }
                                for (int j = 0; j < output.length; j++) {
                                    if (j < 2 || j >= 2 + written) {
                                        equal(0x1234_5678, output[j], "buffer outside written range");
                                    }
                                }
                                next = stop;
                            } while (next < count);
                            equal(expected.length, emitted, "all selected rows emitted");
                        }
                        check(Arrays.equals(rowsBefore, rows), "borrowed rows unchanged");
                        check(Arrays.equals(offsetsBefore, offsets), "borrowed offsets unchanged");
                        check(Arrays.equals(requiredBefore, required), "borrowed required unchanged");
                        check(Arrays.equals(forbiddenBefore, forbidden), "borrowed forbidden unchanged");
                    }
                }
            }
        }
    }

    public static void run(M3IndexPathMasks.Backend backend) {
        Objects.requireNonNull(backend, "backend");
        aliases(backend);
        spectrum(backend);
    }

    public static void main(String[] args) {
        String mode = args.length == 0 ? "java" : args[0];
        switch (mode) {
            case "witness" -> witness();
            case "java" -> run(M3IndexPathMasks.Backend.JAVA);
            case "native" -> {
                check(M3IndexPathMasks.nativeAvailable(), "actual native backend required");
                run(M3IndexPathMasks.Backend.NATIVE_REQUIRED);
            }
            case "missing" -> {
                check(!M3IndexPathMasks.nativeAvailable(), "library must be absent in this JVM");
                aliases(M3IndexPathMasks.Backend.NATIVE_REQUIRED);
                refuses(UnsatisfiedLinkError.class, () -> M3IndexPathMasks.scanWindow(
                        new int[] {1}, new int[] {0}, new int[][] {{1}}, new int[] {1},
                        new int[] {0}, 0, 1, 1, 0, new int[1], 0, 1,
                        M3IndexPathMasks.Backend.NATIVE_REQUIRED), "explicit native unavailable");
            }
            default -> throw new IllegalArgumentException("java|native|missing|witness required");
        }
        System.out.println("PASS M3_PATH_MASK_ALIAS mode=" + mode + " checks=" + checks);
    }
}
