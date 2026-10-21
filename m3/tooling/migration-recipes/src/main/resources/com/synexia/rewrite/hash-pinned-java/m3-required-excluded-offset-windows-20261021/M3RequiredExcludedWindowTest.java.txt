// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Run the identical oracle in the Maven collections tests and JNI qualification. */
final class M3RequiredExcludedWindowTest {
    @Test
    void javaReferencePreservesAllIndexedSelectionContracts() {
        assertTrue(M3RequiredExcludedWindowContract.run(
                M3IndexMaskCompactor.Backend.JAVA) >= 12_000);
    }

    @Test
    void optionalJniHasTheSameObservableResults() {
        if (Boolean.getBoolean("m3.test.native")) {
            assertTrue(M3IndexMaskCompactor.nativeAvailable(),
                    "explicit JNI qualification requires loaded library");
        }
        if (M3IndexMaskCompactor.nativeAvailable()) {
            assertTrue(M3RequiredExcludedWindowContract.run(
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) >= 12_000);
        }
    }
}
