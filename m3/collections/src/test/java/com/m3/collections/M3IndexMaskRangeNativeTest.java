/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

/** Actual JNI regression: all byte masks, detached results and bounded region edges. */
public final class M3IndexMaskRangeNativeTest {
    private static long checks, cases;
    private static MessageDigest results;

    private M3IndexMaskRangeNativeTest() { }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    // Test each required bit separately, independently of the production mask expression.
    private static boolean matches(byte state, int required) {
        int unsigned = Byte.toUnsignedInt(state);
        for (int place = 1; place <= 128; place *= 2) {
            if ((required / place) % 2 == 1 && (unsigned / place) % 2 == 0) return false;
        }
        return true;
    }

    private static int[] oracle(int[] rows, int first, byte[] states, int required) {
        int[] selected = new int[states.length];
        int count = 0;
        for (int i = 0; i < states.length; i++) {
            if (matches(states[i], required)) selected[count++] = rows[first + i];
        }
        return Arrays.copyOf(selected, count);
    }

    private static void record(int[] values) {
        for (int value : values) {
            for (int shift = 24; shift >= 0; shift -= 8) results.update((byte) (value >>> shift));
        }
    }

    private static void trial(int length, int first) {
        int[] rows = new int[first + length + 3];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = switch (i % 5) {
                case 0 -> Integer.MIN_VALUE;
                case 1 -> Integer.MAX_VALUE;
                case 2, 3 -> -7;
                default -> i * 65537;
            };
        }
        byte[] states = new byte[length];
        // Multiplication by an odd number permutes all 256 unsigned-byte states.
        for (int i = 0; i < length; i++) states[i] = (byte) (i * 197 + first * 31);
        int[] originalRows = rows.clone();
        byte[] originalStates = states.clone();
        int[] whole = Arrays.copyOfRange(rows, first, first + length);
        for (int mask = 0; mask <= 255; mask++) {
            int[] expected = oracle(rows, first, states, mask);
            int[] range = M3IndexMaskCompactor.selectNativeRange(rows, first, length, states, mask);
            int[] nativeWhole = M3IndexMaskCompactor.select(whole, states, mask,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            int[] javaWhole = M3IndexMaskCompactor.select(whole, states, mask,
                    M3IndexMaskCompactor.Backend.JAVA);
            check(Arrays.equals(expected, range), "native range disagrees with independent oracle");
            check(Arrays.equals(expected, nativeWhole), "native whole selection disagrees with oracle");
            check(Arrays.equals(expected, javaWhole), "Java selection disagrees with oracle");
            check(range != rows && nativeWhole != whole && javaWhole != whole,
                    "selection result aliases input");
            record(range);
            if (range.length != 0) range[0] ^= -1;
            if (nativeWhole.length != 0) nativeWhole[0] ^= -1;
            if (javaWhole.length != 0) javaWhole[0] ^= -1;
            check(Arrays.equals(rows, originalRows), "range operation changed input rows");
            check(Arrays.equals(whole, Arrays.copyOfRange(originalRows, first, first + length)),
                    "whole operation changed input rows");
            check(Arrays.equals(states, originalStates), "operation changed input states");
            cases++;
        }
    }

    private static Method nativeMethod(String name, Class<?>... types)
            throws ReflectiveOperationException {
        Method method = M3IndexMaskCompactor.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method;
    }

    private static void rejected(Method method, Object... arguments)
            throws ReflectiveOperationException {
        checks++;
        try {
            method.invoke(null, arguments);
        } catch (InvocationTargetException actual) {
            if (actual.getCause() instanceof IllegalArgumentException) return;
            throw new AssertionError("unexpected native validation exception", actual.getCause());
        }
        throw new AssertionError("native validation accepted invalid input");
    }

    private static void nativeValidation() throws ReflectiveOperationException {
        Method whole = nativeMethod("selectNative", int[].class, byte[].class, int.class);
        Method range = nativeMethod("selectRangeNative", int[].class, byte[].class,
                int.class, int.class, int.class);
        for (int invalidMask : new int[] {-1, 256, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            rejected(whole, new int[0], new byte[0], invalidMask);
            rejected(range, new int[0], new byte[0], 0, 0, invalidMask);
        }
        rejected(whole, null, new byte[0], 0);
        rejected(whole, new int[0], null, 0);
        rejected(whole, new int[1], new byte[0], 0);
        rejected(range, null, new byte[0], 0, 0, 0);
        rejected(range, new int[0], null, 0, 0, 0);
        rejected(range, new int[1], new byte[0], -1, 0, 0);
        rejected(range, new int[1], new byte[0], 0, -1, 0);
        rejected(range, new int[1], new byte[0], 2, 0, 0);
        rejected(range, new int[1], new byte[0], 0, Integer.MAX_VALUE, 0);
        rejected(range, new int[1], new byte[0], Integer.MAX_VALUE, 1, 0);
        rejected(range, new int[2], new byte[1], 0, 2, 0);
        int[] end = (int[]) range.invoke(null, new int[1], new byte[0], 1, 0, 0);
        check(end.length == 0, "valid empty window at end was rejected");
    }

    public static void main(String[] args) throws Exception {
        check(M3IndexMaskCompactor.nativeAvailable(), "actual JNI library is required");
        Method abi = nativeMethod("rangeAbiNative");
        check((Integer) abi.invoke(null) == 1, "actual range JNI ABI is required");
        results = MessageDigest.getInstance("SHA-256");
        for (int length : new int[] {0, 1, 2, 255, 256, 257, 511, 512, 513, 1025}) {
            for (int first : new int[] {0, 1, 7}) trial(length, first);
        }
        nativeValidation();
        System.out.println("PASS M3IndexMaskRangeNativeTest nativeLoaded=true cases=" + cases
                + " checks=" + checks + " resultDigest="
                + HexFormat.of().formatHex(results.digest()));
    }
}
