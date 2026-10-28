// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** JUnit and existing JNI-selector parity gates for the implicit identity path. */
final class M3IndexPathIdentityTest {
    @Test void identityModeMatchesOriginalRankAndWindow() throws ReflectiveOperationException {
        M3IndexPathIdentitySelfTest.main(new String[0]);
    }

    @Test void detachedLongStateArrayRetainsItsJavaAndJniContracts() {
        M3LongStateArray owner = new M3LongStateArray();
        for (int i = 0; i < 257; i++) {
            owner.add(i % 9 - 4, i == 63 || i == 128 || i == 255
                    ? 0 : (i % 3 == 0 ? Integer.MIN_VALUE : -1));
        }
        M3LongStateArray.Snapshot capture = owner.snapshot();
        M3LongStateArray.Selection java = capture.prepare(
                Integer.MIN_VALUE, 0, M3IndexPathMasks.Backend.JAVA);
        int count = 0;
        for (int i = 0; i < capture.size(); i++) {
            if ((capture.stateAt(i) & Integer.MIN_VALUE) != 0) count++;
        }
        assertEquals(count, java.size());
        assertEquals(count, java.rank(capture.size()));
        if (M3IndexPathMasks.nativeAvailable()) {
            M3LongStateArray.Selection nativeSelected = capture.prepare(
                    Integer.MIN_VALUE, 0, M3IndexPathMasks.Backend.NATIVE_REQUIRED);
            assertArrayEquals(java.toArray(), nativeSelected.toArray());
            for (int i = 0; i < java.size(); i++) {
                assertEquals(java.sourceOrdinal(i), nativeSelected.sourceOrdinal(i));
            }
        } else {
            assertThrows(UnsatisfiedLinkError.class, () -> capture.prepare(
                    Integer.MIN_VALUE, 0, M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        }
    }
}
