// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.LongStateArray;

/** JUnit and existing JNI-selector parity gates for the implicit identity path. */
//FLLDCIM: MRL - fold-close audit leaf L0180: ADD from PR #1791 head f452419fdcee, retained variant blob d9bf058b77b4, retain merge of #2070 (2028b1fb5f4) kept master's side; recipe make/m3/collections/qualification/foldaudit-collections-20261107/apply.py v1
final class IndexPathIdentityTest {
    @Test void identityModeMatchesOriginalRankAndWindow() throws ReflectiveOperationException {
        IndexPathIdentitySelfTest.main(new String[0]);
    }

    @Test void detachedLongStateArrayRetainsItsJavaAndJniContracts() {
        LongStateArray owner = new LongStateArray();
        for (int i = 0; i < 257; i++) {
            owner.add(i % 9 - 4, i == 63 || i == 128 || i == 255
                    ? 0 : (i % 3 == 0 ? Integer.MIN_VALUE : -1));
        }
        LongStateArray.Snapshot capture = owner.snapshot();
        LongStateArray.Selection java = capture.prepare(
                Integer.MIN_VALUE, 0, IndexPathMasks.Backend.JAVA);
        int count = 0;
        for (int i = 0; i < capture.size(); i++) {
            if ((capture.stateAt(i) & Integer.MIN_VALUE) != 0) count++;
        }
        assertEquals(count, java.size());
        assertEquals(count, java.rank(capture.size()));
        if (IndexPathMasks.nativeAvailable()) {
            LongStateArray.Selection nativeSelected = capture.prepare(
                    Integer.MIN_VALUE, 0, IndexPathMasks.Backend.NATIVE_REQUIRED);
            assertArrayEquals(java.toArray(), nativeSelected.toArray());
            for (int i = 0; i < java.size(); i++) {
                assertEquals(java.sourceOrdinal(i), nativeSelected.sourceOrdinal(i));
            }
        } else {
            assertThrows(UnsatisfiedLinkError.class, () -> capture.prepare(
                    Integer.MIN_VALUE, 0, IndexPathMasks.Backend.NATIVE_REQUIRED));
        }
    }
}
