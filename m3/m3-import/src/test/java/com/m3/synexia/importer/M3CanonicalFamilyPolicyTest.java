// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-07: cover collection/algorithm custody admission and retained refusal gates.
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

final class M3CanonicalFamilyPolicyTest {
    @Test
    void everyCanonicalFamilyClassifiesItsPrefixAndChild() {
        for (M3CanonicalFamilyPolicy.Family family :
                M3CanonicalFamilyPolicy.Family.values()) {
            assertEquals(
                    family,
                    M3CanonicalFamilyPolicy.requireFamily(
                            family.sourcePrefix() + "pom.xml"));
            assertEquals(
                    family,
                    M3CanonicalFamilyPolicy.classify(
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
                        "m3/m3-import/m3index-family-receiver.tsv");
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
        assertEquals(M3CanonicalFamilyPolicy.Family.values().length, rows.size());

        for (M3CanonicalFamilyPolicy.Family family :
                M3CanonicalFamilyPolicy.Family.values()) {
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
                M3CanonicalFamilyPolicy.classify(
                                "other-module/src/main/java/p/A.java")
                        .isEmpty());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3CanonicalFamilyPolicy.requireFamily(
                                "other-module/src/main/java/p/A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3CanonicalFamilyPolicy.classify("../A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3CanonicalFamilyPolicy.classify("/A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3CanonicalFamilyPolicy.classify("a//b.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3CanonicalFamilyPolicy.classify("a/../b.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3CanonicalFamilyPolicy.classify(""));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3CanonicalFamilyPolicy.classify(null));
    }

    @Test
    void vendorTargetMustBeExactSourceMirror() {
        String source =
                "m3-mindex/compiler/src/main/java/jdk/internal/m3/mindex/compiler/ast/A.java";
        String target = "m3/vendor/synexia/" + source;

        M3CanonicalFamilyPolicy.requireMirrorTarget(source, target);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3CanonicalFamilyPolicy.requireMirrorTarget(
                                source,
                                "m3/vendor/synexia/renamed/A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3CanonicalFamilyPolicy.requireMirrorTarget(
                                source, "../" + target));
    }

    @Test
    void m3IndexAndIndexStringCategoriesMustMatchCanonicalFamilies() {
        M3CanonicalFamilyPolicy.requireCategoryFamily(
                "m3index-family",
                "m3-m3index/core/pom.xml");
        M3CanonicalFamilyPolicy.requireCategoryFamily(
                "m3index-compiler-java",
                "m3-mindex/compiler/src/main/java/p/A.java");
        M3CanonicalFamilyPolicy.requireCategoryFamily(
                "m3index-data-structure-tests",
                "m3-mindex/data-structure/src/test/java/p/ATest.java");
        M3CanonicalFamilyPolicy.requireCategoryFamily(
                "m3index-precompute-api-java",
                "m3-mindex/precompute-api/src/main/java/p/A.java");
        M3CanonicalFamilyPolicy.requireCategoryFamily(
                "m3index-db-java",
                "m3-mindex/db/src/main/java/p/A.java");
        M3CanonicalFamilyPolicy.requireCategoryFamily(
                "indexstring-java",
                "m3-indexstring/src/main/java/p/A.java");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3CanonicalFamilyPolicy.requireCategoryFamily(
                                "m3index-compiler-java",
                                "other-module/src/main/java/p/A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3CanonicalFamilyPolicy.requireCategoryFamily(
                                "indexstring-java",
                                "m3-mindex/compiler/src/main/java/p/A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3CanonicalFamilyPolicy.requireCategoryFamily(
                                "m3index-family",
                                "m3-indexstring/src/main/java/p/A.java"));

        // Generic historical seed categories remain legal; this guard only owns M3Index-family labels.
        M3CanonicalFamilyPolicy.requireCategoryFamily(
                "seed",
                "other-module/src/main/java/p/A.java");
    }

    @Test
    void importPlanUsesExplicitM3IndexFamilyLanes() {
        assertEquals(
                M3ImportPlan.Lane.M3INDEX,
                M3ImportPlan.lane(
                        "m3index-family",
                        "m3/vendor/synexia/m3-m3index/core/pom.xml"));
        assertEquals(
                M3ImportPlan.Lane.INDEXSTRING,
                M3ImportPlan.lane(
                        "indexstring-java",
                        "m3/vendor/synexia/m3-indexstring/src/main/java/p/A.java"));
        assertEquals(
                M3ImportPlan.Lane.M3INDEX_COMPILER,
                M3ImportPlan.lane(
                        "m3index-compiler-java",
                        "m3/vendor/synexia/m3-mindex/compiler/src/main/java/p/A.java"));
        assertEquals(
                M3ImportPlan.Lane.M3INDEX_DATA_STRUCTURE,
                M3ImportPlan.lane(
                        "m3index-data-structure-java",
                        "m3/vendor/synexia/m3-mindex/data-structure/src/main/java/p/A.java"));
        assertEquals(
                M3ImportPlan.Lane.M3INDEX_PRECOMPUTE,
                M3ImportPlan.lane(
                        "m3index-precompute-api-java",
                        "m3/vendor/synexia/m3-mindex/precompute-api/src/main/java/p/A.java"));
        assertEquals(
                M3ImportPlan.Lane.M3INDEX_DB,
                M3ImportPlan.lane(
                        "m3index-db-java",
                        "m3/vendor/synexia/m3-mindex/db/src/main/java/p/A.java"));

        // Stale/legacy rows still infer from target path.
        assertEquals(
                M3ImportPlan.Lane.M3INDEX_COMPILER,
                M3ImportPlan.lane(
                        "stale",
                        "m3/vendor/synexia/m3-mindex/compiler/src/main/java/p/A.java"));
    }

    @Test
    void manifestEntryEnforcesMirrorAndCategoryFamilyWithoutWeakeningApacheGate() {
        String source =
                "m3-mindex/precompute-api/src/main/java/p/A.java";
        String target = "m3/vendor/synexia/" + source;

        new M3ImportManifest.Entry(
                "m3index-precompute-api-java",
                source,
                target,
                "1".repeat(64),
                "Apache-2.0",
                M3ImportManifest.Mode.APACHE_SOURCE);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new M3ImportManifest.Entry(
                                "m3index-precompute-api-java",
                                source,
                                "m3/vendor/synexia/renamed/A.java",
                                "1".repeat(64),
                                "Apache-2.0",
                                M3ImportManifest.Mode.APACHE_SOURCE));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new M3ImportManifest.Entry(
                                "indexstring-java",
                                source,
                                target,
                                "1".repeat(64),
                                "Apache-2.0",
                                M3ImportManifest.Mode.APACHE_SOURCE));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new M3ImportManifest.Entry(
                                "m3index-precompute-api-java",
                                source,
                                target,
                                "1".repeat(64),
                                "MIT",
                                M3ImportManifest.Mode.APACHE_SOURCE));
    }

    @Test
    void collectionAndAlgorithmSourcesResolveToTheirCanonicalSynexiaCoordinates() {
        List<String> prefixes = List.of(
                "m3-mindex/collections/", "m3-mindex/algorithm/");
        List<String> names = List.of("M3INDEX_COLLECTIONS", "M3INDEX_ALGORITHM");
        List<String> owners = List.of(
                "com.synexia:m3-m3index-collections",
                "com.synexia:m3-m3index-algorithm");
        for (int i = 0; i < prefixes.size(); i++) {
            M3CanonicalFamilyPolicy.Family family =
                    M3CanonicalFamilyPolicy.requireFamily(
                            prefixes.get(i) + "src/main/java/p/A.java");
            assertEquals(names.get(i), family.name());
            assertEquals(owners.get(i), family.canonicalOwner());
            assertEquals(family, M3CanonicalFamilyPolicy.requireFamily(
                    prefixes.get(i) + "pom.xml"));
        }

        // Additive coordinates retain the original, broad alias custody classification.
        assertEquals(M3CanonicalFamilyPolicy.Family.M3INDEX_ALIAS,
                M3CanonicalFamilyPolicy.requireFamily(
                        "m3-m3index/collections/pom.xml"));
        assertEquals(M3CanonicalFamilyPolicy.Family.M3INDEX_ALIAS,
                M3CanonicalFamilyPolicy.requireFamily(
                        "m3-m3index/algorithm/pom.xml"));
    }

    @Test
    void collectionAndAlgorithmIntakeRetainsApacheAndExactMirrorRequirements() {
        for (String prefix : List.of(
                "m3-mindex/collections/", "m3-mindex/algorithm/")) {
            String source = prefix + "src/main/java/p/A.java";
            String target = "m3/vendor/synexia/" + source;
            new M3ImportManifest.Entry(
                    "m3index-family", source, target, "1".repeat(64), "Apache-2.0",
                    M3ImportManifest.Mode.APACHE_SOURCE);
            assertThrows(IllegalArgumentException.class, () ->
                    new M3ImportManifest.Entry(
                            "m3index-family", source, "m3/vendor/synexia/renamed/A.java",
                            "1".repeat(64), "Apache-2.0",
                            M3ImportManifest.Mode.APACHE_SOURCE));
            assertThrows(IllegalArgumentException.class, () ->
                    new M3ImportManifest.Entry(
                            "m3index-family", source, target, "1".repeat(64), "MIT",
                            M3ImportManifest.Mode.APACHE_SOURCE));
            assertThrows(IllegalArgumentException.class, () ->
                    new M3ImportManifest.Entry(
                            "indexstring-java", source, target, "1".repeat(64), "Apache-2.0",
                            M3ImportManifest.Mode.APACHE_SOURCE));
        }
    }

    @Test
    void collectionAndAlgorithmLookalikesAndUnsafePathsStayRefused() {
        for (String source : List.of(
                "m3-mindex/collections-old/pom.xml",
                "m3-mindex/algorithmic/pom.xml",
                "m3-mindex/collections",
                "m3-mindex/algorithm")) {
            assertTrue(M3CanonicalFamilyPolicy.classify(source).isEmpty());
            assertThrows(IllegalArgumentException.class, () ->
                    M3CanonicalFamilyPolicy.requireCategoryFamily(
                            "m3index-family", source));
        }
        for (String source : List.of(
                "../m3-mindex/collections/pom.xml",
                "m3-mindex/algorithm/../compiler/pom.xml",
                "m3-mindex/collections//pom.xml")) {
            assertThrows(IllegalArgumentException.class, () ->
                    M3CanonicalFamilyPolicy.classify(source));
        }
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
