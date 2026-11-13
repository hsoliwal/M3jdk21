// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Required-native integration tests, selected explicitly after C17 JNI build. */
@EnabledIfSystemProperty(named = "m3.collections.all.native.required", matches = "(?i)true")
final class ZeroMaskSuccessorIT {
    @Test
    void intZeroMaskRealJniWindows() {
        assertTrue(IndexMaskCompactor.nativeAvailable(), "actual zero-mask JNI binary required");
        ZeroMaskSuccessorTest.verifyInt(IndexMaskCompactor.Backend.NATIVE_REQUIRED);
    }

    @Test
    void longZeroMaskRealJniWindows() {
        assertTrue(IndexMaskCompactor.nativeAvailable(), "actual zero-mask JNI binary required");
        ZeroMaskSuccessorTest.verifyLong(IndexMaskCompactor.Backend.NATIVE_REQUIRED);
    }
}
