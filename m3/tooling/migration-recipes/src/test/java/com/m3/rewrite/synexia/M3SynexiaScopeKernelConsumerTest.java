// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.synexia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class M3SynexiaScopeKernelConsumerTest {
    @Test
    void legacyAndCanonicalScopeLatticesRemainOrdinallyIdentical() {
        com.m3.rewrite.scope.M3EditScope[] legacy =
                com.m3.rewrite.scope.M3EditScope.values();
        com.synexia.rewrite.scope.M3EditScope[] canonical =
                com.synexia.rewrite.scope.M3EditScope.values();

        assertEquals(legacy.length, canonical.length);
        for (int i = 0; i < legacy.length; i++) {
            assertEquals(legacy[i].name(), canonical[i].name());
            assertEquals(
                    legacy[i].next().name(),
                    canonical[i].next().name());
            for (int j = 0; j < legacy.length; j++) {
                assertEquals(
                        legacy[i].permits(legacy[j]),
                        canonical[i].permits(canonical[j]));
                assertEquals(
                        legacy[i].promote(legacy[j]).name(),
                        canonical[i].promote(canonical[j]).name());
            }
        }
    }

    @Test
    void legacyAndCanonicalPathInferenceRemainBehaviorallyEquivalent() {
        List<List<String>> cases = List.of(
                List.of("src/main/java/p/A.java"),
                List.of("src/main/java/p/A.java", "src/main/java/p/B.java"),
                List.of("src/main/java/p/A.java", "src/test/java/p/ATest.java"),
                List.of(
                        "src/java.base/share/classes/java/lang/String.java",
                        "src/java.base/share/classes/java/lang/Object.java"),
                List.of(
                        "src/java.base/share/classes/java/lang/String.java",
                        "src/java.base/linux/classes/java/lang/Linux.java"),
                List.of(
                        "src/java.base/share/classes/java/lang/String.java",
                        "src/java.logging/share/classes/java/util/logging/Logger.java"),
                List.of("make/modules/java.base/A.gmk", "make/modules/java.base/B.gmk"),
                List.of("docs/internal/A.md", "docs/internal/B.md"),
                List.of("README.md"));

        for (List<String> paths : cases) {
            assertEquals(
                    com.m3.rewrite.scope.M3ScopeInference.forPaths(paths).name(),
                    com.synexia.rewrite.scope.M3ScopeInference.forPaths(paths).name(),
                    paths.toString());
        }

        assertEquals(
                com.m3.rewrite.scope.M3ScopeInference
                        .forJavaPaths(List.of("src\\main\\java\\p\\A.java"))
                        .name(),
                com.synexia.rewrite.scope.M3ScopeInference
                        .forJavaPaths(List.of("src\\main\\java\\p\\A.java"))
                        .name());
    }

    @Test
    void legacyAndCanonicalPoliciesResolveTheSameAuthority() {
        var legacy = new com.m3.rewrite.scope.M3RecipeScopePolicy(
                com.m3.rewrite.scope.M3EditScope.FILE,
                com.m3.rewrite.scope.M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                true);
        var canonical = new com.synexia.rewrite.scope.M3RecipeScopePolicy(
                com.synexia.rewrite.scope.M3EditScope.FILE,
                com.synexia.rewrite.scope.M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                true);

        for (List<String> paths : List.of(
                List.of("src/main/java/p/A.java"),
                List.of("src/main/java/p/A.java", "src/main/java/p/B.java"),
                List.of(
                        "src/java.base/share/classes/java/lang/String.java",
                        "src/java.logging/share/classes/java/util/logging/Logger.java"))) {
            assertEquals(legacy.resolve(paths).name(), canonical.resolve(paths).name());
            assertEquals(
                    legacy.fileLocalMechanical(paths),
                    canonical.fileLocalMechanical(paths));
        }
    }

    @Test
    void invalidPathFamiliesRefuseInBothMirrors() {
        for (String path : List.of(
                "",
                "/src/main/java/p/A.java",
                "C:/src/main/java/p/A.java",
                "src/main/java/p/../A.java",
                "../src/main/java/p/A.java",
                "src/java.base/share/classes/",
                "test/",
                "make/modules/")) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> com.m3.rewrite.scope.M3ScopeInference.forPaths(List.of(path)),
                    "legacy " + path);
            assertThrows(
                    IllegalArgumentException.class,
                    () -> com.synexia.rewrite.scope.M3ScopeInference.forPaths(List.of(path)),
                    "canonical " + path);
        }
    }

    @Test
    void targetRegistryRemainsLocalButUsesTheSameScopeVocabulary() {
        var targetPolicy =
                com.m3.rewrite.scope.M3RecipeScopeRegistry.require(
                        "com.m3.rewrite.backport.M3Jep458BackportRecipe");
        assertEquals("LIBRARY_API", targetPolicy.minimumScope().name());
        assertEquals("EXPLICIT_CONTRACT_CHANGE", targetPolicy.contractMode().name());
        assertEquals(
                com.synexia.rewrite.scope.M3EditScope.LIBRARY_API.name(),
                targetPolicy.minimumScope().name());
        assertEquals(
                com.synexia.rewrite.scope.M3ContractMode.EXPLICIT_CONTRACT_CHANGE.name(),
                targetPolicy.contractMode().name());
    }
}
