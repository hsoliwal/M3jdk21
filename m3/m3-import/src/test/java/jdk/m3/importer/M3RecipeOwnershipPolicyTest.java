// SPDX-License-Identifier: Apache-2.0
package jdk.m3.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3RecipeOwnershipPolicyTest {
    private static final String INTAKE =
            "m3/m3-import/intakes/recipe-ownership-20261007/";
    private static final String BORROWING =
            INTAKE + "m3jdk21-m3-borrowing.tsv";

    @Test
    void checkedInM3LedgerAuditsEveryCurrentRecipeSource() throws Exception {
        Path repository = repositoryRoot();
        M3RecipeOwnershipPolicy policy =
                M3RecipeOwnershipPolicy.load(repository.resolve(BORROWING));

        List<M3RecipeOwnershipPolicy.AuditRow> rows =
                policy.auditRecipeTree(repository);

        assertFalse(rows.isEmpty());
        assertTrue(rows.stream().allMatch(row ->
                row.sourcePath().startsWith(M3RecipeOwnershipPolicy.RECIPE_SOURCE_ROOT)));
        assertTrue(rows.stream().allMatch(row -> row.targetOwner().startsWith("jdk.m3.rewrite.")));

        assertTrue(rows.stream().anyMatch(row ->
                row.targetOwner().equals("jdk.m3.rewrite.InstallIndexStringCompatibility")
                        && row.classification().thinAdapterOnly()));
        assertTrue(rows.stream().anyMatch(row ->
                row.targetOwner().equals("jdk.m3.rewrite.M3Java21ConvergenceRecipe")
                        && row.classification().synexiaCanonicalResidue()));
        assertTrue(rows.stream().anyMatch(row ->
                row.targetOwner().startsWith("jdk.m3.rewrite.scope.")
                        && row.classification().synexiaCanonicalResidue()));
        assertTrue(rows.stream().anyMatch(row ->
                row.targetOwner().startsWith("jdk.m3.rewrite.semantic.")
                        && row.classification().synexiaCanonicalResidue()));
        assertTrue(rows.stream().anyMatch(row ->
                row.targetOwner().startsWith("jdk.m3.rewrite.a3.")
                        && row.classification().synexiaCanonicalResidue()));
        assertTrue(rows.stream().anyMatch(row ->
                row.targetOwner().startsWith("jdk.m3.rewrite.synexia.")
                        && row.classification().thinAdapterOnly()));
        assertTrue(rows.stream().anyMatch(row ->
                row.targetOwner().startsWith("jdk.m3.rewrite.backport.")
                        && row.classification().reusableEvolutionAllowed()));

        long currentJavaFiles;
        try (var paths = Files.walk(repository.resolve(
                M3RecipeOwnershipPolicy.RECIPE_SOURCE_ROOT))) {
            currentJavaFiles = paths.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".java"))
                    .count();
        }
        assertEquals(currentJavaFiles, rows.size());
    }

    @Test
    void exactRulesWinBeforeWildcardRulesAndEvolutionFenceMatchesDisposition() {
        M3RecipeOwnershipPolicy policy = M3RecipeOwnershipPolicy.parse(
                header()
                        + row(
                                "jdk.m3.rewrite.scope.*",
                                "M3_CANONICAL",
                                "jdk.internal.m3.rewrite.M3RecipeFirstInvariant")
                        + row(
                                "jdk.m3.rewrite.scope.Special",
                                "TARGET_ADAPTER_ONLY",
                                "jdk.internal.m3.rewrite.SpecialAdapter")
                        + row(
                                "jdk.m3.rewrite.backport.*",
                                "JDK_TARGET_SPECIFIC",
                                "jdk.internal.m3.rewrite.M3Jdk21GitBlobRecipeCratePlanner"));

        var exact = policy.require("jdk.m3.rewrite.scope.Special");
        assertEquals(
                M3RecipeOwnershipPolicy.Disposition.TARGET_ADAPTER_ONLY,
                exact.disposition());
        assertTrue(exact.thinAdapterOnly());
        assertFalse(exact.reusableEvolutionAllowed());

        var wildcard = policy.require("jdk.m3.rewrite.scope.Other");
        assertEquals(
                M3RecipeOwnershipPolicy.Disposition.M3_CANONICAL,
                wildcard.disposition());
        assertTrue(wildcard.synexiaCanonicalResidue());
        assertFalse(wildcard.reusableEvolutionAllowed());

        var jdk = policy.require("jdk.m3.rewrite.backport.M3Jep999");
        assertEquals(
                M3RecipeOwnershipPolicy.Disposition.JDK_TARGET_SPECIFIC,
                jdk.disposition());
        assertTrue(jdk.reusableEvolutionAllowed());

        policy.requireReusableEvolutionAllowed("jdk.m3.rewrite.backport.M3Jep999");
        assertThrows(
                IllegalStateException.class,
                () -> policy.requireReusableEvolutionAllowed(
                        "jdk.m3.rewrite.scope.Other"));
        assertThrows(
                IllegalStateException.class,
                () -> policy.requireReusableEvolutionAllowed(
                        "jdk.m3.rewrite.scope.Special"));
        assertThrows(
                IllegalArgumentException.class,
                () -> policy.require("jdk.m3.rewrite.other.Unowned"));
    }

    @Test
    void wildcardResolutionUsesLongestPrefix() {
        M3RecipeOwnershipPolicy policy = M3RecipeOwnershipPolicy.parse(
                header()
                        + row(
                                "jdk.m3.rewrite.*",
                                "TARGET_ADAPTER_ONLY",
                                "jdk.internal.m3.rewrite.M3JdkHandoff")
                        + row(
                                "jdk.m3.rewrite.scope.*",
                                "M3_CANONICAL",
                                "jdk.internal.m3.rewrite.M3RecipeFirstInvariant"));

        assertTrue(policy.require("jdk.m3.rewrite.scope.A").synexiaCanonicalResidue());
        assertTrue(policy.require("jdk.m3.rewrite.other.A").thinAdapterOnly());
    }

    @Test
    void ownerPathMappingIsExactAndRejectsEscapesOrNonRecipeFiles() {
        assertEquals(
                "jdk.m3.rewrite.scope.M3EditScope",
                M3RecipeOwnershipPolicy.ownerFromPath(
                        "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/"
                                + "scope/M3EditScope.java"));
        assertEquals(
                "jdk.m3.rewrite.InstallIndexStringCompatibility",
                M3RecipeOwnershipPolicy.ownerFromPath(
                        "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/"
                                + "InstallIndexStringCompatibility.java"));

        assertThrows(
                IllegalArgumentException.class,
                () -> M3RecipeOwnershipPolicy.ownerFromPath("../A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3RecipeOwnershipPolicy.ownerFromPath(
                        "m3/tooling/migration-recipes/src/main/java/jdk/m3/rewrite/A.txt"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3RecipeOwnershipPolicy.ownerFromPath(
                        "src/main/java/jdk/m3/rewrite/A.java"));
    }

    @Test
    void ledgerParserFailsClosedOnSchemaTargetDispositionLicenseAndRows() {
        assertThrows(
                NullPointerException.class,
                () -> M3RecipeOwnershipPolicy.parse(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3RecipeOwnershipPolicy.parse(""));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3RecipeOwnershipPolicy.parse("bad\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3RecipeOwnershipPolicy.parse(
                        header()
                                + "OTHER\t"
                                + M3RecipeOwnershipPolicy.EXPECTED_TARGET
                                + "\tjdk.m3.rewrite.A\tSYNEXIA_CANONICAL\t"
                                + "jdk.internal.m3.A\tApache-2.0\tx\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3RecipeOwnershipPolicy.parse(
                        header()
                                + M3RecipeOwnershipPolicy.EXPECTED_SCHEMA
                                + "\tother/repo\tjdk.m3.rewrite.A\tSYNEXIA_CANONICAL\t"
                                + "jdk.internal.m3.A\tApache-2.0\tx\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3RecipeOwnershipPolicy.parse(
                        header()
                                + M3RecipeOwnershipPolicy.EXPECTED_SCHEMA
                                + "\t"
                                + M3RecipeOwnershipPolicy.EXPECTED_TARGET
                                + "\tjdk.m3.rewrite.A\tUNKNOWN\t"
                                + "jdk.internal.m3.A\tApache-2.0\tx\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3RecipeOwnershipPolicy.parse(
                        header()
                                + M3RecipeOwnershipPolicy.EXPECTED_SCHEMA
                                + "\t"
                                + M3RecipeOwnershipPolicy.EXPECTED_TARGET
                                + "\tjdk.m3.rewrite.A\tSYNEXIA_CANONICAL\t"
                                + "jdk.internal.m3.A\tMIT\tx\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3RecipeOwnershipPolicy.parse(
                        header()
                                + "too\tfew\tcells\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3RecipeOwnershipPolicy.parse(header()));
    }

    @Test
    void ruleAndClassificationContractsRejectUnsafeMetadata() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3RecipeOwnershipPolicy.Rule(
                        "",
                        M3RecipeOwnershipPolicy.Disposition.M3_CANONICAL,
                        "jdk.internal.m3.A",
                        "Apache-2.0",
                        "x"));
        assertThrows(
                NullPointerException.class,
                () -> new M3RecipeOwnershipPolicy.Rule(
                        "jdk.m3.rewrite.A",
                        null,
                        "jdk.internal.m3.A",
                        "Apache-2.0",
                        "x"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3RecipeOwnershipPolicy.Classification(
                        "jdk.m3.rewrite.A",
                        M3RecipeOwnershipPolicy.Disposition.M3_CANONICAL,
                        "",
                        "x"));

        var wildcard = new M3RecipeOwnershipPolicy.Rule(
                "jdk.m3.rewrite.scope.*",
                M3RecipeOwnershipPolicy.Disposition.M3_CANONICAL,
                "jdk.internal.m3.rewrite.M3RecipeFirstInvariant",
                "Apache-2.0",
                "scope");
        assertTrue(wildcard.wildcard());
        assertEquals("jdk.m3.rewrite.scope.", wildcard.prefix());
        assertTrue(wildcard.matches("jdk.m3.rewrite.scope.A"));
        assertFalse(wildcard.matches("jdk.m3.rewrite.semantic.A"));

        var exact = new M3RecipeOwnershipPolicy.Rule(
                "jdk.m3.rewrite.A",
                M3RecipeOwnershipPolicy.Disposition.TARGET_ADAPTER_ONLY,
                "jdk.internal.m3.A",
                "Apache-2.0",
                "adapter");
        assertFalse(exact.wildcard());
        assertEquals("jdk.m3.rewrite.A", exact.prefix());
        assertTrue(exact.matches("jdk.m3.rewrite.A"));
        assertFalse(exact.matches("jdk.m3.rewrite.A.B"));
    }

    @Test
    void checkedInIntakeIsBoundToExactM3RevisionAndBlobs() throws Exception {
        Path repository = repositoryRoot();
        Path source = repository.resolve(INTAKE + "SOURCE.tsv");
        assertTrue(Files.isRegularFile(source));

        List<String> lines = Files.readAllLines(source);
        assertEquals(
                "schema\tsource_repository\tsource_revision\tsource_path\t"
                        + "source_git_blob\tpurpose",
                lines.getFirst());
        assertEquals(3, lines.size());

        for (String line : lines.subList(1, lines.size())) {
            String[] cells = line.split("\t", -1);
            assertEquals(6, cells.length);
            assertEquals("M3_RECIPE_OWNERSHIP_INTAKE_V1", cells[0]);
            assertEquals("hsoliwal/com.synexia", cells[1]);
            assertEquals("d479e02178d908fd59df941c7d91275970d412d3", cells[2]);
            assertTrue(cells[4].matches("[0-9a-f]{40}"));
        }

        assertTrue(Files.readString(repository.resolve(
                        INTAKE + "recipe-ownership.tsv"))
                .contains("openjdk-exact-backport\thsoliwal/M3jdk21"));
        assertTrue(Files.readString(repository.resolve(BORROWING))
                .contains("jdk.m3.rewrite.backport.*\tJDK_TARGET_SPECIFIC"));
    }

    private static String header() {
        return "schema\ttarget_repository\ttarget_owner\tdisposition\t"
                + "canonical_m3_owner\tlicense\trationale\n";
    }

    private static String row(
            String targetOwner,
            String disposition,
            String canonicalOwner) {
        return M3RecipeOwnershipPolicy.EXPECTED_SCHEMA
                + "\t"
                + M3RecipeOwnershipPolicy.EXPECTED_TARGET
                + "\t"
                + targetOwner
                + "\t"
                + disposition
                + "\t"
                + canonicalOwner
                + "\tApache-2.0\tproof\n";
    }

    private static Path repositoryRoot() {
        Path module = Path.of("").toAbsolutePath().normalize();
        if ("m3-import".equals(module.getFileName().toString())) {
            return module.getParent().getParent();
        }
        Path multi = Path.of(
                        System.getProperty("maven.multiModuleProjectDirectory", "."))
                .toAbsolutePath()
                .normalize();
        return "m3".equals(multi.getFileName().toString())
                ? multi.getParent()
                : multi;
    }
}
