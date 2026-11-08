// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;
import org.junit.jupiter.api.Test;
/** Prepared, sorted, signed-long offset windows under the JNI / Java contract. */
final class LongRankWindowIntersectionTest {
    @Test void distinctAndMultisetCardinality() {
        LongRankWindowIntersectionContract.contracts(
                LongRankBatchNative.nativeAvailable());
    }
}
