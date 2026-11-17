// SPDX-License-Identifier: Apache-2.0
package jdk.m3.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import jdk.internal.m3.db.LegacyFingerprint;
import org.junit.jupiter.api.Test;

final class LegacyFingerprintParityTest {
    @Test
    void canonicalMirrorMatchesHistoricalKnownAnswerVector() {
        var legacy = SemanticFingerprint.leaf(
                "METHOD",
                List.of("private", "static", "int"),
                List.of("a+b", "return"),
                "return a+b;");
        var canonical = LegacyFingerprint.leaf(
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

            var legacy = SemanticFingerprint.leaf(domain, structure, logic, exact);
            var canonical = LegacyFingerprint.leaf(
                    domain, structure, logic, exact);
            same(legacy, canonical);
        }
    }

    @Test
    void canonicalMirrorMatchesHistoricalOrderedCompositionCorpus() {
        List<SemanticFingerprint> oldLeaves = new ArrayList<>();
        List<LegacyFingerprint> newLeaves = new ArrayList<>();

        for (int index = 0; index < 16; index++) {
            oldLeaves.add(SemanticFingerprint.leaf(
                    "LEAF-" + index,
                    List.of("s" + index, "bucket" + (index % 3)),
                    List.of("l" + index),
                    "exact-" + index));
            newLeaves.add(LegacyFingerprint.leaf(
                    "LEAF-" + index,
                    List.of("s" + index, "bucket" + (index % 3)),
                    List.of("l" + index),
                    "exact-" + index));
        }

        for (int width = 0; width <= oldLeaves.size(); width++) {
            List<SemanticFingerprint.Component> oldComponents = new ArrayList<>();
            List<LegacyFingerprint.Component> newComponents =
                    new ArrayList<>();
            for (int index = 0; index < width; index++) {
                String role = "ROLE-" + (index % 5);
                oldComponents.add(new SemanticFingerprint.Component(
                        role, oldLeaves.get(index)));
                newComponents.add(new LegacyFingerprint.Component(
                        role, newLeaves.get(index)));
            }

            var legacy = SemanticFingerprint.compose(
                    "PARENT-" + width, oldComponents);
            var canonical = LegacyFingerprint.compose(
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
                    SemanticFingerprint.hammingDistance(left, right),
                    LegacyFingerprint.hammingDistance(left, right));
        }

        for (String value : List.of(
                "",
                "ascii",
                "Ω",
                "😀",
                "a" + (char) 0 + "b",
                "line1\nline2",
                "𝄞music")) {
            assertEquals(
                    SemanticFingerprint.utf16Sha256(value),
                    LegacyFingerprint.utf16Sha256(value));
        }
    }

    private static void same(
            SemanticFingerprint legacy,
            LegacyFingerprint canonical) {
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
