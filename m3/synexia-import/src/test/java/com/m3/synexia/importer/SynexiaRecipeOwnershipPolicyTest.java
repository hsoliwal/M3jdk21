// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SynexiaRecipeOwnershipPolicyTest {
    private static final String INTAKE =
            "m3/synexia-import/intakes/recipe-ownership-20261007/";
    private static final String BORROWING =
            INTAKE + "m3jdk21-synexia-borrowing.tsv";

    @Test
    void checkedInSynexiaLedgerAuditsEveryCurrentRecipeSource() throws Exception {
        Path repository = repositoryRoot();
        SynexiaRecipeOwnershipPolicy policy =
                SynexiaRecipeOwnershipPolicy.load(repository.resolve(BORROWING));

        List<SynexiaRecipeOwnershipPolicy.AuditRow> rows =
                policy.auditRecipeTree(repository);

        assertFalse(rows.isEmpty());
        assertTrue(rows.stream().allMatch(row ->
                row.sourcePath().startsWith(SynexiaRecipeOwnershipPolicy.RECIPE_SOURCE_ROOT)));
        assertTrue(rows.stream().allMatch(row -> row.targetOwner().startsWith("com.m3.rewrite.")));

        assertTrue(rows.stream().anyMatch(row ->
                row.targetOwner().equals("com.m3.rewrite.InstallIndexStringCompatibility")
                        && row.classification().thinAdapterOnly()));
        assertTrue(rows.stream().anyMatch(row ->
                row.targetOwner().equals("com.m3.rewrite.M3Java21ConvergenceRecipe")
                        && row.classification().synexiaCanonicalResidue()));
        assertTrue(rows.stream().anyMatch(row ->
                row.targetOwner().startsWith("com.m3.rewrite.scope.")
                        && row.classification().synexiaCanonicalResidue()));
        assertTrue(rows.stream().anyMatch(row ->
                row.targetOwner().startsWith("com.m3.rewrite.semantic.")
                        && row.classification().synexiaCanonicalResidue()));
        assertTrue(rows.stream().anyMatch(row ->
                row.targetOwner().startsWith("com.m3.rewrite.a3.")
                        && row.classification().synexiaCanonicalResidue()));
        assertTrue(rows.stream().anyMatch(row ->
                row.targetOwner().startsWith("com.m3.rewrite.synexia.")
                        && row.classification().thinAdapterOnly()));
        assertTrue(rows.stream().anyMatch(row ->
                row.targetOwner().startsWith("com.m3.rewrite.backport.")
                        && row.classification().reusableEvolutionAllowed()));

        long currentJavaFiles;
        try (var paths = Files.walk(repository.resolve(
                SynexiaRecipeOwnershipPolicy.RECIPE_SOURCE_ROOT))) {
            currentJavaFiles = paths.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".java"))
                    .count();
        }
        assertEquals(currentJavaFiles, rows.size());
    }

    @Test
    void exactRulesWinBeforeWildcardRulesAndEvolutionFenceMatchesDisposition() {
        SynexiaRecipeOwnershipPolicy policy = SynexiaRecipeOwnershipPolicy.parse(
                header()
                        + row(
                                "com.m3.rewrite.scope.*",
                                "SYNEXIA_CANONICAL",
                                "com.synexia.rewrite.M3RecipeFirstInvariant")
                        + row(
                                "com.m3.rewrite.scope.Special",
                                "TARGET_ADAPTER_ONLY",
                                "com.synexia.rewrite.SpecialAdapter")
                        + row(
                                "com.m3.rewrite.backport.*",
                                "JDK_TARGET_SPECIFIC",
                                "com.synexia.rewrite.M3Jdk21GitBlobRecipeCratePlanner"));

        var exact = policy.require("com.m3.rewrite.scope.Special");
        assertEquals(
                SynexiaRecipeOwnershipPolicy.Disposition.TARGET_ADAPTER_ONLY,
                exact.disposition());
        assertTrue(exact.thinAdapterOnly());
        assertFalse(exact.reusableEvolutionAllowed());

        var wildcard = policy.require("com.m3.rewrite.scope.Other");
        assertEquals(
                SynexiaRecipeOwnershipPolicy.Disposition.SYNEXIA_CANONICAL,
                wildcard.disposition());
        assertTrue(wildcard.synexiaCanonicalResidue());
        assertFalse(wildcard.reusableEvolutionAllowed());

        var jdk = policy.require("com.m3.rewrite.backport.M3Jep999");
        assertEquals(
                SynexiaRecipeOwnershipPolicy.Disposition.JDK_TARGET_SPECIFIC,
                jdk.disposition());
        assertTrue(jdk.reusableEvolutionAllowed());

        policy.requireReusableEvolutionAllowed("com.m3.rewrite.backport.M3Jep999");
        assertThrows(
                IllegalStateException.class,
                () -> policy.requireReusableEvolutionAllowed(
                        "com.m3.rewrite.scope.Other"));
        assertThrows(
                IllegalStateException.class,
                () -> policy.requireReusableEvolutionAllowed(
                        "com.m3.rewrite.scope.Special"));
        assertThrows(
                IllegalArgumentException.class,
                () -> policy.require("com.m3.rewrite.other.Unowned"));
    }

    @Test
    void wildcardResolutionUsesLongestPrefix() {
        SynexiaRecipeOwnershipPolicy policy = SynexiaRecipeOwnershipPolicy.parse(
                header()
                        + row(
                                "com.m3.rewrite.*",
                                "TARGET_ADAPTER_ONLY",
                                "com.synexia.rewrite.M3JdkHandoff")
                        + row(
                                "com.m3.rewrite.scope.*",
                                "SYNEXIA_CANONICAL",
                                "com.synexia.rewrite.M3RecipeFirstInvariant"));

        assertTrue(policy.require("com.m3.rewrite.scope.A").synexiaCanonicalResidue());
        assertTrue(policy.require("com.m3.rewrite.other.A").thinAdapterOnly());
    }

    @Test
    void ownerPathMappingIsExactAndRejectsEscapesOrNonRecipeFiles() {
        assertEquals(
                "com.m3.rewrite.scope.M3EditScope",
                SynexiaRecipeOwnershipPolicy.ownerFromPath(
                        "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/"
                                + "scope/M3EditScope.java"));
        assertEquals(
                "com.m3.rewrite.InstallIndexStringCompatibility",
                SynexiaRecipeOwnershipPolicy.ownerFromPath(
                        "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/"
                                + "InstallIndexStringCompatibility.java"));

        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaRecipeOwnershipPolicy.ownerFromPath("../A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaRecipeOwnershipPolicy.ownerFromPath(
                        "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/A.txt"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaRecipeOwnershipPolicy.ownerFromPath(
                        "src/main/java/com/m3/rewrite/A.java"));
    }

    @Test
    void ledgerParserFailsClosedOnSchemaTargetDispositionLicenseAndRows() {
        assertThrows(
                NullPointerException.class,
                () -> SynexiaRecipeOwnershipPolicy.parse(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaRecipeOwnershipPolicy.parse(""));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaRecipeOwnershipPolicy.parse("bad\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaRecipeOwnershipPolicy.parse(
                        header()
                                + "OTHER\t"
                                + SynexiaRecipeOwnershipPolicy.EXPECTED_TARGET
                                + "\tcom.m3.rewrite.A\tSYNEXIA_CANONICAL\t"
                                + "com.synexia.A\tApache-2.0\tx\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaRecipeOwnershipPolicy.parse(
                        header()
                                + SynexiaRecipeOwnershipPolicy.EXPECTED_SCHEMA
                                + "\tother/repo\tcom.m3.rewrite.A\tSYNEXIA_CANONICAL\t"
                                + "com.synexia.A\tApache-2.0\tx\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaRecipeOwnershipPolicy.parse(
                        header()
                                + SynexiaRecipeOwnershipPolicy.EXPECTED_SCHEMA
                                + "\t"
                                + SynexiaRecipeOwnershipPolicy.EXPECTED_TARGET
                                + "\tcom.m3.rewrite.A\tUNKNOWN\t"
                                + "com.synexia.A\tApache-2.0\tx\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaRecipeOwnershipPolicy.parse(
                        header()
                                + SynexiaRecipeOwnershipPolicy.EXPECTED_SCHEMA
                                + "\t"
                                + SynexiaRecipeOwnershipPolicy.EXPECTED_TARGET
                                + "\tcom.m3.rewrite.A\tSYNEXIA_CANONICAL\t"
                                + "com.synexia.A\tMIT\tx\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaRecipeOwnershipPolicy.parse(
                        header()
                                + "too\tfew\tcells\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaRecipeOwnershipPolicy.parse(header()));
    }

    @Test
    void ruleAndClassificationContractsRejectUnsafeMetadata() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SynexiaRecipeOwnershipPolicy.Rule(
                        "",
                        SynexiaRecipeOwnershipPolicy.Disposition.SYNEXIA_CANONICAL,
                        "com.synexia.A",
                        "Apache-2.0",
                        "x"));
        assertThrows(
                NullPointerException.class,
                () -> new SynexiaRecipeOwnershipPolicy.Rule(
                        "com.m3.rewrite.A",
                        null,
                        "com.synexia.A",
                        "Apache-2.0",
                        "x"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SynexiaRecipeOwnershipPolicy.Classification(
                        "com.m3.rewrite.A",
                        SynexiaRecipeOwnershipPolicy.Disposition.SYNEXIA_CANONICAL,
                        "",
                        "x"));

        var wildcard = new SynexiaRecipeOwnershipPolicy.Rule(
                "com.m3.rewrite.scope.*",
                SynexiaRecipeOwnershipPolicy.Disposition.SYNEXIA_CANONICAL,
                "com.synexia.rewrite.M3RecipeFirstInvariant",
                "Apache-2.0",
                "scope");
        assertTrue(wildcard.wildcard());
        assertEquals("com.m3.rewrite.scope.", wildcard.prefix());
        assertTrue(wildcard.matches("com.m3.rewrite.scope.A"));
        assertFalse(wildcard.matches("com.m3.rewrite.semantic.A"));

        var exact = new SynexiaRecipeOwnershipPolicy.Rule(
                "com.m3.rewrite.A",
                SynexiaRecipeOwnershipPolicy.Disposition.TARGET_ADAPTER_ONLY,
                "com.synexia.A",
                "Apache-2.0",
                "adapter");
        assertFalse(exact.wildcard());
        assertEquals("com.m3.rewrite.A", exact.prefix());
        assertTrue(exact.matches("com.m3.rewrite.A"));
        assertFalse(exact.matches("com.m3.rewrite.A.B"));
    }

    @Test
    void checkedInIntakeIsBoundToExactSynexiaRevisionAndBlobs() throws Exception {
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
            assertEquals("M3_SYNEXIA_RECIPE_OWNERSHIP_INTAKE_V1", cells[0]);
            assertEquals("hsoliwal/com.synexia", cells[1]);
            assertEquals("d479e02178d908fd59df941c7d91275970d412d3", cells[2]);
            assertTrue(cells[4].matches("[0-9a-f]{40}"));
        }

        assertTrue(Files.readString(repository.resolve(
                        INTAKE + "recipe-ownership.tsv"))
                .contains("openjdk-exact-backport\thsoliwal/M3jdk21"));
        assertTrue(Files.readString(repository.resolve(BORROWING))
                .contains("com.m3.rewrite.backport.*\tJDK_TARGET_SPECIFIC"));
    }

    private static String header() {
        return "schema\ttarget_repository\ttarget_owner\tdisposition\t"
                + "canonical_synexia_owner\tlicense\trationale\n";
    }

    private static String row(
            String targetOwner,
            String disposition,
            String canonicalOwner) {
        return SynexiaRecipeOwnershipPolicy.EXPECTED_SCHEMA
                + "\t"
                + SynexiaRecipeOwnershipPolicy.EXPECTED_TARGET
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
        if ("synexia-import".equals(module.getFileName().toString())) {
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
