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

import java.util.Arrays;
import java.util.Random;

/**
 * Independently authored public-API oracle for detached required/excluded windows.
 *
 * <p>Template status: authored, not compiled or executed. Run unchanged against
 * separately built baseline and candidate owners. The native lane invokes the
 * actual library; the separate C region fixture is only a JNI-table model.
 * Inputs are stable during calls. Concurrent-mutation diagnostics are exercised
 * deterministically in the C fixture, not by a racy Java test.
 */
public final class M3RequiredExcludedWindowRefinementContract {
    private static final long SEED = 0xcbf29ce484222325L;
    private static long checks;
    private static long cases;
    private static long digest;

    private M3RequiredExcludedWindowRefinementContract() { }

    private static final class Input {
        final int[] ints;
        final long[] longs;
        final byte[] states;
        final int rowOffset;
        final int stateOffset;
        final int length;

        Input(int length, int rowOffset, int stateOffset) {
            this.length = length;
            this.rowOffset = rowOffset;
            this.stateOffset = stateOffset;
            ints = new int[rowOffset + length + 7];
            longs = new long[rowOffset + length + 7];
            states = new byte[stateOffset + length + 11];
            Arrays.fill(ints, 0x5a5a5a5a);
            Arrays.fill(longs, 0x5a5a5a5a5a5a5a5aL);
            Arrays.fill(states, (byte) 0xa5);
            for (int i = 0; i < length; i++) {
                ints[rowOffset + i] = switch (i % 8) {
                    case 0, 3 -> Integer.MIN_VALUE;
                    case 1, 6 -> Integer.MAX_VALUE;
                    case 2 -> 0;
                    case 4 -> -7;
                    case 5 -> 7;
                    default -> i % 13 - 6;
                };
                longs[rowOffset + i] = switch (i % 8) {
                    case 0, 3 -> Long.MIN_VALUE;
                    case 1, 6 -> Long.MAX_VALUE;
                    case 2 -> 0L;
                    case 4 -> -7L;
                    case 5 -> 7L;
                    default -> (1L << 40) + i % 13;
                };
            }
        }
    }

    /** Per-bit obligations, independent of the production conjunction expression. */
    private static boolean admitted(byte state, int required, int excluded) {
        int bits = Byte.toUnsignedInt(state);
        for (int bit = 1; bit <= 128; bit <<= 1) {
            boolean present = (bits & bit) != 0;
            if ((required & bit) != 0 && !present) return false;
            if ((excluded & bit) != 0 && present) return false;
        }
        return true;
    }

