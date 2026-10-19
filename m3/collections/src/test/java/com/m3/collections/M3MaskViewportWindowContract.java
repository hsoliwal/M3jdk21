/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Cross-region and alias oracle. JNI is mandatory, never silently skipped. */
public final class M3MaskViewportWindowContract {
    private static int checks;

    private M3MaskViewportWindowContract() { }

    public static void main(String[] args) {
        System.load(System.getProperty("m3.native.path"));
        Random random = new Random(0x193a5b27L);
        for (int length : new int[]{0, 1, 1023, 1024, 1025, 2048, 4097}) {
            for (int alias = 0; alias < 4; alias++) {
                for (int first : new int[]{0, 7, length + 9}) {
                    for (int destination : new int[]{0, 3, length + 11}) {
                        for (int mode = 0; mode < 5; mode++) {
                            compare(random, length, alias, first, destination, mode);
                        }
                    }
                }
            }
        }
        invalidWindows();
        System.out.println("MASK_WINDOW PASS checks=" + checks + " jni=true");
    }

    private static void compare(Random random, int length, int alias,
            int first, int destination, int mode) {
        int size = 3 * length + 32;
        int[] offsets = new int[size];
        int[] states = alias == 3 ? offsets : new int[size];
        for (int i = 0; i < size; i++) {
            // Repeated, ascending, descending and arbitrary logical offsets.
            offsets[i] = mode == 0 ? i / 3 : mode == 1 ? size - i : random.nextInt(257);
            if (states != offsets) states[i] = random.nextInt();
        }
        int required = mode == 0 ? 0 : mode == 1 ? Integer.MIN_VALUE : 1;
        int forbidden = mode == 2 ? required : mode == 3 ? Integer.MIN_VALUE : 0;
        int[] initialTarget = alias == 1 || alias == 3 ? offsets
                : alias == 2 ? states : new int[size];
        int[] expected = initialTarget.clone();
        int count = 0;
        for (int i = first; i < first + length; i++) {
            if ((states[i] & required) == required && (states[i] & forbidden) == 0) {
                expected[destination + count++] = offsets[i];
            }
        }
        for (boolean nativeCall : new boolean[]{false, true}) {
            int[] src = offsets.clone();
            int[] masks = alias == 3 ? src : states.clone();
            int[] target = alias == 1 || alias == 3 ? src
                    : alias == 2 ? masks : initialTarget.clone();
            int actual = nativeCall
                    ? M3MaskViewportNative.project(src, masks, first, length,
                            required, forbidden, target, destination)
                    : M3MaskViewportKernel.project(src, masks, first, length,
                            required, forbidden, target, destination);
            check(actual == count, "selection count");
            check(Arrays.equals(expected, target), "snapshot order and destination fences");
            if (target != src) check(Arrays.equals(offsets, src), "source unchanged");
            if (target != masks) check(Arrays.equals(states, masks), "states unchanged");
        }
    }

    private static void invalidWindows() {
        for (boolean nativeCall : new boolean[]{false, true}) {
            for (int alias = 0; alias < 3; alias++) {
                for (int failure = 0; failure < 4; failure++) {
                    int[] offsets = new int[4099];
                    int[] states = new int[4099];
                    Arrays.fill(offsets, 7);
                    Arrays.fill(states, 1);
                    // A rejected slot in the final region must still be checked.
                    if (failure == 0) {
                        offsets[4097] = -1;
                        states[4097] = 0;
                    }
                    int[] target = alias == 1 ? offsets : alias == 2 ? states : new int[4099];
                    int[] before = target.clone();
                    int first = failure == 2 ? Integer.MAX_VALUE : 0;
                    int length = failure == 3 ? -1 : 4099;
                    int destination = failure == 1 ? 1 : 0;
                    Class<? extends Throwable> expected = failure == 0
                            ? IllegalArgumentException.class : IndexOutOfBoundsException.class;
                    try {
                        if (nativeCall) M3MaskViewportNative.project(offsets, states,
                                first, length, 1, 0, target, destination);
                        else M3MaskViewportKernel.project(offsets, states,
                                first, length, 1, 0, target, destination);
                        throw new AssertionError("missing validation failure");
                    } catch (RuntimeException ex) {
                        check(expected.isInstance(ex), "exception class");
                    }
                    check(Arrays.equals(before, target), "failure left destination intact");
                }
            }
            int[] out = {-9, -9, -9};
            int result = nativeCall
                    ? M3MaskViewportNative.project(new int[]{-1, 42, -1}, new int[]{0, 1, 0},
                            1, 1, 1, 0, out, 1)
                    : M3MaskViewportKernel.project(new int[]{-1, 42, -1}, new int[]{0, 1, 0},
                            1, 1, 1, 0, out, 1);
            check(result == 1 && Arrays.equals(out, new int[]{-9, 42, -9}),
                    "outside-window invalid offsets ignored");
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
