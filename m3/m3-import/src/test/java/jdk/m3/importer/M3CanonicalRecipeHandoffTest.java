// SPDX-License-Identifier: Apache-2.0
package jdk.m3.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class M3CanonicalRecipeHandoffTest {
    private static final String HANDOFF =
            "m3/m3-import/current-recipe-handoff/20261011-canonical-recipe-handoff.tsv";
    private static final String REVISION =
            "25ccf1bea2a07e0b59f00b91d933f10785eedd5d";

    @Test
    void checkedInHandoffPinsCurrentCanonicalM3RecipeAssets() throws Exception {
        Path repository = repositoryRoot();
        M3CanonicalRecipeHandoff handoff =
                M3CanonicalRecipeHandoff.parse(
                        Files.readString(repository.resolve(HANDOFF)));

        assertEquals(M3CanonicalRecipeHandoff.SOURCE_REPOSITORY, handoff.sourceRepository());
        assertEquals(REVISION, handoff.sourceRevision());
        assertEquals(13, handoff.entries().size());
        assertFalse(handoff.materializationAuthority());
        assertFalse(handoff.targetPromotionAuthority());

        assertTrue(handoff.entries().stream()
                .allMatch(entry -> entry.sourceGitBlob().matches("[0-9a-f]{40}")));
        assertTrue(handoff.entries().stream()
                .allMatch(entry -> entry.license().equals("Apache-2.0")));
        assertTrue(handoff.entries().stream()
                .noneMatch(M3CanonicalRecipeHandoff.Entry::reusableEvolutionAllowedInTarget));

        long recipes = handoff.entries().stream()
                .filter(entry -> entry.assetKind()
                        == M3CanonicalRecipeHandoff.AssetKind.OPENREWRITE_RECIPE)
                .count();
        assertEquals(7L, recipes);
        assertTrue(handoff.entries().stream()
                .filter(entry -> entry.assetKind()
                        == M3CanonicalRecipeHandoff.AssetKind.OPENREWRITE_RECIPE)
                .allMatch(entry -> entry.canonicalOwner().startsWith("jdk.internal.m3.rewrite.")));
        assertTrue(handoff.entries().stream()
                .filter(entry -> entry.assetKind()
                        == M3CanonicalRecipeHandoff.AssetKind.OPENREWRITE_RECIPE)
                .allMatch(entry -> entry.targetDisposition()
                        == M3CanonicalRecipeHandoff.TargetDisposition.THIN_RECEIVER_ONLY));

        assertTrue(handoff.entries().stream().anyMatch(entry ->
                entry.sourcePath().equals(".m3/m3-recipe-first-invariants.tsv")
                        && entry.assetKind()
                                == M3CanonicalRecipeHandoff.AssetKind.MACHINE_LEDGER));
        assertTrue(handoff.entries().stream().anyMatch(entry ->
                entry.canonicalOwner().equals(
                        "jdk.internal.m3.rewrite.M3AtomHashDatabaseRecipe")));
        assertTrue(handoff.entries().stream().anyMatch(entry ->
                entry.canonicalOwner().equals(
                        "jdk.internal.m3.rewrite.M3VerbatimJavaBaselineInventoryRecipe")));
    }

    @Test
    void parserFailsClosedOnRepositoryRevisionLicenseOwnerAndDuplicates() {
        String header = M3CanonicalRecipeHandoff.HEADER + "\n";
        String row =
                M3CanonicalRecipeHandoff.SCHEMA
                        + "\t"
                        + M3CanonicalRecipeHandoff.SOURCE_REPOSITORY
                        + "\t"
                        + REVISION
                        + "\tsynexia-openrewrite-recipes/src/main/java/jdk/internal/m3/rewrite/A.java"
                        + "\t"
                        + "a".repeat(40)
                        + "\tOPENREWRITE_RECIPE"
                        + "\tjdk.internal.m3.rewrite.A"
                        + "\tTHIN_RECEIVER_ONLY"
                        + "\tApache-2.0\n";

        M3CanonicalRecipeHandoff parsed =
                M3CanonicalRecipeHandoff.parse(header + row);
        assertEquals(1, parsed.entries().size());

        assertThrows(
                IllegalArgumentException.class,
                () -> M3CanonicalRecipeHandoff.parse(
                        "bad\n" + row));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3CanonicalRecipeHandoff.parse(header));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3CanonicalRecipeHandoff.parse(
                        header + row.replace("hsoliwal/com.synexia", "other/repo")));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3CanonicalRecipeHandoff.parse(
                        header + row.replace("Apache-2.0", "MIT")));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3CanonicalRecipeHandoff.parse(
                        header + row.replace("jdk.internal.m3.rewrite.A", "jdk.m3.rewrite.A")));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3CanonicalRecipeHandoff.parse(header + row + row));
    }

    @Test
    void entryAndReceiptContractsGrantNoTargetEvolutionAuthority() {
        var entry =
                new M3CanonicalRecipeHandoff.Entry(
                        "m3-openrewrite-recipes/src/main/java/jdk/internal/m3/rewrite/A.java",
                        "b".repeat(40),
                        M3CanonicalRecipeHandoff.AssetKind.OPENREWRITE_RECIPE,
                        "jdk.internal.m3.rewrite.A",
                        M3CanonicalRecipeHandoff.TargetDisposition.THIN_RECEIVER_ONLY,
                        "Apache-2.0");
        assertFalse(entry.reusableEvolutionAllowedInTarget());

        assertThrows(
                IllegalArgumentException.class,
                () -> new M3CanonicalRecipeHandoff.Entry(
                        "../A.java",
                        "b".repeat(40),
                        M3CanonicalRecipeHandoff.AssetKind.POLICY,
                        "jdk.internal.m3.rewrite.A",
                        M3CanonicalRecipeHandoff.TargetDisposition.SOURCE_PIN_ONLY,
                        "Apache-2.0"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3CanonicalRecipeHandoff.Entry(
                        "A.java",
                        "not-a-blob",
                        M3CanonicalRecipeHandoff.AssetKind.POLICY,
                        "jdk.internal.m3.rewrite.A",
                        M3CanonicalRecipeHandoff.TargetDisposition.SOURCE_PIN_ONLY,
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
