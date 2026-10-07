// SPDX-License-Identifier: Apache-2.0
package com.m3.indexdb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3IndexDbFingerprintV1ParityTest {
    @Test
    void canonicalMirrorMatchesHistoricalKnownAnswerVector() {
        var legacy = M3IndexDbSemanticFingerprint.leaf(
                "METHOD",
                List.of("private", "static", "int"),
                List.of("a+b", "return"),
                "return a+b;");
        var canonical = com.synexia.mindex.db.M3IndexDbFingerprintV1.leaf(
                "METHOD",
                List.of("private", "static", "int"),
                List.of("a+b", "return"),
                "return a+b;");

        same(legacy, canonical);
        assertEquals(
                "bc38ecd30e18d4aea0dcf50c0eb39c6f713375363378d2c4cb66b287dfeb6267",
                canonical.exactSha256());
        assertEquals(
                "faffec9b3a256c6b8f88e7143b7e09fa309b619332f7ffce36f447d69f79b297",
                canonical.structuralSha256());
        assertEquals(
                "171e1c08919ad8a4953ac8d2efb3f328a99856e21b23b546dff3f5cfa0592ee8",
                canonical.logicSha256());
        assertEquals("f7540c3cb5f5cd1d", canonical.structuralHash64Hex());
        assertEquals("705f301e930d6b49", canonical.logicHash64Hex());
        assertEquals("b7fff9efbfdfffb3", canonical.simHash64Hex());
        assertEquals("METHOD|a+b|return", canonical.normalizedComposition());
    }

    @Test
    void canonicalMirrorMatchesHistoricalGeneratedLeafCorpus() {
        for (int index = 0; index < 128; index++) {
            String domain = "DOMAIN-" + index;
            List<String> structure = List.of(
                    "kind=" + (index % 7),
                    "arity=" + (index % 11),
                    "shape=" + Integer.toHexString(index * 31 + 7));
            List<String> logic = switch (index % 4) {
                case 0 -> List.of();
                case 1 -> List.of("op=add", "constant=" + index);
                case 2 -> List.of("op=xor", "shift=" + (index & 31));
                default -> List.of("op=mix", "left=" + index, "right=" + ~index);
            };
            String exact = "source[" + index + "]::\u03a9::" + (index * index);

            var legacy = M3IndexDbSemanticFingerprint.leaf(domain, structure, logic, exact);
            var canonical = com.synexia.mindex.db.M3IndexDbFingerprintV1.leaf(
                    domain, structure, logic, exact);
            same(legacy, canonical);
        }
    }

    @Test
    void canonicalMirrorMatchesHistoricalOrderedCompositionCorpus() {
        List<M3IndexDbSemanticFingerprint> oldLeaves = new ArrayList<>();
        List<com.synexia.mindex.db.M3IndexDbFingerprintV1> newLeaves = new ArrayList<>();

        for (int index = 0; index < 16; index++) {
            oldLeaves.add(M3IndexDbSemanticFingerprint.leaf(
                    "LEAF-" + index,
                    List.of("s" + index, "bucket" + (index % 3)),
                    List.of("l" + index),
                    "exact-" + index));
            newLeaves.add(com.synexia.mindex.db.M3IndexDbFingerprintV1.leaf(
                    "LEAF-" + index,
                    List.of("s" + index, "bucket" + (index % 3)),
                    List.of("l" + index),
                    "exact-" + index));
        }

        for (int width = 0; width <= oldLeaves.size(); width++) {
            List<M3IndexDbSemanticFingerprint.Component> oldComponents = new ArrayList<>();
            List<com.synexia.mindex.db.M3IndexDbFingerprintV1.Component> newComponents =
                    new ArrayList<>();
            for (int index = 0; index < width; index++) {
                String role = "ROLE-" + (index % 5);
                oldComponents.add(new M3IndexDbSemanticFingerprint.Component(
                        role, oldLeaves.get(index)));
                newComponents.add(new com.synexia.mindex.db.M3IndexDbFingerprintV1.Component(
                        role, newLeaves.get(index)));
            }

            var legacy = M3IndexDbSemanticFingerprint.compose(
                    "PARENT-" + width, oldComponents);
            var canonical = com.synexia.mindex.db.M3IndexDbFingerprintV1.compose(
                    "PARENT-" + width, newComponents);
            same(legacy, canonical);
        }
    }

    @Test
    void hammingDistanceAndUtf16ShaRemainByteCompatible() {
        for (int index = 0; index < 64; index++) {
            long left = Long.rotateLeft(0x0123456789abcdefL, index);
            long right = Long.rotateRight(0xfedcba9876543210L, index);
            assertEquals(
                    M3IndexDbSemanticFingerprint.hammingDistance(left, right),
                    com.synexia.mindex.db.M3IndexDbFingerprintV1.hammingDistance(left, right));
        }

        for (String value : List.of(
                "",
                "ascii",
                "Ω",
                "😀",
                "a\u0000b",
                "line1\nline2",
                "𝄞music")) {
            assertEquals(
                    M3IndexDbSemanticFingerprint.utf16Sha256(value),
                    com.synexia.mindex.db.M3IndexDbFingerprintV1.utf16Sha256(value));
        }
    }

    private static void same(
            M3IndexDbSemanticFingerprint legacy,
            com.synexia.mindex.db.M3IndexDbFingerprintV1 canonical) {
        assertEquals(legacy.exactSha256(), canonical.exactSha256());
        assertEquals(legacy.structuralSha256(), canonical.structuralSha256());
        assertEquals(legacy.logicSha256(), canonical.logicSha256());
        assertEquals(legacy.structuralHash64(), canonical.structuralHash64());
        assertEquals(legacy.logicHash64(), canonical.logicHash64());
        assertEquals(legacy.simHash64(), canonical.simHash64());
        assertEquals(legacy.normalizedComposition(), canonical.normalizedComposition());
        assertEquals(legacy.structuralHash64Hex(), canonical.structuralHash64Hex());
        assertEquals(legacy.logicHash64Hex(), canonical.logicHash64Hex());
        assertEquals(legacy.simHash64Hex(), canonical.simHash64Hex());
    }
}
