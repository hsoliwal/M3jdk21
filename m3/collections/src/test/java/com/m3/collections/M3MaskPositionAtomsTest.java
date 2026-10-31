/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

/** Exhaustive unsigned-byte truth-domain and real JNI position-selection proof. */
public final class M3MaskPositionAtomsTest {
    private static long checks;
    private static long cases;
    private static long truthCases;
    private static MessageDigest results;
    private static Method nativeRequired;
    private static Method nativeBoth;

    private M3MaskPositionAtomsTest() { }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    /* This oracle tests each binary digit using arithmetic, independently of the
       combined-mask expression and its dominating native overlap guard. */
    private static boolean expected(byte value, int required, int excluded) {
        int state = Byte.toUnsignedInt(value);
        for (int weight = 1; weight <= 128; weight *= 2) {
            int digit = (state / weight) % 2;
            if ((required / weight) % 2 == 1 && digit == 0) return false;
            if ((excluded / weight) % 2 == 1 && digit == 1) return false;
        }
        return true;
    }

    private static int[] oracle(byte[] states, int required, int excluded,
            boolean completeDomain) {
        int[] positions = new int[states.length];
        int kept = 0;
        for (int i = 0; i < states.length; i++) {
            boolean selected = expected(states[i], required, excluded);
            if (completeDomain) {
                truthCases++;
                check(selected == M3IndexMaskCompactor.matches(states[i], required, excluded),
                        "Java predicate differs from independent truth oracle");
            }
            if (selected) positions[kept++] = i;
        }
        return Arrays.copyOf(positions, kept);
    }

    private static int[] direct(Method method, Object... arguments)
            throws ReflectiveOperationException {
        try {
            return (int[]) method.invoke(null, arguments);
        } catch (InvocationTargetException failure) {
            throw new AssertionError("direct JNI invocation failed", failure.getCause());
        }
    }

    private static void record(int[] positions) {
        for (int position : positions) {
            for (int shift = 24; shift >= 0; shift -= 8) {
                results.update((byte) (position >>> shift));
            }
        }
        results.update((byte) 0xff);
    }

    private static void trial(byte[] states, int required, int excluded,
            boolean completeDomain) throws ReflectiveOperationException {
        byte[] original = states.clone();
        int[] expected = oracle(states, required, excluded, completeDomain);
        int[] javaResult = M3IndexMaskCompactor.selectPositions(states, required, excluded,
                M3IndexMaskCompactor.Backend.JAVA);
        int[] nativeResult = M3IndexMaskCompactor.selectPositions(states, required, excluded,
                M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
        // Direct entry exercises the two-mask zero/zero branch, which the public
        // overload routes through the required-only legacy-compatible surface.
        int[] directResult = direct(nativeBoth, states, required, excluded);
        check(Arrays.equals(expected, javaResult), "Java position oracle mismatch");
        check(Arrays.equals(expected, nativeResult), "native position oracle mismatch required="
                + required + " excluded=" + excluded + " length=" + states.length);
        check(Arrays.equals(expected, directResult), "direct native position oracle mismatch");
        check(javaResult != nativeResult && directResult != nativeResult,
                "results must have independent array identities");
        check(expected.length == M3IndexMaskCompactor.selectedCount(states, required, excluded),
                "selected count disagrees with position count");
        if (excluded == 0) {
            check(Arrays.equals(expected, direct(nativeRequired, states, required)),
                    "required-only native entry disagrees with oracle");
        }
        record(nativeResult);
        if (nativeResult.length != 0) nativeResult[0] = Integer.MIN_VALUE;
        check(Arrays.equals(expected, javaResult) && Arrays.equals(expected, directResult),
                "result mutation leaked into independent result");
        check(Arrays.equals(states, original), "selection mutated input states");
        cases++;
    }

    private static void exhaustive() throws ReflectiveOperationException {
        byte[] states = new byte[256];
        for (int i = 0; i < states.length; i++) states[i] = (byte) (i * 197 + 31);
        for (int required = 0; required <= 255; required++) {
            for (int excluded = 0; excluded <= 255; excluded++) {
                trial(states, required, excluded, true);
            }
        }
        check(truthCases == 16_777_216L, "incomplete byte/mask truth-domain");
    }

    private static void chunks() throws ReflectiveOperationException {
        int[][] masks = {{0, 0}, {0, 1}, {1, 0}, {128, 0}, {128, 1},
                {255, 0}, {0, 255}, {1, 1}, {3, 12}, {0x55, 0xaa}, {0x80, 0x80}};
        for (int length : new int[] {0, 1, 2, 7, 255, 256, 257, 511, 512, 513, 1025, 2049}) {
            for (int shape = 0; shape < 5; shape++) {
                byte[] states = new byte[length];
                for (int i = 0; i < length; i++) {
                    states[i] = switch (shape) {
                        case 0 -> 0;
                        case 1 -> (byte) 0xff;
                        case 2 -> (byte) (i * 197 + 31);
                        case 3 -> (byte) ((length - i) * 73);
                        default -> (byte) ((i / 256) % 2 == 0 ? 0 : 255);
                    };
                }
                for (int[] pair : masks) trial(states, pair[0], pair[1], false);
            }
        }
    }

    private static Method method(String name, Class<?>... arguments)
            throws ReflectiveOperationException {
        Method result = M3IndexMaskCompactor.class.getDeclaredMethod(name, arguments);
        result.setAccessible(true);
        return result;
    }

    private static void rejected(Method method, Object... arguments)
            throws ReflectiveOperationException {
        checks++;
        try {
            method.invoke(null, arguments);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof IllegalArgumentException
                    && "invalid indexed window or unsigned-byte mask".equals(cause.getMessage())) {
                return;
            }
            throw new AssertionError("native rejection contract changed", cause);
        }
        throw new AssertionError("native entry accepted invalid input");
    }

    private static void validation() throws ReflectiveOperationException {
        rejected(nativeRequired, null, 0);
        rejected(nativeBoth, null, 0, 0);
        for (int invalid : new int[] {-1, 256, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            rejected(nativeRequired, new byte[0], invalid);
            rejected(nativeBoth, new byte[0], invalid, 0);
            rejected(nativeBoth, new byte[0], 0, invalid);
            rejected(nativeBoth, null, invalid, invalid);
        }
        check((Integer) method("positionsAbiNative").invoke(null) == 1,
                "required-only ABI probe changed");
        check((Integer) method("positionsMasksAbiNative").invoke(null) == 1,
                "required/excluded ABI probe changed");
    }

    public static void main(String[] args) throws Exception {
        check(M3IndexMaskCompactor.nativeAvailable(), "real JNI library is required");
        nativeRequired = method("selectPositionsNative", byte[].class, int.class);
        nativeBoth = method("selectPositionsMasksNative", byte[].class, int.class, int.class);
        results = MessageDigest.getInstance("SHA-256");
        exhaustive();
        chunks();
        validation();
        System.out.println("PASS M3MaskPositionAtomsTest nativeLoaded=true truthCases=" + truthCases
                + " cases=" + cases + " checks=" + checks + " resultDigest="
                + HexFormat.of().formatHex(results.digest()));
    }
}
