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
