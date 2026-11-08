// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Exact original-owner multi-lane contract; separate standalone exhaustive proof is source-owned. */
class TokenCursorMultiLaneTest {
    @Test
    void multiLaneDenseAndSparseParity() {
        assertTrue(TokenCursorMultiLaneProof.verify(10_000) != 0L);
    }

    @Test
    void retainedInnerArraysAndIndependentOuterIndex() {
        int[] first = {1, 2};
        int[] last = {9};
        int[][] rows = {first, {}, last};
        TokenCursor cursor = TokenCursor.lanes(rows);
        rows[0] = new int[] {99};
        assertEquals(1, cursor.unitAt(0));
        first[0] = 7;
        assertEquals(7, cursor.unitAt(0));
        assertEquals(9, cursor.unitAt(2));
    }

    @Test
    void nullAndNegativeRefusals() {
        assertThrows(NullPointerException.class, () -> TokenCursor.lanes(null));
        assertThrows(NullPointerException.class,
                () -> TokenCursor.lanes(new int[][] {new int[0], null}));
        assertThrows(IllegalArgumentException.class,
                () -> TokenCursor.lanes(new int[][] {new int[] {1, -7}}));
    }
}
