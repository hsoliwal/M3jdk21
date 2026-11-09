// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-07: cover collection/algorithm custody admission and retained refusal gates.
package jdk.m3.importer;

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

final class CanonicalFamilyPolicyTest {
    @Test
    void everyCanonicalFamilyClassifiesItsPrefixAndChild() {
        for (CanonicalFamilyPolicy.Family family :
                CanonicalFamilyPolicy.Family.values()) {
            assertEquals(
                    family,
                    CanonicalFamilyPolicy.requireFamily(
                            family.sourcePrefix() + "pom.xml"));
            assertEquals(
                    family,
                    CanonicalFamilyPolicy.classify(
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
                "family\tsource_prefix\tcanonical_m3_owner\treceiver_disposition\tlicense",
                lines.getFirst());

        Map<String, String[]> rows = new HashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) continue;
            String[] cells = line.split("\t", -1);
            assertEquals(5, cells.length);
            rows.put(cells[0], cells);
        }
        assertEquals(CanonicalFamilyPolicy.Family.values().length, rows.size());

        for (CanonicalFamilyPolicy.Family family :
                CanonicalFamilyPolicy.Family.values()) {
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
                CanonicalFamilyPolicy.classify(
                                "other-module/src/main/java/p/A.java")
                        .isEmpty());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        CanonicalFamilyPolicy.requireFamily(
                                "other-module/src/main/java/p/A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> CanonicalFamilyPolicy.classify("../A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> CanonicalFamilyPolicy.classify("/A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> CanonicalFamilyPolicy.classify("a//b.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> CanonicalFamilyPolicy.classify("a/../b.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> CanonicalFamilyPolicy.classify(""));
        assertThrows(
                IllegalArgumentException.class,
                () -> CanonicalFamilyPolicy.classify(null));
    }

    @Test
    void vendorTargetMustBeExactSourceMirror() {
        String source =
                "m3-mindex/compiler/src/main/java/jdk/internal/m3/mindex/compiler/ast/A.java";
        String target = "m3/vendor/m3/" + source;

        CanonicalFamilyPolicy.requireMirrorTarget(source, target);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        CanonicalFamilyPolicy.requireMirrorTarget(
                                source,
                                "m3/vendor/m3/renamed/A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        CanonicalFamilyPolicy.requireMirrorTarget(
                                source, "../" + target));
    }

    @Test
    void m3IndexAndIndexStringCategoriesMustMatchCanonicalFamilies() {
        CanonicalFamilyPolicy.requireCategoryFamily(
                "m3index-family",
                "m3-m3index/core/pom.xml");
        CanonicalFamilyPolicy.requireCategoryFamily(
                "m3index-compiler-java",
                "m3-mindex/compiler/src/main/java/p/A.java");
        CanonicalFamilyPolicy.requireCategoryFamily(
                "m3index-data-structure-tests",
                "m3-mindex/data-structure/src/test/java/p/ATest.java");
        CanonicalFamilyPolicy.requireCategoryFamily(
                "m3index-precompute-api-java",
                "m3-mindex/precompute-api/src/main/java/p/A.java");
        CanonicalFamilyPolicy.requireCategoryFamily(
                "m3index-db-java",
                "m3-mindex/db/src/main/java/p/A.java");
        CanonicalFamilyPolicy.requireCategoryFamily(
                "indexstring-java",
                "m3-indexstring/src/main/java/p/A.java");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        CanonicalFamilyPolicy.requireCategoryFamily(
                                "m3index-compiler-java",
                                "other-module/src/main/java/p/A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        CanonicalFamilyPolicy.requireCategoryFamily(
                                "indexstring-java",
                                "m3-mindex/compiler/src/main/java/p/A.java"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        CanonicalFamilyPolicy.requireCategoryFamily(
                                "m3index-family",
                                "m3-indexstring/src/main/java/p/A.java"));

        // Generic historical seed categories remain legal; this guard only owns M3Index-family labels.
        CanonicalFamilyPolicy.requireCategoryFamily(
                "seed",
                "other-module/src/main/java/p/A.java");
    }

    @Test
    void importPlanUsesExplicitM3IndexFamilyLanes() {
        assertEquals(
                ImportPlan.Lane.M3INDEX,
                ImportPlan.lane(
                        "m3index-family",
                        "m3/vendor/m3/m3-m3index/core/pom.xml"));
        assertEquals(
                ImportPlan.Lane.INDEXSTRING,
                ImportPlan.lane(
                        "indexstring-java",
                        "m3/vendor/m3/m3-indexstring/src/main/java/p/A.java"));
        assertEquals(
                ImportPlan.Lane.M3INDEX_COMPILER,
                ImportPlan.lane(
                        "m3index-compiler-java",
                        "m3/vendor/m3/m3-mindex/compiler/src/main/java/p/A.java"));
        assertEquals(
                ImportPlan.Lane.M3INDEX_DATA_STRUCTURE,
                ImportPlan.lane(
                        "m3index-data-structure-java",
                        "m3/vendor/m3/m3-mindex/data-structure/src/main/java/p/A.java"));
        assertEquals(
                ImportPlan.Lane.M3INDEX_PRECOMPUTE,
                ImportPlan.lane(
                        "m3index-precompute-api-java",
                        "m3/vendor/m3/m3-mindex/precompute-api/src/main/java/p/A.java"));
        assertEquals(
                ImportPlan.Lane.M3INDEX_DB,
                ImportPlan.lane(
                        "m3index-db-java",
                        "m3/vendor/m3/m3-mindex/db/src/main/java/p/A.java"));

        // Stale/legacy rows still infer from target path.
        assertEquals(
                ImportPlan.Lane.M3INDEX_COMPILER,
                ImportPlan.lane(
                        "stale",
                        "m3/vendor/m3/m3-mindex/compiler/src/main/java/p/A.java"));
    }

    @Test
    void manifestEntryEnforcesMirrorAndCategoryFamilyWithoutWeakeningApacheGate() {
        String source =
                "m3-mindex/precompute-api/src/main/java/p/A.java";
        String target = "m3/vendor/m3/" + source;

        new ImportManifest.Entry(
                "m3index-precompute-api-java",
                source,
                target,
                "1".repeat(64),
                "Apache-2.0",
                ImportManifest.Mode.APACHE_SOURCE);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ImportManifest.Entry(
                                "m3index-precompute-api-java",
                                source,
                                "m3/vendor/m3/renamed/A.java",
                                "1".repeat(64),
                                "Apache-2.0",
                                ImportManifest.Mode.APACHE_SOURCE));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ImportManifest.Entry(
                                "indexstring-java",
                                source,
                                target,
                                "1".repeat(64),
                                "Apache-2.0",
                                ImportManifest.Mode.APACHE_SOURCE));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ImportManifest.Entry(
                                "m3index-precompute-api-java",
                                source,
                                target,
                                "1".repeat(64),
                                "MIT",
                                ImportManifest.Mode.APACHE_SOURCE));
    }

    @Test
    void collectionAndAlgorithmSourcesResolveToTheirCanonicalM3Coordinates() {
        List<String> prefixes = List.of(
                "m3-mindex/collections/", "m3-mindex/algorithm/");
        List<String> names = List.of("M3INDEX_COLLECTIONS", "M3INDEX_ALGORITHM");
        List<String> owners = List.of(
                "com.synexia:m3-m3index-collections",
                "com.synexia:m3-m3index-algorithm");
        for (int i = 0; i < prefixes.size(); i++) {
            CanonicalFamilyPolicy.Family family =
                    CanonicalFamilyPolicy.requireFamily(
                            prefixes.get(i) + "src/main/java/p/A.java");
            assertEquals(names.get(i), family.name());
            assertEquals(owners.get(i), family.canonicalOwner());
            assertEquals(family, CanonicalFamilyPolicy.requireFamily(
                    prefixes.get(i) + "pom.xml"));
        }

        // Additive coordinates retain the original, broad alias custody classification.
        assertEquals(CanonicalFamilyPolicy.Family.M3INDEX_ALIAS,
                CanonicalFamilyPolicy.requireFamily(
                        "m3-m3index/collections/pom.xml"));
        assertEquals(CanonicalFamilyPolicy.Family.M3INDEX_ALIAS,
                CanonicalFamilyPolicy.requireFamily(
                        "m3-m3index/algorithm/pom.xml"));
    }

    @Test
    void collectionAndAlgorithmIntakeRetainsApacheAndExactMirrorRequirements() {
        for (String prefix : List.of(
                "m3-mindex/collections/", "m3-mindex/algorithm/")) {
            String source = prefix + "src/main/java/p/A.java";
            String target = "m3/vendor/m3/" + source;
            new ImportManifest.Entry(
                    "m3index-family", source, target, "1".repeat(64), "Apache-2.0",
                    ImportManifest.Mode.APACHE_SOURCE);
            assertThrows(IllegalArgumentException.class, () ->
                    new ImportManifest.Entry(
                            "m3index-family", source, "m3/vendor/m3/renamed/A.java",
                            "1".repeat(64), "Apache-2.0",
                            ImportManifest.Mode.APACHE_SOURCE));
            assertThrows(IllegalArgumentException.class, () ->
                    new ImportManifest.Entry(
                            "m3index-family", source, target, "1".repeat(64), "MIT",
                            ImportManifest.Mode.APACHE_SOURCE));
            assertThrows(IllegalArgumentException.class, () ->
                    new ImportManifest.Entry(
                            "indexstring-java", source, target, "1".repeat(64), "Apache-2.0",
                            ImportManifest.Mode.APACHE_SOURCE));
        }
    }

    @Test
    void collectionAndAlgorithmLookalikesAndUnsafePathsStayRefused() {
        for (String source : List.of(
                "m3-mindex/collections-old/pom.xml",
                "m3-mindex/algorithmic/pom.xml",
                "m3-mindex/collections",
                "m3-mindex/algorithm")) {
            assertTrue(CanonicalFamilyPolicy.classify(source).isEmpty());
            assertThrows(IllegalArgumentException.class, () ->
                    CanonicalFamilyPolicy.requireCategoryFamily(
                            "m3index-family", source));
        }
        for (String source : List.of(
                "../m3-mindex/collections/pom.xml",
                "m3-mindex/algorithm/../compiler/pom.xml",
                "m3-mindex/collections//pom.xml")) {
            assertThrows(IllegalArgumentException.class, () ->
                    CanonicalFamilyPolicy.classify(source));
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