    private static void compare(Input input, int required, int excluded,
            M3IndexMaskCompactor.Backend backend, boolean checkDetachment) {
        cases++;
        int[] originalInts = input.ints.clone();
        long[] originalLongs = input.longs.clone();
        byte[] originalStates = input.states.clone();
        int[] intScratch = new int[input.length];
        long[] longScratch = new long[input.length];
        int kept = 0;
        for (int ordinal = 0; ordinal < input.length; ordinal++) {
            if (admitted(input.states[input.stateOffset + ordinal], required, excluded)) {
                intScratch[kept] = input.ints[input.rowOffset + ordinal];
                longScratch[kept] = input.longs[input.rowOffset + ordinal];
                kept++;
            }
        }
        int[] expectedInts = Arrays.copyOf(intScratch, kept);
        long[] expectedLongs = Arrays.copyOf(longScratch, kept);
        int[] selectedInts = M3IndexMaskCompactor.selectWindow(input.ints, input.rowOffset,
                input.states, input.stateOffset, input.length, required, excluded, backend);
        long[] selectedLongs = M3IndexMaskCompactor.selectLongWindow(input.longs, input.rowOffset,
                input.states, input.stateOffset, input.length, required, excluded, backend);
        yes(Arrays.equals(expectedInts, selectedInts), "int oracle, case " + cases);
        yes(Arrays.equals(expectedLongs, selectedLongs), "long oracle, case " + cases);
        yes(selectedInts != input.ints && selectedLongs != input.longs, "detached result owners");
        yes(Arrays.equals(originalInts, input.ints), "int source/canaries unchanged");
        yes(Arrays.equals(originalLongs, input.longs), "long source/canaries unchanged");
        yes(Arrays.equals(originalStates, input.states), "state source/canaries unchanged");
        mix(input.length);
        mix(input.rowOffset);
        mix(input.stateOffset);
        mix(required);
        mix(excluded);
        mix(selectedInts.length);
        for (int value : selectedInts) mix(value);
        mix(selectedLongs.length);
        for (long value : selectedLongs) mix(value);

        if (checkDetachment) {
            int[] againInts = M3IndexMaskCompactor.selectWindow(input.ints, input.rowOffset,
                    input.states, input.stateOffset, input.length, required, excluded, backend);
            long[] againLongs = M3IndexMaskCompactor.selectLongWindow(input.longs, input.rowOffset,
                    input.states, input.stateOffset, input.length, required, excluded, backend);
            yes(againInts != selectedInts && againLongs != selectedLongs, "calls own independent arrays");
            if (kept != 0) {
                selectedInts[0] ^= 1;
                selectedLongs[0] ^= 1L;
                yes(Arrays.equals(expectedInts, againInts), "returned int arrays do not alias");
                yes(Arrays.equals(expectedLongs, againLongs), "returned long arrays do not alias");
                yes(Arrays.equals(originalInts, input.ints), "int result write cannot reach source");
                yes(Arrays.equals(originalLongs, input.longs), "long result write cannot reach source");
            }
            if (input.length != 0) {
                input.ints[input.rowOffset] ^= 1;
                input.longs[input.rowOffset] ^= 1L;
                input.states[input.stateOffset] ^= (byte) 0xff;
                yes(Arrays.equals(expectedInts, againInts), "later input changes cannot reach int result");
                yes(Arrays.equals(expectedLongs, againLongs), "later input changes cannot reach long result");
                input.ints[input.rowOffset] = originalInts[input.rowOffset];
                input.longs[input.rowOffset] = originalLongs[input.rowOffset];
                input.states[input.stateOffset] = originalStates[input.stateOffset];
            }
        }
    }

    private static void boundaries(M3IndexMaskCompactor.Backend backend) {
        int[] lengths = {0, 1, 2, 127, 128, 129, 255, 256, 257, 511, 512, 513,
                1023, 1024, 1025, 4097};
        int[][] offsets = {{0, 0}, {3, 5}, {7, 1}, {1, 9}};
        int[][] masks = {{1, 128}, {128, 1}, {0, 128}, {127, 128}, {0, 255},
                {1, 1}, {255, 255}, {0, 0}, {255, 0}};
        for (int length : lengths) {
            for (int[] offset : offsets) {
                for (int pattern = 0; pattern < 7; pattern++) {
                    Input input = new Input(length, offset[0], offset[1]);
                    for (int i = 0; i < length; i++) {
                        int word = switch (pattern) {
                            case 0 -> 0;
                            case 1 -> 1;
                            case 2 -> 129;
                            case 3 -> i == length - 1 ? 1 : 0;
                            case 4 -> i == 0 || i == length - 1 || i % 256 == 255
                                    || i % 256 == 0 ? 1 : 128;
                            case 5 -> i >= 256 && i < 512 ? 128 : (i % 2 == 0 ? 1 : 129);
                            default -> (i * 73 + (i >>> 2)) & 255;
                        };
                        input.states[input.stateOffset + i] = (byte) word;
                    }
                    for (int[] mask : masks) compare(input, mask[0], mask[1], backend, true);
                }
            }
        }
        int[] rows = {11, 22};
        long[] longRows = {11L, 22L};
        byte[] states = {1, 1, 1};
        yes(M3IndexMaskCompactor.selectWindow(rows, rows.length, states, states.length,
                0, 1, 128, backend).length == 0, "int empty window at both exact ends");
        yes(M3IndexMaskCompactor.selectLongWindow(longRows, longRows.length, states, states.length,
                0, 1, 128, backend).length == 0, "long empty window at both exact ends");
    }

