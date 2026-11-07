// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Census pass 3 leaf P3-007: M3StringTypeBits.NUMERIC, the private requireKnown/add helpers of both bit sets and the
 * received atoms that reach them (donor com.synexia develop 78cf2ad4c7e5). Each test fails on master: NUMERIC does
 * not compile there and every other member throws M3NotYetReceived.
 */
//FLLDCIM: MRL - census3 P3-007 restored-member tests (qualification/census3-gaps-20261107)
class M3Census3BitsTest {

    @Test
    void numericIsIntegerOrDecimal() {
        assertEquals(M3StringTypeBits.INTEGER | M3StringTypeBits.DECIMAL, M3StringTypeBits.NUMERIC);
        assertEquals(48, M3StringTypeBits.NUMERIC);
        assertTrue(M3StringTypeBits.hasAny(M3StringTypeBits.DECIMAL, M3StringTypeBits.NUMERIC));
        assertFalse(M3StringTypeBits.has(M3StringTypeBits.DECIMAL, M3StringTypeBits.NUMERIC));
        assertTrue(M3StringTypeBits.has(M3StringTypeBits.NUMERIC | M3StringTypeBits.ASCII, M3StringTypeBits.NUMERIC));
    }

    @Test
    void stringTypeNamesFollowBitOrder() {
        assertEquals(List.of(), M3StringTypeBits.names(0));
        assertEquals(List.of("EMPTY", "INTEGER", "DECIMAL", "OTHER"),
                M3StringTypeBits.names(M3StringTypeBits.EMPTY | M3StringTypeBits.NUMERIC | M3StringTypeBits.OTHER));
        assertEquals(13, M3StringTypeBits.names(M3StringTypeBits.KNOWN_MASK).size());
        assertThrows(UnsupportedOperationException.class, () -> M3StringTypeBits.names(1).add("X"));
    }

    @Test
    void stringTypeRejectsUnknownBits() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> M3StringTypeBits.names(1 << 13));
        assertEquals("unknown M3String type bits: 8192", failure.getMessage());
        assertThrows(IllegalArgumentException.class, () -> M3StringTypeBits.has(0, -1));
        assertThrows(IllegalArgumentException.class, () -> M3StringTypeBits.hasAny(0, 1 << 20));
    }

    @Test
    void argumentRoleNamesAndChecks() {
        int bits = M3ArgumentRoleBits.CLAIM | M3ArgumentRoleBits.RULE | M3ArgumentRoleBits.OTHER;
        assertEquals(List.of("CLAIM", "RULE", "OTHER"), M3ArgumentRoleBits.names(bits));
        assertEquals(11, M3ArgumentRoleBits.names(M3ArgumentRoleBits.KNOWN_MASK).size());
        assertTrue(M3ArgumentRoleBits.has(bits, M3ArgumentRoleBits.CLAIM | M3ArgumentRoleBits.RULE));
        assertFalse(M3ArgumentRoleBits.has(bits, M3ArgumentRoleBits.FACT | M3ArgumentRoleBits.RULE));
        assertTrue(M3ArgumentRoleBits.hasAny(bits, M3ArgumentRoleBits.FACT | M3ArgumentRoleBits.RULE));
        M3ArgumentRoleBits.requireKnown(M3ArgumentRoleBits.KNOWN_MASK);
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> M3ArgumentRoleBits.requireKnown(1 << 11));
        assertEquals("unknown argument role bits: 2048", failure.getMessage());
    }
}
