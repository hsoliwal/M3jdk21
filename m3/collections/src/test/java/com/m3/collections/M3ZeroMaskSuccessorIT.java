// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/** Required-native integration tests, selected explicitly after C17 JNI build. */
final class M3ZeroMaskSuccessorIT {
    @Test
    void intZeroMaskRealJniWindows() {
        assertTrue(M3IndexMaskCompactor.nativeAvailable(), "actual zero-mask JNI binary required");
        M3ZeroMaskSuccessorTest.verifyInt(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
    }

    @Test
    void longZeroMaskRealJniWindows() {
        assertTrue(M3IndexMaskCompactor.nativeAvailable(), "actual zero-mask JNI binary required");
        M3ZeroMaskSuccessorTest.verifyLong(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
    }
}
