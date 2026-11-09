// SPDX-License-Identifier: Apache-2.0
package jdk.m3.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class CanonicalRecipeHandoffTest {
    private static final String HANDOFF =
            "m3/m3-import/current-recipe-handoff/20261011-canonical-recipe-handoff.tsv";
    private static final String REVISION =
            "25ccf1bea2a07e0b59f00b91d933f10785eedd5d";

    @Test
    void checkedInHandoffPinsCurrentCanonicalM3RecipeAssets() throws Exception {
        Path repository = repositoryRoot();
        CanonicalRecipeHandoff handoff =
                CanonicalRecipeHandoff.parse(
                        Files.readString(repository.resolve(HANDOFF)));

        assertEquals(CanonicalRecipeHandoff.SOURCE_REPOSITORY, handoff.sourceRepository());
        assertEquals(REVISION, handoff.sourceRevision());
        assertEquals(13, handoff.entries().size());
        assertFalse(handoff.materializationAuthority());
        assertFalse(handoff.targetPromotionAuthority());

        assertTrue(handoff.entries().stream()
                .allMatch(entry -> entry.sourceGitBlob().matches("[0-9a-f]{40}")));
        assertTrue(handoff.entries().stream()
                .allMatch(entry -> entry.license().equals("Apache-2.0")));
        assertTrue(handoff.entries().stream()
                .noneMatch(CanonicalRecipeHandoff.Entry::reusableEvolutionAllowedInTarget));

        long recipes = handoff.entries().stream()
                .filter(entry -> entry.assetKind()
                        == CanonicalRecipeHandoff.AssetKind.OPENREWRITE_RECIPE)
                .count();
        assertEquals(7L, recipes);
        assertTrue(handoff.entries().stream()
                .filter(entry -> entry.assetKind()
                        == CanonicalRecipeHandoff.AssetKind.OPENREWRITE_RECIPE)
                .allMatch(entry -> entry.canonicalOwner().startsWith("jdk.internal.m3.rewrite.")));
        assertTrue(handoff.entries().stream()
                .filter(entry -> entry.assetKind()
                        == CanonicalRecipeHandoff.AssetKind.OPENREWRITE_RECIPE)
                .allMatch(entry -> entry.targetDisposition()
                        == CanonicalRecipeHandoff.TargetDisposition.THIN_RECEIVER_ONLY));

        assertTrue(handoff.entries().stream().anyMatch(entry ->
                entry.sourcePath().equals(".m3/m3-recipe-first-invariants.tsv")
                        && entry.assetKind()
                                == CanonicalRecipeHandoff.AssetKind.MACHINE_LEDGER));
        assertTrue(handoff.entries().stream().anyMatch(entry ->
                entry.canonicalOwner().equals(
                        "jdk.internal.m3.rewrite.M3AtomHashDatabaseRecipe")));
        assertTrue(handoff.entries().stream().anyMatch(entry ->
                entry.canonicalOwner().equals(
                        "jdk.internal.m3.rewrite.M3VerbatimJavaBaselineInventoryRecipe")));
    }

    @Test
    void parserFailsClosedOnRepositoryRevisionLicenseOwnerAndDuplicates() {
        String header = CanonicalRecipeHandoff.HEADER + "\n";
        String row =
                CanonicalRecipeHandoff.SCHEMA
                        + "\t"
                        + CanonicalRecipeHandoff.SOURCE_REPOSITORY
                        + "\t"
                        + REVISION
                        + "\tsynexia-openrewrite-recipes/src/main/java/jdk/internal/m3/rewrite/A.java"
                        + "\t"
                        + "a".repeat(40)
                        + "\tOPENREWRITE_RECIPE"
                        + "\tjdk.internal.m3.rewrite.A"
                        + "\tTHIN_RECEIVER_ONLY"
                        + "\tApache-2.0\n";

        CanonicalRecipeHandoff parsed =
                CanonicalRecipeHandoff.parse(header + row);
        assertEquals(1, parsed.entries().size());

        assertThrows(
                IllegalArgumentException.class,
                () -> CanonicalRecipeHandoff.parse(
                        "bad\n" + row));
        assertThrows(
                IllegalArgumentException.class,
                () -> CanonicalRecipeHandoff.parse(header));
        assertThrows(
                IllegalArgumentException.class,
                () -> CanonicalRecipeHandoff.parse(
                        header + row.replace("hsoliwal/com.synexia", "other/repo")));
        assertThrows(
                IllegalArgumentException.class,
                () -> CanonicalRecipeHandoff.parse(
                        header + row.replace("Apache-2.0", "MIT")));
        assertThrows(
                IllegalArgumentException.class,
                () -> CanonicalRecipeHandoff.parse(
                        header + row.replace("jdk.internal.m3.rewrite.A", "jdk.m3.rewrite.A")));
        assertThrows(
                IllegalArgumentException.class,
                () -> CanonicalRecipeHandoff.parse(header + row + row));
    }

    @Test
    void entryAndReceiptContractsGrantNoTargetEvolutionAuthority() {
        var entry =
                new CanonicalRecipeHandoff.Entry(
                        "m3-openrewrite-recipes/src/main/java/jdk/internal/m3/rewrite/A.java",
                        "b".repeat(40),
                        CanonicalRecipeHandoff.AssetKind.OPENREWRITE_RECIPE,
                        "jdk.internal.m3.rewrite.A",
                        CanonicalRecipeHandoff.TargetDisposition.THIN_RECEIVER_ONLY,
                        "Apache-2.0");
        assertFalse(entry.reusableEvolutionAllowedInTarget());

        assertThrows(
                IllegalArgumentException.class,
                () -> new CanonicalRecipeHandoff.Entry(
                        "../A.java",
                        "b".repeat(40),
                        CanonicalRecipeHandoff.AssetKind.POLICY,
                        "jdk.internal.m3.rewrite.A",
                        CanonicalRecipeHandoff.TargetDisposition.SOURCE_PIN_ONLY,
                        "Apache-2.0"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new CanonicalRecipeHandoff.Entry(
                        "A.java",
                        "not-a-blob",
                        CanonicalRecipeHandoff.AssetKind.POLICY,
                        "jdk.internal.m3.rewrite.A",
                        CanonicalRecipeHandoff.TargetDisposition.SOURCE_PIN_ONLY,
                        "Apache-2.0"));
    }

    private static Path repositoryRoot() {
        Path module = Path.of("").toAbsolutePath().normalize();
        if ("m3-import".equals(module.getFileName().toString())) {
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
