// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Original M3JDK21 JNI batch-oracle product test; JNI is opt-in, never mandatory. */
final class M3LongRankMonotoneSweepTest {
    @Test void signedMonotoneAndUnsortedBatchParity() {
        M3LongRankMonotoneSweepContract.contracts(false);
    }
}
