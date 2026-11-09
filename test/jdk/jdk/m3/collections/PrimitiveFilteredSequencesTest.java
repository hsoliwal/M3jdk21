/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Actual LongArrayList/Deque receiver integration, not a mocked backing. */
final class PrimitiveFilteredSequencesTest {
    @Test void fullPrimitiveFilteredSequenceContract() {
        PrimitiveFilteredSequencesRuntimeContract.main(new String[0]);
    }
}
