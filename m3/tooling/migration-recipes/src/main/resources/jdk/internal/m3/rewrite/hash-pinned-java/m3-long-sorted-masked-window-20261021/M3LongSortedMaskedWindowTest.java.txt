// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Same checked scalar oracle for the original Maven/Java21 collections owner. */
final class M3LongSortedMaskedWindowTest {
    @Test void javaIndexedSortedWindowContracts() {
        assertTrue(M3LongSortedMaskedWindowContract.run(
                M3IndexMaskCompactor.Backend.JAVA) >= 6000);
    }

    @Test void explicitNativeIndexedSortedWindowContracts() {
        if (Boolean.getBoolean("m3.test.native")) {
            assertTrue(M3IndexMaskCompactor.nativeAvailable(),
                    "explicit JNI verification requires the native library");
            assertTrue(M3LongSortedMaskedWindowContract.run(
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) >= 6000);
        }
    }
}
