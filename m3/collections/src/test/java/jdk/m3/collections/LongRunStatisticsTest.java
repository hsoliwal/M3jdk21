// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.LongRunStatisticsContract;

/** Original owning M3JDK 21 JUnit discovery for primitive rank-run JNI parity. */
final class LongRunStatisticsTest {
    @Test
    void signedLongDistinctAndConsecutiveRuns() {
        LongRunStatisticsContract.contracts(false);
    }
}
