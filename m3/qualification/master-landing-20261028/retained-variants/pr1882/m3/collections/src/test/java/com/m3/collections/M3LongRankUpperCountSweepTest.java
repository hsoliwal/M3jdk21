// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JDK 21/JNI scalar-vs-batch rank semantics; native availability remains opt-in. */
final class M3LongRankUpperCountSweepTest {
    @Test void upperBoundsAndCountsRespectChunkAndSignedContracts() {
        M3LongRankUpperCountSweepContract.contracts(false);
    }
}
