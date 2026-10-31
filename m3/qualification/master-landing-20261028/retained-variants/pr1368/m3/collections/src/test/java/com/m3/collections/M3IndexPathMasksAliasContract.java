/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;

/** Regressions for source/output mask-vector aliases across the Java and actual JNI paths. */
public final class M3IndexPathMasksAliasContract {
    private static int checks;
    private static boolean nativeRun;

    private M3IndexPathMasksAliasContract() { }

    private record Inputs(int[] rows, int[] offsets, int[][] planes,
                          int[] required, int[] forbidden) { }

    private static Inputs inputs(int masks) {
        int[] required = masks == 0 ? new int[] {1, 0} : new int[] {0, 0};
        int[] forbidden = new int[] {0, 0};
        if (masks == 2) { required[0] = 1; forbidden[0] = 1; }
        return new Inputs(new int[] {3, 17, 3, Integer.MIN_VALUE, Integer.MAX_VALUE, -9},
                new int[] {0, 1, 2, 3, 4, 5},
                new int[][] {{1, 1, 1, 1, 1, 1}, {0, 0, 0, 0, 0, 0}},
                required, forbidden);
    }

    private static int[] alias(Inputs input, int category) {
        return switch (category) {
            case 0 -> input.rows();
            case 1 -> input.offsets();
            case 2 -> input.planes()[0];
            case 3 -> input.planes()[1];
            case 4 -> input.required();
            case 5 -> input.forbidden();
            default -> throw new AssertionError("unknown alias category");
        };
    }

    private static Inputs copy(Inputs input) {
        int[][] planes = new int[input.planes().length][];
        for (int i = 0; i < planes.length; i++) {
            planes[i] = input.planes()[i] == null ? null : input.planes()[i].clone();
        }
        return new Inputs(input.rows().clone(), input.offsets().clone(), planes,
                input.required().clone(), input.forbidden().clone());
    }

