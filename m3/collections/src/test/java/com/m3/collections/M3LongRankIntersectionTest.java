// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Original M3 long index LeetCode #350 Java and optional batch JNI contract. */
final class M3LongRankIntersectionTest {
    @Test void primitiveMultisetCardinality() {
        M3LongRankIntersectionContract.contracts(
                M3LongRankBatchNative.nativeAvailable());
    }
}
