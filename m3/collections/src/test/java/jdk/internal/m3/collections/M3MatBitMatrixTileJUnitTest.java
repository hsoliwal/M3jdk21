// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

/** Explicit native test gate; -Dm3.bitmatrix.requireNative=true forbids assumption skips. */
final class M3MatBitMatrixTileJUnitTest {
    @Test
    void sourceSealedDenseAndSparseMatrixTransposeParity() throws Exception {
        boolean required = Boolean.getBoolean("m3.bitmatrix.requireNative");
        if (required) {
            assertTrue(M3MatBitMatrix.nativeAvailable(), "native matrix test was requested but cannot be skipped");
        } else {
            assumeTrue(M3MatBitMatrix.nativeAvailable(), "optional source-built matrix JNI library not loaded");
        }
        M3MatBitMatrixTileContract.runAll();
    }
}
