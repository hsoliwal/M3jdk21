/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Actual JNI parity for the additive required/excluded logical-position ABI. */
public final class M3RequiredExcludedMaskSelectorNativeContract {
    private M3RequiredExcludedMaskSelectorNativeContract() { }

    public static void main(String[] args) {
        if (!M3IndexMaskCompactor.nativeAvailable()) {
            throw new AssertionError("m3_collections_mask JNI library unavailable");
        }
        Random random = new Random(0x4d334d41534b5258L);
        long checks = 0;
        for (int trial = 0; trial < 512; trial++) {
            int length = switch (trial % 8) {
                case 0 -> 0;
                case 1 -> 1;
                case 2 -> 255;
                case 3 -> 256;
                case 4 -> 257;
                default -> random.nextInt(1200);
            };
            byte[] states = new byte[length];
            random.nextBytes(states);
            int required = random.nextInt(256);
            int excluded = random.nextInt(256);
            int[] javaRows = M3IndexMaskCompactor.selectPositions(
                    states, required, excluded, M3IndexMaskCompactor.Backend.JAVA);
            int[] nativeRows = M3IndexMaskCompactor.selectPositions(
                    states, required, excluded, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            if (!Arrays.equals(javaRows, nativeRows)) {
                throw new AssertionError("JNI mismatch trial=" + trial
                        + " required=" + required + " excluded=" + excluded);
            }
            checks++;
        }
        System.out.println("PASS M3RequiredExcludedMaskSelectorNativeContract checks=" + checks);
    }
}
