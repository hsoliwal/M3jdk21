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
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.IntBinaryOperator;

/** Frozen independent value, callback, alias and exception oracles for all four operations. */
public final class M3ArrayifiedIndexContract {
    private long checks;
    private long digest = 0x714fcbe38a19d26bL;
    private final M3IndexMaskCompactor.Backend backend;

    private M3ArrayifiedIndexContract(boolean nativeMode) {
        backend = nativeMode ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED
                : M3IndexMaskCompactor.Backend.JAVA;
        if (nativeMode && !M3IndexMaskCompactor.nativeAvailable()) {
            throw new AssertionError("native proof requires actual selector binary");
        }
    }

    private void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private void equal(int[] actual, int[] expected) {
        check(Arrays.equals(actual, expected), "value or encounter-order difference");
        digest = 31 * digest + actual.length;
        for (int value : actual) digest = 31 * digest + value;
    }

    private void expect(Class<? extends Throwable> type, String message, Runnable task) {
        Throwable observed = null;
        try { task.run(); } catch (RuntimeException | Error failure) { observed = failure; }
        check(observed != null && type.isInstance(observed), "wrong exception: " + observed);
        if (message != null) check(message.equals(observed.getMessage()), "exception message drift");
        digest = 31 * digest + observed.getClass().getName().hashCode();
    }

    private static int[] selectOracle(int[] rows, int from, int to, byte[] states,
            int stateFrom, int required, int excluded) {
        List<Integer> kept = new ArrayList<>();
        for (int i = 0; i < to - from; i++) {
            int value = Byte.toUnsignedInt(states[stateFrom + i]);
            if ((value | required) == value && (value & excluded) == 0) kept.add(rows[from + i]);
        }
        return kept.stream().mapToInt(Integer::intValue).toArray();
    }

