// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;

/** Preserve the current capability/refusal contract across older and absent JNI binaries. */
public final class M3RequiredExcludedLegacyProbe {
    private static int checks;
    private M3RequiredExcludedLegacyProbe() { }
    private static void require(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("capability contract");
    }
    private static void unavailable(String message, Runnable operation) {
        try { operation.run(); }
        catch (UnsatisfiedLinkError expected) {
            require(message.equals(expected.getMessage()));
            return;
        }
        throw new AssertionError("native-required operation did not refuse");
    }
    public static void main(String[] args) {
        String mode = args[0];
        var nativeBackend = M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
        byte[] states = {0, 1, 2, 3, (byte) 128, (byte) 255};
        require(M3IndexMaskCompactor.nativeAvailable() != mode.equals("missing"));
        for (int[] masks : new int[][] {{0,1},{1,2},{128,1},{1,1},{255,255}}) {
            int required = masks[0], excluded = masks[1];
            if (mode.equals("current")) {
                require(Arrays.equals(
                        M3IndexMaskCompactor.selectPositions(states, required, excluded,
                                M3IndexMaskCompactor.Backend.JAVA),
                        M3IndexMaskCompactor.selectPositions(states, required, excluded, nativeBackend)));
            } else {
                String message = mode.equals("missing")
                        ? "m3_index_mask JNI library unavailable"
                        : "m3_index_mask required/excluded position ABI unavailable";
                unavailable(message, () -> M3IndexMaskCompactor.selectPositions(
                        states, required, excluded, nativeBackend));
            }
        }
        if (mode.equals("missing")) {
            unavailable("m3_index_mask JNI library unavailable", () ->
                    M3IndexMaskCompactor.selectPositions(states, 1, 0, nativeBackend));
        } else {
            require(Arrays.equals(new int[] {1,3,5},
                    M3IndexMaskCompactor.selectPositions(states, 1, 0, nativeBackend)));
        }
        System.out.println("PASS M3RequiredExcludedLegacyProbe mode=" + mode + " checks=" + checks);
    }
}
