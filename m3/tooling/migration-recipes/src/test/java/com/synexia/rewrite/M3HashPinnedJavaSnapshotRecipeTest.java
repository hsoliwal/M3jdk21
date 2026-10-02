// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

final class M3HashPinnedJavaSnapshotRecipeTest implements RewriteTest {
    private static final String BEFORE = """
            package p;

            final class A {
                private int value() {
                    return 1;
                }
            }
            """;

    private static final String AFTER = """
            package p;

            final class A {
                private int value() {
                    int m3$atom = 1;
                    return m3$atom;
                }
            }
            """;

    @Test
    void exactOpenJdk21PreimageAppliesReviewedPostimage() {
        rewriteRun(
                spec -> spec.recipe(
                        new M3HashPinnedJavaSnapshotRecipe("jdk21-openjdk-path")),
                java(BEFORE, AFTER)
                        .path("src/java.base/share/classes/p/A.java"));
    }

    @Test
    void exactReviewedPostimageIsAlreadyAtFixedPoint() {
        rewriteRun(
                spec -> spec.recipe(
                        new M3HashPinnedJavaSnapshotRecipe("jdk21-openjdk-path")),
                java(AFTER)
                        .path("src/java.base/share/classes/p/A.java"));
    }

    @Test
    void exactOpenJdk21PathRecipeRejectsDriftInsteadOfGuessing() {
        IllegalStateException drift = assertThrows(
                IllegalStateException.class,
                () -> rewriteRun(
                        spec -> spec.recipe(
                                new M3HashPinnedJavaSnapshotRecipe("jdk21-openjdk-path")),
                        java(
                                        """
                                        package p;

                                        final class A {
                                            private int value() {
                                                return 2;
                                            }
                                        }
                                        """)
                                .path("src/java.base/share/classes/p/A.java")));
        assertTrue(drift.getMessage().contains("source drift"));
    }

    @Test
    void recipeMetadataAndCrateValidationRemainExplicit() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("jdk21-openjdk-path");
        assertEquals("jdk21-openjdk-path", recipe.getCrateName());
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getDisplayName().contains("hash-pinned"));
        assertTrue(recipe.getDescription().contains("exact SHA-256"));
        assertTrue(recipe.getTags().contains("candidate-only"));

        assertThrows(
                IllegalArgumentException.class,
                () -> new M3HashPinnedJavaSnapshotRecipe(""));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3HashPinnedJavaSnapshotRecipe("Bad_Crate"));
    }
}