    private void allMaskPairs() {
        int[] rows = new int[270];
        byte[] states = new byte[280];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = i % 9 == 0 ? Integer.MIN_VALUE : i % 11 == 0 ? Integer.MAX_VALUE : i % 13 - 6;
        }
        for (int i = 0; i < 256; i++) states[7 + i] = (byte) i;
        int[] savedRows = rows.clone();
        byte[] savedStates = states.clone();
        for (int required = 0; required <= 255; required++) {
            for (int excluded = 0; excluded <= 255; excluded++) {
                int[] actual = M3ArrayifiedIndexOps.selected(rows, 3, 259, states, 7,
                        required, excluded, backend);
                equal(actual, selectOracle(rows, 3, 259, states, 7, required, excluded));
                check(actual != rows, "selected result aliases source");
                if (actual.length != 0) actual[0] ^= -1;
                check(Arrays.equals(rows, savedRows), "selected wrote source");
                check(Arrays.equals(states, savedStates), "selected wrote states");
            }
        }
    }

    private void rotationAndGatherWindows() {
        int[] distances = {Integer.MIN_VALUE, -257, -129, -65, -2, -1, 0, 1, 2,
                63, 64, 65, 129, 257, Integer.MAX_VALUE};
        for (int length = 0; length <= 132; length++) {
            int[] rows = new int[length + 8];
            for (int i = 0; i < rows.length; i++) rows[i] = (i & 1) == 0 ? i : -i;
            int[] saved = rows.clone();
            for (int from : new int[]{0, 3, 8}) {
                for (int distance : distances) {
                    int[] expected = new int[length];
                    if (length != 0) {
                        for (int out = 0; out < length; out++) {
                            int input = (int) Math.floorMod((long) out - distance, (long) length);
                            expected[out] = rows[from + input];
                        }
                    }
                    int[] actual = M3ArrayifiedIndexOps.rotated(rows, from, from + length, distance);
                    equal(actual, expected);
                    check(actual != rows, "rotation result aliases source");
                    check(Arrays.equals(rows, saved), "rotation wrote source");
                    int[] ordinals = new int[length * 2];
                    int[] gathered = new int[ordinals.length];
                    for (int i = 0; i < ordinals.length; i++) {
                        ordinals[i] = i % length;
                        gathered[i] = expected[ordinals[i]];
                    }
                    equal(M3ArrayifiedIndexOps.gather(actual, 0, length, ordinals), gathered);
                }
            }
        }
    }

    /** Original scalar merge schedule is the frozen callback oracle, not an optimized copy. */
    private static int[] mergeOracle(int[] left, int a, int b, int[] right, int c, int d,
            IntBinaryOperator compare) {
        for (int i = a + 1; i < b; i++) {
            if (compare.applyAsInt(left[i - 1], left[i]) > 0) throw new IllegalArgumentException();
        }
        for (int i = c + 1; i < d; i++) {
            if (compare.applyAsInt(right[i - 1], right[i]) > 0) throw new IllegalArgumentException();
        }
        int[] output = new int[b - a + d - c];
        int at = 0;
        while (a < b && c < d) {
            if (compare.applyAsInt(left[a], right[c]) <= 0) output[at++] = left[a++];
            else output[at++] = right[c++];
        }
        while (a < b) output[at++] = left[a++];
        while (c < d) output[at++] = right[c++];
        return output;
    }

    private void mergeCallbacks() {
        Random inputs = new Random(0x67a30d19L);
        for (int trial = 0; trial < 1500; trial++) {
            int n = inputs.nextInt(96), m = inputs.nextInt(96);
            int[] left = new int[n + 5], right = new int[m + 8];
            for (int i = 0; i < n; i++) left[i + 2] = inputs.nextInt(17) - 8;
            for (int i = 0; i < m; i++) right[i + 3] = inputs.nextInt(17) - 8;
            Arrays.sort(left, 2, n + 2);
            Arrays.sort(right, 3, m + 3);
            int[] savedLeft = left.clone(), savedRight = right.clone();
            List<Long> expectedTape = new ArrayList<>(), actualTape = new ArrayList<>();
            IntBinaryOperator expectedCompare = (x, y) -> {
                expectedTape.add(((long) x << 32) ^ (y & 0xffffffffL));
                return Integer.compare(x, y);
            };
            IntBinaryOperator actualCompare = (x, y) -> {
                actualTape.add(((long) x << 32) ^ (y & 0xffffffffL));
                return Integer.compare(x, y);
            };
            int[] expected = mergeOracle(left, 2, n + 2, right, 3, m + 3, expectedCompare);
            int[] actual = M3ArrayifiedIndexOps.mergeSorted(left, 2, n + 2, right, 3, m + 3, actualCompare);
            equal(actual, expected);
            check(expectedTape.equals(actualTape), "comparator callback tape drift");
            digest = 31 * digest + actualTape.hashCode();
            check(Arrays.equals(left, savedLeft) && Arrays.equals(right, savedRight), "merge wrote inputs");
            check(actual != left && actual != right, "merge result aliases source");
            equal(M3ArrayifiedIndexOps.mergeSorted(left, 2, n + 2, left, 2, n + 2, Integer::compare),
                    mergeOracle(left, 2, n + 2, left, 2, n + 2, Integer::compare));
        }
        equal(M3ArrayifiedIndexOps.mergeSorted(new int[]{21, 22}, 0, 2,
                new int[]{11, 12}, 0, 2, (x, y) -> 0), new int[]{21, 22, 11, 12});
        int[] left = {1, 3, 5}, right = {2, 4, 6};
        for (int stop = 0; stop < 9; stop++) {
            RuntimeException sentinel = new IllegalStateException("sentinel-" + stop);
            int stopAt = stop;
            int[] calls = {0};
            IntBinaryOperator compare = (x, y) -> {
                if (calls[0]++ == stopAt) throw sentinel;
                return x < y ? Integer.MIN_VALUE : x > y ? 17 : 0;
            };
            Throwable failure = null;
            try { M3ArrayifiedIndexOps.mergeSorted(left, 0, 3, right, 0, 3, compare); }
            catch (RuntimeException thrown) { failure = thrown; }
            check(failure == sentinel, "comparator exception identity drift");
            check(calls[0] == stop + 1, "callback after exception");
        }
    }

    private void offsetsAndSeams() {
        for (int length : new int[]{0, 1, 2, 63, 64, 65, 255, 256, 257, 511, 512, 513, 4096}) {
            int[] rows = new int[length + 11];
            byte[] states = new byte[length + 19];
            for (int i = 0; i < rows.length; i++) rows[i] = i % 3 == 0 ? -1 : i;
            for (int i = 0; i < states.length; i++) states[i] = (byte) (i * 13 + 7);
            for (int from : new int[]{0, 5, 11}) for (int stateFrom : new int[]{0, 7, 19}) {
                for (int mask : new int[]{0, 1, 3, 127, 128, 255}) {
                    int[] actual = M3ArrayifiedIndexOps.selected(rows, from, from + length,
                            states, stateFrom, mask, mask >>> 1, backend);
                    equal(actual, selectOracle(rows, from, from + length, states, stateFrom, mask, mask >>> 1));
                }
            }
        }
    }

    private void exceptions() {
        expect(NullPointerException.class, "rows", () -> M3ArrayifiedIndexOps.gather(null, -1, -2, null));
        expect(NullPointerException.class, "ordinals", () -> M3ArrayifiedIndexOps.gather(new int[0], -1, -2, null));
        expect(IndexOutOfBoundsException.class, null, () -> M3ArrayifiedIndexOps.gather(new int[]{1}, 0, 1, new int[]{0, 1}));
        expect(NullPointerException.class, "rows", () -> M3ArrayifiedIndexOps.rotated(null, -1, -2, 0));
        expect(IndexOutOfBoundsException.class, null, () -> M3ArrayifiedIndexOps.rotated(new int[3], 2, 1, 0));
        expect(NullPointerException.class, "rows", () -> M3ArrayifiedIndexOps.selected(null, 0, 0, null, 0, 0, 0, null));
        expect(NullPointerException.class, "alignedStates", () -> M3ArrayifiedIndexOps.selected(new int[0], 0, 0, null, 0, 0, 0, null));
        expect(NullPointerException.class, "backend", () -> M3ArrayifiedIndexOps.selected(new int[0], 0, 0, new byte[0], 0, 0, 0, null));
        expect(IndexOutOfBoundsException.class, null, () -> M3ArrayifiedIndexOps.selected(new int[3], 0, 3, new byte[2], 0, 0, 0, backend));
        expect(IllegalArgumentException.class, "state masks must fit unsigned bytes", () -> M3ArrayifiedIndexOps.selected(new int[0], 0, 0, new byte[0], 0, -1, 0, backend));
        expect(IllegalArgumentException.class, "state masks must fit unsigned bytes", () -> M3ArrayifiedIndexOps.selected(new int[0], 0, 0, new byte[0], 0, 0, 256, backend));
        expect(NullPointerException.class, "left", () -> M3ArrayifiedIndexOps.mergeSorted(null, 0, 0, null, 0, 0, null));
        expect(NullPointerException.class, "right", () -> M3ArrayifiedIndexOps.mergeSorted(new int[0], 0, 0, null, 0, 0, null));
        expect(NullPointerException.class, "order", () -> M3ArrayifiedIndexOps.mergeSorted(new int[0], 0, 0, new int[0], 0, 0, null));
        expect(IllegalArgumentException.class, "input index window is not sorted", () -> M3ArrayifiedIndexOps.mergeSorted(new int[]{3, 1}, 0, 2, new int[0], 0, 0, Integer::compare));
        if (!M3IndexMaskCompactor.nativeAvailable()) {
            for (int required : new int[]{0, 1, 255}) {
                expect(UnsatisfiedLinkError.class, "m3_index_mask JNI library unavailable",
                        () -> M3ArrayifiedIndexOps.selected(new int[0], 0, 0, new byte[0], 0,
                                required, required, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            }
        }
    }

    /** Run on the actual compiled source owner; a native run must never silently skip. */
    public static void run(boolean nativeMode) {
        M3ArrayifiedIndexContract proof = new M3ArrayifiedIndexContract(nativeMode);
        proof.rotationAndGatherWindows();
        proof.allMaskPairs();
        proof.offsetsAndSeams();
        proof.mergeCallbacks();
        proof.exceptions();
        System.out.println("INDEX_CONTRACT_PASS backend=" + proof.backend + " checks=" + proof.checks
                + " digest=" + Long.toUnsignedString(proof.digest));
    }

    public static void main(String[] args) {
        if (args.length != 1 || !(args[0].equals("java") || args[0].equals("native"))) {
            throw new IllegalArgumentException("java|native required");
        }
        run(args[0].equals("native"));
    }
}