    private static void same(int[] expected, int[] actual, String what) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(what + ": " + Arrays.toString(expected)
                    + " != " + Arrays.toString(actual));
        }
    }

    private static void unchanged(Inputs before, Inputs input) {
        same(before.rows(), input.rows(), "rows changed");
        same(before.offsets(), input.offsets(), "offsets changed");
        for (int i = 0; i < before.planes().length; i++) {
            same(before.planes()[i], input.planes()[i], "state plane changed");
        }
        same(before.required(), input.required(), "required masks changed");
        same(before.forbidden(), input.forbidden(), "forbidden masks changed");
    }

    private static long scan(Inputs input, int first, int count, int stride, int next,
            int[] output, int outputOffset, int limit, M3IndexPathMasks.Backend backend) {
        return M3IndexPathMasks.scanWindow(input.rows(), input.offsets(), input.planes(),
                input.required(), input.forbidden(), first, count, stride, next,
                output, outputOffset, limit, backend);
    }

    private static void refuses(Inputs input, int[] output, int first, int count,
            int stride, int next, int offset, int limit, M3IndexPathMasks.Backend backend,
            Class<? extends RuntimeException> expected, String message) {
        Inputs before = copy(input);
        int[] outputBefore = output.clone();
        checks++;
        try {
            scan(input, first, count, stride, next, output, offset, limit, backend);
            throw new AssertionError("did not reject " + expected.getSimpleName() + " " + backend);
        } catch (RuntimeException failure) {
            if (!expected.isInstance(failure)) {
                throw new AssertionError("wrong refusal " + backend, failure);
            }
            if (message != null && !failure.getMessage().contains(message)) {
                throw new AssertionError("validation order changed: " + failure, failure);
            }
        }
        unchanged(before, input);
        same(outputBefore, output, "output changed before refusal");
    }

    private static void aliasesBeforeAnyWrite() {
        for (M3IndexPathMasks.Backend backend : M3IndexPathMasks.Backend.values()) {
            for (int masks = 0; masks < 3; masks++) {
                for (int category = 0; category < 6; category++) {
                    for (int variant = 0; variant < 7; variant++) {
                        Inputs input = inputs(masks);
                        int[] output = alias(input, category);
                        int first = 0, count = 6, stride = 1, next = 0, offset = 0, limit = 2;
                        switch (variant) {
                            case 1 -> { first = 5; stride = -1; }
                            case 2 -> { next = 3; offset = 1; limit = 1; }
                            case 3 -> { first = 6; count = 0; limit = 0; }
                            case 4 -> { first = 6; count = 0; }
                            case 5 -> { offset = output.length; limit = 0; }
                            case 6 -> next = count;
                            default -> { }
                        }
                        refuses(input, output, first, count, stride, next, offset, limit,
                                backend, IllegalArgumentException.class, "alias");
                    }
                }
            }
        }
    }

    private static void sharedMasksAndEmptyInputs() {
        for (M3IndexPathMasks.Backend backend : M3IndexPathMasks.Backend.values()) {
            for (int limit : new int[] {0, 1, 2}) {
                Inputs normal = inputs(0);
                int[] masks = {1, 0};
                Inputs shared = new Inputs(normal.rows(), normal.offsets(), normal.planes(), masks, masks);
                refuses(shared, masks, 0, 6, 1, 0, 0, limit, backend,
                        IllegalArgumentException.class, "alias");
            }
            for (int category = 0; category < 6; category++) {
                Inputs empty = new Inputs(new int[0], new int[0],
                        new int[][] {new int[0], new int[0]}, new int[] {1, 0}, new int[2]);
                refuses(empty, alias(empty, category), 0, 0, 1, 0, 0, 0,
                        backend, IllegalArgumentException.class, "alias");
            }
            // Zero stages is invalid independently of aliases and empty path/limit.
            int[] masks = new int[0];
            Inputs noStages = new Inputs(new int[0], new int[0], new int[0][], masks, masks);
            refuses(noStages, masks, 0, 0, 1, 0, 0, 0, backend,
                    IllegalArgumentException.class, "aligned mask stages");
        }
    }

    private static void validationOrder() {
        for (M3IndexPathMasks.Backend backend : M3IndexPathMasks.Backend.values()) {
            Inputs input = inputs(0);
            refuses(input, input.required(), 0, 6, 1, -1, 0, 1, backend,
                    IndexOutOfBoundsException.class, "path ordinal");
            refuses(input, input.required(), 0, 6, 1, 0, 0, -1, backend,
                    IllegalArgumentException.class, "bounded window");
            refuses(input, input.forbidden(), 0, 6, 1, 0, 0, M3IndexPathMasks.MAX_WINDOW + 1,
                    backend, IllegalArgumentException.class, "bounded window");
            refuses(input, input.required(), 0, 6, 1, 0, 3, 0, backend,
                    IndexOutOfBoundsException.class, null);
            refuses(input, input.required(), 0, 6, 0, 0, 0, 1, backend,
                    IndexOutOfBoundsException.class, "offset/count/stride");
            Inputs nullPlane = inputs(0);
            nullPlane.planes()[1] = null;
            refuses(nullPlane, nullPlane.required(), 0, 6, 1, 0, 0, 1,
                    backend, NullPointerException.class, "statePlanes");
        }
    }

    private static void result(long packed, int next, int written) {
        checks++;
        if (M3IndexPathMasks.nextOrdinal(packed) != next || M3IndexPathMasks.written(packed) != written) {
            throw new AssertionError("wrong cursor or selected count");
        }
    }

    private static void validSeparateOutput(M3IndexPathMasks.Backend backend) {
        Inputs input = new Inputs(new int[] {9, 9, 9, 7, -1, Integer.MIN_VALUE},
                new int[] {5, 4, 3, 2, 1, 0},
                new int[][] {{1, 0, 3, 1, 1, 1}, {0, 0, 0, 2, 0, 0}},
                new int[] {1, 0}, new int[] {0, 2});
        Inputs before = copy(input);
        int[] output = {101, 101, 101, 101};
        result(scan(input, 0, 6, 1, 0, output, 1, 2, backend), 2, 2);
        same(new int[] {101, 9, 9, 101}, output, "forward duplicate rows");
        result(scan(input, 0, 6, 1, 2, output, 1, 2, backend), 6, 2);
        same(new int[] {101, 7, Integer.MIN_VALUE, 101}, output, "forward resume");
        result(scan(input, 5, 6, -1, 0, output, 1, 2, backend), 3, 2);
        same(new int[] {101, Integer.MIN_VALUE, 7, 101}, output, "reverse order");
        result(scan(input, 5, 6, -1, 3, output, 1, 2, backend), 6, 2);
        same(new int[] {101, 9, 9, 101}, output, "reverse resume");
        result(scan(input, 0, 6, 1, 3, output, output.length, 0, backend), 3, 0);
        result(scan(input, 6, 0, 1, 0, output, 1, 2, backend), 0, 0);
        same(new int[] {101, 9, 9, 101}, output, "empty windows changed output");
        unchanged(before, input);

        int[] sharedMasks = {1, 0};
        Inputs shared = new Inputs(input.rows(), input.offsets(), input.planes(), sharedMasks, sharedMasks);
        Inputs sharedBefore = copy(shared);
        result(scan(shared, 0, 6, 1, 0, output, 1, 2, backend), 6, 0);
        same(new int[] {101, 9, 9, 101}, output, "overlapping masks changed output");
        unchanged(sharedBefore, shared);

        int[] commonMetadata = {0, 1};
        Inputs inputAliases = new Inputs(new int[] {7, 8}, commonMetadata,
                new int[][] {commonMetadata}, new int[] {0}, new int[] {0});
        Inputs inputAliasesBefore = copy(inputAliases);
        result(scan(inputAliases, 0, 2, 1, 0, output, 1, 2, backend), 2, 2);
        same(new int[] {101, 7, 8, 101}, output, "safe input/input alias");
        unchanged(inputAliasesBefore, inputAliases);
    }

    public static void main(String[] args) {
        nativeRun = args.length != 0 && args[0].equals("--native");
        if (nativeRun != M3IndexPathMasks.nativeAvailable()) {
            throw new AssertionError("requested native availability does not match actual library");
        }
        aliasesBeforeAnyWrite();
        sharedMasksAndEmptyInputs();
        validationOrder();
        validSeparateOutput(M3IndexPathMasks.Backend.JAVA);
        if (nativeRun) validSeparateOutput(M3IndexPathMasks.Backend.NATIVE_REQUIRED);
        else {
            Inputs input = inputs(0);
            int[] output = {101, 101};
            checks++;
            try {
                scan(input, 0, 6, 1, 0, output, 0, 2, M3IndexPathMasks.Backend.NATIVE_REQUIRED);
                throw new AssertionError("missing native library was not refused");
            } catch (UnsatisfiedLinkError expected) {
                same(new int[] {101, 101}, output, "missing native wrote output");
            }
        }
        System.out.println("M3_PATH_MASK_ALIAS_PASS checks=" + checks + " actualJNI=" + nativeRun);
    }
}
