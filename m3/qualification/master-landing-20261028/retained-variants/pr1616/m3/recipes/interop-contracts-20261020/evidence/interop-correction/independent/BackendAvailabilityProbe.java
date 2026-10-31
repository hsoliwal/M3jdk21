/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import com.m3.collections.M3IndexMaskCompactor;
import com.m3.collections.M3IndexPathMasks;

/** Scratch observation of each actual backend before the existing contract runs. */
public final class BackendAvailabilityProbe {
    private BackendAvailabilityProbe() { }
    public static void main(String[] args) {
        boolean expected = Boolean.parseBoolean(args[0]);
        boolean pathMasks = M3IndexPathMasks.nativeAvailable();
        boolean byteMasks = M3IndexMaskCompactor.nativeAvailable();
        if (pathMasks != expected || byteMasks != expected) {
            throw new AssertionError("backend observation differs from requested mode");
        }
        System.out.println("BACKEND_AVAILABILITY pathMasks=" + pathMasks + " byteMasks=" + byteMasks);
        M3GuavaListMultimapContract.main(new String[0]);
    }
}
