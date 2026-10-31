// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import org.junit.jupiter.api.Test;
/** Prepared, sorted, signed-long offset windows under the JNI / Java contract. */
final class M3LongRankWindowIntersectionTest {
    @Test void distinctAndMultisetCardinality() {
        M3LongRankWindowIntersectionContract.contracts(
                M3LongRankBatchNative.nativeAvailable());
    }
}
