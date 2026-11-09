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
class Census3BitsTest {

    @Test
    void numericIsIntegerOrDecimal() {
        assertEquals(StringTypeBits.INTEGER | StringTypeBits.DECIMAL, StringTypeBits.NUMERIC);
        assertEquals(48, StringTypeBits.NUMERIC);
        assertTrue(StringTypeBits.hasAny(StringTypeBits.DECIMAL, StringTypeBits.NUMERIC));
        assertFalse(StringTypeBits.has(StringTypeBits.DECIMAL, StringTypeBits.NUMERIC));
        assertTrue(StringTypeBits.has(StringTypeBits.NUMERIC | StringTypeBits.ASCII, StringTypeBits.NUMERIC));
    }

    @Test
    void stringTypeNamesFollowBitOrder() {
        assertEquals(List.of(), StringTypeBits.names(0));
        assertEquals(List.of("EMPTY", "INTEGER", "DECIMAL", "OTHER"),
                StringTypeBits.names(StringTypeBits.EMPTY | StringTypeBits.NUMERIC | StringTypeBits.OTHER));
        assertEquals(13, StringTypeBits.names(StringTypeBits.KNOWN_MASK).size());
        assertThrows(UnsupportedOperationException.class, () -> StringTypeBits.names(1).add("X"));
    }

    @Test
    void stringTypeRejectsUnknownBits() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> StringTypeBits.names(1 << 13));
        assertEquals("unknown M3String type bits: 8192", failure.getMessage());
        assertThrows(IllegalArgumentException.class, () -> StringTypeBits.has(0, -1));
        assertThrows(IllegalArgumentException.class, () -> StringTypeBits.hasAny(0, 1 << 20));
    }

    @Test
    void argumentRoleNamesAndChecks() {
        int bits = ArgumentRoleBits.CLAIM | ArgumentRoleBits.RULE | ArgumentRoleBits.OTHER;
        assertEquals(List.of("CLAIM", "RULE", "OTHER"), ArgumentRoleBits.names(bits));
        assertEquals(11, ArgumentRoleBits.names(ArgumentRoleBits.KNOWN_MASK).size());
        assertTrue(ArgumentRoleBits.has(bits, ArgumentRoleBits.CLAIM | ArgumentRoleBits.RULE));
        assertFalse(ArgumentRoleBits.has(bits, ArgumentRoleBits.FACT | ArgumentRoleBits.RULE));
        assertTrue(ArgumentRoleBits.hasAny(bits, ArgumentRoleBits.FACT | ArgumentRoleBits.RULE));
        ArgumentRoleBits.requireKnown(ArgumentRoleBits.KNOWN_MASK);
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> ArgumentRoleBits.requireKnown(1 << 11));
        assertEquals("unknown argument role bits: 2048", failure.getMessage());
    }
}
