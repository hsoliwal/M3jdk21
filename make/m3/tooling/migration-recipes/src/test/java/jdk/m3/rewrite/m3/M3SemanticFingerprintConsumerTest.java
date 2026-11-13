// SPDX-License-Identifier: Apache-2.0
package jdk.m3.rewrite.synexia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class M3SemanticFingerprintConsumerTest {
    private static final String PLAIN = """
            private static int compute(int a, int b) {
                return (a + b) * 31;
            }
            """;

    @Test
    void unchangedOrdinarySourceRetainsLegacyFingerprintIdentity() {
        var legacy = jdk.m3.rewrite.semantic.M3SemanticHasher.method(PLAIN);
        var canonical = jdk.internal.m3.rewrite.semantic.M3SemanticHasher.method(PLAIN);

        assertEquals(legacy.contractHash(), canonical.contractHash());
        assertEquals(legacy.logicHash(), canonical.logicHash());
        assertEquals(legacy.architectureHash(), canonical.architectureHash());
        assertEquals(legacy.behavioralHash(), canonical.behavioralHash());
        assertEquals(legacy.wholeHash(), canonical.wholeHash());
    }

    @Test
    void realArchitectureCommentsRetainArchitectureIdentity() {
        String source = PLAIN.replace(
                "return (a + b) * 31;",
                "/* M3-IOP: PURE_INT_EXPRESSION */ return (a + b) * 31;");

        var legacy = jdk.m3.rewrite.semantic.M3SemanticHasher.method(source);
        var canonical = jdk.internal.m3.rewrite.semantic.M3SemanticHasher.method(source);

        assertEquals(legacy.contractHash(), canonical.contractHash());
        assertEquals(legacy.logicHash(), canonical.logicHash());
        assertEquals(legacy.architectureHash(), canonical.architectureHash());
        assertEquals(legacy.behavioralHash(), canonical.behavioralHash());
        assertEquals(legacy.wholeHash(), canonical.wholeHash());
    }

    @Test
    void canonicalFingerprintRejectsStringPayloadAsArchitectureAuthority() {
        String ordinary = """
                private static int compute(int a, int b) {
                    String payload = "ordinary";
                    return (a + b) * 31;
                }
                """;
        String fake = ordinary.replace(
                "\"ordinary\"",
                "\"M3-IOP: PURE_INT_EXPRESSION M3-ATOM: fake\"");

        var legacyOrdinary = jdk.m3.rewrite.semantic.M3SemanticHasher.method(ordinary);
        var legacyFake = jdk.m3.rewrite.semantic.M3SemanticHasher.method(fake);
        var canonicalOrdinary = jdk.internal.m3.rewrite.semantic.M3SemanticHasher.method(ordinary);
        var canonicalFake = jdk.internal.m3.rewrite.semantic.M3SemanticHasher.method(fake);

        assertNotEquals(
                legacyOrdinary.architectureHash(),
                legacyFake.architectureHash(),
                "legacy raw-source scan is retained only for historical receipts");
        assertEquals(
                canonicalOrdinary.architectureHash(),
                canonicalFake.architectureHash(),
                "string payload must not establish M3 architecture authority");
    }

    @Test
    void canonicalFingerprintRejectsTextBlockPayloadAsArchitectureAuthority() {
        String ordinary =
                "private static int compute(int a, int b) {\n"
                        + "    String payload = \"\"\"\n"
                        + "            ordinary\n"
                        + "            \"\"\";\n"
                        + "    return (a + b) * 31;\n"
                        + "}\n";
        String fake = ordinary.replace(
                "ordinary",
                "M3-IOP: PURE_INT_EXPRESSION M3-ATOM: fake");

        var canonicalOrdinary = jdk.internal.m3.rewrite.semantic.M3SemanticHasher.method(ordinary);
        var canonicalFake = jdk.internal.m3.rewrite.semantic.M3SemanticHasher.method(fake);

        assertEquals(
                canonicalOrdinary.architectureHash(),
                canonicalFake.architectureHash());
        assertNotEquals(
                canonicalOrdinary.logicHash(),
                canonicalFake.logicHash(),
                "text-block payload remains executable-source data for logic identity");
    }

    @Test
    void canonicalRecipeIsNonMutatingEvidenceOnly() {
        var recipe = new jdk.internal.m3.rewrite.semantic.M3SemanticHashRecipe();
        assertTrue(recipe.getTags().contains("semantic-hash"));
        assertTrue(recipe.getTags().contains("non-mutating"));
        assertTrue(recipe.getDescription().contains("without modifying source"));
    }
}
