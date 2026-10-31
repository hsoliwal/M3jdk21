// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/** Original-owner Java 21 index/state differential; JNI remains an opt-in backend. */
public final class M3StateMaskSelectContract {
    private M3StateMaskSelectContract() { }
    private static long assertions;
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    private static void mustThrow(Class<? extends Throwable> type, Runnable work) {
        boolean caught = false;
        try { work.run(); } catch (Throwable problem) {
            if (!type.isInstance(problem)) throw new AssertionError("unexpected failure", problem);
            caught = true;
        }
        check(caught, "expected " + type.getSimpleName());
    }
    public static void main(String[] args) {
        final Random random = new Random(0x5E1EC7L);
        for (int repetition = 0; repetition < 4400; repetition++) {
            int length = random.nextInt(154);
            int[] slots = new int[length];
            byte[] states = new byte[length];
            for (int i = 0; i < length; i++) {
                slots[i] = random.nextInt(40) - 20;
                states[i] = (byte) random.nextInt(256);
            }
            int required = random.nextInt(256);
            int forbidden = random.nextInt(256) & ~required;
            int[] originalSlots = slots.clone();
            byte[] originalStates = states.clone();
            ArrayList<Integer> oracle = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                int bits = states[i] & 255;
                if ((bits & required) == required && (bits & forbidden) == 0) {
                    oracle.add(slots[i]);
                }
                check(M3StateMaskArrays.matches(states[i], required, forbidden)
                        == ((bits & required) == required && (bits & forbidden) == 0),
                        "mask matches oracle " + repetition);
            }
            int[] actual = M3StateMaskArrays.select(slots, states, required, forbidden,
                    M3IndexMaskCompactor.Backend.JAVA);
            check(actual.length == oracle.size(), "kept count " + repetition);
            for (int i = 0; i < actual.length; i++) {
                check(actual[i] == oracle.get(i), "stable encounter-order slot " + repetition);
            }
            check(Arrays.equals(slots, originalSlots), "caller slots remain owned");
            check(Arrays.equals(states, originalStates), "caller states remain owned");
            int[] direct = M3IndexMaskCompactor.selectWindow(slots, 0, states, 0,
                    slots.length, required, forbidden, M3IndexMaskCompactor.Backend.JAVA);
            check(Arrays.equals(actual, direct), "existing primitive selector equivalence");
            if (forbidden == 0) {
                check(Arrays.equals(actual, M3StateMaskArrays.select(slots, states, required,
                        M3IndexMaskCompactor.Backend.JAVA)), "required-only compatibility");
            }
        }
        int[] rows = {5, 0, -1, 0, 5};
        byte[] flags = {(byte) 0xff, 0, 1, (byte) 0x80, 1};
        check(Arrays.equals(new int[] {-1, 5},
                M3StateMaskArrays.select(rows, flags, 1, 2, M3IndexMaskCompactor.Backend.JAVA)),
                "duplicate and negative physical slot retention");
        mustThrow(IllegalArgumentException.class, () ->
                M3StateMaskArrays.select(rows, flags, 1, 1, M3IndexMaskCompactor.Backend.JAVA));
        mustThrow(IllegalArgumentException.class, () ->
                M3StateMaskArrays.select(rows, flags, 256, 0, M3IndexMaskCompactor.Backend.JAVA));
        mustThrow(IllegalArgumentException.class, () ->
                M3StateMaskArrays.select(rows, flags, 1, 256, M3IndexMaskCompactor.Backend.JAVA));
        mustThrow(IllegalArgumentException.class, () ->
                M3StateMaskArrays.select(rows, new byte[4], 1, 2, M3IndexMaskCompactor.Backend.JAVA));
        mustThrow(NullPointerException.class, () ->
                M3StateMaskArrays.select(null, flags, 1, 2, M3IndexMaskCompactor.Backend.JAVA));
        mustThrow(NullPointerException.class, () ->
                M3StateMaskArrays.select(rows, null, 1, 2, M3IndexMaskCompactor.Backend.JAVA));
        mustThrow(NullPointerException.class, () ->
                M3StateMaskArrays.select(rows, flags, 1, 2, null));
        check(Arrays.equals(rows, new int[] {5, 0, -1, 0, 5}), "validation leaves slots unchanged");
        check(Arrays.equals(flags, new byte[] {(byte) 0xff, 0, 1, (byte) 0x80, 1}),
                "validation leaves flags unchanged");
        if (!M3IndexMaskCompactor.nativeAvailable()) {
            mustThrow(UnsatisfiedLinkError.class, () ->
                    M3StateMaskArrays.select(rows, flags, 1, 2,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
        System.out.println("PASS M3 required/excluded state-mask select checks=" + assertions);
    }
}
