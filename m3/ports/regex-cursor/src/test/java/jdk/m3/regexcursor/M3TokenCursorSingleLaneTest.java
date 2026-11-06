// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/** JUnit/Surefire wrapper for the standalone deterministic constructor oracle. */
final class M3TokenCursorSingleLaneTest {
    @Test
    void preservesFullSingleLaneAndMultiLaneContracts() {
        long checksum = M3TokenCursorSingleLaneProof.verifyRandom(20_000);
        assertTrue(checksum != 0L);
    }

    @Test
    void emptyAndReservedZeroAreStillValidGenericTokenSymbols() {
        assertEquals(0, M3TokenCursor.of(new int[0]).totalLength());
        assertEquals(0, M3TokenCursor.of(new int[] {0}).unitAt(0));
        assertEquals(0, M3TokenCursor.lanes(new int[][] {{}, {}}).totalLength());
    }
}
