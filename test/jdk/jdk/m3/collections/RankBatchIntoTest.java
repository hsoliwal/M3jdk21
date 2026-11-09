// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.RankBatchIntoContract;

/** Real Java/JNI parity for LeetCode-category primitive count/upper-bound batches. */
final class RankBatchIntoTest {
    @Test
    void primitiveWindowsAndNativeParity() throws Exception {
        RankBatchIntoContract.main(new String[0]);
    }
}