    private static void allMaskPairs(M3IndexMaskCompactor.Backend backend) {
        Input input = new Input(256, 3, 5);
        for (int state = 0; state < 256; state++) input.states[input.stateOffset + state] = (byte) state;
        for (int required = 0; required < 256; required++) {
            for (int excluded = 0; excluded < 256; excluded++) {
                compare(input, required, excluded, backend, false);
            }
        }
    }

    private static void randomized(M3IndexMaskCompactor.Backend backend) {
        Random random = new Random(0x4d33575052454658L);
        for (int trial = 0; trial < 512; trial++) {
            Input input = new Input(random.nextInt(2050), random.nextInt(13), random.nextInt(13));
            random.nextBytes(input.states);
            for (int i = 0; i < input.length; i++) {
                if (i % 5 != 0) {
                    input.ints[input.rowOffset + i] = random.nextInt(23) - 11;
                    input.longs[input.rowOffset + i] = random.nextLong();
                }
            }
            int required = random.nextInt(256);
            int excluded = random.nextInt(256);
            compare(input, required, excluded, backend, trial % 17 == 0);
        }
    }

    private static void validation(M3IndexMaskCompactor.Backend backend) {
        int[] rows = {11, 22};
        long[] longRows = {11, 22};
        byte[] states = {1, 1, 1};
        refused(NullPointerException.class, "orderedRows", () -> M3IndexMaskCompactor.selectWindow(
                null, -1, null, -1, -1, -1, -1, null));
        refused(NullPointerException.class, "orderedRows", () -> M3IndexMaskCompactor.selectLongWindow(
                null, -1, null, -1, -1, -1, -1, null));
        refused(NullPointerException.class, "states", () -> M3IndexMaskCompactor.selectWindow(
                rows, -1, null, -1, -1, -1, -1, null));
        refused(NullPointerException.class, "states", () -> M3IndexMaskCompactor.selectLongWindow(
                longRows, -1, null, -1, -1, -1, -1, null));
        refused(NullPointerException.class, "backend", () -> M3IndexMaskCompactor.selectWindow(
                rows, -1, states, -1, -1, -1, -1, null));
        refused(NullPointerException.class, "backend", () -> M3IndexMaskCompactor.selectLongWindow(
                longRows, -1, states, -1, -1, -1, -1, null));
        for (int[] geometry : new int[][] {{-1, 0, 0}, {0, -1, 0}, {0, 0, -1},
                {3, 0, 0}, {0, 4, 0}, {1, 0, 2}, {0, 2, 2},
                {Integer.MAX_VALUE, 0, 1}, {0, Integer.MAX_VALUE, 1},
                {1, 1, Integer.MAX_VALUE}}) {
            refused(IndexOutOfBoundsException.class, null, () -> M3IndexMaskCompactor.selectWindow(
                    rows, geometry[0], states, geometry[1], geometry[2], 256, -1, backend));
            refused(IndexOutOfBoundsException.class, null, () -> M3IndexMaskCompactor.selectLongWindow(
                    longRows, geometry[0], states, geometry[1], geometry[2], 256, -1, backend));
        }
        for (int[] masks : new int[][] {{-1, 0}, {256, 0}, {0, -1}, {0, 256}, {256, -1}}) {
            refused(IllegalArgumentException.class, "required mask must fit an unsigned byte",
                    () -> M3IndexMaskCompactor.selectWindow(rows, 0, states, 0, 2,
                            masks[0], masks[1], backend));
            refused(IllegalArgumentException.class, "required mask must fit an unsigned byte",
                    () -> M3IndexMaskCompactor.selectLongWindow(longRows, 0, states, 0, 2,
                            masks[0], masks[1], backend));
        }
        yes(Arrays.equals(rows, new int[] {11, 22}), "invalid int calls preserve inputs");
        yes(Arrays.equals(longRows, new long[] {11, 22}), "invalid long calls preserve inputs");
        yes(Arrays.equals(states, new byte[] {1, 1, 1}), "invalid calls preserve states");
    }

