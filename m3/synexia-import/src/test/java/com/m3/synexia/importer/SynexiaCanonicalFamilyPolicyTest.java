// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class SynexiaCanonicalFamilyPolicyTest {
    @Test
    void everyCanonicalFamilyClassifiesItsPrefixAndChild() {
        for (SynexiaCanonicalFamilyPolicy.Family family :
                SynexiaCanonicalFamilyPolicy.Family.values()) {
            assertEquals(
                    family,
                    SynexiaCanonicalFamilyPolicy.requireFamily(
                            family.sourcePrefix() + "pom.xml"));
            assertEquals(
                    family,
                    SynexiaCanonicalFamilyPolicy.classify(
                                    family.sourcePrefix() + "src/main/java/p/A.java")
                            .orElseThrow());
            assertFalse(family.canonicalOwner().isBlank());
        }
    }

    @Test
    void checkedInReceiverTableMatchesJavaClassifier() throws Exception {
        Path repository = repositoryRoot();
        Path file =
                repository.resolve(
                        "m3/synexia-import/m3index-family-receiver.tsv");
        assertTrue(Files.isRegularFile(file));

        List<String> lines = Files.readAllLines(file);
        assertEquals(
                "family\tsource_prefix\tcanonical_synexia_owner\treceiver_disposition\tlicense",
                lines.getFirst());

        Map<String, String[]> rows = new HashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) continue;
            String[] cells = line.split("\t", -1);
            assertEquals(5, cells.length);
            rows.put(cells[0], cells);
        }
        assertEquals(SynexiaCanonicalFamilyPolicy.Family.values().length, rows.size());

        for (SynexiaCanonicalFamilyPolicy.Family family :
                SynexiaCanonicalFamilyPolicy.Family.values()) {
            String[] cells = rows.get(family.name());
            assertEquals(family.sourcePrefix(), cells[1]);
            assertEquals(family.canonicalOwner(), cells[2]);
            assertEquals("PINNED_VENDOR_CUSTODY", cells[3]);
            assertEquals("Apache-2.0", cells[4]);
        }
    }

    @Test
    void unclassifiedAndUnsafeSourcePathsFailClosedWhenRequired() {
        assertTrue(
                SynexiaCanonicalFamilyPolicy.classify(
                                "other-module/src/main/java/p/A.java")
                        .isEmpty());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        SynexiaCanonicalFamilyPolicy.requireFamily(
                                "other-module/src/main/java/p/A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaCanonicalFamilyPolicy.classify("../A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaCanonicalFamilyPolicy.classify("/A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaCanonicalFamilyPolicy.classify("a//b.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaCanonicalFamilyPolicy.classify("a/../b.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaCanonicalFamilyPolicy.classify(""));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaCanonicalFamilyPolicy.classify(null));
    }

    @Test
    void vendorTargetMustBeExactSourceMirror() {
        String source =
                "synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/ast/A.java";
        String target = "m3/vendor/synexia/" + source;

        SynexiaCanonicalFamilyPolicy.requireMirrorTarget(source, target);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        SynexiaCanonicalFamilyPolicy.requireMirrorTarget(
                                source,
                                "m3/vendor/synexia/renamed/A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        SynexiaCanonicalFamilyPolicy.requireMirrorTarget(
                                source, "../" + target));
    }

    @Test
    void m3IndexAndIndexStringCategoriesMustMatchCanonicalFamilies() {
        SynexiaCanonicalFamilyPolicy.requireCategoryFamily(
                "m3index-family",
                "synexia-m3index/core/pom.xml");
        SynexiaCanonicalFamilyPolicy.requireCategoryFamily(
                "m3index-compiler-java",
                "synexia-mindex/compiler/src/main/java/p/A.java");
        SynexiaCanonicalFamilyPolicy.requireCategoryFamily(
                "m3index-data-structure-tests",
                "synexia-mindex/data-structure/src/test/java/p/ATest.java");
        SynexiaCanonicalFamilyPolicy.requireCategoryFamily(
                "m3index-precompute-api-java",
                "synexia-mindex/precompute-api/src/main/java/p/A.java");
        SynexiaCanonicalFamilyPolicy.requireCategoryFamily(
                "m3index-db-java",
                "synexia-mindex/db/src/main/java/p/A.java");
        SynexiaCanonicalFamilyPolicy.requireCategoryFamily(
                "indexstring-java",
                "synexia-indexstring/src/main/java/p/A.java");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        SynexiaCanonicalFamilyPolicy.requireCategoryFamily(
                                "m3index-compiler-java",
                                "other-module/src/main/java/p/A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        SynexiaCanonicalFamilyPolicy.requireCategoryFamily(
                                "indexstring-java",
                                "synexia-mindex/compiler/src/main/java/p/A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        SynexiaCanonicalFamilyPolicy.requireCategoryFamily(
                                "m3index-family",
                                "synexia-indexstring/src/main/java/p/A.java"));

        // Generic historical seed categories remain legal; this guard only owns M3Index-family labels.
        SynexiaCanonicalFamilyPolicy.requireCategoryFamily(
                "seed",
                "other-module/src/main/java/p/A.java");
    }

    @Test
    void importPlanUsesExplicitM3IndexFamilyLanes() {
        assertEquals(
                SynexiaImportPlan.Lane.M3INDEX,
                SynexiaImportPlan.lane(
                        "m3index-family",
                        "m3/vendor/synexia/synexia-m3index/core/pom.xml"));
        assertEquals(
                SynexiaImportPlan.Lane.INDEXSTRING,
                SynexiaImportPlan.lane(
                        "indexstring-java",
                        "m3/vendor/synexia/synexia-indexstring/src/main/java/p/A.java"));
        assertEquals(
                SynexiaImportPlan.Lane.M3INDEX_COMPILER,
                SynexiaImportPlan.lane(
                        "m3index-compiler-java",
                        "m3/vendor/synexia/synexia-mindex/compiler/src/main/java/p/A.java"));
        assertEquals(
                SynexiaImportPlan.Lane.M3INDEX_DATA_STRUCTURE,
                SynexiaImportPlan.lane(
                        "m3index-data-structure-java",
                        "m3/vendor/synexia/synexia-mindex/data-structure/src/main/java/p/A.java"));
        assertEquals(
                SynexiaImportPlan.Lane.M3INDEX_PRECOMPUTE,
                SynexiaImportPlan.lane(
                        "m3index-precompute-api-java",
                        "m3/vendor/synexia/synexia-mindex/precompute-api/src/main/java/p/A.java"));
        assertEquals(
                SynexiaImportPlan.Lane.M3INDEX_DB,
                SynexiaImportPlan.lane(
                        "m3index-db-java",
                        "m3/vendor/synexia/synexia-mindex/db/src/main/java/p/A.java"));

        // Stale/legacy rows still infer from target path.
        assertEquals(
                SynexiaImportPlan.Lane.M3INDEX_COMPILER,
                SynexiaImportPlan.lane(
                        "stale",
                        "m3/vendor/synexia/synexia-mindex/compiler/src/main/java/p/A.java"));
    }

    @Test
    void manifestEntryEnforcesMirrorAndCategoryFamilyWithoutWeakeningApacheGate() {
        String source =
                "synexia-mindex/precompute-api/src/main/java/p/A.java";
        String target = "m3/vendor/synexia/" + source;

        new SynexiaImportManifest.Entry(
                "m3index-precompute-api-java",
                source,
                target,
                "1".repeat(64),
                "Apache-2.0",
                SynexiaImportManifest.Mode.APACHE_SOURCE);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new SynexiaImportManifest.Entry(
                                "m3index-precompute-api-java",
                                source,
                                "m3/vendor/synexia/renamed/A.java",
                                "1".repeat(64),
                                "Apache-2.0",
                                SynexiaImportManifest.Mode.APACHE_SOURCE));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new SynexiaImportManifest.Entry(
                                "indexstring-java",
                                source,
                                target,
                                "1".repeat(64),
                                "Apache-2.0",
                                SynexiaImportManifest.Mode.APACHE_SOURCE));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new SynexiaImportManifest.Entry(
                                "m3index-precompute-api-java",
                                source,
                                target,
                                "1".repeat(64),
                                "MIT",
                                SynexiaImportManifest.Mode.APACHE_SOURCE));
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
