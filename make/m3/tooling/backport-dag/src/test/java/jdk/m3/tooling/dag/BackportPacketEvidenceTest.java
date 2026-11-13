// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.m3.rewrite.scope.M3EditScope;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class BackportPacketEvidenceTest {

    @Test
    void strictManifestCompletesPacketAndHasStableContentAddress() {
        BackportPacket packet = packet();
        BackportPacketEvidence evidence =
                BackportPacketEvidenceLoader.parse(evidenceTsv());

        evidence.requireComplete(packet);
        assertEquals("jdk-x", evidence.packetId());
        assertEquals(3, evidence.atoms().size());
        assertEquals("Strategy", evidence.require("java-a").pattern());
        assertEquals(
                List.of("java-a", "text-b", "package-join"),
                packet.atoms().stream().map(RecipeAtom::id).toList());

        String first = AtomEvidenceRoot.of(packet, evidence);
        String second =
                AtomEvidenceRoot.of(
                        packet(),
                        BackportPacketEvidenceLoader.parse(evidenceTsv()));
        assertTrue(first.matches("[0-9a-f]{64}"));
        assertEquals(first, second);

        String changed =
                AtomEvidenceRoot.of(
                        packet(),
                        BackportPacketEvidenceLoader.parse(
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
        RecipeAtomEvidence a =
                evidence("a", "contract", "doc", "Pattern", "Role", "Test", true);
        assertThrows(
                IllegalArgumentException.class,
                () -> new BackportPacketEvidence("Bad_Id", List.of(a)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new BackportPacketEvidence("p", List.of()));
        ArrayList<RecipeAtomEvidence> tooMany = new ArrayList<>();
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
                () -> new BackportPacketEvidence("p", tooMany));
        assertThrows(
                IllegalArgumentException.class,
                () -> new BackportPacketEvidence("p", List.of(a, a)));

        BackportPacketEvidence one =
                new BackportPacketEvidence("p", List.of(a));
        assertEquals(a, one.require("a"));
        assertThrows(IllegalArgumentException.class, () -> one.require("missing"));
    }

    @Test
    void completenessRejectsPacketMismatchMissingAndExtraEvidence() {
        BackportPacket packet = packet();
        BackportPacketEvidence complete =
                BackportPacketEvidenceLoader.parse(evidenceTsv());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new BackportPacketEvidence(
                                        "other",
                                        complete.atoms())
                                .requireComplete(packet));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new BackportPacketEvidence(
                                        "jdk-x",
                                        complete.atoms().subList(0, 2))
                                .requireComplete(packet));

        ArrayList<RecipeAtomEvidence> extra = new ArrayList<>(complete.atoms());
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
                () -> new BackportPacketEvidence("jdk-x", extra).requireComplete(packet));
    }

    @Test
    void loaderRejectsMalformedRowsMixedIdsAndInvalidBooleans() {
        assertThrows(
                IllegalArgumentException.class,
                () -> BackportPacketEvidenceLoader.parse(""));
        assertThrows(
                IllegalArgumentException.class,
                () -> BackportPacketEvidenceLoader.parse("wrong\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> BackportPacketEvidenceLoader.parse(
                        BackportPacketEvidenceLoader.HEADER + "\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BackportPacketEvidenceLoader.parse(
                                BackportPacketEvidenceLoader.HEADER
                                        + "\np\ta\tcontract\tdoc\tPattern\tRole\tTest\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BackportPacketEvidenceLoader.parse(
                                BackportPacketEvidenceLoader.HEADER
                                        + "\np\ta\tcontract\tdoc\tPattern\tRole\tTest\tmaybe\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BackportPacketEvidenceLoader.parse(
                                BackportPacketEvidenceLoader.HEADER
                                        + "\np\ta\tcontract\tdoc\tPattern\tRole\tTest\ttrue\n"
                                        + "q\tb\tcontract\tdoc\tPattern\tRole\tTest\ttrue\n"));
    }

    @Test
    void commentsAndBlankRowsDoNotChangeEvidence() {
        BackportPacketEvidence evidence =
                BackportPacketEvidenceLoader.parse(
                        BackportPacketEvidenceLoader.HEADER
                                + "\n# reviewed evidence\n\n"
                                + "p\ta\tcontract\tdocs/a.md\tStrategy\tJavaOracle\tATest\ttrue\n");
        assertEquals("p", evidence.packetId());
        assertEquals(1, evidence.atoms().size());
        assertTrue(evidence.require("a").fixedPointRequired());
    }

    private static BackportPacket packet() {
        return new BackportPacket(
                "jdk-x",
                List.of(
                        new RecipeAtom(
                                "java-a",
                                M3EditScope.FILE,
                                false,
                                "recipe.Java",
                                List.of()),
                        new RecipeAtom(
                                "text-b",
                                M3EditScope.FILE,
                                false,
                                "recipe.Text",
                                List.of()),
                        new RecipeAtom(
                                "package-join",
                                M3EditScope.PACKAGE,
                                true,
                                "recipe.Join",
                                List.of("java-a", "text-b"))));
    }

    private static String evidenceTsv() {
        return BackportPacketEvidenceLoader.HEADER
                + "\n"
                + "jdk-x\tjava-a\tcontract/java\tdocs/java.md\tStrategy\tStrategy.Java\tJavaRecipeTest\ttrue\n"
                + "jdk-x\ttext-b\tcontract/text\tdocs/text.md\tAdapter\tAdapter.Text\tTextRecipeTest\ttrue\n"
                + "jdk-x\tpackage-join\tcontract/package\tdocs/package.md\tComposite\tDag.Join\tJoinRecipeTest\ttrue\n";
    }

    private static RecipeAtomEvidence evidence(
            String id,
            String contract,
            String documentation,
            String pattern,
            String role,
            String junit,
            boolean fixedPoint) {
        return new RecipeAtomEvidence(
                id,
                contract,
                documentation,
                pattern,
                role,
                junit,
                fixedPoint);
    }
}
