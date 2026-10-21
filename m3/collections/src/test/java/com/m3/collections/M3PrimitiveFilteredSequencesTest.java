/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Actual M3LongArrayList/Deque receiver integration, not a mocked backing. */
final class M3PrimitiveFilteredSequencesTest {
    @Test void fullPrimitiveFilteredSequenceContract() {
        M3PrimitiveFilteredSequencesRuntimeContract.main(new String[0]);
    }
}
