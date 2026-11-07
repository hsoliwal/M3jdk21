// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.M3RankBatchIntoContract;

/** Real Java/JNI parity for LeetCode-category primitive count/upper-bound batches. */
final class M3RankBatchIntoTest {
    @Test
    void primitiveWindowsAndNativeParity() throws Exception {
        M3RankBatchIntoContract.main(new String[0]);
    }
}
