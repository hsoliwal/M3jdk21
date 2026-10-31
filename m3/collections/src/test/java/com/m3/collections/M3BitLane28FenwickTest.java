// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/** JUnit adapter for the standalone deterministic primitive oracle. */
final class M3BitLane28FenwickTest {
    @Test
    void indexedRankSelectMatchesIndependentOrderedSetOracle() {
        M3BitLane28FenwickSelfTest.main(new String[0]);
    }

    @Test
    void repeatedWriteNoopAndInvalidBoundsPreserveBitsAndCounts() {
        M3BitLane28 lane = new M3BitLane28();
        lane.set(M3Address28.MAX_SLOT);
        lane.set(M3Address28.MAX_SLOT);
        lane.clear(0);
        assertEquals(1, lane.cardinality());
        assertEquals(0, lane.rank(M3Address28.MAX_SLOT));
        assertEquals(1, lane.rank(M3Address28.MAX_SLOTS));
        assertEquals(M3Address28.MAX_SLOT, lane.select(0));
        assertEquals(512L, lane.payloadBytes());
        assertEquals(2056L, lane.indexPayloadBytes());
        assertThrows(IndexOutOfBoundsException.class, () -> lane.cardinality(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> lane.cardinality(1, M3Address28.MAX_SLOTS + 1));
        lane.clear(M3Address28.MAX_SLOT);
        lane.clear(M3Address28.MAX_SLOT);
        assertEquals(0, lane.cardinality());
        assertEquals(-1, lane.select(0));
    }
}