    private static void unavailable(boolean libraryMissing) {
        yes(M3IndexMaskCompactor.nativeAvailable() != libraryMissing,
                libraryMissing ? "unavailable lane must have no loadable library"
                        : "legacy lane must load a library without the window-mask ABI");
        var backend = M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
        validation(backend);
        String message = libraryMissing ? "m3_index_mask JNI library unavailable"
                : "m3_index_mask required/excluded window ABI unavailable";
        for (int length : new int[] {0, 1, 257}) {
            Input input = new Input(length, 3, 5);
            for (int[] masks : new int[][] {{0, 128}, {1, 128}, {1, 1}, {255, 255}}) {
                refused(UnsatisfiedLinkError.class, message, () -> M3IndexMaskCompactor.selectWindow(
                        input.ints, input.rowOffset, input.states, input.stateOffset, input.length,
                        masks[0], masks[1], backend));
                refused(UnsatisfiedLinkError.class, message, () -> M3IndexMaskCompactor.selectLongWindow(
                        input.longs, input.rowOffset, input.states, input.stateOffset, input.length,
                        masks[0], masks[1], backend));
            }
        }
        System.out.println("CAPABILITY_REFUSAL mode="
                + (libraryMissing ? "unavailable" : "legacy-window-abi") + " PASS");
    }

    private static long run(M3IndexMaskCompactor.Backend backend) {
        long priorChecks = checks;
        long priorCases = cases;
        digest = SEED;
        boundaries(backend);
        allMaskPairs(backend);
        randomized(backend);
        validation(backend);
        long result = digest;
        System.out.printf("WINDOW_CONTRACT backend=%s cases=%d checks=%d digest=%016x%n",
                backend, cases - priorCases, checks - priorChecks, result);
        return result;
    }

    private static void refused(Class<? extends Throwable> expected, String message, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (failure.getClass() != expected) throw new AssertionError("wrong exception type", failure);
            if (message != null && !message.equals(failure.getMessage())) {
                throw new AssertionError("wrong exception message: " + failure.getMessage(), failure);
            }
            for (char c : expected.getName().toCharArray()) mix(c);
            if (message != null) for (char c : message.toCharArray()) mix(c);
            return;
        }
        throw new AssertionError("missing " + expected.getName());
    }

    private static void yes(boolean condition, String finding) {
        checks++;
        if (!condition) throw new AssertionError(finding + " at check " + checks);
    }

    private static void mix(long value) {
        for (int shift = 0; shift < Long.SIZE; shift += Byte.SIZE) {
            digest ^= (value >>> shift) & 0xffL;
            digest *= 0x100000001b3L;
        }
    }

    public static void main(String[] args) {
        String mode = args.length == 0 ? "--java" : args[0];
        if (args.length > 1 || !("--java".equals(mode) || "--native".equals(mode)
                || "--unavailable".equals(mode) || "--legacy-window-abi".equals(mode))) {
            throw new IllegalArgumentException("use --java, --native, --unavailable, or --legacy-window-abi");
        }
        if ("--unavailable".equals(mode)) {
            digest = SEED;
            unavailable(true);
        } else if ("--legacy-window-abi".equals(mode)) {
            digest = SEED;
            unavailable(false);
        } else {
            if ("--native".equals(mode)) {
                yes(M3IndexMaskCompactor.nativeAvailable(), "current real JNI library is required");
            }
            long javaDigest = run(M3IndexMaskCompactor.Backend.JAVA);
            if ("--native".equals(mode)) {
                long nativeDigest = run(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                yes(javaDigest == nativeDigest, "Java and actual JNI observation digests agree");
            }
        }
        System.out.println("PASS M3RequiredExcludedWindowRefinementContract mode=" + mode + " checks=" + checks);
    }
}
