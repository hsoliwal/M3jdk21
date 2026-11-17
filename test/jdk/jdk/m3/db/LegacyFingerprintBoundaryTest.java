// SPDX-License-Identifier: Apache-2.0
package jdk.m3.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import jdk.internal.m3.db.LegacyFingerprint;
import org.junit.jupiter.api.Test;

final class LegacyFingerprintBoundaryTest {
    private static final String ZERO = "0".repeat(64);

    @Test
    void constructorAndComponentRefuseMalformedIdentity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new LegacyFingerprint(
                        "bad", ZERO, ZERO, 0, 0, 0, "x"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new LegacyFingerprint(
                        ZERO, "bad", ZERO, 0, 0, 0, "x"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new LegacyFingerprint(
                        ZERO, ZERO, "bad", 0, 0, 0, "x"));
        assertThrows(
                NullPointerException.class,
                () -> new LegacyFingerprint(
                        ZERO, ZERO, ZERO, 0, 0, 0, null));

        var fingerprint = LegacyFingerprint.leaf(
                "ATOM", List.of(), List.of(), "x");
        for (String invalid : List.of("", " ", "A\0B")) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new LegacyFingerprint.Component(
                            invalid, fingerprint));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> LegacyFingerprint.leaf(
                            invalid, List.of(), List.of(), "x"));
        }
        assertThrows(
                NullPointerException.class,
                () -> new LegacyFingerprint.Component(
                        "ROLE", null));
    }

    @Test
    void leafRefusesNullCollectionsElementsAndSource() {
        assertThrows(
                NullPointerException.class,
                () -> LegacyFingerprint.leaf(
                        "D", null, List.of(), "x"));
        assertThrows(
                NullPointerException.class,
                () -> LegacyFingerprint.leaf(
                        "D", List.of(), null, "x"));
        assertThrows(
                NullPointerException.class,
                () -> LegacyFingerprint.leaf(
                        "D", Arrays.asList("s", null), List.of(), "x"));
        assertThrows(
                NullPointerException.class,
                () -> LegacyFingerprint.leaf(
                        "D", List.of(), Arrays.asList("l", null), "x"));
        assertThrows(
                NullPointerException.class,
                () -> LegacyFingerprint.leaf(
                        "D", List.of(), List.of(), null));
        assertThrows(
                NullPointerException.class,
                () -> LegacyFingerprint.utf16Sha256(null));
    }

    @Test
    void emptyAndPopulatedLeafPathsStayDistinctAndDeterministic() {
        var empty = LegacyFingerprint.leaf(
                "EMPTY", List.of(), List.of(), "");
        var populated = LegacyFingerprint.leaf(
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
                LegacyFingerprint.hammingDistance(
                        empty.simHash64(), populated.simHash64()));
    }

    @Test
    void compositionRefusesInvalidInputsAndPreservesOrder() {
        var a = LegacyFingerprint.leaf(
                "A", List.of("sa"), List.of("la"), "a");
        var b = LegacyFingerprint.leaf(
                "B", List.of("sb"), List.of("lb"), "b");

        assertThrows(
                NullPointerException.class,
                () -> LegacyFingerprint.compose(
                        null, List.of()));
        assertThrows(
                NullPointerException.class,
                () -> LegacyFingerprint.compose(
                        "P", null));
        assertThrows(
                NullPointerException.class,
                () -> LegacyFingerprint.compose(
                        "P",
                        Arrays.asList(
                                (LegacyFingerprint.Component) null)));

        var empty = LegacyFingerprint.compose(
                "P", List.of());
        var leftRight = LegacyFingerprint.compose(
                "P",
                List.of(
                        new LegacyFingerprint.Component("LEFT", a),
                        new LegacyFingerprint.Component("RIGHT", b)));
        var rightLeft = LegacyFingerprint.compose(
                "P",
                List.of(
                        new LegacyFingerprint.Component("RIGHT", b),
                        new LegacyFingerprint.Component("LEFT", a)));

        assertTrue(empty.normalizedComposition().startsWith("P|"));
        assertNotEquals(leftRight.logicSha256(), rightLeft.logicSha256());
        assertNotEquals(leftRight.structuralSha256(), rightLeft.structuralSha256());
    }
}
