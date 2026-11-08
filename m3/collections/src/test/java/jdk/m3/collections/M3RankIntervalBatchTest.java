// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.RankIntervalBatchContract;

/** Original owner Java/JNI interval-batch JUnit adapter. */
final class M3RankIntervalBatchTest {
    @Test void halfOpenSignedLongJniAndJavaParity() throws Exception {
        RankIntervalBatchContract.main(new String[0]);
    }
}
