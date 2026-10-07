// SPDX-License-Identifier: Apache-2.0
package com.m3.indexdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3IndexDbFingerprintV1BoundaryTest {
    private static final String ZERO = "0".repeat(64);

    @Test
    void constructorAndComponentRefuseMalformedIdentity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new com.synexia.mindex.db.M3IndexDbFingerprintV1(
                        "bad", ZERO, ZERO, 0, 0, 0, "x"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new com.synexia.mindex.db.M3IndexDbFingerprintV1(
                        ZERO, "bad", ZERO, 0, 0, 0, "x"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new com.synexia.mindex.db.M3IndexDbFingerprintV1(
                        ZERO, ZERO, "bad", 0, 0, 0, "x"));
        assertThrows(
                NullPointerException.class,
                () -> new com.synexia.mindex.db.M3IndexDbFingerprintV1(
                        ZERO, ZERO, ZERO, 0, 0, 0, null));

        var fingerprint = com.synexia.mindex.db.M3IndexDbFingerprintV1.leaf(
                "ATOM", List.of(), List.of(), "x");
        for (String invalid : List.of("", " ", "A\0B")) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new com.synexia.mindex.db.M3IndexDbFingerprintV1.Component(
                            invalid, fingerprint));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> com.synexia.mindex.db.M3IndexDbFingerprintV1.leaf(
                            invalid, List.of(), List.of(), "x"));
        }
        assertThrows(
                NullPointerException.class,
                () -> new com.synexia.mindex.db.M3IndexDbFingerprintV1.Component(
                        "ROLE", null));
    }

    @Test
    void leafRefusesNullCollectionsElementsAndSource() {
        assertThrows(
                NullPointerException.class,
                () -> com.synexia.mindex.db.M3IndexDbFingerprintV1.leaf(
                        "D", null, List.of(), "x"));
        assertThrows(
                NullPointerException.class,
                () -> com.synexia.mindex.db.M3IndexDbFingerprintV1.leaf(
                        "D", List.of(), null, "x"));
        assertThrows(
                NullPointerException.class,
                () -> com.synexia.mindex.db.M3IndexDbFingerprintV1.leaf(
                        "D", Arrays.asList("s", null), List.of(), "x"));
        assertThrows(
                NullPointerException.class,
                () -> com.synexia.mindex.db.M3IndexDbFingerprintV1.leaf(
                        "D", List.of(), Arrays.asList("l", null), "x"));
        assertThrows(
                NullPointerException.class,
                () -> com.synexia.mindex.db.M3IndexDbFingerprintV1.leaf(
                        "D", List.of(), List.of(), null));
        assertThrows(
                NullPointerException.class,
                () -> com.synexia.mindex.db.M3IndexDbFingerprintV1.utf16Sha256(null));
    }

    @Test
    void emptyAndPopulatedLeafPathsStayDistinctAndDeterministic() {
        var empty = com.synexia.mindex.db.M3IndexDbFingerprintV1.leaf(
                "EMPTY", List.of(), List.of(), "");
        var populated = com.synexia.mindex.db.M3IndexDbFingerprintV1.leaf(
                "ATOM",
                List.of("Return", "Literal"),
                List.of("RETURN", "CONST:1"),
                "return 1;");

        assertEquals("EMPTY|", empty.normalizedComposition());
        assertEquals(64, empty.exactSha256().length());
        assertEquals(16, empty.structuralHash64Hex().length());
        assertEquals(16, empty.logicHash64Hex().length());
        assertEquals(16, empty.simHash64Hex().length());
        assertNotEquals(empty.logicSha256(), populated.logicSha256());
        assertEquals(
                Long.bitCount(empty.simHash64() ^ populated.simHash64()),
                com.synexia.mindex.db.M3IndexDbFingerprintV1.hammingDistance(
                        empty.simHash64(), populated.simHash64()));
    }

    @Test
    void compositionRefusesInvalidInputsAndPreservesOrder() {
        var a = com.synexia.mindex.db.M3IndexDbFingerprintV1.leaf(
                "A", List.of("sa"), List.of("la"), "a");
        var b = com.synexia.mindex.db.M3IndexDbFingerprintV1.leaf(
                "B", List.of("sb"), List.of("lb"), "b");

        assertThrows(
                NullPointerException.class,
                () -> com.synexia.mindex.db.M3IndexDbFingerprintV1.compose(
                        null, List.of()));
        assertThrows(
                NullPointerException.class,
                () -> com.synexia.mindex.db.M3IndexDbFingerprintV1.compose(
                        "P", null));
        assertThrows(
                NullPointerException.class,
                () -> com.synexia.mindex.db.M3IndexDbFingerprintV1.compose(
                        "P",
                        Arrays.asList(
                                (com.synexia.mindex.db.M3IndexDbFingerprintV1.Component) null)));

        var empty = com.synexia.mindex.db.M3IndexDbFingerprintV1.compose(
                "P", List.of());
        var leftRight = com.synexia.mindex.db.M3IndexDbFingerprintV1.compose(
                "P",
                List.of(
                        new com.synexia.mindex.db.M3IndexDbFingerprintV1.Component("LEFT", a),
                        new com.synexia.mindex.db.M3IndexDbFingerprintV1.Component("RIGHT", b)));
        var rightLeft = com.synexia.mindex.db.M3IndexDbFingerprintV1.compose(
                "P",
                List.of(
                        new com.synexia.mindex.db.M3IndexDbFingerprintV1.Component("RIGHT", b),
                        new com.synexia.mindex.db.M3IndexDbFingerprintV1.Component("LEFT", a)));

        assertTrue(empty.normalizedComposition().startsWith("P|"));
        assertNotEquals(leftRight.logicSha256(), rightLeft.logicSha256());
        assertNotEquals(leftRight.structuralSha256(), rightLeft.structuralSha256());
    }
}
