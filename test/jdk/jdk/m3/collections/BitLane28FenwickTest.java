// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/** JUnit adapter for the standalone deterministic primitive oracle. */
final class BitLane28FenwickTest {
    @Test
    void indexedRankSelectMatchesIndependentOrderedSetOracle() {
        BitLane28FenwickSelfTest.main(new String[0]);
    }

    @Test
    void repeatedWriteNoopAndInvalidBoundsPreserveBitsAndCounts() {
        BitLane28 lane = new BitLane28();
        lane.set(Address28.MAX_SLOT);
        lane.set(Address28.MAX_SLOT);
        lane.clear(0);
        assertEquals(1, lane.cardinality());
        assertEquals(0, lane.rank(Address28.MAX_SLOT));
        assertEquals(1, lane.rank(Address28.MAX_SLOTS));
        assertEquals(Address28.MAX_SLOT, lane.select(0));
        assertEquals(512L, lane.payloadBytes());
        assertEquals(2056L, lane.indexPayloadBytes());
        assertThrows(IndexOutOfBoundsException.class, () -> lane.cardinality(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> lane.cardinality(1, Address28.MAX_SLOTS + 1));
        lane.clear(Address28.MAX_SLOT);
        lane.clear(Address28.MAX_SLOT);
        assertEquals(0, lane.cardinality());
        assertEquals(-1, lane.select(0));
    }
}
