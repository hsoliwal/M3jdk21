// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Run the identical oracle in the Maven collections tests and JNI qualification. */
final class RequiredExcludedWindowTest {
    @Test
    void javaReferencePreservesAllIndexedSelectionContracts() {
        assertTrue(RequiredExcludedWindowContract.run(
                IndexMaskCompactor.Backend.JAVA) >= 12_000);
    }

    @Test
    void optionalJniHasTheSameObservableResults() {
        if (Boolean.getBoolean("m3.test.native")) {
            assertTrue(IndexMaskCompactor.nativeAvailable(),
                    "explicit JNI qualification requires loaded library");
        }
        if (IndexMaskCompactor.nativeAvailable()) {
            assertTrue(RequiredExcludedWindowContract.run(
                    IndexMaskCompactor.Backend.NATIVE_REQUIRED) >= 12_000);
        }
    }
}
