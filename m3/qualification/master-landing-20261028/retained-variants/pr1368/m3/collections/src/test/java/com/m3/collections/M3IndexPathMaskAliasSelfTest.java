/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;

/** Mask-directory output aliases must fail before writes for both existing backends. */
public final class M3IndexPathMaskAliasSelfTest {
    private static int checks;
    private M3IndexPathMaskAliasSelfTest() { }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private static void alias(M3IndexPathMasks.Backend backend, boolean requiredAlias,
            int next, int limit, boolean sharedMasks) {
        int[] rows = {7, 9};
        int[] offsets = {0, 1};
        int[][] planes = {{1, 1}, {1, 1}};
        int[] required = {1, 1};
        int[] forbidden = sharedMasks ? required : new int[] {0, 0};
        int[] output = requiredAlias ? required : forbidden;
        int[] priorRequired = required.clone();
        int[] priorForbidden = forbidden.clone();
        try {
            M3IndexPathMasks.scanWindow(rows, offsets, planes, required, forbidden,
                    0, 2, 1, next, output, 0, limit, backend);
            throw new AssertionError("accepted " + (requiredAlias ? "required" : "forbidden")
                    + " mask alias backend=" + backend + " next=" + next + " limit=" + limit);
        } catch (IllegalArgumentException expected) {
            check(Arrays.equals(priorRequired, required), "required directory changed on refusal");
            check(Arrays.equals(priorForbidden, forbidden), "forbidden directory changed on refusal");
            check(Arrays.equals(rows, new int[] {7, 9}), "row input changed");
            check(Arrays.equals(offsets, new int[] {0, 1}), "offset input changed");
            check(Arrays.deepEquals(planes, new int[][] {{1, 1}, {1, 1}}), "state input changed");
        }
    }

    private static void positive(M3IndexPathMasks.Backend backend) {
        int[] required = {1, 1}, forbidden = {0, 0};
        int[] output = {31, 31, 31, 31};
        long result = M3IndexPathMasks.scanWindow(new int[] {7, 9}, new int[] {0, 1},
                new int[][] {{1, 1}, {1, 1}}, required, forbidden,
                1, 2, -1, 0, output, 1, 2, backend);
        check(M3IndexPathMasks.nextOrdinal(result) == 2, "reverse cursor");
        check(M3IndexPathMasks.written(result) == 2, "reverse count");
        check(Arrays.equals(output, new int[] {31, 9, 7, 31}), "reverse output and guards");
        check(Arrays.equals(required, new int[] {1, 1}), "required directory isolated");
        check(Arrays.equals(forbidden, new int[] {0, 0}), "forbidden directory isolated");
    }

    public static void main(String[] args) {
        boolean nativeMode = args.length > 0 && args[0].equals("--native");
        boolean missingMode = args.length > 0 && args[0].equals("--missing");
        var backend = nativeMode || missingMode ? M3IndexPathMasks.Backend.NATIVE_REQUIRED
                : M3IndexPathMasks.Backend.JAVA;
        if (nativeMode) check(M3IndexPathMasks.nativeAvailable(), "actual native library required");
        if (missingMode) check(!M3IndexPathMasks.nativeAvailable(), "native library must be absent");
        for (boolean requiredAlias : new boolean[] {true, false}) {
            for (boolean sharedMasks : new boolean[] {false, true}) {
                for (int next : new int[] {0, 1, 2}) {
                    for (int limit : new int[] {0, 1, 2}) {
                        alias(backend, requiredAlias, next, limit, sharedMasks);
                    }
                }
            }
        }
        if (!missingMode) positive(backend);
        System.out.println("PATH_MASK_ALIAS_PASS checks=" + checks + " backend=" + backend);
    }
}
