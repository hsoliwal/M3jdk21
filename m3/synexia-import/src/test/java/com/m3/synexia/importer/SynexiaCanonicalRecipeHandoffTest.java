// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class SynexiaCanonicalRecipeHandoffTest {
    private static final String HANDOFF =
            "m3/synexia-import/current-recipe-handoff/20261011-canonical-recipe-handoff.tsv";
    private static final String REVISION =
            "25ccf1bea2a07e0b59f00b91d933f10785eedd5d";

    @Test
    void checkedInHandoffPinsCurrentCanonicalSynexiaRecipeAssets() throws Exception {
        Path repository = repositoryRoot();
        SynexiaCanonicalRecipeHandoff handoff =
                SynexiaCanonicalRecipeHandoff.parse(
                        Files.readString(repository.resolve(HANDOFF)));

        assertEquals(SynexiaCanonicalRecipeHandoff.SOURCE_REPOSITORY, handoff.sourceRepository());
        assertEquals(REVISION, handoff.sourceRevision());
        assertEquals(13, handoff.entries().size());
        assertFalse(handoff.materializationAuthority());
        assertFalse(handoff.targetPromotionAuthority());

        assertTrue(handoff.entries().stream()
                .allMatch(entry -> entry.sourceGitBlob().matches("[0-9a-f]{40}")));
        assertTrue(handoff.entries().stream()
                .allMatch(entry -> entry.license().equals("Apache-2.0")));
        assertTrue(handoff.entries().stream()
                .noneMatch(SynexiaCanonicalRecipeHandoff.Entry::reusableEvolutionAllowedInTarget));

        long recipes = handoff.entries().stream()
                .filter(entry -> entry.assetKind()
                        == SynexiaCanonicalRecipeHandoff.AssetKind.OPENREWRITE_RECIPE)
                .count();
        assertEquals(7L, recipes);
        assertTrue(handoff.entries().stream()
                .filter(entry -> entry.assetKind()
                        == SynexiaCanonicalRecipeHandoff.AssetKind.OPENREWRITE_RECIPE)
                .allMatch(entry -> entry.canonicalOwner().startsWith("com.synexia.rewrite.")));
        assertTrue(handoff.entries().stream()
                .filter(entry -> entry.assetKind()
                        == SynexiaCanonicalRecipeHandoff.AssetKind.OPENREWRITE_RECIPE)
                .allMatch(entry -> entry.targetDisposition()
                        == SynexiaCanonicalRecipeHandoff.TargetDisposition.THIN_RECEIVER_ONLY));

        assertTrue(handoff.entries().stream().anyMatch(entry ->
                entry.sourcePath().equals(".m3/m3-recipe-first-invariants.tsv")
                        && entry.assetKind()
                                == SynexiaCanonicalRecipeHandoff.AssetKind.MACHINE_LEDGER));
        assertTrue(handoff.entries().stream().anyMatch(entry ->
                entry.canonicalOwner().equals(
                        "com.synexia.rewrite.M3AtomHashDatabaseRecipe")));
        assertTrue(handoff.entries().stream().anyMatch(entry ->
                entry.canonicalOwner().equals(
                        "com.synexia.rewrite.M3VerbatimJavaBaselineInventoryRecipe")));
    }

    @Test
    void parserFailsClosedOnRepositoryRevisionLicenseOwnerAndDuplicates() {
        String header = SynexiaCanonicalRecipeHandoff.HEADER + "\n";
        String row =
                SynexiaCanonicalRecipeHandoff.SCHEMA
                        + "\t"
                        + SynexiaCanonicalRecipeHandoff.SOURCE_REPOSITORY
                        + "\t"
                        + REVISION
                        + "\tsynexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/A.java"
                        + "\t"
                        + "a".repeat(40)
                        + "\tOPENREWRITE_RECIPE"
                        + "\tcom.synexia.rewrite.A"
                        + "\tTHIN_RECEIVER_ONLY"
                        + "\tApache-2.0\n";

        SynexiaCanonicalRecipeHandoff parsed =
                SynexiaCanonicalRecipeHandoff.parse(header + row);
        assertEquals(1, parsed.entries().size());

        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaCanonicalRecipeHandoff.parse(
                        "bad\n" + row));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaCanonicalRecipeHandoff.parse(header));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaCanonicalRecipeHandoff.parse(
                        header + row.replace("hsoliwal/com.synexia", "other/repo")));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaCanonicalRecipeHandoff.parse(
                        header + row.replace("Apache-2.0", "MIT")));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaCanonicalRecipeHandoff.parse(
                        header + row.replace("com.synexia.rewrite.A", "com.m3.rewrite.A")));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaCanonicalRecipeHandoff.parse(header + row + row));
    }

    @Test
    void entryAndReceiptContractsGrantNoTargetEvolutionAuthority() {
        var entry =
                new SynexiaCanonicalRecipeHandoff.Entry(
                        "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/A.java",
                        "b".repeat(40),
                        SynexiaCanonicalRecipeHandoff.AssetKind.OPENREWRITE_RECIPE,
                        "com.synexia.rewrite.A",
                        SynexiaCanonicalRecipeHandoff.TargetDisposition.THIN_RECEIVER_ONLY,
                        "Apache-2.0");
        assertFalse(entry.reusableEvolutionAllowedInTarget());

        assertThrows(
                IllegalArgumentException.class,
                () -> new SynexiaCanonicalRecipeHandoff.Entry(
                        "../A.java",
                        "b".repeat(40),
                        SynexiaCanonicalRecipeHandoff.AssetKind.POLICY,
                        "com.synexia.rewrite.A",
                        SynexiaCanonicalRecipeHandoff.TargetDisposition.SOURCE_PIN_ONLY,
                        "Apache-2.0"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SynexiaCanonicalRecipeHandoff.Entry(
                        "A.java",
                        "not-a-blob",
                        SynexiaCanonicalRecipeHandoff.AssetKind.POLICY,
                        "com.synexia.rewrite.A",
                        SynexiaCanonicalRecipeHandoff.TargetDisposition.SOURCE_PIN_ONLY,
                        "Apache-2.0"));
    }

    private static Path repositoryRoot() {
        Path module = Path.of("").toAbsolutePath().normalize();
        if ("synexia-import".equals(module.getFileName().toString())) {
            return module.getParent().getParent();
        }
        Path multi =
                Path.of(System.getProperty("maven.multiModuleProjectDirectory", "."))
                        .toAbsolutePath()
                        .normalize();
        return "m3".equals(multi.getFileName().toString())
                ? multi.getParent()
                : multi;
    }
}
