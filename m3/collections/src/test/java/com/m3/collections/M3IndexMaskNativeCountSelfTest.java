/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Independent native/cardinality differential; no challenge implementation is copied. */
public final class M3IndexMaskNativeCountSelfTest {
    private static final long SEED = 0x4d334e4154495645L;
    private static long cases;
    private static long checks;

    private M3IndexMaskNativeCountSelfTest() { }

    public static void main(String[] arguments) {
        boolean nativeRun = arguments.length == 1 && "--native".equals(arguments[0]);
        boolean legacy = arguments.length == 1 && "--legacy-native".equals(arguments[0]);
        boolean absent = arguments.length == 1 && "--native-unavailable".equals(arguments[0]);
        if (arguments.length > 1 || (arguments.length == 1 && !nativeRun && !legacy && !absent)) {
            throw new IllegalArgumentException("expected --native, --legacy-native or --native-unavailable");
        }
        if (nativeRun || legacy) check(M3IndexMaskCompactor.nativeAvailable(), "library must load");
        if (absent) check(!M3IndexMaskCompactor.nativeAvailable(), "missing library expected");
        validation();

        for (int state = 0; state < 256; state++) {
            byte[] input = {(byte) state};
            for (int mask = 0; mask < 256; mask++) countOracle(input, mask, nativeRun);
        }
        for (int width : new int[] {0, 1, 2, 7, 31, 32, 63, 64, 65, 127, 128,
                255, 256, 257, 511, 512, 513, 1024, 4097}) {
            byte[] states = new byte[width];
            for (int i = 0; i < states.length; i++) {
                states[i] = (byte) (i * 101 + (i >>> 3));
            }
            for (int mask = 0; mask < 256; mask++) countOracle(states, mask, nativeRun);
            Arrays.fill(states, (byte) 0xff);
            for (int mask : new int[] {0, 1, 127, 128, 255}) countOracle(states, mask, nativeRun);
            Arrays.fill(states, (byte) 0);
            for (int mask : new int[] {0, 1, 127, 128, 255}) countOracle(states, mask, nativeRun);
        }
        Random random = new Random(SEED);
        for (int trial = 0; trial < 2048; trial++) {
            byte[] states = new byte[random.nextInt(trial % 29 == 0 ? 32769 : 1025)];
            random.nextBytes(states);
            countOracle(states, random.nextInt(256), nativeRun);
        }

        if (legacy) {
            for (int mask : new int[] {0, 1, 128, 255}) {
                failures(UnsatisfiedLinkError.class,
                        "m3_collections_mask native count ABI unavailable",
                        () -> M3IndexMaskCompactor.selectedCount(
                                new byte[] {1, 2, 3}, mask,
                                M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            }
            int[] rowIds = {-4, Integer.MIN_VALUE, -4, Integer.MAX_VALUE};
            int[] selected = M3IndexMaskCompactor.select(rowIds,
                    new byte[] {1, 0, 1, 0}, 1, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            check(Arrays.equals(selected, new int[] {-4, -4}),
                    "legacy JNI selector changed");
        }
        if (absent) {
            failures(UnsatisfiedLinkError.class, "m3_index_mask JNI library unavailable",
                    () -> M3IndexMaskCompactor.selectedCount(new byte[] {1}, 0,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
        System.out.println("PASS M3IndexMaskNativeCountSelfTest cases=" + cases
                + " checks=" + checks + " native=" + nativeRun
                + " legacy=" + legacy + " absent=" + absent);
    }

    private static void validation() {
        for (M3IndexMaskCompactor.Backend backend
                : M3IndexMaskCompactor.Backend.values()) {
            failures(NullPointerException.class, "rowStates",
                    () -> M3IndexMaskCompactor.selectedCount(null, -1, backend));
            for (int invalid : new int[] {-256, -1, 256, 257, Integer.MIN_VALUE,
                    Integer.MAX_VALUE}) {
                failures(IllegalArgumentException.class,
                        "required mask must fit an unsigned byte",
                        () -> M3IndexMaskCompactor.selectedCount(new byte[0],
                                invalid, backend));
            }
        }
        failures(NullPointerException.class, "backend",
                () -> M3IndexMaskCompactor.selectedCount(new byte[0], -1, null));
    }

    private static void countOracle(byte[] states, int mask, boolean nativeRun) {
        cases++;
        byte[] snapshot = states.clone();
        int expected = 0;
        for (byte state : states) {
            int bits = Byte.toUnsignedInt(state);
            boolean matches = true;
            for (int bit = 0; bit < Byte.SIZE; bit++) {
                if ((mask & (1 << bit)) != 0 && (bits & (1 << bit)) == 0) {
                    matches = false;
                    break;
                }
            }
            if (matches) expected++;
        }
        check(M3IndexMaskCompactor.selectedCount(states, mask) == expected,
                "legacy Java cardinality mismatch");
        check(M3IndexMaskCompactor.selectedCount(states, mask,
                M3IndexMaskCompactor.Backend.JAVA) == expected,
                "additive Java cardinality mismatch");
        check(M3IndexMaskCompactor.selectPositions(states, mask,
                M3IndexMaskCompactor.Backend.JAVA).length == expected,
                "position-selection cardinality mismatch");
        if (nativeRun) {
            check(M3IndexMaskCompactor.selectedCount(states, mask,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) == expected,
                    "native cardinality mismatch");
        }
        check(Arrays.equals(snapshot, states), "state lane mutated");
    }

    private static void failures(Class<? extends Throwable> expected,
            String message, Runnable operation) {
        checks++;
        try {
            operation.run();
        } catch (Throwable failure) {
            if (failure.getClass() == expected && message.equals(failure.getMessage())) {
                return;
            }
            throw new AssertionError("incorrect failure: " + failure, failure);
        }
        throw new AssertionError("expected failure missing: " + expected.getSimpleName());
    }

    private static void check(boolean assertion, String reason) {
        checks++;
        if (!assertion) throw new AssertionError(reason);
    }
}
