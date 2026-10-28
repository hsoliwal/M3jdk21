/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/**
 * Independent real JNI/Java viewport parity oracle. Run with -Xcheck:jni
 * and the exact M3JDK21 native library compiled from m3maskviewport.c.
 * It proves only the explicitly checked source and runtime profile.
 */
public final class M3MaskViewportCheckedJniContract {
    private static final int SENTINEL = -0x6f6f6f6f;
    private static long checks;

    private M3MaskViewportCheckedJniContract() { }

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: M3MaskViewportCheckedJniContract <native-so>");
        }
        System.load(Path.of(args[0]).toAbsolutePath().toString());
        Random random = new Random(0x4d334a4e494cL);
        for (int size : new int[]{0, 1, 2, 127, 128, 129, 255, 256, 257,
                511, 512, 513, 1024}) {
            int[] offsets = new int[size];
            int[] states = new int[size];
            for (int i = 0; i < size; i++) {
                offsets[i] = random.nextInt(10000);
                states[i] = random.nextInt();
            }
            for (int mode = 0; mode < 7; mode++) {
                int required = switch (mode) {
                    case 1 -> 1;
                    case 3 -> 3;
                    case 4 -> Integer.MIN_VALUE;
                    case 6 -> 1;
                    default -> 0;
                };
                int forbidden = switch (mode) {
                    case 2 -> 2;
                    case 3 -> 4;
                    case 5 -> Integer.MIN_VALUE;
                    case 6 -> 1;
                    default -> 0;
                };
                verifyProject(offsets, states, required, forbidden, 0, size);
                int first = size > 4 ? 2 : 0;
                verifyProject(offsets, states, required, forbidden, first, size - first);
                for (int skip : new int[]{0, 1, 64, Integer.MAX_VALUE}) {
                    for (int limit : new int[]{0, 1, 127, 128, 129, Integer.MAX_VALUE}) {
                        verifyPage(offsets, states, required, forbidden,
                                first, size - first, skip, limit);
                        verifyFrame(offsets, states, required, forbidden,
                                first, size - first, skip, limit);
                    }
                }
            }
        }
        verifyAliases();
        verifyPreflightRefusal();
        System.out.println("PASS M3MaskViewportCheckedJniContract checks=" + checks
                + " -Xcheck:jni=true project/projectPage/projectFramePage");
    }

    private static void verifyProject(int[] offsets, int[] states, int required,
            int forbidden, int first, int length) {
        int[] java = filled(offsets.length + 5);
        int[] nativeResult = filled(offsets.length + 5);
        int a = M3MaskViewportKernel.project(offsets, states, first, length,
                required, forbidden, java, 2);
        int b = M3MaskViewportNative.project(offsets, states, first, length,
                required, forbidden, nativeResult, 2);
        equals(a, b);
        same(java, nativeResult);
    }

    private static void verifyPage(int[] offsets, int[] states, int required,
            int forbidden, int first, int length, int skip, int limit) {
        int[] java = filled(offsets.length + 5);
        int[] nativeResult = filled(offsets.length + 5);
        int a = M3MaskViewportKernel.projectPage(offsets, states, first, length,
                required, forbidden, skip, limit, java, 2);
        int b = M3MaskViewportNative.projectPage(offsets, states, first, length,
                required, forbidden, skip, limit, nativeResult, 2);
        equals(a, b);
        same(java, nativeResult);
    }

    private static void verifyFrame(int[] offsets, int[] states, int required,
            int forbidden, int first, int length, int skip, int limit) {
        int[] expectedRows = filled(offsets.length + 5);
        int[] expectedSlots = filled(offsets.length + 5);
        int[] expectedStates = filled(offsets.length + 5);
        int expected = 0;
        int matches = 0;
        for (int at = first; at < first + length; at++) {
            if (matches(states[at], required, forbidden)) {
                if (matches++ >= skip && expected < limit) {
                    expectedRows[2 + expected] = offsets[at];
                    expectedSlots[2 + expected] = at;
                    expectedStates[2 + expected] = states[at];
                    expected++;
                }
            }
        }
        int[] rows = filled(offsets.length + 5);
        int[] slots = filled(offsets.length + 5);
        int[] words = filled(offsets.length + 5);
        int actual = M3MaskViewportNative.projectFramePage(offsets, states, first,
                length, required, forbidden, skip, limit, rows, 2, slots, 2, words, 2);
        equals(expected, actual);
        same(expectedRows, rows);
        same(expectedSlots, slots);
        same(expectedStates, words);
    }

    private static void verifyAliases() {
        int[] offsets = {4, 8, 16, 32, 64, 128};
        int[] states = {0, 1, 0, 1, 0, 1};
        for (boolean aliasOffsets : new boolean[]{false, true}) {
            int[] javaOffsets = offsets.clone();
            int[] nativeOffsets = offsets.clone();
            int[] javaStates = states.clone();
            int[] nativeStates = states.clone();
            int[] javaTarget = aliasOffsets ? javaOffsets : javaStates;
            int[] nativeTarget = aliasOffsets ? nativeOffsets : nativeStates;
            equals(M3MaskViewportKernel.project(javaOffsets, javaStates, 0, 6,
                            1, 0, javaTarget, 1),
                    M3MaskViewportNative.project(nativeOffsets, nativeStates, 0, 6,
                            1, 0, nativeTarget, 1));
            same(javaTarget, nativeTarget);

            javaOffsets = offsets.clone();
            nativeOffsets = offsets.clone();
            javaStates = states.clone();
            nativeStates = states.clone();
            javaTarget = aliasOffsets ? javaOffsets : javaStates;
            nativeTarget = aliasOffsets ? nativeOffsets : nativeStates;
            equals(M3MaskViewportKernel.projectPage(javaOffsets, javaStates, 0, 6,
                            1, 0, 1, 2, javaTarget, 1),
                    M3MaskViewportNative.projectPage(nativeOffsets, nativeStates, 0, 6,
                            1, 0, 1, 2, nativeTarget, 1));
            same(javaTarget, nativeTarget);
        }
    }

    private static void verifyPreflightRefusal() {
        int[] offsets = {1, 2, -1};
        int[] states = {0, 0, 0};
        int[] output = filled(5);
        int[] before = output.clone();
        refusal(IllegalArgumentException.class, () ->
                M3MaskViewportNative.project(offsets, states, 0, 3,
                        0, 0, output, 0));
        same(before, output);
        refusal(IllegalArgumentException.class, () ->
                M3MaskViewportNative.projectPage(offsets, states, 0, 3,
                        0, 0, Integer.MAX_VALUE, 0, output, 0));
        same(before, output);
        refusal(IllegalArgumentException.class, () ->
                M3MaskViewportNative.projectFramePage(offsets, states, 0, 3,
                        0, 0, Integer.MAX_VALUE, 0,
                        output, 0, new int[2], 0, new int[2], 0));
        same(before, output);
        refusal(IllegalArgumentException.class, () ->
                M3MaskViewportNative.projectFramePage(new int[]{1, 2}, new int[]{0, 0},
                        0, 2, 0, 0, 0, 2,
                        output, 0, output, 0, new int[2], 0));
        same(before, output);
        refusal(IndexOutOfBoundsException.class, () ->
                M3MaskViewportNative.project(new int[]{1, 2}, new int[]{0, 0},
                        0, 2, 0, 0, new int[1], 0));
        refusal(NullPointerException.class, () ->
                M3MaskViewportNative.project(null, new int[0], 0, 0,
                        0, 0, new int[0], 0));
        refusal(IllegalArgumentException.class, () ->
                M3MaskViewportNative.project(new int[]{1, 2}, new int[]{0},
                        0, 1, 0, 0, new int[3], 0));
        refusal(IndexOutOfBoundsException.class, () ->
                M3MaskViewportNative.projectPage(new int[]{1, 2}, new int[]{0, 0},
                        0, 2, 0, 0, -1, 1, output, 0));
        int[] tooSmall = {SENTINEL};
        refusal(IndexOutOfBoundsException.class, () ->
                M3MaskViewportNative.projectFramePage(new int[]{1, 2},
                        new int[]{0, 0}, 0, 2, 0, 0, 0, 2,
                        tooSmall, 0, new int[2], 0, new int[2], 0));
        same(new int[]{SENTINEL}, tooSmall);
    }

    private static boolean matches(int value, int required, int forbidden) {
        return (value & required) == required && (value & forbidden) == 0;
    }

    private static int[] filled(int length) {
        int[] result = new int[length];
        Arrays.fill(result, SENTINEL);
        return result;
    }

    private static void refusal(Class<? extends Throwable> expected, Runnable task) {
        try {
            task.run();
            throw new AssertionError("Expected " + expected.getName());
        } catch (Throwable failure) {
            if (!expected.isInstance(failure)) {
                throw new AssertionError("Unexpected native exception", failure);
            }
            checks++;
        }
    }

    private static void equals(int expected, int observed) {
        if (expected != observed) {
            throw new AssertionError("Expected " + expected + ", found " + observed);
        }
        checks++;
    }

    private static void same(int[] expected, int[] observed) {
        if (!Arrays.equals(expected, observed)) {
            throw new AssertionError("JNI/Java projected arrays diverged");
        }
        checks++;
    }
}
