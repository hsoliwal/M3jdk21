// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.scope.M3EditScope;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3BackportPacketEvidenceTest {

    @Test
    void strictManifestCompletesPacketAndHasStableContentAddress() {
        M3BackportPacket packet = packet();
        M3BackportPacketEvidence evidence =
                M3BackportPacketEvidenceLoader.parse(evidenceTsv());

        evidence.requireComplete(packet);
        assertEquals("jdk-x", evidence.packetId());
        assertEquals(3, evidence.atoms().size());
        assertEquals("Strategy", evidence.require("java-a").pattern());
        assertEquals(
                List.of("java-a", "text-b", "package-join"),
                packet.atoms().stream().map(M3RecipeAtom::id).toList());

        String first = M3AtomEvidenceRoot.of(packet, evidence);
        String second =
                M3AtomEvidenceRoot.of(
                        packet(),
                        M3BackportPacketEvidenceLoader.parse(evidenceTsv()));
        assertTrue(first.matches("[0-9a-f]{64}"));
        assertEquals(first, second);

        String changed =
                M3AtomEvidenceRoot.of(
                        packet(),
                        M3BackportPacketEvidenceLoader.parse(
                                evidenceTsv().replace("Strategy.Java", "Strategy.JavaOracle")));
        assertNotEquals(first, changed);
    }

    @Test
    void evidenceRowsFailClosedOnInvalidSemanticFields() {
        assertThrows(
                IllegalArgumentException.class,
                () -> evidence("Bad_Id", "contract", "doc", "Pattern", "Role", "Test", true));
        assertThrows(
                IllegalArgumentException.class,
                () -> evidence("a", "", "doc", "Pattern", "Role", "Test", true));
        assertThrows(
                IllegalArgumentException.class,
                () -> evidence("a", "contract", " ", "Pattern", "Role", "Test", true));
        assertThrows(
                IllegalArgumentException.class,
                () -> evidence("a", "contract", "doc", "Pattern\tBad", "Role", "Test", true));
        assertThrows(
                IllegalArgumentException.class,
                () -> evidence("a", "contract", "doc", "Pattern", "Role\nBad", "Test", true));
        assertThrows(
                IllegalArgumentException.class,
                () -> evidence("a", "contract", "doc", "Pattern", "Role", "Test\rBad", true));
        assertThrows(
                IllegalArgumentException.class,
                () -> evidence("a", "contract", "doc\0bad", "Pattern", "Role", "Test", true));
        assertThrows(
                IllegalArgumentException.class,
                () -> evidence("a", "contract", "doc", "Pattern", "Role", "Test", false));
    }

    @Test
    void evidencePacketRejectsDuplicatesBadBudgetsAndUnknownRows() {
        M3RecipeAtomEvidence a =
                evidence("a", "contract", "doc", "Pattern", "Role", "Test", true);
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3BackportPacketEvidence("Bad_Id", List.of(a)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3BackportPacketEvidence("p", List.of()));
        ArrayList<M3RecipeAtomEvidence> tooMany = new ArrayList<>();
        for (int index = 0; index < 257; index++) {
            tooMany.add(
                    evidence(
                            "a" + index,
                            "contract",
                            "doc",
                            "Pattern",
                            "Role",
                            "Test",
                            true));
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3BackportPacketEvidence("p", tooMany));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3BackportPacketEvidence("p", List.of(a, a)));

        M3BackportPacketEvidence one =
                new M3BackportPacketEvidence("p", List.of(a));
        assertEquals(a, one.require("a"));
        assertThrows(IllegalArgumentException.class, () -> one.require("missing"));
    }

    @Test
    void completenessRejectsPacketMismatchMissingAndExtraEvidence() {
        M3BackportPacket packet = packet();
        M3BackportPacketEvidence complete =
                M3BackportPacketEvidenceLoader.parse(evidenceTsv());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new M3BackportPacketEvidence(
                                        "other",
                                        complete.atoms())
                                .requireComplete(packet));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new M3BackportPacketEvidence(
                                        "jdk-x",
                                        complete.atoms().subList(0, 2))
                                .requireComplete(packet));

        ArrayList<M3RecipeAtomEvidence> extra = new ArrayList<>(complete.atoms());
        extra.add(
                evidence(
                        "extra",
                        "contract.extra",
                        "docs/extra.md",
                        "Pattern",
                        "Role",
                        "ExtraTest",
                        true));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3BackportPacketEvidence("jdk-x", extra).requireComplete(packet));
    }

    @Test
    void loaderRejectsMalformedRowsMixedIdsAndInvalidBooleans() {
        assertThrows(
                IllegalArgumentException.class,
                () -> M3BackportPacketEvidenceLoader.parse(""));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3BackportPacketEvidenceLoader.parse("wrong\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3BackportPacketEvidenceLoader.parse(
                        M3BackportPacketEvidenceLoader.HEADER + "\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3BackportPacketEvidenceLoader.parse(
                                M3BackportPacketEvidenceLoader.HEADER
                                        + "\np\ta\tcontract\tdoc\tPattern\tRole\tTest\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3BackportPacketEvidenceLoader.parse(
                                M3BackportPacketEvidenceLoader.HEADER
                                        + "\np\ta\tcontract\tdoc\tPattern\tRole\tTest\tmaybe\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3BackportPacketEvidenceLoader.parse(
                                M3BackportPacketEvidenceLoader.HEADER
                                        + "\np\ta\tcontract\tdoc\tPattern\tRole\tTest\ttrue\n"
                                        + "q\tb\tcontract\tdoc\tPattern\tRole\tTest\ttrue\n"));
    }

    @Test
    void commentsAndBlankRowsDoNotChangeEvidence() {
        M3BackportPacketEvidence evidence =
                M3BackportPacketEvidenceLoader.parse(
                        M3BackportPacketEvidenceLoader.HEADER
                                + "\n# reviewed evidence\n\n"
                                + "p\ta\tcontract\tdocs/a.md\tStrategy\tJavaOracle\tATest\ttrue\n");
        assertEquals("p", evidence.packetId());
        assertEquals(1, evidence.atoms().size());
        assertTrue(evidence.require("a").fixedPointRequired());
    }

    private static M3BackportPacket packet() {
        return new M3BackportPacket(
                "jdk-x",
                List.of(
                        new M3RecipeAtom(
                                "java-a",
                                M3EditScope.FILE,
                                false,
                                "recipe.Java",
                                List.of()),
                        new M3RecipeAtom(
                                "text-b",
                                M3EditScope.FILE,
                                false,
                                "recipe.Text",
                                List.of()),
                        new M3RecipeAtom(
                                "package-join",
                                M3EditScope.PACKAGE,
                                true,
                                "recipe.Join",
                                List.of("java-a", "text-b"))));
    }

    private static String evidenceTsv() {
        return M3BackportPacketEvidenceLoader.HEADER
                + "\n"
                + "jdk-x\tjava-a\tcontract/java\tdocs/java.md\tStrategy\tStrategy.Java\tJavaRecipeTest\ttrue\n"
                + "jdk-x\ttext-b\tcontract/text\tdocs/text.md\tAdapter\tAdapter.Text\tTextRecipeTest\ttrue\n"
                + "jdk-x\tpackage-join\tcontract/package\tdocs/package.md\tComposite\tDag.Join\tJoinRecipeTest\ttrue\n";
    }

    private static M3RecipeAtomEvidence evidence(
            String id,
            String contract,
            String documentation,
            String pattern,
            String role,
            String junit,
            boolean fixedPoint) {
        return new M3RecipeAtomEvidence(
                id,
                contract,
                documentation,
                pattern,
                role,
                junit,
                fixedPoint);
    }
}
