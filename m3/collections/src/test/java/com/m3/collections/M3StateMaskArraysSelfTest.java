/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Self-contained Java21 differential oracle; no test framework dependency. */
public final class M3StateMaskArraysSelfTest {
    private static long checks;
    private static final int LIVE = 0x01;
    private static final int DIRTY = 0x02;
    private static final int PINNED = 0x04;

    private record Pair(int slot, byte state) { }

    private M3StateMaskArraysSelfTest() { }

    private static void equal(Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError("expected " + expected + "; got " + actual);
        }
    }

    private static void truth(boolean value) {
        checks++;
        if (!value) throw new AssertionError("failed check " + checks);
    }

    private static void fails(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong failure " + failure, failure);
        }
        throw new AssertionError("missing exception " + type.getName());
    }

    private static void cases() {
        byte[] states = {(byte) 0x80, 0, (byte) 0xff};
        equal(0x80, M3StateMaskArrays.update(states, 0, LIVE, 0));
        equal(0x81, states[0] & 0xff);
        equal(0xff, M3StateMaskArrays.update(states, 2, 0, PINNED));
        equal(0xfb, states[2] & 0xff);
        truth(M3StateMaskArrays.matches(states[0], LIVE, PINNED));
        truth(!M3StateMaskArrays.matches(states[1], LIVE, 0));
        truth(M3StateMaskArrays.matches((byte) 0xff, 0, 0));
        fails(IllegalArgumentException.class, () -> M3StateMaskArrays.matches((byte) 0, 256, 0));
        fails(IllegalArgumentException.class, () -> M3StateMaskArrays.matches((byte) 0, 1, 1));
        fails(IllegalArgumentException.class, () -> M3StateMaskArrays.update(states, 0, 1, 1));
        fails(IndexOutOfBoundsException.class, () -> M3StateMaskArrays.update(states, 3, 1, 0));
        fails(NullPointerException.class, () -> M3StateMaskArrays.update(null, 0, 1, 0));

        int[] slots = {9, 0, 9, -1, 42, 7};
        byte[] flags = {1, 0, 3, 5, 7, 2};
        int next = M3StateMaskArrays.compactWindow(slots, flags, 1, 5, LIVE, DIRTY);
        equal(2, next);
        truth(Arrays.equals(slots, new int[] {9, -1, 0, 0, 0, 7}));
        truth(Arrays.equals(flags, new byte[] {1, 5, 0, 0, 0, 2}));
        M3StateMaskArrays.rotateWindow(slots, flags, 0, 2, Integer.MIN_VALUE);
        truth(Arrays.equals(slots, new int[] {9, -1, 0, 0, 0, 7}));
        int[] empty = {};
        byte[] emptyFlags = {};
        M3StateMaskArrays.rotateWindow(empty, emptyFlags, 0, 0, Integer.MIN_VALUE);
        equal(0, M3StateMaskArrays.compactWindow(empty, emptyFlags, 0, 0, 0, 0));

        int[] mismatched = {1, 2};
        byte[] shortState = {1};
        int[] pristine = mismatched.clone();
        fails(IllegalArgumentException.class,
                () -> M3StateMaskArrays.compactWindow(mismatched, shortState, 0, 1, 1, 0));
        truth(Arrays.equals(pristine, mismatched));
        fails(IndexOutOfBoundsException.class,
                () -> M3StateMaskArrays.rotateWindow(mismatched, new byte[2], -1, 2, 1));
        fails(IllegalArgumentException.class,
                () -> M3StateMaskArrays.compactWindow(mismatched, new byte[2], 0, 2, 1, 1));
        truth(Arrays.equals(pristine, mismatched));
    }

    private static void randomCases() {
        Random random = new Random(0x5A71B17L);
        for (int trial = 0; trial < 700; trial++) {
            int length = random.nextInt(101);
            int[] slots = new int[length];
            byte[] states = new byte[length];
            List<Pair> oracle = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                slots[i] = random.nextInt(13) - 6;  // repeat slots; zero and negative IDs
                states[i] = (byte) random.nextInt(256);
                oracle.add(new Pair(slots[i], states[i]));
            }
            int from = random.nextInt(length + 1);
            int to = from + random.nextInt(length - from + 1);
            int distance = random.nextInt();  // overflow including MIN_VALUE
            Collections.rotate(oracle.subList(from, to), distance);
            M3StateMaskArrays.rotateWindow(slots, states, from, to, distance);
            for (int i = 0; i < length; i++) {
                equal(oracle.get(i).slot, slots[i]);
                equal(oracle.get(i).state, states[i]);
            }
            int required = random.nextInt(256);
            int forbidden = random.nextInt(256) & ~required;
            int written = from;
            for (int read = from; read < to; read++) {
                Pair current = oracle.get(read);
                int value = current.state & 0xff;
                if ((value & required) == required && (value & forbidden) == 0) {
                    oracle.set(written++, current);
                }
            }
            for (int i = written; i < to; i++) oracle.set(i, new Pair(0, (byte) 0));
            int selectedEnd = M3StateMaskArrays.compactWindow(
                    slots, states, from, to, required, forbidden);
            equal(written, selectedEnd);
            for (int i = 0; i < length; i++) {
                equal(oracle.get(i).slot, slots[i]);
                equal(oracle.get(i).state, states[i]);
            }
            // Existing Java and real bounded JNI both use identical required-bit selection.
            int[] javaSelected = M3StateMaskArrays.select(
                    slots, states, LIVE, M3IndexMaskCompactor.Backend.JAVA);
            List<Integer> expected = new ArrayList<>();
            for (int i = 0; i < length; i++) if (((states[i] & 0xff) & LIVE) != 0) {
                expected.add(slots[i]);
            }
            equal(expected.size(), javaSelected.length);
            for (int i = 0; i < javaSelected.length; i++) equal(expected.get(i), javaSelected[i]);
            if (M3IndexMaskCompactor.nativeAvailable()) {
                int[] nativeSelected = M3StateMaskArrays.select(
                        slots, states, LIVE, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                truth(Arrays.equals(javaSelected, nativeSelected));
            }
        }
    }

    public static void main(String[] args) {
        cases();
        randomCases();
        if (!M3IndexMaskCompactor.nativeAvailable()) {
            fails(UnsatisfiedLinkError.class,
                    () -> M3StateMaskArrays.select(new int[] {0}, new byte[] {1}, LIVE,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
        System.out.println("PASS M3StateMaskArrays checks=" + checks
                + " JNI=" + M3IndexMaskCompactor.nativeAvailable());
    }
}
