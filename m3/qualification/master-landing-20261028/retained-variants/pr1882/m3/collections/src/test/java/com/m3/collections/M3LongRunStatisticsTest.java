// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Original owning M3JDK21 JUnit discovery for primitive rank-run JNI parity. */
final class M3LongRunStatisticsTest {
    @Test
    void signedLongDistinctAndConsecutiveRuns() {
        M3LongRunStatisticsContract.contracts(false);
    }
}
