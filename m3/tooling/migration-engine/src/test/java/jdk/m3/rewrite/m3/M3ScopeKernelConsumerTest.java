// SPDX-License-Identifier: Apache-2.0
package jdk.m3.rewrite.synexia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

final class M3ScopeKernelConsumerTest {
    @Test
    void targetScopeVocabularyDelegatesToCanonicalM3Lattice() {
        jdk.m3.rewrite.scope.M3EditScope[] target =
                jdk.m3.rewrite.scope.M3EditScope.values();
        jdk.internal.m3.rewrite.M3EditScope[] canonical =
                jdk.internal.m3.rewrite.M3EditScope.values();

        assertEquals(target.length, canonical.length);
        for (int i = 0; i < target.length; i++) {
            assertEquals(target[i].name(), canonical[i].name());
            assertEquals(target[i].next().name(), canonical[i].next().name());
            for (int j = 0; j < target.length; j++) {
                assertEquals(
                        target[i].permits(target[j]),
                        canonical[i].permits(canonical[j]));
                assertEquals(
                        target[i].promote(target[j]).name(),
                        canonical[i].promote(canonical[j]).name());
            }
        }
    }

    @Test
    void javaPathInferenceMatchesCanonicalM3Exactly() {
        for (List<String> paths : List.of(
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
                        "src/java.logging/share/classes/java/util/logging/Logger.java"))) {
            assertEquals(
                    jdk.internal.m3.rewrite.M3ScopeInference.inferJavaTargets(paths).name(),
                    jdk.m3.rewrite.scope.M3ScopeInference.forJavaPaths(paths).name(),
                    paths.toString());
        }

        assertEquals(
                jdk.internal.m3.rewrite.M3ScopeInference
                        .inferJavaTargets(List.of("src/main/java/p/A.java"))
                        .name(),
                jdk.m3.rewrite.scope.M3ScopeInference
                        .forJavaPaths(List.of("src\\main\\java\\p\\A.java"))
                        .name());
    }

    @Test
    void targetOnlyNonJavaInferenceDoesNotPretendToBeM3JavaAuthority() {
        assertEquals(
                "PACKAGE",
                jdk.m3.rewrite.scope.M3ScopeInference
                        .forPaths(List.of(
                                "make/modules/java.base/A.gmk",
                                "make/modules/java.base/B.gmk"))
                        .name());
        assertEquals(
                "PACKAGE",
                jdk.m3.rewrite.scope.M3ScopeInference
                        .forPaths(List.of("docs/internal/A.md", "docs/internal/B.md"))
                        .name());
        assertEquals(
                "FILE",
                jdk.m3.rewrite.scope.M3ScopeInference
                        .forPaths(List.of("README.md"))
                        .name());
    }

    @Test
    void invalidJavaPathFamiliesRefuseInTargetAndCanonicalOwners() {
        for (String path : List.of(
                "",
                "/src/main/java/p/A.java",
                "C:/src/main/java/p/A.java",
                "src/main/java/p/../A.java",
                "../src/main/java/p/A.java")) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> jdk.m3.rewrite.scope.M3ScopeInference.forJavaPaths(List.of(path)),
                    "target " + path);
            assertThrows(
                    IllegalArgumentException.class,
                    () -> jdk.internal.m3.rewrite.M3ScopeInference.inferJavaTargets(List.of(path)),
                    "canonical " + path);
        }
    }

    @Test
    void targetRegistryRemainsLocalButUsesCanonicalScopeVocabulary() {
        var targetPolicy =
                jdk.m3.rewrite.scope.M3RecipeScopeRegistry.require(
                        "jdk.m3.rewrite.backport.M3Jep458BackportRecipe");
        assertEquals("LIBRARY_API", targetPolicy.minimumScope().name());
        assertEquals("EXPLICIT_CONTRACT_CHANGE", targetPolicy.contractMode().name());
        assertEquals(
                jdk.internal.m3.rewrite.M3EditScope.LIBRARY_API.name(),
                targetPolicy.minimumScope().name());
    }
}
